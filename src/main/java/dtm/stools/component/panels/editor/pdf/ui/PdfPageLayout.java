package dtm.stools.component.panels.editor.pdf.ui;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

public final class PdfPageLayout {
    public static final int MARGIN = 28, GAP = 20;
    private static final PdfPageLayout EMPTY = new PdfPageLayout(new PdfPageGeometry[0], new Rectangle[0], new Dimension(0, 0));

    private final PdfPageGeometry[] geometries;
    private final Rectangle[] bounds;
    private final Dimension size;

    private PdfPageLayout(PdfPageGeometry[] geometries, Rectangle[] bounds, Dimension size) {
        this.geometries = geometries;
        this.bounds = bounds;
        this.size = size;
    }

    public static PdfPageLayout empty() { return EMPTY; }

    public static PdfPageLayout compute(PdfPageGeometry[] pages, boolean continuous, int current, int viewportWidth) {
        Rectangle[] bounds = new Rectangle[pages.length];
        int maxWidth = 0;
        for (int page = 0; page < pages.length; page++)
            if (visible(page, continuous, current)) maxWidth = Math.max(maxWidth, (int) Math.ceil(pages[page].viewWidth()));
        int width = Math.max(viewportWidth, maxWidth + 2 * MARGIN);
        int y = MARGIN;
        for (int page = 0; page < pages.length; page++) {
            if (!visible(page, continuous, current)) continue;
            int w = (int) Math.ceil(pages[page].viewWidth()), h = (int) Math.ceil(pages[page].viewHeight());
            bounds[page] = new Rectangle((width - w) / 2, y, w, h);
            y += h + GAP;
        }
        int height = pages.length == 0 ? 2 * MARGIN : y - GAP + MARGIN;
        return new PdfPageLayout(pages.clone(), bounds, new Dimension(width, height));
    }

    private static boolean visible(int page, boolean continuous, int current) { return continuous || page == current; }

    public Dimension size() { return new Dimension(size); }
    public int pageCount() { return geometries.length; }
    public boolean contains(int page) { return page >= 0 && page < bounds.length && bounds[page] != null; }
    public Rectangle bounds(int page) { return contains(page) ? new Rectangle(bounds[page]) : null; }
    public PdfPageGeometry geometry(int page) { return page >= 0 && page < geometries.length ? geometries[page] : null; }

    public int pageAt(Point point) {
        for (int page = 0; page < bounds.length; page++) if (bounds[page] != null && bounds[page].contains(point)) return page;
        return -1;
    }

    public int nearestPage(Point point) {
        int best = -1;
        double distance = Double.MAX_VALUE;
        for (int page = 0; page < bounds.length; page++) {
            Rectangle box = bounds[page];
            if (box == null) continue;
            double dy = point.y < box.y ? box.y - point.y : point.y > box.getMaxY() ? point.y - box.getMaxY() : 0;
            double dx = point.x < box.x ? box.x - point.x : point.x > box.getMaxX() ? point.x - box.getMaxX() : 0;
            double value = dx + dy * 4;
            if (value < distance) { distance = value; best = page; }
        }
        return best;
    }

    public int pageAtViewportCenter(Rectangle viewport) {
        int best = -1;
        long overlap = -1;
        for (int page = 0; page < bounds.length; page++) {
            if (bounds[page] == null) continue;
            Rectangle visible = bounds[page].intersection(viewport);
            long area = visible.isEmpty() ? 0 : (long) visible.width * visible.height;
            if (area > overlap) { overlap = area; best = page; }
        }
        return best;
    }

    public Point2D.Float toPdf(int page, Point2D view) {
        Rectangle box = bounds(page);
        return geometries[page].toPdf(view.getX() - box.x, view.getY() - box.y);
    }

    public Point2D.Float toPdfClamped(int page, Point2D view) {
        return geometries[page].clamp(toPdf(page, view));
    }

    public Rectangle2D.Float toPdf(int page, Rectangle2D view) {
        Rectangle box = bounds(page);
        return geometries[page].toPdf(new Rectangle2D.Double(view.getX() - box.x, view.getY() - box.y, view.getWidth(), view.getHeight()));
    }

    public Rectangle2D.Double toView(int page, Rectangle2D pdf) {
        Rectangle box = bounds(page);
        Rectangle2D.Double view = geometries[page].toView(pdf);
        view.x += box.x;
        view.y += box.y;
        return view;
    }

    public Point2D.Double toView(int page, double pdfX, double pdfY) {
        Rectangle box = bounds(page);
        Point2D.Double point = geometries[page].toView(pdfX, pdfY);
        point.x += box.x;
        point.y += box.y;
        return point;
    }

    public boolean sameShape(PdfPageLayout other) {
        return other != null && size.equals(other.size) && Arrays.equals(bounds, other.bounds);
    }
}
