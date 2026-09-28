package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticSeverity;
import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorUnnecessaryCodeTest {

    @Test
    void unnecessarySpansCoverOnlyTaggedRanges() {
        CodeEditorTextArea area = new CodeEditorTextArea("keep drop\nnext");
        area.diagnostics.add(new Diagnostic(0, 5, 0, 9, DiagnosticSeverity.HINT, "unused", null, null, true));
        area.diagnostics.add(new Diagnostic(1, 0, 1, 4, DiagnosticSeverity.WARNING, "plain"));

        Map<Integer, List<int[]>> spans = area.unnecessarySpans(0, 1);

        assertEquals(1, spans.size());
        assertArrayEquals(new int[]{5, 9}, spans.get(0).getFirst());
    }

    @Test
    void fadeOnlyDiagnosticsAreHintsTaggedAsUnnecessary() {
        Diagnostic hint = new Diagnostic(0, 0, 0, 1, DiagnosticSeverity.HINT, "x");
        Diagnostic warning = new Diagnostic(0, 0, 0, 1, DiagnosticSeverity.WARNING, "x");

        assertFalse(hint.isFadeOnly());
        assertTrue(hint.withUnnecessary(true).isFadeOnly());
        assertFalse(warning.withUnnecessary(true).isFadeOnly());
    }

    @Test
    void paintsUnnecessaryCodeWithLessContrast() {
        CodeEditorTextArea area = new CodeEditorTextArea("MMMM MMMM");
        area.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        area.setSize(500, 120);
        area.diagnostics.add(new Diagnostic(0, 5, 0, 9, DiagnosticSeverity.HINT, "unused", null, null, true));
        BufferedImage image = new BufferedImage(500, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        area.paint(graphics);

        graphics.dispose();
        FontMetrics fm = area.getFontMetrics(area.getFont());
        int charWidth = fm.charWidth('M');
        int left = CodeEditorTextAreaGeometry.TEXT_LEFT_MARGIN;
        int background = image.getRGB(480, fm.getHeight() / 2);
        int normal = maxContrast(image, left, left + 4 * charWidth, fm.getHeight(), background);
        int faded = maxContrast(image, left + 5 * charWidth, left + 9 * charWidth, fm.getHeight(), background);

        assertTrue(faded > 0, "o trecho esmaecido ainda precisa aparecer");
        assertTrue(faded < normal, "faded=" + faded + " normal=" + normal);
    }

    @Test
    void fadeIsConfigurableAndClamped() {
        CodeEditorTextArea area = new CodeEditorTextArea("x");

        assertEquals(0.3f, area.getUnnecessaryCodeFade());
        area.setUnnecessaryCodeFade(0.8f);
        assertEquals(0.8f, area.getUnnecessaryCodeFade());
        area.setUnnecessaryCodeFade(3f);
        assertEquals(1f, area.getUnnecessaryCodeFade());
        area.setUnnecessaryCodeFade(-1f);
        assertEquals(0f, area.getUnnecessaryCodeFade());
    }

    @Test
    void strongerFadeLowersTheContrastAndZeroDisablesIt() {
        assertTrue(fadedContrast(0.8f) < fadedContrast(0.3f));
        assertEquals(normalContrast(), fadedContrast(0f));
    }

    private static int fadedContrast(float fade) {
        return contrastOfColumns(fade, 5, 9);
    }

    private static int normalContrast() {
        return contrastOfColumns(0.5f, 0, 4);
    }

    private static int contrastOfColumns(float fade, int fromCol, int toCol) {
        CodeEditorTextArea area = new CodeEditorTextArea("MMMM MMMM");
        area.setFont(new Font(Font.MONOSPACED, Font.BOLD, 16));
        area.setSize(500, 120);
        area.setUnnecessaryCodeFade(fade);
        area.diagnostics.add(new Diagnostic(0, 5, 0, 9, DiagnosticSeverity.HINT, "unused", null, null, true));
        BufferedImage image = new BufferedImage(500, 120, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        area.paint(graphics);
        graphics.dispose();
        FontMetrics fm = area.getFontMetrics(area.getFont());
        int charWidth = fm.charWidth('M');
        int left = CodeEditorTextAreaGeometry.TEXT_LEFT_MARGIN;
        int background = image.getRGB(480, fm.getHeight() / 2);
        return maxContrast(image, left + fromCol * charWidth, left + toCol * charWidth, fm.getHeight(), background);
    }

    private static int maxContrast(BufferedImage image, int fromX, int toX, int height, int background) {
        int best = 0;
        for (int x = fromX; x < toX; x++) {
            for (int y = 0; y < height; y++) {
                best = Math.max(best, distance(image.getRGB(x, y), background));
            }
        }
        return best;
    }

    private static int distance(int a, int b) {
        return Math.abs(((a >> 16) & 0xFF) - ((b >> 16) & 0xFF))
                + Math.abs(((a >> 8) & 0xFF) - ((b >> 8) & 0xFF))
                + Math.abs((a & 0xFF) - (b & 0xFF));
    }
}
