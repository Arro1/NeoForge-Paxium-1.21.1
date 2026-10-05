package net.arro.paxium.component;

import net.arro.paxium.Paxium;
import net.minecraft.core.component.DataComponentType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Paxium.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FireMeterUpgrades>> FIRE_METER_UPGRADES =
            DATA_COMPONENTS.registerComponentType("fire_meter_upgrades", builder -> builder
                    .persistent(FireMeterUpgrades.CODEC)
                    .networkSynchronized(FireMeterUpgrades.STREAM_CODEC));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
