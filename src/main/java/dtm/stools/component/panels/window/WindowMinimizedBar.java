package dtm.stools.component.panels.window;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class WindowMinimizedBar extends JPanel {
    public static final String PROPERTY_EXPANDED = "expanded";
    public static final String PROPERTY_AUTO_HIDE = "autoHideEnabled";

    private final Timer collapseDelayTimer;
    private Timer sizeAnimationTimer;
    private boolean autoHideEnabled;
    private boolean expanded = true;
    private boolean available;
    private boolean popupActive;
    private int expandedHeight = 38;
    private int collapsedHeight = 3;
    private int collapseDelayMillis = 700;
    private int animationDurationMillis = 170;

    public WindowMinimizedBar() {
        super(new FlowLayout(FlowLayout.LEFT, 6, 4));
        Color background = UIManager.getColor("Panel.background");
        setBackground(background == null ? new Color(0x292B2E) : background.darker());
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                UIManager.getColor("Component.borderColor") == null
                        ? new Color(0x4B4D50) : UIManager.getColor("Component.borderColor")));
        setPreferredSize(new Dimension(10, expandedHeight));
        setOpaque(true);
        setVisible(false);

        collapseDelayTimer = new Timer(collapseDelayMillis, event -> {
            if (!popupActive && !isPointerInside()) collapse();
        });
        collapseDelayTimer.setRepeats(false);

        MouseAdapter hoverHandler = new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) {
                collapseDelayTimer.stop();
                if (autoHideEnabled && available) expand();
            }

            @Override public void mouseExited(MouseEvent event) {
                if (autoHideEnabled && available) scheduleCollapse();
            }
        };
        addMouseListener(hoverHandler);
    }

    public AbstractButton createWindowButton(WindowPanel window) {
        JButton button = new JButton(window.getTitle(), window.getIcon());
        button.setFocusable(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setPreferredSize(new Dimension(180, 28));
        button.addActionListener(event -> window.restore().activate());
        return button;
    }

    public WindowMinimizedBar autoHideEnabled(boolean value) {
        if (autoHideEnabled == value) return this;
        boolean old = autoHideEnabled;
        autoHideEnabled = value;
        collapseDelayTimer.stop();
        if (!value) {
            expand();
        } else if (available) {
            expand();
            scheduleCollapse();
        }
        firePropertyChange(PROPERTY_AUTO_HIDE, old, value);
        return this;
    }

    public WindowMinimizedBar expandedHeight(int value) {
        expandedHeight = Math.max(24, value);
        if (expanded || !autoHideEnabled) setBarHeight(expandedHeight);
        return this;
    }

    public WindowMinimizedBar collapsedHeight(int value) {
        collapsedHeight = Math.max(1, Math.min(8, value));
        if (!expanded && autoHideEnabled) setBarHeight(collapsedHeight);
        return this;
    }

    public WindowMinimizedBar collapseDelay(int milliseconds) {
        collapseDelayMillis = Math.max(0, milliseconds);
        collapseDelayTimer.setInitialDelay(collapseDelayMillis);
        return this;
    }

    public WindowMinimizedBar animationDuration(int milliseconds) {
        animationDurationMillis = Math.max(0, milliseconds);
        return this;
    }

    public WindowMinimizedBar expand() {
        if (!available) return this;
        setExpanded(true, expandedHeight);
        return this;
    }

    public WindowMinimizedBar collapse() {
        if (!autoHideEnabled || !available) return this;
        setExpanded(false, collapsedHeight);
        return this;
    }

    public void setAvailable(boolean value) {
        if (available == value) return;
        available = value;
        if (!value) {
            collapseDelayTimer.stop();
            stopSizeAnimation();
            expanded = true;
            setBarHeight(expandedHeight);
            setVisible(false);
            return;
        }
        setVisible(true);
        if (autoHideEnabled) {
            expand();
            scheduleCollapse();
        } else {
            expand();
        }
    }

    protected void scheduleCollapse() {
        collapseDelayTimer.setInitialDelay(collapseDelayMillis);
        collapseDelayTimer.restart();
    }

    protected boolean isPointerInside() {
        if (popupActive) return true;
        try {
            Point point = getMousePosition(true);
            return point != null;
        } catch (HeadlessException | IllegalComponentStateException ignored) {
            return false;
        }
    }

    protected void setExpanded(boolean value, int targetHeight) {
        collapseDelayTimer.stop();
        boolean old = expanded;
        expanded = value;
        animateHeight(targetHeight);
        if (old != value) firePropertyChange(PROPERTY_EXPANDED, old, value);
    }

    protected void animateHeight(int targetHeight) {
        stopSizeAnimation();
        int fromHeight = Math.max(1, getPreferredSize().height);
        if (animationDurationMillis == 0 || !isShowing() || fromHeight == targetHeight) {
            setBarHeight(targetHeight);
            return;
        }
        long started = System.nanoTime();
        sizeAnimationTimer = new Timer(15, event -> {
            float elapsed = (System.nanoTime() - started) / 1_000_000f;
            float progress = Math.min(1f, elapsed / animationDurationMillis);
            float inverse = 1f - progress;
            float eased = 1f - inverse * inverse * inverse;
            setBarHeight(Math.round(fromHeight + (targetHeight - fromHeight) * eased));
            if (progress >= 1f) {
                stopSizeAnimation();
                setBarHeight(targetHeight);
            }
        });
        sizeAnimationTimer.start();
    }

    protected void setBarHeight(int height) {
        setPreferredSize(new Dimension(Math.max(10, getPreferredSize().width), Math.max(1, height)));
        revalidate();
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.doLayout();
            parent.repaint();
        }
    }

    protected void stopSizeAnimation() {
        if (sizeAnimationTimer != null) {
            sizeAnimationTimer.stop();
            sizeAnimationTimer = null;
        }
    }

    public boolean isAutoHideEnabled() { return autoHideEnabled; }
    public boolean isExpanded() { return expanded; }
    public boolean isAvailable() { return available; }
    public boolean isPopupActive() { return popupActive; }
    public int getExpandedHeight() { return expandedHeight; }
    public int getCollapsedHeight() { return collapsedHeight; }
    public int getCollapseDelayMillis() { return collapseDelayMillis; }
    public int getAnimationDurationMillis() { return animationDurationMillis; }

    void setPopupActive(boolean value) {
        popupActive = value;
        if (value) {
            collapseDelayTimer.stop();
            if (autoHideEnabled && available) expand();
        } else if (autoHideEnabled && available) {
            scheduleCollapse();
        }
    }
}
