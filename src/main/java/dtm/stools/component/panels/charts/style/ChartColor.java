package dtm.stools.component.panels.charts.style;

import java.awt.Color;

public final class ChartColor {

    public static final ChartColor TRANSPARENT = new ChartColor(0f, 0f, 0f, 0f);
    public static final ChartColor WHITE = new ChartColor(1f, 1f, 1f, 1f);
    public static final ChartColor BLACK = new ChartColor(0f, 0f, 0f, 1f);

    private final float r;
    private final float g;
    private final float b;
    private final float a;

    private ChartColor(float r, float g, float b, float a) {
        this.r = clamp(r);
        this.g = clamp(g);
        this.b = clamp(b);
        this.a = clamp(a);
    }

    public static ChartColor of(float r, float g, float b) {
        return new ChartColor(r, g, b, 1f);
    }

    public static ChartColor of(float r, float g, float b, float a) {
        return new ChartColor(r, g, b, a);
    }

    public static ChartColor rgb(int r, int g, int b) {
        return new ChartColor(r / 255f, g / 255f, b / 255f, 1f);
    }

    public static ChartColor rgba(int r, int g, int b, float alpha) {
        return new ChartColor(r / 255f, g / 255f, b / 255f, alpha);
    }

    public static ChartColor hex(String hex) {
        String value = hex.startsWith("#") ? hex.substring(1) : hex;
        if (value.length() == 3) {
            int r = Integer.parseInt(value.substring(0, 1), 16);
            int g = Integer.parseInt(value.substring(1, 2), 16);
            int b = Integer.parseInt(value.substring(2, 3), 16);
            return rgb(r * 17, g * 17, b * 17);
        }
        int r = Integer.parseInt(value.substring(0, 2), 16);
        int g = Integer.parseInt(value.substring(2, 4), 16);
        int b = Integer.parseInt(value.substring(4, 6), 16);
        float a = value.length() >= 8 ? Integer.parseInt(value.substring(6, 8), 16) / 255f : 1f;
        return new ChartColor(r / 255f, g / 255f, b / 255f, a);
    }

    public static ChartColor fromAwt(Color color) {
        return new ChartColor(color.getRed() / 255f, color.getGreen() / 255f,
                color.getBlue() / 255f, color.getAlpha() / 255f);
    }

    public float r() { return r; }
    public float g() { return g; }
    public float b() { return b; }
    public float a() { return a; }

    public ChartColor withAlpha(float alpha) {
        return new ChartColor(r, g, b, alpha);
    }

    public ChartColor mulAlpha(float factor) {
        return new ChartColor(r, g, b, a * factor);
    }

    public ChartColor lerp(ChartColor other, float t) {
        float k = clamp(t);
        return new ChartColor(
                r + (other.r - r) * k,
                g + (other.g - g) * k,
                b + (other.b - b) * k,
                a + (other.a - a) * k);
    }

    public ChartColor brighter(float amount) {
        return lerpRgb(1f, 1f, 1f, amount);
    }

    public ChartColor darker(float amount) {
        return lerpRgb(0f, 0f, 0f, amount);
    }

    public Color toAwt() {
        return new Color(r, g, b, a);
    }

    private ChartColor lerpRgb(float tr, float tg, float tb, float t) {
        float k = clamp(t);
        return new ChartColor(
                r + (tr - r) * k,
                g + (tg - g) * k,
                b + (tb - b) * k,
                a);
    }

    private static float clamp(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }

    @Override
    public String toString() {
        return "ChartColor(" + r + ", " + g + ", " + b + ", " + a + ")";
    }
}
