package net.arro.paxium.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.arro.paxium.block.entity.StarforgeBlockEntity;
import net.arro.paxium.client.render.LightningBoltDrawer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// Draws the Starforge's charging beacon beam and its purely visual lightning (see LightningBoltDrawer).
// Strike timing lives in StarforgeBlockEntity - this only draws whatever is currently active.
public class StarforgeBlockEntityRenderer implements BlockEntityRenderer<StarforgeBlockEntity> {
    // Paxium gold, opaque ARGB.
    private static final int BEAM_COLOR = 0xFFFFC14D;
    // Bolts land on the center of the forge's top face.
    private static final Vec3 BOLT_BASE = new Vec3(0.5, 1.0, 0.5);

    public StarforgeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StarforgeBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (blockEntity.isChargingClient()) {
            renderChargingBeam(blockEntity, partialTick, poseStack, bufferSource);
        }

        if (!blockEntity.isStriking() || blockEntity.getBoltHeight() <= 0) {
            return;
        }

        LightningBoltDrawer.draw(poseStack, bufferSource, blockEntity.getBoltSeed(),
                BOLT_BASE, blockEntity.getBoltHeight());
    }

    // Charging: a Paxium-gold beacon beam that climbs from the forge to the sky, reaching full
    // height ~80% of the way through the charge, and thickening slightly as it nears completion.
    private static void renderChargingBeam(StarforgeBlockEntity blockEntity, float partialTick,
                                           PoseStack poseStack, MultiBufferSource bufferSource) {
        if (blockEntity.getLevel() == null) {
            return;
        }

        float charge = Mth.clamp((blockEntity.getClientChargeTicks() + partialTick) / StarforgeBlockEntity.CHARGE_TIME, 0.0F, 1.0F);
        float rise = Mth.clamp(charge / 0.8F, 0.0F, 1.0F);
        float eased = 1.0F - (1.0F - rise) * (1.0F - rise);
        int height = Math.max(1, Math.round(eased * blockEntity.getBoltHeight()));

        float beamRadius = 0.12F + 0.1F * charge;
        float glowRadius = beamRadius + 0.08F;

        BeaconRenderer.renderBeaconBeam(poseStack, bufferSource, BeaconRenderer.BEAM_LOCATION, partialTick, 1.0F,
                blockEntity.getLevel().getGameTime(), 1, height, BEAM_COLOR, beamRadius, glowRadius);
    }

    // The bolt reaches far above the block, so keep rendering when the forge itself is off-screen.
    @Override
    public boolean shouldRenderOffScreen(StarforgeBlockEntity blockEntity) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(StarforgeBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos())
                .expandTowards(0.0, blockEntity.getBoltHeight() + 1.0, 0.0)
                .inflate(6.0, 0.0, 6.0);
    }
}
