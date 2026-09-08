package net.arro.paxium.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class StarforgeRecipe implements Recipe<StarforgeRecipeInput> {
    private final Ingredient paxium;
    private final Ingredient dragonBreath;
    private final Ingredient netherStar;
    private final ItemStack result;

    public StarforgeRecipe(Ingredient paxium, Ingredient dragonBreath, Ingredient netherStar, ItemStack result) {
        this.paxium = paxium;
        this.dragonBreath = dragonBreath;
        this.netherStar = netherStar;
        this.result = result;
    }

    public Ingredient getPaxium() {
        return paxium;
    }

    public Ingredient getDragonBreath() {
        return dragonBreath;
    }

    public Ingredient getNetherStar() {
        return netherStar;
    }

    @Override
    public boolean matches(StarforgeRecipeInput input, Level level) {
        return paxium.test(input.paxium()) && dragonBreath.test(input.dragonBreath()) && netherStar.test(input.netherStar());
    }

    @Override
    public ItemStack assemble(StarforgeRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<StarforgeRecipe> getSerializer() {
        return ModRecipeSerializers.STARFORGING_SERIALIZER.get();
    }

    @Override
    public RecipeType<StarforgeRecipe> getType() {
        return ModRecipeTypes.STARFORGING.get();
    }

    public static class Serializer implements RecipeSerializer<StarforgeRecipe> {
        public static final MapCodec<StarforgeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC.fieldOf("paxium").forGetter(StarforgeRecipe::getPaxium),
                        Ingredient.CODEC.fieldOf("dragon_breath").forGetter(StarforgeRecipe::getDragonBreath),
                        Ingredient.CODEC.fieldOf("nether_star").forGetter(StarforgeRecipe::getNetherStar),
                        ItemStack.CODEC.fieldOf("result").forGetter(recipe -> recipe.result)
                ).apply(instance, StarforgeRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, StarforgeRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, StarforgeRecipe::getPaxium,
                Ingredient.CONTENTS_STREAM_CODEC, StarforgeRecipe::getDragonBreath,
                Ingredient.CONTENTS_STREAM_CODEC, StarforgeRecipe::getNetherStar,
                ItemStack.STREAM_CODEC, recipe -> recipe.result,
                StarforgeRecipe::new
        );

        @Override
        public MapCodec<StarforgeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, StarforgeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
