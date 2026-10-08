package dtm.stools.component.panels.editor.pdf.ui;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public record PdfPageGeometry(float x, float y, float width, float height, int rotation, double scale) {
    public PdfPageGeometry {
        if (width <= 0 || height <= 0 || scale <= 0 || rotation % 90 != 0)
            throw new IllegalArgumentException("Geometria de página inválida");
        rotation = Math.floorMod(rotation, 360);
    }
    public double viewWidth() { return (rotation % 180 == 0 ? width : height) * scale; }
    public double viewHeight() { return (rotation % 180 == 0 ? height : width) * scale; }
    public double aspect() { return viewHeight() / viewWidth(); }
    public PdfPageGeometry withScale(double value) { return new PdfPageGeometry(x, y, width, height, rotation, value); }
    public Point2D.Double toView(double pdfX, double pdfY) {
        double px = pdfX - x, py = pdfY - y;
        return switch (rotation) {
            case 90 -> new Point2D.Double(py * scale, px * scale);
            case 180 -> new Point2D.Double((width - px) * scale, py * scale);
            case 270 -> new Point2D.Double((height - py) * scale, (width - px) * scale);
            default -> new Point2D.Double(px * scale, (height - py) * scale);
        };
    }
    public Rectangle2D.Double toView(Rectangle2D pdf) {
        Point2D.Double a = toView(pdf.getMinX(), pdf.getMinY());
        Point2D.Double b = toView(pdf.getMaxX(), pdf.getMaxY());
        return new Rectangle2D.Double(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }
    public Point2D.Float toPdf(double viewX, double viewY) {
        double vx = viewX / scale, vy = viewY / scale;
        double px, py;
        switch (rotation) {
            case 90 -> { px = vy; py = vx; }
            case 180 -> { px = width - vx; py = vy; }
            case 270 -> { px = width - vy; py = height - vx; }
            default -> { px = vx; py = height - vy; }
        }
        return new Point2D.Float((float) (x + px), (float) (y + py));
    }
    public Rectangle2D.Float toPdf(Rectangle2D view) {
        Point2D.Float a = toPdf(view.getMinX(), view.getMinY());
        Point2D.Float b = toPdf(view.getMaxX(), view.getMaxY());
        return new Rectangle2D.Float(Math.min(a.x, b.x), Math.min(a.y, b.y), Math.abs(a.x - b.x), Math.abs(a.y - b.y));
    }
    public Point2D.Float clamp(Point2D.Float point) {
        return new Point2D.Float(Math.max(x, Math.min(x + width, point.x)), Math.max(y, Math.min(y + height, point.y)));
    }
}
