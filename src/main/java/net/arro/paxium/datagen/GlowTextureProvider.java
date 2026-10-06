package net.arro.paxium.datagen;

import com.google.common.hash.Hashing;
import net.arro.paxium.glow.ModGlow;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.IntPredicate;

/**
 * Generates the animated "_glow" texture of every {@link ModGlow} entry: a vertical frame strip containing only the
 * vein pixels, pulsing and carrying a heat wave that drifts orange. Animated through the usual {@code .mcmeta}.
 */
public class GlowTextureProvider implements DataProvider {
    private static final int FRAMES = 24;
    private static final int FRAME_TIME_TICKS = 3;
    // Brightness of the veins relative to the base texture: the pulse swings between MIN_HEAT and 1.
    private static final float MIN_HEAT = 0.72F;

    private final PackOutput output;

    public GlowTextureProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        this.output = output;
        // Model providers validate that their textures exist; these are produced by this provider.
        for (ModGlow.Entry entry : ModGlow.all()) {
            existingFileHelper.trackGenerated(entry.glow(), PackType.CLIENT_RESOURCES, ".png", "textures");
        }
        for (ResourceLocation variant : ModGlow.cooldownVariants().values()) {
            existingFileHelper.trackGenerated(variant, PackType.CLIENT_RESOURCES, ".png", "textures");
        }
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        try {
            Path assets = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
            for (ModGlow.Entry entry : ModGlow.all()) {
                BufferedImage base = loadBase(entry);
                boolean extruded = entry.base().getPath().startsWith("item/");
                byte[] png = encode(buildStrip(base, entry.palette()::matches, extruded));

                Path pngPath = assets.resolve(entry.glow().getNamespace())
                        .resolve("textures/" + entry.glow().getPath() + ".png");
                cache.writeIfNeeded(pngPath, png, Hashing.sha1().hashBytes(png));

                byte[] meta = ("{\"animation\":{\"frametime\":" + FRAME_TIME_TICKS + ",\"interpolate\":true}}")
                        .getBytes(StandardCharsets.UTF_8);
                cache.writeIfNeeded(pngPath.resolveSibling(pngPath.getFileName() + ".mcmeta"), meta,
                        Hashing.sha1().hashBytes(meta));
            }

            for (Map.Entry<ResourceLocation, ResourceLocation> variant : ModGlow.cooldownVariants().entrySet()) {
                ModGlow.Entry entry = ModGlow.get(variant.getKey()).orElseThrow(() ->
                        new IOException("Cooldown variant without a glow entry: " + variant.getKey()));
                byte[] png = encode(greyOut(loadBase(entry), entry.palette()::matches));
                cache.writeIfNeeded(assets.resolve(variant.getValue().getNamespace())
                        .resolve("textures/" + variant.getValue().getPath() + ".png"), png, Hashing.sha1().hashBytes(png));
            }
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new UncheckedIOException(e));
        }
        return CompletableFuture.completedFuture(null);
    }

    private static BufferedImage loadBase(ModGlow.Entry entry) throws IOException {
        String resource = "/assets/" + entry.base().getNamespace() + "/textures/" + entry.base().getPath() + ".png";
        try (InputStream in = GlowTextureProvider.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("Missing base texture for glow: " + resource);
            }
            return ImageIO.read(in);
        }
    }

    /**
     * @param extruded true for items: they render as a thin 3D slab, and the glow layer's side faces on the silhouette
     *                 would coincide with the base layer's and z-fight, so vein pixels touching the silhouette are skipped.
     */
    private static BufferedImage buildStrip(BufferedImage base, IntPredicate isVein, boolean extruded) {
        int w = base.getWidth();
        int h = base.getWidth(); // textures are square; a taller image would already be an animation strip
        BufferedImage strip = new BufferedImage(w, h * FRAMES, BufferedImage.TYPE_INT_ARGB);

        for (int frame = 0; frame < FRAMES; frame++) {
            double phase = 2 * Math.PI * frame / FRAMES;
            double pulse = 0.5 + 0.5 * Math.sin(phase);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = base.getRGB(x, y);
                    if ((argb >>> 24) == 0 || !isVein.test(argb) || (extruded && touchesSilhouette(base, x, y))) {
                        continue;
                    }
                    // Heat wave drifting along the texture; loops seamlessly because only time wraps.
                    double wave = Math.max(0, Math.sin(phase - y * 0.4 - x * 0.15));
                    wave = wave * wave * wave;
                    double heat = MIN_HEAT + (1 - MIN_HEAT) * (0.45 * pulse + 0.55 * wave);

                    double lum = ((argb >> 16) & 0xFF) / 255.0;
                    int r = clamp(((argb >> 16) & 0xFF) * heat);
                    int g = clamp(((argb >> 8) & 0xFF) * heat + wave * 55 * lum * lum);
                    int b = clamp((argb & 0xFF) * heat + wave * wave * 8 * lum * lum);
                    strip.setRGB(x, frame * h + y, 0xFF000000 | r << 16 | g << 8 | b);
                }
            }
        }
        return strip;
    }

    /** Replaces vein pixels with a dark, near-black grey that keeps their relative shading, so the item looks dormant. */
    private static BufferedImage greyOut(BufferedImage base, IntPredicate isVein) {
        BufferedImage out = new BufferedImage(base.getWidth(), base.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < base.getHeight(); y++) {
            for (int x = 0; x < base.getWidth(); x++) {
                int argb = base.getRGB(x, y);
                if ((argb >>> 24) != 0 && isVein.test(argb)) {
                    double lum = 0.3 * ((argb >> 16) & 0xFF) + 0.59 * ((argb >> 8) & 0xFF) + 0.11 * (argb & 0xFF);
                    int grey = clamp(lum * 0.18 + 8);
                    argb = (argb & 0xFF000000) | grey << 16 | grey << 8 | Math.min(255, grey + 4);
                }
                out.setRGB(x, y, argb);
            }
        }
        return out;
    }

    private static boolean touchesSilhouette(BufferedImage image, int x, int y) {
        return isEmpty(image, x - 1, y) || isEmpty(image, x + 1, y) || isEmpty(image, x, y - 1) || isEmpty(image, x, y + 1);
    }

    private static boolean isEmpty(BufferedImage image, int x, int y) {
        return x < 0 || y < 0 || x >= image.getWidth() || y >= image.getHeight() || (image.getRGB(x, y) >>> 24) == 0;
    }

    private static int clamp(double value) {
        return Math.max(0, Math.min(255, (int) Math.round(value)));
    }

    private static byte[] encode(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    @Override
    public String getName() {
        return "Paxium glow textures";
    }
}
