package dtm.stools.component.panels.window;

import dtm.stools.component.icon.FittedIcon;

import javax.swing.*;
import java.awt.*;

public class DefaultWindowMinimizedButtonFactory implements WindowMinimizedButtonFactory {
    @Override
    public AbstractButton createButton(WindowMinimizedBar bar, WindowPanel window) {
        JButton button = new JButton(window.getTitle(), resolveIcon(bar, window));
        button.setFocusable(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        if (button.getIcon() == null) button.setIconTextGap(0);
        button.setPreferredSize(buttonSize(bar));
        installAction(button, window);
        return button;
    }

    protected Icon resolveIcon(WindowMinimizedBar bar, WindowPanel window) {
        return FittedIcon.fit(window.getIcon(), iconSize(bar));
    }

    protected int iconSize(WindowMinimizedBar bar) {
        int available = (bar == null ? 38 : bar.getExpandedHeight()) - 18;
        return Math.max(12, Math.min(24, available));
    }

    protected Dimension buttonSize(WindowMinimizedBar bar) {
        int height = (bar == null ? 38 : bar.getExpandedHeight()) - 10;
        return new Dimension(180, Math.max(20, height));
    }

    protected void installAction(AbstractButton button, WindowPanel window) {
        button.addActionListener(event -> window.restore().activate());
    }
}
