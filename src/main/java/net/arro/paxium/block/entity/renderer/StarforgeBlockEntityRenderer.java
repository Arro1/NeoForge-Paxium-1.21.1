package net.arro.paxium.block.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.arro.paxium.block.entity.StarforgeBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

// Draws the Starforge's purely visual lightning: a jagged bolt from the sky (or ceiling) down onto
// the forge's top face, using vanilla's additive lightning render type. Strike timing lives in
// StarforgeBlockEntity#clientTick - this only draws whatever bolt is currently active.
public class StarforgeBlockEntityRenderer implements BlockEntityRenderer<StarforgeBlockEntity> {
    // Golden-white, a touch warmer than vanilla's blue-white (0.45, 0.45, 0.5).
    private static final float RED = 0.55F;
    private static final float GREEN = 0.50F;
    private static final float BLUE = 0.40F;
    private static final float ALPHA = 0.3F;

    // Like vanilla, each segment is drawn as several nested prisms; the additive blending makes the
    // overlapping inner layers read as a bright core with a softer glow around it.
    private static final int LAYERS = 4;
    private static final float SEGMENT_LENGTH = 4.0F;
    private static final float WANDER = 0.7F;
    private static final int BRANCHES = 2;

    // Paxium gold, opaque ARGB.
    private static final int BEAM_COLOR = 0xFFFFC14D;

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

        RandomSource random = RandomSource.create(blockEntity.getBoltSeed());
        Vec3[] trunk = buildTrunk(random, blockEntity.getBoltHeight());

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        drawPath(matrix, consumer, trunk, LAYERS);

        for (int b = 0; b < BRANCHES && trunk.length > 4; b++) {
            int start = trunk.length / 2 + random.nextInt(trunk.length / 2 - 1);
            drawPath(matrix, consumer, buildBranch(random, trunk[start]), LAYERS - 2);
        }
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

    // Random walk from the sky down; the wander tapers to zero near the bottom so the bolt lands
    // exactly on the center of the top face.
    private static Vec3[] buildTrunk(RandomSource random, int height) {
        int segments = Mth.clamp(Math.round(height / SEGMENT_LENGTH), 3, 24);
        Vec3[] points = new Vec3[segments + 1];
        double x = 0.0;
        double z = 0.0;
        for (int i = 0; i <= segments; i++) {
            double taper = Math.min(1.0, i / 3.0);
            points[i] = new Vec3(0.5 + x * taper, 1.0 + (double) height * i / segments, 0.5 + z * taper);
            x += (random.nextDouble() - 0.5) * 2.0 * WANDER;
            z += (random.nextDouble() - 0.5) * 2.0 * WANDER;
        }
        return points;
    }

    // A short fork that peels off the trunk and drifts downward and outward.
    private static Vec3[] buildBranch(RandomSource random, Vec3 origin) {
        int segments = 3 + random.nextInt(2);
        double dirX = (random.nextDouble() - 0.5) * 2.0;
        double dirZ = (random.nextDouble() - 0.5) * 2.0;
        Vec3[] points = new Vec3[segments + 1];
        Vec3 point = origin;
        for (int i = segments; i >= 0; i--) {
            points[i] = point;
            point = point.add(dirX + (random.nextDouble() - 0.5) * WANDER,
                    -SEGMENT_LENGTH * 0.6,
                    dirZ + (random.nextDouble() - 0.5) * WANDER);
        }
        return points;
    }

    private static void drawPath(Matrix4f matrix, VertexConsumer consumer, Vec3[] points, int layers) {
        for (int layer = 0; layer < layers; layer++) {
            float halfWidth = 0.03F + layer * 0.06F;
            for (int i = 0; i < points.length - 1; i++) {
                prism(matrix, consumer, points[i], points[i + 1], halfWidth);
            }
        }
    }

    // The four side faces of a square prism between two points, emitted in both windings so
    // they show from any angle regardless of face culling.
    private static void prism(Matrix4f matrix, VertexConsumer consumer, Vec3 bottom, Vec3 top, float w) {
        float[][] corners = {{-w, -w}, {w, -w}, {w, w}, {-w, w}};
        for (int c = 0; c < 4; c++) {
            float[] a = corners[c];
            float[] b = corners[(c + 1) % 4];
            vertex(matrix, consumer, bottom, a);
            vertex(matrix, consumer, top, a);
            vertex(matrix, consumer, top, b);
            vertex(matrix, consumer, bottom, b);

            vertex(matrix, consumer, bottom, b);
            vertex(matrix, consumer, top, b);
            vertex(matrix, consumer, top, a);
            vertex(matrix, consumer, bottom, a);
        }
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, Vec3 point, float[] corner) {
        consumer.addVertex(matrix, (float) point.x + corner[0], (float) point.y, (float) point.z + corner[1])
                .setColor(RED, GREEN, BLUE, ALPHA);
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
