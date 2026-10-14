package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.inputfields.textarea.diagnostic.DiagnosticSeverity;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.JComponent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.util.Map;

final class TextAreaLineGutter extends JComponent {

    private static final int MIN_DIGITS = 2;

    private final SmartTextArea area;
    private int digits = MIN_DIGITS;

    private final DocumentListener documentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            documentChanged();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            documentChanged();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
        }
    };

    private final CaretListener caretListener = e -> repaint();

    private final ComponentListener resizeListener = new ComponentAdapter() {
        @Override
        public void componentResized(ComponentEvent e) {
            revalidate();
            repaint();
        }
    };

    private final PropertyChangeListener propertyListener = e -> {
        switch (e.getPropertyName()) {
            case "document" -> {
                if (e.getOldValue() instanceof Document previous) previous.removeDocumentListener(documentListener);
                if (e.getNewValue() instanceof Document next) next.addDocumentListener(documentListener);
                documentChanged();
            }
            case "font", "highlightCurrentLine", "currentLineColor", "background", "foreground", "diagnostics" -> {
                revalidate();
                repaint();
            }
            default -> {
            }
        }
    };

    TextAreaLineGutter(SmartTextArea area) {
        this.area = area;
        setOpaque(true);
        digits = digitsFor(area.getLineCount());
    }

    @Override
    public void addNotify() {
        super.addNotify();
        area.getDocument().addDocumentListener(documentListener);
        area.addCaretListener(caretListener);
        area.addComponentListener(resizeListener);
        area.addPropertyChangeListener(propertyListener);
        documentChanged();
    }

    @Override
    public void removeNotify() {
        area.getDocument().removeDocumentListener(documentListener);
        area.removeCaretListener(caretListener);
        area.removeComponentListener(resizeListener);
        area.removePropertyChangeListener(propertyListener);
        super.removeNotify();
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics metrics = getFontMetrics(numberFont());
        int width = padding() * 2 + digitsFor(area.getLineCount()) * metrics.charWidth('0') + 1;
        int height = Math.max(area.getHeight(), area.getPreferredSize().height);
        return new Dimension(width, height);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            Rectangle clip = g2.getClipBounds();
            if (clip == null) clip = new Rectangle(0, 0, getWidth(), getHeight());
            Color background = area.getBackground() != null ? area.getBackground() : UiTokens.surface();
            g2.setColor(background);
            g2.fillRect(clip.x, clip.y, clip.width, clip.height);

            Rectangle current = area.isHighlightCurrentLine() ? area.currentLineBounds() : null;
            if (current != null) {
                g2.setColor(area.resolveCurrentLineColor());
                g2.fillRect(0, current.y, getWidth(), current.height);
            }

            g2.setColor(UiTokens.border());
            g2.fillRect(getWidth() - 1, clip.y, 1, clip.height);

            paintNumbers(g2, clip);
        } finally {
            g2.dispose();
        }
    }

    private void paintNumbers(Graphics2D g2, Rectangle clip) {
        Font font = numberFont();
        g2.setFont(font);
        FontMetrics metrics = g2.getFontMetrics();
        int firstLine = area.getLineOfOffsetSafe(area.viewToModel2D(new Point(0, clip.y)));
        int lastLine = area.getLineOfOffsetSafe(area.viewToModel2D(new Point(0, clip.y + clip.height)));
        int caretLine = area.getLineOfOffsetSafe(area.getCaretPosition());
        Map<Integer, DiagnosticSeverity> marks = area.diagnosticSeverityByLine();
        int markSize = Math.max(4, UiTokens.scale(6));
        Color muted = UiTokens.muted();
        Color strong = area.getForeground() != null ? area.getForeground() : UiTokens.foreground();
        int right = getWidth() - padding();
        for (int line = Math.max(0, firstLine); line <= lastLine; line++) {
            Rectangle2D row;
            try {
                row = area.modelToView2D(area.getLineStartOffset(line));
            } catch (BadLocationException ex) {
                break;
            }
            if (row == null) continue;
            String number = String.valueOf(line + 1);
            int baseline = (int) Math.round(row.getY() + (row.getHeight() - metrics.getHeight()) / 2d) + metrics.getAscent();
            DiagnosticSeverity mark = marks.get(line);
            if (mark != null && mark != DiagnosticSeverity.HINT) {
                g2.setColor(TextAreaDiagnosticPainter.colorOf(mark));
                int top = (int) Math.round(row.getY() + (row.getHeight() - markSize) / 2d);
                g2.fillOval(Math.max(1, UiTokens.scale(1)), top, markSize, markSize);
            }
            g2.setColor(line == caretLine ? strong : muted);
            g2.drawString(number, right - metrics.stringWidth(number), baseline);
        }
    }

    private void documentChanged() {
        int next = digitsFor(area.getLineCount());
        if (next != digits) {
            digits = next;
            revalidate();
        }
        repaint();
    }

    private Font numberFont() {
        Font font = area.getFont();
        return font != null ? font : UiTokens.fontMono();
    }

    private int padding() {
        return UiTokens.scale(8);
    }

    private static int digitsFor(int lineCount) {
        return Math.max(MIN_DIGITS, String.valueOf(Math.max(1, lineCount)).length());
    }
}
