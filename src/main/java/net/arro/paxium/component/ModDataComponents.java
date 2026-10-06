package net.arro.paxium.component;

import com.mojang.serialization.Codec;
import net.arro.paxium.Paxium;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Paxium.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FireMeterUpgrades>> FIRE_METER_UPGRADES =
            DATA_COMPONENTS.registerComponentType("fire_meter_upgrades", builder -> builder
                    .persistent(FireMeterUpgrades.CODEC)
                    .networkSynchronized(FireMeterUpgrades.STREAM_CODEC));

    // Single upgrade level on a Paxium weapon: Beam Damage on the sword, Blast on the bow.
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> WEAPON_UPGRADE_LEVEL =
            DATA_COMPONENTS.registerComponentType("weapon_upgrade_level", builder -> builder
                    .persistent(Codec.intRange(0, PaxiumUpgradeStat.MAX_LEVEL))
                    .networkSynchronized(ByteBufCodecs.VAR_INT));

    public static void register(IEventBus eventBus) {
        DATA_COMPONENTS.register(eventBus);
    }
}
