package net.arro.paxium.util;

import net.arro.paxium.attachment.ModAttachmentTypes;
import net.arro.paxium.component.FireMeterUpgrades;
import net.arro.paxium.component.ModDataComponents;
import net.arro.paxium.item.ModItems;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class PaxiumArmor {
    private static final EquipmentSlot[] ARMOR_SLOTS =
            {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    public static boolean hasFullSet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.PAXIUM_HELMET.get())
                && entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.PAXIUM_CHESTPLATE.get())
                && entity.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PAXIUM_LEGGINGS.get())
                && entity.getItemBySlot(EquipmentSlot.FEET).is(ModItems.PAXIUM_BOOTS.get());
    }

    public static boolean isPaxiumArmor(ItemStack stack) {
        return stack.is(ModItems.PAXIUM_HELMET.get()) || stack.is(ModItems.PAXIUM_CHESTPLATE.get())
                || stack.is(ModItems.PAXIUM_LEGGINGS.get()) || stack.is(ModItems.PAXIUM_BOOTS.get());
    }

    public static FireMeterUpgrades getUpgrades(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FIRE_METER_UPGRADES.get(), FireMeterUpgrades.EMPTY);
    }

    private static int totalLevels(LivingEntity entity, FireMeterUpgrades.Stat stat) {
        int total = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            total += getUpgrades(entity.getItemBySlot(slot)).level(stat);
        }
        return total;
    }

    public static int getFireMeterCapacity(LivingEntity entity) {
        return ModAttachmentTypes.BASE_FIRE_METER_CAPACITY
                + totalLevels(entity, FireMeterUpgrades.Stat.CAPACITY) * ModAttachmentTypes.CAPACITY_PER_LEVEL;
    }

    public static float getFireMeterRechargeRate(LivingEntity entity) {
        return ModAttachmentTypes.BASE_RECHARGE_PER_TICK
                * (1.0F + totalLevels(entity, FireMeterUpgrades.Stat.RECHARGE) * ModAttachmentTypes.RECHARGE_BONUS_PER_LEVEL);
    }

    private PaxiumArmor() {
    }
}
