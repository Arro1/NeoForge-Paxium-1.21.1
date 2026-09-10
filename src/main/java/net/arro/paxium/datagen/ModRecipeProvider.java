package net.arro.paxium.datagen;

import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.item.ModItems;
import net.arro.paxium.recipe.StarforgeRecipe;
import net.minecraft.core.HolderLookup;
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

        smithingUpgrade(recipeOutput, Items.NETHERITE_HELMET, ModItems.PAXIUM_HELMET.get(), "paxium_helmet_smithing");
        smithingUpgrade(recipeOutput, Items.NETHERITE_CHESTPLATE, ModItems.PAXIUM_CHESTPLATE.get(), "paxium_chestplate_smithing");
        smithingUpgrade(recipeOutput, Items.NETHERITE_LEGGINGS, ModItems.PAXIUM_LEGGINGS.get(), "paxium_leggings_smithing");
        smithingUpgrade(recipeOutput, Items.NETHERITE_BOOTS, ModItems.PAXIUM_BOOTS.get(), "paxium_boots_smithing");
        smithingUpgrade(recipeOutput, Items.NETHERITE_SWORD, ModItems.PAXIUM_SWORD.get(), "paxium_sword_smithing");
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
