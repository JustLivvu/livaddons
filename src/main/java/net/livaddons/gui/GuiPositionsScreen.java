package net.livaddons.gui;

import net.livaddons.feature.FeatureSettings;
import net.livaddons.feature.TerminalsHud;
import net.livaddons.feature.DungeonMapHud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class GuiPositionsScreen extends Screen {
    private int dragging; 
    private int offsetX;
    private int offsetY;

    public GuiPositionsScreen() {
        super(Component.literal("GUI Positions"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, 0xA008090D);
        if (dragging == 1) {
            FeatureSettings.setTerminalsGuiPosition(
                    Math.max(0, Math.min(width - TerminalsHud.WIDTH, mouseX + offsetX)),
                    Math.max(0, Math.min(height - 70, mouseY + offsetY)));
        } else if (dragging == 2) {
            FeatureSettings.setDungeonMapPosition(
                    Math.max(0, Math.min(width - DungeonMapHud.renderedSize(), mouseX + offsetX)),
                    Math.max(0, Math.min(height - DungeonMapHud.renderedSize(), mouseY + offsetY)));
        }
        TerminalsHud.render(graphics, true);
        DungeonMapHud.render(graphics, true);
        graphics.centeredText(font, Component.literal("Drag HUD elements"),
                width / 2, 12, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        int x = FeatureSettings.terminalsGuiX();
        int y = FeatureSettings.terminalsGuiY();
        if (event.button() == 0 && event.x() >= x && event.x() <= x + TerminalsHud.WIDTH
                && event.y() >= y && event.y() <= y + 80) {
            dragging = 1;
            offsetX = x - (int) event.x();
            offsetY = y - (int) event.y();
            return true;
        }
        x = FeatureSettings.dungeonMapX();
        y = FeatureSettings.dungeonMapY();
        if (event.button() == 0 && event.x() >= x && event.x() <= x + DungeonMapHud.renderedSize()
                && event.y() >= y && event.y() <= y + DungeonMapHud.renderedSize()) {
            dragging = 2;
            offsetX = x - (int) event.x();
            offsetY = y - (int) event.y();
            return true;
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = 0;
        FeatureSettings.save();
        return super.mouseReleased(event);
    }
}
