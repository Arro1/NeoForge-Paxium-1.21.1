package net.arro.paxium.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.recipe.ModRecipeTypes;
import net.arro.paxium.recipe.PaxiumUpgradeSmithingRecipe;
import net.arro.paxium.recipe.StarforgeRecipe;
import net.arro.paxium.screen.ModMenuTypes;
import net.arro.paxium.screen.custom.StarforgeMenu;
import net.arro.paxium.screen.custom.StarforgeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@JeiPlugin
public class JEIPaxiumPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new StarforgeRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getSmithingCategory().addExtension(PaxiumUpgradeSmithingRecipe.class, new PaxiumUpgradeSmithingExtension());
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<StarforgeRecipe> starforgeRecipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.STARFORGING.get()).stream()
                .map(RecipeHolder::value)
                .toList();
        registration.addRecipes(StarforgeRecipeCategory.STARFORGING_TYPE, starforgeRecipes);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.STARFORGE.get()), StarforgeRecipeCategory.STARFORGING_TYPE);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(StarforgeScreen.class, 73, 35, 24, 16, StarforgeRecipeCategory.STARFORGING_TYPE);
    }

    // StarforgeMenu adds the 36 player slots first, then the Starforge slots: 36 base, 37 breath, 38 star, 39 output.
    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(StarforgeMenu.class, ModMenuTypes.STARFORGE_MENU.get(),
                StarforgeRecipeCategory.STARFORGING_TYPE, 36, 3, 0, 36);
    }
}
