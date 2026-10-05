package net.arro.paxium.compat.jei;

import mezz.jei.api.gui.builder.IIngredientAcceptor;
import mezz.jei.api.gui.ingredient.IRecipeSlotDrawable;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.smithing.ISmithingCategoryExtension;
import net.arro.paxium.recipe.PaxiumUpgradeSmithingRecipe;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;

// Shows the Fire Meter upgrade recipes in JEI's vanilla smithing tab; the output mirrors whichever
// armor piece is currently cycling in the base slot, at level I.
public class PaxiumUpgradeSmithingExtension implements ISmithingCategoryExtension<PaxiumUpgradeSmithingRecipe> {
    @Override
    public <T extends IIngredientAcceptor<T>> void setTemplate(PaxiumUpgradeSmithingRecipe recipe, T acceptor) {
        acceptor.addIngredients(recipe.getTemplate());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setBase(PaxiumUpgradeSmithingRecipe recipe, T acceptor) {
        acceptor.addIngredients(recipe.getBase());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setAddition(PaxiumUpgradeSmithingRecipe recipe, T acceptor) {
        acceptor.addIngredients(recipe.getAddition());
    }

    @Override
    public <T extends IIngredientAcceptor<T>> void setOutput(PaxiumUpgradeSmithingRecipe recipe, T acceptor) {
        acceptor.addItemStacks(Arrays.stream(recipe.getBase().getItems()).map(recipe::upgrade).toList());
    }

    @Override
    public void onDisplayedIngredientsUpdate(PaxiumUpgradeSmithingRecipe recipe, IRecipeSlotDrawable templateSlot,
                                             IRecipeSlotDrawable baseSlot, IRecipeSlotDrawable additionSlot,
                                             IRecipeSlotDrawable outputSlot, IFocusGroup focuses) {
        ItemStack base = baseSlot.getDisplayedItemStack().orElse(ItemStack.EMPTY);
        if (!base.isEmpty()) {
            outputSlot.createDisplayOverrides().addItemStack(recipe.upgrade(base));
        }
    }
}
