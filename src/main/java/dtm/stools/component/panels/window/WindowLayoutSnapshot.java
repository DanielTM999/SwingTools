package dtm.stools.component.panels.window;

import java.awt.*;
import java.util.List;

public record WindowLayoutSnapshot(List<WindowSnapshot> windows, String activeWindowKey) {
    public WindowLayoutSnapshot {
        windows = windows == null ? List.of() : List.copyOf(windows);
    }

    public record WindowSnapshot(String key, Rectangle bounds, Rectangle normalBounds,
                                 WindowState state, WindowSnap snap, int zOrder, boolean visible) {
        public WindowSnapshot {
            bounds = bounds == null ? null : new Rectangle(bounds);
            normalBounds = normalBounds == null ? null : new Rectangle(normalBounds);
            state = state == null ? WindowState.NORMAL : state;
            snap = snap == null ? WindowSnap.NONE : snap;
        }

        @Override public Rectangle bounds() { return bounds == null ? null : new Rectangle(bounds); }
        @Override public Rectangle normalBounds() { return normalBounds == null ? null : new Rectangle(normalBounds); }
    }
}
