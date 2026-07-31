package net.livaddons.feature;

import net.minecraft.client.Minecraft;

public final class LavaToWater {
    private static boolean lastActive;

    private LavaToWater() {
    }

    public static boolean active() {
        return lastActive;
    }

    public static void tick(Minecraft client) {
        boolean active = FeatureSettings.lavaToWaterEnabled()
                && client.level != null
                && ScoreboardUtils.sidebarContains(client, "catacombs");
        if (active != lastActive) {
            lastActive = active;
            if (client.level != null) client.levelRenderer.allChanged();
        }
    }
}
