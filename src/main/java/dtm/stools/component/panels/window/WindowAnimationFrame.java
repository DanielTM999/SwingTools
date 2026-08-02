package dtm.stools.component.panels.window;

import java.awt.*;

public record WindowAnimationFrame(float progress, Rectangle bounds, float alpha) {
    public WindowAnimationFrame {
        progress = Math.max(0f, Math.min(1f, progress));
        bounds = new Rectangle(bounds);
        alpha = Math.max(0f, Math.min(1f, alpha));
    }

    @Override public Rectangle bounds() { return new Rectangle(bounds); }
}
