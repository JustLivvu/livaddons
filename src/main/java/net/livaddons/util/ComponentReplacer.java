package net.livaddons.util;

import net.livaddons.data.PlayerCosmeticData;
import net.livaddons.data.PlayerDataManager;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class ComponentReplacer {

    public static Component replaceInComponent(Component original) {
        if (original == null) return null;
        if (!HypixelUtil.isOnHypixel()) return original;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return original;

        Component result = original;

        for (PlayerCosmeticData data : PlayerDataManager.getInstance().getAllCosmeticData()) {
            if (data != null && data.username != null && !data.username.isBlank()
                    && data.customNick != null && !data.customNick.isBlank()) {

                Component customNickComponent = TextGradientUtil.buildGradientText(
                        data.customNick,
                        data.colorStart,
                        data.colorEnd,
                        data.isBold,
                        data.isItalic
                );
                result = replaceSubstring(result, data.username, customNickComponent);
            }
        }

        return result;
    }

    public static Component replaceSubstring(Component component, String target, Component replacement) {
        if (component == null || target == null || target.isEmpty()) return component;
        if (!component.getString().contains(target)) return component;

        MutableComponent result = Component.empty();

        component.visit((style, text) -> {
            if (text.contains(target)) {
                String[] parts = text.split(Pattern.quote(target), -1);
                for (int i = 0; i < parts.length; i++) {
                    if (!parts[i].isEmpty()) {
                        result.append(Component.literal(parts[i]).setStyle(style));
                    }
                    if (i < parts.length - 1) {
                        result.append(replacement.copy());
                    }
                }
            } else {
                result.append(Component.literal(text).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        return result;
    }
}
