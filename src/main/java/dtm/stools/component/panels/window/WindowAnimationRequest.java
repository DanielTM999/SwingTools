package dtm.stools.component.panels.window;

import java.awt.*;

public class WindowAnimationRequest {
    private final WindowPanel window;
    private final WindowAnimationType type;
    private final Rectangle fromBounds;
    private final Rectangle toBounds;
    private final float fromAlpha;
    private final float toAlpha;
    private final int durationMillis;

    public WindowAnimationRequest(WindowPanel window, WindowAnimationType type,
                                  Rectangle fromBounds, Rectangle toBounds,
                                  float fromAlpha, float toAlpha, int durationMillis) {
        this.window = window;
        this.type = type;
        this.fromBounds = new Rectangle(fromBounds);
        this.toBounds = new Rectangle(toBounds);
        this.fromAlpha = fromAlpha;
        this.toAlpha = toAlpha;
        this.durationMillis = Math.max(0, durationMillis);
    }

    public WindowPanel getWindow() { return window; }
    public WindowAnimationType getType() { return type; }
    public Rectangle getFromBounds() { return new Rectangle(fromBounds); }
    public Rectangle getToBounds() { return new Rectangle(toBounds); }
    public float getFromAlpha() { return fromAlpha; }
    public float getToAlpha() { return toAlpha; }
    public int getDurationMillis() { return durationMillis; }
}
