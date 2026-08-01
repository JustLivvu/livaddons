package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.Locale;

public final class DungeonFinishSong {
    private static final SoundEvent SOUND = SoundEvent.createVariableRangeEvent(
            Identifier.fromNamespaceAndPath("livaddons", "dungeon_finish"));
    private static long lastPlayedAt;

    private DungeonFinishSong() {
    }

    public static void onChatMessage(Component message) {
        if (!FeatureSettings.dungeonFinishSongEnabled() || message == null) return;
        String text = message.getString().toLowerCase(Locale.ROOT);
        if (!text.contains("the catacombs") || !text.contains("floor")) return;

        long now = System.currentTimeMillis();
        if (now - lastPlayedAt < 30_000L) return;
        lastPlayedAt = now;

        Minecraft client = Minecraft.getInstance();
        client.execute(() -> client.getSoundManager().play(SimpleSoundInstance.forUI(SOUND, 1.0f)));
    }
}
