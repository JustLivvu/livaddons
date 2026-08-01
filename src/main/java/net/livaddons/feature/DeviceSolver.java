package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public final class DeviceSolver {
    private static final ArrayDeque<BlockPos> SIMON_SEQUENCE = new ArrayDeque<>();
    private static final Map<BlockPos, Boolean> LAST_LIT = new HashMap<>();
    private static final List<RenderBox> RENDER_BOXES = new ArrayList<>();
    private static final List<RenderLabel> RENDER_LABELS = new ArrayList<>();

    private static final BlockPos[] I4_BLOCKS = {
            new BlockPos(68, 130, 50), new BlockPos(66, 130, 50), new BlockPos(64, 130, 50),
            new BlockPos(68, 128, 50), new BlockPos(66, 128, 50), new BlockPos(64, 128, 50),
            new BlockPos(68, 126, 50), new BlockPos(66, 126, 50), new BlockPos(64, 126, 50)
    };
    private static final int[][] ARROW_SOLUTIONS = {
            {7,7,-1,-1,-1,1,-1,-1,-1,-1,1,3,3,3,3,-1,-1,-1,-1,1,-1,-1,-1,7,1},
            {-1,-1,7,7,5,-1,7,1,-1,5,-1,-1,-1,-1,-1,-1,7,5,-1,1,-1,-1,7,7,1},
            {7,7,-1,-1,-1,1,-1,-1,-1,-1,1,3,-1,7,5,-1,-1,-1,-1,5,-1,-1,-1,3,3},
            {5,3,3,3,-1,5,-1,-1,-1,-1,7,7,-1,-1,-1,1,-1,-1,-1,-1,1,3,3,3,-1},
            {5,3,3,3,3,5,-1,-1,-1,1,7,7,-1,-1,1,-1,-1,-1,-1,1,-1,7,7,7,1},
            {7,7,7,7,-1,1,-1,-1,-1,-1,1,3,3,3,3,-1,-1,-1,-1,1,-1,7,7,7,1},
            {-1,-1,-1,-1,-1,1,-1,1,-1,1,1,-1,1,-1,1,1,-1,1,-1,1,-1,-1,-1,-1,-1},
            {-1,-1,-1,-1,-1,1,3,3,3,3,-1,-1,-1,-1,1,7,7,7,7,1,-1,-1,-1,-1,-1},
            {-1,-1,-1,-1,-1,-1,1,-1,1,-1,7,1,7,1,3,1,-1,1,-1,1,-1,-1,-1,-1,-1}
    };

    private DeviceSolver() {
    }

    public static void tick(Minecraft client) {
        if (!FeatureSettings.deviceSolverEnabled() || client.level == null || client.player == null) return;
        if (client.level.getGameTime() % 3 != 0) return;
        RENDER_BOXES.clear();
        RENDER_LABELS.clear();
        tickSimon(client);
        tickI4(client);
        tickArrowAlign(client);
    }

    private static void tickSimon(Minecraft client) {
        for (int y = 120; y <= 123; y++) {
            for (int z = 92; z <= 95; z++) {
                BlockPos lamp = new BlockPos(111, y, z);
                boolean lit = client.level.getBlockState(lamp).is(Blocks.SEA_LANTERN);
                boolean wasLit = LAST_LIT.getOrDefault(lamp, false);
                if (lit && !wasLit) {
                    BlockPos button = lamp.west();
                    if (!SIMON_SEQUENCE.contains(button)) SIMON_SEQUENCE.add(button);
                }
                LAST_LIT.put(lamp, lit);
            }
        }

        BlockPos expected = SIMON_SEQUENCE.peekFirst();
        if (expected == null) return;
        boolean powered = client.level.getBlockState(expected)
                .getOptionalValue(net.minecraft.world.level.block.ButtonBlock.POWERED).orElse(false);
        if (powered) SIMON_SEQUENCE.pollFirst();

        int index = 0;
        for (BlockPos button : SIMON_SEQUENCE) {
            int color = index == 0 ? 0xFF20FF55 : index == 1 ? 0xFFFFD52A : 0xFFFF3030;
            addBox(button, color);
            index++;
        }
    }

    private static void tickI4(Minecraft client) {
        if (client.player.distanceToSqr(65, 128, 36) > 900) return;
        for (BlockPos pos : I4_BLOCKS) {
            if (client.level.getBlockState(pos).is(Blocks.EMERALD_BLOCK)) {
                addBox(pos, 0xFF20FF55);
            } else if (client.level.getBlockState(pos).is(Blocks.BLUE_TERRACOTTA)) {
                addBox(pos, 0xFFFF3030);
            }
        }
    }

    private static void tickArrowAlign(Minecraft client) {
        BlockPos corner = new BlockPos(-2, 120, 75);
        if (client.player.distanceToSqr(corner.getX(), corner.getY(), corner.getZ()) > 250) return;

        int[] rotations = new int[25];
        java.util.Arrays.fill(rotations, -1);
        var frames = client.level.getEntitiesOfClass(ItemFrame.class,
                new AABB(-2.5, 119.5, 74.5, -0.5, 125.5, 80.5));
        for (ItemFrame frame : frames) {
            if (!frame.getItem().is(Items.ARROW)) continue;
            BlockPos pos = frame.blockPosition();
            int index = (pos.getY() - corner.getY()) + (pos.getZ() - corner.getZ()) * 5;
            if (index >= 0 && index < 25) rotations[index] = frame.getRotation();
        }

        int[] solution = null;
        for (int[] candidate : ARROW_SOLUTIONS) {
            boolean matches = true;
            for (int i = 0; i < 25; i++) {
                if ((candidate[i] == -1 || rotations[i] == -1) && candidate[i] != rotations[i]) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                solution = candidate;
                break;
            }
        }
        if (solution == null) return;

        for (int i = 0; i < 25; i++) {
            if (rotations[i] == -1 || solution[i] == -1) continue;
            int clicks = (8 - rotations[i] + solution[i]) % 8;
            if (clicks == 0) continue;
            int color = clicks < 3 ? 0xFF20FF55 : clicks < 5 ? 0xFFFFA020 : 0xFFFF3030;
            BlockPos framePos = new BlockPos(corner.getX(), corner.getY() + i % 5, corner.getZ() + i / 5);
            RENDER_LABELS.add(new RenderLabel(framePos, String.valueOf(clicks), color));
        }
    }

    private static void addBox(BlockPos pos, int color) {
        RENDER_BOXES.add(new RenderBox(new AABB(pos), color));
    }

    public static List<RenderBox> getRenderBoxes() {
        return List.copyOf(RENDER_BOXES);
    }

    public static List<RenderLabel> getRenderLabels() {
        return List.copyOf(RENDER_LABELS);
    }
}
