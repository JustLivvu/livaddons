package net.livaddons.feature;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;

public final class PartyCommands {
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{1,16}");
    private static final Pattern SAFE_NAME = Pattern.compile("^[A-Za-z0-9_]{1,16}$");
    private static final String[] EIGHT_BALL = {
            "It is certain", "It is decidedly so", "Without a doubt", "Yes definitely",
            "Most likely", "Outlook good", "Signs point to yes", "Ask again later",
            "Cannot predict now", "Don't count on it", "My reply is no", "Very doubtful"
    };
    private static final Queue<PendingCommand> PENDING = new ArrayDeque<>();
    private static long tick;
    private static long lastHandledAt;
    private static final Map<String, String> EMOTES = Map.ofEntries(
            Map.entry("<3", "❤"), Map.entry("o/", "( ﾟ◡ﾟ)/"), Map.entry(":star:", "✮"),
            Map.entry(":yes:", "✔"), Map.entry(":no:", "✖"), Map.entry(":java:", "☕"),
            Map.entry(":arrow:", "➜"), Map.entry(":shrug:", "¯\\_(ツ)_/¯"),
            Map.entry(":tableflip:", "(╯°□°）╯︵ ┻━┻"), Map.entry(":totem:", "☉_☉"),
            Map.entry(":typing:", "✎..."), Map.entry(":snail:", "@'-'"),
            Map.entry(":thinking:", "(0.o?)"), Map.entry(":gimme:", "༼つ◕_◕༽つ"),
            Map.entry(":wizard:", "('-')⊃━☆ﾟ.*･｡ﾟ"), Map.entry(":pvp:", "⚔"),
            Map.entry(":peace:", "✌"), Map.entry(":puffer:", "<('O')>"),
            Map.entry(":dog:", "(ᵔᴥᵔ)"), Map.entry(":dj:", "ヽ(⌐■_■)ノ♬"),
            Map.entry(":yey:", "ヽ (◕◡◕) ﾉ"), Map.entry(":snow:", "☃"),
            Map.entry(":dab:", "<o/"), Map.entry(":cat:", "= ＾● ⋏ ●＾ ="),
            Map.entry(":cute:", "(✿◠‿◠)"), Map.entry(":skull:", "☠"),
            Map.entry(":bum:", "♿")
    );

    private PartyCommands() {
    }

    public static void onChatMessage(Component component) {
        if (!FeatureSettings.partyCommandsEnabled() || component == null) return;
        ParsedPartyMessage parsed = parsePartyMessage(component.getString());
        if (parsed == null) return;
        String sender = parsed.sender;
        String message = parsed.message;
        if (!message.startsWith("!")) return;

        long now = System.currentTimeMillis();
        if (now - lastHandledAt < 500L) return;
        lastHandledAt = now;
        PENDING.add(new PendingCommand(tick + 4, sender, message.substring(1)));
    }

    public static void tick(Minecraft client) {
        tick++;
        while (!PENDING.isEmpty() && PENDING.peek().executeAt <= tick) {
            PendingCommand command = PENDING.remove();
            handle(client, command.sender, command.input);
        }
    }

    private static void handle(Minecraft client, String sender, String input) {
        if (!FeatureSettings.partyCommandsEnabled() || client.player == null) return;
        String[] words = input.trim().split("\\s+");
        if (words.length == 0 || words[0].isBlank()) return;
        String command = words[0].toLowerCase(Locale.ROOT);
        String canonical = canonicalCommand(command);
        if (!FeatureSettings.partyCommandEnabled(canonical)) return;
        String argument = words.length > 1 && SAFE_NAME.matcher(words[1]).matches() ? words[1] : null;

        switch (command) {
            case "help", "h" -> party(client, "Commands: " + String.join(", ", enabledCommands()));
            case "coords", "co" -> party(client, "Coords: " + client.player.getBlockX() + ", "
                    + client.player.getBlockY() + ", " + client.player.getBlockZ());
            case "cf" -> party(client, ThreadLocalRandom.current().nextBoolean() ? "heads" : "tails");
            case "8ball" -> party(client, EIGHT_BALL[ThreadLocalRandom.current().nextInt(EIGHT_BALL.length)]);
            case "dice" -> party(client, "Dice: " + ThreadLocalRandom.current().nextInt(1, 7));
            case "fps" -> party(client, "Current FPS: " + Minecraft.getInstance().getFps());
            case "time" -> party(client, "Current Time: " + ZonedDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")));
            case "holding" -> party(client, "Holding: " + (client.player.getMainHandItem().isEmpty()
                    ? "Nothing" : client.player.getMainHandItem().getHoverName().getString()));
            case "warp", "w" -> send(client, "party warp");
            case "allinvite", "allinv" -> send(client, "party settings allinvite");
            case "pt", "ptme", "transfer" -> send(client, "p transfer " + (argument != null ? argument : sender));
            case "promote" -> send(client, "party promote " + (argument != null ? argument : sender));
            case "demote" -> send(client, "party demote " + (argument != null ? argument : sender));
            case "kick", "k" -> send(client, "party kick " + (argument != null ? argument : sender));
            case "kickoffline", "ko" -> send(client, "party kickoffline");
            case "boop" -> send(client, "boop " + (argument != null ? argument : sender));
            default -> { }
        }
    }

    public static String replaceEmotes(String message) {
        if (!FeatureSettings.partyCommandsEnabled() || !FeatureSettings.partyCommandEmotesEnabled()) return message;
        String[] words = message.split(" ", -1);
        boolean changed = false;
        for (int i = 0; i < words.length; i++) {
            String replacement = EMOTES.get(words[i]);
            if (replacement != null) {
                words[i] = replacement;
                changed = true;
            }
        }
        return changed ? String.join(" ", words) : message;
    }

    private static String canonicalCommand(String command) {
        return switch (command) {
            case "h" -> "help"; case "co" -> "coords"; case "w" -> "warp";
            case "allinv" -> "allinvite"; case "ptme", "transfer" -> "pt";
            case "k" -> "kick"; case "ko" -> "kickoffline"; default -> command;
        };
    }

    private static java.util.List<String> enabledCommands() {
        return java.util.List.of("help", "coords", "cf", "8ball", "dice", "fps", "time", "holding",
                "warp", "allinvite", "pt", "promote", "demote", "kick", "kickoffline", "boop")
                .stream().filter(FeatureSettings::partyCommandEnabled).toList();
    }

    private static void party(Minecraft client, String text) {
        send(client, "pc " + text);
    }

    private static ParsedPartyMessage parsePartyMessage(String raw) {
        int partyStart = raw.indexOf("Party > ");
        if (partyStart < 0) return null;
        String payload = raw.substring(partyStart + "Party > ".length());
        int separator = payload.indexOf(": ");
        if (separator < 0) return null;
        String header = payload.substring(0, separator);
        String message = payload.substring(separator + 2).trim();

        Matcher names = USERNAME.matcher(header);
        String sender = null;
        while (names.find()) {
            String candidate = names.group();
            if (!candidate.equalsIgnoreCase("MVP") && !candidate.equalsIgnoreCase("VIP")) {
                sender = candidate;
            }
        }
        return sender == null ? null : new ParsedPartyMessage(sender, message);
    }

    private static void send(Minecraft client, String command) {
        if (client.player != null && client.player.connection != null) {
            client.player.connection.sendCommand(command);
        }
    }

    private record PendingCommand(long executeAt, String sender, String input) { }
    private record ParsedPartyMessage(String sender, String message) { }
}
