package net.arro.paxium.datagen;

import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.item.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        add(ModBlocks.DEEPSLATE_PAXIUM_ORE.get(),
                block -> createOreDrop(ModBlocks.DEEPSLATE_PAXIUM_ORE.get(), ModItems.RAW_PAXIUM.get()));

        dropSelf(ModBlocks.STARFORGE.get());
        dropSelf(ModBlocks.PAXIUM_BOMB.get());

    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}