package dtm.stools.component.panels.editor.pdf.backend;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

final class PdfBoxItem {
    final PdfBoxItemKind kind;
    final int first;
    final int last;
    final Rectangle2D bounds;
    final AffineTransform ctm;
    final boolean clip;
    final COSName name;
    final PDImage image;
    final float[] fill;
    final float[] stroke;
    final float lineWidth;
    final int lineCap;
    final int lineJoin;
    final float[] dash;
    final int dashPhase;

    PdfBoxItem(PdfBoxItemKind kind, int first, int last, Rectangle2D bounds, AffineTransform ctm, boolean clip,
               COSName name, PDImage image, float[] fill, float[] stroke, float lineWidth, int lineCap,
               int lineJoin, float[] dash, int dashPhase) {
        this.kind = kind;
        this.first = first;
        this.last = last;
        this.bounds = bounds;
        this.ctm = ctm;
        this.clip = clip;
        this.name = name;
        this.image = image;
        this.fill = fill;
        this.stroke = stroke;
        this.lineWidth = lineWidth;
        this.lineCap = lineCap;
        this.lineJoin = lineJoin;
        this.dash = dash;
        this.dashPhase = dashPhase;
    }

    boolean image() { return kind == PdfBoxItemKind.IMAGE || kind == PdfBoxItemKind.INLINE_IMAGE; }
}
