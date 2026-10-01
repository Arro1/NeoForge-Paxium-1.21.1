package net.arro.paxium.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.arro.paxium.entity.custom.PaxiumFireBurstEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

// No model - the fire burst's own particle trail (see PaxiumFireBurstEntity) is the entire
// visual, so this renderer intentionally draws nothing.
public class PaxiumFireBurstRenderer extends EntityRenderer<PaxiumFireBurstEntity> {
    public PaxiumFireBurstRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PaxiumFireBurstEntity entity) {
        return TextureManager.INTENTIONAL_MISSING_TEXTURE;
    }

    @Override
    public void render(PaxiumFireBurstEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight) {
    }
}
