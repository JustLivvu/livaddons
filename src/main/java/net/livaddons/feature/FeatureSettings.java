package net.livaddons.feature;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FeatureSettings {
    private static final Gson GSON = new Gson();
    private static boolean terminalWaypoints;
    private static boolean terminalSolver;
    private static boolean deviceSolver;
    private static boolean melodyAlert;
    private static String melodyAlertMessage = "[LivAddons] Melody terminal started";
    private static boolean terminalsGui;
    private static int terminalsGuiX = 10;
    private static int terminalsGuiY = 10;
    private static int guiAccent = 0xFF9B5CFF;
    private static int guiHeader = 0xF01A1B22;
    private static int guiBody = 0xED101116;
    private static boolean disableFire;
    private static boolean copyChat;
    private static int copyChatMode;

    private FeatureSettings() {
    }

    public static boolean terminalWaypointsEnabled() {
        return terminalWaypoints;
    }

    public static void setTerminalWaypointsEnabled(boolean enabled) {
        terminalWaypoints = enabled;
        save();
    }

    public static boolean terminalSolverEnabled() {
        return terminalSolver;
    }

    public static void setTerminalSolverEnabled(boolean enabled) {
        terminalSolver = enabled;
        save();
    }

    public static boolean deviceSolverEnabled() {
        return deviceSolver;
    }

    public static void setDeviceSolverEnabled(boolean enabled) {
        deviceSolver = enabled;
        save();
    }

    public static boolean melodyAlertEnabled() {
        return melodyAlert;
    }

    public static void setMelodyAlertEnabled(boolean enabled) {
        melodyAlert = enabled;
        save();
    }

    public static String melodyAlertMessage() {
        return melodyAlertMessage;
    }

    public static void setMelodyAlertMessage(String message) {
        melodyAlertMessage = message == null || message.isBlank()
                ? "[LivAddons] Melody terminal started" : message;
        save();
    }

    public static boolean terminalsGuiEnabled() { return terminalsGui; }
    public static void setTerminalsGuiEnabled(boolean value) { terminalsGui = value; save(); }
    public static int terminalsGuiX() { return terminalsGuiX; }
    public static int terminalsGuiY() { return terminalsGuiY; }
    public static void setTerminalsGuiPosition(int x, int y) {
        terminalsGuiX = x; terminalsGuiY = y;
    }
    public static int guiAccent() { return guiAccent; }
    public static int guiHeader() { return guiHeader; }
    public static int guiBody() { return guiBody; }
    public static boolean disableFireEnabled() { return disableFire; }
    public static void setDisableFireEnabled(boolean value) { disableFire = value; save(); }
    public static boolean copyChatEnabled() { return copyChat; }
    public static void setCopyChatEnabled(boolean value) { copyChat = value; save(); }
    public static int copyChatMode() { return copyChatMode; }
    public static void setCopyChatMode(int value) {
        copyChatMode = Math.max(0, Math.min(2, value));
        save();
    }
    public static void setGuiColors(String accent, String header, String body) {
        guiAccent = parseColor(accent, guiAccent);
        guiHeader = parseColor(header, guiHeader);
        guiBody = parseColor(body, guiBody);
        save();
    }
    public static String colorHex(int color) { return String.format("#%06X", color & 0xFFFFFF); }

    public static void load() {
        try {
            Path path = configPath();
            if (!Files.exists(path)) return;
            JsonObject o = GSON.fromJson(Files.readString(path), JsonObject.class);
            terminalWaypoints = bool(o, "terminalWaypoints", false);
            terminalSolver = bool(o, "terminalSolver", false);
            deviceSolver = bool(o, "deviceSolver", false);
            melodyAlert = bool(o, "melodyAlert", false);
            terminalsGui = bool(o, "terminalsGui", false);
            if (o.has("melodyAlertMessage")) melodyAlertMessage = o.get("melodyAlertMessage").getAsString();
            if (o.has("terminalsGuiX")) terminalsGuiX = o.get("terminalsGuiX").getAsInt();
            if (o.has("terminalsGuiY")) terminalsGuiY = o.get("terminalsGuiY").getAsInt();
            if (o.has("guiAccent")) guiAccent = o.get("guiAccent").getAsInt();
            if (o.has("guiHeader")) guiHeader = o.get("guiHeader").getAsInt();
            if (o.has("guiBody")) guiBody = o.get("guiBody").getAsInt();
            disableFire = bool(o, "disableFire", false);
            copyChat = bool(o, "copyChat", false);
            if (o.has("copyChatMode")) copyChatMode = Math.max(0, Math.min(2, o.get("copyChatMode").getAsInt()));
        } catch (Exception e) {
            System.err.println("[LivAddons] Could not load config: " + e.getMessage());
        }
    }

    public static synchronized void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("terminalWaypoints", terminalWaypoints);
            o.addProperty("terminalSolver", terminalSolver);
            o.addProperty("deviceSolver", deviceSolver);
            o.addProperty("melodyAlert", melodyAlert);
            o.addProperty("melodyAlertMessage", melodyAlertMessage);
            o.addProperty("terminalsGui", terminalsGui);
            o.addProperty("terminalsGuiX", terminalsGuiX);
            o.addProperty("terminalsGuiY", terminalsGuiY);
            o.addProperty("guiAccent", guiAccent);
            o.addProperty("guiHeader", guiHeader);
            o.addProperty("guiBody", guiBody);
            o.addProperty("disableFire", disableFire);
            o.addProperty("copyChat", copyChat);
            o.addProperty("copyChatMode", copyChatMode);
            Path path = configPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(o));
        } catch (Exception e) {
            System.err.println("[LivAddons] Could not save config: " + e.getMessage());
        }
    }

    private static Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("livaddons.json");
    }
    private static boolean bool(JsonObject o, String key, boolean fallback) {
        return o.has(key) ? o.get(key).getAsBoolean() : fallback;
    }
    private static int parseColor(String value, int fallback) {
        try {
            String clean = value.trim().replace("#", "");
            return 0xFF000000 | Integer.parseInt(clean, 16);
        } catch (Exception ignored) { return fallback; }
    }
}
