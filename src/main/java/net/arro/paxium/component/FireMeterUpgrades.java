package net.arro.paxium.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

// Per-armor-piece Fire Meter upgrade levels (0..MAX_LEVEL each), applied via the upgrade smithing templates.
public record FireMeterUpgrades(int capacity, int recharge) {
    public static final int MAX_LEVEL = 3;
    public static final FireMeterUpgrades EMPTY = new FireMeterUpgrades(0, 0);

    public static final Codec<FireMeterUpgrades> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.intRange(0, MAX_LEVEL).optionalFieldOf("capacity", 0).forGetter(FireMeterUpgrades::capacity),
                    Codec.intRange(0, MAX_LEVEL).optionalFieldOf("recharge", 0).forGetter(FireMeterUpgrades::recharge)
            ).apply(instance, FireMeterUpgrades::new));

    public static final StreamCodec<ByteBuf, FireMeterUpgrades> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FireMeterUpgrades::capacity,
            ByteBufCodecs.VAR_INT, FireMeterUpgrades::recharge,
            FireMeterUpgrades::new);

    public int level(Stat stat) {
        return stat == Stat.CAPACITY ? capacity : recharge;
    }

    public FireMeterUpgrades upgraded(Stat stat) {
        return stat == Stat.CAPACITY
                ? new FireMeterUpgrades(Math.min(MAX_LEVEL, capacity + 1), recharge)
                : new FireMeterUpgrades(capacity, Math.min(MAX_LEVEL, recharge + 1));
    }

    public enum Stat {
        CAPACITY("capacity"),
        RECHARGE("recharge");

        public static final Codec<Stat> CODEC = Codec.STRING.xmap(Stat::byName, Stat::getName);

        private final String name;

        Stat(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Stat byName(String name) {
            for (Stat stat : values()) {
                if (stat.name.equals(name)) {
                    return stat;
                }
            }
            throw new IllegalArgumentException("Unknown fire meter stat: " + name);
        }
    }
}
