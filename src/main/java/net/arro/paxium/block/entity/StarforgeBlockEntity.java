package net.arro.paxium.block.entity;

import net.arro.paxium.block.custom.StarforgeBlock;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.screen.custom.StarforgeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

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
                case INPUT_SLOT -> stack.getItem() == ModItems.RAW_PAXIUM.get();
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

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 80;


    public StarforgeBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.STARFORGE_BE.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> StarforgeBlockEntity.this.progress;
                    case 1 -> StarforgeBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                switch (i) {
                    case 0: StarforgeBlockEntity.this.progress = value;
                    case 1: StarforgeBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
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
        pTag.putInt("starforge.max_progress", maxProgress);

        super.saveAdditional(pTag, pRegistries);
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag, pRegistries);

        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory"));
        progress = pTag.getInt("starforge.progress");
        maxProgress = pTag.getInt("starforge.max_progress");
    }

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        if (level.isClientSide()) {
            return;
        }

        boolean isCurrentlyLit = blockState.getValue(StarforgeBlock.LIT);

        if (hasRecipe() != isCurrentlyLit) {
            level.setBlock(blockPos, blockState.setValue(StarforgeBlock.LIT, hasRecipe()), 3);
            setChanged(level, blockPos, blockState);
        }

        if(hasRecipe()) {
            increaseCraftingProgress();
            setChanged(level, blockPos, blockState);

            if(hasCraftingFinished()) {
                craftItem();
                resetProgress();
            }
        } else {
            resetProgress();
            setChanged(level, blockPos, blockState);
        }
    }

    private void craftItem() {
        ItemStack output = new ItemStack(ModItems.PAXIUM.get(), 1);

        itemHandler.extractItem(INPUT_SLOT, 1, false);
        itemHandler.extractItem(INPUT_BREATH_SLOT, 1, false);
        itemHandler.extractItem(INPUT_STAR_SLOT, 1, false);
        itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(output.getItem(),
                itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + output.getCount()));
    }

    private void resetProgress() {
        progress = 0;
        //maxProgress = 80;
    }

    private boolean hasCraftingFinished() {
        return this.progress >= this.maxProgress;
    }

    private void increaseCraftingProgress() {
        progress++;
    }

    private boolean hasRecipe() {
        ItemStack output = new ItemStack(ModItems.PAXIUM.get(), 1);

        return itemHandler.getStackInSlot(INPUT_SLOT).is(ModItems.RAW_PAXIUM) &&
                itemHandler.getStackInSlot(INPUT_BREATH_SLOT).is(Items.DRAGON_BREATH) &&
                itemHandler.getStackInSlot(INPUT_STAR_SLOT).is(Items.NETHER_STAR) &&
                canInsertAmountIntoOutputSlot(output.getCount()) && canInsertItemIntoOutputSlot(output);
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
