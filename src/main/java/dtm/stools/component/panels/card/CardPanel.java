package dtm.stools.component.panels.card;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Superfície arredondada com cabeçalho, corpo e rodapé opcionais, em variantes elevada, contornada ou preenchida.
 */
public class CardPanel extends PanelEventListener {

    /**
     * Aparência da superfície do cartão.
     */
    public enum Variant {
        ELEVATED, OUTLINED, FILLED
    }

    private final JPanel header = new JPanel(new BorderLayout(UiTokens.space(2), 0));
    private final JPanel body = new JPanel(new BorderLayout());
    private final JPanel footer = new JPanel(new BorderLayout(UiTokens.space(2), 0));

    private final JLabel titleLabel = new JLabel();
    private final JLabel subtitleLabel = new JLabel();

    private Variant variant = Variant.ELEVATED;
    private int arc = UiTokens.radius(UiTokens.Radius.LG);
    private int shadowSpread = 6;
    private boolean clickable;
    private boolean hover;
    private boolean pressed;

    private Color backgroundColor;
    private Color borderColor;
    private Insets padding = new Insets(UiTokens.space(4), UiTokens.space(4), UiTokens.space(4), UiTokens.space(4));

    public CardPanel() {
        this(null, null);
    }

    public CardPanel(String title) {
        this(title, null);
    }

    public CardPanel(String title, String subtitle) {
        super(new BorderLayout(0, UiTokens.space(3)), false);
        setOpaque(false);

        configureHeader();
        configureSections();
        setTitle(title);
        setSubtitle(subtitle);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        applyPadding();
        installListeners();
    }

    /**
     * Define o conteúdo principal do cartão.
     */
    public CardPanel setContent(JComponent content) {
        body.removeAll();
        if (content != null) {
            body.add(content, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define o componente exibido no rodapé.
     */
    public CardPanel setFooter(JComponent content) {
        footer.removeAll();
        if (content != null) {
            footer.add(content, BorderLayout.CENTER);
        }
        footer.setVisible(content != null);
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define o componente exibido à direita do cabeçalho.
     */
    public CardPanel setHeaderAction(JComponent action) {
        java.awt.Component previous = ((BorderLayout) header.getLayout()).getLayoutComponent(BorderLayout.EAST);
        if (previous != null) {
            header.remove(previous);
        }
        if (action != null) {
            header.add(action, BorderLayout.EAST);
        }
        refreshHeaderVisibility();
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define o título do cartão.
     */
    public CardPanel setTitle(String title) {
        titleLabel.setText(title != null ? title : "");
        titleLabel.setVisible(title != null && !title.isEmpty());
        refreshHeaderVisibility();
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define o subtítulo do cartão.
     */
    public CardPanel setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle != null ? subtitle : "");
        subtitleLabel.setVisible(subtitle != null && !subtitle.isEmpty());
        refreshHeaderVisibility();
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define a variante visual da superfície.
     */
    public CardPanel setVariant(Variant variant) {
        if (variant == null) {
            throw new IllegalArgumentException("variant cannot be null");
        }
        this.variant = variant;
        repaint();
        return this;
    }

    /**
     * Variante visual corrente.
     */
    public Variant getVariant() {
        return variant;
    }

    /**
     * Define o raio de canto do cartão.
     */
    public CardPanel setArc(int arc) {
        if (arc < 0) {
            throw new IllegalArgumentException("arc cannot be negative");
        }
        this.arc = arc;
        repaint();
        return this;
    }

    /**
     * Define a intensidade da sombra na variante elevada.
     */
    public CardPanel setShadowSpread(int shadowSpread) {
        if (shadowSpread < 0) {
            throw new IllegalArgumentException("shadowSpread cannot be negative");
        }
        this.shadowSpread = shadowSpread;
        applyPadding();
        repaint();
        return this;
    }

    /**
     * Define o espaçamento interno do cartão.
     */
    public CardPanel setPadding(Insets padding) {
        if (padding == null) {
            throw new IllegalArgumentException("padding cannot be null");
        }
        this.padding = padding;
        applyPadding();
        revalidate();
        repaint();
        return this;
    }

    /**
     * Torna o cartão clicável, disparando o evento de ação.
     */
    public CardPanel setClickable(boolean clickable) {
        this.clickable = clickable;
        setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        return this;
    }

    /**
     * Indica se o cartão responde a cliques.
     */
    public boolean isClickable() {
        return clickable;
    }

    /**
     * Define as cores de fundo e de borda do cartão.
     */
    public CardPanel setColors(Color backgroundColor, Color borderColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        repaint();
        return this;
    }

    /**
     * Painel de corpo, exposto para composições avançadas.
     */
    public JPanel getBody() {
        return body;
    }

    private void configureHeader() {
        header.setOpaque(false);
        JPanel titles = new JPanel(new BorderLayout());
        titles.setOpaque(false);

        titleLabel.setFont(UiTokens.font().deriveFont(Font.BOLD, UiTokens.font().getSize2D() + 1f));
        titleLabel.setForeground(UiTokens.foreground());
        subtitleLabel.setFont(UiTokens.fontSmall());
        subtitleLabel.setForeground(UiTokens.muted());

        titles.add(titleLabel, BorderLayout.NORTH);
        titles.add(subtitleLabel, BorderLayout.CENTER);
        header.add(titles, BorderLayout.CENTER);
    }

    private void configureSections() {
        body.setOpaque(false);
        footer.setOpaque(false);
        footer.setVisible(false);
    }

    private void installListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (!clickable || !isEnabled()) {
                    return;
                }
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean wasPressed = pressed;
                pressed = false;
                repaint();
                if (wasPressed && clickable && isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                    dispatchEvent(EventType.ACTION, CardPanel.this, CardPanel.this, Map.of());
                }
            }
        });
    }

    private void applyPadding() {
        int inset = variant == Variant.ELEVATED ? shadowSpread : 0;
        setBorder(BorderFactory.createEmptyBorder(
                padding.top + inset,
                padding.left + inset,
                padding.bottom + inset,
                padding.right + inset));
    }

    private void refreshHeaderVisibility() {
        boolean hasAction = ((BorderLayout) header.getLayout()).getLayoutComponent(BorderLayout.EAST) != null;
        header.setVisible(titleLabel.isVisible() || subtitleLabel.isVisible() || hasAction);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle bounds = getSurfaceBounds();
            if (variant == Variant.ELEVATED) {
                paintShadow(g2, bounds);
            }
            paintSurface(g2, bounds);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Retângulo ocupado pela superfície do cartão.
     */
    protected Rectangle getSurfaceBounds() {
        int inset = variant == Variant.ELEVATED ? shadowSpread : 0;
        return new Rectangle(inset, inset,
                Math.max(0, getWidth() - inset * 2),
                Math.max(0, getHeight() - inset * 2));
    }

    /**
     * Pinta a sombra sob a superfície.
     */
    protected void paintShadow(Graphics2D g2, Rectangle bounds) {
        if (shadowSpread <= 0) {
            return;
        }
        PaintUtils.softShadow(g2, PaintUtils.roundRect(bounds, arc),
                UiTokens.overlay(Color.BLACK, hover && clickable ? 0.22f : 0.14f), shadowSpread, 2);
    }

    /**
     * Pinta o fundo e a borda da superfície.
     */
    protected void paintSurface(Graphics2D g2, Rectangle bounds) {
        Color fill = resolveBackground();
        PaintUtils.fillRoundRect(g2, bounds, arc, fill);

        if (variant != Variant.ELEVATED || hover) {
            Color stroke = borderColor != null ? borderColor : UiTokens.border();
            PaintUtils.drawRoundRect(g2, bounds, arc, stroke, UiTokens.stroke());
        }
    }

    private Color resolveBackground() {
        Color base = backgroundColor != null
                ? backgroundColor
                : (variant == Variant.FILLED ? UiTokens.surfaceAlt() : UiTokens.surface());
        if (!isEnabled()) {
            return UiTokens.disabled(base);
        }
        if (!clickable) {
            return base;
        }
        if (pressed) {
            return UiTokens.pressed(base);
        }
        return hover ? UiTokens.hover(base) : base;
    }
}
