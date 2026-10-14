package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.inputfields.textarea.diagnostic.DiagnosticSeverity;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.LayeredHighlighter;
import javax.swing.text.Position;
import javax.swing.text.View;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;

final class TextAreaDiagnosticPainter extends LayeredHighlighter.LayerPainter {

    private final DiagnosticSeverity severity;

    TextAreaDiagnosticPainter(DiagnosticSeverity severity) {
        this.severity = severity;
    }

    static Color colorOf(DiagnosticSeverity severity) {
        return switch (severity) {
            case ERROR -> UiTokens.danger();
            case WARNING -> UiTokens.warning();
            case INFO -> UiTokens.info();
            case HINT -> UiTokens.muted();
        };
    }

    @Override
    public void paint(Graphics g, int p0, int p1, Shape bounds, JTextComponent component) {
    }

    @Override
    public Shape paintLayer(Graphics g, int offs0, int offs1, Shape bounds, JTextComponent component, View view) {
        int end = offs1;
        if (end == view.getEndOffset() && end > offs0) end--;
        if (end <= offs0) return null;
        Rectangle area;
        try {
            Shape shape = view.modelToView(offs0, Position.Bias.Forward, end, Position.Bias.Backward, bounds);
            area = shape instanceof Rectangle rectangle ? rectangle : shape.getBounds();
        } catch (BadLocationException ex) {
            return null;
        }
        if (area.width <= 0) return area;
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            g2.setColor(colorOf(severity));
            int base = area.y + area.height - Math.max(1, UiTokens.scale(1));
            if (severity == DiagnosticSeverity.HINT) paintDots(g2, area.x, area.x + area.width, base);
            else paintWave(g2, area.x, area.x + area.width, base);
        } finally {
            g2.dispose();
        }
        return area;
    }

    private static void paintWave(Graphics2D g2, int from, int to, int base) {
        float step = Math.max(2f, UiTokens.scale(2));
        float amplitude = Math.max(1.5f, UiTokens.scale(2) * 0.9f);
        g2.setStroke(new BasicStroke(Math.max(1f, UiTokens.stroke())));
        Path2D.Float wave = new Path2D.Float();
        wave.moveTo(from, base);
        boolean up = true;
        for (float x = from + step; x < to + step; x += step) {
            wave.lineTo(Math.min(x, to), up ? base - amplitude : base);
            up = !up;
        }
        g2.draw(wave);
    }

    private static void paintDots(Graphics2D g2, int from, int to, int base) {
        int size = Math.max(1, UiTokens.scale(1));
        int gap = size * 3;
        for (int x = from; x < to; x += gap) g2.fillRect(x, base - size, size, size);
    }
}
