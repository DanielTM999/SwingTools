package dtm.stools.component.panels.editor.pdf.element;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeKind;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

public final class ShapeElementFactory extends BasePdfElementFactory {
    private final PdfShapeKind kind;

    public ShapeElementFactory(PdfShapeKind kind) {
        super("pdf.factory." + kind.name().toLowerCase(java.util.Locale.ROOT), commandId(kind), title(kind),
                icon(kind), "Arraste na página para desenhar: " + title(kind).toLowerCase(java.util.Locale.ROOT),
                PdfPlacementMode.DRAG_RECT);
        this.kind = kind;
    }

    public PdfShapeKind kind() { return kind; }

    @Override public void insert(PdfEditor editor, PdfPlacement placement) throws IOException {
        Rectangle2D.Float area = placement.bounds();
        Point2D.Float start = placement.point();
        if (kind == PdfShapeKind.LINE || kind == PdfShapeKind.ARROW) {
            float x2, y2;
            if (placement.dragged() || area.width >= 4 || area.height >= 4) {
                x2 = Math.abs(start.x - area.x) < .01 ? area.x + area.width : area.x;
                y2 = Math.abs(start.y - area.y) < .01 ? area.y + area.height : area.y;
            } else { x2 = start.x + 120; y2 = start.y; }
            editor.addLine(placement.page(), start.x, start.y, x2, y2, kind == PdfShapeKind.ARROW);
            return;
        }
        if (!placement.dragged()) area = new Rectangle2D.Float(start.x, start.y - 70, 120, 70);
        editor.addShape(placement.page(), kind, area);
    }

    private static String commandId(PdfShapeKind kind) {
        return switch (kind) {
            case RECTANGLE -> "pdf.square";
            case ELLIPSE -> "pdf.ellipse";
            case LINE -> "pdf.line";
            case ARROW -> "pdf.arrow";
        };
    }
    private static String title(PdfShapeKind kind) {
        return switch (kind) {
            case RECTANGLE -> "Retângulo";
            case ELLIPSE -> "Elipse";
            case LINE -> "Linha";
            case ARROW -> "Seta";
        };
    }
    private static String icon(PdfShapeKind kind) {
        return switch (kind) {
            case RECTANGLE -> "rectangle";
            case ELLIPSE -> "ellipse";
            case LINE -> "line";
            case ARROW -> "arrow";
        };
    }
}
