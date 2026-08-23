package dtm.stools.component.panels.emptystate;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.configs.UiTokens;
import dtm.stools.layouts.FlexBoxLayout;
import dtm.stools.utils.PaintUtils;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Map;

/**
 * Estado vazio com ícone, título, descrição e ação opcional.
 */
public class EmptyStatePanel extends PanelEventListener {

    public static final String ACTION_TRIGGERED = "emptyStateAction";

    private final JLabel iconLabel = new JLabel();
    private final JLabel titleLabel = new JLabel();
    private final JLabel descriptionLabel = new JLabel();

    private JComponent action;
    private boolean dashedBorder;
    private int arc = UiTokens.radius(UiTokens.Radius.LG);

    private Color backgroundColor;
    private Color borderColor;

    public EmptyStatePanel() {
        this("", "");
    }

    public EmptyStatePanel(String title, String description) {
        super(FlexBoxLayout.builder()
                .direction(FlexBoxLayout.Direction.COLUMN)
                .align(FlexBoxLayout.Align.CENTER)
                .justify(FlexBoxLayout.Justify.CENTER)
                .gap(UiTokens.space(2))
                .padding(UiTokens.space(6))
                .build(), false);

        setOpaque(false);
        configureLabels();
        setTitle(title);
        setDescription(description);

        add(iconLabel);
        add(titleLabel);
        add(descriptionLabel);

        setPreferredSize(new Dimension(UiTokens.scale(320), UiTokens.scale(200)));
    }

    /**
     * Define o ícone exibido acima do título.
     */
    public EmptyStatePanel setIcon(Icon icon) {
        iconLabel.setIcon(icon);
        iconLabel.setVisible(icon != null);
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define o título do estado vazio.
     */
    public EmptyStatePanel setTitle(String title) {
        titleLabel.setText(title != null ? title : "");
        titleLabel.setVisible(title != null && !title.isEmpty());
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define a descrição exibida abaixo do título.
     */
    public EmptyStatePanel setDescription(String description) {
        descriptionLabel.setText(description != null ? "<html><center>" + description + "</center></html>" : "");
        descriptionLabel.setVisible(description != null && !description.isEmpty());
        revalidate();
        repaint();
        return this;
    }

    /**
     * Define o componente de ação exibido no rodapé.
     */
    public EmptyStatePanel setAction(JComponent action) {
        if (this.action != null) {
            remove(this.action);
        }
        this.action = action;
        if (action != null) {
            add(action);
        }
        revalidate();
        repaint();
        return this;
    }

    /**
     * Cria um botão de ação com o texto informado e o dispara como evento.
     */
    public EmptyStatePanel setActionButton(String text) {
        JButton button = new JButton(text != null ? text : "");
        button.addActionListener(e ->
                dispatchEvent(ACTION_TRIGGERED, this, text, Map.of("action", String.valueOf(text))));
        button.addActionListener(e ->
                dispatchEvent(EventType.ACTION, this, text, Map.of("action", String.valueOf(text))));
        return setAction(button);
    }

    /**
     * Habilita o contorno tracejado ao redor do painel.
     */
    public EmptyStatePanel setDashedBorder(boolean dashedBorder) {
        this.dashedBorder = dashedBorder;
        repaint();
        return this;
    }

    /**
     * Define o raio de canto do painel.
     */
    public EmptyStatePanel setArc(int arc) {
        if (arc < 0) {
            throw new IllegalArgumentException("arc cannot be negative");
        }
        this.arc = arc;
        repaint();
        return this;
    }

    /**
     * Define as cores de fundo e de borda.
     */
    public EmptyStatePanel setColors(Color backgroundColor, Color borderColor) {
        this.backgroundColor = backgroundColor;
        this.borderColor = borderColor;
        repaint();
        return this;
    }

    private void configureLabels() {
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setVisible(false);

        titleLabel.setFont(UiTokens.font().deriveFont(Font.BOLD, UiTokens.font().getSize2D() + 2f));
        titleLabel.setForeground(UiTokens.foreground());
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);

        descriptionLabel.setFont(UiTokens.fontSmall());
        descriptionLabel.setForeground(UiTokens.muted());
        descriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (backgroundColor == null && !dashedBorder && borderColor == null) {
            return;
        }

        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle bounds = new Rectangle(0, 0, getWidth(), getHeight());
            if (backgroundColor != null) {
                PaintUtils.fillRoundRect(g2, bounds, arc, backgroundColor);
            }
            paintBorder(g2, bounds);
        } finally {
            g2.dispose();
        }
    }

    /**
     * Pinta o contorno sólido ou tracejado do painel.
     */
    protected void paintBorder(Graphics2D g2, Rectangle bounds) {
        Color stroke = borderColor != null ? borderColor : UiTokens.border();
        if (!dashedBorder) {
            if (borderColor != null) {
                PaintUtils.drawRoundRect(g2, bounds, arc, stroke, UiTokens.stroke());
            }
            return;
        }

        g2.setColor(stroke);
        g2.setStroke(new java.awt.BasicStroke(UiTokens.stroke(),
                java.awt.BasicStroke.CAP_ROUND,
                java.awt.BasicStroke.JOIN_ROUND,
                1f,
                new float[]{UiTokens.scale(6), UiTokens.scale(5)},
                0f));
        g2.draw(PaintUtils.roundRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2, arc));
    }
}
