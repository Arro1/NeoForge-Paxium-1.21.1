package net.arro.paxium.datagen;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModBlockTagProvider extends BlockTagsProvider {
    public ModBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Paxium.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.DEEPSLATE_PAXIUM_ORE.get());


        //tag(BlockTags.NEEDS_IRON_TOOL)
        //        .add(ModBlocks.BISMUTH_DEEPSLATE_ORE.get());

    }
}