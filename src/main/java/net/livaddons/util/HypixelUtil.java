package net.livaddons.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;

import java.util.Locale;

public class HypixelUtil {

    public static boolean isOnHypixel() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return false;
        
        ServerData server = client.getCurrentServer();
        if (server != null && server.ip != null) {
            String ip = server.ip.toLowerCase(Locale.ROOT);
            return ip.contains("hypixel.net")
                    || ip.contains("hypixel")
                    || ip.contains("p3sim.net");
        }
        return false;
    }
}
