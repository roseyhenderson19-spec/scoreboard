package dev.spice.spicescoreboard;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

final class Format {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .build();

    private static final String[] SUFFIXES = {"", "K", "M", "B", "T", "Q"};

    private Format() {}

    /** MiniMessage if the text contains tags, otherwise legacy &-codes. */
    static Component parseText(String raw) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        int open = raw.indexOf('<');
        if (open >= 0 && raw.indexOf('>', open) > open) {
            return MM.deserialize(raw.replace('§', '&').contains("&") ? legacyToMini(raw) : raw);
        }
        return LEGACY.deserialize(raw.replace('§', '&'));
    }

    /** Mixed text like "&c<bold>Owner": convert the legacy bits so MiniMessage can parse everything. */
    private static String legacyToMini(String raw) {
        String s = raw.replace('§', '&');
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length()) {
                String tag = legacyTag(s.charAt(i + 1));
                if (tag != null) {
                    out.append(tag);
                    i++;
                    continue;
                }
            }
            out.append(c);
        }
        return out.toString();
    }

    private static String legacyTag(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> "<black>";
            case '1' -> "<dark_blue>";
            case '2' -> "<dark_green>";
            case '3' -> "<dark_aqua>";
            case '4' -> "<dark_red>";
            case '5' -> "<dark_purple>";
            case '6' -> "<gold>";
            case '7' -> "<gray>";
            case '8' -> "<dark_gray>";
            case '9' -> "<blue>";
            case 'a' -> "<green>";
            case 'b' -> "<aqua>";
            case 'c' -> "<red>";
            case 'd' -> "<light_purple>";
            case 'e' -> "<yellow>";
            case 'f' -> "<white>";
            case 'l' -> "<bold>";
            case 'o' -> "<italic>";
            case 'n' -> "<underlined>";
            case 'm' -> "<strikethrough>";
            case 'k' -> "<obfuscated>";
            case 'r' -> "<reset>";
            default -> null;
        };
    }

    static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    static String money(double amount, boolean abbreviated, String symbol) {
        DecimalFormatSymbols syms = DecimalFormatSymbols.getInstance(Locale.US);
        if (!abbreviated) {
            return symbol + new DecimalFormat("#,##0.00", syms).format(amount);
        }
        double x = amount;
        int i = 0;
        while (Math.abs(x) >= 1000 && i < SUFFIXES.length - 1) {
            x /= 1000;
            i++;
        }
        String pattern = i == 0 ? "#,##0.##" : "0.##";
        return symbol + new DecimalFormat(pattern, syms).format(x) + SUFFIXES[i];
    }

    static String duration(long totalSeconds) {
        long d = totalSeconds / 86400;
        long h = (totalSeconds % 86400) / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        if (d > 0) return d + "d " + h + "h " + m + "m";
        if (h > 0) return h + "h " + m + "m";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }
}
