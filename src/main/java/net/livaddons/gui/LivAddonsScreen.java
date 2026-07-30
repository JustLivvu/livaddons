package net.livaddons.gui;

import net.livaddons.data.PlayerCosmeticData;
import net.livaddons.data.PlayerDataManager;
import net.livaddons.feature.FeatureSettings;
import net.livaddons.net.ApiClient;
import net.livaddons.util.TextGradientUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class LivAddonsScreen extends Screen {
    private static final int ACCENT = 0xFF9B5CFF;
    private static final int ACCENT_DARK = 0xFF7040C8;
    private static final int HEADER = 0xF01A1B22;
    private static final int BODY = 0xED101116;
    private static final int ROW = 0xF0191A21;
    private static final int TEXT_MUTED = 0xFF8C8D98;

    private final Category[] categories = {
            new Category("General"), new Category("Dungeons"), new Category("Floor 7"),
            new Category("Render"), new Category("Misc")
    };
    private final List<String> activeCosmetics = new ArrayList<>();

    private EditBox nickField;
    private EditBox startColorField;
    private EditBox endColorField;
    private EditBox melodyMessageField;
    private EditBox searchField;
    private boolean cosmeticsEnabled;
    private boolean cosmeticsExpanded;
    private boolean melodyAlertExpanded;
    private boolean clickGuiExpanded;
    private boolean copyChatExpanded;
    private int colorTarget;
    private boolean bold;
    private boolean italic;
    private float visualHeight = 1.0f;
    private Component status = Component.empty();
    private boolean syncing;

    private int firstX;
    private int panelY;
    private int panelWidth;
    private int gap;
    private Category draggedCategory;
    private int dragOffsetX;
    private int dragOffsetY;

    public LivAddonsScreen() {
        super(Component.literal("LivAddons ClickGUI"));
    }

    @Override
    protected void init() {
        super.init();
        loadData();
        cosmeticsEnabled = PlayerDataManager.getInstance().areCosmeticsVisible();

        gap = 6;
        panelWidth = Math.min(112, Math.max(82, (width - 32 - gap * 4) / 5));
        firstX = (width - (panelWidth * 5 + gap * 4)) / 2;
        panelY = Math.max(18, height / 9);
        for (int i = 0; i < categories.length; i++) {
            if (categories[i].x < 0) categories[i].x = firstX + i * (panelWidth + gap);
            if (categories[i].y < 0) categories[i].y = panelY;
        }

        int settingsX = miscX() + 6;
        int settingsWidth = panelWidth - 12;
        int y = miscY() + 69;

        PlayerCosmeticData data = currentData();
        Minecraft client = Minecraft.getInstance();

        nickField = new EditBox(font, settingsX, y, settingsWidth, 18, Component.literal("Nickname"));
        nickField.setMaxLength(20);
        String nick = data != null && data.customNick != null ? data.customNick
                : client.player != null ? client.player.getName().getString() : "";
        nickField.setValue(nick.substring(0, Math.min(20, nick.length())));
        addRenderableWidget(nickField);

        startColorField = new EditBox(font, settingsX, y + 31, settingsWidth, 18, Component.literal("Start HEX"));
        startColorField.setMaxLength(7);
        startColorField.setValue(data != null && data.colorStart != null ? data.colorStart : "#FF5555");
        addRenderableWidget(startColorField);

        endColorField = new EditBox(font, settingsX, y + 53, settingsWidth, 18, Component.literal("End HEX"));
        endColorField.setMaxLength(7);
        endColorField.setValue(data != null && data.colorEnd != null ? data.colorEnd : "#55FFFF");
        addRenderableWidget(endColorField);

        int floorX = categories[2].x;
        melodyMessageField = new EditBox(font, floorX + 6, floorY() + 124,
                panelWidth - 12, 18, Component.literal("Alert message"));
        melodyMessageField.setMaxLength(100);
        melodyMessageField.setValue(FeatureSettings.melodyAlertMessage());
        melodyMessageField.setResponder(FeatureSettings::setMelodyAlertMessage);
        addRenderableWidget(melodyMessageField);

        searchField = new EditBox(font, width / 2 - 110, height - 31,
                220, 20, Component.literal("Search modules"));
        searchField.setMaxLength(24);
        searchField.setHint(Component.literal("Search modules..."));
        searchField.setResponder(value -> updateWidgetVisibility());
        addRenderableWidget(searchField);

        updateWidgetVisibility();
    }

    private void loadData() {
        PlayerCosmeticData data = currentData();
        if (data == null) return;
        bold = data.isBold;
        italic = data.isItalic;
        visualHeight = data.visualHeight;
        activeCosmetics.clear();
        if (data.cosmetics != null) activeCosmetics.addAll(data.cosmetics);
    }

    private PlayerCosmeticData currentData() {
        Minecraft client = Minecraft.getInstance();
        return client.player == null ? null
                : PlayerDataManager.getInstance().getCachedCosmeticData(client.player.getUUID());
    }

    private int miscX() {
        return categories[4].x;
    }

    private int miscY() {
        return categories[4].y;
    }

    private int floorY() {
        return categories[2].y;
    }

    private void updateWidgetVisibility() {
        boolean visible = !isSearching() && categories[4].open && cosmeticsExpanded;
        nickField.visible = visible;
        startColorField.visible = visible;
        endColorField.visible = visible;
        melodyMessageField.visible = !isSearching() && categories[2].open && melodyAlertExpanded;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();

        for (int i = 0; i < categories.length; i++) {
            Category category = categories[i];
            if (inside(mouseX, mouseY, category.x, category.y, panelWidth, 24)) {
                if (event.button() == 0) {
                    draggedCategory = category;
                    dragOffsetX = category.x - (int) mouseX;
                    dragOffsetY = category.y - (int) mouseY;
                    return true;
                }
                if (event.button() == 1) {
                    category.open = !category.open;
                    updateWidgetVisibility();
                    return true;
                }
            }
        }

        if (isSearching()) {
            for (Category category : categories) {
                if (!category.open) continue;
                int resultY = category.y + 24;
                for (String module : modulesFor(category.name)) {
                    if (!matchesSearch(module)) continue;
                    if (inside(mouseX, mouseY, category.x, resultY, panelWidth, 22)) {
                        if (event.button() == 0) activateSearchResult(module);
                        return true;
                    }
                    resultY += 22;
                }
            }
            return super.mouseClicked(event, consumed);
        }

        int x = miscX();
        int moduleY = miscY() + 24;
        if (categories[4].open && matchesSearch("Cosmetics")
                && inside(mouseX, mouseY, x, moduleY, panelWidth, 22)) {
            if (event.button() == 0) {
                cosmeticsEnabled = !cosmeticsEnabled;
                PlayerDataManager.getInstance().setCosmeticsVisible(cosmeticsEnabled);
                return true;
            }
            if (event.button() == 1) {
                cosmeticsExpanded = !cosmeticsExpanded;
                if (cosmeticsExpanded) clickGuiExpanded = false;
                updateWidgetVisibility();
                return true;
            }
        }

        int floorX = categories[2].x;
        int floorModuleY = floorY() + 24;
        if (categories[2].open && matchesSearch("Terminal Waypoints")
                && inside(mouseX, mouseY, floorX, floorModuleY, panelWidth, 22)
                && event.button() == 0) {
            FeatureSettings.setTerminalWaypointsEnabled(!FeatureSettings.terminalWaypointsEnabled());
            return true;
        }
        if (categories[2].open && matchesSearch("Terminal Solver")
                && inside(mouseX, mouseY, floorX, floorModuleY + 22, panelWidth, 22)
                && event.button() == 0) {
            FeatureSettings.setTerminalSolverEnabled(!FeatureSettings.terminalSolverEnabled());
            return true;
        }
        if (categories[2].open && matchesSearch("Device Solver")
                && inside(mouseX, mouseY, floorX, floorModuleY + 44, panelWidth, 22)
                && event.button() == 0) {
            FeatureSettings.setDeviceSolverEnabled(!FeatureSettings.deviceSolverEnabled());
            return true;
        }
        if (categories[2].open && matchesSearch("Melody Alert")
                && inside(mouseX, mouseY, floorX, floorModuleY + 66, panelWidth, 22)) {
            if (event.button() == 0) {
                FeatureSettings.setMelodyAlertEnabled(!FeatureSettings.melodyAlertEnabled());
                return true;
            }
            if (event.button() == 1) {
                melodyAlertExpanded = !melodyAlertExpanded;
                updateWidgetVisibility();
                return true;
            }
        }
        if (categories[2].open && matchesSearch("Terminals GUI")
                && inside(mouseX, mouseY, floorX,
                floorModuleY + 88 + (melodyAlertExpanded ? 36 : 0), panelWidth, 22)
                && event.button() == 0) {
            FeatureSettings.setTerminalsGuiEnabled(!FeatureSettings.terminalsGuiEnabled());
            return true;
        }

        int generalX = categories[0].x;
        int generalModuleY = categories[0].y + 24;
        if (categories[0].open && matchesSearch("Copy Chat")
                && inside(mouseX, mouseY, generalX, generalModuleY, panelWidth, 22)) {
            if (event.button() == 0) {
                FeatureSettings.setCopyChatEnabled(!FeatureSettings.copyChatEnabled());
                return true;
            }
            if (event.button() == 1) {
                copyChatExpanded = !copyChatExpanded;
                return true;
            }
        }
        if (categories[0].open && copyChatExpanded && event.button() == 0
                && inside(mouseX, mouseY, generalX + 6, generalModuleY + 26, panelWidth - 12, 18)) {
            FeatureSettings.setCopyChatMode((FeatureSettings.copyChatMode() + 1) % 3);
            return true;
        }

        int renderX = categories[3].x;
        int renderModuleY = categories[3].y + 24;
        if (categories[3].open && matchesSearch("Disable Fire")
                && inside(mouseX, mouseY, renderX, renderModuleY, panelWidth, 22)
                && event.button() == 0) {
            FeatureSettings.setDisableFireEnabled(!FeatureSettings.disableFireEnabled());
            return true;
        }

        int miscModuleY = miscY() + 24;
        int cosmeticsExtra = cosmeticsExpanded ? 234 : 0;
        int clickGuiY = miscModuleY + 22 + cosmeticsExtra;
        if (categories[4].open
                && inside(mouseX, mouseY, miscX(), clickGuiY, panelWidth, 22)) {
            if (event.button() == 1) {
                clickGuiExpanded = !clickGuiExpanded;
                updateWidgetVisibility();
                return true;
            }
        }
        int clickExtra = clickGuiExpanded ? 88 : 0;
        int positionsY = clickGuiY + 22 + clickExtra;
        if (categories[4].open
                && inside(mouseX, mouseY, miscX(), positionsY, panelWidth, 22)
                && event.button() == 0) {
            Minecraft.getInstance().setScreen(new GuiPositionsScreen());
            return true;
        }

        if (categories[4].open && cosmeticsExpanded && event.button() == 0) {
            int controlX = miscX() + 6;
            int controlWidth = panelWidth - 12;
            int controlsY = miscY() + 69;
            if (inside(mouseX, mouseY, controlX, controlsY + 75, controlWidth, 18)) {
                bold = !bold;
                return true;
            }
            if (inside(mouseX, mouseY, controlX, controlsY + 97, controlWidth, 18)) {
                italic = !italic;
                return true;
            }
            if (inside(mouseX, mouseY, controlX, controlsY + 119, controlWidth, 18)) {
                double progress = Math.max(0, Math.min(1, (mouseX - controlX) / controlWidth));
                visualHeight = (float) (0.5 + progress * 1.5);
                return true;
            }
            if (inside(mouseX, mouseY, controlX, controlsY + 167, controlWidth, 18)) {
                save();
                return true;
            }
        }
        if (categories[4].open && clickGuiExpanded && event.button() == 0) {
            int xControl = miscX() + 6;
            int yControl = clickGuiY + 26;
            if (inside(mouseX, mouseY, xControl, yControl, panelWidth - 12, 18)) {
                colorTarget = (colorTarget + 1) % 3;
                return true;
            }
            for (int channel = 0; channel < 3; channel++) {
                int sliderY = yControl + 22 + channel * 22;
                if (inside(mouseX, mouseY, xControl, sliderY, panelWidth - 12, 18)) {
                    int value = (int) Math.round(Math.max(0, Math.min(1,
                            (mouseX - xControl) / (panelWidth - 12.0))) * 255);
                    setSelectedColorChannel(channel, value);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggedCategory = null;
        return super.mouseReleased(event);
    }

    private boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private void save() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || syncing) return;

        String nick = nickField.getValue();
        nick = nick.substring(0, Math.min(20, nick.length()));
        PlayerCosmeticData data = new PlayerCosmeticData(
                client.player.getUUID().toString(), client.player.getName().getString(), nick,
                startColorField.getValue(), endColorField.getValue(), bold, italic,
                visualHeight, activeCosmetics
        );

        status = Component.literal("Syncing...").withStyle(ChatFormatting.YELLOW);
        syncing = true;
        ApiClient.syncProfile(data).thenAccept(result -> client.execute(() -> {
            syncing = false;
            if (result.success()) {
                PlayerDataManager.getInstance().updateCache(data);
                status = Component.literal(result.message()).withStyle(ChatFormatting.GREEN);
            } else {
                status = Component.literal(result.message()).withStyle(ChatFormatting.RED);
            }
        })).exceptionally(error -> {
            client.execute(() -> {
                syncing = false;
                status = Component.literal("Sync exception: " + error.getMessage())
                        .withStyle(ChatFormatting.RED);
            });
            return null;
        });
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (draggedCategory != null) {
            draggedCategory.x = Math.max(0, Math.min(width - panelWidth, mouseX + dragOffsetX));
            draggedCategory.y = Math.max(0, Math.min(height - 24, mouseY + dragOffsetY));
            repositionSettingFields();
        }
        graphics.fill(0, 0, width, height, 0x8506070A);

        for (int i = 0; i < categories.length; i++) {
            renderCategory(graphics, categories[i], mouseX, mouseY);
        }

        if (categories[4].open && cosmeticsExpanded) renderCosmeticSettings(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if (categories[4].open && cosmeticsExpanded && !status.getString().isEmpty()) {
            graphics.centeredText(font, status, miscX() + panelWidth / 2,
                    miscY() + 261, 0xFFFFFFFF);
        }
    }

    private void renderCategory(GuiGraphicsExtractor graphics, Category category, int mouseX, int mouseY) {
        int x = category.x;
        int y = category.y;
        boolean hover = inside(mouseX, mouseY, x, y, panelWidth, 24);
        graphics.fill(x, y, x + panelWidth, y + 24, hover ? ACCENT_DARK : FeatureSettings.guiHeader());
        graphics.fill(x, y + 22, x + panelWidth, y + 24, FeatureSettings.guiAccent());
        graphics.text(font, Component.literal(category.name).withStyle(ChatFormatting.BOLD),
                x + 7, y + 8, 0xFFFFFFFF);
        graphics.text(font, Component.literal(category.open ? "-" : "+"),
                x + panelWidth - 12, y + 8, 0xFFFFFFFF);

        if (!category.open) return;

        if (isSearching()) {
            int resultY = y + 24;
            boolean found = false;
            for (String module : modulesFor(category.name)) {
                if (!matchesSearch(module)) continue;
                found = true;
                boolean enabled = moduleEnabled(module);
                boolean rowHover = inside(mouseX, mouseY, x, resultY, panelWidth, 22);
                graphics.fill(x, resultY, x + panelWidth, resultY + 22, rowHover ? 0xFF22242D : ROW);
                graphics.fill(x, resultY, x + 2, resultY + 22,
                        enabled ? FeatureSettings.guiAccent() : 0xFF343640);
                graphics.text(font, Component.literal(displayModuleName(module)), x + 7, resultY + 8,
                        enabled ? 0xFFFFFFFF : TEXT_MUTED);
                resultY += 22;
            }
            if (!found) {
                graphics.fill(x, y + 24, x + panelWidth, y + 44, FeatureSettings.guiBody());
                graphics.centeredText(font, Component.literal("No results"), x + panelWidth / 2,
                        y + 31, 0xFF555761);
            }
            return;
        }

        if (category.name.equals("General")) {
            int moduleY = y + 24;
            boolean enabled = FeatureSettings.copyChatEnabled();
            graphics.fill(x, moduleY, x + panelWidth, moduleY + 22, ROW);
            graphics.fill(x, moduleY, x + 2, moduleY + 22,
                    enabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Copy Chat"), x + 7, moduleY + 8,
                    enabled ? 0xFFFFFFFF : TEXT_MUTED);
            graphics.text(font, Component.literal(copyChatExpanded ? "-" : "+"),
                    x + panelWidth - 12, moduleY + 8, TEXT_MUTED);
            if (copyChatExpanded) {
                graphics.fill(x, moduleY + 22, x + panelWidth, moduleY + 48, FeatureSettings.guiBody());
                renderButton(graphics, x + 6, moduleY + 26, panelWidth - 12, copyChatModeName());
            }
            return;
        }

        if (category.name.equals("Floor 7")) {
            int moduleY = y + 24;
            boolean moduleHover = inside(mouseX, mouseY, x, moduleY, panelWidth, 22);
            boolean enabled = FeatureSettings.terminalWaypointsEnabled();
            graphics.fill(x, moduleY, x + panelWidth, moduleY + 22, moduleHover ? 0xFF22242D : ROW);
            graphics.fill(x, moduleY, x + 2, moduleY + 22, enabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Terminal WPs"), x + 7, moduleY + 8,
                    enabled ? 0xFFFFFFFF : TEXT_MUTED);
            int solverY = moduleY + 22;
            boolean solverHover = inside(mouseX, mouseY, x, solverY, panelWidth, 22);
            boolean solverEnabled = FeatureSettings.terminalSolverEnabled();
            graphics.fill(x, solverY, x + panelWidth, solverY + 22, solverHover ? 0xFF22242D : ROW);
            graphics.fill(x, solverY, x + 2, solverY + 22, solverEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Terminal Solver"), x + 7, solverY + 8,
                    solverEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            int deviceY = solverY + 22;
            boolean deviceHover = inside(mouseX, mouseY, x, deviceY, panelWidth, 22);
            boolean deviceEnabled = FeatureSettings.deviceSolverEnabled();
            graphics.fill(x, deviceY, x + panelWidth, deviceY + 22, deviceHover ? 0xFF22242D : ROW);
            graphics.fill(x, deviceY, x + 2, deviceY + 22, deviceEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Device Solver"), x + 7, deviceY + 8,
                    deviceEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            int melodyY = deviceY + 22;
            boolean melodyHover = inside(mouseX, mouseY, x, melodyY, panelWidth, 22);
            boolean melodyEnabled = FeatureSettings.melodyAlertEnabled();
            graphics.fill(x, melodyY, x + panelWidth, melodyY + 22, melodyHover ? 0xFF22242D : ROW);
            graphics.fill(x, melodyY, x + 2, melodyY + 22, melodyEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Melody Alert"), x + 7, melodyY + 8,
                    melodyEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            graphics.text(font, Component.literal(melodyAlertExpanded ? "-" : "+"),
                    x + panelWidth - 12, melodyY + 8, TEXT_MUTED);
            if (melodyAlertExpanded) {
                graphics.fill(x, melodyY + 22, x + panelWidth, melodyY + 58, FeatureSettings.guiBody());
                graphics.text(font, Component.literal("Message"), x + 6, melodyY + 26, TEXT_MUTED);
            }
            int terminalsY = melodyY + 22 + (melodyAlertExpanded ? 36 : 0);
            boolean terminalsEnabled = FeatureSettings.terminalsGuiEnabled();
            graphics.fill(x, terminalsY, x + panelWidth, terminalsY + 22, ROW);
            graphics.fill(x, terminalsY, x + 2, terminalsY + 22,
                    terminalsEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Terminals GUI"), x + 7, terminalsY + 8,
                    terminalsEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            return;
        }

        if (!category.name.equals("Misc")) {
            if (category.name.equals("Render")) {
                int moduleY = y + 24;
                boolean enabled = FeatureSettings.disableFireEnabled();
                graphics.fill(x, moduleY, x + panelWidth, moduleY + 22, ROW);
                graphics.fill(x, moduleY, x + 2, moduleY + 22,
                        enabled ? FeatureSettings.guiAccent() : 0xFF343640);
                graphics.text(font, Component.literal("Disable Fire"), x + 7, moduleY + 8,
                        enabled ? 0xFFFFFFFF : TEXT_MUTED);
                return;
            }
            graphics.fill(x, y + 24, x + panelWidth, y + 44, FeatureSettings.guiBody());
            graphics.centeredText(font, Component.literal("No modules"), x + panelWidth / 2,
                    y + 31, 0xFF555761);
            return;
        }

        int moduleY = y + 24;
        boolean moduleHover = inside(mouseX, mouseY, x, moduleY, panelWidth, 22);
        graphics.fill(x, moduleY, x + panelWidth, moduleY + 22, moduleHover ? 0xFF22242D : ROW);
        graphics.fill(x, moduleY, x + 2, moduleY + 22, cosmeticsEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
        graphics.text(font, Component.literal("Cosmetics"), x + 7, moduleY + 8,
                cosmeticsEnabled ? 0xFFFFFFFF : TEXT_MUTED);
        graphics.text(font, Component.literal(cosmeticsExpanded ? "-" : "+"),
                x + panelWidth - 12, moduleY + 8, TEXT_MUTED);

        int cosmeticsExtra = cosmeticsExpanded ? 234 : 0;
        if (cosmeticsExpanded)
            graphics.fill(x, moduleY + 22, x + panelWidth, moduleY + 22 + cosmeticsExtra, FeatureSettings.guiBody());
        int clickGuiY = moduleY + 22 + cosmeticsExtra;
        graphics.fill(x, clickGuiY, x + panelWidth, clickGuiY + 22, ROW);
        graphics.text(font, Component.literal("Click GUI"), x + 7, clickGuiY + 8, 0xFFFFFFFF);
        graphics.text(font, Component.literal(clickGuiExpanded ? "-" : "+"),
                x + panelWidth - 12, clickGuiY + 8, TEXT_MUTED);
        int clickExtra = clickGuiExpanded ? 88 : 0;
        int positionsY = clickGuiY + 22 + clickExtra;
        graphics.fill(x, positionsY, x + panelWidth, positionsY + 22, ROW);
        graphics.text(font, Component.literal("GUI Positions"), x + 7, positionsY + 8, 0xFFFFFFFF);
        if (clickGuiExpanded) {
            graphics.fill(x, clickGuiY + 22, x + panelWidth, clickGuiY + 22 + clickExtra, FeatureSettings.guiBody());
            int controlY = clickGuiY + 26;
            String target = switch (colorTarget) { case 1 -> "Header"; case 2 -> "Body"; default -> "Accent"; };
            renderButton(graphics, x + 6, controlY, panelWidth - 12, target);
            int color = selectedGuiColor();
            renderRgbSlider(graphics, x + 6, controlY + 22, panelWidth - 12, "R", (color >> 16) & 255, 0xFFFF5555);
            renderRgbSlider(graphics, x + 6, controlY + 44, panelWidth - 12, "G", (color >> 8) & 255, 0xFF55FF55);
            renderRgbSlider(graphics, x + 6, controlY + 66, panelWidth - 12, "B", color & 255, 0xFF5599FF);
        }
    }

    private void renderCosmeticSettings(GuiGraphicsExtractor graphics) {
        int x = miscX() + 6;
        int y = miscY() + 57;
        graphics.text(font, Component.literal("Nickname"), x, y, TEXT_MUTED);
        graphics.text(font, Component.literal("Gradient"), x, y + 31, TEXT_MUTED);

        renderToggle(graphics, x, y + 87, panelWidth - 12, "Bold", bold);
        renderToggle(graphics, x, y + 109, panelWidth - 12, "Italic", italic);
        renderSlider(graphics, x, y + 131, panelWidth - 12);

        Component preview = TextGradientUtil.buildGradientText(
                nickField.getValue().isEmpty() ? "Preview" : nickField.getValue(),
                startColorField.getValue(), endColorField.getValue(), bold, italic
        );
        graphics.centeredText(font, preview, miscX() + panelWidth / 2, y + 157, 0xFFFFFFFF);
        renderButton(graphics, x, y + 179, panelWidth - 12, "Save & sync");
    }

    private void renderToggle(GuiGraphicsExtractor graphics, int x, int y, int width, String label, boolean enabled) {
        graphics.fill(x, y, x + width, y + 18, enabled ? 0xFF372456 : 0xFF202128);
        graphics.fill(x, y, x + 2, y + 18, enabled ? FeatureSettings.guiAccent() : 0xFF4B4D58);
        graphics.text(font, Component.literal(label), x + 6, y + 6,
                enabled ? 0xFFFFFFFF : TEXT_MUTED);
        String state = enabled ? "ON" : "OFF";
        graphics.text(font, Component.literal(state), x + width - font.width(state) - 5, y + 6,
                enabled ? FeatureSettings.guiAccent() : 0xFF686A74);
    }

    private void renderSlider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        double progress = (visualHeight - 0.5) / 1.5;
        int fillWidth = (int) Math.round(width * Math.max(0, Math.min(1, progress)));
        graphics.fill(x, y, x + width, y + 18, 0xFF202128);
        graphics.fill(x, y + 16, x + width, y + 18, 0xFF363842);
        graphics.fill(x, y + 16, x + fillWidth, y + 18, FeatureSettings.guiAccent());
        graphics.text(font, Component.literal(String.format("Scale %.2fx", visualHeight)),
                x + 6, y + 6, 0xFFD8D9DF);
    }

    private int selectedGuiColor() {
        return switch (colorTarget) {
            case 1 -> FeatureSettings.guiHeader();
            case 2 -> FeatureSettings.guiBody();
            default -> FeatureSettings.guiAccent();
        };
    }

    private void setSelectedColorChannel(int channel, int value) {
        int color = selectedGuiColor();
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        if (channel == 0) red = value;
        else if (channel == 1) green = value;
        else blue = value;

        String changed = String.format("#%02X%02X%02X", red, green, blue);
        String accent = FeatureSettings.colorHex(FeatureSettings.guiAccent());
        String header = FeatureSettings.colorHex(FeatureSettings.guiHeader());
        String body = FeatureSettings.colorHex(FeatureSettings.guiBody());
        switch (colorTarget) {
            case 1 -> FeatureSettings.setGuiColors(accent, changed, body);
            case 2 -> FeatureSettings.setGuiColors(accent, header, changed);
            default -> FeatureSettings.setGuiColors(changed, header, body);
        }
    }

    private void renderRgbSlider(GuiGraphicsExtractor graphics, int x, int y, int width,
                                 String label, int value, int sliderColor) {
        int fillWidth = (int) Math.round(width * (value / 255.0));
        graphics.fill(x, y, x + width, y + 18, 0xFF202128);
        graphics.fill(x, y + 15, x + width, y + 18, 0xFF363842);
        graphics.fill(x, y + 15, x + fillWidth, y + 18, sliderColor);
        graphics.text(font, Component.literal(label + "  " + value), x + 6, y + 6, 0xFFFFFFFF);
    }

    private void renderButton(GuiGraphicsExtractor graphics, int x, int y, int width, String label) {
        graphics.fill(x, y, x + width, y + 18, syncing ? 0xFF383943 : ACCENT_DARK);
        graphics.fill(x, y, x + width, y + 1, syncing ? 0xFF555762 : FeatureSettings.guiAccent());
        graphics.centeredText(font, Component.literal(syncing ? "Syncing..." : label),
                x + width / 2, y + 6, syncing ? TEXT_MUTED : 0xFFFFFFFF);
    }

    private String copyChatModeName() {
        return switch (FeatureSettings.copyChatMode()) {
            case 1 -> "Shift + Left Click";
            case 2 -> "Right Click";
            default -> "Left Click";
        };
    }

    private boolean matchesSearch(String moduleName) {
        return searchField == null || searchField.getValue().isBlank()
                || moduleName.toLowerCase().contains(searchField.getValue().trim().toLowerCase());
    }

    private boolean isSearching() {
        return searchField != null && !searchField.getValue().trim().isEmpty();
    }

    private String[] modulesFor(String category) {
        return switch (category) {
            case "General" -> new String[]{"Copy Chat"};
            case "Floor 7" -> new String[]{"Terminal Waypoints", "Terminal Solver", "Device Solver",
                    "Melody Alert", "Terminals GUI"};
            case "Render" -> new String[]{"Disable Fire"};
            case "Misc" -> new String[]{"Cosmetics", "Click GUI", "GUI Positions"};
            default -> new String[0];
        };
    }

    private String displayModuleName(String module) {
        return module.equals("Terminal Waypoints") ? "Terminal WPs" : module;
    }

    private boolean moduleEnabled(String module) {
        return switch (module) {
            case "Copy Chat" -> FeatureSettings.copyChatEnabled();
            case "Terminal Waypoints" -> FeatureSettings.terminalWaypointsEnabled();
            case "Terminal Solver" -> FeatureSettings.terminalSolverEnabled();
            case "Device Solver" -> FeatureSettings.deviceSolverEnabled();
            case "Melody Alert" -> FeatureSettings.melodyAlertEnabled();
            case "Terminals GUI" -> FeatureSettings.terminalsGuiEnabled();
            case "Disable Fire" -> FeatureSettings.disableFireEnabled();
            case "Cosmetics" -> cosmeticsEnabled;
            default -> true;
        };
    }

    private void activateSearchResult(String module) {
        switch (module) {
            case "Copy Chat" -> FeatureSettings.setCopyChatEnabled(!FeatureSettings.copyChatEnabled());
            case "Terminal Waypoints" ->
                    FeatureSettings.setTerminalWaypointsEnabled(!FeatureSettings.terminalWaypointsEnabled());
            case "Terminal Solver" ->
                    FeatureSettings.setTerminalSolverEnabled(!FeatureSettings.terminalSolverEnabled());
            case "Device Solver" -> FeatureSettings.setDeviceSolverEnabled(!FeatureSettings.deviceSolverEnabled());
            case "Melody Alert" -> FeatureSettings.setMelodyAlertEnabled(!FeatureSettings.melodyAlertEnabled());
            case "Terminals GUI" -> FeatureSettings.setTerminalsGuiEnabled(!FeatureSettings.terminalsGuiEnabled());
            case "Disable Fire" -> FeatureSettings.setDisableFireEnabled(!FeatureSettings.disableFireEnabled());
            case "Cosmetics" -> {
                cosmeticsEnabled = !cosmeticsEnabled;
                PlayerDataManager.getInstance().setCosmeticsVisible(cosmeticsEnabled);
            }
            case "Click GUI" -> {
                searchField.setValue("");
                clickGuiExpanded = true;
                cosmeticsExpanded = false;
                updateWidgetVisibility();
            }
            case "GUI Positions" -> Minecraft.getInstance().setScreen(new GuiPositionsScreen());
            default -> {
            }
        }
    }

    private void repositionSettingFields() {
        int x = miscX() + 6;
        int y = miscY() + 69;
        nickField.setX(x);
        nickField.setY(y);
        startColorField.setX(x);
        startColorField.setY(y + 31);
        endColorField.setX(x);
        endColorField.setY(y + 53);
        melodyMessageField.setX(categories[2].x + 6);
        melodyMessageField.setY(categories[2].y + 124);
    }

    private static class Category {
        final String name;
        boolean open = true;
        int x = -1;
        int y = -1;

        Category(String name) {
            this.name = name;
        }
    }

}
