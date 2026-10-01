package net.arro.paxium.entity;

import net.arro.paxium.Paxium;
import net.arro.paxium.entity.custom.PaxiumFireBurstEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Paxium.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<PaxiumFireBurstEntity>> PAXIUM_FIRE_BURST = ENTITY_TYPES.register(
            "paxium_fire_burst",
            () -> EntityType.Builder.<PaxiumFireBurstEntity>of(PaxiumFireBurstEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(4)
                    .updateInterval(10)
                    .build("paxium_fire_burst"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
