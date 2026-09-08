package net.arro.paxium.recipe;

import net.arro.paxium.Paxium;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Paxium.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<StarforgeRecipe>> STARFORGING =
            RECIPE_TYPES.register("starforging", () -> new RecipeType<StarforgeRecipe>() {
                @Override
                public String toString() {
                    return "paxium:starforging";
                }
            });

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
    }
}
