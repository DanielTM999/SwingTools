package dtm.stools.component.panels.split;

import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Divisor moderno com faixa fina, alça visível no hover e colapso por duplo clique.
 */
public class SplitPanel extends JSplitPane {

    private int dividerThickness = UiTokens.scale(6);
    private boolean collapseOnDoubleClick = true;
    private double restoreLocation = 0.5d;

    private Color dividerColor;
    private Color handleColor;

    public SplitPanel() {
        this(HORIZONTAL_SPLIT, null, null);
    }

    public SplitPanel(int orientation) {
        this(orientation, null, null);
    }

    public SplitPanel(int orientation, JComponent first, JComponent second) {
        super(orientation, true, first, second);
        setBorder(BorderFactory.createEmptyBorder());
        setOpaque(false);
        setContinuousLayout(true);
        setResizeWeight(0.5d);
        applyDividerUi();
    }

    /**
     * Define a espessura do divisor.
     */
    public SplitPanel setDividerThickness(int dividerThickness) {
        if (dividerThickness <= 0) {
            throw new IllegalArgumentException("dividerThickness must be greater than zero");
        }
        this.dividerThickness = dividerThickness;
        setDividerSize(dividerThickness);
        repaint();
        return this;
    }

    /**
     * Habilita o colapso do painel por duplo clique no divisor.
     */
    public SplitPanel setCollapseOnDoubleClick(boolean collapseOnDoubleClick) {
        this.collapseOnDoubleClick = collapseOnDoubleClick;
        return this;
    }

    /**
     * Define as cores da faixa e da alça do divisor.
     */
    public SplitPanel setDividerColors(Color dividerColor, Color handleColor) {
        this.dividerColor = dividerColor;
        this.handleColor = handleColor;
        repaint();
        return this;
    }

    /**
     * Colapsa o primeiro painel, memorizando a posição corrente.
     */
    public SplitPanel collapseFirst() {
        restoreLocation = currentProportion();
        setDividerLocation(0d);
        return this;
    }

    /**
     * Colapsa o segundo painel, memorizando a posição corrente.
     */
    public SplitPanel collapseSecond() {
        restoreLocation = currentProportion();
        setDividerLocation(1d);
        return this;
    }

    /**
     * Restaura a posição memorizada antes do último colapso.
     */
    public SplitPanel restore() {
        setDividerLocation(restoreLocation);
        return this;
    }

    private double currentProportion() {
        int span = getOrientation() == HORIZONTAL_SPLIT ? getWidth() : getHeight();
        return span <= 0 ? 0.5d : Math.max(0.05d, Math.min(0.95d, (double) getDividerLocation() / span));
    }

    private void applyDividerUi() {
        setDividerSize(dividerThickness);
        setUI(new BasicSplitPaneUI() {
            @Override
            public BasicSplitPaneDivider createDefaultDivider() {
                return new ModernDivider(this);
            }
        });
        setBorder(BorderFactory.createEmptyBorder());
    }

    /**
     * Divisor pintado com faixa fina e alça central.
     */
    private final class ModernDivider extends BasicSplitPaneDivider {

        private boolean hover;

        private ModernDivider(BasicSplitPaneUI ui) {
            super(ui);
            setBorder(BorderFactory.createEmptyBorder());
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!collapseOnDoubleClick || e.getClickCount() != 2) {
                        return;
                    }
                    double proportion = currentProportion();
                    if (proportion <= 0.05d || proportion >= 0.95d) {
                        restore();
                    } else {
                        collapseFirst();
                    }
                }
            });
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
            try {
                Color line = dividerColor != null ? dividerColor : UiTokens.border();
                g2.setColor(hover ? UiTokens.primary() : line);
                paintTrack(g2);
                if (hover) {
                    paintHandle(g2);
                }
            } finally {
                g2.dispose();
            }
        }

        private void paintTrack(Graphics2D g2) {
            if (getOrientation() == HORIZONTAL_SPLIT) {
                g2.fillRect(getWidth() / 2, 0, 1, getHeight());
            } else {
                g2.fillRect(0, getHeight() / 2, getWidth(), 1);
            }
        }

        private void paintHandle(Graphics2D g2) {
            Color color = handleColor != null ? handleColor : UiTokens.primary();
            int length = UiTokens.scale(28);
            Rectangle handle = getOrientation() == HORIZONTAL_SPLIT
                    ? new Rectangle(getWidth() / 2 - 1, (getHeight() - length) / 2, 3, length)
                    : new Rectangle((getWidth() - length) / 2, getHeight() / 2 - 1, length, 3);
            PaintUtils.fillRoundRect(g2, handle, 3, color);
        }
    }
}
