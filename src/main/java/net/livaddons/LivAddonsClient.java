package net.livaddons;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.livaddons.data.PlayerDataManager;
import net.livaddons.gui.LivAddonsScreen;
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
