package dtm.stools.component.panels.card;

import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.List;

/**
 * Cartão de indicador com rótulo, valor destacado, variação percentual e minigráfico opcional.
 */
public class StatCard extends CardPanel {

    /**
     * Direção da variação exibida ao lado do valor.
     */
    public enum Trend {
        UP, DOWN, NEUTRAL
    }

    private final Metrics metrics = new Metrics();

    private String label = "";
    private String value = "";
    private String delta = "";
    private String caption = "";
    private Trend trend = Trend.NEUTRAL;
    private Icon icon;
    private List<Double> sparkline = List.of();

    private Color valueColor;
    private Color labelColor;

    public StatCard() {
        this("", "");
    }

    public StatCard(String label, String value) {
        super();
        this.label = label != null ? label : "";
        this.value = value != null ? value : "";

        setVariant(Variant.OUTLINED);
        setContent(metrics);
        setPreferredSize(new Dimension(UiTokens.scale(220), UiTokens.scale(120)));
    }

    /**
     * Define o rótulo do indicador.
     */
    public StatCard setLabel(String label) {
        this.label = label != null ? label : "";
        metrics.repaint();
        return this;
    }

    /**
     * Define o valor principal exibido.
     */
    public StatCard setValue(String value) {
        this.value = value != null ? value : "";
        metrics.repaint();
        return this;
    }

    /**
     * Define a variação e a direção exibidas ao lado do valor.
     */
    public StatCard setDelta(String delta, Trend trend) {
        this.delta = delta != null ? delta : "";
        this.trend = trend != null ? trend : Trend.NEUTRAL;
        metrics.repaint();
        return this;
    }

    /**
     * Define a legenda exibida abaixo do valor.
     */
    public StatCard setCaption(String caption) {
        this.caption = caption != null ? caption : "";
        metrics.repaint();
        return this;
    }

    /**
     * Define o ícone exibido no canto do cartão.
     */
    public StatCard setIcon(Icon icon) {
        this.icon = icon;
        metrics.repaint();
        return this;
    }

    /**
     * Define a série usada no minigráfico de tendência.
     */
    public StatCard setSparkline(List<Double> sparkline) {
        this.sparkline = sparkline != null ? List.copyOf(sparkline) : List.of();
        metrics.repaint();
        return this;
    }

    /**
     * Define as cores do valor e do rótulo.
     */
    public StatCard setTextColors(Color valueColor, Color labelColor) {
        this.valueColor = valueColor;
        this.labelColor = labelColor;
        metrics.repaint();
        return this;
    }

    private Color trendColor() {
        return switch (trend) {
            case UP -> UiTokens.success();
            case DOWN -> UiTokens.danger();
            case NEUTRAL -> UiTokens.muted();
        };
    }

    /**
     * Área desenhada com o rótulo, o valor, a variação e o minigráfico.
     */
    private final class Metrics extends JComponent {

        private Metrics() {
            setOpaque(false);
            setPreferredSize(new Dimension(UiTokens.scale(200), UiTokens.scale(88)));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
            try {
                int y = paintLabel(g2, 0);
                y = paintValue(g2, y);
                paintCaption(g2, y);
                paintSparkline(g2);
                paintIcon(g2);
            } finally {
                g2.dispose();
            }
        }

        private int paintLabel(Graphics2D g2, int y) {
            if (label.isEmpty()) {
                return y;
            }
            g2.setFont(UiTokens.fontSmall());
            FontMetrics fontMetrics = g2.getFontMetrics();
            Color color = labelColor != null ? labelColor : UiTokens.muted();
            PaintUtils.drawLeftText(g2, label,
                    new Rectangle(0, y, iconAwareWidth(), fontMetrics.getHeight()), color);
            return y + fontMetrics.getHeight() + UiTokens.space(1);
        }

        private int paintValue(Graphics2D g2, int y) {
            g2.setFont(UiTokens.font().deriveFont(Font.BOLD, UiTokens.font().getSize2D() + 12f));
            FontMetrics fontMetrics = g2.getFontMetrics();
            Color color = valueColor != null ? valueColor : UiTokens.foreground();

            int valueWidth = fontMetrics.stringWidth(value);
            PaintUtils.drawLeftText(g2, value,
                    new Rectangle(0, y, iconAwareWidth(), fontMetrics.getHeight()), color);

            if (!delta.isEmpty()) {
                paintDelta(g2, valueWidth + UiTokens.space(2), y, fontMetrics.getHeight());
            }
            return y + fontMetrics.getHeight() + UiTokens.space(1);
        }

        private void paintDelta(Graphics2D g2, int x, int y, int height) {
            g2.setFont(UiTokens.fontSmall().deriveFont(Font.BOLD));
            FontMetrics fontMetrics = g2.getFontMetrics();
            Color color = trendColor();

            int arrowSize = UiTokens.scale(7);
            int centerY = y + height / 2;
            paintArrow(g2, x, centerY, arrowSize, color);

            int textX = x + arrowSize + UiTokens.space(1);
            PaintUtils.drawLeftText(g2, delta,
                    new Rectangle(textX, y, Math.max(0, getWidth() - textX), height), color);
            g2.setFont(fontMetrics.getFont());
        }

        private void paintArrow(Graphics2D g2, int x, int centerY, int size, Color color) {
            if (trend == Trend.NEUTRAL) {
                g2.setColor(color);
                g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x, centerY, x + size, centerY);
                return;
            }

            int direction = trend == Trend.UP ? -1 : 1;
            g2.setColor(color);
            g2.setStroke(new BasicStroke(UiTokens.stroke(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + size / 2, centerY - size / 2 * direction, x + size / 2, centerY + size / 2 * direction);
            g2.drawLine(x, centerY, x + size / 2, centerY - size / 2 * direction);
            g2.drawLine(x + size, centerY, x + size / 2, centerY - size / 2 * direction);
        }

        private void paintCaption(Graphics2D g2, int y) {
            if (caption.isEmpty()) {
                return;
            }
            g2.setFont(UiTokens.fontSmall());
            PaintUtils.drawLeftText(g2, caption,
                    new Rectangle(0, y, getWidth(), g2.getFontMetrics().getHeight()), UiTokens.muted());
        }

        private void paintSparkline(Graphics2D g2) {
            if (sparkline.size() < 2) {
                return;
            }

            int height = UiTokens.scale(28);
            Rectangle area = new Rectangle(0, getHeight() - height, getWidth(), height);

            double minimum = sparkline.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double maximum = sparkline.stream().mapToDouble(Double::doubleValue).max().orElse(1);
            double span = maximum - minimum;

            java.awt.geom.Path2D.Float path = new java.awt.geom.Path2D.Float();
            for (int i = 0; i < sparkline.size(); i++) {
                float x = area.x + (float) area.width * i / (sparkline.size() - 1);
                float ratio = span == 0 ? 0.5f : (float) ((sparkline.get(i) - minimum) / span);
                float y = area.y + area.height - ratio * area.height;
                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            g2.setColor(trendColor());
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(path);
        }

        private void paintIcon(Graphics2D g2) {
            if (icon == null) {
                return;
            }
            icon.paintIcon(this, g2, getWidth() - icon.getIconWidth(), 0);
        }

        private int iconAwareWidth() {
            return icon != null
                    ? Math.max(0, getWidth() - icon.getIconWidth() - UiTokens.space(2))
                    : getWidth();
        }
    }
}
