package net.livaddons.feature;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import net.livaddons.mixin.AbstractContainerScreenAccessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TerminalSolver {
    private static final String[] COLORS = {
            "red", "orange", "yellow", "green", "lime", "blue", "light blue",
            "cyan", "purple", "magenta", "pink", "white", "gray", "grey", "black", "brown"
    };

    private TerminalSolver() {
    }

    public static void render(AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics) {
        if (!FeatureSettings.terminalSolverEnabled()) return;
        String title = screen.getTitle().getString().toLowerCase(Locale.ROOT);
        boolean rubixTerminal = title.contains("change all") && title.contains("same color");
        List<Target> targets = solve(title, screen.getMenu().slots);
        if (targets.isEmpty()) return;

        AbstractContainerScreenAccessor accessor = (AbstractContainerScreenAccessor) screen;
        int left = accessor.livaddons$getLeftPos();
        int top = accessor.livaddons$getTopPos();
        for (Target target : targets) {
            Slot slot = target.slot;
            int x = left + slot.x;
            int y = top + slot.y;
            graphics.fill(x, y, x + 16, y + 16, 0xA000FF66);
            graphics.fill(x, y, x + 16, y + 1, 0xFF70FF9D);
            graphics.fill(x, y + 15, x + 16, y + 16, 0xFF70FF9D);
            graphics.fill(x, y, x + 1, y + 16, 0xFF70FF9D);
            graphics.fill(x + 15, y, x + 16, y + 16, 0xFF70FF9D);
            if (rubixTerminal || Math.abs(target.clicks) > 1 || target.clicks < 0) {
                String clickText = target.clicks > 0 ? "+" + target.clicks : String.valueOf(target.clicks);
                graphics.centeredText(screen.getFont(),
                        net.minecraft.network.chat.Component.literal(clickText),
                        x + 8, y + 5, 0xFFFFFFFF);
            }
        }
        graphics.centeredText(screen.getFont(),
                net.minecraft.network.chat.Component.literal("Terminal Solver: " + targets.size() + " target(s)"),
                screen.width / 2, Math.max(6, top - 13), 0xFF70FF9D);
        if (rubixTerminal) {
            graphics.centeredText(screen.getFont(),
                    net.minecraft.network.chat.Component.literal("+ = left click, - = right click"),
                    screen.width / 2, Math.max(16, top - 3), 0xFFB7B8C0);
        }
    }

    private static List<Target> solve(String title, List<Slot> slots) {
        List<Target> result = new ArrayList<>();
        List<Slot> terminalSlots = terminalSlots(slots);
        String wantedColor = findColor(title);

        if (title.contains("select all") && wantedColor != null) {
            for (Slot slot : terminalSlots) {
                if (matchesColor(slot.getItem(), wantedColor)) result.add(new Target(slot, 1));
            }
        } else if (title.contains("change all") && title.contains("same color")) {
            solveSameColor(terminalSlots, result);
        } else if (title.contains("correct all") && title.contains("pane")) {
            for (Slot slot : terminalSlots) {
                ItemStack stack = slot.getItem();
                if (stack.is(Items.RED_STAINED_GLASS_PANE)
                        || itemName(slot).contains("red stained glass")) {
                    result.add(new Target(slot, 1));
                }
            }
        } else if (title.contains("click in order")) {
            Slot lowest = null;
            int lowestNumber = Integer.MAX_VALUE;
            for (Slot slot : terminalSlots) {
                ItemStack stack = slot.getItem();
                if (stack.isEmpty() || isCompletedOrderSlot(stack) || !isOrderCandidate(stack)) continue;
                int number = stack.getCount();
                if (number > 0 && number < lowestNumber) {
                    lowestNumber = number;
                    lowest = slot;
                }
            }
            if (lowest != null) result.add(new Target(lowest, 1));
        } else if (title.contains("starts with")) {
            Matcher matcher = Pattern.compile("[\"']([a-z])[\"']").matcher(title);
            if (matcher.find()) {
                String letter = matcher.group(1);
                for (Slot slot : terminalSlots) {
                    if (!slot.getItem().hasFoil() && itemName(slot).startsWith(letter)) {
                        result.add(new Target(slot, 1));
                    }
                }
            }
        } else if (title.contains("melody")) {
            for (Slot slot : terminalSlots) {
                String name = itemName(slot);
                if (name.contains("lime") || name.contains("green")) result.add(new Target(slot, 1));
            }
        }
        return result;
    }

    private static List<Slot> terminalSlots(List<Slot> slots) {
        int terminalSize = Math.max(0, slots.size() - 36);
        return slots.subList(0, terminalSize);
    }

    private static boolean isCompletedOrderSlot(ItemStack stack) {
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        return stack.is(Items.LIME_STAINED_GLASS_PANE)
                || stack.is(Items.GREEN_STAINED_GLASS_PANE)
                || name.contains("green stained glass")
                || name.contains("lime stained glass");
    }

    private static boolean isOrderCandidate(ItemStack stack) {
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        return stack.is(Items.RED_STAINED_GLASS_PANE)
                || name.contains("red stained glass");
    }

    private static void solveSameColor(List<Slot> slots, List<Target> result) {
        String[] cycle = {"red", "orange", "yellow", "green", "blue"};
        List<Slot> panes = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        int[] allowedIndexes = {12, 13, 14, 21, 22, 23, 30, 31, 32};
        for (int slotIndex : allowedIndexes) {
            if (slotIndex < 0 || slotIndex >= slots.size()) continue;
            Slot slot = slots.get(slotIndex);
            int color = rubixColorIndex(slot.getItem());
            if (color == -1) continue;
            panes.add(slot);
            colors.add(color);
        }
        int bestTarget = 0;
        int bestClicks = Integer.MAX_VALUE;
        for (int target = 0; target < cycle.length; target++) {
            int clicks = 0;
            for (int color : colors) {
                int distance = Math.abs(target - color);
                clicks += Math.min(distance, cycle.length - distance);
            }
            if (clicks < bestClicks) {
                bestClicks = clicks;
                bestTarget = target;
            }
        }
        for (int i = 0; i < panes.size(); i++) {
            int difference = bestTarget - colors.get(i);
            if (difference > 2) difference -= cycle.length;
            if (difference < -2) difference += cycle.length;
            if (difference != 0) result.add(new Target(panes.get(i), difference));
        }
    }

    private static int rubixColorIndex(ItemStack stack) {
        if (stack.is(Items.RED_STAINED_GLASS_PANE)) return 0;
        if (stack.is(Items.ORANGE_STAINED_GLASS_PANE)) return 1;
        if (stack.is(Items.YELLOW_STAINED_GLASS_PANE)) return 2;
        if (stack.is(Items.GREEN_STAINED_GLASS_PANE)) return 3;
        if (stack.is(Items.BLUE_STAINED_GLASS_PANE)) return 4;
        return -1;
    }

    private static String findColor(String title) {
        if (title.contains("light blue")) return "light blue";
        for (String color : COLORS) if (title.contains(color)) return color;
        return null;
    }

    private static boolean matchesColor(ItemStack stack, String color) {
        if (stack.isEmpty() || stack.hasFoil() || isCompletionMarker(stack)
                || stack.is(Items.BLACK_STAINED_GLASS_PANE)) return false;
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().toLowerCase(Locale.ROOT);
        String searchable = name + " " + id.replace('_', ' ');
        if (searchable.contains(color)) return true;

        String[] aliases = switch (color) {
            case "red" -> new String[]{"redstone", "apple", "poppy", "rose", "spider eye",
                    "nether wart", "brick", "beetroot", "cherry", "magma"};
            case "orange" -> new String[]{"carrot", "pumpkin", "blaze", "copper", "fire coral"};
            case "yellow" -> new String[]{"gold", "sunflower", "dandelion", "glowstone",
                    "hay bale", "sponge", "honey", "potato"};
            case "green", "lime" -> new String[]{"emerald", "cactus", "slime", "kelp",
                    "seagrass", "fern", "vine", "bamboo", "melon", "moss"};
            case "blue" -> new String[]{"lapis", "cornflower", "water bucket", "blue ice"};
            case "light blue" -> new String[]{"diamond", "packed ice", "ice", "blue orchid"};
            case "cyan" -> new String[]{"prismarine", "warped", "heart of the sea"};
            case "purple" -> new String[]{"amethyst", "chorus", "purpur", "shulker"};
            case "magenta" -> new String[]{"allium", "dragon breath"};
            case "pink" -> new String[]{"porkchop", "peony", "pink petals"};
            case "brown" -> new String[]{"leather", "cocoa", "dirt", "soul sand", "brown mushroom",
                    "spruce", "oak", "stick", "chest", "crafting table", "rabbit hide",
                    "rotten flesh", "wood", "log", "mud", "granite", "dripstone"};
            case "white" -> new String[]{"bone", "quartz", "snow", "sugar", "egg",
                    "feather", "paper", "ghast tear"};
            case "gray", "grey" -> new String[]{"iron", "stone", "cobble", "gravel",
                    "flint", "clay", "ash"};
            case "black" -> new String[]{"coal", "obsidian", "ink sac", "wither",
                    "blackstone", "charcoal"};
            default -> new String[0];
        };
        for (String alias : aliases) {
            if (searchable.contains(alias)) return true;
        }
        return false;
    }

    private static boolean isCompletionMarker(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String name = stack.getHoverName().getString().toLowerCase(Locale.ROOT);
        return id.contains("stained_glass_pane")
                && (name.contains("selected") || name.contains("completed")
                || id.contains("lime") || id.contains("green"));
    }

    private static String itemName(Slot slot) {
        ItemStack stack = slot.getItem();
        return stack.isEmpty() ? "" : stack.getHoverName().getString().toLowerCase(Locale.ROOT);
    }

    private record Target(Slot slot, int clicks) {
    }
}
