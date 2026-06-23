package dtm.stools.component.annimation;

import javax.swing.*;

public interface ComponentAnimator<T extends JComponent> {
    void animate(T from, T to, Runnable onComplete);
}
