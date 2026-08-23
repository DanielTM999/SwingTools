package dtm.stools.component.feedback.badge;

import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.ColorUtils;
import dtm.stools.utils.PaintUtils;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * Etiqueta compacta em formato de pílula usada para indicar status.
 */
public class BadgeLabel extends PanelEventListener {

    /**
     * Cor semântica da etiqueta.
     */
    public enum Tone {
        NEUTRAL, PRIMARY, SUCCESS, WARNING, DANGER, INFO
    }

    /**
     * Estilo de preenchimento da etiqueta.
     */
    public enum Style {
        SOFT, SOLID, OUTLINE
    }

    /**
     * Tamanho da etiqueta.
     */
    public enum Size {
        SM, MD
    }

    private String text = "";
    private Tone tone = Tone.NEUTRAL;
    private Style style = Style.SOFT;
    private Size size = Size.MD;
    private boolean showDot;

    private Color customColor;

    public BadgeLabel() {
        this("", Tone.NEUTRAL);
    }

    public BadgeLabel(String text) {
        this(text, Tone.NEUTRAL);
    }

    public BadgeLabel(String text, Tone tone) {
        super(null, false);
        this.text = text != null ? text : "";
        this.tone = tone != null ? tone : Tone.NEUTRAL;

        setOpaque(false);
        setFont(UiTokens.fontSmall().deriveFont(Font.BOLD));
        updatePreferredSize();
    }

    /**
     * Texto exibido na etiqueta.
     */
    public String getText() {
        return text;
    }

    /**
     * Define o texto exibido na etiqueta.
     */
    public BadgeLabel setText(String text) {
        this.text = text != null ? text : "";
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define a cor semântica da etiqueta.
     */
    public BadgeLabel setTone(Tone tone) {
        if (tone == null) {
            throw new IllegalArgumentException("tone cannot be null");
        }
        this.tone = tone;
        repaint();
        return this;
    }

    /**
     * Cor semântica corrente.
     */
    public Tone getTone() {
        return tone;
    }

    /**
     * Define o estilo de preenchimento.
     */
    public BadgeLabel setStyle(Style style) {
        if (style == null) {
            throw new IllegalArgumentException("style cannot be null");
        }
        this.style = style;
        repaint();
        return this;
    }

    /**
     * Define o tamanho da etiqueta.
     */
    public BadgeLabel setSize(Size size) {
        if (size == null) {
            throw new IllegalArgumentException("size cannot be null");
        }
        this.size = size;
        setFont(UiTokens.fontSmall().deriveFont(Font.BOLD,
                size == Size.SM ? UiTokens.fontSmall().getSize2D() - 1f : UiTokens.fontSmall().getSize2D()));
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Exibe um ponto colorido antes do texto.
     */
    public BadgeLabel setShowDot(boolean showDot) {
        this.showDot = showDot;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define uma cor customizada, ignorando o tom semântico.
     */
    public BadgeLabel setCustomColor(Color customColor) {
        this.customColor = customColor;
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
        int horizontalPadding = size == Size.SM ? UiTokens.space(2) : UiTokens.space(3);
        int verticalPadding = size == Size.SM ? UiTokens.space(1) : UiTokens.space(1) + 2;

        int width = metrics.stringWidth(text) + horizontalPadding * 2;
        if (showDot) {
            width += dotSize() + UiTokens.space(1);
        }
        int height = metrics.getHeight() + verticalPadding;

        Dimension preferred = new Dimension(width, height);
        setPreferredSize(preferred);
        setMinimumSize(preferred);
        setMaximumSize(preferred);
        revalidate();
    }

    private int dotSize() {
        return size == Size.SM ? UiTokens.scale(5) : UiTokens.scale(6);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
            paintSurface(g2, bounds);
            paintContent(g2, bounds);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Pinta o fundo em formato de pílula.
     */
    protected void paintSurface(Graphics2D g2, Rectangle bounds) {
        Color accent = resolveAccent();
        int arc = bounds.height;

        switch (style) {
            case SOLID -> PaintUtils.fillRoundRect(g2, bounds, arc, accent);
            case SOFT -> PaintUtils.fillRoundRect(g2, bounds, arc, ColorUtils.withAlpha(accent, 0.16f));
            case OUTLINE -> PaintUtils.drawRoundRect(g2, bounds, arc, accent, UiTokens.stroke());
        }
    }

    /**
     * Pinta o ponto indicador e o texto.
     */
    protected void paintContent(Graphics2D g2, Rectangle bounds) {
        g2.setFont(getFont() != null ? getFont() : UiTokens.fontSmall());
        Color accent = resolveAccent();
        Color foreground = style == Style.SOLID ? UiTokens.onColor(accent) : accent;
        if (!isEnabled()) {
            foreground = UiTokens.disabled(foreground);
        }

        int horizontalPadding = size == Size.SM ? UiTokens.space(2) : UiTokens.space(3);
        int x = horizontalPadding;

        if (showDot) {
            int dot = dotSize();
            g2.setColor(foreground);
            g2.fillOval(x, (bounds.height - dot) / 2, dot, dot);
            x += dot + UiTokens.space(1);
        }

        Rectangle textBounds = new Rectangle(x, 0, Math.max(0, bounds.width - x - horizontalPadding), bounds.height);
        PaintUtils.drawLeftText(g2, text, textBounds, foreground);
    }

    private Color resolveAccent() {
        if (customColor != null) {
            return customColor;
        }
        return switch (tone) {
            case PRIMARY -> UiTokens.primary();
            case SUCCESS -> UiTokens.success();
            case WARNING -> UiTokens.warning();
            case DANGER -> UiTokens.danger();
            case INFO -> UiTokens.info();
            case NEUTRAL -> UiTokens.muted();
        };
    }
}
