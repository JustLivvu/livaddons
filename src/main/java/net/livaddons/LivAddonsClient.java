package net.livaddons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.livaddons.data.PlayerDataManager;
import net.livaddons.gui.LivAddonsScreen;
import net.livaddons.feature.TerminalSolver;
import net.livaddons.feature.TerminalWaypoints;
import net.livaddons.feature.DeviceSolver;
import net.livaddons.feature.WorldBoxRenderer;
import net.livaddons.feature.MelodyAlert;
import net.livaddons.feature.FeatureSettings;
import net.livaddons.feature.TerminalsHud;
import net.livaddons.feature.LavaToWater;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LivAddonsClient implements ClientModInitializer {

    private int tickCounter = 0;

    @Override
    public void onInitializeClient() {
        System.out.println("[LivAddons] Initializing LivAddons Client...");
        FeatureSettings.load();
        TerminalsHud.register();
        WorldBoxRenderer.register();

        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            MelodyAlert.onScreenOpened(client, screen);
            if (screen instanceof AbstractContainerScreen<?> containerScreen) {
                ScreenEvents.afterExtract(screen).register((ignored, graphics, mouseX, mouseY, delta) ->
                        TerminalSolver.render(containerScreen, graphics));
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("livaddons")
                    .executes(context -> {
                        Minecraft client = Minecraft.getInstance();
                        client.execute(() -> client.setScreen(new LivAddonsScreen()));
                        return 1;
                    })
            );
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level == null || client.player == null) return;

            PlayerDataManager.getInstance().requestCosmeticDirectory();
            DeviceSolver.tick(client);
            LavaToWater.tick(client);

            tickCounter++;
            if (tickCounter >= 40) {
                tickCounter = 0;

                List<UUID> visiblePlayerUuids = new ArrayList<>();

                if (client.getConnection() != null) {
                    for (net.minecraft.client.multiplayer.PlayerInfo info : client.getConnection().getOnlinePlayers()) {
                        if (info.getProfile() != null && info.getProfile().id() != null) {
                            visiblePlayerUuids.add(info.getProfile().id());
                        }
                    }
                }

                for (AbstractClientPlayer player : client.level.players()) {
                    if (!visiblePlayerUuids.contains(player.getUUID())) {
                        visiblePlayerUuids.add(player.getUUID());
                    }
                }

                if (!visiblePlayerUuids.isEmpty()) {
                    PlayerDataManager.getInstance().requestBulkFetch(visiblePlayerUuids);
                }
            }
        });
    }
}
