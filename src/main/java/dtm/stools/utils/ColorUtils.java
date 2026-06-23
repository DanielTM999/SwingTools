package dtm.stools.utils;

import java.awt.*;
import java.util.Locale;
import java.util.Objects;

public class ColorUtils {

    private ColorUtils(){
            throw new IllegalStateException("utility class");
    }

    public static String toHex(Color color) {
        return toHex(color, false, true);
    }

    public static String toHex(Color color, boolean includeAlpha) {
        return toHex(color, includeAlpha, true);
    }

    public static String toHex(Color color, boolean includeAlpha, boolean withHash) {
        Objects.requireNonNull(color, "color");

        String prefix = withHash ? "#" : "";

        if (includeAlpha) {
            return prefix + String.format(
                    "%02X%02X%02X%02X",
                    color.getAlpha(),
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue()
            );
        }

        return prefix + String.format(
                "%02X%02X%02X",
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );
    }

    public static String toRgbHex(Color color) {
        return toHex(color, false, true);
    }

    public static String toArgbHex(Color color) {
        return toHex(color, true, true);
    }

    public static Color fromHex(String hex) {
        return fromHex(hex, true);
    }

    public static Color fromHex(String hex, boolean allowAlpha) {
        if (hex == null || hex.isBlank()) {
            throw new IllegalArgumentException("Hex color cannot be null or blank");
        }

        String value = normalizeHex(hex);

        if (value.length() == 3) {
            value = expandShortRgb(value);
        } else if (value.length() == 4) {
            if (!allowAlpha) {
                throw new IllegalArgumentException("Alpha channel is not allowed: " + hex);
            }
            value = expandShortArgb(value);
        }

        if (value.length() == 6) {
            int rgb = parseHexInt(value, hex);
            return new Color(rgb);
        }

        if (value.length() == 8) {
            if (!allowAlpha) {
                throw new IllegalArgumentException("Alpha channel is not allowed: " + hex);
            }

            int argb = parseHexInt(value, hex);

            int alpha = (argb >> 24) & 0xFF;
            int red = (argb >> 16) & 0xFF;
            int green = (argb >> 8) & 0xFF;
            int blue = argb & 0xFF;

            return new Color(red, green, blue, alpha);
        }

        throw new IllegalArgumentException(
                "Invalid hex color. Use #RGB, #ARGB, #RRGGBB or #AARRGGBB: " + hex
        );
    }

    public static Color fromRgbHex(String hex) {
        Color color = fromHex(hex, false);
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    public static Color fromArgbHex(String hex) {
        return fromHex(hex, true);
    }

    public static Color fromRgb(int red, int green, int blue) {
        return new Color(
                clamp(red),
                clamp(green),
                clamp(blue)
        );
    }

    public static Color fromRgba(int red, int green, int blue, int alpha) {
        return new Color(
                clamp(red),
                clamp(green),
                clamp(blue),
                clamp(alpha)
        );
    }

    public static Color withAlpha(Color color, int alpha) {
        Objects.requireNonNull(color, "color");

        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                clamp(alpha)
        );
    }

    public static Color withAlpha(Color color, float alpha) {
        return withAlpha(color, Math.round(clamp01(alpha) * 255f));
    }

    public static Color darker(Color color, float factor) {
        Objects.requireNonNull(color, "color");

        factor = clamp01(factor);

        return new Color(
                Math.round(color.getRed() * (1f - factor)),
                Math.round(color.getGreen() * (1f - factor)),
                Math.round(color.getBlue() * (1f - factor)),
                color.getAlpha()
        );
    }

    public static Color brighter(Color color, float factor) {
        Objects.requireNonNull(color, "color");

        factor = clamp01(factor);

        return new Color(
                Math.round(color.getRed() + (255 - color.getRed()) * factor),
                Math.round(color.getGreen() + (255 - color.getGreen()) * factor),
                Math.round(color.getBlue() + (255 - color.getBlue()) * factor),
                color.getAlpha()
        );
    }

    public static Color mix(Color first, Color second, float ratio) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");

        ratio = clamp01(ratio);
        float inverse = 1f - ratio;

        return new Color(
                Math.round(first.getRed() * inverse + second.getRed() * ratio),
                Math.round(first.getGreen() * inverse + second.getGreen() * ratio),
                Math.round(first.getBlue() * inverse + second.getBlue() * ratio),
                Math.round(first.getAlpha() * inverse + second.getAlpha() * ratio)
        );
    }

    public static Color foregroundFor(Color background) {
        Objects.requireNonNull(background, "background");

        return isDark(background) ? Color.WHITE : Color.BLACK;
    }

    public static boolean isDark(Color color) {
        Objects.requireNonNull(color, "color");

        double luminance = luminance(color);
        return luminance < 0.5;
    }

    public static double luminance(Color color) {
        Objects.requireNonNull(color, "color");

        double red = linearize(color.getRed() / 255.0);
        double green = linearize(color.getGreen() / 255.0);
        double blue = linearize(color.getBlue() / 255.0);

        return 0.2126 * red + 0.7152 * green + 0.0722 * blue;
    }

    public static int clamp(int value) {
        return Math.clamp(value, 0, 255);
    }

    public static float clamp01(float value) {
        return Math.clamp(value, 0f, 1f);
    }

    private static String normalizeHex(String hex) {
        String value = hex.trim()
                .replace("_", "")
                .replace(" ", "")
                .toUpperCase(Locale.ROOT);

        if (value.startsWith("#")) {
            value = value.substring(1);
        }

        if (value.startsWith("0X")) {
            value = value.substring(2);
        }

        if (!value.matches("[0-9A-F]+")) {
            throw new IllegalArgumentException("Invalid hex color: " + hex);
        }

        return value;
    }

    private static String expandShortRgb(String value) {
        return ""
                + value.charAt(0) + value.charAt(0)
                + value.charAt(1) + value.charAt(1)
                + value.charAt(2) + value.charAt(2);
    }

    private static String expandShortArgb(String value) {
        return ""
                + value.charAt(0) + value.charAt(0)
                + value.charAt(1) + value.charAt(1)
                + value.charAt(2) + value.charAt(2)
                + value.charAt(3) + value.charAt(3);
    }

    private static int parseHexInt(String value, String original) {
        try {
            return (int) Long.parseLong(value, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid hex color: " + original, e);
        }
    }

    private static double linearize(double value) {
        if (value <= 0.03928) {
            return value / 12.92;
        }

        return Math.pow((value + 0.055) / 1.055, 2.4);
    }
}
