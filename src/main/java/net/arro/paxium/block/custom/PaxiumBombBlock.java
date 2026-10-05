package net.arro.paxium.block.custom;

import com.mojang.serialization.MapCodec;
import net.arro.paxium.entity.custom.PrimedPaxiumBombEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// Armed only by flint and steel or a redstone signal. Deliberately a plain Block rather than a
// TntBlock: fire, fire charges and other explosions (including another Paxium Bomb) never set it off,
// so there's no accidental chain reaction.
public class PaxiumBombBlock extends Block {
    public static final MapCodec<PaxiumBombBlock> CODEC = simpleCodec(PaxiumBombBlock::new);

    public PaxiumBombBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.FLINT_AND_STEEL)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }

        arm(level, pos, player);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock()) && level.hasNeighborSignal(pos)) {
            arm(level, pos, null);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level.hasNeighborSignal(pos)) {
            arm(level, pos, null);
        }
    }

    // Swaps the block for the floating, charging bomb entity that runs the countdown.
    private static void arm(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
        if (level.isClientSide) {
            return;
        }

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);

        PrimedPaxiumBombEntity primed = new PrimedPaxiumBombEntity(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, igniter);
        level.addFreshEntity(primed);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2.0F, 0.8F);
        level.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 2.0F, 0.5F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
    }
}
