package dtm.stools.component.panels.editor.pdf.api;

import java.awt.geom.Rectangle2D;

public record PdfPageElement(String id, String type, Rectangle2D.Float bounds, int layer,
                             boolean direct, String text) {
    public PdfPageElement(String id, String type, Rectangle2D.Float bounds, int layer) {
        this(id, type, bounds, layer, true, "");
    }
    public PdfPageElement {
        bounds = (Rectangle2D.Float) bounds.clone();
        text = text == null ? "" : text;
    }
    @Override public Rectangle2D.Float bounds() { return (Rectangle2D.Float) bounds.clone(); }
    public boolean annotation() { return id.startsWith("annotation:"); }
    public boolean textual() { return id.startsWith("text:"); }
    public boolean textBox() { return annotation() && "FreeText".equals(type); }
}
