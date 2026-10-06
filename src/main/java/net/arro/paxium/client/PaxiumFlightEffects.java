package net.arro.paxium.client;

import net.arro.paxium.attachment.ModAttachmentTypes;
import net.arro.paxium.particle.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Flame exhaust blowing out of the player's feet while flying in the Paxium armor.
 * Called every client tick for every player; does nothing unless the player is flying.
 */
public final class PaxiumFlightEffects {
    private static final double FOOT_SPACING = 0.14;

    public static void emit(Player player) {
        if (!player.getData(ModAttachmentTypes.FLYING.get())) {
            return;
        }

        Level level = player.level();
        RandomSource random = level.random;
        Vec3 motion = player.getDeltaMovement();

        double yaw = Math.toRadians(player.yBodyRot);
        Vec3 right = new Vec3(-Math.cos(yaw), 0.0, -Math.sin(yaw));

        // Two feet, a couple of flames each per tick.
        for (int side = -1; side <= 1; side += 2) {
            Vec3 foot = player.position().add(right.scale(FOOT_SPACING * side));
            for (int i = 0; i < 2; i++) {
                level.addParticle(ModParticles.FLIGHT_FLAME.get(),
                        foot.x + (random.nextDouble() - 0.5) * 0.1,
                        foot.y + 0.05 + random.nextDouble() * 0.1,
                        foot.z + (random.nextDouble() - 0.5) * 0.1,
                        // Blown downward, and pushed back against the direction of travel like real thrust.
                        -motion.x * 0.3 + (random.nextDouble() - 0.5) * 0.04,
                        -0.12 - random.nextDouble() * 0.08,
                        -motion.z * 0.3 + (random.nextDouble() - 0.5) * 0.04);
            }
        }

        if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SMOKE,
                    player.getX() + (random.nextDouble() - 0.5) * 0.3,
                    player.getY() + 0.1,
                    player.getZ() + (random.nextDouble() - 0.5) * 0.3,
                    0.0, -0.05, 0.0);
        }
    }

    private PaxiumFlightEffects() {
    }
}
