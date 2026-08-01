package net.livaddons.feature;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class WorldBoxRenderer {
    private WorldBoxRenderer() {
    }

    public static void register() {
        LevelRenderEvents.END_EXTRACTION.register(context -> {
            Minecraft client = Minecraft.getInstance();
            List<RenderBox> boxes = new ArrayList<>();
            boxes.addAll(TerminalWaypoints.collectBoxes(client));
            boxes.addAll(DeviceSolver.getRenderBoxes());
            boxes.addAll(DungeonHighlights.collectBoxes(client));
            boxes.addAll(ThreeByThreeHighlights.collectBoxes(client));
            try (var ignored = context.levelRenderer().collectPerFrameGizmos()) {
                for (RenderBox box : boxes) {
                    int rgb = box.color() & 0x00FFFFFF;
                    int stroke = 0xFF000000 | rgb;
                    int fill = 0x48000000 | rgb;
                    GizmoStyle style = switch (box.style()) {
                        case FILLED -> GizmoStyle.fill(fill);
                        case OUTLINE -> GizmoStyle.stroke(stroke, 2.0f);
                        case FILLED_OUTLINE -> GizmoStyle.strokeAndFill(stroke, 2.0f, fill);
                    };
                    Gizmos.cuboid(box.bounds(), style);
                }
                for (RenderLabel label : DeviceSolver.getRenderLabels()) {
                    Vec3 position = Vec3.atCenterOf(label.position());
                    Gizmos.billboardText(label.text(), position,
                            TextGizmo.Style.forColorAndCentered(label.color()).withScale(1.2f));
                }
            }
        });
    }
}
