package dtm.stools.component.menu.bar.config;

import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class MenuBarStyle {

    private Color background;
    private Color backgroundGradientEnd;
    private double backgroundGradientAngle = 0;
    private float[] backgroundGradientStops;
    private Color[] backgroundGradientColors;
    private Color popupBackground;
    private Color foreground;
    private Color hoverForeground;
    private Color selectedForeground;
    private Color borderColor;
    private Color popupBorderColor;
    private Color hoverBackground;
    private Color selectedBackground;
    private Color accent;
    private Color separatorColor;

    private Font menuFont;
    private Font itemFont;

    private Insets barPadding = new Insets(4, 8, 4, 8);
    private Insets menuPadding = new Insets(4, 10, 4, 10);
    private Insets itemPadding = new Insets(6, 14, 6, 18);
    private Insets popupPadding = new Insets(6, 6, 6, 6);

    private int itemHeight = 28;
    private int minItemWidth = 180;
    private int barHeight = 0;

    private int hoverArc = 8;
    private int itemArc = 6;
    private int popupArc = 10;
    private int gap = 2;

    private boolean accentIndicator = true;
    private int accentIndicatorHeight = 2;
    private boolean drawBottomBorder = true;

    public static MenuBarStyle auto() {
        return new MenuBarStyle();
    }

    public static MenuBarStyle modern() {
        MenuBarStyle s = new MenuBarStyle();
        s.barPadding = new Insets(5, 8, 5, 8);
        s.menuPadding = new Insets(5, 12, 5, 12);
        s.itemPadding = new Insets(7, 14, 7, 18);
        s.popupPadding = new Insets(6, 6, 6, 6);
        s.itemHeight = 30;
        s.minItemWidth = 180;
        s.hoverArc = 8;
        s.itemArc = 7;
        s.popupArc = 10;
        s.accentIndicator = true;
        s.accentIndicatorHeight = 2;
        s.drawBottomBorder = true;
        return s;
    }

    public static MenuBarStyle dark() {
        MenuBarStyle s = new MenuBarStyle();
        s.background = new Color(0x2B2D30);
        s.backgroundGradientEnd = new Color(0x1F2124);
        s.popupBackground = new Color(0x2B2D30);
        s.hoverForeground = Color.WHITE;
        s.selectedForeground = Color.WHITE;
        s.borderColor = new Color(0x1B1D20);
        s.popupBorderColor = new Color(0x3C3F41);
        s.hoverBackground = new Color(0x3C3F41);
        s.selectedBackground = new Color(0x4B6EAF);
        s.accent = new Color(0x3592C4);
        s.separatorColor = new Color(0x3C3F41);
        s.menuFont = new Font("Segoe UI", Font.PLAIN, 12);
        s.itemFont = new Font("Segoe UI", Font.PLAIN, 12);
        return s;
    }

    public static MenuBarStyle light() {
        MenuBarStyle s = new MenuBarStyle();
        s.background = new Color(0xF7F8FA);
        s.backgroundGradientEnd = new Color(0xEDEEF0);
        s.popupBackground = Color.WHITE;
        s.hoverForeground = new Color(0x111827);
        s.selectedForeground = Color.WHITE;
        s.borderColor = new Color(0xD3D5DB);
        s.popupBorderColor = new Color(0xD3D5DB);
        s.hoverBackground = new Color(0xE5E7EB);
        s.selectedBackground = new Color(0x3574F0);
        s.accent = new Color(0x3574F0);
        s.separatorColor = new Color(0xE5E7EB);
        s.menuFont = new Font("Segoe UI", Font.PLAIN, 12);
        s.itemFont = new Font("Segoe UI", Font.PLAIN, 12);
        return s;
    }

    public static MenuBarStyle compact() {
        return modern()
                .barPadding(new Insets(2, 6, 2, 6))
                .menuPadding(new Insets(3, 8, 3, 8))
                .itemPadding(new Insets(4, 10, 4, 12))
                .barHeight(30)
                .itemHeight(24)
                .minItemWidth(150)
                .arcs(6, 5, 8);
    }

    public static MenuBarStyle flat() {
        return modern()
                .clearBackgroundGradient()
                .accentIndicator(false)
                .bottomBorder(true)
                .arcs(4, 4, 8);
    }

    public static MenuBarStyle toolbar() {
        return modern()
                .barPadding(new Insets(6, 8, 6, 8))
                .menuPadding(new Insets(5, 11, 5, 11))
                .barHeight(42)
                .accentIndicator(false)
                .bottomBorder(false)
                .arcs(10, 8, 10);
    }

    public MenuBarStyle copy() {
        MenuBarStyle s = new MenuBarStyle();
        copyTo(s);
        return s;
    }

    public void copyTo(MenuBarStyle target) {
        target.background = background;
        target.backgroundGradientEnd = backgroundGradientEnd;
        target.backgroundGradientAngle = backgroundGradientAngle;
        target.backgroundGradientStops = backgroundGradientStops == null ? null : backgroundGradientStops.clone();
        target.backgroundGradientColors = backgroundGradientColors == null ? null : backgroundGradientColors.clone();
        target.popupBackground = popupBackground;
        target.foreground = foreground;
        target.hoverForeground = hoverForeground;
        target.selectedForeground = selectedForeground;
        target.borderColor = borderColor;
        target.popupBorderColor = popupBorderColor;
        target.hoverBackground = hoverBackground;
        target.selectedBackground = selectedBackground;
        target.accent = accent;
        target.separatorColor = separatorColor;
        target.menuFont = menuFont;
        target.itemFont = itemFont;
        target.barPadding = copyInsets(barPadding);
        target.menuPadding = copyInsets(menuPadding);
        target.itemPadding = copyInsets(itemPadding);
        target.popupPadding = copyInsets(popupPadding);
        target.itemHeight = itemHeight;
        target.minItemWidth = minItemWidth;
        target.barHeight = barHeight;
        target.hoverArc = hoverArc;
        target.itemArc = itemArc;
        target.popupArc = popupArc;
        target.gap = gap;
        target.accentIndicator = accentIndicator;
        target.accentIndicatorHeight = accentIndicatorHeight;
        target.drawBottomBorder = drawBottomBorder;
    }

    public MenuBarStyle background(Color color) { this.background = color; return this; }
    public MenuBarStyle foreground(Color color) { this.foreground = color; return this; }
    public MenuBarStyle accent(Color color) { this.accent = color; return this; }
    public MenuBarStyle selectedBackground(Color color) { this.selectedBackground = color; return this; }
    public MenuBarStyle hoverBackground(Color color) { this.hoverBackground = color; return this; }
    public MenuBarStyle popupBackground(Color color) { this.popupBackground = color; return this; }

    public MenuBarStyle backgroundGradient(Color start, Color end, double angleDegrees) {
        this.background = start;
        this.backgroundGradientEnd = end;
        this.backgroundGradientAngle = angleDegrees;
        this.backgroundGradientStops = null;
        this.backgroundGradientColors = null;
        return this;
    }

    public MenuBarStyle backgroundGradient(double angleDegrees, GradientStop... stops) {
        if (stops == null || stops.length < 2) {
            this.backgroundGradientStops = null;
            this.backgroundGradientColors = null;
            return this;
        }
        GradientStop[] sorted = stops.clone();
        Arrays.sort(sorted, (a, b) -> Float.compare(a.getPosition(), b.getPosition()));
        List<Float> fractions = new ArrayList<>(sorted.length);
        List<Color> colors = new ArrayList<>(sorted.length);
        float lastPos = -1f;
        for (GradientStop s : sorted) {
            float p = s.getPosition();
            if (p <= lastPos) p = Math.min(1f, lastPos + 0.0001f);
            fractions.add(p);
            colors.add(s.getColor());
            lastPos = p;
        }
        float[] fracArr = new float[fractions.size()];
        for (int i = 0; i < fractions.size(); i++) fracArr[i] = fractions.get(i);
        this.backgroundGradientStops = fracArr;
        this.backgroundGradientColors = colors.toArray(new Color[0]);
        this.backgroundGradientAngle = angleDegrees;
        this.background = colors.get(0);
        this.backgroundGradientEnd = colors.get(colors.size() - 1);
        return this;
    }

    public MenuBarStyle clearBackgroundGradient() {
        this.backgroundGradientEnd = null;
        this.backgroundGradientStops = null;
        this.backgroundGradientColors = null;
        return this;
    }

    public MenuBarStyle menuFont(Font font) { this.menuFont = font; return this; }
    public MenuBarStyle itemFont(Font font) { this.itemFont = font; return this; }
    public MenuBarStyle barPadding(Insets padding) { this.barPadding = copyInsets(padding); return this; }
    public MenuBarStyle menuPadding(Insets padding) { this.menuPadding = copyInsets(padding); return this; }
    public MenuBarStyle itemPadding(Insets padding) { this.itemPadding = copyInsets(padding); return this; }
    public MenuBarStyle popupPadding(Insets padding) { this.popupPadding = copyInsets(padding); return this; }
    public MenuBarStyle barHeight(int barHeight) { this.barHeight = Math.max(0, barHeight); return this; }
    public MenuBarStyle itemHeight(int itemHeight) { this.itemHeight = Math.max(0, itemHeight); return this; }
    public MenuBarStyle minItemWidth(int minItemWidth) { this.minItemWidth = Math.max(0, minItemWidth); return this; }

    public MenuBarStyle arcs(int menuArc, int itemArc, int popupArc) {
        this.hoverArc = Math.max(0, menuArc);
        this.itemArc = Math.max(0, itemArc);
        this.popupArc = Math.max(0, popupArc);
        return this;
    }

    public MenuBarStyle accentIndicator(boolean accentIndicator) { this.accentIndicator = accentIndicator; return this; }
    public MenuBarStyle bottomBorder(boolean drawBottomBorder) { this.drawBottomBorder = drawBottomBorder; return this; }

    private static Insets copyInsets(Insets insets) {
        return insets == null ? new Insets(0, 0, 0, 0) : new Insets(insets.top, insets.left, insets.bottom, insets.right);
    }
}
