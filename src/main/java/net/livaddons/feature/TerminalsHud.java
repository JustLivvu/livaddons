package net.livaddons.feature;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class TerminalsHud {
    public static final int WIDTH = 126;
    private TerminalsHud() {}

    public static void register() {
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("livaddons", "terminals_gui"),
                (graphics, delta) -> render(graphics, false));
    }

    public static void render(net.minecraft.client.gui.GuiGraphicsExtractor graphics, boolean preview) {
        if (!preview && !FeatureSettings.terminalsGuiEnabled()) return;
        Minecraft client = Minecraft.getInstance();
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
        graphics.fill(x, y, x + WIDTH, y + height, FeatureSettings.guiBody());
        graphics.fill(x, y, x + 3, y + height, FeatureSettings.guiAccent());
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
}
