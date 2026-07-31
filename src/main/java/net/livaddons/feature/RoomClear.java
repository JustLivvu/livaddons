package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public final class RoomClear {
    private RoomClear() {}

    static void onCheckmark(boolean green) {
        if (!FeatureSettings.roomClearEnabled()) return;
        int mode = FeatureSettings.roomClearMode();
        if ((green && mode == 2) || (!green && mode == 1)) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !ScoreboardUtils.sidebarContains(client, "catacombs")) return;
        client.gui.setTimes(0, 20, 5);
        client.gui.setTitle(Component.literal(green ? "§aRoom Complete!" : "Room Cleared!"));
        client.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 1.0f);
    }
}
