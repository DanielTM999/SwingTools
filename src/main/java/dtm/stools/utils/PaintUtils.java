package dtm.stools.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.RoundRectangle2D;

/**
 * Rotinas de pintura compartilhadas pelos componentes desenhados manualmente.
 */
public final class PaintUtils {

    private static final String ELLIPSIS = "…";

    private PaintUtils() {
        throw new IllegalStateException("utility class");
    }

    /**
     * Ativa antialiasing de formas e de texto no contexto informado.
     */
    public static Graphics2D antialias(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    /**
     * Cria um retângulo arredondado em coordenadas de ponto flutuante.
     */
    public static RoundRectangle2D.Float roundRect(float x, float y, float width, float height, float arc) {
        return new RoundRectangle2D.Float(x, y, width, height, arc, arc);
    }

    /**
     * Cria um retângulo arredondado a partir de um retângulo inteiro.
     */
    public static RoundRectangle2D.Float roundRect(Rectangle bounds, float arc) {
        return roundRect(bounds.x, bounds.y, bounds.width, bounds.height, arc);
    }

    /**
     * Preenche um retângulo arredondado com a cor informada.
     */
    public static void fillRoundRect(Graphics2D g2, Rectangle bounds, float arc, Color color) {
        if (color == null) {
            return;
        }
        g2.setColor(color);
        g2.fill(roundRect(bounds, arc));
    }

    /**
     * Desenha o contorno de um retângulo arredondado respeitando a espessura do traço.
     */
    public static void drawRoundRect(Graphics2D g2, Rectangle bounds, float arc, Color color, float strokeWidth) {
        if (color == null || strokeWidth <= 0f) {
            return;
        }
        float inset = strokeWidth / 2f;
        Stroke previous = g2.getStroke();
        g2.setColor(color);
        g2.setStroke(new BasicStroke(strokeWidth));
        g2.draw(roundRect(
                bounds.x + inset,
                bounds.y + inset,
                bounds.width - strokeWidth,
                bounds.height - strokeWidth,
                arc));
        g2.setStroke(previous);
    }

    /**
     * Desenha o anel de foco ao redor dos limites informados.
     */
    public static void focusRing(Graphics2D g2, Rectangle bounds, float arc, Color color, float strokeWidth, int gap) {
        if (color == null || strokeWidth <= 0f) {
            return;
        }
        Rectangle ring = new Rectangle(
                bounds.x - gap,
                bounds.y - gap,
                bounds.width + gap * 2,
                bounds.height + gap * 2);
        drawRoundRect(g2, ring, arc + gap, color, strokeWidth);
    }

    /**
     * Pinta uma sombra suave por trás da forma informada.
     */
    public static void softShadow(Graphics2D g2, Shape shape, Color color, int spread, int offsetY) {
        if (shape == null || spread <= 0) {
            return;
        }
        Color base = color != null ? color : Color.BLACK;
        Stroke previous = g2.getStroke();
        for (int i = spread; i >= 1; i--) {
            int alpha = Math.max(2, (base.getAlpha() > 0 ? base.getAlpha() : 40) / (i * 2));
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha));
            g2.setStroke(new BasicStroke(i * 2f));
            g2.translate(0, offsetY);
            g2.draw(shape);
            g2.translate(0, -offsetY);
        }
        g2.setStroke(previous);
    }

    /**
     * Reduz o texto com reticências até caber na largura disponível.
     */
    public static String fitText(FontMetrics metrics, String text, int maxWidth) {
        if (text == null || text.isEmpty() || maxWidth <= 0) {
            return "";
        }
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        int ellipsisWidth = metrics.stringWidth(ELLIPSIS);
        if (ellipsisWidth > maxWidth) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int width = ellipsisWidth;
        for (int i = 0; i < text.length(); i++) {
            int charWidth = metrics.charWidth(text.charAt(i));
            if (width + charWidth > maxWidth) {
                break;
            }
            builder.append(text.charAt(i));
            width += charWidth;
        }
        return builder.append(ELLIPSIS).toString();
    }

    /**
     * Calcula a linha de base que centraliza o texto verticalmente na altura informada.
     */
    public static int centeredBaseline(FontMetrics metrics, int y, int height) {
        return y + (height - metrics.getHeight()) / 2 + metrics.getAscent();
    }

    /**
     * Desenha o texto centralizado vertical e horizontalmente nos limites informados.
     */
    public static void drawCenteredText(Graphics2D g2, String text, Rectangle bounds, Color color) {
        if (text == null || text.isEmpty() || color == null) {
            return;
        }
        FontMetrics metrics = g2.getFontMetrics();
        String fitted = fitText(metrics, text, bounds.width);
        int x = bounds.x + (bounds.width - metrics.stringWidth(fitted)) / 2;
        g2.setColor(color);
        g2.drawString(fitted, x, centeredBaseline(metrics, bounds.y, bounds.height));
    }

    /**
     * Desenha o texto alinhado à esquerda e centralizado verticalmente.
     */
    public static void drawLeftText(Graphics2D g2, String text, Rectangle bounds, Color color) {
        if (text == null || text.isEmpty() || color == null) {
            return;
        }
        FontMetrics metrics = g2.getFontMetrics();
        g2.setColor(color);
        g2.drawString(fitText(metrics, text, bounds.width), bounds.x, centeredBaseline(metrics, bounds.y, bounds.height));
    }

    /**
     * Desenha o texto de placeholder em itálico na posição informada.
     */
    public static void drawPlaceholder(Graphics2D g2, String text, Rectangle bounds, Color color, Font baseFont) {
        if (text == null || text.isEmpty() || color == null) {
            return;
        }
        Font previous = g2.getFont();
        g2.setFont(baseFont.deriveFont(Font.ITALIC));
        drawLeftText(g2, text, bounds, color);
        g2.setFont(previous);
    }

    /**
     * Interpola linearmente entre duas cores.
     */
    public static Color blend(Color from, Color to, float amount) {
        return ColorUtils.mix(from, to, ColorUtils.clamp01(amount));
    }

    /**
     * Aplica aceleração e desaceleração suaves ao progresso normalizado.
     */
    public static float easeInOut(float progress) {
        float clamped = ColorUtils.clamp01(progress);
        return clamped < 0.5f
                ? 2f * clamped * clamped
                : 1f - (float) Math.pow(-2f * clamped + 2f, 2) / 2f;
    }

    /**
     * Aplica desaceleração suave ao progresso normalizado.
     */
    public static float easeOut(float progress) {
        float clamped = ColorUtils.clamp01(progress);
        return 1f - (1f - clamped) * (1f - clamped);
    }
}
