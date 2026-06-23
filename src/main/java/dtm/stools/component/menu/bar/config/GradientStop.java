package dtm.stools.component.menu.bar.config;

import java.awt.Color;
import java.util.Objects;

public final class GradientStop {

    private final float position;
    private final Color color;

    public GradientStop(float position, Color color) {
        Objects.requireNonNull(color, "color");
        this.position = Math.max(0f, Math.min(1f, position));
        this.color = color;
    }

    public static GradientStop at(float position, Color color) {
        return new GradientStop(position, color);
    }

    public static GradientStop atPercent(double percent, Color color) {
        return new GradientStop((float) (percent / 100.0), color);
    }

    public float getPosition() {
        return position;
    }

    public Color getColor() {
        return color;
    }
}
