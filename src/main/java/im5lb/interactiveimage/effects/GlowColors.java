package im5lb.interactiveimage.effects;

import org.bukkit.Color;
import org.bukkit.ChatColor;

public final class GlowColors {

    private GlowColors() {
    }

    public static Color parse(String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim().toUpperCase();
        return switch (s) {
            case "WHITE" -> Color.fromRGB(0xFFFFFF);
            case "BLACK" -> Color.fromRGB(0x000000);
            case "RED" -> Color.fromRGB(0xFF5555);
            case "GREEN" -> Color.fromRGB(0x55FF55);
            case "BLUE" -> Color.fromRGB(0x5555FF);
            case "YELLOW" -> Color.fromRGB(0xFFFF55);
            case "AQUA", "CYAN" -> Color.fromRGB(0x55FFFF);
            case "PURPLE", "MAGENTA" -> Color.fromRGB(0xFF55FF);
            case "ORANGE" -> Color.fromRGB(0xFFAA00);
            case "PINK" -> Color.fromRGB(0xFFAAAA);
            case "GRAY" -> Color.fromRGB(0xAAAAAA);
            case "DARK_GRAY" -> Color.fromRGB(0x555555);
            case "DARK_RED" -> Color.fromRGB(0xAA0000);
            case "DARK_GREEN" -> Color.fromRGB(0x00AA00);
            case "DARK_BLUE" -> Color.fromRGB(0x0000AA);
            case "GOLD" -> Color.fromRGB(0xFFAA00);
            default -> {
                if (s.startsWith("#") && s.length() == 7) {
                    try {
                        int rgb = Integer.parseInt(s.substring(1), 16);
                        yield Color.fromRGB(rgb);
                    } catch (NumberFormatException e) {
                        yield null;
                    }
                }
                yield null;
            }
        };
    }

    public static ChatColor toChatColor(String input) {
        if (input == null) {
            return null;
        }
        String s = input.trim().toUpperCase();
        return switch (s) {
            case "WHITE" -> ChatColor.WHITE;
            case "BLACK" -> ChatColor.BLACK;
            case "RED" -> ChatColor.RED;
            case "GREEN" -> ChatColor.GREEN;
            case "BLUE" -> ChatColor.BLUE;
            case "YELLOW" -> ChatColor.YELLOW;
            case "AQUA", "CYAN" -> ChatColor.AQUA;
            case "PURPLE", "MAGENTA", "PINK" -> ChatColor.LIGHT_PURPLE;
            case "ORANGE", "GOLD" -> ChatColor.GOLD;
            case "GRAY" -> ChatColor.GRAY;
            case "DARK_GRAY" -> ChatColor.DARK_GRAY;
            case "DARK_RED" -> ChatColor.DARK_RED;
            case "DARK_GREEN" -> ChatColor.DARK_GREEN;
            case "DARK_BLUE" -> ChatColor.DARK_BLUE;
            default -> null;
        };
    }
}
