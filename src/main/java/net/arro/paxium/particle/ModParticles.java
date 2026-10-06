package net.arro.paxium.particle;

import net.arro.paxium.Paxium;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, Paxium.MODID);

    // Short-lived, fullbright flame used for the flight exhaust at the feet.
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> FLIGHT_FLAME =
            PARTICLE_TYPES.register("flight_flame", () -> new SimpleParticleType(false));

    public static void register(IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}
