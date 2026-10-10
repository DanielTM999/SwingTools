package dtm.stools.component.panels.editor.pdf.api;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public record PdfSelection(int page, List<PdfPageElement> elements, Rectangle2D.Float area) {
    private static final PdfSelection EMPTY = new PdfSelection(-1, List.of(), null);
    public PdfSelection {
        elements = List.copyOf(elements);
        area = area == null ? null : (Rectangle2D.Float) area.clone();
    }
    public static PdfSelection empty() { return EMPTY; }
    public static PdfSelection of(int page, List<PdfPageElement> elements) { return new PdfSelection(page, elements, null); }
    public static PdfSelection area(int page, List<PdfPageElement> inside, Rectangle2D.Float area) { return new PdfSelection(page, inside, area); }
    @Override
    public Rectangle2D.Float area() { return area == null ? null : (Rectangle2D.Float) area.clone(); }
    public boolean isEmpty() { return page < 0 || (elements.isEmpty() && !hasArea()); }
    public boolean hasArea() { return area != null && !area.isEmpty(); }
    public boolean contains(String id) { return elements.stream().anyMatch(element -> element.id().equals(id)); }
    public boolean onlyAnnotations() { return !hasArea() && !elements.isEmpty() && elements.stream().allMatch(PdfPageElement::annotation); }
    public Rectangle2D.Float bounds() {
        Rectangle2D.Float result = hasArea() ? area() : null;
        for (PdfPageElement element : elements) {
            if (result == null) result = element.bounds();
            else Rectangle2D.union(result, element.bounds(), result);
        }
        return result;
    }
    public PdfTarget target() {
        List<String> ids = new ArrayList<>();
        for (PdfPageElement element : elements) ids.add(element.id());
        return new PdfTarget(ids, area);
    }
    public String text() {
        StringBuilder builder = new StringBuilder();
        PdfPageElement previous = null;
        for (PdfPageElement element : elements) {
            if (element.text().isBlank()) continue;
            if (previous != null) {
                Rectangle2D.Float a = previous.bounds(), b = element.bounds();
                builder.append(Math.abs(a.getCenterY() - b.getCenterY()) > Math.max(a.height, b.height) * .6 ? "\n" : " ");
            }
            builder.append(element.text());
            previous = element;
        }
        return builder.toString();
    }
}
