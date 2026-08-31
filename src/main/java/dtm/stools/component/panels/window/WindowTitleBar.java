package dtm.stools.component.panels.window;

import dtm.stools.component.icon.FittedIcon;
import dtm.stools.component.menu.bar.MenuBar;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class WindowTitleBar extends JPanel {
    protected final WindowPanel window;
    protected final JLabel iconLabel = new JLabel();
    protected final JLabel titleLabel = new JLabel();
    protected final JPanel leading = new JPanel(new CenteredFlowLayout(FlowLayout.LEFT, 8, 0));
    protected final JPanel center = new JPanel(null) {
        @Override public void doLayout() { layoutCenterComponents(); }
        @Override public boolean isOptimizedDrawingEnabled() { return false; }
    };
    protected final JPanel controls = new JPanel(new CenteredFlowLayout(FlowLayout.RIGHT, 0, 0));
    private Component centerComponent = titleLabel;
    private Component userCenterComponent;
    private JMenuBar menuBar;
    private boolean menuBarWasOpaque;
    private Border menuBarPreviousBorder;
    private Color menuBarPreviousBackground;

    public WindowTitleBar(WindowPanel window) {
        this.window = window;
        setLayout(new BorderLayout(8, 0));
        setOpaque(false);
        leading.setOpaque(false);
        center.setOpaque(false);
        controls.setOpaque(false);
        leading.add(iconLabel);
        center.add(titleLabel, BorderLayout.CENTER);
        add(leading, BorderLayout.WEST);
        add(center, BorderLayout.CENTER);
        add(controls, BorderLayout.EAST);
    }

    public void addControl(Component component) {
        if (component != null) component.setCursor(Cursor.getDefaultCursor());
        controls.add(component);
    }
    public void addLeading(Component component) {
        leading.add(component);
        window.installTitleBarDragSource(component);
    }
    public void setCenterComponent(Component component) {
        centerComponent = component == null ? titleLabel : component;
        rebuildCenter();
        window.installTitleBarDragSource(centerComponent);
    }
    public void setUserCenterComponent(Component component) {
        if (userCenterComponent == component) return;
        userCenterComponent = component;
        rebuildCenter();
        window.installTitleBarDragSource(component);
    }
    public void setMenuBar(JMenuBar value) {
        if (menuBar == value) return;
        restoreMenuBarPresentation();
        menuBar = value;
        applyIntegratedMenuBarPresentation();
        rebuildCenter();
    }
    private void rebuildCenter() {
        center.removeAll();
        center.add(menuBar == null ? centerComponent : menuBar);
        if (userCenterComponent != null) center.add(userCenterComponent, 0);
        revalidate();
        repaint();
    }

    private void layoutCenterComponents() {
        Component base = menuBar == null ? centerComponent : menuBar;
        if (base != null) base.setBounds(0, 0, center.getWidth(), center.getHeight());
        if (userCenterComponent == null) return;
        Dimension preferred = userCenterComponent.getPreferredSize();
        int width = Math.min(preferred.width, center.getWidth());
        int height = Math.min(preferred.height, center.getHeight());
        int titleBarCenterX = (getWidth() - width) / 2;
        int x = Math.max(0, Math.min(center.getWidth() - width, titleBarCenterX - center.getX()));
        int y = Math.max(0, (center.getHeight() - height) / 2);
        userCenterComponent.setBounds(x, y, width, height);
    }

    private void applyIntegratedMenuBarPresentation() {
        if (menuBar == null) return;
        menuBarWasOpaque = menuBar.isOpaque();
        menuBarPreviousBorder = menuBar.getBorder();
        menuBarPreviousBackground = menuBar.getBackground();
        if (menuBar instanceof MenuBar modernMenuBar) {
            modernMenuBar.titleBarEmbedded(true);
        } else {
            menuBar.setOpaque(false);
            menuBar.setBorder(BorderFactory.createEmptyBorder());
        }
        updateIntegratedMenuBarColor();
    }

    private void restoreMenuBarPresentation() {
        if (menuBar == null) return;
        if (menuBar instanceof MenuBar modernMenuBar) {
            modernMenuBar.titleBarEmbedded(false);
        } else {
            menuBar.setOpaque(menuBarWasOpaque);
            menuBar.setBorder(menuBarPreviousBorder);
            menuBar.setBackground(menuBarPreviousBackground);
        }
    }

    private void updateIntegratedMenuBarColor() {
        if (menuBar == null) return;
        Color color = null;
        if (window.isMenuBarFollowTitleBarColor()) {
            WindowStyle style = window.getWindowStyle();
            color = window.isActive() ? style.getActiveTitleBackground() : style.getTitleBackground();
        }
        if (menuBar instanceof MenuBar modernMenuBar) {
            modernMenuBar.titleBarEmbeddedBackground(color);
        } else {
            menuBar.setOpaque(color != null);
            if (color != null) menuBar.setBackground(color);
        }
        menuBar.repaint();
    }

    public JLabel getTitleLabel() { return titleLabel; }
    public JLabel getIconLabel() { return iconLabel; }
    public JPanel getControls() { return controls; }
    public JMenuBar getMenuBar() { return menuBar; }
    public Component getUserCenterComponent() { return userCenterComponent; }

    public void updateFromWindow() {
        WindowStyle style = window.getWindowStyle();
        titleLabel.setText(window.getTitle());
        titleLabel.setIcon(null);
        titleLabel.setFont(style.getTitleFont());
        titleLabel.setForeground(window.isActive() ? style.getForeground() : style.getInactiveForeground());
        iconLabel.setIcon(resolveIcon(style));
        iconLabel.setVisible(window.getIcon() != null);
        updateIntegratedMenuBarColor();
        setPreferredSize(new Dimension(10, style.getTitleBarHeight()));
        applyTitleBarInsets(style);
        repaint();
    }

    /**
     * Applies {@link WindowStyle#getTitleBarInsets()} as the title bar padding and
     * {@link WindowStyle#getTitleBarIconGap()} as the spacing between the leading
     * components. Larger insets keep the icon and the controls off the window edges.
     */
    protected void applyTitleBarInsets(WindowStyle style) {
        Insets insets = style.getTitleBarInsets();
        int left = menuBar != null && window.getIcon() == null ? 0 : insets.left;
        setBorder(BorderFactory.createEmptyBorder(
                insets.top, left, insets.bottom, insets.right));
        int gap = style.getTitleBarIconGap();
        if (leading.getLayout() instanceof FlowLayout layout) layout.setHgap(gap);
        if (getLayout() instanceof BorderLayout layout) {
            layout.setHgap(window.getIcon() == null ? 0 : gap);
        }
        leading.revalidate();
        controls.revalidate();
        revalidate();
    }

    /**
     * {@link FlowLayout} adjusted for title bar rows in two ways:
     * <ul>
     *   <li>the row is centered vertically instead of stacked from the top, which left
     *       short components (the window icon) glued to the top edge;</li>
     *   <li>the horizontal gap applies only <em>between</em> components. The plain layout
     *       also pads before the first and after the last one, so the icon kept an extra
     *       gap of margin that no {@link WindowStyle#getTitleBarInsets()} value could
     *       remove, and an empty row still reserved {@code hgap * 2} of width.</li>
     * </ul>
     */
    protected static class CenteredFlowLayout extends FlowLayout {
        public CenteredFlowLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override
        public void layoutContainer(Container target) {
            super.layoutContainer(target);
            synchronized (target.getTreeLock()) {
                Insets insets = target.getInsets();
                int available = target.getHeight() - insets.top - insets.bottom;
                int shift = getAlignment() == RIGHT ? getHgap() : -getHgap();
                for (Component component : target.getComponents()) {
                    if (!component.isVisible()) continue;
                    int y = insets.top + Math.max(0, (available - component.getHeight()) / 2);
                    component.setLocation(component.getX() + shift, y);
                }
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container target) {
            return withoutOuterGaps(target, super.preferredLayoutSize(target));
        }

        @Override
        public Dimension minimumLayoutSize(Container target) {
            return withoutOuterGaps(target, super.minimumLayoutSize(target));
        }

        private Dimension withoutOuterGaps(Container target, Dimension size) {
            synchronized (target.getTreeLock()) {
                Insets insets = target.getInsets();
                int floor = insets.left + insets.right;
                size.width = Math.max(floor, size.width - getHgap() * 2);
                if (!hasVisibleComponent(target)) size.width = floor;
                return size;
            }
        }

        private static boolean hasVisibleComponent(Container target) {
            for (Component component : target.getComponents()) {
                if (component.isVisible()) return true;
            }
            return false;
        }
    }

    protected Icon resolveIcon(WindowStyle style) {
        return FittedIcon.fit(window.getIcon(), iconSize(style));
    }

    protected int iconSize(WindowStyle style) {
        return Math.max(12, Math.min(20, style.getTitleBarHeight() - 20));
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            WindowStyle style = window.getWindowStyle();
            g.setColor(window.isActive() ? style.getActiveTitleBackground() : style.getTitleBackground());
            int arc = window.getEffectiveWindowArc();
            g.fillRoundRect(0, 0, getWidth(), getHeight() + arc, arc, arc);
        } finally {
            g.dispose();
        }
        super.paintComponent(graphics);
    }
}
