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
    private boolean terminalsGuiExpanded;
    private boolean clickGuiExpanded;
    private boolean copyChatExpanded;
    private boolean highlightsExpanded;
    private boolean dungeonMapExpanded;
    private boolean roomClearExpanded;
    private boolean partyCommandsExpanded;
    private int colorTarget;
    private boolean bold;
    private boolean italic;
    private float visualHeight = 1.0f;
    private Component status = Component.empty();
    private boolean syncing;
    private String settingsModule;
    private String draggedSlider;
    private int draggedSliderChannel;
    private boolean draggingSettingsPopup;
    private int settingsPopupX = -1;
    private int settingsPopupY = -1;
    private int settingsDragOffsetX;
    private int settingsDragOffsetY;

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
        panelWidth = Math.max(126, Math.min(148, (width - 32 - gap * 4) / 5));
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
        boolean visible = !isSearching() && "Cosmetics".equals(settingsModule);
        nickField.visible = visible;
        startColorField.visible = visible;
        endColorField.visible = visible;
        melodyMessageField.visible = !isSearching() && "Melody Alert".equals(settingsModule);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();

        if (settingsModule != null) {
            int px = popupX() + 12, py = popupY() + 38;
            boolean textField = ("Melody Alert".equals(settingsModule) && inside(mouseX, mouseY, px, py, 256, 18))
                    || ("Cosmetics".equals(settingsModule) && (inside(mouseX, mouseY, px, py, 256, 18)
                    || inside(mouseX, mouseY, px, py + 31, 256, 18)
                    || inside(mouseX, mouseY, px, py + 53, 256, 18)));
            if (textField) return super.mouseClicked(event, consumed);
            if (event.button() == 0 && inside(mouseX, mouseY, popupX(), popupY(), 252, 28)) {
                draggingSettingsPopup = true;
                settingsDragOffsetX = popupX() - (int) mouseX;
                settingsDragOffsetY = popupY() - (int) mouseY;
                return true;
            }
            return handleSettingsPopupClick(mouseX, mouseY, event.button());
        }

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

        for (Category category : categories) {
            if (!category.open) continue;
            int rowY = category.y + 24;
            for (String module : modulesFor(category.name)) {
                if (inside(mouseX, mouseY, category.x, rowY, panelWidth, 22)) {
                    if (event.button() == 1 && hasSettings(module)) {
                        settingsModule = module;
                        clearLegacyExpansions();
                        repositionSettingFields();
                        updateWidgetVisibility();
                    } else if (event.button() == 0) activateSearchResult(module);
                    return true;
                }
                rowY += 22;
            }
        }

        int dungeonsX = categories[1].x;
        int highlightsY = categories[1].y + 24;
        if (categories[1].open && inside(mouseX, mouseY, dungeonsX, highlightsY, panelWidth, 22)) {
            if (event.button() == 0) {
                FeatureSettings.setHighlightsEnabled(!FeatureSettings.highlightsEnabled());
                return true;
            }
            if (event.button() == 1) {
                highlightsExpanded = !highlightsExpanded;
                return true;
            }
        }
        if (categories[1].open && highlightsExpanded && event.button() == 0) {
            int controlX = dungeonsX + 6;
            int controlY = highlightsY + 26;
            if (inside(mouseX, mouseY, controlX, controlY, panelWidth - 12, 18)) {
                FeatureSettings.setHighlightsStyle((FeatureSettings.highlightsStyle() + 1) % 3);
                return true;
            }
            for (int channel = 0; channel < 3; channel++) {
                int sliderY = controlY + 22 + channel * 22;
                if (inside(mouseX, mouseY, controlX, sliderY, panelWidth - 12, 18)) {
                    int value = (int) Math.round(Math.max(0, Math.min(1,
                            (mouseX - controlX) / (panelWidth - 12.0))) * 255);
                    setHighlightColorChannel(channel, value);
                    return true;
                }
            }
        }
        int lavaToWaterY = highlightsY + 22 + (highlightsExpanded ? 92 : 0);
        if (categories[1].open && inside(mouseX, mouseY, dungeonsX, lavaToWaterY, panelWidth, 22)
                && event.button() == 0) {
            FeatureSettings.setLavaToWaterEnabled(!FeatureSettings.lavaToWaterEnabled());
            return true;
        }
        int finishSongY = lavaToWaterY + 22;
        if (categories[1].open && inside(mouseX, mouseY, dungeonsX, finishSongY, panelWidth, 22)
                && event.button() == 0) {
            FeatureSettings.setDungeonFinishSongEnabled(!FeatureSettings.dungeonFinishSongEnabled());
            return true;
        }
        int leapAlertY = finishSongY + 22;
        if (categories[1].open && inside(mouseX, mouseY, dungeonsX, leapAlertY, panelWidth, 22)
                && event.button() == 0) {
            FeatureSettings.setLeapAlertEnabled(!FeatureSettings.leapAlertEnabled());
            return true;
        }
        int dungeonMapY = leapAlertY + 22;
        if (categories[1].open && inside(mouseX, mouseY, dungeonsX, dungeonMapY, panelWidth, 22)) {
            if (event.button() == 0) FeatureSettings.setDungeonMapEnabled(!FeatureSettings.dungeonMapEnabled());
            else if (event.button() == 1) dungeonMapExpanded = !dungeonMapExpanded;
            return true;
        }
        if (categories[1].open && dungeonMapExpanded && event.button() == 0) {
            int controlX = dungeonsX + 6;
            int scaleY = dungeonMapY + 26;
            if (inside(mouseX, mouseY, controlX, scaleY, panelWidth - 12, 18)) {
                double progress = Math.max(0, Math.min(1, (mouseX - controlX) / (panelWidth - 12.0)));
                FeatureSettings.setDungeonMapScale(50 + (int) Math.round(progress * 150));
                return true;
            }
            if (inside(mouseX, mouseY, controlX, scaleY + 22, panelWidth - 12, 18)) {
                FeatureSettings.setDungeonMapSpinny(!FeatureSettings.dungeonMapSpinny());
                return true;
            }
            if (inside(mouseX, mouseY, controlX, scaleY + 44, panelWidth - 12, 18)) {
                FeatureSettings.setDungeonMapClearBackground(!FeatureSettings.dungeonMapClearBackground());
                return true;
            }
        }
        int roomClearY = dungeonMapY + 22 + (dungeonMapExpanded ? 70 : 0);
        if (categories[1].open && inside(mouseX, mouseY, dungeonsX, roomClearY, panelWidth, 22)) {
            if (event.button() == 0) FeatureSettings.setRoomClearEnabled(!FeatureSettings.roomClearEnabled());
            else if (event.button() == 1) roomClearExpanded = !roomClearExpanded;
            return true;
        }
        if (categories[1].open && roomClearExpanded && event.button() == 0
                && inside(mouseX, mouseY, dungeonsX + 6, roomClearY + 26, panelWidth - 12, 18)) {
            FeatureSettings.setRoomClearMode((FeatureSettings.roomClearMode() + 1) % 3);
            return true;
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
                floorModuleY + 88 + (melodyAlertExpanded ? 36 : 0), panelWidth, 22)) {
            if (event.button() == 0)
                FeatureSettings.setTerminalsGuiEnabled(!FeatureSettings.terminalsGuiEnabled());
            else if (event.button() == 1) terminalsGuiExpanded = !terminalsGuiExpanded;
            return true;
        }
        int terminalsSettingsY = floorModuleY + 114 + (melodyAlertExpanded ? 36 : 0);
        if (categories[2].open && terminalsGuiExpanded && event.button() == 0
                && inside(mouseX, mouseY, floorX + 6, terminalsSettingsY, panelWidth - 12, 18)) {
            FeatureSettings.setTerminalsGuiClearBackground(!FeatureSettings.terminalsGuiClearBackground());
            return true;
        }
        if (categories[2].open && matchesSearch("3x3 Highlights")
                && inside(mouseX, mouseY, floorX,
                floorModuleY + 110 + (melodyAlertExpanded ? 36 : 0)
                        + (terminalsGuiExpanded ? 26 : 0), panelWidth, 22)
                && event.button() == 0) {
            FeatureSettings.setThreeByThreeHighlightsEnabled(
                    !FeatureSettings.threeByThreeHighlightsEnabled());
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
        int partyCommandsY = generalModuleY + 22 + (copyChatExpanded ? 26 : 0);
        if (categories[0].open && inside(mouseX, mouseY, generalX, partyCommandsY, panelWidth, 22)) {
            if (event.button() == 0) {
                FeatureSettings.setPartyCommandsEnabled(!FeatureSettings.partyCommandsEnabled());
                return true;
            }
            if (event.button() == 1) {
                partyCommandsExpanded = !partyCommandsExpanded;
                return true;
            }
        }
        if (categories[0].open && partyCommandsExpanded && event.button() == 0) {
            int settingY = partyCommandsY + 22;
            for (int i = 0; i < partySettingKeys().length; i++) {
                if (inside(mouseX, mouseY, generalX, settingY + i * 14, panelWidth, 14)) {
                    if (i == 0) {
                        FeatureSettings.setPartyCommandEmotesEnabled(!FeatureSettings.partyCommandEmotesEnabled());
                    } else {
                        String key = partySettingKeys()[i];
                        FeatureSettings.setPartyCommandEnabled(key, !FeatureSettings.partyCommandEnabled(key));
                    }
                    return true;
                }
            }
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
        draggedSlider = null;
        draggingSettingsPopup = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingSettingsPopup) {
            settingsPopupX = Math.max(0, Math.min(width - 280, (int) event.x() + settingsDragOffsetX));
            settingsPopupY = Math.max(0, Math.min(height - 28, (int) event.y() + settingsDragOffsetY));
            repositionSettingFields();
            return true;
        }
        if (draggedSlider != null) {
            updateDraggedSlider(event.x());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
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
        if (settingsModule != null) renderSettingsPopup(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        if ("Cosmetics".equals(settingsModule) && !status.getString().isEmpty()) {
            graphics.centeredText(font, status, width / 2, popupY() + 260, 0xFFFFFFFF);
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

        if (renderFlatModules(graphics, category, mouseX, mouseY)) return;

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
                renderSettingsCard(graphics, x, moduleY + 22, panelWidth, 26);
                renderButton(graphics, x + 6, moduleY + 26, panelWidth - 12, copyChatModeName());
            }
            int partyY = moduleY + 22 + (copyChatExpanded ? 26 : 0);
            boolean partyEnabled = FeatureSettings.partyCommandsEnabled();
            graphics.fill(x, partyY, x + panelWidth, partyY + 22, ROW);
            graphics.fill(x, partyY, x + 2, partyY + 22,
                    partyEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Party Commands"), x + 7, partyY + 8,
                    partyEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            graphics.text(font, Component.literal(partyCommandsExpanded ? "-" : "+"),
                    x + panelWidth - 12, partyY + 8, TEXT_MUTED);
            if (partyCommandsExpanded) {
                String[] keys = partySettingKeys();
                String[] labels = partySettingLabels();
                int settingsY = partyY + 22;
                graphics.fill(x, settingsY, x + panelWidth, settingsY + keys.length * 14,
                        FeatureSettings.guiBody());
                for (int i = 0; i < keys.length; i++) {
                    boolean settingEnabled = i == 0
                            ? FeatureSettings.partyCommandEmotesEnabled()
                            : FeatureSettings.partyCommandEnabled(keys[i]);
                    int rowY = settingsY + i * 14;
                    graphics.fill(x, rowY, x + 2, rowY + 14,
                            settingEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
                    graphics.text(font, Component.literal(labels[i]), x + 6, rowY + 4,
                            settingEnabled ? 0xFFFFFFFF : TEXT_MUTED);
                    renderMiniSwitch(graphics, x + panelWidth - 34, rowY + 2, settingEnabled);
                }
            }
            return;
        }

        if (category.name.equals("Dungeons")) {
            int moduleY = y + 24;
            boolean enabled = FeatureSettings.highlightsEnabled();
            graphics.fill(x, moduleY, x + panelWidth, moduleY + 22, ROW);
            graphics.fill(x, moduleY, x + 2, moduleY + 22,
                    enabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Highlights"), x + 7, moduleY + 8,
                    enabled ? 0xFFFFFFFF : TEXT_MUTED);
            graphics.text(font, Component.literal(highlightsExpanded ? "-" : "+"),
                    x + panelWidth - 12, moduleY + 8, TEXT_MUTED);
            if (highlightsExpanded) {
                renderSettingsCard(graphics, x, moduleY + 22, panelWidth, 92);
                int controlY = moduleY + 26;
                renderButton(graphics, x + 6, controlY, panelWidth - 12, highlightStyleName());
                int color = FeatureSettings.highlightsColor();
                renderRgbSlider(graphics, x + 6, controlY + 22, panelWidth - 12,
                        "R", (color >> 16) & 255, 0xFFFF5555);
                renderRgbSlider(graphics, x + 6, controlY + 44, panelWidth - 12,
                        "G", (color >> 8) & 255, 0xFF55FF55);
                renderRgbSlider(graphics, x + 6, controlY + 66, panelWidth - 12,
                        "B", color & 255, 0xFF5599FF);
            }
            int lavaY = moduleY + 22 + (highlightsExpanded ? 92 : 0);
            boolean lavaEnabled = FeatureSettings.lavaToWaterEnabled();
            graphics.fill(x, lavaY, x + panelWidth, lavaY + 22, ROW);
            graphics.fill(x, lavaY, x + 2, lavaY + 22,
                    lavaEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Lava to Water"), x + 7, lavaY + 8,
                    lavaEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            int finishSongY = lavaY + 22;
            boolean finishSongEnabled = FeatureSettings.dungeonFinishSongEnabled();
            graphics.fill(x, finishSongY, x + panelWidth, finishSongY + 22, ROW);
            graphics.fill(x, finishSongY, x + 2, finishSongY + 22,
                    finishSongEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Dungeon Finish Song"), x + 7, finishSongY + 8,
                    finishSongEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            int leapY = finishSongY + 22;
            boolean leapEnabled = FeatureSettings.leapAlertEnabled();
            graphics.fill(x, leapY, x + panelWidth, leapY + 22, ROW);
            graphics.fill(x, leapY, x + 2, leapY + 22,
                    leapEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Leap Alert"), x + 7, leapY + 8,
                    leapEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            int mapY = leapY + 22;
            boolean mapEnabled = FeatureSettings.dungeonMapEnabled();
            graphics.fill(x, mapY, x + panelWidth, mapY + 22, ROW);
            graphics.fill(x, mapY, x + 2, mapY + 22,
                    mapEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Dungeon Map"), x + 7, mapY + 8,
                    mapEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            graphics.text(font, Component.literal(dungeonMapExpanded ? "-" : "+"),
                    x + panelWidth - 12, mapY + 8, TEXT_MUTED);
            if (dungeonMapExpanded) {
                renderSettingsCard(graphics, x, mapY + 22, panelWidth, 70);
                int controlY = mapY + 26;
                renderScaleSlider(graphics, x + 6, controlY, panelWidth - 12);
                int spinY = controlY + 22;
                graphics.fill(x + 6, spinY, x + panelWidth - 6, spinY + 18, 0xFF202128);
                graphics.text(font, Component.literal("Spinny"), x + 12, spinY + 6, 0xFFFFFFFF);
                renderMiniSwitch(graphics, x + panelWidth - 38, spinY + 4,
                        FeatureSettings.dungeonMapSpinny());
                int clearBgY = controlY + 44;
                graphics.fill(x + 6, clearBgY, x + panelWidth - 6, clearBgY + 18, 0xFF202128);
                graphics.text(font, Component.literal("Clear Background"), x + 12, clearBgY + 6, 0xFFFFFFFF);
                renderMiniSwitch(graphics, x + panelWidth - 38, clearBgY + 4,
                        FeatureSettings.dungeonMapClearBackground());
            }
            int clearY = mapY + 22 + (dungeonMapExpanded ? 70 : 0);
            boolean clearEnabled = FeatureSettings.roomClearEnabled();
            graphics.fill(x, clearY, x + panelWidth, clearY + 22, ROW);
            graphics.fill(x, clearY, x + 2, clearY + 22,
                    clearEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Room Clear"), x + 7, clearY + 8,
                    clearEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            graphics.text(font, Component.literal(roomClearExpanded ? "-" : "+"),
                    x + panelWidth - 12, clearY + 8, TEXT_MUTED);
            if (roomClearExpanded) {
                renderSettingsCard(graphics, x, clearY + 22, panelWidth, 26);
                renderButton(graphics, x + 6, clearY + 26, panelWidth - 12, roomClearModeName());
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
                renderSettingsCard(graphics, x, melodyY + 22, panelWidth, 36);
                graphics.text(font, Component.literal("Message"), x + 6, melodyY + 26, TEXT_MUTED);
            }
            int terminalsY = melodyY + 22 + (melodyAlertExpanded ? 36 : 0);
            boolean terminalsEnabled = FeatureSettings.terminalsGuiEnabled();
            graphics.fill(x, terminalsY, x + panelWidth, terminalsY + 22, ROW);
            graphics.fill(x, terminalsY, x + 2, terminalsY + 22,
                    terminalsEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("Terminals GUI"), x + 7, terminalsY + 8,
                    terminalsEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            graphics.text(font, Component.literal(terminalsGuiExpanded ? "-" : "+"),
                    x + panelWidth - 12, terminalsY + 8, TEXT_MUTED);
            if (terminalsGuiExpanded) {
                renderSettingsCard(graphics, x, terminalsY + 22, panelWidth, 26);
                int clearY = terminalsY + 26;
                graphics.fill(x + 6, clearY, x + panelWidth - 6, clearY + 18, 0xFF202128);
                graphics.text(font, Component.literal("Clear Background"), x + 12, clearY + 6, 0xFFFFFFFF);
                renderMiniSwitch(graphics, x + panelWidth - 38, clearY + 4,
                        FeatureSettings.terminalsGuiClearBackground());
            }
            int threeByThreeY = terminalsY + 22 + (terminalsGuiExpanded ? 26 : 0);
            boolean threeByThreeEnabled = FeatureSettings.threeByThreeHighlightsEnabled();
            graphics.fill(x, threeByThreeY, x + panelWidth, threeByThreeY + 22, ROW);
            graphics.fill(x, threeByThreeY, x + 2, threeByThreeY + 22,
                    threeByThreeEnabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal("3x3 Highlights"), x + 7, threeByThreeY + 8,
                    threeByThreeEnabled ? 0xFFFFFFFF : TEXT_MUTED);
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
            renderSettingsCard(graphics, x, moduleY + 22, panelWidth, cosmeticsExtra);
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
            renderSettingsCard(graphics, x, clickGuiY + 22, panelWidth, clickExtra);
            int controlY = clickGuiY + 26;
            String target = switch (colorTarget) { case 1 -> "Header"; case 2 -> "Body"; default -> "Accent"; };
            renderButton(graphics, x + 6, controlY, panelWidth - 12, target);
            int color = selectedGuiColor();
            renderRgbSlider(graphics, x + 6, controlY + 22, panelWidth - 12, "R", (color >> 16) & 255, 0xFFFF5555);
            renderRgbSlider(graphics, x + 6, controlY + 44, panelWidth - 12, "G", (color >> 8) & 255, 0xFF55FF55);
            renderRgbSlider(graphics, x + 6, controlY + 66, panelWidth - 12, "B", color & 255, 0xFF5599FF);
        }
    }

    private boolean renderFlatModules(GuiGraphicsExtractor graphics, Category category, int mouseX, int mouseY) {
        int rowY = category.y + 24;
        for (String module : modulesFor(category.name)) {
            boolean enabled = moduleEnabled(module);
            boolean hovered = inside(mouseX, mouseY, category.x, rowY, panelWidth, 22);
            graphics.fill(category.x, rowY, category.x + panelWidth, rowY + 22, hovered ? 0xFF22242D : ROW);
            graphics.fill(category.x, rowY, category.x + 2, rowY + 22,
                    enabled ? FeatureSettings.guiAccent() : 0xFF343640);
            graphics.text(font, Component.literal(displayModuleName(module)), category.x + 7, rowY + 8,
                    enabled ? 0xFFFFFFFF : TEXT_MUTED);
            rowY += 22;
        }
        return true;
    }

    private int popupX() { return settingsPopupX >= 0 ? settingsPopupX : width / 2 - 140; }
    private int popupY() { return settingsPopupY >= 0 ? settingsPopupY : Math.max(22, height / 2 - 135); }

    private void renderSettingsPopup(GuiGraphicsExtractor graphics) {
        int x = popupX(), y = popupY(), w = 280;
        int h = "Party Commands".equals(settingsModule) ? 286 : "Cosmetics".equals(settingsModule) ? 276 : 150;
        graphics.fill(x, y, x + w, y + h, 0xFC111219);
        graphics.fill(x, y, x + w, y + 28, FeatureSettings.guiHeader());
        graphics.fill(x, y + 26, x + w, y + 28, FeatureSettings.guiAccent());
        graphics.text(font, Component.literal(settingsModule + " Settings").withStyle(ChatFormatting.BOLD),
                x + 10, y + 10, 0xFFFFFFFF);
        graphics.text(font, Component.literal("×"), x + w - 17, y + 9, TEXT_MUTED);
        int cx = x + 12, cy = y + 38, cw = w - 24;
        switch (settingsModule) {
            case "Copy Chat" -> renderButton(graphics, cx, cy, cw, copyChatModeName());
            case "Highlights" -> {
                renderButton(graphics, cx, cy, cw, highlightStyleName());
                int c = FeatureSettings.highlightsColor();
                renderRgbSlider(graphics, cx, cy + 24, cw, "R", (c >> 16) & 255, 0xFFFF5555);
                renderRgbSlider(graphics, cx, cy + 52, cw, "G", (c >> 8) & 255, 0xFF55FF55);
                renderRgbSlider(graphics, cx, cy + 80, cw, "B", c & 255, 0xFF5599FF);
            }
            case "Dungeon Map" -> {
                renderScaleSlider(graphics, cx, cy, cw);
                renderToggle(graphics, cx, cy + 22, cw, "Spinny", FeatureSettings.dungeonMapSpinny());
                renderToggle(graphics, cx, cy + 44, cw, "Clear Background", FeatureSettings.dungeonMapClearBackground());
            }
            case "Room Clear" -> renderButton(graphics, cx, cy, cw, roomClearModeName());
            case "Melody Alert" -> graphics.text(font, Component.literal("Party chat message"), cx, cy + 22, TEXT_MUTED);
            case "Terminals GUI" -> renderToggle(graphics, cx, cy, cw, "Clear Background",
                    FeatureSettings.terminalsGuiClearBackground());
            case "Party Commands" -> {
                String[] keys = partySettingKeys(), labels = partySettingLabels();
                for (int i = 0; i < keys.length; i++) {
                    boolean value = i == 0 ? FeatureSettings.partyCommandEmotesEnabled()
                            : FeatureSettings.partyCommandEnabled(keys[i]);
                    renderToggle(graphics, cx, cy + i * 14, cw, labels[i], value);
                }
            }
            case "Click GUI" -> {
                String target = switch (colorTarget) { case 1 -> "Header"; case 2 -> "Body"; default -> "Accent"; };
                renderButton(graphics, cx, cy, cw, target);
                int c = selectedGuiColor();
                renderRgbSlider(graphics, cx, cy + 24, cw, "R", (c >> 16) & 255, 0xFFFF5555);
                renderRgbSlider(graphics, cx, cy + 52, cw, "G", (c >> 8) & 255, 0xFF55FF55);
                renderRgbSlider(graphics, cx, cy + 80, cw, "B", c & 255, 0xFF5599FF);
            }
            case "Cosmetics" -> {
                graphics.text(font, Component.literal("Nickname / gradient"), cx, cy + 75, TEXT_MUTED);
                renderToggle(graphics, cx, cy + 97, cw, "Bold", bold);
                renderToggle(graphics, cx, cy + 119, cw, "Italic", italic);
                renderSlider(graphics, cx, cy + 141, cw);
                renderButton(graphics, cx, cy + 185, cw, "Save & sync");
            }
        }
    }

    private boolean handleSettingsPopupClick(double mx, double my, int button) {
        int x = popupX(), y = popupY(), w = 280, cx = x + 12, cy = y + 38, cw = w - 24;
        int h = "Party Commands".equals(settingsModule) ? 286 : "Cosmetics".equals(settingsModule) ? 276 : 150;
        if (!inside(mx, my, x, y, w, h) || inside(mx, my, x + w - 28, y, 28, 28)) {
            settingsModule = null; updateWidgetVisibility(); return true;
        }
        if (button != 0) return true;
        switch (settingsModule) {
            case "Copy Chat" -> { if (inside(mx, my, cx, cy, cw, 18)) FeatureSettings.setCopyChatMode((FeatureSettings.copyChatMode() + 1) % 3); }
            case "Highlights" -> {
                if (inside(mx, my, cx, cy, cw, 18)) FeatureSettings.setHighlightsStyle((FeatureSettings.highlightsStyle() + 1) % 3);
                for (int i = 0; i < 3; i++) if (inside(mx, my, cx, cy + 24 + i * 28, cw, 22)) {
                    draggedSlider = "highlight"; draggedSliderChannel = i; updateDraggedSlider(mx);
                }
            }
            case "Dungeon Map" -> {
                if (inside(mx, my, cx, cy, cw, 22)) {
                    draggedSlider = "dungeon_map"; updateDraggedSlider(mx);
                }
                else if (inside(mx, my, cx, cy + 22, cw, 18)) FeatureSettings.setDungeonMapSpinny(!FeatureSettings.dungeonMapSpinny());
                else if (inside(mx, my, cx, cy + 44, cw, 18)) FeatureSettings.setDungeonMapClearBackground(!FeatureSettings.dungeonMapClearBackground());
            }
            case "Room Clear" -> { if (inside(mx, my, cx, cy, cw, 18)) FeatureSettings.setRoomClearMode((FeatureSettings.roomClearMode() + 1) % 3); }
            case "Terminals GUI" -> { if (inside(mx, my, cx, cy, cw, 18)) FeatureSettings.setTerminalsGuiClearBackground(!FeatureSettings.terminalsGuiClearBackground()); }
            case "Party Commands" -> {
                for (int i = 0; i < partySettingKeys().length; i++) if (inside(mx, my, cx, cy + i * 14, cw, 14)) {
                    if (i == 0) FeatureSettings.setPartyCommandEmotesEnabled(!FeatureSettings.partyCommandEmotesEnabled());
                    else { String key = partySettingKeys()[i]; FeatureSettings.setPartyCommandEnabled(key, !FeatureSettings.partyCommandEnabled(key)); }
                }
            }
            case "Click GUI" -> {
                if (inside(mx, my, cx, cy, cw, 18)) colorTarget = (colorTarget + 1) % 3;
                for (int i = 0; i < 3; i++) if (inside(mx, my, cx, cy + 24 + i * 28, cw, 22)) {
                    draggedSlider = "click_gui"; draggedSliderChannel = i; updateDraggedSlider(mx);
                }
            }
            case "Cosmetics" -> {
                if (inside(mx, my, cx, cy + 97, cw, 18)) bold = !bold;
                else if (inside(mx, my, cx, cy + 119, cw, 18)) italic = !italic;
                else if (inside(mx, my, cx, cy + 141, cw, 24)) {
                    draggedSlider = "cosmetics"; updateDraggedSlider(mx);
                }
                else if (inside(mx, my, cx, cy + 185, cw, 18)) save();
            }
        }
        return true;
    }

    private int sliderValue(double mouseX, int x, int width, int max) {
        return (int) Math.round(Math.max(0, Math.min(1, (mouseX - x) / width)) * max);
    }

    private void updateDraggedSlider(double mouseX) {
        int x = popupX() + 12, w = 256;
        switch (draggedSlider) {
            case "highlight" -> setHighlightColorChannel(draggedSliderChannel, sliderValue(mouseX, x, w, 255));
            case "click_gui" -> setSelectedColorChannel(draggedSliderChannel, sliderValue(mouseX, x, w, 255));
            case "dungeon_map" -> FeatureSettings.setDungeonMapScale(50 + sliderValue(mouseX, x, w, 150));
            case "cosmetics" -> visualHeight = 0.5f + sliderValue(mouseX, x, w, 150) / 100f;
        }
    }

    private boolean hasSettings(String module) {
        return switch (module) {
            case "Copy Chat", "Party Commands", "Highlights", "Dungeon Map", "Room Clear",
                    "Melody Alert", "Terminals GUI", "Cosmetics", "Click GUI" -> true;
            default -> false;
        };
    }

    private void clearLegacyExpansions() {
        cosmeticsExpanded = melodyAlertExpanded = terminalsGuiExpanded = clickGuiExpanded = false;
        copyChatExpanded = highlightsExpanded = dungeonMapExpanded = roomClearExpanded = partyCommandsExpanded = false;
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
        graphics.text(font, Component.literal(label), x + 6, y + 6,
                enabled ? 0xFFFFFFFF : TEXT_MUTED);
        renderMiniSwitch(graphics, x + width - 32, y + 4, enabled);
    }

    private void renderSettingsCard(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x + 3, y + 3, x + width + 3, y + height + 3, 0x78000000);
        graphics.fill(x, y, x + width, y + height, 0xFA15161D);
        graphics.fill(x, y, x + width, y + 2, FeatureSettings.guiAccent());
        graphics.fill(x, y, x + 1, y + height, 0xFF30323C);
        graphics.fill(x + width - 1, y, x + width, y + height, 0xFF30323C);
    }

    private void renderMiniSwitch(GuiGraphicsExtractor graphics, int x, int y, boolean enabled) {
        int track = enabled ? FeatureSettings.guiAccent() : 0xFF3B3D47;
        graphics.fill(x, y, x + 26, y + 10, track);
        int knobX = enabled ? x + 17 : x + 1;
        graphics.fill(knobX, y + 1, knobX + 8, y + 9, 0xFFF2F2F4);
    }

    private void renderSlider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        double progress = (visualHeight - 0.5) / 1.5;
        int fillWidth = (int) Math.round(width * Math.max(0, Math.min(1, progress)));
        graphics.text(font, Component.literal(String.format("Scale %.2fx", visualHeight)),
                x, y + 4, 0xFFD8D9DF);
        renderSliderTrack(graphics, x, y + 16, width, fillWidth, FeatureSettings.guiAccent());
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
        graphics.text(font, Component.literal(label + "  " + value), x, y + 4, 0xFFFFFFFF);
        renderSliderTrack(graphics, x, y + 16, width, fillWidth, sliderColor);
    }

    private void renderScaleSlider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        int value = FeatureSettings.dungeonMapScale();
        int fillWidth = (int) Math.round(width * ((value - 50) / 150.0));
        graphics.text(font, Component.literal("Scale  " + value + "%"), x, y + 4, 0xFFFFFFFF);
        renderSliderTrack(graphics, x, y + 16, width, fillWidth, FeatureSettings.guiAccent());
    }

    private void renderSliderTrack(GuiGraphicsExtractor graphics, int x, int y, int width,
                                   int fillWidth, int color) {
        graphics.fill(x, y, x + width, y + 3, 0xFF3A3C46);
        graphics.fill(x, y, x + fillWidth, y + 3, color);
        int knob = x + Math.max(0, Math.min(width - 6, fillWidth - 3));
        graphics.fill(knob, y - 2, knob + 6, y + 5, 0xFFFFFFFF);
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

    private String[] partySettingKeys() {
        return new String[]{"emotes", "help", "coords", "cf", "8ball", "dice", "fps", "time",
                "holding", "warp", "allinvite", "pt", "promote", "demote", "kick", "kickoffline", "boop"};
    }

    private String[] partySettingLabels() {
        return new String[]{"Enable Emotes", "Help", "Coords", "Coinflip", "8ball", "Dice", "FPS",
                "Time", "Holding", "Warp", "Allinvite", "Transfer", "Promote", "Demote", "Kick",
                "Kick Offline", "Boop"};
    }

    private String highlightStyleName() {
        return switch (FeatureSettings.highlightsStyle()) {
            case 0 -> "Filled";
            case 2 -> "Filled Outline";
            default -> "Outline";
        };
    }

    private String roomClearModeName() {
        return switch (FeatureSettings.roomClearMode()) {
            case 1 -> "Mode: Green";
            case 2 -> "Mode: White";
            default -> "Mode: Both";
        };
    }

    private void setHighlightColorChannel(int channel, int value) {
        int color = FeatureSettings.highlightsColor();
        int red = (color >> 16) & 255;
        int green = (color >> 8) & 255;
        int blue = color & 255;
        if (channel == 0) red = value;
        else if (channel == 1) green = value;
        else blue = value;
        FeatureSettings.setHighlightsColor((red << 16) | (green << 8) | blue);
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
            case "General" -> new String[]{"Copy Chat", "Party Commands"};
            case "Dungeons" -> new String[]{"Highlights", "Lava to Water", "Diorite To Glass", "Dungeon Finish Song", "Leap Alert", "Dungeon Map", "Room Clear"};
            case "Floor 7" -> new String[]{"Terminal Waypoints", "Terminal Solver", "Device Solver",
                    "Melody Alert", "Terminals GUI", "3x3 Highlights"};
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
            case "Party Commands" -> FeatureSettings.partyCommandsEnabled();
            case "Highlights" -> FeatureSettings.highlightsEnabled();
            case "Lava to Water" -> FeatureSettings.lavaToWaterEnabled();
            case "Diorite To Glass" -> FeatureSettings.dioriteToGlassEnabled();
            case "Dungeon Finish Song" -> FeatureSettings.dungeonFinishSongEnabled();
            case "Leap Alert" -> FeatureSettings.leapAlertEnabled();
            case "Dungeon Map" -> FeatureSettings.dungeonMapEnabled();
            case "Room Clear" -> FeatureSettings.roomClearEnabled();
            case "Terminal Waypoints" -> FeatureSettings.terminalWaypointsEnabled();
            case "Terminal Solver" -> FeatureSettings.terminalSolverEnabled();
            case "Device Solver" -> FeatureSettings.deviceSolverEnabled();
            case "Melody Alert" -> FeatureSettings.melodyAlertEnabled();
            case "Terminals GUI" -> FeatureSettings.terminalsGuiEnabled();
            case "3x3 Highlights" -> FeatureSettings.threeByThreeHighlightsEnabled();
            case "Disable Fire" -> FeatureSettings.disableFireEnabled();
            case "Cosmetics" -> cosmeticsEnabled;
            default -> true;
        };
    }

    private void activateSearchResult(String module) {
        switch (module) {
            case "Copy Chat" -> FeatureSettings.setCopyChatEnabled(!FeatureSettings.copyChatEnabled());
            case "Party Commands" -> FeatureSettings.setPartyCommandsEnabled(!FeatureSettings.partyCommandsEnabled());
            case "Highlights" -> FeatureSettings.setHighlightsEnabled(!FeatureSettings.highlightsEnabled());
            case "Lava to Water" -> FeatureSettings.setLavaToWaterEnabled(!FeatureSettings.lavaToWaterEnabled());
            case "Diorite To Glass" -> FeatureSettings.setDioriteToGlassEnabled(!FeatureSettings.dioriteToGlassEnabled());
            case "Dungeon Finish Song" -> FeatureSettings.setDungeonFinishSongEnabled(
                    !FeatureSettings.dungeonFinishSongEnabled());
            case "Leap Alert" -> FeatureSettings.setLeapAlertEnabled(!FeatureSettings.leapAlertEnabled());
            case "Dungeon Map" -> FeatureSettings.setDungeonMapEnabled(!FeatureSettings.dungeonMapEnabled());
            case "Room Clear" -> FeatureSettings.setRoomClearEnabled(!FeatureSettings.roomClearEnabled());
            case "Terminal Waypoints" ->
                    FeatureSettings.setTerminalWaypointsEnabled(!FeatureSettings.terminalWaypointsEnabled());
            case "Terminal Solver" ->
                    FeatureSettings.setTerminalSolverEnabled(!FeatureSettings.terminalSolverEnabled());
            case "Device Solver" -> FeatureSettings.setDeviceSolverEnabled(!FeatureSettings.deviceSolverEnabled());
            case "Melody Alert" -> FeatureSettings.setMelodyAlertEnabled(!FeatureSettings.melodyAlertEnabled());
            case "Terminals GUI" -> FeatureSettings.setTerminalsGuiEnabled(!FeatureSettings.terminalsGuiEnabled());
            case "3x3 Highlights" -> FeatureSettings.setThreeByThreeHighlightsEnabled(
                    !FeatureSettings.threeByThreeHighlightsEnabled());
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
        int x = popupX() + 12;
        int y = popupY() + 38;
        int fieldWidth = 256;
        nickField.setX(x);
        nickField.setY(y);
        nickField.setWidth(fieldWidth);
        startColorField.setX(x);
        startColorField.setY(y + 31);
        startColorField.setWidth(fieldWidth);
        endColorField.setX(x);
        endColorField.setY(y + 53);
        endColorField.setWidth(fieldWidth);
        melodyMessageField.setX(x);
        melodyMessageField.setY(y);
        melodyMessageField.setWidth(fieldWidth);
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
