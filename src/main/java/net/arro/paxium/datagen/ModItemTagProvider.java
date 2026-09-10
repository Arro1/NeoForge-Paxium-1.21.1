package net.arro.paxium.datagen;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.util.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

public class ModItemTagProvider extends ItemTagsProvider {
    public ModItemTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                              CompletableFuture<TagLookup<Block>> blockTags, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Paxium.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.TRIMMABLE_ARMOR)
                .add(ModItems.PAXIUM_HELMET.get())
                .add(ModItems.PAXIUM_CHESTPLATE.get())
                .add(ModItems.PAXIUM_LEGGINGS.get())
                .add(ModItems.PAXIUM_BOOTS.get());

        tag(ItemTags.ARMOR_ENCHANTABLE)
                .add(ModItems.PAXIUM_HELMET.get())
                .add(ModItems.PAXIUM_CHESTPLATE.get())
                .add(ModItems.PAXIUM_LEGGINGS.get())
                .add(ModItems.PAXIUM_BOOTS.get());
        tag(ItemTags.HEAD_ARMOR_ENCHANTABLE).add(ModItems.PAXIUM_HELMET.get());
        tag(ItemTags.CHEST_ARMOR_ENCHANTABLE).add(ModItems.PAXIUM_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR_ENCHANTABLE).add(ModItems.PAXIUM_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR_ENCHANTABLE).add(ModItems.PAXIUM_BOOTS.get());

        tag(ItemTags.EQUIPPABLE_ENCHANTABLE)
                .add(ModItems.PAXIUM_HELMET.get())
                .add(ModItems.PAXIUM_CHESTPLATE.get())
                .add(ModItems.PAXIUM_LEGGINGS.get())
                .add(ModItems.PAXIUM_BOOTS.get());
        tag(ItemTags.DURABILITY_ENCHANTABLE)
                .add(ModItems.PAXIUM_HELMET.get())
                .add(ModItems.PAXIUM_CHESTPLATE.get())
                .add(ModItems.PAXIUM_LEGGINGS.get())
                .add(ModItems.PAXIUM_BOOTS.get());
        tag(ItemTags.VANISHING_ENCHANTABLE)
                .add(ModItems.PAXIUM_HELMET.get())
                .add(ModItems.PAXIUM_CHESTPLATE.get())
                .add(ModItems.PAXIUM_LEGGINGS.get())
                .add(ModItems.PAXIUM_BOOTS.get());

        // Membership grants enchantable/sword, enchantable/sharp_weapon, enchantable/weapon,
        // enchantable/fire_aspect, enchantable/durability and enchantable/vanishing transitively.
        tag(ItemTags.SWORDS).add(ModItems.PAXIUM_SWORD.get());
    }
}