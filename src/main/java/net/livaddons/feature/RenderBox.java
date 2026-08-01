package net.livaddons.feature;

import net.minecraft.world.phys.AABB;

public record RenderBox(AABB bounds, int color, Style style) {
    public RenderBox(AABB bounds, int color) {
        this(bounds, color, Style.FILLED_OUTLINE);
    }

    public enum Style { FILLED, OUTLINE, FILLED_OUTLINE }
}
