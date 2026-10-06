package net.arro.paxium.block.entity;

import net.arro.paxium.block.custom.StarforgeBlock;
import net.arro.paxium.recipe.ModRecipeTypes;
import net.arro.paxium.recipe.StarforgeRecipe;
import net.arro.paxium.recipe.StarforgeRecipeInput;
import net.arro.paxium.screen.custom.StarforgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;

public class StarforgeBlockEntity extends BlockEntity implements MenuProvider {
    private static final int INPUT_SLOT = 0;
    private static final int INPUT_BREATH_SLOT = 1;
    private static final int INPUT_STAR_SLOT = 2;
    private static final int OUTPUT_SLOT = 3;

    public final ItemStackHandler itemHandler = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if(!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return switch(slot) {
                // Any Starforge recipe's base item (raw paxium, echo shard, amethyst shard, ...).
                case INPUT_SLOT -> level != null && level.getRecipeManager()
                        .getAllRecipesFor(ModRecipeTypes.STARFORGING.get()).stream()
                        .anyMatch(recipe -> recipe.value().getBase().test(stack));
                case INPUT_BREATH_SLOT -> stack.getItem() == Items.DRAGON_BREATH;
                case INPUT_STAR_SLOT -> stack.getItem() == Items.NETHER_STAR;
                case OUTPUT_SLOT -> false;
                default -> super.isItemValid(slot, stack);
            };
        }

        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (slot == OUTPUT_SLOT) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

    };

    // 10 seconds per craft. Shared with the JEI category's arrow animation.
    public static final int CRAFT_TIME = 200;
    // 5 second charge-up before the first craft after the forge was stopped.
    public static final int CHARGE_TIME = 100;
    private static final int CHARGE_CHIME_INTERVAL = 10;

    // IDLE -> CHARGING -> CRAFTING, looping in CRAFTING while ingredients last. Anything that stops
    // crafting drops back to IDLE, so the next start charges again.
    public enum Phase { IDLE, CHARGING, CRAFTING }

    // ContainerData indices, read by StarforgeMenu.
    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_SKY_ACCESS = 2;
    public static final int DATA_PHASE = 3;
    public static final int DATA_CHARGE_PROGRESS = 4;
    public static final int DATA_COUNT = 5;

    // Each strike is one lightning bolt + anvil clink, scheduled by the server and mirrored to
    // clients through a block event so the sound and the bolt land on the same tick.
    private static final int MIN_STRIKE_INTERVAL = 30;
    private static final int MAX_STRIKE_INTERVAL = 50;
    private static final int STRIKE_EVENT_ID = 1;

    // Client-only lightning state, set by triggerEvent() and read by StarforgeBlockEntityRenderer.
    private static final int STRIKE_DURATION = 6;
    private static final int MAX_BOLT_HEIGHT = 96;

    protected final ContainerData data;
    private int progress = 0;
    private final int maxProgress = CRAFT_TIME;
    private int ticksUntilStrike = MIN_STRIKE_INTERVAL;
    private Phase phase = Phase.IDLE;
    private int chargeProgress = 0;

    private int strikeTicksLeft = 0;
    private long boltSeed = 0L;
    // Client-only: counts up while the block state says CHARGING, so the beam can grow without syncing.
    private int clientChargeTicks = 0;


    public StarforgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.STARFORGE_BE.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case DATA_PROGRESS -> StarforgeBlockEntity.this.progress;
                    case DATA_MAX_PROGRESS -> StarforgeBlockEntity.this.maxProgress;
                    // Computed live so the screen's warning updates even on an empty, idle forge.
                    case DATA_SKY_ACCESS -> level != null && hasSkyAccess(level, getBlockPos()) ? 1 : 0;
                    case DATA_PHASE -> StarforgeBlockEntity.this.phase.ordinal();
                    case DATA_CHARGE_PROGRESS -> StarforgeBlockEntity.this.chargeProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                // Everything else is fixed or derived, so only progress is settable.
                if (i == DATA_PROGRESS) {
                    StarforgeBlockEntity.this.progress = value;
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.paxium.starforge");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new StarforgeMenu(i, inventory, this, this.data);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries));
        pTag.putInt("starforge.progress", progress);
        pTag.putString("starforge.phase", phase.name());
        pTag.putInt("starforge.charge_progress", chargeProgress);

        super.saveAdditional(pTag, pRegistries);
    }

    // max_progress is no longer read: older saves stored 80, which would override CRAFT_TIME.
    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);

        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
        progress = pTag.getInt("starforge.progress");
        chargeProgress = pTag.getInt("starforge.charge_progress");
        try {
            phase = Phase.valueOf(pTag.getString("starforge.phase"));
        } catch (IllegalArgumentException e) {
            // Missing (pre-charging saves) or unknown: start idle, the next tick re-evaluates.
            phase = Phase.IDLE;
        }
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide()) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level;

        if (!hasRecipe()) {
            // Interrupted (ingredient removed, output full, sky covered) - power down. A craft that
            // simply used up the last ingredients already went idle in craftTick without this sound.
            if (phase != Phase.IDLE) {
                serverLevel.playSound(null, blockPos, SoundEvents.BEACON_DEACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            setPhase(Phase.IDLE);
            resetProgress();
            chargeProgress = 0;
            setChanged(level, blockPos, blockState);
            return;
        }

        switch (phase) {
            case IDLE -> {
                chargeProgress = 0;
                setPhase(Phase.CHARGING);
                serverLevel.playSound(null, blockPos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            case CHARGING -> chargeTick(serverLevel, blockPos);
            case CRAFTING -> craftTick(serverLevel, blockPos);
        }
        setChanged(level, blockPos, blockState);
    }

    // Calibration: chimes rising in pitch, enchant glyphs drawn into the forge, beacon swells,
    // then power-up and the first lightning strike as crafting begins.
    private void chargeTick(ServerLevel level, BlockPos pos) {
        chargeProgress++;
        RandomSource random = level.random;
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;

        if (chargeProgress % CHARGE_CHIME_INTERVAL == 0) {
            float pitch = 0.6F + 1.4F * chargeProgress / CHARGE_TIME;
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, pitch);
        }

        if (chargeProgress == CHARGE_TIME / 2 || chargeProgress == CHARGE_TIME * 4 / 5) {
            level.playSound(null, pos, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.0F);
        }

        // Count 0 makes the offsets a velocity; enchant particles travel from (pos + velocity) to pos.
        for (int i = 0; i < 3; i++) {
            level.sendParticles(ParticleTypes.ENCHANT, x, y + 0.2, z, 0,
                    (random.nextDouble() - 0.5) * 4.0, random.nextDouble() * 1.5, (random.nextDouble() - 0.5) * 4.0, 1.0);
        }

        if (chargeProgress >= CHARGE_TIME) {
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.0F);
            chargeProgress = 0;
            resetProgress();
            setPhase(Phase.CRAFTING);
            strike(level, pos);
        }
    }

    private void craftTick(ServerLevel level, BlockPos pos) {
        increaseCraftingProgress();

        if (hasCraftingFinished()) {
            craftItem();
            resetProgress();
            strike(level, pos);
            // Out of ingredients (or output full): stop quietly - the completion chime just played.
            if (!hasRecipe()) {
                setPhase(Phase.IDLE);
            }
        } else if (--ticksUntilStrike <= 0) {
            strike(level, pos);
        }
    }

    // Keeps the block state (lit front, charging beam) in step with the phase.
    private void setPhase(Phase newPhase) {
        phase = newPhase;
        BlockState state = getBlockState();
        BlockState updated = state
                .setValue(StarforgeBlock.LIT, newPhase != Phase.IDLE)
                .setValue(StarforgeBlock.CHARGING, newPhase == Phase.CHARGING);
        if (updated != state && level != null) {
            level.setBlock(getBlockPos(), updated, 3);
        }
    }

    // A lightning strike from the sky: the anvil clink and sparks happen here on the server, the
    // block event tells every nearby client to draw the bolt this same tick.
    private void strike(ServerLevel level, BlockPos pos) {
        RandomSource random = level.random;
        ticksUntilStrike = MIN_STRIKE_INTERVAL + random.nextInt(MAX_STRIKE_INTERVAL - MIN_STRIKE_INTERVAL + 1);

        level.blockEvent(pos, getBlockState().getBlock(), STRIKE_EVENT_ID, 0);
        level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS,
                0.5F, 0.8F + random.nextFloat() * 0.4F);

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.LAVA, x, y, z, 2, 0.2, 0.0, 0.2, 0.0);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 8, 0.25, 0.1, 0.25, 0.3);
    }

    // Just the bolt, flash and sparks on nearby clients - no anvil clink. Used for the placement impact.
    public void strikeVisualOnly(ServerLevel level) {
        level.blockEvent(getBlockPos(), getBlockState().getBlock(), STRIKE_EVENT_ID, 0);
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id != STRIKE_EVENT_ID) {
            return super.triggerEvent(id, type);
        }
        if (level != null && level.isClientSide()) {
            startClientStrike(level, getBlockPos());
        }
        return true;
    }

    private void craftItem() {
        Optional<RecipeHolder<StarforgeRecipe>> recipe = getCurrentRecipe();
        if (recipe.isEmpty()) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = getBlockPos();
            serverLevel.playSound(null, pos, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    20, 0.3, 0.3, 0.3, 0.08);
        }

        ItemStack output = recipe.get().value().assemble(getRecipeInput(), level.registryAccess());

        itemHandler.extractItem(INPUT_SLOT, 1, false);
        itemHandler.extractItem(INPUT_BREATH_SLOT, 1, false);
        itemHandler.extractItem(INPUT_STAR_SLOT, 1, false);
        itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(output.getItem(),
                itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + output.getCount()));
    }

    private void resetProgress() {
        progress = 0;
    }

    // Client side only: flickers an active bolt. No entity is spawned, so there is no thunder,
    // fire or damage - just the renderer and particles.
    public void clientTick(Level level, BlockPos pos, BlockState state) {
        clientChargeTicks = state.getValue(StarforgeBlock.CHARGING) ? clientChargeTicks + 1 : 0;

        if (strikeTicksLeft > 0) {
            strikeTicksLeft--;
            // New seed each tick so the bolt flickers and re-forks while it's visible.
            boltSeed = level.random.nextLong();
        }
    }

    private void startClientStrike(Level level, BlockPos pos) {
        RandomSource random = level.random;
        strikeTicksLeft = STRIKE_DURATION;
        boltSeed = random.nextLong();

        double x = pos.getX() + 0.5;
        double y = pos.getY() + 1.0;
        double z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.FLASH, x, y + 0.1, z, 0.0, 0.0, 0.0);
        for (int i = 0; i < 12; i++) {
            level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    x + (random.nextDouble() - 0.5) * 0.9, y + random.nextDouble() * 0.3, z + (random.nextDouble() - 0.5) * 0.9,
                    (random.nextDouble() - 0.5) * 0.4, random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.4);
        }
    }

    // The forge draws its power from the sky: nothing that blocks motion (blocks, glass, leaves...)
    // may sit anywhere above it. Uses the heightmap so it works the same in every dimension.
    public static boolean hasSkyAccess(Level level, BlockPos pos) {
        return level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) <= pos.getY() + 1;
    }

    public boolean isChargingClient() {
        return getBlockState().getValue(StarforgeBlock.CHARGING);
    }

    public int getClientChargeTicks() {
        return clientChargeTicks;
    }

    public boolean isStriking() {
        return strikeTicksLeft > 0;
    }

    public long getBoltSeed() {
        return boltSeed;
    }

    // Sky access is guaranteed while crafting, so the bolt always comes from high above,
    // capped by the build limit.
    public int getBoltHeight() {
        if (level == null) {
            return MAX_BOLT_HEIGHT;
        }
        return Math.max(1, Math.min(MAX_BOLT_HEIGHT, level.getMaxBuildHeight() - getBlockPos().getY() - 1));
    }

    private boolean hasCraftingFinished() {
        return this.progress >= this.maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }

    private StarforgeRecipeInput getRecipeInput() {
        return new StarforgeRecipeInput(itemHandler.getStackInSlot(INPUT_SLOT),
                itemHandler.getStackInSlot(INPUT_BREATH_SLOT), itemHandler.getStackInSlot(INPUT_STAR_SLOT));
    }

    private Optional<RecipeHolder<StarforgeRecipe>> getCurrentRecipe() {
        return level.getRecipeManager().getRecipeFor(ModRecipeTypes.STARFORGING.get(), getRecipeInput(), level);
    }

    private boolean hasRecipe() {
        if (!hasSkyAccess(level, getBlockPos())) {
            return false;
        }

        Optional<RecipeHolder<StarforgeRecipe>> recipe = getCurrentRecipe();
        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack output = recipe.get().value().getResultItem(level.registryAccess());

        return canInsertAmountIntoOutputSlot(output.getCount()) && canInsertItemIntoOutputSlot(output);
    }

    private boolean canInsertItemIntoOutputSlot(ItemStack output) {
        return itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ||
                itemHandler.getStackInSlot(OUTPUT_SLOT).getItem() == output.getItem();
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        int maxCount = itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() ? 64 : itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
        int currentCount = itemHandler.getStackInSlot(OUTPUT_SLOT).getCount();

        return maxCount >= currentCount + count;
    }


    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }


}
