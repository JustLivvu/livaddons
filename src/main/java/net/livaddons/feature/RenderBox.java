package net.livaddons.feature;

import net.minecraft.world.phys.AABB;

public record RenderBox(AABB bounds, int color) {
}
