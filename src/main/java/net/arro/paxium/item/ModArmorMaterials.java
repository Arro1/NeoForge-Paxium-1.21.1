package net.arro.paxium.item;

import net.arro.paxium.Paxium;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

public class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Paxium.MODID);

    // Total defense across a full set matches netherite (20 armor points).
    // Toughness (5.0/piece) and knockback resistance (0.15/piece) each sum to
    // 20 and 0.6 (60%) respectively across the 4 humanoid pieces - a clear step above netherite's 12 toughness / 40% resistance.
    public static final Holder<ArmorMaterial> PAXIUM = ARMOR_MATERIALS.register("paxium", () -> {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        defense.put(ArmorItem.Type.BOOTS, 3);
        defense.put(ArmorItem.Type.LEGGINGS, 6);
        defense.put(ArmorItem.Type.CHESTPLATE, 8);
        defense.put(ArmorItem.Type.HELMET, 3);
        defense.put(ArmorItem.Type.BODY, 11);

        return new ArmorMaterial(
                defense,
                18,
                SoundEvents.ARMOR_EQUIP_NETHERITE,
                () -> Ingredient.of(ModItems.PAXIUM.get()),
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Paxium.MODID, "paxium"))),
                5.0F,
                0.15F
        );
    });

    public static void register(IEventBus eventBus) {
        ARMOR_MATERIALS.register(eventBus);
    }
}
