package net.arro.paxium.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.arro.paxium.Paxium;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.recipe.StarforgeRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class StarforgeRecipeCategory implements IRecipeCategory<StarforgeRecipe> {
    public static final RecipeType<StarforgeRecipe> STARFORGING_TYPE =
            new RecipeType<>(ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "starforging"), StarforgeRecipe.class);

    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "textures/gui/starforge/starforge_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "textures/gui/arrow_progress.png");

    // Region of starforge_gui.png shown in JEI; slot positions below are StarforgeMenu positions minus this offset.
    private static final int OFFSET_X = 46;
    private static final int OFFSET_Y = 12;
    private static final int WIDTH = 82;
    private static final int HEIGHT = 62;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated arrow;

    public StarforgeRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(GUI_TEXTURE, OFFSET_X, OFFSET_Y, WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModBlocks.STARFORGE.get()));
        // 80 ticks matches StarforgeBlockEntity's maxProgress
        this.arrow = guiHelper.drawableBuilder(ARROW_TEXTURE, 0, 0, 24, 16)
                .setTextureSize(24, 16)
                .buildAnimated(80, IDrawableAnimated.StartDirection.LEFT, false);
    }

    @Override
    public RecipeType<StarforgeRecipe> getRecipeType() {
        return STARFORGING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.paxium.starforging");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    // Slot order must match the Starforge menu slots (paxium, dragon breath, nether star) for recipe transfer.
    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, StarforgeRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 54 - OFFSET_X, 34 - OFFSET_Y).addIngredients(recipe.getPaxium());
        builder.addSlot(RecipeIngredientRole.INPUT, 54 - OFFSET_X, 52 - OFFSET_Y).addIngredients(recipe.getDragonBreath());
        builder.addSlot(RecipeIngredientRole.INPUT, 54 - OFFSET_X, 16 - OFFSET_Y).addIngredients(recipe.getNetherStar());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 104 - OFFSET_X, 34 - OFFSET_Y)
                .addItemStack(recipe.getResultItem(Minecraft.getInstance().level.registryAccess()));
    }

    @Override
    public void draw(StarforgeRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
        arrow.draw(guiGraphics, 73 - OFFSET_X, 35 - OFFSET_Y);
    }
}
