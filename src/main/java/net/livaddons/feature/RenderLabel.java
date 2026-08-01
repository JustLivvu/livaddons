package net.livaddons.feature;

import net.minecraft.core.BlockPos;

public record RenderLabel(BlockPos position, String text, int color) {
}
