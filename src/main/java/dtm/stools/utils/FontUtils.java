package dtm.stools.utils;

import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.List;

public final class FontUtils {

    private static volatile Font globalFont;

    private FontUtils() {
        throw new IllegalStateException("utility class");
    }

    public static boolean hasGlobalFont(String fontName) {
        if (fontName == null || fontName.isBlank()) return false;

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (String available : ge.getAvailableFontFamilyNames()) {
            if (available.equalsIgnoreCase(fontName)) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasGlobalFont(Font font) {
        if (font == null) return false;
        return hasGlobalFont(font.getFamily());
    }

    public static Font getGlobalFont() {
        return globalFont;
    }

    public static void setGlobalFont(Font font) {
        if (font == null) return;

        globalFont = font;

        UIDefaults defaults = UIManager.getDefaults();
        Enumeration<Object> keys = defaults.keys();

        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);

            if (value instanceof Font) {
                UIManager.put(key, font);
            }
        }
    }

    public static boolean isFontInstalled(String fontFamily) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (String availableFont : ge.getAvailableFontFamilyNames()) {
            if (availableFont.equalsIgnoreCase(fontFamily)) {
                return true;
            }
        }

        return false;
    }

    public static List<String> listInstalledFonts() {
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        String[] fontFamilies = graphicsEnvironment.getAvailableFontFamilyNames();

        return Arrays.stream(fontFamilies)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public static Font installFontResource(@NonNull String resourcePath){
        return installFontResource(resourcePath, FontUtils.class);
    }

    public static Font installFontResource(@NonNull String resourcePath, @NonNull Class<?> resourceClass) {
        Objects.requireNonNull(resourcePath, "resourcePath cannot be null");
        Objects.requireNonNull(resourceClass, "resourceClass cannot be null");

        if (resourcePath.isBlank()) {
            throw new IllegalArgumentException("resourcePath cannot be blank");
        }

        try(InputStream inputStream = ResourceUtils.getResourceAsStream(resourceClass, resourcePath)){
            Font font = Font.createFont(Font.TRUETYPE_FONT, inputStream);
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            graphicsEnvironment.registerFont(font);
            return font;
        } catch (FontFormatException e) {
            throw new IllegalArgumentException("Invalid font format: " + resourcePath, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read font resource: " + resourcePath, e);
        }
    }

    public static List<Font> installFontResources(@NonNull Collection<String> resourcePaths) {
        return installFontResources(resourcePaths, FontUtils.class);
    }

    public static List<Font> installFontResources(@NonNull Collection<String> resourcePaths, @NonNull Class<?> resourceClass) {
        Objects.requireNonNull(resourcePaths, "resourcePaths cannot be null");
        Objects.requireNonNull(resourceClass, "resourceClass cannot be null");

        if (resourcePaths.isEmpty()) {
            return List.of();
        }

        return resourcePaths.stream()
                .map(resourcePath -> {
                    try {
                        return installFontResource(resourcePath, resourceClass);
                    } catch (RuntimeException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();
    }

    public static Font installFontResource(String resourcePath, float size) {
        Font font = installFontResource(resourcePath);
        return font.deriveFont(Font.PLAIN, size);
    }

    public static Font installFontResource(String resourcePath, int style, float size) {
        Font font = installFontResource(resourcePath);
        return font.deriveFont(style, size);
    }

}
