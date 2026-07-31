package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.Locale;

public final class ScoreboardUtils {
    private ScoreboardUtils() {
    }

    public static boolean sidebarContains(Minecraft client, String wantedText) {
        if (client.level == null) return false;
        String wanted = wantedText.toLowerCase(Locale.ROOT);
        Scoreboard scoreboard = client.level.getScoreboard();
        Objective sidebar = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (sidebar != null && objectiveContains(scoreboard, sidebar, wanted)) return true;

        // Hypixel can update or encode the visible sidebar through a non-standard
        // objective/team combination. Scan all synchronized scoreboard data as fallback.
        for (Objective objective : scoreboard.getObjectives()) {
            if (objectiveContains(scoreboard, objective, wanted)) return true;
        }
        for (PlayerTeam team : scoreboard.getPlayerTeams()) {
            String teamText = team.getDisplayName().getString() + " "
                    + team.getPlayerPrefix().getString() + " " + team.getPlayerSuffix().getString();
            if (containsNormalized(teamText, wanted)) return true;
            for (String player : team.getPlayers()) {
                if (containsNormalized(PlayerTeam.formatNameForTeam(team,
                        net.minecraft.network.chat.Component.literal(player)).getString(), wanted)) return true;
            }
        }
        return false;
    }

    private static boolean objectiveContains(Scoreboard scoreboard, Objective objective, String wanted) {
        if (containsNormalized(objective.getDisplayName().getString(), wanted)) return true;
        for (PlayerScoreEntry entry : scoreboard.listPlayerScores(objective)) {
            if (containsNormalized(entry.owner(), wanted)) return true;
            if (entry.display() != null && containsNormalized(entry.display().getString(), wanted)) return true;
            PlayerTeam team = scoreboard.getPlayersTeam(entry.owner());
            if (containsNormalized(PlayerTeam.formatNameForTeam(team, entry.ownerName()).getString(), wanted)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsNormalized(String text, String wanted) {
        return text != null && text.toLowerCase(Locale.ROOT).replaceAll("§.", "").contains(wanted);
    }
}
