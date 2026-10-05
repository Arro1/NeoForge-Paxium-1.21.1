package net.arro.paxium.item;

import net.arro.paxium.Paxium;
import net.arro.paxium.item.custom.PaxiumBowItem;
import net.arro.paxium.item.custom.PaxiumSwordItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Paxium.MODID);

    public static final DeferredItem<Item> RAW_PAXIUM = ITEMS.register("raw_paxium",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PAXIUM = ITEMS.register("paxium",
            () -> new Item(new Item.Properties()));

    public static final DeferredItem<ArmorItem> PAXIUM_HELMET = ITEMS.register("paxium_helmet",
            () -> new ArmorItem(ModArmorMaterials.PAXIUM, ArmorItem.Type.HELMET,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC)
                            .durability(ArmorItem.Type.HELMET.getDurability(50))));
    public static final DeferredItem<ArmorItem> PAXIUM_CHESTPLATE = ITEMS.register("paxium_chestplate",
            () -> new ArmorItem(ModArmorMaterials.PAXIUM, ArmorItem.Type.CHESTPLATE,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC)
                            .durability(ArmorItem.Type.CHESTPLATE.getDurability(50))));
    public static final DeferredItem<ArmorItem> PAXIUM_LEGGINGS = ITEMS.register("paxium_leggings",
            () -> new ArmorItem(ModArmorMaterials.PAXIUM, ArmorItem.Type.LEGGINGS,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC)
                            .durability(ArmorItem.Type.LEGGINGS.getDurability(50))));
    public static final DeferredItem<ArmorItem> PAXIUM_BOOTS = ITEMS.register("paxium_boots",
            () -> new ArmorItem(ModArmorMaterials.PAXIUM, ArmorItem.Type.BOOTS,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC)
                            .durability(ArmorItem.Type.BOOTS.getDurability(50))));

    // Total attack damage = 1 (base) + 5 (this bonus) + 4 (netherite tier bonus) = 10.
    public static final DeferredItem<PaxiumSwordItem> PAXIUM_SWORD = ITEMS.register("paxium_sword",
            () -> new PaxiumSwordItem(Tiers.NETHERITE, new Item.Properties().fireResistant().rarity(Rarity.EPIC)
                    .attributes(SwordItem.createAttributes(Tiers.NETHERITE, 5, -2.4F))));

    public static final DeferredItem<PaxiumBowItem> PAXIUM_BOW = ITEMS.register("paxium_bow",
            () -> new PaxiumBowItem(new Item.Properties().fireResistant().rarity(Rarity.EPIC)));

    // Applied in the smithing table (template + Paxium armor piece + Paxium) to raise one Fire Meter
    // stat on that piece by one level, up to III - see PaxiumUpgradeSmithingRecipe.
    public static final DeferredItem<SmithingTemplateItem> PAXIUM_CAPACITY_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            "paxium_capacity_upgrade_smithing_template", () -> fireMeterUpgradeTemplate("capacity"));
    public static final DeferredItem<SmithingTemplateItem> PAXIUM_RECHARGE_UPGRADE_SMITHING_TEMPLATE = ITEMS.register(
            "paxium_recharge_upgrade_smithing_template", () -> fireMeterUpgradeTemplate("recharge"));

    private static SmithingTemplateItem fireMeterUpgradeTemplate(String stat) {
        return new SmithingTemplateItem(
                Component.translatable("item.paxium.smithing_template.fire_meter_upgrade.applies_to")
                        .withStyle(ChatFormatting.BLUE),
                Component.translatable("item.paxium.smithing_template.fire_meter_upgrade.ingredients")
                        .withStyle(ChatFormatting.BLUE),
                Component.translatable("upgrade.paxium." + stat + "_upgrade").withStyle(ChatFormatting.GRAY),
                Component.translatable("item.paxium.smithing_template.fire_meter_upgrade.base_slot_description"),
                Component.translatable("item.paxium.smithing_template.fire_meter_upgrade.additions_slot_description"),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_armor_slot_helmet"),
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_chestplate"),
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_leggings"),
                        ResourceLocation.withDefaultNamespace("item/empty_armor_slot_boots")),
                List.of(ResourceLocation.withDefaultNamespace("item/empty_slot_ingot")));
    }

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
