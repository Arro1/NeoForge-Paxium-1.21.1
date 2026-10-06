package net.arro.paxium.block.custom;

import com.mojang.serialization.MapCodec;
import net.arro.paxium.block.entity.ModBlockEntities;
import net.arro.paxium.block.entity.StarforgeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class StarforgeBlock extends BaseEntityBlock {
    public static final MapCodec<StarforgeBlock> CODEC = simpleCodec(StarforgeBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    // True during the pre-crafting charge-up (LIT is also true then); drives the client-side beacon beam.
    public static final BooleanProperty CHARGING = BooleanProperty.create("charging");

    public StarforgeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(super.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(LIT, false).setValue(CHARGING, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    // Only fires for an actual placement by an entity (not pistons or /setblock), once per placement.
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level instanceof ServerLevel serverLevel) {
            playPlacementImpact(serverLevel, pos);
        }
    }

    // The forge slamming into the ground: a deep layered impact, a shockwave of the ground's own
    // debris rolling outward, a dust puff, sparks - and a silent bolt from the sky if it can see it.
    private static void playPlacementImpact(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.0F, 0.5F);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.5F);

        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        BlockState ground = level.getBlockState(pos.below());
        if (!ground.isAir()) {
            BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, ground);
            shockwaveRing(level, debris, x, y + 0.05, z, 0.6, 20, 0.25);
            shockwaveRing(level, debris, x, y + 0.05, z, 1.2, 32, 0.35);
        }

        shockwaveRing(level, ParticleTypes.POOF, x, y + 0.1, z, 0.7, 16, 0.12);
        level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y + 0.2, z, 6, 0.6, 0.05, 0.6, 0.01);

        level.sendParticles(ParticleTypes.LAVA, x, y + 1.0, z, 4, 0.2, 0.0, 0.2, 0.0);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y + 1.0, z, 16, 0.3, 0.1, 0.3, 0.4);

        if (level.getBlockEntity(pos) instanceof StarforgeBlockEntity starforge
                && StarforgeBlockEntity.hasSkyAccess(level, pos)) {
            starforge.strikeVisualOnly(level);
        }
    }

    // Count 0 turns the offset into a velocity, so each particle travels outward from the center.
    private static void shockwaveRing(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                                      double radius, int points, double speed) {
        for (int i = 0; i < points; i++) {
            double angle = Mth.TWO_PI * i / points + level.random.nextDouble() * 0.2;
            double dirX = Math.cos(angle);
            double dirZ = Math.sin(angle);
            level.sendParticles(particle, x + dirX * radius, y, z + dirZ * radius, 0,
                    dirX, 0.15, dirZ, speed);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, CHARGING);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new StarforgeBlockEntity(blockPos, blockState);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof StarforgeBlockEntity starforgeBlockEntity) {
                starforgeBlockEntity.drops();
            }
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos,
                                              Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
        if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if(entity instanceof StarforgeBlockEntity starforgeBlockEntity) {
                ((ServerPlayer) pPlayer).openMenu(new SimpleMenuProvider(starforgeBlockEntity, Component.literal("Starforge")), pPos);
            } else {
                throw new IllegalStateException("Our Container provider is missing!");
            }
        }

        return ItemInteractionResult.sidedSuccess(pLevel.isClientSide());
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }

        double x = pos.getX() + 0.5;
        double y = pos.getY();
        double z = pos.getZ() + 0.5;

        // Quieter than before so it sits under the anvil hammering instead of competing with it.
        if (random.nextDouble() < 0.05) {
            level.playLocalSound(x, y, z, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 1.0F, 1.0F, false);
        }

        Direction direction = state.getValue(FACING);
        Direction.Axis axis = direction.getAxis();
        double lateral = random.nextDouble() * 0.6 - 0.3;
        double offsetX = axis == Direction.Axis.X ? direction.getStepX() * 0.52 : lateral;
        double offsetZ = axis == Direction.Axis.Z ? direction.getStepZ() * 0.52 : lateral;
        double offsetY = random.nextDouble() * 6.0 / 16.0;

        level.addParticle(ParticleTypes.FLAME, x + offsetX, y + offsetY, z + offsetZ, 0.0, 0.0, 0.0);

        // Bright motes drifting up out of the forge, like a small star burning inside it.
        if (random.nextDouble() < 0.5) {
            double starX = x + (random.nextDouble() - 0.5) * 0.8;
            double starZ = z + (random.nextDouble() - 0.5) * 0.8;
            double riseSpeed = 0.02 + random.nextDouble() * 0.02;
            level.addParticle(ParticleTypes.END_ROD, starX, y + 0.2, starZ, 0.0, riseSpeed, 0.0);
        }

        if (random.nextDouble() < 0.2) {
            double glowX = x + (random.nextDouble() - 0.5) * 0.6;
            double glowZ = z + (random.nextDouble() - 0.5) * 0.6;
            double glowY = y + 0.3 + random.nextDouble() * 0.5;
            level.addParticle(ParticleTypes.GLOW, glowX, glowY, glowZ, 0.0, 0.0, 0.0);
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if(level.isClientSide()) {
            return createTickerHelper(blockEntityType, ModBlockEntities.STARFORGE_BE.get(),
                    (level1, blockPos, blockState, blockEntity) -> blockEntity.clientTick(level1, blockPos, blockState));
        }

        return createTickerHelper(blockEntityType, ModBlockEntities.STARFORGE_BE.get(),
                (level1, blockPos, blockState, blockEntity) -> blockEntity.tick(level1, blockPos, blockState));
    }
}
