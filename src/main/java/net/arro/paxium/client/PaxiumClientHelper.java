package net.arro.paxium.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

// Client-only lookups and effects, kept in their own class so common code (ModEvents, entities) can
// call them from client-side branches without a dedicated server ever loading client classes.
public final class PaxiumClientHelper {
    private static final long SHAKE_DURATION_MS = 2500L;
    private static final float MAX_SHAKE_DEGREES = 6.0F;

    private static long shakeStartMs = 0L;
    private static float shakeStrength = 0.0F;

    public static boolean isLocalFirstPerson(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
    }

    // Paxium Bomb impact: shake the camera, stronger the closer the local player is.
    public static void startBlastShake(Vec3 center, double range) {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float strength = (float) Math.max(0.0, 1.0 - player.position().distanceTo(center) / range);
        if (strength > 0.0F) {
            shakeStartMs = Util.getMillis();
            shakeStrength = Math.max(shakeStrength * currentDecay(), strength);
        }
    }

    private static float currentDecay() {
        float elapsed = (Util.getMillis() - shakeStartMs) / (float) SHAKE_DURATION_MS;
        return Math.max(0.0F, 1.0F - elapsed);
    }

    // Registered on the game bus (client only). Jitters pitch/yaw/roll, fading out over SHAKE_DURATION_MS.
    @SubscribeEvent
    public static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float decay = currentDecay();
        if (shakeStrength <= 0.0F || decay <= 0.0F) {
            return;
        }
        float amount = MAX_SHAKE_DEGREES * shakeStrength * decay * decay;
        double time = Util.getMillis() / 1000.0;
        event.setPitch(event.getPitch() + amount * (float) Math.sin(time * 47.0));
        event.setYaw(event.getYaw() + amount * 0.6F * (float) Math.sin(time * 39.0 + 1.3));
        event.setRoll(event.getRoll() + amount * 0.8F * (float) Math.sin(time * 53.0 + 2.1));
    }

    private PaxiumClientHelper() {
    }
}
