package net.arro.paxium.screen.custom;

import com.mojang.blaze3d.systems.RenderSystem;
import net.arro.paxium.Paxium;
import net.minecraft.Util;
import net.minecraft.util.Mth;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class StarforgeScreen extends AbstractContainerScreen<StarforgeMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Paxium.MODID,"textures/gui/starforge/starforge_gui.png");
    private static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(Paxium.MODID,"textures/gui/arrow_progress.png");

    // Free strip of the GUI to the right of the input column, below the arrow and output slot.
    private static final int STATUS_LEFT = 72;
    private static final int STATUS_RIGHT = 170;
    private static final int STATUS_Y = 62;

    public StarforgeScreen(StarforgeMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float v, int i, int i1) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderProgressArrow(guiGraphics, x, y);
    }

    private void renderProgressArrow(GuiGraphics guiGraphics, int x, int y) {
        if (menu.isCharging()) {
            // Pulsing gold while charging, filled by charge progress.
            float pulse = 0.75F + 0.25F * Mth.sin((Util.getMillis() % 1000L) / 1000.0F * Mth.TWO_PI);
            guiGraphics.setColor(1.0F, 0.76F * pulse, 0.3F * pulse, 1.0F);
            guiGraphics.blit(ARROW_TEXTURE, x + 73, y + 35, 0, 0, menu.getScaledChargeProgress(), 16, 24, 16);
            guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        } else if (menu.isCrafting()) {
            guiGraphics.blit(ARROW_TEXTURE,x + 73, y + 35, 0, 0, menu.getScaledArrowProgress(), 16, 24, 16);
        }
    }

    // Status line under the arrow/output: the sky warning wins over the charging label.
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderLabels(guiGraphics, mouseX, mouseY);

        Component status = null;
        int color = 0;
        if (!menu.hasSkyAccess()) {
            status = Component.translatable("gui.paxium.starforge.sky_obstructed");
            color = 0xFF5555;
        } else if (menu.isCharging()) {
            status = Component.translatable("gui.paxium.starforge.charging");
            color = 0xFFAA00;
        }

        if (status != null) {
            int centerX = (STATUS_LEFT + STATUS_RIGHT) / 2;
            guiGraphics.drawString(font, status, centerX - font.width(status) / 2, STATUS_Y, color, false);
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }
}
