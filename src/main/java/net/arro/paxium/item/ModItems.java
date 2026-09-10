package net.arro.paxium.item;

import net.arro.paxium.Paxium;
import net.arro.paxium.item.custom.PaxiumSwordItem;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

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

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
