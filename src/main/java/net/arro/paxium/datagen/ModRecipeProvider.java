package net.arro.paxium.datagen;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.component.PaxiumUpgradeStat;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.recipe.PaxiumUpgradeSmithingRecipe;
import net.arro.paxium.recipe.StarforgeRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SmithingTransformRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput recipeOutput) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.STARFORGE.get())
                .pattern("OGO")
                .pattern("NBN")
                .pattern("OGO")
                .define('O', Items.OBSIDIAN)
                .define('G', Items.GOLD_BLOCK)
                .define('N', Items.NETHERITE_BLOCK)
                .define('B', Items.BEACON)
                .unlockedBy("has_paxium", has(ModItems.RAW_PAXIUM))
                .save(recipeOutput);

        recipeOutput.accept(
                ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "starforging/paxium"),
                new StarforgeRecipe(
                        Ingredient.of(ModItems.RAW_PAXIUM.get()),
                        Ingredient.of(Items.DRAGON_BREATH),
                        Ingredient.of(Items.NETHER_STAR),
                        new ItemStack(ModItems.PAXIUM.get())),
                null);

        starforging(recipeOutput, Items.ECHO_SHARD, ModItems.PAXIUM_CAPACITY_UPGRADE_SMITHING_TEMPLATE.get());
        starforging(recipeOutput, Items.AMETHYST_SHARD, ModItems.PAXIUM_RECHARGE_UPGRADE_SMITHING_TEMPLATE.get());

        starforging(recipeOutput, Items.BLAZE_ROD, ModItems.PAXIUM_BEAM_DAMAGE_UPGRADE_SMITHING_TEMPLATE.get());
        starforging(recipeOutput, Items.FIRE_CHARGE, ModItems.PAXIUM_BLAST_UPGRADE_SMITHING_TEMPLATE.get());

        Ingredient paxiumArmor = Ingredient.of(ModItems.PAXIUM_HELMET.get(), ModItems.PAXIUM_CHESTPLATE.get(),
                ModItems.PAXIUM_LEGGINGS.get(), ModItems.PAXIUM_BOOTS.get());
        upgradeSmithing(recipeOutput, ModItems.PAXIUM_CAPACITY_UPGRADE_SMITHING_TEMPLATE.get(), paxiumArmor,
                PaxiumUpgradeStat.CAPACITY, "capacity_upgrade_smithing");
        upgradeSmithing(recipeOutput, ModItems.PAXIUM_RECHARGE_UPGRADE_SMITHING_TEMPLATE.get(), paxiumArmor,
                PaxiumUpgradeStat.RECHARGE, "recharge_upgrade_smithing");
        upgradeSmithing(recipeOutput, ModItems.PAXIUM_BEAM_DAMAGE_UPGRADE_SMITHING_TEMPLATE.get(),
                Ingredient.of(ModItems.PAXIUM_SWORD.get()), PaxiumUpgradeStat.BEAM_DAMAGE, "beam_damage_upgrade_smithing");
        upgradeSmithing(recipeOutput, ModItems.PAXIUM_BLAST_UPGRADE_SMITHING_TEMPLATE.get(),
                Ingredient.of(ModItems.PAXIUM_BOW.get()), PaxiumUpgradeStat.BLAST, "blast_upgrade_smithing");

        smithingUpgrade(recipeOutput, Items.NETHERITE_HELMET, ModItems.PAXIUM_HELMET.get(), "paxium_helmet_smithing");
        smithingUpgrade(recipeOutput, Items.NETHERITE_CHESTPLATE, ModItems.PAXIUM_CHESTPLATE.get(), "paxium_chestplate_smithing");
        smithingUpgrade(recipeOutput, Items.NETHERITE_LEGGINGS, ModItems.PAXIUM_LEGGINGS.get(), "paxium_leggings_smithing");
        smithingUpgrade(recipeOutput, Items.NETHERITE_BOOTS, ModItems.PAXIUM_BOOTS.get(), "paxium_boots_smithing");
        smithingUpgrade(recipeOutput, Items.NETHERITE_SWORD, ModItems.PAXIUM_SWORD.get(), "paxium_sword_smithing");
        smithingUpgrade(recipeOutput, Items.BOW, ModItems.PAXIUM_BOW.get(), "paxium_bow_smithing");
    }

    // Same Dragon's Breath + Nether Star pair as the Paxium ingot, with a different base item.
    private void starforging(RecipeOutput recipeOutput, Item base, Item result) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(result);
        recipeOutput.accept(
                ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "starforging/" + id.getPath()),
                new StarforgeRecipe(
                        Ingredient.of(base),
                        Ingredient.of(Items.DRAGON_BREATH),
                        Ingredient.of(Items.NETHER_STAR),
                        new ItemStack(result)),
                null);
    }

    // Template + base item + Paxium ingot -> same item with the stat one level higher.
    private void upgradeSmithing(RecipeOutput recipeOutput, Item template, Ingredient base, PaxiumUpgradeStat stat, String recipeId) {
        recipeOutput.accept(
                ResourceLocation.fromNamespaceAndPath(Paxium.MODID, recipeId),
                new PaxiumUpgradeSmithingRecipe(
                        Ingredient.of(template),
                        base,
                        Ingredient.of(ModItems.PAXIUM.get()),
                        stat),
                null);
    }

    private void smithingUpgrade(RecipeOutput recipeOutput, Item base, Item result, String recipeId) {
        SmithingTransformRecipeBuilder.smithing(
                        Ingredient.of(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE),
                        Ingredient.of(base),
                        Ingredient.of(ModItems.PAXIUM.get()),
                        RecipeCategory.COMBAT,
                        result)
                .unlocks("has_paxium", has(ModItems.PAXIUM))
                .save(recipeOutput, ResourceLocation.fromNamespaceAndPath(Paxium.MODID, recipeId));
    }
}
