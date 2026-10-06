package net.arro.paxium.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

// Purely visual Paxium lightning: a jagged golden bolt from `height` blocks above a base point down
// onto it, using vanilla's additive lightning render type. No entity, sound or damage involved.
// Shared by the Starforge and the primed Paxium Bomb.
public final class LightningBoltDrawer {
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

    // base is in the pose's local space (e.g. the top-center of a block, or an entity's center).
    public static void draw(PoseStack poseStack, MultiBufferSource bufferSource, long seed, Vec3 base, int height) {
        if (height <= 0) {
            return;
        }

        RandomSource random = RandomSource.create(seed);
        Vec3[] trunk = buildTrunk(random, base, height);

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();

        drawPath(matrix, consumer, trunk, LAYERS);

        for (int b = 0; b < BRANCHES && trunk.length > 4; b++) {
            int start = trunk.length / 2 + random.nextInt(trunk.length / 2 - 1);
            drawPath(matrix, consumer, buildBranch(random, trunk[start]), LAYERS - 2);
        }
    }

    // Random walk from the sky down; the wander tapers to zero near the bottom so the bolt lands
    // exactly on the base point.
    private static Vec3[] buildTrunk(RandomSource random, Vec3 base, int height) {
        int segments = Mth.clamp(Math.round(height / SEGMENT_LENGTH), 3, 24);
        Vec3[] points = new Vec3[segments + 1];
        double x = 0.0;
        double z = 0.0;
        for (int i = 0; i <= segments; i++) {
            double taper = Math.min(1.0, i / 3.0);
            points[i] = new Vec3(base.x + x * taper, base.y + (double) height * i / segments, base.z + z * taper);
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

    private LightningBoltDrawer() {
    }
}
