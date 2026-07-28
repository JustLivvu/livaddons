package net.livaddons.gui;

import net.livaddons.data.PlayerCosmeticData;
import net.livaddons.data.PlayerDataManager;
import net.livaddons.net.ApiClient;
import net.livaddons.util.TextGradientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class LivAddonsScreen extends Screen {

    private static final String GITHUB_URL = "https://github.com/justlivvu/livaddons";
    private static final String DISCORD_URL = "https://discord.gg/B4aNfKPpuG";

    private EditBox customNickField;
    private EditBox colorStartField;
    private EditBox colorEndField;

    private boolean isBold = false;
    private boolean isItalic = false;
    private float visualHeight = 1.0f;
    private final List<String> activeCosmetics = new ArrayList<>();

    private Button boldButton;
    private Button italicButton;
    private HeightSlider heightSlider;
    private Button saveButton;

    private Component statusMessage = Component.empty();

    // Click bounds for links (populated at render time)
    private int githubX, githubY, githubW, githubH;
    private int discordX, discordY, discordW, discordH;

    public LivAddonsScreen() {
        super(Component.literal("LivAddons Cosmetics"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 60;

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            PlayerCosmeticData currentData = PlayerDataManager.getInstance().getCosmeticData(client.player.getUUID());
            if (currentData != null) {
                this.isBold = currentData.isBold;
                this.isItalic = currentData.isItalic;
                this.visualHeight = currentData.visualHeight;
                if (currentData.cosmetics != null) {
                    this.activeCosmetics.clear();
                    this.activeCosmetics.addAll(currentData.cosmetics);
                }
            }
        }

        this.customNickField = new EditBox(
                this.font,
                centerX - 100,
                startY + 15,
                200,
                20,
                Component.literal("Custom Nickname")
        );
        this.customNickField.setMaxLength(20);
        if (client.player != null) {
            PlayerCosmeticData currentData = PlayerDataManager.getInstance().getCosmeticData(client.player.getUUID());
            if (currentData != null && currentData.customNick != null) {
                String nick = currentData.customNick;
                if (nick.length() > 20) nick = nick.substring(0, 20);
                this.customNickField.setValue(nick);
            } else {
                String name = client.player.getName().getString();
                if (name.length() > 20) name = name.substring(0, 20);
                this.customNickField.setValue(name);
            }
        }
        this.addRenderableWidget(this.customNickField);

        this.colorStartField = new EditBox(
                this.font,
                centerX - 100,
                startY + 55,
                95,
                20,
                Component.literal("Start Hex")
        );
        this.colorStartField.setMaxLength(7);
        this.colorStartField.setValue("#FF5555");

        this.colorEndField = new EditBox(
                this.font,
                centerX + 5,
                startY + 55,
                95,
                20,
                Component.literal("End Hex")
        );
        this.colorEndField.setMaxLength(7);
        this.colorEndField.setValue("#55FFFF");

        if (client.player != null) {
            PlayerCosmeticData currentData = PlayerDataManager.getInstance().getCosmeticData(client.player.getUUID());
            if (currentData != null) {
                if (currentData.colorStart != null) this.colorStartField.setValue(currentData.colorStart);
                if (currentData.colorEnd != null) this.colorEndField.setValue(currentData.colorEnd);
            }
        }

        this.addRenderableWidget(this.colorStartField);
        this.addRenderableWidget(this.colorEndField);

        this.boldButton = Button.builder(
                Component.literal("Bold: " + (isBold ? "YES" : "NO")),
                button -> {
                    isBold = !isBold;
                    button.setMessage(Component.literal("Bold: " + (isBold ? "YES" : "NO")));
                }
        ).bounds(centerX - 100, startY + 85, 95, 20).build();
        this.addRenderableWidget(this.boldButton);

        this.italicButton = Button.builder(
                Component.literal("Italic: " + (isItalic ? "YES" : "NO")),
                button -> {
                    isItalic = !isItalic;
                    button.setMessage(Component.literal("Italic: " + (isItalic ? "YES" : "NO")));
                }
        ).bounds(centerX + 5, startY + 85, 95, 20).build();
        this.addRenderableWidget(this.italicButton);

        double initialProgress = (visualHeight - 0.5f) / 1.5f;
        this.heightSlider = new HeightSlider(centerX - 100, startY + 115, 200, 20, initialProgress);
        this.addRenderableWidget(this.heightSlider);

        this.saveButton = Button.builder(
                Component.literal("Save & Sync"),
                button -> saveAndSync()
        ).bounds(centerX - 100, startY + 155, 200, 20).build();
        this.addRenderableWidget(this.saveButton);
    }

    private void saveAndSync() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        String uuid = client.player.getUUID().toString();
        String username = client.player.getName().getString();
        String customNick = this.customNickField.getValue();
        if (customNick.length() > 20) {
            customNick = customNick.substring(0, 20);
        }

        String colorStart = this.colorStartField.getValue();
        String colorEnd = this.colorEndField.getValue();
        float height = this.heightSlider.getHeightValue();

        PlayerCosmeticData newData = new PlayerCosmeticData(
                uuid,
                username,
                customNick,
                colorStart,
                colorEnd,
                this.isBold,
                this.isItalic,
                height,
                this.activeCosmetics
        );

        this.statusMessage = Component.literal("Saving...");
        this.saveButton.active = false;

        ApiClient.syncProfile(newData).thenAccept(success -> {
            client.execute(() -> {
                this.saveButton.active = true;
                if (success) {
                    PlayerDataManager.getInstance().updateCache(newData);
                    this.statusMessage = Component.literal("§aSaved Successfully");
                } else {
                    this.statusMessage = Component.literal("§cConnection error. Try again.");
                }
            });
        });
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        double mouseX = event.x();
        double mouseY = event.y();
        if (event.button() == 0) {
            if (mouseX >= githubX && mouseX <= githubX + githubW && mouseY >= githubY && mouseY <= githubY + githubH) {
                Screen.clickUrlAction(Minecraft.getInstance(), this, URI.create(GITHUB_URL));
                return true;
            }
            if (mouseX >= discordX && mouseX <= discordX + discordW && mouseY >= discordY && mouseY <= discordY + discordH) {
                Screen.clickUrlAction(Minecraft.getInstance(), this, URI.create(DISCORD_URL));
                return true;
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int centerX = this.width / 2;
        int cardWidth = 240;
        int cardHeight = 240;
        int cardLeft = centerX - cardWidth / 2;
        int cardTop = 15;

        graphics.fill(0, 0, this.width, this.height, 0x88000000);
        graphics.fill(cardLeft - 1, cardTop - 1, cardLeft + cardWidth + 1, cardTop + cardHeight + 1, 0x44FFFFFF);
        graphics.fill(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight, 0xF012131A);

        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int startY = 60;

        graphics.centeredText(this.font, Component.literal("§fLivAddons"), centerX, cardTop + 10, 0xFFFFFFFF);

        // Links row: GitHub | Discord
        String githubLabel = "§nGitHub";
        String discordLabel = "§nDiscord";
        int githubTxtW = this.font.width("GitHub");
        int discordTxtW = this.font.width("Discord");
        int gap = 12;
        int totalLinksW = githubTxtW + gap + discordTxtW;
        int linksStartX = centerX - totalLinksW / 2;
        int linksY = cardTop + 23;

        boolean overGithub = mouseX >= linksStartX && mouseX <= linksStartX + githubTxtW && mouseY >= linksY && mouseY <= linksY + 9;
        boolean overDiscord = mouseX >= linksStartX + githubTxtW + gap && mouseX <= linksStartX + githubTxtW + gap + discordTxtW && mouseY >= linksY && mouseY <= linksY + 9;

        graphics.text(this.font, Component.literal(githubLabel), linksStartX, linksY, overGithub ? 0xFFAAAAFF : 0xFF8888FF);
        graphics.text(this.font, Component.literal("§7|"), linksStartX + githubTxtW + 4, linksY, 0xFF555555);
        graphics.text(this.font, Component.literal(discordLabel), linksStartX + githubTxtW + gap, linksY, overDiscord ? 0xFFBBAAFF : 0xFF9B84EC);

        // Store click bounds
        githubX = linksStartX; githubY = linksY; githubW = githubTxtW; githubH = 9;
        discordX = linksStartX + githubTxtW + gap; discordY = linksY; discordW = discordTxtW; discordH = 9;

        graphics.text(this.font, Component.literal("Custom Nickname (max 20 chars):"), centerX - 100, startY + 3, 0xFFAAAAAA);
        graphics.text(this.font, Component.literal("Gradient Start / End Hex:"), centerX - 100, startY + 43, 0xFFAAAAAA);

        Component previewText = TextGradientUtil.buildGradientText(
                this.customNickField.getValue().isEmpty() ? "Preview" : this.customNickField.getValue(),
                this.colorStartField.getValue(),
                this.colorEndField.getValue(),
                this.isBold,
                this.isItalic
        );
        graphics.centeredText(this.font, Component.empty().append("Preview: ").append(previewText), centerX, startY + 140, 0xFFFFFFFF);

        if (this.statusMessage != null) {
            graphics.centeredText(this.font, this.statusMessage, centerX, startY + 185, 0xFFFFFF55);
        }
    }

    private class HeightSlider extends AbstractSliderButton {
        public HeightSlider(int x, int y, int width, int height, double value) {
            super(x, y, width, height, Component.empty(), value);
            updateMessage();
        }

        public float getHeightValue() {
            return (float) (0.5 + this.value * 1.5);
        }

        @Override
        protected void updateMessage() {
            float h = getHeightValue();
            this.setMessage(Component.literal(String.format("Player Scale: %.2fx", h)));
        }

        @Override
        protected void applyValue() {
            visualHeight = getHeightValue();
        }
    }
}
