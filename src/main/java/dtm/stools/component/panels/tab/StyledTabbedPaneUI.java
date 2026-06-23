package dtm.stools.component.panels.tab;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class StyledTabbedPaneUI extends BasicTabbedPaneUI {

    private final TabbedPanel tabs;

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
        tabAreaInsets = safeInsets(style.getTabAreaInsets());
        contentBorderInsets = safeInsets(style.getContentBorderInsets());

        highlight = null;
        lightHighlight = null;
        shadow = null;
        darkShadow = null;
        focus = null;
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

}