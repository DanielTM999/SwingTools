package dtm.stools.component.panels.editor.pdf.backend;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

final class PdfBoxGlyph {
    final int op;
    final int element;
    final byte[] bytes;
    final String unicode;
    final Rectangle2D box;
    final Point2D center;
    final Point2D origin;
    final Point2D end;
    final double advance;
    final double fontSize;
    final double horizontalScaling;
    final AffineTransform matrix;
    final PDFont font;
    final float[] fill;
    final float[] stroke;
    final RenderingMode renderingMode;
    final boolean editable;

    PdfBoxGlyph(int op, int element, byte[] bytes, String unicode, Rectangle2D box, Point2D center, Point2D origin, Point2D end, double advance,
                double fontSize, double horizontalScaling, AffineTransform matrix, PDFont font,
                float[] fill, float[] stroke, RenderingMode renderingMode, boolean editable) {
        this.op = op;
        this.element = element;
        this.bytes = bytes;
        this.unicode = unicode;
        this.box = box;
        this.center = center;
        this.origin = origin;
        this.end = end;
        this.advance = advance;
        this.fontSize = fontSize;
        this.horizontalScaling = horizontalScaling;
        this.matrix = matrix;
        this.font = font;
        this.fill = fill;
        this.stroke = stroke;
        this.renderingMode = renderingMode;
        this.editable = editable;
    }

    boolean whitespace() { return unicode == null || unicode.isBlank(); }
    double height() { return Math.max(1, Math.hypot(matrix.getShearX(), matrix.getScaleY())); }
    double kerning() {
        double scale = fontSize * horizontalScaling;
        return scale == 0 ? Double.NaN : -advance * 1000 / scale;
    }
}
