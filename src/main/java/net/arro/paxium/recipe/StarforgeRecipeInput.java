package net.arro.paxium.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record StarforgeRecipeInput(ItemStack base, ItemStack dragonBreath, ItemStack netherStar) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> base;
            case 1 -> dragonBreath;
            case 2 -> netherStar;
            default -> throw new IllegalArgumentException("No such slot " + index);
        };
    }

    @Override
    public int size() {
        return 3;
    }
}
