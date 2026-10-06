package net.arro.paxium.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.arro.paxium.component.PaxiumUpgradeStat;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;

import java.util.stream.Stream;

// Smithing-table recipe that keeps the base item (armor piece or weapon) as-is and raises one
// PaxiumUpgradeStat by a single level. Stops matching once that stat is at MAX_LEVEL, so a level III
// item shows no output instead of eating the template.
public class PaxiumUpgradeSmithingRecipe implements SmithingRecipe {
    private final Ingredient template;
    private final Ingredient base;
    private final Ingredient addition;
    private final PaxiumUpgradeStat stat;

    public PaxiumUpgradeSmithingRecipe(Ingredient template, Ingredient base, Ingredient addition, PaxiumUpgradeStat stat) {
        this.template = template;
        this.base = base;
        this.addition = addition;
        this.stat = stat;
    }

    public Ingredient getTemplate() {
        return template;
    }

    public Ingredient getBase() {
        return base;
    }

    public Ingredient getAddition() {
        return addition;
    }

    public PaxiumUpgradeStat getStat() {
        return stat;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return template.test(input.template()) && base.test(input.base()) && addition.test(input.addition())
                && stat.getLevel(input.base()) < PaxiumUpgradeStat.MAX_LEVEL;
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input, HolderLookup.Provider registries) {
        return upgrade(input.base());
    }

    // Copies every other component (enchantments, damage, trims, the other stat) from the base.
    public ItemStack upgrade(ItemStack baseStack) {
        return stat.upgraded(baseStack);
    }

    // Display only (recipe book/JEI): the first base item at level I.
    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        ItemStack[] bases = base.getItems();
        return bases.length == 0 ? ItemStack.EMPTY : upgrade(bases[0]);
    }

    @Override
    public boolean isTemplateIngredient(ItemStack stack) {
        return template.test(stack);
    }

    @Override
    public boolean isBaseIngredient(ItemStack stack) {
        return base.test(stack);
    }

    @Override
    public boolean isAdditionIngredient(ItemStack stack) {
        return addition.test(stack);
    }

    @Override
    public boolean isIncomplete() {
        return Stream.of(template, base, addition).anyMatch(Ingredient::hasNoItems);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeSerializers.PAXIUM_UPGRADE_SMITHING_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<PaxiumUpgradeSmithingRecipe> {
        public static final MapCodec<PaxiumUpgradeSmithingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Ingredient.CODEC.fieldOf("template").forGetter(PaxiumUpgradeSmithingRecipe::getTemplate),
                        Ingredient.CODEC.fieldOf("base").forGetter(PaxiumUpgradeSmithingRecipe::getBase),
                        Ingredient.CODEC.fieldOf("addition").forGetter(PaxiumUpgradeSmithingRecipe::getAddition),
                        PaxiumUpgradeStat.CODEC.fieldOf("stat").forGetter(PaxiumUpgradeSmithingRecipe::getStat)
                ).apply(instance, PaxiumUpgradeSmithingRecipe::new)
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, PaxiumUpgradeSmithingRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, PaxiumUpgradeSmithingRecipe::getTemplate,
                Ingredient.CONTENTS_STREAM_CODEC, PaxiumUpgradeSmithingRecipe::getBase,
                Ingredient.CONTENTS_STREAM_CODEC, PaxiumUpgradeSmithingRecipe::getAddition,
                ByteBufCodecs.STRING_UTF8.map(PaxiumUpgradeStat::byName, PaxiumUpgradeStat::getName),
                PaxiumUpgradeSmithingRecipe::getStat,
                PaxiumUpgradeSmithingRecipe::new
        );

        @Override
        public MapCodec<PaxiumUpgradeSmithingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, PaxiumUpgradeSmithingRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
