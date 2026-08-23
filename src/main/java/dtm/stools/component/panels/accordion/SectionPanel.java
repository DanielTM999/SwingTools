package dtm.stools.component.panels.accordion;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Seção colapsável com cabeçalho clicável, seta animada e transição de altura do conteúdo.
 */
public class SectionPanel extends PanelEventListener {

    public static final String EXPANDED = "sectionExpanded";
    public static final String COLLAPSED = "sectionCollapsed";

    private final Header header = new Header();
    private final JPanel content = new JPanel(new BorderLayout());

    private String title;
    private String subtitle = "";
    private boolean expanded = true;
    private boolean animated = true;

    private int animationDuration = 200;
    private float animationProgress = 1f;
    private float animationStart;
    private float animationTarget = 1f;
    private long animationStartedAtNanos;
    private long animationRunDurationNanos;

    private int arc = UiTokens.radius(UiTokens.Radius.MD);
    private int headerHeight = 44;

    private Color backgroundColor;
    private Color headerColor;
    private Color borderColor;

    private final Timer animationTimer;

    private SectionGroup group;

    public SectionPanel() {
        this("", null);
    }

    public SectionPanel(String title) {
        this(title, null);
    }

    public SectionPanel(String title, JComponent content) {
        super(new BorderLayout(), false);
        this.title = title != null ? title : "";

        setOpaque(false);
        this.content.setOpaque(false);
        this.content.setBorder(BorderFactory.createEmptyBorder(
                0, UiTokens.space(4), UiTokens.space(3), UiTokens.space(4)));

        add(header, BorderLayout.NORTH);
        add(this.content, BorderLayout.CENTER);

        setContent(content);
        animationTimer = new Timer(16, e -> updateAnimation());
    }

    /**
     * Define o componente exibido quando a seção está expandida.
     */
    public SectionPanel setContent(JComponent component) {
        content.removeAll();
        if (component != null) {
            content.add(component, BorderLayout.CENTER);
        }
        revalidate();
        repaint();
        return this;
    }

    /**
     * Título exibido no cabeçalho.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Define o título exibido no cabeçalho.
     */
    public SectionPanel setTitle(String title) {
        this.title = title != null ? title : "";
        header.repaint();
        return this;
    }

    /**
     * Define o subtítulo exibido à direita do cabeçalho.
     */
    public SectionPanel setSubtitle(String subtitle) {
        this.subtitle = subtitle != null ? subtitle : "";
        header.repaint();
        return this;
    }

    /**
     * Indica se a seção está expandida.
     */
    public boolean isExpanded() {
        return expanded;
    }

    /**
     * Expande ou colapsa a seção disparando eventos.
     */
    public SectionPanel setExpanded(boolean expanded) {
        return setExpanded(expanded, true);
    }

    /**
     * Expande ou colapsa a seção, opcionalmente sem disparar eventos.
     */
    public SectionPanel setExpanded(boolean expanded, boolean fireEvent) {
        if (this.expanded == expanded) {
            return this;
        }

        boolean oldValue = this.expanded;
        this.expanded = expanded;
        animateToState();
        firePropertyChange("expanded", oldValue, expanded);

        if (expanded && group != null) {
            group.notifyExpanded(this);
        }

        if (fireEvent) {
            Map<String, Object> props = Map.of("oldValue", oldValue, "newValue", expanded);
            dispatchEvent(expanded ? EXPANDED : COLLAPSED, this, expanded, props);
            dispatchEvent(expanded ? EventType.EXPAND : EventType.COLLAPSE, this, expanded, props);
            dispatchEvent(EventType.CHANGE, this, expanded, props);
        }
        return this;
    }

    /**
     * Alterna entre expandido e colapsado.
     */
    public SectionPanel toggle() {
        return setExpanded(!expanded);
    }

    /**
     * Habilita a animação da transição de altura.
     */
    public SectionPanel setAnimated(boolean animated) {
        this.animated = animated;
        if (!animated) {
            animationTimer.stop();
            animationProgress = expanded ? 1f : 0f;
            animationTarget = animationProgress;
            content.setVisible(expanded);
            revalidate();
            repaint();
        }
        return this;
    }

    /**
     * Define a duração da animação em milissegundos.
     */
    public SectionPanel setAnimationDuration(int animationDuration) {
        if (animationDuration < 0) {
            throw new IllegalArgumentException("animationDuration cannot be negative");
        }
        this.animationDuration = animationDuration;
        return this;
    }

    /**
     * Define a altura do cabeçalho.
     */
    public SectionPanel setHeaderHeight(int headerHeight) {
        if (headerHeight <= 0) {
            throw new IllegalArgumentException("headerHeight must be greater than zero");
        }
        this.headerHeight = headerHeight;
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define o raio de canto da seção.
     */
    public SectionPanel setArc(int arc) {
        if (arc < 0) {
            throw new IllegalArgumentException("arc cannot be negative");
        }
        this.arc = arc;
        repaint();
        return this;
    }

    /**
     * Define as cores de fundo, de cabeçalho e de borda.
     */
    public SectionPanel setColors(Color backgroundColor, Color headerColor, Color borderColor) {
        this.backgroundColor = backgroundColor;
        this.headerColor = headerColor;
        this.borderColor = borderColor;
        repaint();
        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferred = super.getPreferredSize();
        if (animationProgress >= 1f || !content.isVisible()) {
            return preferred;
        }
        int contentHeight = content.getPreferredSize().height;
        int height = preferred.height - contentHeight + Math.round(contentHeight * animationProgress);
        return new Dimension(preferred.width, Math.max(headerHeight, height));
    }

    @Override
    public void removeNotify() {
        animationTimer.stop();
        super.removeNotify();
    }

    void attachGroup(SectionGroup group) {
        this.group = group;
    }

    private void animateToState() {
        float target = expanded ? 1f : 0f;
        if (!animated || animationDuration <= 0 || !isShowing()) {
            animationTimer.stop();
            animationProgress = target;
            animationTarget = target;
            content.setVisible(expanded);
            revalidate();
            repaint();
            return;
        }

        content.setVisible(true);
        animationStart = animationProgress;
        animationTarget = target;
        animationStartedAtNanos = System.nanoTime();
        animationRunDurationNanos = (long) (animationDuration * 1_000_000L
                * Math.abs(target - animationStart));
        if (animationRunDurationNanos <= 0) {
            animationProgress = target;
            content.setVisible(expanded);
            revalidate();
            repaint();
            return;
        }
        animationTimer.start();
    }

    private void updateAnimation() {
        long elapsed = System.nanoTime() - animationStartedAtNanos;
        float ratio = Math.min(1f, (float) elapsed / animationRunDurationNanos);
        animationProgress = animationStart + (animationTarget - animationStart) * PaintUtils.easeInOut(ratio);
        if (ratio >= 1f) {
            animationProgress = animationTarget;
            animationTimer.stop();
            content.setVisible(expanded);
        }
        revalidate();
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
            Color fill = backgroundColor != null ? backgroundColor : UiTokens.surface();
            PaintUtils.fillRoundRect(g2, bounds, arc, isEnabled() ? fill : UiTokens.disabled(fill));
            PaintUtils.drawRoundRect(g2, bounds, arc,
                    borderColor != null ? borderColor : UiTokens.border(), UiTokens.stroke());
        } finally {
            g2.dispose();
        }
    }

    /**
     * Cabeçalho clicável que alterna o estado da seção.
     */
    private final class Header extends JComponent {

        private boolean hover;

        private Header() {
            setOpaque(false);
            setFocusable(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            installListeners();
        }

        @Override
        public Dimension getPreferredSize() {
            java.awt.FontMetrics metrics = getFontMetrics(UiTokens.font().deriveFont(Font.BOLD));
            int width = UiTokens.space(4) + UiTokens.space(3)
                    + metrics.stringWidth(title != null ? title : "")
                    + UiTokens.space(4);
            return new Dimension(width, headerHeight);
        }

        private void installListeners() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!SectionPanel.this.isEnabled() || !SwingUtilities.isLeftMouseButton(e)) {
                        return;
                    }
                    requestFocusInWindow();
                    toggle();
                }

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
            });

            addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (!SectionPanel.this.isEnabled()) {
                        return;
                    }
                    if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                        toggle();
                        e.consume();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
            try {
                Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
                if (hover && SectionPanel.this.isEnabled()) {
                    Color base = headerColor != null ? headerColor : UiTokens.surfaceAlt();
                    PaintUtils.fillRoundRect(g2, bounds, arc, UiTokens.hover(base));
                } else if (headerColor != null) {
                    PaintUtils.fillRoundRect(g2, bounds, arc, headerColor);
                }

                paintChevron(g2);
                paintTitles(g2);
            } finally {
                g2.dispose();
            }
        }

        private void paintChevron(Graphics2D g2) {
            int cx = UiTokens.space(4);
            int cy = getHeight() / 2;
            int arm = UiTokens.scale(4);

            Color color = SectionPanel.this.isEnabled()
                    ? (hover ? UiTokens.primary() : UiTokens.muted())
                    : UiTokens.disabled(UiTokens.muted());
            g2.setColor(color);
            g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            g2.rotate(Math.toRadians(90 * animationProgress), cx, cy);
            g2.drawLine(cx - arm / 2, cy - arm, cx + arm / 2, cy);
            g2.drawLine(cx + arm / 2, cy, cx - arm / 2, cy + arm);
            g2.rotate(-Math.toRadians(90 * animationProgress), cx, cy);
        }

        private void paintTitles(Graphics2D g2) {
            int x = UiTokens.space(4) + UiTokens.space(3);
            Color titleColor = SectionPanel.this.isEnabled()
                    ? UiTokens.foreground()
                    : UiTokens.disabled(UiTokens.foreground());

            g2.setFont(UiTokens.font().deriveFont(Font.BOLD));
            java.awt.FontMetrics metrics = g2.getFontMetrics();
            int subtitleWidth = subtitle.isEmpty() ? 0 : subtitleWidth(g2);
            int available = Math.max(0, getWidth() - x - UiTokens.space(4) - subtitleWidth);
            PaintUtils.drawLeftText(g2, title, new Rectangle(x, 0, available, getHeight()), titleColor);

            if (subtitle.isEmpty()) {
                return;
            }
            g2.setFont(UiTokens.fontSmall());
            Rectangle bounds = new Rectangle(
                    getWidth() - UiTokens.space(4) - subtitleWidth, 0, subtitleWidth, getHeight());
            PaintUtils.drawLeftText(g2, subtitle, bounds, UiTokens.muted());
            g2.setFont(metrics.getFont());
        }

        private int subtitleWidth(Graphics2D g2) {
            java.awt.Font previous = g2.getFont();
            g2.setFont(UiTokens.fontSmall());
            int width = g2.getFontMetrics().stringWidth(subtitle) + UiTokens.space(2);
            g2.setFont(previous);
            return width;
        }
    }
}
