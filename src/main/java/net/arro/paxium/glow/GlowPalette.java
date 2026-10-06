package net.arro.paxium.glow;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

/**
 * The exact RGB colours of a texture that count as glowing "veins". Everything else stays untouched.
 */
public final class GlowPalette {
    /** The red fire veins found on the netherite-style Paxium textures. */
    public static final GlowPalette VEINS = of(0xFF0000, 0x991515, 0xFF5E5E, 0xAA0000, 0xCC0E0E, 0xBD0707, 0xFF2D2D);

    private final IntSet colors;

    private GlowPalette(IntSet colors) {
        this.colors = colors;
    }

    public static GlowPalette of(int... rgbColors) {
        return new GlowPalette(new IntOpenHashSet(rgbColors));
    }

    public boolean matches(int rgb) {
        return colors.contains(rgb & 0xFFFFFF);
    }
}
