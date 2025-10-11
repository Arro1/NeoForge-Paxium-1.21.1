package net.arro.paxium.item;

import net.arro.paxium.Paxium;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Paxium.MODID);

    public static final DeferredItem<Item> RAW_PAXIUM = ITEMS.register("raw_paxium",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> PAXIUM = ITEMS.register("paxium",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
