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
        boolean visible = categories[4].open && cosmeticsExpanded;
        nickField.visible = visible;
        startColorField.visible = visible;
        endColorField.visible = visible;
        melodyMessageField.visible = categories[2].open && melodyAlertExpanded;
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
        graphics.fill(x, y, x + panelWidth, y + 24, hover ? ACCENT_DARK : HEADER);
        graphics.fill(x, y + 22, x + panelWidth, y + 24, ACCENT);
        graphics.text(font, Component.literal(category.name).withStyle(ChatFormatting.BOLD),
                x + 7, y + 8, 0xFFFFFFFF);
        graphics.text(font, Component.literal(category.open ? "-" : "+"),
                x + panelWidth - 12, y + 8, 0xFFFFFFFF);

        if (!category.open) return;

        if (category.name.equals("Floor 7")) {
            int moduleY = y + 24;
            boolean moduleHover = inside(mouseX, mouseY, x, moduleY, panelWidth, 22);
            boolean enabled = FeatureSettings.terminalWaypointsEnabled();
            graphics.fill(x, moduleY, x + panelWidth, moduleY + 22, moduleHover ? 0xFF22242D : ROW);
            graphics.fill(x, moduleY, x + 2, moduleY + 22, enabled ? ACCENT : 0xFF343640);
            graphics.text(font, Component.literal("Terminal WPs"), x + 7, moduleY + 8,
                    enabled ? 0xFFFFFFFF : TEXT_MUTED);
            int solverY = moduleY + 22;
            boolean solverHover = inside(mouseX, mouseY, x, solverY, panelWidth, 22);
            boolean solverEnabled = FeatureSettings.terminalSolverEnabled();
            graphics.fill(x, solverY, x + panelWidth, solverY + 22, solverHover ? 0xFF22242D : ROW);
            graphics.fill(x, solverY, x + 2, solverY + 22, solverEnabled ? ACCENT : 0xFF343640);
            graphics.text(font, Component.literal("Terminal Solver"), x + 7, solverY + 8,
                    solverEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            int deviceY = solverY + 22;
            boolean deviceHover = inside(mouseX, mouseY, x, deviceY, panelWidth, 22);
            boolean deviceEnabled = FeatureSettings.deviceSolverEnabled();
            graphics.fill(x, deviceY, x + panelWidth, deviceY + 22, deviceHover ? 0xFF22242D : ROW);
            graphics.fill(x, deviceY, x + 2, deviceY + 22, deviceEnabled ? ACCENT : 0xFF343640);
            graphics.text(font, Component.literal("Device Solver"), x + 7, deviceY + 8,
                    deviceEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            int melodyY = deviceY + 22;
            boolean melodyHover = inside(mouseX, mouseY, x, melodyY, panelWidth, 22);
            boolean melodyEnabled = FeatureSettings.melodyAlertEnabled();
            graphics.fill(x, melodyY, x + panelWidth, melodyY + 22, melodyHover ? 0xFF22242D : ROW);
            graphics.fill(x, melodyY, x + 2, melodyY + 22, melodyEnabled ? ACCENT : 0xFF343640);
            graphics.text(font, Component.literal("Melody Alert"), x + 7, melodyY + 8,
                    melodyEnabled ? 0xFFFFFFFF : TEXT_MUTED);
            graphics.text(font, Component.literal(melodyAlertExpanded ? "-" : "+"),
                    x + panelWidth - 12, melodyY + 8, TEXT_MUTED);
            if (melodyAlertExpanded) {
                graphics.fill(x, melodyY + 22, x + panelWidth, melodyY + 58, BODY);
                graphics.text(font, Component.literal("Message"), x + 6, melodyY + 26, TEXT_MUTED);
            }
            return;
        }

        if (!category.name.equals("Misc")) {
            graphics.fill(x, y + 24, x + panelWidth, y + 44, BODY);
            graphics.centeredText(font, Component.literal("No modules"), x + panelWidth / 2,
                    y + 31, 0xFF555761);
            return;
        }

        int moduleY = y + 24;
        boolean moduleHover = inside(mouseX, mouseY, x, moduleY, panelWidth, 22);
        graphics.fill(x, moduleY, x + panelWidth, moduleY + 22, moduleHover ? 0xFF22242D : ROW);
        graphics.fill(x, moduleY, x + 2, moduleY + 22, cosmeticsEnabled ? ACCENT : 0xFF343640);
        graphics.text(font, Component.literal("Cosmetics"), x + 7, moduleY + 8,
                cosmeticsEnabled ? 0xFFFFFFFF : TEXT_MUTED);
        graphics.text(font, Component.literal(cosmeticsExpanded ? "-" : "+"),
                x + panelWidth - 12, moduleY + 8, TEXT_MUTED);

        if (cosmeticsExpanded) {
            graphics.fill(x, moduleY + 22, x + panelWidth, y + 280, BODY);
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
        graphics.fill(x, y, x + 2, y + 18, enabled ? ACCENT : 0xFF4B4D58);
        graphics.text(font, Component.literal(label), x + 6, y + 6,
                enabled ? 0xFFFFFFFF : TEXT_MUTED);
        String state = enabled ? "ON" : "OFF";
        graphics.text(font, Component.literal(state), x + width - font.width(state) - 5, y + 6,
                enabled ? ACCENT : 0xFF686A74);
    }

    private void renderSlider(GuiGraphicsExtractor graphics, int x, int y, int width) {
        double progress = (visualHeight - 0.5) / 1.5;
        int fillWidth = (int) Math.round(width * Math.max(0, Math.min(1, progress)));
        graphics.fill(x, y, x + width, y + 18, 0xFF202128);
        graphics.fill(x, y + 16, x + width, y + 18, 0xFF363842);
        graphics.fill(x, y + 16, x + fillWidth, y + 18, ACCENT);
        graphics.text(font, Component.literal(String.format("Scale %.2fx", visualHeight)),
                x + 6, y + 6, 0xFFD8D9DF);
    }

    private void renderButton(GuiGraphicsExtractor graphics, int x, int y, int width, String label) {
        graphics.fill(x, y, x + width, y + 18, syncing ? 0xFF383943 : ACCENT_DARK);
        graphics.fill(x, y, x + width, y + 1, syncing ? 0xFF555762 : ACCENT);
        graphics.centeredText(font, Component.literal(syncing ? "Syncing..." : label),
                x + width / 2, y + 6, syncing ? TEXT_MUTED : 0xFFFFFFFF);
    }

    private boolean matchesSearch(String moduleName) {
        return searchField == null || searchField.getValue().isBlank()
                || moduleName.toLowerCase().contains(searchField.getValue().trim().toLowerCase());
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
