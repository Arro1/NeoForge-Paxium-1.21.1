package net.arro.paxium.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

// Client-only lookups, kept in their own class so common code (ModEvents) can call them from
// client-side branches without a dedicated server ever loading Minecraft client classes.
public final class PaxiumClientHelper {
    public static boolean isLocalFirstPerson(Player player) {
        Minecraft minecraft = Minecraft.getInstance();
        return player == minecraft.player && minecraft.options.getCameraType().isFirstPerson();
    }

    private PaxiumClientHelper() {
    }
}
