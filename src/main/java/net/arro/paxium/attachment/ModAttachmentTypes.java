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
    // 10 seconds of flight at 20 ticks/second with no capacity upgrades.
    public static final int BASE_FIRE_METER_CAPACITY = 200;
    // Per capacity level on any armor piece; a maxed set (12 levels) reaches 500.
    public static final int CAPACITY_PER_LEVEL = 25;
    // Base refill per tick, plus this fraction per recharge level; a maxed set (12 levels) reaches 2.2/tick.
    public static final float BASE_RECHARGE_PER_TICK = 1.0F;
    public static final float RECHARGE_BONUS_PER_LEVEL = 0.10F;

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Paxium.MODID);

    // Float so fractional recharge rates accumulate correctly between ticks.
    public static final Supplier<AttachmentType<Float>> FIRE_METER = ATTACHMENT_TYPES.register(
            "fire_meter",
            () -> AttachmentType.builder(() -> (float) BASE_FIRE_METER_CAPACITY)
                    .serialize(Codec.FLOAT)
                    .sync((holder, to) -> holder == to, ByteBufCodecs.FLOAT)
                    .build());

    // True while the player is flying in the Paxium armor. Synced to every tracking client (unlike the
    // Fire Meter) so other players see the flight effects; not saved, a fresh login starts grounded.
    public static final Supplier<AttachmentType<Boolean>> FLYING = ATTACHMENT_TYPES.register(
            "flying",
            () -> AttachmentType.builder(() -> false)
                    .sync(ByteBufCodecs.BOOL)
                    .build());

    public static void register(IEventBus eventBus) {
        ATTACHMENT_TYPES.register(eventBus);
    }
}
