package dtm.stools.component.feedback.avatar;

import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.ColorUtils;
import dtm.stools.utils.PaintUtils;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.util.Locale;

/**
 * Avatar circular ou arredondado exibindo imagem, ícone ou iniciais, com indicador de presença opcional.
 */
public class AvatarLabel extends PanelEventListener {

    /**
     * Formato da moldura do avatar.
     */
    public enum Shape2D {
        CIRCLE, ROUNDED
    }

    /**
     * Estado de presença exibido no canto do avatar.
     */
    public enum Presence {
        NONE, ONLINE, BUSY, AWAY, OFFLINE
    }

    private String name = "";
    private Image image;
    private Icon icon;
    private Shape2D shape = Shape2D.CIRCLE;
    private Presence presence = Presence.NONE;

    private int size = 40;
    private int arc = UiTokens.radius(UiTokens.Radius.MD);
    private int ringWidth;

    private Color backgroundColor;
    private Color textColor;
    private Color ringColor;

    public AvatarLabel() {
        this("");
    }

    public AvatarLabel(String name) {
        super(null, false);
        this.name = name != null ? name : "";

        setOpaque(false);
        setFont(UiTokens.font().deriveFont(Font.BOLD));
        updatePreferredSize();
    }

    /**
     * Nome usado para gerar as iniciais e a cor de fundo.
     */
    public String getDisplayName() {
        return name;
    }

    /**
     * Define o nome exibido como iniciais.
     */
    public AvatarLabel setDisplayName(String name) {
        this.name = name != null ? name : "";
        repaint();
        return this;
    }

    /**
     * Define a imagem exibida no avatar.
     */
    public AvatarLabel setImage(Image image) {
        this.image = image;
        repaint();
        return this;
    }

    /**
     * Define um ícone exibido no lugar das iniciais.
     */
    public AvatarLabel setIcon(Icon icon) {
        this.icon = icon;
        repaint();
        return this;
    }

    /**
     * Define o formato da moldura.
     */
    public AvatarLabel setShape(Shape2D shape) {
        if (shape == null) {
            throw new IllegalArgumentException("shape cannot be null");
        }
        this.shape = shape;
        repaint();
        return this;
    }

    /**
     * Define o estado de presença exibido.
     */
    public AvatarLabel setPresence(Presence presence) {
        this.presence = presence != null ? presence : Presence.NONE;
        repaint();
        return this;
    }

    /**
     * Define o tamanho do avatar.
     */
    public AvatarLabel setSize(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("size must be greater than zero");
        }
        this.size = size;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define a espessura do anel externo.
     */
    public AvatarLabel setRingWidth(int ringWidth) {
        if (ringWidth < 0) {
            throw new IllegalArgumentException("ringWidth cannot be negative");
        }
        this.ringWidth = ringWidth;
        updatePreferredSize();
        repaint();
        return this;
    }

    /**
     * Define as cores de fundo, de texto e do anel.
     */
    public AvatarLabel setColors(Color backgroundColor, Color textColor, Color ringColor) {
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.ringColor = ringColor;
        repaint();
        return this;
    }

    /**
     * Iniciais derivadas do nome informado.
     */
    public String getInitials() {
        if (name.isBlank()) {
            return "";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase(Locale.ROOT);
        }
        return (parts[0].charAt(0) + String.valueOf(parts[parts.length - 1].charAt(0))).toUpperCase(Locale.ROOT);
    }

    private void updatePreferredSize() {
        int total = size + ringWidth * 2;
        Dimension preferred = new Dimension(total, total);
        setPreferredSize(preferred);
        setMinimumSize(preferred);
        setMaximumSize(preferred);
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle bounds = getAvatarBounds();
            Shape clip = buildShape(bounds);

            if (ringWidth > 0) {
                paintRing(g2, bounds);
            }
            paintSurface(g2, bounds, clip);
            paintContent(g2, bounds, clip);
            if (presence != Presence.NONE) {
                paintPresence(g2, bounds);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo ocupado pelo avatar, descontando o anel.
     */
    protected Rectangle getAvatarBounds() {
        return new Rectangle(ringWidth, ringWidth, size, size);
    }

    /**
     * Pinta o anel externo.
     */
    protected void paintRing(Graphics2D g2, Rectangle bounds) {
        Color color = ringColor != null ? ringColor : UiTokens.surface();
        g2.setColor(color);
        Rectangle outer = new Rectangle(0, 0, bounds.width + ringWidth * 2, bounds.height + ringWidth * 2);
        g2.fill(buildShape(outer));
    }

    /**
     * Pinta o fundo do avatar.
     */
    protected void paintSurface(Graphics2D g2, Rectangle bounds, Shape clip) {
        Color fill = backgroundColor != null ? backgroundColor : generatedColor();
        g2.setColor(isEnabled() ? fill : UiTokens.disabled(fill));
        g2.fill(clip);
    }

    /**
     * Pinta a imagem, o ícone ou as iniciais.
     */
    protected void paintContent(Graphics2D g2, Rectangle bounds, Shape clip) {
        if (image != null) {
            Shape previousClip = g2.getClip();
            g2.clip(clip);
            g2.drawImage(image, bounds.x, bounds.y, bounds.width, bounds.height, null);
            g2.setClip(previousClip);
            return;
        }

        if (icon != null) {
            icon.paintIcon(this, g2,
                    bounds.x + (bounds.width - icon.getIconWidth()) / 2,
                    bounds.y + (bounds.height - icon.getIconHeight()) / 2);
            return;
        }

        String initials = getInitials();
        if (initials.isEmpty()) {
            return;
        }

        Color fill = backgroundColor != null ? backgroundColor : generatedColor();
        Color color = textColor != null ? textColor : UiTokens.onColor(fill);
        g2.setFont(getFont().deriveFont(Font.BOLD, size * 0.4f));
        PaintUtils.drawCenteredText(g2, initials, bounds, isEnabled() ? color : UiTokens.disabled(color));
    }

    /**
     * Pinta o indicador de presença no canto inferior direito.
     */
    protected void paintPresence(Graphics2D g2, Rectangle bounds) {
        int dot = Math.max(UiTokens.scale(8), size / 4);
        int x = bounds.x + bounds.width - dot;
        int y = bounds.y + bounds.height - dot;

        g2.setColor(UiTokens.surface());
        g2.fillOval(x - 2, y - 2, dot + 4, dot + 4);

        g2.setColor(presenceColor());
        g2.fillOval(x, y, dot, dot);
    }

    private Shape buildShape(Rectangle bounds) {
        return shape == Shape2D.CIRCLE
                ? new Ellipse2D.Float(bounds.x, bounds.y, bounds.width, bounds.height)
                : PaintUtils.roundRect(bounds, arc);
    }

    private Color presenceColor() {
        return switch (presence) {
            case ONLINE -> UiTokens.success();
            case BUSY -> UiTokens.danger();
            case AWAY -> UiTokens.warning();
            case OFFLINE, NONE -> UiTokens.muted();
        };
    }

    private Color generatedColor() {
        if (name.isBlank()) {
            return UiTokens.overlay(UiTokens.muted(), 0.35f);
        }
        float hue = Math.abs(name.hashCode() % 360) / 360f;
        Color base = Color.getHSBColor(hue, 0.5f, UiTokens.isDarkTheme() ? 0.55f : 0.72f);
        return ColorUtils.mix(base, UiTokens.surface(), 0.12f);
    }
}
