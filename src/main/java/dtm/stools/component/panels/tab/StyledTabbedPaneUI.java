package dtm.stools.component.panels.tab;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;

public class StyledTabbedPaneUI extends BasicTabbedPaneUI {

    static final String SCROLL_BUTTON_PROPERTY = "TabbedPanel.scrollButton";
    static final String OVERFLOW_MENU_BUTTON_PROPERTY = "TabbedPanel.overflowMenuButton";

    private final TabbedPanel tabs;
    private JViewport overflowViewport;
    private ChangeListener overflowViewportListener;
    private boolean adjustingOverflowViewport;

    public StyledTabbedPaneUI(TabbedPanel tabs) {
        this.tabs = tabs;
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();

        TabStyle style = tabs.getTabStyle();

        tabPane.setOpaque(false);

        tabInsets = safeInsets(style.getTabInsets());
        selectedTabPadInsets = safeInsets(style.getSelectedTabPadInsets());
        tabAreaInsets = resolveTabAreaInsets(style);
        contentBorderInsets = safeInsets(style.getContentBorderInsets());

        highlight = null;
        lightHighlight = null;
        shadow = null;
        darkShadow = null;
        focus = null;
    }

    private Insets resolveTabAreaInsets(TabStyle style) {
        Insets configured = safeInsets(style.getTabAreaInsets());
        if (tabs.getTabOverflowMode() != TabOverflowMode.MENU) {
            return configured;
        }

        int placement = tabPane.getTabPlacement();
        if (placement == JTabbedPane.LEFT || placement == JTabbedPane.RIGHT) {
            return new Insets(configured.top, configured.left, 0, configured.right);
        }
        return new Insets(configured.top, configured.left, configured.bottom, 0);
    }

    @Override
    protected JButton createScrollButton(int direction) {
        if (direction != SwingConstants.NORTH
                && direction != SwingConstants.SOUTH
                && direction != SwingConstants.EAST
                && direction != SwingConstants.WEST) {
            throw new IllegalArgumentException("Invalid tab scroll direction: " + direction);
        }

        if (tabs.getTabOverflowMode() == TabOverflowMode.MENU) {
            return isForwardDirection(direction)
                    ? new TabOverflowMenuButton(direction)
                    : new HiddenTabScrollButton(direction);
        }

        return new TabScrollButton(direction);
    }

    private boolean isForwardDirection(int direction) {
        int placement = tabPane.getTabPlacement();
        boolean vertical = placement == JTabbedPane.LEFT || placement == JTabbedPane.RIGHT;
        return direction == (vertical ? SwingConstants.SOUTH : SwingConstants.EAST);
    }

    @Override
    protected void installListeners() {
        super.installListeners();
        overflowViewport = findOverflowViewport();
        if (overflowViewport != null) {
            overflowViewportListener = event -> normalizeMenuOverflowViewport();
            overflowViewport.addChangeListener(overflowViewportListener);
        }
    }

    @Override
    protected void uninstallListeners() {
        if (overflowViewport != null && overflowViewportListener != null) {
            overflowViewport.removeChangeListener(overflowViewportListener);
        }
        overflowViewport = null;
        overflowViewportListener = null;
        super.uninstallListeners();
    }

    private JViewport findOverflowViewport() {
        for (Component component : tabPane.getComponents()) {
            if (component instanceof JViewport candidate) {
                return candidate;
            }
        }
        return null;
    }

    private void normalizeMenuOverflowViewport() {
        if (adjustingOverflowViewport
                || tabs.getTabOverflowMode() != TabOverflowMode.MENU
                || overflowViewport == null) {
            return;
        }

        JButton menuButton = null;
        for (Component component : tabPane.getComponents()) {
            if (component instanceof JButton button
                    && Boolean.TRUE.equals(button.getClientProperty(OVERFLOW_MENU_BUTTON_PROPERTY))) {
                menuButton = button;
                break;
            }
        }
        if (menuButton == null || !menuButton.isVisible()) {
            return;
        }

        adjustingOverflowViewport = true;
        try {
            int placement = tabPane.getTabPlacement();
            boolean vertical = placement == JTabbedPane.LEFT || placement == JTabbedPane.RIGHT;
            Rectangle viewportBounds = overflowViewport.getBounds();
            alignOverflowMenuButton(menuButton, viewportBounds, vertical);
            Rectangle menuBounds = menuButton.getBounds();
            Dimension viewSize = overflowViewport.getViewSize();
            Point viewPosition = overflowViewport.getViewPosition();

            if (vertical) {
                int targetHeight = menuBounds.y - viewportBounds.y;
                if (targetHeight <= 0) {
                    return;
                }
                overflowViewport.setBounds(
                        viewportBounds.x,
                        viewportBounds.y,
                        viewportBounds.width,
                        targetHeight
                );
                int maxY = Math.max(0, viewSize.height - targetHeight);
                overflowViewport.setViewPosition(new Point(viewPosition.x, Math.min(viewPosition.y, maxY)));
                return;
            }

            int targetWidth = menuBounds.x - viewportBounds.x;
            if (targetWidth <= 0) {
                return;
            }
            overflowViewport.setBounds(
                    viewportBounds.x,
                    viewportBounds.y,
                    targetWidth,
                    viewportBounds.height
            );
            int maxX = Math.max(0, viewSize.width - targetWidth);
            overflowViewport.setViewPosition(new Point(Math.min(viewPosition.x, maxX), viewPosition.y));
        } finally {
            adjustingOverflowViewport = false;
        }
    }

    private void alignOverflowMenuButton(
            JButton menuButton,
            Rectangle viewportBounds,
            boolean vertical
    ) {
        Rectangle tabBounds = findVisibleTabBounds(viewportBounds);

        if (vertical) {
            int tabX = tabBounds == null ? viewportBounds.x : tabBounds.x;
            int tabWidth = tabBounds == null
                    ? (maxTabWidth > 0 ? maxTabWidth : viewportBounds.width)
                    : tabBounds.width;
            int x = tabX + Math.max(0, (tabWidth - menuButton.getWidth()) / 2);
            if (menuButton.getX() != x) {
                menuButton.setLocation(x, menuButton.getY());
            }
            return;
        }

        int tabY = tabBounds == null ? viewportBounds.y : tabBounds.y;
        int tabHeight = tabBounds == null
                ? (maxTabHeight > 0 ? maxTabHeight : viewportBounds.height)
                : tabBounds.height;
        int y = tabY + Math.max(0, (tabHeight - menuButton.getHeight()) / 2);
        if (menuButton.getY() != y) {
            menuButton.setLocation(menuButton.getX(), y);
        }
    }

    private Rectangle findVisibleTabBounds(Rectangle viewportBounds) {
        int selectedIndex = tabPane.getSelectedIndex();
        Rectangle selectedBounds = getVisibleTabBounds(selectedIndex, viewportBounds);
        if (selectedBounds != null) {
            return selectedBounds;
        }

        for (int index = 0; index < tabPane.getTabCount(); index++) {
            Rectangle bounds = getVisibleTabBounds(index, viewportBounds);
            if (bounds != null) {
                return bounds;
            }
        }
        return null;
    }

    private Rectangle getVisibleTabBounds(int index, Rectangle viewportBounds) {
        if (index < 0 || index >= tabPane.getTabCount()) {
            return null;
        }

        Rectangle bounds = tabPane.getBoundsAt(index);
        return bounds != null
                && bounds.width > 0
                && bounds.height > 0
                && bounds.intersects(viewportBounds)
                ? bounds
                : null;
    }

    @Override
    protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
        TabStyle style = tabs.getTabStyle();

        return Math.max(
                style.getMinTabHeight(),
                super.calculateTabHeight(tabPlacement, tabIndex, fontHeight)
        );
    }

    @Override
    protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
        int width = super.calculateTabWidth(tabPlacement, tabIndex, metrics);

        int extra = getMaxHeaderBorderExtra();

        return width + extra * 2;
    }

    @Override
    protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
        int height = super.calculateTabAreaHeight(tabPlacement, horizRunCount, maxTabHeight);

        TabStyle style = tabs.getTabStyle();

        if (tabPlacement == JTabbedPane.TOP || tabPlacement == JTabbedPane.BOTTOM) {
            if (style.isPaintTabAreaBottomLine() && style.getTabHeaderBottomLineSeparatorColor() != null) {
                height += Math.max(0, style.getTabAreaBottomLineGap());
            }
        }

        return height;
    }

    @Override
    protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        Graphics2D g2 = (Graphics2D) g.create();

        try {
            paintConfiguredTabArea(g2, tabPlacement);

            int tabCount = tabPane.getTabCount();

            Rectangle localIconRect = new Rectangle();
            Rectangle localTextRect = new Rectangle();

            for (int i = 0; i < tabCount; i++) {
                if (i == selectedIndex) {
                    continue;
                }

                if (!isValidTabRect(i)) {
                    continue;
                }

                paintTab(
                        g2,
                        tabPlacement,
                        rects,
                        i,
                        localIconRect,
                        localTextRect
                );
            }

            if (selectedIndex >= 0 && selectedIndex < tabCount && isValidTabRect(selectedIndex)) {
                paintTab(
                        g2,
                        tabPlacement,
                        rects,
                        selectedIndex,
                        localIconRect,
                        localTextRect
                );
            }

        } finally {
            g2.dispose();
        }
    }

    private void paintConfiguredTabArea(Graphics2D g2, int tabPlacement) {
        TabStyle style = tabs.getTabStyle();

        Rectangle area = getRealTabAreaBounds(tabPlacement);

        if (style.isPaintTabAreaBackground()) {
            Color background = style.getTabAreaBackground();

            if (background != null) {
                g2.setColor(background);
                g2.fillRect(area.x, area.y, area.width, area.height);
            }
        }

        paintTabAreaLines(g2, area, style);
    }

    private void paintTabAreaLines(Graphics2D g2, Rectangle area, TabStyle style) {
        if (area == null || area.width <= 0 || area.height <= 0) {
            return;
        }

        if (style.isPaintTabAreaTopLine() && style.getTabAreaTopLineColor() != null) {
            g2.setColor(style.getTabAreaTopLineColor());
            g2.drawLine(0, area.y, tabPane.getWidth() - 1, area.y);
        }

        if (style.isPaintTabAreaBottomLine() && style.getTabHeaderBottomLineSeparatorColor() != null) {
            g2.setColor(style.getTabHeaderBottomLineSeparatorColor());

            int y = area.y + area.height - 1;

            g2.drawLine(0, y, tabPane.getWidth() - 1, y);
        }

        if (style.isPaintTabAreaLeftLine() && style.getTabAreaLeftLineColor() != null) {
            g2.setColor(style.getTabAreaLeftLineColor());
            g2.drawLine(area.x, area.y, area.x, area.y + area.height - 1);
        }

        if (style.isPaintTabAreaRightLine() && style.getTabAreaRightLineColor() != null) {
            g2.setColor(style.getTabAreaRightLineColor());

            int x = area.x + area.width - 1;

            g2.drawLine(x, area.y, x, area.y + area.height - 1);
        }
    }

    @Override
    protected void paintTabBackground(
            Graphics g,
            int tabPlacement,
            int tabIndex,
            int x,
            int y,
            int w,
            int h,
            boolean selected
    ) {
        TabStyle style = tabs.getTabStyle();

        if (style.isPaintOnlySelectedTabBackground() && !selected) {
            return;
        }

        Color background = selected
                ? style.getSelectedTabHeaderBackground()
                : style.getTabHeaderBackground();

        if (background == null) {
            return;
        }

        Insets insets = safeInsets(style.getTabHeaderBackgroundInsets());

        float borderWidth = selected
                ? Math.max(1f, style.getSelectedTabHeaderBorderWidth())
                : Math.max(1f, style.getTabHeaderBorderWidth());

        float half = borderWidth / 2f;

        float rx = x + insets.left + half;
        float ry = y + insets.top + half;
        float rw = w - insets.left - insets.right - borderWidth;
        float rh = h - insets.top - insets.bottom - borderWidth;

        if (rw <= 0 || rh <= 0) {
            return;
        }

        int arc = Math.max(0, style.getTabHeaderArc());

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(background);

            if (arc <= 0) {
                g2.fill(new java.awt.geom.Rectangle2D.Float(rx, ry, rw, rh));
            } else {
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(
                        rx,
                        ry,
                        rw,
                        rh,
                        arc,
                        arc
                ));
            }

        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintTabBorder(
            Graphics g,
            int tabPlacement,
            int tabIndex,
            int x,
            int y,
            int w,
            int h,
            boolean selected
    ) {
        TabStyle style = tabs.getTabStyle();

        if (style.isPaintOnlySelectedTabBorder() && !selected) {
            return;
        }

        Color border = selected
                ? style.getSelectedTabHeaderBorderColor()
                : style.getTabHeaderBorderColor();

        Color selectedBottomBorder = selected
                ? style.getTabAreaBottomLineColor()
                : null;

        if (border == null && selectedBottomBorder == null) {
            return;
        }

        Insets insets = safeInsets(style.getTabHeaderBackgroundInsets());

        float borderWidth = selected
                ? style.getSelectedTabHeaderBorderWidth()
                : style.getTabHeaderBorderWidth();

        float half = borderWidth / 2f;

        float rx = x + insets.left + half;
        float ry = y + insets.top + half;
        float rw = w - insets.left - insets.right - borderWidth;
        float rh = h - insets.top - insets.bottom - borderWidth;

        if (rw <= 0 || rh <= 0) {
            return;
        }

        int arc = Math.max(0, style.getTabHeaderArc());

        Graphics2D g2 = (Graphics2D) g.create();

        try {
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            Stroke oldStroke = g2.getStroke();

            g2.setStroke(new BasicStroke(
                    borderWidth,
                    BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND
            ));

            if (border != null) {
                g2.setColor(border);

                if (arc <= 0) {
                    g2.draw(new Rectangle.Float(rx, ry, rw, rh));
                } else {
                    g2.draw(new java.awt.geom.RoundRectangle2D.Float(
                            rx,
                            ry,
                            rw,
                            rh,
                            arc,
                            arc
                    ));
                }
            }

            if (selected && selectedBottomBorder != null) {
                g2.setColor(selectedBottomBorder);

                float y1 = ry + rh;
                float margin = arc <= 0 ? 0f : Math.max(1f, arc / 2f);

                if (rw - margin * 2f > 0) {
                    g2.draw(new java.awt.geom.Line2D.Float(
                            rx + margin,
                            y1,
                            rx + rw - margin,
                            y1
                    ));
                }
            }

            g2.setStroke(oldStroke);

        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        if (tabs.getTabStyle().isPaintContentBorder()) {
            super.paintContentBorder(g, tabPlacement, selectedIndex);
        }
    }

    @Override
    protected void paintFocusIndicator(
            Graphics g,
            int tabPlacement,
            Rectangle[] rects,
            int tabIndex,
            Rectangle iconRect,
            Rectangle textRect,
            boolean selected
    ) {
        if (tabs.getTabStyle().isPaintFocusIndicator()) {
            super.paintFocusIndicator(
                    g,
                    tabPlacement,
                    rects,
                    tabIndex,
                    iconRect,
                    textRect,
                    selected
            );
        }
    }

    private Rectangle getRealTabAreaBounds(int tabPlacement) {
        int width = tabPane.getWidth();
        int height = tabPane.getHeight();

        int areaHeight = calculateSafeTabAreaHeight(tabPlacement);
        int areaWidth = calculateSafeTabAreaWidth();

        if (tabPlacement == JTabbedPane.TOP) {
            return new Rectangle(0, 0, width, areaHeight);
        }

        if (tabPlacement == JTabbedPane.BOTTOM) {
            return new Rectangle(0, height - areaHeight, width, areaHeight);
        }

        if (tabPlacement == JTabbedPane.LEFT) {
            return new Rectangle(0, 0, areaWidth, height);
        }

        if (tabPlacement == JTabbedPane.RIGHT) {
            return new Rectangle(width - areaWidth, 0, areaWidth, height);
        }

        return new Rectangle(0, 0, width, areaHeight);
    }

    private int calculateSafeTabAreaHeight(int tabPlacement) {
        int height = 0;

        try {
            height = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
        } catch (Exception ignored) {
        }

        if (height <= 0) {
            height = maxTabHeight;
        }

        if (height <= 0 && rects != null) {
            for (Rectangle rect : rects) {
                if (rect != null) {
                    height = Math.max(height, rect.y + rect.height);
                }
            }
        }

        if (height <= 0) {
            height = tabs.getTabStyle().getMinTabHeight();
        }

        if (height <= 0) {
            height = 30;
        }

        return height;
    }

    private int calculateSafeTabAreaWidth() {
        int width = 0;

        if (rects != null) {
            for (Rectangle rect : rects) {
                if (rect != null) {
                    width = Math.max(width, rect.x + rect.width);
                }
            }
        }

        if (width <= 0) {
            width = maxTabWidth;
        }

        if (width <= 0) {
            width = 160;
        }

        return width;
    }

    private Insets safeInsets(Insets insets) {
        return insets == null
                ? new Insets(0, 0, 0, 0)
                : insets;
    }

    private boolean isValidTabRect(int index) {
        return rects != null
                && index >= 0
                && index < rects.length
                && rects[index] != null
                && rects[index].width > 0
                && rects[index].height > 0;
    }

    private int getMaxHeaderBorderExtra() {
        TabStyle style = tabs.getTabStyle();

        float normal = Math.max(1f, style.getTabHeaderBorderWidth());
        float selected = Math.max(1f, style.getSelectedTabHeaderBorderWidth());

        int max = (int) Math.ceil(Math.max(normal, selected));

        return Math.max(0, max - 1);
    }

    private class TabScrollButton extends JButton implements UIResource {

        private final int direction;

        private TabScrollButton(int direction) {
            this.direction = direction;

            setBorder(BorderFactory.createEmptyBorder());
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            putClientProperty(SCROLL_BUTTON_PROPERTY, direction);

            String description = switch (direction) {
                case SwingConstants.WEST -> "Mostrar abas anteriores";
                case SwingConstants.EAST -> "Mostrar próximas abas";
                case SwingConstants.NORTH -> "Mostrar abas acima";
                case SwingConstants.SOUTH -> "Mostrar abas abaixo";
                default -> "Navegar pelas abas";
            };
            setToolTipText(description);
            getAccessibleContext().setAccessibleName(description);
        }

        @Override
        public Dimension getPreferredSize() {
            int size = Math.max(18, tabs.getTabStyle().getTabScrollButtonSize());
            return new Dimension(size, size);
        }

        @Override
        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                paintButtonBackground(g2);
                paintChevron(g2);
            } finally {
                g2.dispose();
            }
        }

        protected void paintButtonBackground(Graphics2D g2) {
            ButtonModel model = getModel();
            TabStyle style = tabs.getTabStyle();
            Color background = null;

            if (model.isPressed() && model.isArmed()) {
                background = style.getTabScrollButtonPressedBackground();
            } else if (model.isRollover()) {
                background = style.getTabScrollButtonHoverBackground();
            }

            if (background == null) {
                return;
            }

            int margin = 3;
            int width = getWidth() - margin * 2;
            int height = getHeight() - margin * 2;
            if (width <= 0 || height <= 0) {
                return;
            }

            int arc = Math.max(0, style.getTabScrollButtonArc());
            g2.setColor(background);
            g2.fillRoundRect(margin, margin, width, height, arc, arc);
        }

        private void paintChevron(Graphics2D g2) {
            TabStyle style = tabs.getTabStyle();
            Color foreground = isEnabled()
                    ? style.getTabScrollButtonForeground()
                    : style.getTabScrollButtonDisabledForeground();
            if (foreground == null) {
                return;
            }

            float centerX = getWidth() / 2f;
            float centerY = getHeight() / 2f;
            float radius = Math.max(3f, Math.min(getWidth(), getHeight()) * 0.16f);
            Path2D.Float chevron = new Path2D.Float();

            switch (direction) {
                case SwingConstants.WEST -> {
                    chevron.moveTo(centerX + radius * 0.45f, centerY - radius);
                    chevron.lineTo(centerX - radius * 0.45f, centerY);
                    chevron.lineTo(centerX + radius * 0.45f, centerY + radius);
                }
                case SwingConstants.EAST -> {
                    chevron.moveTo(centerX - radius * 0.45f, centerY - radius);
                    chevron.lineTo(centerX + radius * 0.45f, centerY);
                    chevron.lineTo(centerX - radius * 0.45f, centerY + radius);
                }
                case SwingConstants.NORTH -> {
                    chevron.moveTo(centerX - radius, centerY + radius * 0.45f);
                    chevron.lineTo(centerX, centerY - radius * 0.45f);
                    chevron.lineTo(centerX + radius, centerY + radius * 0.45f);
                }
                case SwingConstants.SOUTH -> {
                    chevron.moveTo(centerX - radius, centerY - radius * 0.45f);
                    chevron.lineTo(centerX, centerY + radius * 0.45f);
                    chevron.lineTo(centerX + radius, centerY - radius * 0.45f);
                }
                default -> {
                    return;
                }
            }

            float strokeWidth = Math.max(1f, style.getTabScrollButtonStrokeWidth());
            g2.setColor(foreground);
            g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(chevron);
        }
    }

    private final class TabOverflowMenuButton extends TabScrollButton {

        private TabOverflowMenuButton(int direction) {
            super(direction);
            putClientProperty(OVERFLOW_MENU_BUTTON_PROPERTY, Boolean.TRUE);
            setToolTipText("Mostrar abas ocultas");
            getAccessibleContext().setAccessibleName("Mostrar abas ocultas");
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferred = super.getPreferredSize();
            int placement = tabPane.getTabPlacement();
            if (placement == JTabbedPane.TOP || placement == JTabbedPane.BOTTOM) {
                preferred.height = Math.max(preferred.height, tabs.getTabStyle().getMinTabHeight());
            }
            return preferred;
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(tabPane == null || tabPane.isEnabled());
        }

        @Override
        protected void paintButtonBackground(Graphics2D g2) {
            ButtonModel model = getModel();
            TabStyle style = tabs.getTabStyle();
            Color background = null;
            if (model.isPressed() && model.isArmed()) {
                background = style.getTabScrollButtonPressedBackground();
            } else if (model.isRollover()) {
                background = style.getTabScrollButtonHoverBackground();
            }
            if (background == null) {
                return;
            }

            Insets insets = safeInsets(style.getTabHeaderBackgroundInsets());
            float borderWidth = Math.max(1f, style.getSelectedTabHeaderBorderWidth());
            float half = borderWidth / 2f;
            float x = insets.left + half;
            float y = insets.top + half;
            float width = getWidth() - insets.left - insets.right - borderWidth;
            float height = getHeight() - insets.top - insets.bottom - borderWidth;
            if (width <= 0 || height <= 0) {
                return;
            }

            int arc = Math.max(0, style.getTabHeaderArc());
            g2.setColor(background);
            if (arc == 0) {
                g2.fill(new Rectangle.Float(x, y, width, height));
            } else {
                g2.fill(new java.awt.geom.RoundRectangle2D.Float(
                        x,
                        y,
                        width,
                        height,
                        arc,
                        arc
                ));
            }
        }

        @Override
        protected void fireActionPerformed(ActionEvent event) {
            tabs.showTabOverflowMenu(this);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                paintButtonBackground(g2);
                paintDots(g2);
            } finally {
                g2.dispose();
            }
        }

        private void paintDots(Graphics2D g2) {
            TabStyle style = tabs.getTabStyle();
            Color foreground = isEnabled()
                    ? style.getTabScrollButtonForeground()
                    : style.getTabScrollButtonDisabledForeground();
            if (foreground == null) {
                return;
            }

            float diameter = Math.max(2.2f, Math.min(getWidth(), getHeight()) * 0.09f);
            float gap = diameter * 1.8f;
            float x = (getWidth() - diameter) / 2f;
            float centerY = getHeight() / 2f;
            g2.setColor(foreground);
            for (int offset = -1; offset <= 1; offset++) {
                float y = centerY + offset * gap - diameter / 2f;
                g2.fill(new Ellipse2D.Float(x, y, diameter, diameter));
            }
        }
    }

    private static final class HiddenTabScrollButton extends JButton implements UIResource {

        private HiddenTabScrollButton(int direction) {
            setFocusable(false);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            putClientProperty(SCROLL_BUTTON_PROPERTY, direction);
            super.setVisible(false);
        }

        @Override
        public void setVisible(boolean visible) {
            super.setVisible(false);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(0, 0);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, 0);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(0, 0);
        }
    }

}
