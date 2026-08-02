package dtm.stools.component.panels.window;

import java.awt.*;
import java.util.function.Consumer;

public interface WindowAnimator {
    void animate(WindowAnimationRequest request, Consumer<WindowAnimationFrame> frameConsumer, Runnable completion);
    boolean cancel(WindowPanel window);

    default void animateBounds(WindowPanel window, Rectangle from, Rectangle to, Runnable completion) {
        animate(new WindowAnimationRequest(window, WindowAnimationType.RESTORE,
                from, to, 1f, 1f, 180), frame -> {
                    window.setBounds(frame.bounds());
                    window.setAnimationAlpha(frame.alpha());
                }, completion);
    }
}
