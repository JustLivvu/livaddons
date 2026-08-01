package net.livaddons;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class LivAddonsMod implements ModInitializer {
    public static final String MOD_ID = "livaddons";

    @Override
    public void onInitialize() {
        System.out.println("[LivAddons] Initializing LivAddons Fabric...");

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("livaddons")
                    .executes(context -> {
                        context.getSource().sendSuccess(() -> Component.literal("§aOpening LivAddons menu..."), false);
                        return 1;
                    })
            );
        });
    }
}
