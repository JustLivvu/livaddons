package net.livaddons.feature;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.client.Minecraft;
import java.util.ArrayList;
import java.util.List;

import java.util.Locale;

public final class TerminalWaypoints {
    private static final Waypoint[] WAYPOINTS = {
            new Waypoint("Terminal 1", 110, 113, 73, Type.TERMINAL),
            new Waypoint("Terminal 2", 110, 119, 79, Type.TERMINAL),
            new Waypoint("Terminal 3", 90, 112, 92, Type.TERMINAL),
            new Waypoint("Terminal 4", 90, 122, 101, Type.TERMINAL),
            new Waypoint("Lever 1", 94, 124, 113, Type.LEVER),
            new Waypoint("Lever 2", 106, 124, 113, Type.LEVER),
            new Waypoint("Device: Simon Says", 110, 120, 91, Type.DEVICE),
            new Waypoint("Terminal 1", 68, 109, 122, Type.TERMINAL),
            new Waypoint("Terminal 2", 59, 120, 123, Type.TERMINAL),
            new Waypoint("Terminal 3", 47, 109, 122, Type.TERMINAL),
            new Waypoint("Terminal 4", 39, 108, 142, Type.TERMINAL),
            new Waypoint("Device: Levers", 60, 132, 142, Type.DEVICE),
            new Waypoint("Lever 1", 27, 124, 127, Type.LEVER),
            new Waypoint("Lever 2", 23, 132, 138, Type.LEVER),
            new Waypoint("Terminal 1", -2, 109, 112, Type.TERMINAL),
            new Waypoint("Terminal 2", 19, 123, 93, Type.TERMINAL),
            new Waypoint("Terminal 3", -2, 119, 93, Type.TERMINAL),
            new Waypoint("Terminal 4", -2, 109, 77, Type.TERMINAL),
            new Waypoint("Device: Arrow Align", -2, 119, 74, Type.DEVICE),
            new Waypoint("Lever 1", 14, 122, 55, Type.LEVER),
            new Waypoint("Lever 2", 2, 122, 55, Type.LEVER),
            new Waypoint("Terminal 1", 41, 109, 30, Type.TERMINAL),
            new Waypoint("Terminal 2", 44, 121, 30, Type.TERMINAL),
            new Waypoint("Terminal 3", 67, 109, 30, Type.TERMINAL),
            new Waypoint("Terminal 4", 72, 115, 47, Type.TERMINAL),
            new Waypoint("Device: I4", 63, 127, 34, Type.DEVICE),
            new Waypoint("Lever 1", 86, 128, 46, Type.LEVER),
            new Waypoint("Lever 2", 84, 121, 34, Type.LEVER)
    };

    private TerminalWaypoints() {
    }

    public static Component waypointName(ArmorStand armorStand, Component original) {
        if (!FeatureSettings.terminalWaypointsEnabled() || original == null) return null;

        String state = original.getString().toLowerCase(Locale.ROOT);
        Waypoint closest = null;
        double closestDistance = Double.MAX_VALUE;

        for (Waypoint waypoint : WAYPOINTS) {
            double dx = armorStand.getX() - waypoint.x;
            double dy = armorStand.getY() - waypoint.y;
            double dz = armorStand.getZ() - waypoint.z;
            double distance = dx * dx + dy * dy + dz * dz;
            if (distance <= 9.0 && distance < closestDistance) {
                closest = waypoint;
                closestDistance = distance;
            }
        }

        if (closest == null || !closest.isInactive(state)) return null;
        int distance = (int) Math.round(armorStand.distanceToSqr(
                closest.x, closest.y, closest.z));
        return Component.literal("◆ " + closest.name + "  ")
                .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)
                .append(Component.literal("[" + closest.x + " " + closest.y + " " + closest.z + "]")
                        .withStyle(ChatFormatting.GRAY));
    }

    public static List<RenderBox> collectBoxes(Minecraft client) {
        List<RenderBox> boxes = new ArrayList<>();
        if (!FeatureSettings.terminalWaypointsEnabled() || client.level == null || client.player == null) return boxes;

        for (Waypoint waypoint : WAYPOINTS) {
            boolean inactive = client.level.getEntitiesOfClass(
                    ArmorStand.class,
                    new net.minecraft.world.phys.AABB(
                            waypoint.x - 3, waypoint.y - 3, waypoint.z - 3,
                            waypoint.x + 3, waypoint.y + 3, waypoint.z + 3)
            ).stream().anyMatch(stand -> {
                Component name = stand.getCustomName();
                return name != null && waypoint.isInactive(name.getString().toLowerCase(Locale.ROOT));
            });
            if (inactive) {
                boxes.add(new RenderBox(new net.minecraft.world.phys.AABB(
                        waypoint.x - 0.55, waypoint.y - 0.2, waypoint.z - 0.55,
                        waypoint.x + 0.55, waypoint.y + 1.9, waypoint.z + 0.55), 0xFFFF3030));
            }
        }
        return boxes;
    }

    private enum Type {
        TERMINAL, LEVER, DEVICE
    }

    private record Waypoint(String name, int x, int y, int z, Type type) {
        boolean isInactive(String state) {
            return switch (type) {
                case TERMINAL -> state.contains("inactive terminal");
                case LEVER -> state.contains("not activated");
                case DEVICE -> state.contains("inactive") || state.contains("not activated");
            };
        }
    }
}
