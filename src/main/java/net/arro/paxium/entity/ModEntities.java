package net.arro.paxium.entity;

import net.arro.paxium.Paxium;
import net.arro.paxium.entity.custom.PaxiumBlastEntity;
import net.arro.paxium.entity.custom.PaxiumFireBurstEntity;
import net.arro.paxium.entity.custom.PrimedPaxiumBombEntity;
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

    // The armed, floating, charging Paxium Bomb.
    public static final DeferredHolder<EntityType<?>, EntityType<PrimedPaxiumBombEntity>> PRIMED_PAXIUM_BOMB = ENTITY_TYPES.register(
            "primed_paxium_bomb",
            () -> EntityType.Builder.<PrimedPaxiumBombEntity>of(PrimedPaxiumBombEntity::new, MobCategory.MISC)
                    .sized(0.98F, 0.98F)
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(2)
                    .build("primed_paxium_bomb"));

    // Invisible controller that runs the detonation (crater, volcano) and carries the explosion visuals.
    public static final DeferredHolder<EntityType<?>, EntityType<PaxiumBlastEntity>> PAXIUM_BLAST = ENTITY_TYPES.register(
            "paxium_blast",
            () -> EntityType.Builder.<PaxiumBlastEntity>of(PaxiumBlastEntity::new, MobCategory.MISC)
                    .sized(0.1F, 0.1F)
                    .fireImmune()
                    .noSummon()
                    .clientTrackingRange(16)
                    .updateInterval(20)
                    .build("paxium_blast"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
