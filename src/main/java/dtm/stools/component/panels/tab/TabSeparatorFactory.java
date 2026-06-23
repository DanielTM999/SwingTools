package dtm.stools.component.panels.tab;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.*;

@FunctionalInterface
public interface TabSeparatorFactory {
    JSplitPane createSeparator(TabbedPanel source, TabbedPanel target, TabSplitPlacement placement, int orientation);

    static TabSeparatorFactory defaultSeparator() {
        return (source, target, placement, orientation) -> createBaseSeparator(orientation, 10, true);
    }

    static TabSeparatorFactory colored(Color dividerColor) {
        return colored(dividerColor, dividerColor == null ? null : dividerColor.brighter(), 12, true);
    }

    static TabSeparatorFactory colored(Color dividerColor, Color lineColor) {
        return colored(dividerColor, lineColor, 12, true);
    }

    static TabSeparatorFactory colored(Color dividerColor, Color lineColor, int dividerSize, boolean oneTouchExpandable) {
        return colored(dividerColor, lineColor, dividerSize, oneTouchExpandable, true);
    }

    static TabSeparatorFactory solid(Color dividerColor) {
        return solid(dividerColor, 12, true);
    }

    static TabSeparatorFactory solid(Color dividerColor, int dividerSize, boolean oneTouchExpandable) {
        return colored(dividerColor, null, dividerSize, oneTouchExpandable, false);
    }

    static TabSeparatorFactory colored(Color dividerColor, Color lineColor, int dividerSize, boolean oneTouchExpandable, boolean lineVisible) {
        return (source, target, placement, orientation) -> {
            JSplitPane split = createBaseSeparator(orientation, dividerSize, oneTouchExpandable);
            split.setUI(new ColoredSplitPaneUI(
                    dividerColor == null ? new Color(0x2563EB) : dividerColor,
                    lineColor == null ? new Color(0x93C5FD) : lineColor,
                    lineVisible
            ));
            return split;
        };
    }

    private static JSplitPane createBaseSeparator(int orientation, int dividerSize, boolean oneTouchExpandable) {
        JSplitPane split = new JSplitPane(orientation);
        split.setContinuousLayout(true);
        split.setResizeWeight(0.5);
        split.setDividerSize(Math.max(1, dividerSize));
        split.setOneTouchExpandable(oneTouchExpandable);
        split.setBorder(BorderFactory.createEmptyBorder());
        return split;
    }

    class ColoredSplitPaneUI extends BasicSplitPaneUI {
        private final Color dividerColor;
        private final Color lineColor;
        private final boolean lineVisible;

        private ColoredSplitPaneUI(Color dividerColor, Color lineColor, boolean lineVisible) {
            this.dividerColor = dividerColor;
            this.lineColor = lineColor;
            this.lineVisible = lineVisible;
        }

        @Override
        public BasicSplitPaneDivider createDefaultDivider() {
            BasicSplitPaneDivider divider = new BasicSplitPaneDivider(this) {
                @Override
                public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setColor(dividerColor);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    if (lineVisible) {
                        g2.setColor(lineColor);
                        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
                            int x = getWidth() / 2;
                            g2.drawLine(x, 3, x, getHeight() - 4);
                        } else {
                            int y = getHeight() / 2;
                            g2.drawLine(3, y, getWidth() - 4, y);
                        }
                    }
                    g2.dispose();
                    super.paint(g);
                }
            };
            divider.setBackground(dividerColor);
            return divider;
        }
    }
}
