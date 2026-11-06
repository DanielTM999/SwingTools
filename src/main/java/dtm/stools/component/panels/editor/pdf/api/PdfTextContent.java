package dtm.stools.component.panels.editor.pdf.api;

import java.awt.geom.Rectangle2D;
import java.util.List;

public record PdfTextContent(String text, List<Glyph> glyphs) {
    public PdfTextContent { glyphs = List.copyOf(glyphs); }
    public record Glyph(int start, int end, Rectangle2D.Float bounds) {}
    public static PdfTextContent empty() { return new PdfTextContent("", List.of()); }
}
