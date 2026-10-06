package net.arro.paxium.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// Per-armor-piece Fire Meter upgrade levels (0..MAX_LEVEL each), applied via the upgrade smithing
// templates. Read and written through PaxiumUpgradeStat.CAPACITY / RECHARGE.
public record FireMeterUpgrades(int capacity, int recharge) {
    public static final FireMeterUpgrades EMPTY = new FireMeterUpgrades(0, 0);

    public static final Codec<FireMeterUpgrades> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(0, PaxiumUpgradeStat.MAX_LEVEL).optionalFieldOf("capacity", 0).forGetter(FireMeterUpgrades::capacity),
                    Codec.intRange(0, PaxiumUpgradeStat.MAX_LEVEL).optionalFieldOf("recharge", 0).forGetter(FireMeterUpgrades::recharge)
            ).apply(instance, FireMeterUpgrades::new));

    public static final StreamCodec<ByteBuf, FireMeterUpgrades> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FireMeterUpgrades::capacity,
            ByteBufCodecs.VAR_INT, FireMeterUpgrades::recharge,
            FireMeterUpgrades::new);
}
