package dtm.stools.component.panels.editor.code;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorFontRenderContextTest {

    @Test
    void measuresPaintedRunsWithTheCurrentGraphicsContext() {
        TrackingTextArea area = new TrackingTextArea("private String value = otherValue;");
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setSize(500, 120);
        area.fontMetricsFor(area.getFont());

        BufferedImage image = new BufferedImage(700, 180, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(1.25, 1.25);
        graphics.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontRenderContext expectedContext = graphics.getFontRenderContext();

        area.paint(graphics);
        graphics.dispose();

        assertFalse(area.paintContexts.isEmpty());
        assertTrue(area.paintContexts.stream().allMatch(expectedContext::equals));
    }

    @Test
    void rebuildsGeometryWhenThePaintContextScaleChanges() {
        TrackingTextArea area = new TrackingTextArea("first line\nsecond line");
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        area.setSize(500, 120);

        paintAtScale(area, 1.0);
        int rebuildsAtInitialScale = area.geometryRebuilds;
        paintAtScale(area, 1.5);

        assertTrue(area.geometryRebuilds > rebuildsAtInitialScale);
    }

    private static void paintAtScale(CodeEditorTextArea area, double scale) {
        BufferedImage image = new BufferedImage(800, 240, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.scale(scale, scale);
        area.paint(graphics);
        graphics.dispose();
    }

    private static final class TrackingTextArea extends CodeEditorTextArea {
        private final List<FontRenderContext> paintContexts = new ArrayList<>();
        private boolean painting;
        private int geometryRebuilds;

        private TrackingTextArea(String text) {
            super(text);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            painting = true;
            try {
                super.paintComponent(graphics);
            } finally {
                painting = false;
            }
        }

        @Override
        protected FontMetrics fontMetricsFor(Font font) {
            FontMetrics metrics = super.fontMetricsFor(font);
            if (painting) paintContexts.add(metrics.getFontRenderContext());
            return metrics;
        }

        @Override
        protected void rebuildGeometryCache() {
            geometryRebuilds++;
            super.rebuildGeometryCache();
        }
    }
}
