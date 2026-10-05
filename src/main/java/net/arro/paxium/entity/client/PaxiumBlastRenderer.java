package net.arro.paxium.entity.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.arro.paxium.entity.custom.PaxiumBlastEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

// The Paxium Bomb's rendered explosion, drawn additively (vanilla lightning render type):
//  - a churning fireball of three nested, wobbling shells (white-hot core -> orange -> deep red)
//    that blooms out, rises and fades over ~1.5 s
//  - a shockwave ring racing outward along the ground past the crater edge
// The fire column, smoke cap and embers are particles spawned in PaxiumBlastEntity#clientTick.
public class PaxiumBlastRenderer extends EntityRenderer<PaxiumBlastEntity> {
    private static final int FIREBALL_TICKS = 30;
    private static final int SHOCKWAVE_TICKS = 40;
    private static final int LAT_STEPS = 16;
    private static final int LON_STEPS = 24;
    private static final int RING_SEGMENTS = 64;
    // The bomb detonates ~3 blocks above where it was placed; the ring hugs that ground level.
    private static final float GROUND_OFFSET = -3.0F;

    // {radius factor, r, g, b, alpha factor}, inner to outer.
    private static final float[][] SHELLS = {
            {0.55F, 1.00F, 0.95F, 0.75F, 1.0F},
            {0.80F, 1.00F, 0.60F, 0.15F, 0.8F},
            {1.00F, 0.90F, 0.25F, 0.05F, 0.6F},
    };

    public PaxiumBlastRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(PaxiumBlastEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        float t = entity.getVisualAge(partialTick);
        if (t > Math.max(FIREBALL_TICKS, SHOCKWAVE_TICKS)) {
            return;
        }

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        Matrix4f matrix = poseStack.last().pose();
        float phase = (entity.getBlastSeed() & 0xFFFF) / 6553.6F;

        if (t < FIREBALL_TICKS) {
            float grow = 1.0F - Mth.square(1.0F - Math.min(1.0F, t / 12.0F));
            float radius = PaxiumBlastEntity.RADIUS * 0.7F * grow;
            float fade = Mth.square(1.0F - t / FIREBALL_TICKS);
            float rise = Math.max(0.0F, t - 10.0F) * 0.35F;
            for (float[] shell : SHELLS) {
                drawShell(matrix, consumer, radius * shell[0], rise, t, phase,
                        shell[1], shell[2], shell[3], 0.55F * fade * shell[4]);
            }
        }

        if (t < SHOCKWAVE_TICKS) {
            float progress = t / SHOCKWAVE_TICKS;
            float radius = PaxiumBlastEntity.RADIUS * 1.6F * (1.0F - Mth.square(1.0F - progress));
            drawShockwave(matrix, consumer, radius, 0.5F * (1.0F - progress));
        }
    }

    // A UV sphere whose radius is perturbed by drifting sine waves, so it reads as churning fire
    // rather than a perfect ball.
    private static void drawShell(Matrix4f matrix, VertexConsumer consumer, float radius, float rise, float t, float phase,
                                  float r, float g, float b, float a) {
        if (radius <= 0.01F || a <= 0.0F) {
            return;
        }
        for (int i = 0; i < LAT_STEPS; i++) {
            float lat0 = Mth.PI * i / LAT_STEPS;
            float lat1 = Mth.PI * (i + 1) / LAT_STEPS;
            for (int j = 0; j < LON_STEPS; j++) {
                float lon0 = Mth.TWO_PI * j / LON_STEPS;
                float lon1 = Mth.TWO_PI * (j + 1) / LON_STEPS;
                float[] v00 = spherePoint(radius, lat0, lon0, rise, t, phase);
                float[] v01 = spherePoint(radius, lat0, lon1, rise, t, phase);
                float[] v11 = spherePoint(radius, lat1, lon1, rise, t, phase);
                float[] v10 = spherePoint(radius, lat1, lon0, rise, t, phase);
                quad(matrix, consumer, v00, v01, v11, v10, r, g, b, a);
            }
        }
    }

    private static float[] spherePoint(float radius, float lat, float lon, float rise, float t, float phase) {
        float wobble = 1.0F
                + 0.12F * Mth.sin(3.0F * lat + t * 0.30F + phase) * Mth.cos(2.0F * lon - t * 0.20F)
                + 0.06F * Mth.sin(5.0F * lon + 2.0F * lat + phase);
        float rr = radius * wobble;
        float sinLat = Mth.sin(lat);
        return new float[]{rr * sinLat * Mth.cos(lon), rr * Mth.cos(lat) + rise, rr * sinLat * Mth.sin(lon)};
    }

    // A flat glowing annulus on the ground plus a short vertical band at its leading edge.
    private static void drawShockwave(Matrix4f matrix, VertexConsumer consumer, float radius, float a) {
        float inner = Math.max(0.0F, radius - 4.0F);
        float outer = radius + 1.0F;
        float y = GROUND_OFFSET + 0.3F;
        for (int s = 0; s < RING_SEGMENTS; s++) {
            float a0 = Mth.TWO_PI * s / RING_SEGMENTS;
            float a1 = Mth.TWO_PI * (s + 1) / RING_SEGMENTS;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);

            quad(matrix, consumer,
                    new float[]{inner * c0, y, inner * s0}, new float[]{outer * c0, y, outer * s0},
                    new float[]{outer * c1, y, outer * s1}, new float[]{inner * c1, y, inner * s1},
                    1.0F, 0.75F, 0.40F, a);
            quad(matrix, consumer,
                    new float[]{radius * c0, y, radius * s0}, new float[]{radius * c0, y + 3.0F, radius * s0},
                    new float[]{radius * c1, y + 3.0F, radius * s1}, new float[]{radius * c1, y, radius * s1},
                    1.0F, 0.85F, 0.55F, a * 0.7F);
        }
    }

    // Emitted in both windings so it shows from inside and outside regardless of culling.
    private static void quad(Matrix4f matrix, VertexConsumer consumer, float[] p0, float[] p1, float[] p2, float[] p3,
                             float r, float g, float b, float a) {
        vertex(matrix, consumer, p0, r, g, b, a);
        vertex(matrix, consumer, p1, r, g, b, a);
        vertex(matrix, consumer, p2, r, g, b, a);
        vertex(matrix, consumer, p3, r, g, b, a);

        vertex(matrix, consumer, p3, r, g, b, a);
        vertex(matrix, consumer, p2, r, g, b, a);
        vertex(matrix, consumer, p1, r, g, b, a);
        vertex(matrix, consumer, p0, r, g, b, a);
    }

    private static void vertex(Matrix4f matrix, VertexConsumer consumer, float[] p, float r, float g, float b, float a) {
        consumer.addVertex(matrix, p[0], p[1], p[2]).setColor(r, g, b, a);
    }

    // Far larger than the entity's (tiny) box - always render while the effect is alive.
    @Override
    public boolean shouldRender(PaxiumBlastEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(PaxiumBlastEntity entity) {
        return TextureManager.INTENTIONAL_MISSING_TEXTURE;
    }
}
