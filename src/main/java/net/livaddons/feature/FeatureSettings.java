package net.livaddons.feature;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FeatureSettings {
    private static final Gson GSON = new Gson();
    private static boolean terminalWaypoints;
    private static boolean terminalSolver;
    private static boolean deviceSolver;
    private static boolean melodyAlert;
    private static String melodyAlertMessage = "[LivAddons] Melody terminal started";
    private static boolean terminalsGui;
    private static boolean terminalsGuiClearBackground;
    private static int terminalsGuiX = 10;
    private static int terminalsGuiY = 10;
    private static int guiAccent = 0xFF9B5CFF;
    private static int guiHeader = 0xF01A1B22;
    private static int guiBody = 0xED101116;
    private static boolean disableFire;
    private static boolean copyChat;
    private static int copyChatMode;
    private static boolean highlights;
    private static int highlightsColor = 0xFFFFFFFF;
    private static int highlightsStyle = 1;
    private static boolean threeByThreeHighlights;
    private static boolean lavaToWater;
    private static boolean dioriteToGlass;
    private static boolean dungeonFinishSong;
    private static boolean partyCommands;
    private static boolean partyCommandEmotes;
    private static boolean leapAlert;
    private static boolean dungeonMap;
    private static int dungeonMapX = 10;
    private static int dungeonMapY = 90;
    private static int dungeonMapScale = 100;
    private static boolean dungeonMapSpinny;
    private static boolean dungeonMapClearBackground;
    private static boolean roomClear;
    private static int roomClearMode;
    private static final Map<String, Boolean> PARTY_COMMAND_TOGGLES = new LinkedHashMap<>();
    private static final List<CommandKeybind> COMMAND_KEYBINDS = new ArrayList<>();

    public record CommandKeybind(int keyCode, String command) {}

    static {
        for (String command : new String[]{"help", "coords", "cf", "8ball", "dice", "fps", "time",
                "holding", "warp", "allinvite", "pt", "promote", "demote", "kick", "kickoffline", "boop"}) {
            PARTY_COMMAND_TOGGLES.put(command, true);
        }
    }

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
    public static boolean terminalsGuiClearBackground() { return terminalsGuiClearBackground; }
    public static void setTerminalsGuiClearBackground(boolean value) {
        terminalsGuiClearBackground = value; save();
    }
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
    public static boolean highlightsEnabled() { return highlights; }
    public static void setHighlightsEnabled(boolean value) { highlights = value; save(); }
    public static int highlightsColor() { return highlightsColor; }
    public static void setHighlightsColor(int value) { highlightsColor = 0xFF000000 | (value & 0xFFFFFF); save(); }
    public static int highlightsStyle() { return highlightsStyle; }
    public static void setHighlightsStyle(int value) {
        highlightsStyle = Math.max(0, Math.min(2, value));
        save();
    }
    public static boolean threeByThreeHighlightsEnabled() { return threeByThreeHighlights; }
    public static void setThreeByThreeHighlightsEnabled(boolean value) {
        threeByThreeHighlights = value;
        save();
    }
    public static boolean lavaToWaterEnabled() { return lavaToWater; }
    public static void setLavaToWaterEnabled(boolean value) { lavaToWater = value; save(); }
    public static boolean dioriteToGlassEnabled() { return dioriteToGlass; }
    public static void setDioriteToGlassEnabled(boolean value) { dioriteToGlass = value; save(); }
    public static boolean dungeonFinishSongEnabled() { return dungeonFinishSong; }
    public static void setDungeonFinishSongEnabled(boolean value) { dungeonFinishSong = value; save(); }
    public static boolean partyCommandsEnabled() { return partyCommands; }
    public static void setPartyCommandsEnabled(boolean value) { partyCommands = value; save(); }
    public static boolean partyCommandEmotesEnabled() { return partyCommandEmotes; }
    public static void setPartyCommandEmotesEnabled(boolean value) { partyCommandEmotes = value; save(); }
    public static boolean leapAlertEnabled() { return leapAlert; }
    public static void setLeapAlertEnabled(boolean value) { leapAlert = value; save(); }
    public static boolean dungeonMapEnabled() { return dungeonMap; }
    public static void setDungeonMapEnabled(boolean value) { dungeonMap = value; save(); }
    public static int dungeonMapX() { return dungeonMapX; }
    public static int dungeonMapY() { return dungeonMapY; }
    public static void setDungeonMapPosition(int x, int y) { dungeonMapX = x; dungeonMapY = y; }
    public static int dungeonMapScale() { return dungeonMapScale; }
    public static void setDungeonMapScale(int value) {
        dungeonMapScale = Math.max(50, Math.min(200, value)); save();
    }
    public static boolean dungeonMapSpinny() { return dungeonMapSpinny; }
    public static void setDungeonMapSpinny(boolean value) { dungeonMapSpinny = value; save(); }
    public static boolean dungeonMapClearBackground() { return dungeonMapClearBackground; }
    public static void setDungeonMapClearBackground(boolean value) { dungeonMapClearBackground = value; save(); }
    public static boolean roomClearEnabled() { return roomClear; }
    public static void setRoomClearEnabled(boolean value) { roomClear = value; save(); }
    public static int roomClearMode() { return roomClearMode; }
    public static void setRoomClearMode(int value) { roomClearMode = Math.max(0, Math.min(2, value)); save(); }
    public static boolean partyCommandEnabled(String command) {
        return PARTY_COMMAND_TOGGLES.getOrDefault(command, true);
    }
    public static void setPartyCommandEnabled(String command, boolean value) {
        if (PARTY_COMMAND_TOGGLES.containsKey(command)) {
            PARTY_COMMAND_TOGGLES.put(command, value);
            save();
        }
    }
    public static List<CommandKeybind> commandKeybinds() {
        return Collections.unmodifiableList(COMMAND_KEYBINDS);
    }
    public static void addCommandKeybind() {
        COMMAND_KEYBINDS.add(new CommandKeybind(-1, ""));
        save();
    }
    public static void removeCommandKeybind(int index) {
        if (index >= 0 && index < COMMAND_KEYBINDS.size()) {
            COMMAND_KEYBINDS.remove(index);
            save();
        }
    }
    public static void setCommandKeybind(int index, int keyCode, String command) {
        if (index >= 0 && index < COMMAND_KEYBINDS.size()) {
            COMMAND_KEYBINDS.set(index, new CommandKeybind(keyCode, command == null ? "" : command));
            save();
        }
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
            terminalsGuiClearBackground = bool(o, "terminalsGuiClearBackground", false);
            if (o.has("melodyAlertMessage")) melodyAlertMessage = o.get("melodyAlertMessage").getAsString();
            if (o.has("terminalsGuiX")) terminalsGuiX = o.get("terminalsGuiX").getAsInt();
            if (o.has("terminalsGuiY")) terminalsGuiY = o.get("terminalsGuiY").getAsInt();
            if (o.has("guiAccent")) guiAccent = o.get("guiAccent").getAsInt();
            if (o.has("guiHeader")) guiHeader = o.get("guiHeader").getAsInt();
            if (o.has("guiBody")) guiBody = o.get("guiBody").getAsInt();
            disableFire = bool(o, "disableFire", false);
            copyChat = bool(o, "copyChat", false);
            if (o.has("copyChatMode")) copyChatMode = Math.max(0, Math.min(2, o.get("copyChatMode").getAsInt()));
            highlights = bool(o, "highlights", false);
            if (o.has("highlightsColor")) highlightsColor = o.get("highlightsColor").getAsInt();
            if (o.has("highlightsStyle")) highlightsStyle = Math.max(0, Math.min(2, o.get("highlightsStyle").getAsInt()));
            threeByThreeHighlights = bool(o, "threeByThreeHighlights", false);
            lavaToWater = bool(o, "lavaToWater", false);
            dioriteToGlass = bool(o, "dioriteToGlass", false);
            dungeonFinishSong = bool(o, "dungeonFinishSong", false);
            partyCommands = bool(o, "partyCommands", false);
            partyCommandEmotes = bool(o, "partyCommandEmotes", false);
            leapAlert = bool(o, "leapAlert", false);
            dungeonMap = bool(o, "dungeonMap", false);
            if (o.has("dungeonMapX")) dungeonMapX = o.get("dungeonMapX").getAsInt();
            if (o.has("dungeonMapY")) dungeonMapY = o.get("dungeonMapY").getAsInt();
            if (o.has("dungeonMapScale")) dungeonMapScale = Math.max(50, Math.min(200, o.get("dungeonMapScale").getAsInt()));
            dungeonMapSpinny = bool(o, "dungeonMapSpinny", false);
            dungeonMapClearBackground = bool(o, "dungeonMapClearBackground", false);
            roomClear = bool(o, "roomClear", false);
            if (o.has("roomClearMode")) roomClearMode = Math.max(0, Math.min(2, o.get("roomClearMode").getAsInt()));
            if (o.has("partyCommandToggles") && o.get("partyCommandToggles").isJsonObject()) {
                JsonObject toggles = o.getAsJsonObject("partyCommandToggles");
                PARTY_COMMAND_TOGGLES.replaceAll((key, oldValue) -> bool(toggles, key, oldValue));
            }
            COMMAND_KEYBINDS.clear();
            if (o.has("commandKeybinds") && o.get("commandKeybinds").isJsonArray()) {
                for (var element : o.getAsJsonArray("commandKeybinds")) {
                    if (!element.isJsonObject()) continue;
                    JsonObject binding = element.getAsJsonObject();
                    int keyCode = binding.has("keyCode") ? binding.get("keyCode").getAsInt() : -1;
                    String command = binding.has("command") ? binding.get("command").getAsString() : "";
                    COMMAND_KEYBINDS.add(new CommandKeybind(keyCode, command));
                }
            }
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
            o.addProperty("terminalsGuiClearBackground", terminalsGuiClearBackground);
            o.addProperty("terminalsGuiX", terminalsGuiX);
            o.addProperty("terminalsGuiY", terminalsGuiY);
            o.addProperty("guiAccent", guiAccent);
            o.addProperty("guiHeader", guiHeader);
            o.addProperty("guiBody", guiBody);
            o.addProperty("disableFire", disableFire);
            o.addProperty("copyChat", copyChat);
            o.addProperty("copyChatMode", copyChatMode);
            o.addProperty("highlights", highlights);
            o.addProperty("highlightsColor", highlightsColor);
            o.addProperty("highlightsStyle", highlightsStyle);
            o.addProperty("threeByThreeHighlights", threeByThreeHighlights);
            o.addProperty("lavaToWater", lavaToWater);
            o.addProperty("dioriteToGlass", dioriteToGlass);
            o.addProperty("dungeonFinishSong", dungeonFinishSong);
            o.addProperty("partyCommands", partyCommands);
            o.addProperty("partyCommandEmotes", partyCommandEmotes);
            o.addProperty("leapAlert", leapAlert);
            o.addProperty("dungeonMap", dungeonMap);
            o.addProperty("dungeonMapX", dungeonMapX);
            o.addProperty("dungeonMapY", dungeonMapY);
            o.addProperty("dungeonMapScale", dungeonMapScale);
            o.addProperty("dungeonMapSpinny", dungeonMapSpinny);
            o.addProperty("dungeonMapClearBackground", dungeonMapClearBackground);
            o.addProperty("roomClear", roomClear);
            o.addProperty("roomClearMode", roomClearMode);
            JsonObject partyToggles = new JsonObject();
            PARTY_COMMAND_TOGGLES.forEach(partyToggles::addProperty);
            o.add("partyCommandToggles", partyToggles);
            JsonArray commandKeybinds = new JsonArray();
            for (CommandKeybind binding : COMMAND_KEYBINDS) {
                JsonObject item = new JsonObject();
                item.addProperty("keyCode", binding.keyCode());
                item.addProperty("command", binding.command());
                commandKeybinds.add(item);
            }
            o.add("commandKeybinds", commandKeybinds);
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
