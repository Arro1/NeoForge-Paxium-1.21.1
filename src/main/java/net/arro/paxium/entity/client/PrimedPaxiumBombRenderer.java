package net.arro.paxium.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.arro.paxium.block.ModBlocks;
import net.arro.paxium.client.render.LightningBoltDrawer;
import net.arro.paxium.entity.custom.PrimedPaxiumBombEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.TntMinecartRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

// The charging bomb: the bomb block spinning faster and flashing white more often as the fuse runs
// down (like TNT), a golden beacon beam into the sky, and the silent lightning strikes.
public class PrimedPaxiumBombRenderer extends EntityRenderer<PrimedPaxiumBombEntity> {
    private static final int BEAM_COLOR = 0xFFFFC14D;
    private static final Vec3 BOLT_BASE = new Vec3(0.0, 0.5, 0.0);

    private final BlockRenderDispatcher blockRenderer;

    public PrimedPaxiumBombRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.5F;
        this.blockRenderer = context.getBlockRenderDispatcher();
    }

    @Override
    public void render(PrimedPaxiumBombEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float progress = entity.getProgress(partialTick);
        int fuse = entity.getFuse();
        float time = entity.tickCount + partialTick;

        // Block: spin speeds up, swells over the last second, flashes white faster and faster.
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.5F, 0.0F);
        float swell = fuse < 20 ? 1.0F + 0.3F * Mth.square(1.0F - fuse / 20.0F) : 1.0F;
        poseStack.scale(swell, swell, swell);
        poseStack.mulPose(Axis.YP.rotationDegrees(time * (2.0F + 14.0F * progress * progress)));
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        int flashInterval = Math.max(2, fuse / 30);
        boolean white = (fuse / flashInterval) % 2 == 0;
        TntMinecartRenderer.renderWhiteSolidBlock(blockRenderer, ModBlocks.PAXIUM_BOMB.get().defaultBlockState(),
                poseStack, bufferSource, LightTexture.FULL_BRIGHT, white);
        poseStack.popPose();

        if (entity.level() != null) {
            int height = entity.getBoltHeight();
            float radius = 0.12F + 0.18F * progress;

            // Beam (renderBeaconBeam centers itself on +0.5, so shift back onto the entity).
            poseStack.pushPose();
            poseStack.translate(-0.5F, 0.0F, -0.5F);
            BeaconRenderer.renderBeaconBeam(poseStack, bufferSource, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F,
                    entity.level().getGameTime(), 1, height, BEAM_COLOR, radius, radius + 0.1F);
            poseStack.popPose();

            if (entity.isStriking()) {
                LightningBoltDrawer.draw(poseStack, bufferSource, entity.getBoltSeed(), BOLT_BASE, height);
            }
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    // The beam and bolts reach far above the entity's box.
    @Override
    public boolean shouldRender(PrimedPaxiumBombEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(PrimedPaxiumBombEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
