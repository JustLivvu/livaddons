package net.livaddons.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.awt.Color;

public class TextGradientUtil {

    public static MutableComponent buildGradientText(String input, String hexStart, String hexEnd, boolean isBold, boolean isItalic) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }

        Color colorStart = parseHexColor(hexStart, Color.WHITE);
        Color colorEnd = parseHexColor(hexEnd, colorStart);

        MutableComponent rootContainer = Component.empty();
        int length = input.length();

        for (int i = 0; i < length; i++) {
            char c = input.charAt(i);

            float ratio = length > 1 ? (float) i / (float) (length - 1) : 0f;

            int red = (int) (colorStart.getRed() + ratio * (colorEnd.getRed() - colorStart.getRed()));
            int green = (int) (colorStart.getGreen() + ratio * (colorEnd.getGreen() - colorStart.getGreen()));
            int blue = (int) (colorStart.getBlue() + ratio * (colorEnd.getBlue() - colorStart.getBlue()));

            int rgb = ((red & 0xFF) << 16) | ((green & 0xFF) << 8) | (blue & 0xFF);

            Style style = Style.EMPTY
                    .withColor(TextColor.fromRgb(rgb))
                    .withBold(isBold)
                    .withItalic(isItalic);

            MutableComponent charComponent = Component.literal(String.valueOf(c)).setStyle(style);
            rootContainer.append(charComponent);
        }

        return rootContainer;
    }

    private static Color parseHexColor(String hex, Color fallback) {
        if (hex == null) return fallback;
        try {
            String cleanHex = hex.startsWith("#") ? hex.substring(1) : hex;
            if (cleanHex.length() == 6) {
                int r = Integer.parseInt(cleanHex.substring(0, 2), 16);
                int g = Integer.parseInt(cleanHex.substring(2, 4), 16);
                int b = Integer.parseInt(cleanHex.substring(4, 6), 16);
                return new Color(r, g, b);
            }
        } catch (Exception ignored) {}
        return fallback;
    }
}
