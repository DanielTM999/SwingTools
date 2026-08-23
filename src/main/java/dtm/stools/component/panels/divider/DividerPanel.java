package dtm.stools.component.panels.divider;

import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Separador horizontal ou vertical com rótulo centralizado opcional.
 */
public class DividerPanel extends PanelEventListener {

    /**
     * Orientação do separador.
     */
    public enum Orientation {
        HORIZONTAL, VERTICAL
    }

    /**
     * Alinhamento do rótulo sobre a linha.
     */
    public enum LabelAlignment {
        START, CENTER, END
    }

    private Orientation orientation = Orientation.HORIZONTAL;
    private LabelAlignment labelAlignment = LabelAlignment.CENTER;

    private String text = "";
    private int thickness = 1;
    private int labelGap = UiTokens.space(2);
    private int inset;

    private Color lineColor;
    private Color textColor;

    public DividerPanel() {
        this("", Orientation.HORIZONTAL);
    }

    public DividerPanel(String text) {
        this(text, Orientation.HORIZONTAL);
    }

    public DividerPanel(String text, Orientation orientation) {
        super(null, false);
        this.text = text != null ? text : "";
        this.orientation = orientation != null ? orientation : Orientation.HORIZONTAL;

        setOpaque(false);
        setFont(UiTokens.fontSmall());
        updatePreferredSize();
    }

    /**
     * Rótulo exibido sobre a linha.
     */
    public String getText() {
        return text;
    }

    /**
     * Define o rótulo exibido sobre a linha.
     */
    public DividerPanel setText(String text) {
        this.text = text != null ? text : "";
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define a orientação do separador.
     */
    public DividerPanel setOrientation(Orientation orientation) {
        if (orientation == null) {
            throw new IllegalArgumentException("orientation cannot be null");
        }
        this.orientation = orientation;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Orientação corrente.
     */
    public Orientation getOrientation() {
        return orientation;
    }

    /**
     * Define o alinhamento do rótulo.
     */
    public DividerPanel setLabelAlignment(LabelAlignment labelAlignment) {
        if (labelAlignment == null) {
            throw new IllegalArgumentException("labelAlignment cannot be null");
        }
        this.labelAlignment = labelAlignment;
        repaint();
        return this;
    }

    /**
     * Define a espessura da linha.
     */
    public DividerPanel setThickness(int thickness) {
        if (thickness <= 0) {
            throw new IllegalArgumentException("thickness must be greater than zero");
        }
        this.thickness = thickness;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define o recuo aplicado às extremidades da linha.
     */
    public DividerPanel setInset(int inset) {
        if (inset < 0) {
            throw new IllegalArgumentException("inset cannot be negative");
        }
        this.inset = inset;
        repaint();
        return this;
    }

    /**
     * Define as cores da linha e do rótulo.
     */
    public DividerPanel setColors(Color lineColor, Color textColor) {
        this.lineColor = lineColor;
        this.textColor = textColor;
        repaint();
        return this;
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        updatePreferredSize();
    }

    private void updatePreferredSize() {
        if (text == null) {
            return;
        }
        FontMetrics metrics = getFontMetrics(getFont() != null ? getFont() : UiTokens.fontSmall());
        if (orientation == Orientation.HORIZONTAL) {
            int height = text.isEmpty() ? Math.max(thickness, UiTokens.space(2)) : metrics.getHeight();
            setPreferredSize(new Dimension(UiTokens.scale(120), height));
            setMinimumSize(new Dimension(UiTokens.scale(24), height));
        } else {
            int width = Math.max(thickness, UiTokens.space(2));
            setPreferredSize(new Dimension(width, UiTokens.scale(24)));
            setMinimumSize(new Dimension(width, UiTokens.scale(12)));
        }
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            if (orientation == Orientation.VERTICAL) {
                paintVertical(g2);
                return;
            }
            if (text.isEmpty()) {
                paintHorizontal(g2, inset, getWidth() - inset);
                return;
            }
            paintLabelled(g2);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Pinta um trecho horizontal da linha.
     */
    protected void paintHorizontal(Graphics2D g2, int fromX, int toX) {
        if (toX <= fromX) {
            return;
        }
        g2.setColor(resolveLineColor());
        g2.fillRect(fromX, (getHeight() - thickness) / 2, toX - fromX, thickness);
    }

    /**
     * Pinta a linha vertical.
     */
    protected void paintVertical(Graphics2D g2) {
        g2.setColor(resolveLineColor());
        g2.fillRect((getWidth() - thickness) / 2, inset, thickness, Math.max(0, getHeight() - inset * 2));
    }

    /**
     * Pinta a linha com o rótulo posicionado conforme o alinhamento.
     */
    protected void paintLabelled(Graphics2D g2) {
        g2.setFont(getFont() != null ? getFont() : UiTokens.fontSmall());
        FontMetrics metrics = g2.getFontMetrics();
        int textWidth = Math.min(metrics.stringWidth(text), Math.max(0, getWidth() - inset * 2 - labelGap * 2));

        int textX = switch (labelAlignment) {
            case START -> inset + labelGap;
            case END -> getWidth() - inset - labelGap - textWidth;
            case CENTER -> (getWidth() - textWidth) / 2;
        };

        paintHorizontal(g2, inset, textX - labelGap);
        paintHorizontal(g2, textX + textWidth + labelGap, getWidth() - inset);

        Color color = textColor != null ? textColor : UiTokens.muted();
        PaintUtils.drawLeftText(g2, text, new Rectangle(textX, 0, textWidth, getHeight()), color);
    }

    private Color resolveLineColor() {
        Color base = lineColor != null ? lineColor : UiTokens.border();
        return isEnabled() ? base : UiTokens.disabled(base);
    }
}
