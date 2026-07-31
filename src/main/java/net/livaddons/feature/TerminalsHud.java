package net.livaddons.feature;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class TerminalsHud {
    public static final int WIDTH = 126;
    private static final String GOLDOR_START = "[BOSS] Goldor: Who dares trespass into my domain?";
    private static boolean goldorStarted;
    private static boolean phaseFourSeenIncomplete;
    private TerminalsHud() {}

    public static void register() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onGameMessage(message));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("livaddons", "terminals_gui"),
                (graphics, delta) -> render(graphics, false));
    }

    public static void onGameMessage(Component message) {
        if (message.getString().contains(GOLDOR_START)) {
            goldorStarted = true;
            phaseFourSeenIncomplete = false;
        }
    }

    public static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, boolean preview) {
        if (!preview && !FeatureSettings.terminalsGuiEnabled()) return;
        Minecraft client = Minecraft.getInstance();
        if (!preview) {
            if (!isInCatacombs(client)) {
                // Hypixel briefly rebuilds the sidebar between Goldor phases.
                // Keep the run state so the HUD can return without another boss message.
                return;
            }
            if (!goldorStarted) return;
            if (TerminalWaypoints.isPlayerNearPhaseFour(client)) {
                if (TerminalWaypoints.phaseFourHasInactive(client)) phaseFourSeenIncomplete = true;
                if (phaseFourSeenIncomplete && TerminalWaypoints.phaseFourComplete(client)) {
                    goldorStarted = false;
                    phaseFourSeenIncomplete = false;
                    return;
                }
            }
        }
        List<TerminalWaypoints.HudLine> lines = preview
                ? List.of(
                    new TerminalWaypoints.HudLine("Phase 3", TerminalWaypoints.State.HEADER),
                    new TerminalWaypoints.HudLine("Terminal 1", TerminalWaypoints.State.DONE),
                    new TerminalWaypoints.HudLine("Terminal 2", TerminalWaypoints.State.PENDING),
                    new TerminalWaypoints.HudLine("Device: Arrow Align", TerminalWaypoints.State.PENDING),
                    new TerminalWaypoints.HudLine("Lever 1", TerminalWaypoints.State.UNKNOWN))
                : TerminalWaypoints.currentPhaseHud(client);
        if (lines.isEmpty()) return;
        int x = FeatureSettings.terminalsGuiX();
        int y = FeatureSettings.terminalsGuiY();
        int height = 8 + lines.size() * 12;
        if (!FeatureSettings.terminalsGuiClearBackground() || preview) {
            graphics.fill(x, y, x + WIDTH, y + height, FeatureSettings.guiBody());
            graphics.fill(x, y, x + 3, y + height, FeatureSettings.guiAccent());
        }
        int lineY = y + 6;
        for (TerminalWaypoints.HudLine line : lines) {
            int color = switch (line.state()) {
                case HEADER -> 0xFFFFFFFF;
                case DONE -> 0xFF55FF88;
                case PENDING -> 0xFFFF6666;
                case UNKNOWN -> 0xFFAAAAAA;
            };
            String prefix = switch (line.state()) {
                case HEADER -> "";
                case DONE -> "✓ ";
                case PENDING -> "✗ ";
                case UNKNOWN -> "? ";
            };
            graphics.text(client.font, net.minecraft.network.chat.Component.literal(prefix + line.name()),
                    x + 8, lineY, color);
            lineY += 12;
        }
    }


    private static boolean isInCatacombs(Minecraft client) {
        return ScoreboardUtils.sidebarContains(client, "catacombs");
    }
}
