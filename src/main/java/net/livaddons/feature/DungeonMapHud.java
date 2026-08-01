package net.livaddons.feature;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Arrays;

public final class DungeonMapHud {
    public static final int SIZE = 128;
    private static final int MAP_SIZE = 128;
    private static final byte[] colors = new byte[MAP_SIZE * MAP_SIZE];
    private static boolean hasMap;
    private static final Identifier PLAYER_MARKER =
            Identifier.withDefaultNamespace("textures/map/decorations/frame.png");
    private static int roomSize = 16, roomGap = 20, startX = 5, startY = 5;
    private static final int[] checkmarks = new int[36];
    private static boolean checkmarksInitialized;

    private DungeonMapHud() {}

    public static void register() {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("livaddons", "dungeon_map"),
                (graphics, delta) -> render(graphics, false));
    }

    public static void onMapPacket(ClientboundMapItemDataPacket packet) {
        packet.colorPatch().ifPresent(patch -> {
            int[] previous = checkmarks.clone();
            byte[] update = patch.mapColors();
            int width = patch.width();
            int height = patch.height();
            if (width <= 0 || height <= 0) return;
            for (int z = 0; z < height; z++) {
                int source = z * width;
                int target = (patch.startY() + z) * MAP_SIZE + patch.startX();
                if (target >= 0 && target + width <= colors.length && source + width <= update.length)
                    System.arraycopy(update, source, colors, target, width);
            }
            hasMap = detectLayout();
            if (hasMap) updateRoomClear(previous);
        });
    }

    public static void reset() {
        Arrays.fill(colors, (byte) 0);
        hasMap = false;
        Arrays.fill(checkmarks, -1);
        checkmarksInitialized = false;
    }

    private static boolean detectLayout() {
        for (int index = 0; index < colors.length; index++) {
            if (colors[index] != 30) continue;
            int end = index;
            while (end < colors.length && colors[end] == 30) end++;
            int length = end - index;
            if (length == 16 || length == 18) {
                roomSize = length;
                roomGap = length + 4;
                startX = (index % MAP_SIZE) % roomGap;
                startY = (index / MAP_SIZE) % roomGap;
                if (startX == 0) startX = 22;
                if (startY == 0) startY = 22;
                return true;
            }
        }
        return hasMap;
    }

    private static void updateRoomClear(int[] previous) {
        Minecraft client = Minecraft.getInstance();
        for (int i = 0; i < 36; i++) {
            int tx = i % 6, tz = i / 6;
            int px = startX + tx * roomGap, pz = startY + tz * roomGap;
            checkmarks[i] = checkmark(pixel(px + roomSize / 2, pz + roomSize / 2));
        }
        if (!checkmarksInitialized) {
            checkmarksInitialized = true;
            return;
        }
        if (client.player == null) return;
        int playerX = (int) Math.floor((client.player.getX() + 200.0) / 32.0);
        int playerZ = (int) Math.floor((client.player.getZ() + 200.0) / 32.0);
        if (playerX < 0 || playerX > 5 || playerZ < 0 || playerZ > 5) return;
        int current = playerX + playerZ * 6;
        byte currentType = pixel(startX + playerX * roomGap, startY + playerZ * roomGap);
        if (currentType == 30 || currentType == 82) return; 
        for (int i = 0; i < 36; i++) {
            if (checkmarks[i] == previous[i] || (checkmarks[i] != 1 && checkmarks[i] != 2)) continue;
            if (sameRoom(current, i)) {
                RoomClear.onCheckmark(checkmarks[i] == 1);
                return;
            }
        }
    }

    private static int checkmark(byte color) {
        return switch (color & 0xFF) { case 30 -> 1; case 34 -> 2; case 18 -> 3; case 119 -> 4; default -> 0; };
    }

    private static boolean sameRoom(int start, int target) {
        if (start == target) return true;
        boolean[] seen = new boolean[36];
        int[] queue = new int[36];
        int head = 0, tail = 0;
        queue[tail++] = start;
        seen[start] = true;
        byte type = tileType(start);
        while (head < tail) {
            int at = queue[head++], x = at % 6, z = at / 6;
            int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] direction : directions) {
                int nx = x + direction[0], nz = z + direction[1];
                if (nx < 0 || nx > 5 || nz < 0 || nz > 5) continue;
                int next = nx + nz * 6;
                if (seen[next] || tileType(next) != type || !segmentsConnected(x, z, nx, nz)) continue;
                if (next == target) return true;
                seen[next] = true;
                queue[tail++] = next;
            }
        }
        return false;
    }

    private static byte tileType(int index) {
        return pixel(startX + (index % 6) * roomGap, startY + (index / 6) * roomGap);
    }

    private static boolean segmentsConnected(int x, int z, int nx, int nz) {
        int left = Math.min(x, nx), top = Math.min(z, nz);
        int px = startX + left * roomGap, pz = startY + top * roomGap;
        return x != nx ? pixel(px + roomSize + 2, pz) != 0 : pixel(px, pz + roomSize + 2) != 0;
    }

    public static void render(GuiGraphicsExtractor graphics, boolean preview) {
        Minecraft client = Minecraft.getInstance();
        if (!preview && (!FeatureSettings.dungeonMapEnabled()
                || !ScoreboardUtils.sidebarContains(client, "catacombs") || !hasMap)) return;
        int x = FeatureSettings.dungeonMapX();
        int y = FeatureSettings.dungeonMapY();
        float scale = FeatureSettings.dungeonMapScale() / 100f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(scale);
        renderAtOrigin(graphics, preview, client);
        graphics.pose().popMatrix();
    }

    private static void renderAtOrigin(GuiGraphicsExtractor graphics, boolean preview, Minecraft client) {
        int x = 0, y = 0;
        if (!FeatureSettings.dungeonMapClearBackground() || preview) {
            graphics.fill(0, 0, SIZE, SIZE, 0xD914151B);
            graphics.fill(0, 0, SIZE, 2, FeatureSettings.guiAccent());
        }
        if (preview || !hasMap) {
            graphics.centeredText(client.font, net.minecraft.network.chat.Component.literal("DUNGEON MAP"),
                    x + SIZE / 2, y + SIZE / 2 - 4, 0xFFFFFFFF);
            renderPlayerMarker(graphics, SIZE / 2f, SIZE / 2f, 0f, false);
            return;
        }
        boolean spinny = FeatureSettings.dungeonMapSpinny();
        float yaw = client.player == null ? 0f : client.player.getYRot();
        graphics.enableScissor(0, 0, SIZE, SIZE);
        if (spinny) {
            graphics.pose().pushMatrix();
            graphics.pose().translate(SIZE / 2f, SIZE / 2f);
            graphics.pose().rotate((float) Math.toRadians(180.0 - yaw));
            graphics.pose().translate(-SIZE / 2f, -SIZE / 2f);
        }
        int ox = x + 4;
        int oy = y + 4;
        int drawRoom = Math.max(10, roomSize - 2);
        int drawGap = Math.max(13, roomGap - 2);
        for (int tz = 0; tz < 6; tz++) for (int tx = 0; tx < 6; tx++) {
            int px = startX + tx * roomGap;
            int pz = startY + tz * roomGap;
            byte type = pixel(px, pz);
            if (type == 0) continue;
            int rx = ox + tx * drawGap;
            int ry = oy + tz * drawGap;
            graphics.fill(rx, ry, rx + drawRoom, ry + drawRoom, roomColor(type));
            int center = pixel(px + roomSize / 2, pz + roomSize / 2) & 0xFF;
            String mark = center == 30 ? "✓" : center == 34 ? "✓" : center == 18 ? "✕" : center == 119 ? "?" : "";
            int markColor = center == 30 ? 0xFF55FF55 : center == 18 ? 0xFFFF5555 : 0xFFFFFFFF;
            if (!mark.isEmpty()) graphics.centeredText(client.font,
                    net.minecraft.network.chat.Component.literal(mark), rx + drawRoom / 2, ry + 3, markColor);
            if (tx < 5) {
                byte connection = pixel(px + roomSize + 2, pz);
                byte door = pixel(px + roomSize + 2, pz + roomSize / 2);
                byte doorSide = pixel(px + roomSize + 2, pz + roomSize / 2 - 4);
                if (connection != 0) {
                    graphics.fill(rx + drawRoom, ry, rx + drawGap, ry + drawRoom, roomColor(type));
                } else if (door != 0 && doorSide == 0) {
                    graphics.fill(rx + drawRoom, ry + drawRoom / 2 - 2,
                            rx + drawGap, ry + drawRoom / 2 + 2, doorColor(door));
                }
            }
            if (tz < 5) {
                byte connection = pixel(px, pz + roomSize + 2);
                byte door = pixel(px + roomSize / 2, pz + roomSize + 2);
                byte doorSide = pixel(px + roomSize / 2 - 4, pz + roomSize + 2);
                if (connection != 0) {
                    graphics.fill(rx, ry + drawRoom, rx + drawRoom, ry + drawGap, roomColor(type));
                } else if (door != 0 && doorSide == 0) {
                    graphics.fill(rx + drawRoom / 2 - 2, ry + drawRoom,
                            rx + drawRoom / 2 + 2, ry + drawGap, doorColor(door));
                }
            }
        }
        float playerX = 4f + (float) ((client.player.getX() + 200.0) * roomGap / 32.0 * drawGap / roomGap);
        float playerY = 4f + (float) ((client.player.getZ() + 200.0) * roomGap / 32.0 * drawGap / roomGap);
        if (spinny) graphics.pose().popMatrix();
        if (spinny) {
            double radians = Math.toRadians(180.0 - yaw);
            float dx = playerX - SIZE / 2f, dy = playerY - SIZE / 2f;
            playerX = SIZE / 2f + (float) (dx * Math.cos(radians) - dy * Math.sin(radians));
            playerY = SIZE / 2f + (float) (dx * Math.sin(radians) + dy * Math.cos(radians));
        }
        renderPlayerMarker(graphics, playerX, playerY, yaw, spinny);
        graphics.disableScissor();
    }

    private static void renderPlayerMarker(GuiGraphicsExtractor graphics, float x, float y, float yaw, boolean spinny) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        if (!spinny) graphics.pose().rotate((float) Math.toRadians(180.0 + yaw));
        graphics.blit(RenderPipelines.GUI_TEXTURED, PLAYER_MARKER, -2, -3, 2f, 0f, 5, 7, 8, 8);
        graphics.pose().popMatrix();
    }

    public static int renderedSize() {
        return Math.round(SIZE * FeatureSettings.dungeonMapScale() / 100f);
    }

    private static byte pixel(int x, int y) {
        return x >= 0 && x < MAP_SIZE && y >= 0 && y < MAP_SIZE ? colors[y * MAP_SIZE + x] : 0;
    }

    private static int roomColor(byte value) {
        return switch (value) {
            case 30 -> 0xFF148500; case 82 -> 0xFFE000FF; case 18 -> 0xFFFF0000;
            case 74 -> 0xFFFEDF00; case 66 -> 0xFF750085; case 62 -> 0xFFD87F33;
            case 63 -> 0xFF6B3A11; default -> 0xFF3C3C3C;
        };
    }

    private static int doorColor(byte value) {
        return switch (value) { case 119 -> 0xFF050505; case 18 -> 0xFFFF0000; case 82 -> 0xFF9000A0; default -> 0xFF4A2509; };
    }
}
