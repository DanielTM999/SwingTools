package dtm.stools.component.panels.editor.pdf.backend;

import dtm.stools.component.panels.editor.pdf.api.PdfTextContent;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.StringWriter;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class PdfBoxTextSearch extends PDFTextStripper {
    private record Span(int start, int end, Rectangle2D.Float bounds) {}

    private final StringWriter text = new StringWriter();
    private final List<Span> spans = new ArrayList<>();
    private final PDRectangle crop;

    private PdfBoxTextSearch(PDRectangle crop) {
        this.crop = crop;
        setSortByPosition(true);
    }

    static List<Rectangle2D.Float> find(PDDocument document, int page, String query) throws IOException {
        if (query == null || query.isBlank()) return List.of();
        PdfBoxTextSearch search = new PdfBoxTextSearch(document.getPage(page).getCropBox());
        search.setStartPage(page + 1);
        search.setEndPage(page + 1);
        search.writeText(document, search.text);
        var matcher = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE)
                .matcher(search.text.toString());
        List<Rectangle2D.Float> found = new ArrayList<>();
        while (matcher.find()) {
            Rectangle2D.Float line = null;
            for (Span span : search.spans) {
                if (span.end() <= matcher.start() || span.start() >= matcher.end()) continue;
                Rectangle2D.Float box = span.bounds();
                if (line != null && Math.abs(line.y - box.y) < Math.min(line.height, box.height) * .4
                        && Math.abs(line.height - box.height) < Math.max(line.height, box.height) * .4
                        && box.x <= line.getMaxX() + box.height && box.getMaxX() >= line.x - box.height) {
                    Rectangle2D.union(line, box, line);
                } else {
                    line = new Rectangle2D.Float(box.x, box.y, box.width, box.height);
                    found.add(line);
                }
            }
        }
        return List.copyOf(found);
    }

    static PdfTextContent content(PDDocument document, int page) throws IOException {
        PdfBoxTextSearch search = new PdfBoxTextSearch(document.getPage(page).getCropBox());
        search.setStartPage(page + 1);
        search.setEndPage(page + 1);
        search.writeText(document, search.text);
        return new PdfTextContent(search.text.toString(), search.spans.stream()
                .map(span -> new PdfTextContent.Glyph(span.start(), span.end(), span.bounds())).toList());
    }

    @Override
    protected void writeString(String value, List<TextPosition> positions) throws IOException {
        int start = text.getBuffer().length();
        super.writeString(value, positions);
        int offset = 0;
        for (TextPosition position : positions) {
            String unicode = position.getUnicode();
            if (unicode == null || unicode.isEmpty()) continue;
            int index = value.indexOf(unicode, offset);
            if (index < 0) {
                unicode = Normalizer.normalize(unicode, Normalizer.Form.NFKC);
                index = value.indexOf(unicode, offset);
            }
            if (index < 0) continue;
            spans.add(new Span(start + index, start + index + unicode.length(), bounds(position)));
            offset = index + unicode.length();
        }
    }

    private Rectangle2D.Float bounds(TextPosition position) {
        var matrix = position.getTextMatrix();
        double length = Math.hypot(matrix.getScaleX(), matrix.getShearY());
        double nx = length == 0 ? 0 : -matrix.getShearY() / length;
        double ny = length == 0 ? 1 : matrix.getScaleX() / length;
        float height = Math.max(1, Math.max(position.getHeightDir(), matrix.getScalingFactorY() * .8f));
        double x1 = matrix.getTranslateX() + crop.getLowerLeftX();
        double y1 = matrix.getTranslateY() + crop.getLowerLeftY();
        double x2 = position.getEndX() + crop.getLowerLeftX();
        double y2 = position.getEndY() + crop.getLowerLeftY();
        double minX = Math.min(x1, x2) + Math.min(-nx * height * .2, nx * height);
        double minY = Math.min(y1, y2) + Math.min(-ny * height * .2, ny * height);
        double maxX = Math.max(x1, x2) + Math.max(-nx * height * .2, nx * height);
        double maxY = Math.max(y1, y2) + Math.max(-ny * height * .2, ny * height);
        return new Rectangle2D.Float((float) minX, (float) minY, (float) Math.max(.5, maxX - minX),
                (float) Math.max(.5, maxY - minY));
    }
}
