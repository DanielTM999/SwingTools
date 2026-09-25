package dtm.stools.component.panels.editor.sheet.render;

import dtm.stools.component.panels.editor.sheet.model.SheetTheme;

import java.awt.Color;

public final class SheetPalette {
    private SheetPalette() {}

    public static Color color(int argb) { return new Color(argb, true); }

    public static Color series(int index, SheetTheme theme) {
        int k = index % 6;
        int base = theme.accent(k);
        int round = index / 6;
        if (round == 0) return color(base);
        return color(SheetTheme.tint(base, round % 2 == 1 ? -0.25 : 0.4));
    }

    public static Color contrast(Color c) {
        double l = (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255;
        return l > 0.6 ? Color.BLACK : Color.WHITE;
    }
}
