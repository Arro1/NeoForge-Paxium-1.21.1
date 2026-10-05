package net.arro.paxium.component;

import com.mojang.serialization.Codec;
import net.minecraft.world.item.ItemStack;

// Every stat a Paxium upgrade template can raise, levels 0..MAX_LEVEL. Armor stats live in the
// FIRE_METER_UPGRADES component; each weapon has exactly one stat, stored in WEAPON_UPGRADE_LEVEL.
public enum PaxiumUpgradeStat {
    CAPACITY("capacity") {
        @Override
        public int getLevel(ItemStack stack) {
            return fireMeter(stack).capacity();
        }

        @Override
        public void setLevel(ItemStack stack, int level) {
            stack.set(ModDataComponents.FIRE_METER_UPGRADES.get(), new FireMeterUpgrades(level, fireMeter(stack).recharge()));
        }
    },
    RECHARGE("recharge") {
        @Override
        public int getLevel(ItemStack stack) {
            return fireMeter(stack).recharge();
        }

        @Override
        public void setLevel(ItemStack stack, int level) {
            stack.set(ModDataComponents.FIRE_METER_UPGRADES.get(), new FireMeterUpgrades(fireMeter(stack).capacity(), level));
        }
    },
    // Paxium Sword: fire beam damage.
    BEAM_DAMAGE("beam_damage") {
        @Override
        public int getLevel(ItemStack stack) {
            return weaponLevel(stack);
        }

        @Override
        public void setLevel(ItemStack stack, int level) {
            stack.set(ModDataComponents.WEAPON_UPGRADE_LEVEL.get(), level);
        }
    },
    // Paxium Bow: fire burst damage and blast radius.
    BLAST("blast") {
        @Override
        public int getLevel(ItemStack stack) {
            return weaponLevel(stack);
        }

        @Override
        public void setLevel(ItemStack stack, int level) {
            stack.set(ModDataComponents.WEAPON_UPGRADE_LEVEL.get(), level);
        }
    };

    public static final int MAX_LEVEL = 3;

    public static final Codec<PaxiumUpgradeStat> CODEC = Codec.STRING.xmap(PaxiumUpgradeStat::byName, PaxiumUpgradeStat::getName);

    private final String name;

    PaxiumUpgradeStat(String name) {
        this.name = name;
    }

    public abstract int getLevel(ItemStack stack);

    protected abstract void setLevel(ItemStack stack, int level);

    // Copy of the stack with this stat one level higher (capped); all other components carry over.
    public ItemStack upgraded(ItemStack stack) {
        ItemStack result = stack.copyWithCount(1);
        setLevel(result, Math.min(MAX_LEVEL, getLevel(stack) + 1));
        return result;
    }

    public String getName() {
        return name;
    }

    public static PaxiumUpgradeStat byName(String name) {
        for (PaxiumUpgradeStat stat : values()) {
            if (stat.name.equals(name)) {
                return stat;
            }
        }
        throw new IllegalArgumentException("Unknown Paxium upgrade stat: " + name);
    }

    private static FireMeterUpgrades fireMeter(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FIRE_METER_UPGRADES.get(), FireMeterUpgrades.EMPTY);
    }

    public static int weaponLevel(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.WEAPON_UPGRADE_LEVEL.get(), 0);
    }
}
