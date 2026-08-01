package net.livaddons.feature;

import net.minecraft.client.Minecraft;

public final class DioriteToGlass {
    private static boolean active;

    private DioriteToGlass() {}

    public static boolean active() {
        return active;
    }

    public static void tick(Minecraft client) {
        boolean next = FeatureSettings.dioriteToGlassEnabled()
                && client.level != null
                && ScoreboardUtils.sidebarContains(client, "catacombs");
        if (next != active) {
            active = next;
            if (client.level != null) client.levelRenderer.allChanged();
        }
    }
}
