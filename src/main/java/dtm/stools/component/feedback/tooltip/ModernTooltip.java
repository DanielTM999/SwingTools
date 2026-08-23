package dtm.stools.component.feedback.tooltip;

import dtm.stools.configs.UiTokens;
import dtm.stools.utils.ColorUtils;
import dtm.stools.utils.PaintUtils;

import javax.swing.JComponent;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

/**
 * Balão de dica arredondado com seta, instalável em qualquer componente.
 */
public final class ModernTooltip {

    /**
     * Lado em que o balão é exibido em relação ao componente.
     */
    public enum Placement {
        TOP, BOTTOM, LEFT, RIGHT
    }

    private static final int ARROW_SIZE = 6;
    private static final int PADDING_X = 10;
    private static final int PADDING_Y = 6;

    private final JComponent target;
    private final Bubble bubble = new Bubble();

    private JWindow window;
    private String text;
    private Placement placement = Placement.TOP;
    private int showDelay = 450;
    private int hideDelay = 120;
    private int offset = 8;

    private Color backgroundColor;
    private Color foregroundColor;

    private final Timer showTimer;
    private final Timer hideTimer;

    private ModernTooltip(JComponent target, String text) {
        this.target = target;
        this.text = text != null ? text : "";

        this.showTimer = new Timer(showDelay, e -> showBubble());
        this.showTimer.setRepeats(false);
        this.hideTimer = new Timer(hideDelay, e -> hideBubble());
        this.hideTimer.setRepeats(false);

        installListeners();
    }

    /**
     * Instala uma dica no componente informado.
     */
    public static ModernTooltip install(JComponent target, String text) {
        if (target == null) {
            throw new IllegalArgumentException("target cannot be null");
        }
        return new ModernTooltip(target, text);
    }

    /**
     * Define o texto exibido no balão.
     */
    public ModernTooltip setText(String text) {
        this.text = text != null ? text : "";
        return this;
    }

    /**
     * Define o lado em que o balão aparece.
     */
    public ModernTooltip setPlacement(Placement placement) {
        if (placement == null) {
            throw new IllegalArgumentException("placement cannot be null");
        }
        this.placement = placement;
        return this;
    }

    /**
     * Define o atraso antes de exibir o balão.
     */
    public ModernTooltip setShowDelay(int showDelay) {
        if (showDelay < 0) {
            throw new IllegalArgumentException("showDelay cannot be negative");
        }
        this.showDelay = showDelay;
        showTimer.setInitialDelay(showDelay);
        return this;
    }

    /**
     * Define o atraso antes de ocultar o balão.
     */
    public ModernTooltip setHideDelay(int hideDelay) {
        if (hideDelay < 0) {
            throw new IllegalArgumentException("hideDelay cannot be negative");
        }
        this.hideDelay = hideDelay;
        hideTimer.setInitialDelay(hideDelay);
        return this;
    }

    /**
     * Define a distância entre o balão e o componente.
     */
    public ModernTooltip setOffset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative");
        }
        this.offset = offset;
        return this;
    }

    /**
     * Define as cores do balão e do texto.
     */
    public ModernTooltip setColors(Color backgroundColor, Color foregroundColor) {
        this.backgroundColor = backgroundColor;
        this.foregroundColor = foregroundColor;
        return this;
    }

    /**
     * Remove os listeners e descarta o balão.
     */
    public void uninstall() {
        showTimer.stop();
        hideTimer.stop();
        hideBubble();
    }

    private void installListeners() {
        target.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hideTimer.stop();
                showTimer.restart();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                showTimer.stop();
                hideTimer.restart();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                showTimer.stop();
                hideBubble();
            }
        });
    }

    private void showBubble() {
        if (text.isEmpty() || !target.isShowing() || !target.isEnabled()) {
            return;
        }

        if (window == null) {
            window = new JWindow(SwingUtilities.getWindowAncestor(target));
            window.setBackground(new Color(0, 0, 0, 0));
            window.setContentPane(bubble);
            window.setFocusableWindowState(false);
        }

        Dimension size = bubble.measure();
        window.setSize(size);
        window.setLocation(resolveLocation(size));
        window.setVisible(true);
    }

    private void hideBubble() {
        if (window != null) {
            window.setVisible(false);
        }
    }

    private Point resolveLocation(Dimension size) {
        Point origin = target.getLocationOnScreen();
        int width = target.getWidth();
        int height = target.getHeight();

        return switch (placement) {
            case TOP -> new Point(
                    origin.x + (width - size.width) / 2,
                    origin.y - size.height - offset);
            case BOTTOM -> new Point(
                    origin.x + (width - size.width) / 2,
                    origin.y + height + offset);
            case LEFT -> new Point(
                    origin.x - size.width - offset,
                    origin.y + (height - size.height) / 2);
            case RIGHT -> new Point(
                    origin.x + width + offset,
                    origin.y + (height - size.height) / 2);
        };
    }

    /**
     * Superfície do balão, com fundo arredondado e seta apontando para o componente.
     */
    private final class Bubble extends JComponent {

        private Bubble() {
            setOpaque(false);
            setFont(UiTokens.fontSmall());
        }

        private Dimension measure() {
            FontMetrics metrics = getFontMetrics(UiTokens.fontSmall());
            int width = metrics.stringWidth(text) + PADDING_X * 2;
            int height = metrics.getHeight() + PADDING_Y * 2;
            boolean vertical = placement == Placement.TOP || placement == Placement.BOTTOM;
            return new Dimension(
                    vertical ? width : width + ARROW_SIZE,
                    vertical ? height + ARROW_SIZE : height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
            try {
                Rectangle body = bodyBounds();
                Color background = backgroundColor != null
                        ? backgroundColor
                        : ColorUtils.withAlpha(UiTokens.isDarkTheme()
                        ? UiTokens.surfaceAlt() : new Color(0x1F2937), 0.96f);

                g2.setColor(background);
                g2.fill(PaintUtils.roundRect(body, UiTokens.radius(UiTokens.Radius.SM)));
                g2.fill(buildArrow(body));

                g2.setFont(UiTokens.fontSmall());
                Color color = foregroundColor != null ? foregroundColor : UiTokens.onColor(background);
                PaintUtils.drawCenteredText(g2, text, body, color);
            } finally {
                g2.dispose();
            }
        }

        private Rectangle bodyBounds() {
            return switch (placement) {
                case TOP -> new Rectangle(0, 0, getWidth(), getHeight() - ARROW_SIZE);
                case BOTTOM -> new Rectangle(0, ARROW_SIZE, getWidth(), getHeight() - ARROW_SIZE);
                case LEFT -> new Rectangle(0, 0, getWidth() - ARROW_SIZE, getHeight());
                case RIGHT -> new Rectangle(ARROW_SIZE, 0, getWidth() - ARROW_SIZE, getHeight());
            };
        }

        private Path2D.Float buildArrow(Rectangle body) {
            Path2D.Float arrow = new Path2D.Float();
            switch (placement) {
                case TOP -> {
                    int cx = getWidth() / 2;
                    arrow.moveTo(cx - ARROW_SIZE, body.height);
                    arrow.lineTo(cx + ARROW_SIZE, body.height);
                    arrow.lineTo(cx, body.height + ARROW_SIZE);
                }
                case BOTTOM -> {
                    int cx = getWidth() / 2;
                    arrow.moveTo(cx - ARROW_SIZE, ARROW_SIZE);
                    arrow.lineTo(cx + ARROW_SIZE, ARROW_SIZE);
                    arrow.lineTo(cx, 0);
                }
                case LEFT -> {
                    int cy = getHeight() / 2;
                    arrow.moveTo(body.width, cy - ARROW_SIZE);
                    arrow.lineTo(body.width, cy + ARROW_SIZE);
                    arrow.lineTo(body.width + ARROW_SIZE, cy);
                }
                case RIGHT -> {
                    int cy = getHeight() / 2;
                    arrow.moveTo(ARROW_SIZE, cy - ARROW_SIZE);
                    arrow.lineTo(ARROW_SIZE, cy + ARROW_SIZE);
                    arrow.lineTo(0, cy);
                }
            }
            arrow.closePath();
            return arrow;
        }
    }
}
