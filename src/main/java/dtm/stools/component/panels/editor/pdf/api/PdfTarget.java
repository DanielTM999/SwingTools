package dtm.stools.component.panels.editor.pdf.api;

import java.awt.geom.Rectangle2D;
import java.util.List;

public record PdfTarget(List<String> ids, Rectangle2D.Float area) {
    public PdfTarget {
        ids = List.copyOf(ids);
        area = area == null ? null : (Rectangle2D.Float) area.clone();
    }
    public static PdfTarget of(String... ids) { return new PdfTarget(List.of(ids), null); }
    public static PdfTarget of(List<String> ids) { return new PdfTarget(ids, null); }
    public static PdfTarget area(Rectangle2D.Float area) { return new PdfTarget(List.of(), area); }
    @Override public Rectangle2D.Float area() { return area == null ? null : (Rectangle2D.Float) area.clone(); }
    public boolean isEmpty() { return ids.isEmpty() && (area == null || area.isEmpty()); }
}
