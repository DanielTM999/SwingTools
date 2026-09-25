package dtm.stools.component.panels.editor.sheet.model;

import java.util.List;
import java.util.Objects;

public record SheetTheme(String name, List<Integer> colors, String majorFont, String minorFont) {
    public static final SheetTheme OFFICE = new SheetTheme("Office", List.of(0xFF000000, 0xFFFFFFFF, 0xFF0E2841, 0xFFE8E8E8, 0xFF156082, 0xFFE97132,
            0xFF196B24, 0xFF0F9ED5, 0xFFA02B93, 0xFF4EA72E, 0xFF467886, 0xFF96607D), "Aptos Display", "Aptos Narrow");

    public SheetTheme {
        Objects.requireNonNull(name);
        colors = List.copyOf(colors);
        if (colors.size() != 12) throw new IllegalArgumentException("A theme needs 12 colors");
        majorFont = Objects.requireNonNullElse(majorFont, "Calibri Light");
        minorFont = Objects.requireNonNullElse(minorFont, "Calibri");
    }

    public int color(int index) { return colors.get(Math.max(0, Math.min(11, index))); }
    public int accent(int n) { return colors.get(4 + Math.max(0, Math.min(5, n))); }

    public static int tint(int argb, double tint) {
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        if (tint < 0) { r = (int) (r * (1 + tint)); g = (int) (g * (1 + tint)); b = (int) (b * (1 + tint)); }
        else { r = (int) (r + (255 - r) * tint); g = (int) (g + (255 - g) * tint); b = (int) (b + (255 - b) * tint); }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
