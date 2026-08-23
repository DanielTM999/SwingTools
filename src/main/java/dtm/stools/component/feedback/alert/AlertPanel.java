package dtm.stools.component.feedback.alert;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.ColorUtils;
import dtm.stools.utils.PaintUtils;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;

/**
 * Faixa de aviso em linha com ícone semântico, título, mensagem, ações e botão de fechar.
 */
public class AlertPanel extends PanelEventListener {

    public static final String DISMISSED = "alertDismissed";

    /**
     * Severidade do aviso.
     */
    public enum Severity {
        INFO, SUCCESS, WARNING, ERROR
    }

    private final JLabel titleLabel = new JLabel();
    private final JLabel messageLabel = new JLabel();
    private final JPanel texts = new JPanel(new BorderLayout());
    private final JPanel actions = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, UiTokens.space(2), 0));
    private final CloseButton closeButton = new CloseButton();

    private Severity severity = Severity.INFO;
    private boolean closable = true;
    private boolean showIcon = true;
    private int arc = UiTokens.radius(UiTokens.Radius.MD);
    private int accentBarWidth = 3;

    private Color customAccent;

    public AlertPanel() {
        this(Severity.INFO, "", "");
    }

    public AlertPanel(Severity severity, String title, String message) {
        super(new BorderLayout(UiTokens.space(3), 0), false);
        this.severity = severity != null ? severity : Severity.INFO;

        setOpaque(false);
        configureTexts();
        setTitle(title);
        setMessage(message);

        add(texts, BorderLayout.CENTER);
        add(closeButton, BorderLayout.EAST);

        closeButton.onClick(this::dismiss);
        applyPadding();
    }

    /**
     * Define a severidade do aviso.
     */
    public AlertPanel setSeverity(Severity severity) {
        if (severity == null) {
            throw new IllegalArgumentException("severity cannot be null");
        }
        this.severity = severity;
        refreshColors();
        repaint();
        return this;
    }

    /**
     * Severidade corrente.
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Define o título do aviso.
     */
    public AlertPanel setTitle(String title) {
        titleLabel.setText(title != null ? title : "");
        titleLabel.setVisible(title != null && !title.isEmpty());
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define a mensagem do aviso.
     */
    public AlertPanel setMessage(String message) {
        messageLabel.setText(message != null && !message.isEmpty() ? "<html>" + message + "</html>" : "");
        messageLabel.setVisible(message != null && !message.isEmpty());
        revalidate();
        repaint();
        return this;
    }

    /**
     * Adiciona um componente de ação ao rodapé do aviso.
     */
    public AlertPanel addAction(JComponent action) {
        if (action == null) {
            throw new IllegalArgumentException("action cannot be null");
        }
        actions.add(action);
        if (texts.getComponentCount() < 3) {
            texts.add(actions, BorderLayout.SOUTH);
        }
        revalidate();
        repaint();
        return this;
    }

    /**
     * Habilita o botão de fechar.
     */
    public AlertPanel setClosable(boolean closable) {
        this.closable = closable;
        closeButton.setVisible(closable);
        revalidate();
        repaint();
        return this;
    }

    /**
     * Exibe ou oculta o ícone semântico.
     */
    public AlertPanel setShowIcon(boolean showIcon) {
        this.showIcon = showIcon;
        applyPadding();
        repaint();
        return this;
    }

    /**
     * Define uma cor de destaque customizada.
     */
    public AlertPanel setAccentColor(Color customAccent) {
        this.customAccent = customAccent;
        refreshColors();
        repaint();
        return this;
    }

    /**
     * Define o raio de canto do aviso.
     */
    public AlertPanel setArc(int arc) {
        if (arc < 0) {
            throw new IllegalArgumentException("arc cannot be negative");
        }
        this.arc = arc;
        repaint();
        return this;
    }

    /**
     * Oculta o aviso e dispara o evento de dispensa.
     */
    public AlertPanel dismiss() {
        setVisible(false);
        Map<String, Object> props = Map.of("severity", severity.name());
        dispatchEvent(DISMISSED, this, severity, props);
        dispatchEvent(EventType.DISMISS, this, severity, props);
        return this;
    }

    /**
     * Torna o aviso visível novamente após uma dispensa.
     */
    public AlertPanel restore() {
        setVisible(true);
        revalidate();
        repaint();
        return this;
    }

    private void configureTexts() {
        texts.setOpaque(false);
        actions.setOpaque(false);

        titleLabel.setFont(UiTokens.font().deriveFont(Font.BOLD));
        messageLabel.setFont(UiTokens.fontSmall());

        texts.add(titleLabel, BorderLayout.NORTH);
        texts.add(messageLabel, BorderLayout.CENTER);
        refreshColors();
    }

    private void refreshColors() {
        titleLabel.setForeground(UiTokens.foreground());
        messageLabel.setForeground(UiTokens.muted());
    }

    private void applyPadding() {
        int left = accentBarWidth + UiTokens.space(3) + (showIcon ? UiTokens.scale(22) : 0);
        setBorder(BorderFactory.createEmptyBorder(
                UiTokens.space(3), left, UiTokens.space(3), UiTokens.space(3)));
    }

    private Color resolveAccent() {
        if (customAccent != null) {
            return customAccent;
        }
        return switch (severity) {
            case SUCCESS -> UiTokens.success();
            case WARNING -> UiTokens.warning();
            case ERROR -> UiTokens.danger();
            case INFO -> UiTokens.info();
        };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
            paintSurface(g2, bounds);
            paintAccentBar(g2, bounds);
            if (showIcon) {
                paintIcon(g2);
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * Pinta o fundo translúcido do aviso.
     */
    protected void paintSurface(Graphics2D g2, Rectangle bounds) {
        Color accent = resolveAccent();
        PaintUtils.fillRoundRect(g2, bounds, arc, ColorUtils.withAlpha(accent, 0.12f));
        PaintUtils.drawRoundRect(g2, bounds, arc, ColorUtils.withAlpha(accent, 0.42f), UiTokens.stroke());
    }

    /**
     * Pinta a faixa de destaque à esquerda.
     */
    protected void paintAccentBar(Graphics2D g2, Rectangle bounds) {
        if (accentBarWidth <= 0) {
            return;
        }
        g2.setColor(resolveAccent());
        g2.fillRoundRect(0, 0, accentBarWidth * 2, bounds.height, arc, arc);
        g2.fillRect(accentBarWidth, 0, accentBarWidth, bounds.height);
    }

    /**
     * Pinta o glifo correspondente à severidade.
     */
    protected void paintIcon(Graphics2D g2) {
        int size = UiTokens.scale(16);
        int x = accentBarWidth + UiTokens.space(3);
        int y = UiTokens.space(3) + 2;
        Color accent = resolveAccent();

        g2.setColor(accent);
        g2.fillOval(x, y, size, size);

        g2.setColor(UiTokens.onColor(accent));
        g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = x + size / 2;
        int cy = y + size / 2;

        switch (severity) {
            case SUCCESS -> {
                g2.drawLine(cx - size / 5, cy, cx - size / 16, cy + size / 5);
                g2.drawLine(cx - size / 16, cy + size / 5, cx + size / 4, cy - size / 5);
            }
            case ERROR -> {
                g2.drawLine(cx - size / 5, cy - size / 5, cx + size / 5, cy + size / 5);
                g2.drawLine(cx + size / 5, cy - size / 5, cx - size / 5, cy + size / 5);
            }
            case WARNING, INFO -> {
                boolean warning = severity == Severity.WARNING;
                int barTop = warning ? cy - size / 4 : cy - size / 8;
                int barBottom = warning ? cy + size / 12 : cy + size / 4;
                g2.drawLine(cx, barTop, cx, barBottom);
                int dotY = warning ? cy + size / 4 : cy - size / 4;
                g2.fillOval(cx - 1, dotY - 1, 2, 2);
            }
        }
    }

    /**
     * Botão de fechar desenhado como um X.
     */
    private static final class CloseButton extends JComponent {

        private Runnable action = () -> { };
        private boolean hover;

        private CloseButton() {
            setOpaque(false);
            setFocusable(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(UiTokens.scale(22), UiTokens.scale(22)));
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
                public void mousePressed(MouseEvent e) {
                    if (isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                        action.run();
                    }
                }
            });
        }

        private void onClick(Runnable action) {
            this.action = action != null ? action : () -> { };
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
            try {
                if (hover) {
                    PaintUtils.fillRoundRect(g2, new Rectangle(0, 0, getWidth(), getHeight()),
                            UiTokens.radius(UiTokens.Radius.SM), UiTokens.overlay(UiTokens.muted(), 0.18f));
                }
                g2.setColor(hover ? UiTokens.foreground() : UiTokens.muted());
                g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

                int arm = UiTokens.scale(4);
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.drawLine(cx - arm, cy - arm, cx + arm, cy + arm);
                g2.drawLine(cx + arm, cy - arm, cx - arm, cy + arm);
            } finally {
                g2.dispose();
            }
        }
    }
}
