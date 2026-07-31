package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LeapAlert {
    private static final Pattern TELEPORT = Pattern.compile(
            "^You have teleported to ([A-Za-z0-9_]{1,16})!$", Pattern.CASE_INSENSITIVE);
    private static final Pattern DUNGEON_CLASS = Pattern.compile(
            "\\((Healer|Archer|Tank|Mage|Berserk(?:er)?)(?:\\s+[IVXLCDM]+)?\\)",
            Pattern.CASE_INSENSITIVE);
    private static Pending pending;
    private static long tick;
    private static String lastPlayer = "";
    private static long lastAlertAt;

    private LeapAlert() {
    }

    public static void onChatMessage(Component message) {
        if (!FeatureSettings.leapAlertEnabled() || message == null) return;
        Matcher matcher = TELEPORT.matcher(message.getString().trim());
        if (!matcher.matches()) return;
        String username = matcher.group(1);
        long now = System.currentTimeMillis();
        if (username.equalsIgnoreCase(lastPlayer) && now - lastAlertAt < 3000L) return;
        lastPlayer = username;
        lastAlertAt = now;
        pending = new Pending(username, tick + 1, 20);
    }

    public static void tick(Minecraft client) {
        tick++;
        if (pending == null || tick < pending.nextAttempt) return;
        String dungeonClass = findClass(client, pending.username);
        if (dungeonClass != null) {
            sendAlert(client, pending.username, dungeonClass);
            pending = null;
            return;
        }
        if (pending.attemptsLeft <= 1) {
            sendAlert(client, pending.username, "Unknown");
            pending = null;
        } else {
            pending = new Pending(pending.username, tick + 2, pending.attemptsLeft - 1);
        }
    }

    private static String findClass(Minecraft client, String username) {
        if (client.getConnection() == null) return null;
        for (PlayerInfo info : client.getConnection().getOnlinePlayers()) {
            Component display = info.getTabListDisplayName();
            String line = display != null ? display.getString()
                    : info.getProfile() != null && info.getProfile().name() != null
                    ? info.getProfile().name() : "";
            if (!line.toLowerCase(Locale.ROOT).contains(username.toLowerCase(Locale.ROOT))) continue;
            Matcher matcher = DUNGEON_CLASS.matcher(line);
            if (!matcher.find()) continue;
            String value = matcher.group(1).toLowerCase(Locale.ROOT);
            if (value.startsWith("berserk")) return "Berserker";
            return Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }
        return null;
    }

    private static void sendAlert(Minecraft client, String username, String dungeonClass) {
        if (client.player != null && client.player.connection != null) {
            client.player.connection.sendCommand("pc Leaped to " + username + " (" + dungeonClass + ")");
        }
    }

    private record Pending(String username, long nextAttempt, int attemptsLeft) { }
}
