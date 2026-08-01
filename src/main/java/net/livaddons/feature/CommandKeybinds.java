package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;

public final class CommandKeybinds {
    private static final Set<Integer> PRESSED = new HashSet<>();

    private CommandKeybinds() {}

    public static void tick(Minecraft client) {
        if (client.player == null || client.getConnection() == null || client.screen != null) {
            PRESSED.clear();
            return;
        }

        long window = client.getWindow().handle();
        Set<Integer> stillPressed = new HashSet<>();
        for (FeatureSettings.CommandKeybind binding : FeatureSettings.commandKeybinds()) {
            int key = binding.keyCode();
            if (key < 0 || binding.command().isBlank()) continue;
            if (GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS) {
                stillPressed.add(key);
                if (!PRESSED.contains(key)) {
                    String command = binding.command().trim();
                    while (command.startsWith("/")) command = command.substring(1);
                    if (!command.isBlank()) client.getConnection().sendCommand(command);
                }
            }
        }
        PRESSED.clear();
        PRESSED.addAll(stillPressed);
    }
}
