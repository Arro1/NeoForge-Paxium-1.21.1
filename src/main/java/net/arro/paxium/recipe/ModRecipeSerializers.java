package net.arro.paxium.recipe;

import net.arro.paxium.Paxium;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Paxium.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, StarforgeRecipe.Serializer> STARFORGING_SERIALIZER =
            RECIPE_SERIALIZERS.register("starforging", StarforgeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, PaxiumUpgradeSmithingRecipe.Serializer> PAXIUM_UPGRADE_SMITHING_SERIALIZER =
            RECIPE_SERIALIZERS.register("armor_upgrade_smithing", PaxiumUpgradeSmithingRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_SERIALIZERS.register(eventBus);
    }
}
