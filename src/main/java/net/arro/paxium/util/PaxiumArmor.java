package net.arro.paxium.util;

import net.arro.paxium.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

public class PaxiumArmor {
    public static boolean hasFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.PAXIUM_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.PAXIUM_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PAXIUM_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.PAXIUM_BOOTS.get());
    }

    private PaxiumArmor() {
    }
}
