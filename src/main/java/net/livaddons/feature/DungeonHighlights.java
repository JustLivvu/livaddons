package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class DungeonHighlights {
    private static final Set<String> DUNGEON_MOBS = Set.of(
            "Lurker", "Dreadlord", "Souleater", "Zombie", "Skeleton", "Skeletor",
            "Sniper", "Super Archer", "Spider", "Fels", "Withermancer",
            "Lost Adventurer", "Angry Archaeologist", "Frozen Adventurer"
    );

    private DungeonHighlights() {
    }

    public static List<RenderBox> collectBoxes(Minecraft client) {
        List<RenderBox> result = new ArrayList<>();
        if (!FeatureSettings.highlightsEnabled() || client.level == null || client.player == null) return result;

        RenderBox.Style style = switch (FeatureSettings.highlightsStyle()) {
            case 0 -> RenderBox.Style.FILLED;
            case 2 -> RenderBox.Style.FILLED_OUTLINE;
            default -> RenderBox.Style.OUTLINE;
        };

        for (Entity entity : client.level.entitiesForRendering()) {
            if (!(entity instanceof ArmorStand stand) || !stand.isAlive()) continue;
            Component customName = stand.getCustomName();
            if (customName == null) continue;
            String name = customName.getString();
            if (!name.contains("✯") || DUNGEON_MOBS.stream().noneMatch(name::contains)) continue;

            List<Entity> candidates = client.level.getEntities(stand,
                    stand.getBoundingBox().move(0.0, -1.0, 0.0).inflate(0.45),
                    DungeonHighlights::isValidEntity);
            if (!candidates.isEmpty()) {
                Entity mob = candidates.getFirst();
                result.add(new RenderBox(mob.getBoundingBox().inflate(0.03),
                        FeatureSettings.highlightsColor(), style));
            }
        }
        return result;
    }

    private static boolean isValidEntity(Entity entity) {
        if (!entity.isAlive() || entity instanceof ArmorStand || entity instanceof WitherBoss) return false;
        if (entity instanceof Player player) return player.getUUID().version() == 2;
        return !entity.isInvisible();
    }
}
