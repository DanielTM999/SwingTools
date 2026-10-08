package dtm.stools.component.panels.editor.pdf.ui;

import java.awt.Cursor;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public enum PdfHandle {
    NORTH_WEST(0, 0, Cursor.NW_RESIZE_CURSOR), NORTH(.5, 0, Cursor.N_RESIZE_CURSOR), NORTH_EAST(1, 0, Cursor.NE_RESIZE_CURSOR),
    EAST(1, .5, Cursor.E_RESIZE_CURSOR), SOUTH_EAST(1, 1, Cursor.SE_RESIZE_CURSOR), SOUTH(.5, 1, Cursor.S_RESIZE_CURSOR),
    SOUTH_WEST(0, 1, Cursor.SW_RESIZE_CURSOR), WEST(0, .5, Cursor.W_RESIZE_CURSOR), ROTATE(.5, -1, Cursor.HAND_CURSOR);

    public static final int SIZE = 8, ROTATE_DISTANCE = 22;

    private final double fx, fy;
    private final int cursor;

    PdfHandle(double fx, double fy, int cursor) {
        this.fx = fx;
        this.fy = fy;
        this.cursor = cursor;
    }

    public int cursor() { return cursor; }
    public boolean corner() { return fx != .5 && fy != .5 && this != ROTATE; }

    public Point2D.Double point(Rectangle2D bounds) {
        if (this == ROTATE) return new Point2D.Double(bounds.getCenterX(), bounds.getMinY() - ROTATE_DISTANCE);
        return new Point2D.Double(bounds.getMinX() + bounds.getWidth() * fx, bounds.getMinY() + bounds.getHeight() * fy);
    }

    public Rectangle2D.Double box(Rectangle2D bounds) {
        Point2D.Double point = point(bounds);
        return new Rectangle2D.Double(point.x - SIZE / 2.0, point.y - SIZE / 2.0, SIZE, SIZE);
    }

    public Rectangle2D.Double resize(Rectangle2D start, double dx, double dy, boolean keepAspect) {
        double x1 = start.getMinX(), y1 = start.getMinY(), x2 = start.getMaxX(), y2 = start.getMaxY();
        if (fx == 0) x1 += dx;
        if (fx == 1) x2 += dx;
        if (fy == 0) y1 += dy;
        if (fy == 1) y2 += dy;
        double width = Math.max(4, x2 - x1), height = Math.max(4, y2 - y1);
        if (keepAspect && corner() && start.getWidth() > 0 && start.getHeight() > 0) {
            double ratio = start.getHeight() / start.getWidth();
            if (Math.abs(width - start.getWidth()) * ratio > Math.abs(height - start.getHeight())) height = width * ratio;
            else width = height / ratio;
        }
        double x = fx == 0 ? start.getMaxX() - width : start.getMinX();
        double y = fy == 0 ? start.getMaxY() - height : start.getMinY();
        if (fx == .5) { x = start.getMinX(); width = start.getWidth(); }
        if (fy == .5) { y = start.getMinY(); height = start.getHeight(); }
        return new Rectangle2D.Double(x, y, width, height);
    }
}
