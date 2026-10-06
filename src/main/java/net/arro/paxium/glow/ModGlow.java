package net.arro.paxium.glow;

import net.arro.paxium.Paxium;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Central list of every texture that gets glowing, animated veins.
 *
 * <p>For each entry, data generation ({@code ./gradlew runData}) creates an animated "_glow" texture (only the vein
 * pixels, pulsing with a heat wave) and the models add it as a fullbright layer on top of the base texture.
 * To make a new item or block glow:
 * <ol>
 *     <li>add a line below ({@link #item}, {@link #block} or {@link #texture});</li>
 *     <li>build its model through the helpers in {@code ModItemModelProvider} / {@code ModBlockStateProvider}
 *     ({@code glowItem}, {@code glowCube}) - they pick the glow up automatically;</li>
 *     <li>run {@code runData}.</li>
 * </ol>
 * Armor worn on the body is a separate system, see {@code PaxiumArmorGlowLayer}.
 */
public final class ModGlow {
    /** A glowing texture: the base texture, its generated glow texture, and which pixels glow. */
    public record Entry(ResourceLocation base, ResourceLocation glow, GlowPalette palette) {
    }

    /** Item model predicate: 1 while the holder has the item on cooldown. Registered in the client setup. */
    public static final ResourceLocation ON_COOLDOWN = ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "on_cooldown");

    private static final Map<ResourceLocation, Entry> ENTRIES = new LinkedHashMap<>();
    // base texture -> "dormant" variant with grey, unlit veins (shown while the item is on cooldown)
    private static final Map<ResourceLocation, ResourceLocation> COOLDOWN_VARIANTS = new LinkedHashMap<>();

    static {
        // Netherite items with the standard red veins
        item("paxium");
        item("raw_paxium");
        item("paxium_helmet");
        item("paxium_chestplate");
        item("paxium_leggings");
        item("paxium_boots");
        item("paxium_sword");
        item("paxium_bow");
        item("paxium_bow_pulling_0");
        item("paxium_bow_pulling_1");
        item("paxium_bow_pulling_2");
        item("paxium_capacity_upgrade_smithing_template");
        item("paxium_recharge_upgrade_smithing_template");
        item("paxium_beam_damage_upgrade_smithing_template");
        item("paxium_blast_upgrade_smithing_template");

        // Items with their own shades of red
        item("paxium_infused_crystal", GlowPalette.of(0xE20627, 0xCE0624, 0xB70520));
        item("unstable_paxium_charge", GlowPalette.of(0xE20627, 0xCE0624, 0xB70520));
        item("refined_paxium_charge", GlowPalette.of(0xFF2A2A, 0xFF4040, 0xD01010));

        // Items whose veins go dark while on cooldown
        cooldownItem("paxium_sword");
        cooldownItem("paxium_bow");

        // Blocks
        block("deepslate_paxium_ore");
        block("paxium_bomb_side", GlowPalette.of(0xB30000, 0xFF3B2A));
        block("paxium_bomb_top", GlowPalette.of(0xFF4C4C));
    }

    public static void item(String name) {
        texture("item/" + name, GlowPalette.VEINS);
    }

    public static void item(String name, GlowPalette palette) {
        texture("item/" + name, palette);
    }

    public static void block(String name) {
        texture("block/" + name, GlowPalette.VEINS);
    }

    public static void block(String name, GlowPalette palette) {
        texture("block/" + name, palette);
    }

    /** @param path texture path without extension, e.g. {@code item/paxium_sword} */
    public static void texture(String path, GlowPalette palette) {
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath(Paxium.MODID, path);
        ResourceLocation glow = ResourceLocation.fromNamespaceAndPath(Paxium.MODID, path + "_glow");
        ENTRIES.put(base, new Entry(base, glow, palette));
    }

    /**
     * Also generates a grey "name_cooldown" texture (veins greyed out, no glow). The item model still has to switch
     * to it, see {@code ModItemModelProvider#cooldownOverride} and the {@link #ON_COOLDOWN} item property.
     */
    public static void cooldownItem(String name) {
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "item/" + name);
        COOLDOWN_VARIANTS.put(base, ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "item/" + name + "_cooldown"));
    }

    public static Optional<ResourceLocation> getCooldownVariant(ResourceLocation baseTexture) {
        return Optional.ofNullable(COOLDOWN_VARIANTS.get(baseTexture));
    }

    public static Map<ResourceLocation, ResourceLocation> cooldownVariants() {
        return COOLDOWN_VARIANTS;
    }

    public static Optional<Entry> get(ResourceLocation baseTexture) {
        return Optional.ofNullable(ENTRIES.get(baseTexture));
    }

    public static Collection<Entry> all() {
        return ENTRIES.values();
    }

    private ModGlow() {
    }
}
