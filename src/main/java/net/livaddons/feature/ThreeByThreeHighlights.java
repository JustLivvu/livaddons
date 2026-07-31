package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;

import java.util.List;

public final class ThreeByThreeHighlights {
    private ThreeByThreeHighlights() {
    }

    public static List<RenderBox> collectBoxes(Minecraft client) {
        if (!FeatureSettings.threeByThreeHighlightsEnabled()
                || client.level == null || client.player == null) return List.of();

        return List.of(new RenderBox(
                new AABB(53.0, 63.0, 113.0, 56.0, 64.0, 116.0),
                0xFFFF3030,
                RenderBox.Style.FILLED_OUTLINE));
    }
}
