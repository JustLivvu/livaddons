package net.livaddons.gui;

import net.livaddons.feature.FeatureSettings;
import net.livaddons.feature.TerminalsHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class GuiPositionsScreen extends Screen {
    private boolean dragging;
    private int offsetX;
    private int offsetY;

    public GuiPositionsScreen() {
        super(Component.literal("GUI Positions"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xA008090D);
        if (dragging) {
            FeatureSettings.setTerminalsGuiPosition(
                    Math.max(0, Math.min(width - TerminalsHud.WIDTH, mouseX + offsetX)),
                    Math.max(0, Math.min(height - 70, mouseY + offsetY)));
        }
        TerminalsHud.render(graphics, true);
        graphics.centeredText(font, Component.literal("Drag the Terminals GUI"),
                width / 2, 12, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        int x = FeatureSettings.terminalsGuiX();
        int y = FeatureSettings.terminalsGuiY();
        if (event.button() == 0 && event.x() >= x && event.x() <= x + TerminalsHud.WIDTH
                && event.y() >= y && event.y() <= y + 80) {
            dragging = true;
            offsetX = x - (int) event.x();
            offsetY = y - (int) event.y();
            return true;
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        FeatureSettings.save();
        return super.mouseReleased(event);
    }
}
