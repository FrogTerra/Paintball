package me.FrogTerra.paintball.utility;

import me.FrogTerra.paintball.Paintball;
import me.FrogTerra.paintball.player.PlayerProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;

/**
 * Utility class for handling MiniMessage formatting and text processing
 */
public class MessageUtils {

    private static final Paintball plugin = Paintball.getPlugin();

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    public static void playerTabListFormat(final PlayerProfile playerProfile) {
        // check is player is in game

        Bukkit.getPlayer(playerProfile.getPlayerId()).playerListName(MessageUtils.parseMessage(MessageUtils.formatPlayer(playerProfile, Formats.TAB)));
    }

    public static String formatPlayer(PlayerProfile profile, Formats format) {
        final User user = plugin.getLuckPerms().getUserManager().getUser(profile.getPlayerId());
        String prefix = "";
        if (user != null) {
            final var cachedData = user.getCachedData();
            final var metaData = cachedData.getMetaData();
            final String prefixNode = metaData.getPrefix();
            if (prefixNode != null && !prefixNode.isEmpty()) prefix = prefixNode + " ";
        }
        final String squadTag = profile.getSquadName() != null ? profile.getSquadName() + " ":""; // TODO:: Squad Color
        final String level = getLevelColor(profile.getLevel()) + convertToRoman(profile.getLevel()) + " ";
        final String prestige = profile.hasPrestige() ? getPrestigeColor(profile.getPrestige()) + profile.getPrestige() + " ":"";
        final String playerName = profile.getPlayerName();
        // TODO:: Player Tags
        return switch (format) {
            case CONNECTION -> prefix + squadTag + playerName;
            case TAB -> prefix + prestige + playerName + squadTag;
            case CHAT -> prefix + squadTag + level + prestige + playerName;
            case NAME -> prefix + playerName + level + prestige;
            default -> "<red>RUH-ROH?";
        };
    }

    /**
     * Parse a MiniMessage string into a Component
     * @param message the message string with MiniMessage formatting
     * @return the parsed Component
     */
    public static Component parseMessage(String message) {
        if (message == null || message.isEmpty()) return Component.empty();
        try {
            return MINI_MESSAGE.deserialize(message);
        } catch (Exception exception) {
            // Fallback to plain text if parsing fails
            return Component.text(message);
        }
    }

    /**
     * Format time in seconds to a readable format
     * @param seconds the time in seconds
     * @return the formatted time string
     */
    public static String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            int minutes = seconds / 60;
            int remainingSeconds = seconds % 60;
            return minutes + "m " + remainingSeconds + "s";
        } else {
            int hours = seconds / 3600;
            int minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }

    /**
     * Calculate K/D ratio with proper formatting
     * @param kills  the number of kills
     * @param deaths the number of deaths
     * @return the formatted K/D ratio
     */
    public static String calculateKD(int kills, int deaths) {
        if (deaths == 0) {
            return kills > 0 ? String.valueOf(kills) : "0.00";
        }

        double ratio = (double) kills / deaths;
        return String.format("%.2f", ratio);
    }

    /**
     * Convert a Component to plain text (strip all formatting)
     * @param component the component to convert
     * @return the plain text string
     */
    public static String stripColors(final Component component) {
        return PLAIN_SERIALIZER.serialize(component);
    }

    /**
     * Convert a MiniMessage string to plain text (strip all formatting)
     * @param message the message string with MiniMessage formatting
     * @return the plain text string
     */
    public static String stripColors(final String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        try {
            final Component component = MINI_MESSAGE.deserialize(message);
            return PLAIN_SERIALIZER.serialize(component);
        } catch (final Exception exception) {
            // Fallback to original message if parsing fails
            return message;
        }
    }

    /**
     * Convert an integer into a roman numeral
     * @param number the number to be converted
     * @return the plain text string
     */
    public static String convertToRoman(final int number) {
        if (number <= 0)
            return "0";

        if (number <= 50 && number < ROMAN_NUMERALS.length)
            return ROMAN_NUMERALS[number];

        // For numbers above 50, use a more complex conversion
        final StringBuilder result = new StringBuilder();

        final int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        final String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

        int num = number;
        for (int i = 0; i < values.length; i++) {
            while (num >= values[i]) {
                num -= values[i];
                result.append(symbols[i]);
            }
        }

        return result.toString();
    }

    /**
     * Return a <color> by providing an integer
     * @param level To reference for <color>
     * @return the <color> from PROGRESSION_COLOURS
     */
    public static String getLevelColor(final int level) {
        final int colorIndex = Math.min((level -1)/100, PROGRESSIONS_COLOURS.length - 1);
        return PROGRESSIONS_COLOURS[colorIndex];
    }

    /**
     * Return a <color> by providing an integer
     * @param prestige To reference for <color>
     * @return the <color> from PROGRESSION_COLOURS
     */
    public static String getPrestigeColor(final int prestige) {
        final int colorIndex = Math.min(prestige, PROGRESSIONS_COLOURS.length - 1);
        return PROGRESSIONS_COLOURS[colorIndex];
    }



    private static final String[] ROMAN_NUMERALS = {
            "", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
            "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX",
            "XXI", "XXII", "XXIII", "XXIV", "XXV", "XXVI", "XXVII", "XXVIII", "XXIX", "XXX",
            "XXXI", "XXXII", "XXXIII", "XXXIV", "XXXV", "XXXVI", "XXXVII", "XXXVIII", "XXXIX", "XL",
            "XLI", "XLII", "XLIII", "XLIV", "XLV", "XLVI", "XLVII", "XLVIII", "XLIX", "L"
    };

    private static final String[] PROGRESSIONS_COLOURS = {
            "<white>",       // 1-99    | 0
            "<blue>",        // 100-199 | 1
            "<dark_blue>",   // 200-299 | 2
            "<green>",       // 300-399 | 3
            "<dark_green>",  // 400-499 | 4
            "<light_purple>",// 500-599 | 5
            "<dark_purple>", // 600-699 | 6
            "<yellow>",      // 700-799 | 7
            "<gold>",        // 800-899 | 8
            "<red>",         // 900-999 | 9
            "<dark_red>"     // 1000    | 10
    };

    public enum Formats {
        CONNECTION, CHAT, TAB, NAME
    }
}


