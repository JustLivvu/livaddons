package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
public final class MelodyAlert {
    private static Screen lastMelodyScreen;
    private static long lastAlertAt;

    private MelodyAlert() {
    }

    public static void onScreenOpened(Minecraft client, Screen screen) {
        if (!FeatureSettings.melodyAlertEnabled()) return;
        if (!screen.getTitle().getString().trim().equalsIgnoreCase("Click the button on time!")) return;
        long now = System.currentTimeMillis();
        if (screen == lastMelodyScreen || now - lastAlertAt < 5000) return;
        lastMelodyScreen = screen;
        lastAlertAt = now;
        if (client.getConnection() != null) {
            client.getConnection().sendCommand(
                    "pc " + FeatureSettings.melodyAlertMessage());
        }
    }
}
