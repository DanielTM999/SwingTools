package dtm.stools.component.panels.editor.pdf.api;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public record PdfPlacement(int page, Point2D.Float point, Rectangle2D.Float bounds, float[] stroke) {
    public PdfPlacement(int page, Point2D.Float point, Rectangle2D.Float bounds) { this(page, point, bounds, new float[0]); }
    public PdfPlacement {
        point = (Point2D.Float) point.clone();
        bounds = bounds == null ? new Rectangle2D.Float(point.x, point.y, 0, 0) : (Rectangle2D.Float) bounds.clone();
        stroke = stroke == null ? new float[0] : stroke.clone();
    }
    public boolean dragged() { return bounds.width >= 4 && bounds.height >= 4; }
    @Override
    public Point2D.Float point() { return (Point2D.Float) point.clone(); }
    @Override
    public Rectangle2D.Float bounds() { return (Rectangle2D.Float) bounds.clone(); }
    @Override
    public float[] stroke() { return stroke.clone(); }
}
