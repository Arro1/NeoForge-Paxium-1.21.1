package net.arro.paxium.attachment;

import com.mojang.serialization.Codec;
import net.arro.paxium.Paxium;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachmentTypes {
    // 10 seconds of flight at 20 ticks/second.
    public static final int MAX_FLIGHT_FUEL_TICKS = 200;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Paxium.MODID);

    public static final Supplier<AttachmentType<Integer>> FLIGHT_FUEL_TICKS = ATTACHMENT_TYPES.register(
            "flight_fuel_ticks",
            () -> AttachmentType.builder(() -> MAX_FLIGHT_FUEL_TICKS)
                    .serialize(Codec.INT)
                    .sync((holder, to) -> holder == to, ByteBufCodecs.VAR_INT)
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
