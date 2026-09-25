package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

import java.util.List;
import java.util.Objects;

@With
@Builder(toBuilder = true)
public record PrintSettings(PageOrientation orientation, PaperSize paper, int scale, int fitWidth, int fitHeight, double marginLeft, double marginRight,
                            double marginTop, double marginBottom, double marginHeader, double marginFooter, String headerLeft, String headerCenter,
                            String headerRight, String footerLeft, String footerCenter, String footerRight, CellRange printArea, Integer repeatRowFirst,
                            Integer repeatRowLast, Integer repeatColumnFirst, Integer repeatColumnLast, boolean gridlines, boolean headings,
                            boolean centerHorizontally, boolean centerVertically, boolean overThenDown, List<Integer> rowBreaks, List<Integer> columnBreaks) {
    public static final PrintSettings DEFAULT = new PrintSettings(PageOrientation.PORTRAIT, PaperSize.A4, 100, 0, 0, .7, .7, .75, .75, .3, .3,
            "", "", "", "", "Página &P", "", null, null, null, null, null, false, false, false, false, false, List.of(), List.of());

    public PrintSettings {
        orientation = Objects.requireNonNullElse(orientation, PageOrientation.PORTRAIT);
        paper = Objects.requireNonNullElse(paper, PaperSize.A4);
        scale = Math.max(10, Math.min(400, scale == 0 ? 100 : scale));
        headerLeft = Objects.requireNonNullElse(headerLeft, ""); headerCenter = Objects.requireNonNullElse(headerCenter, ""); headerRight = Objects.requireNonNullElse(headerRight, "");
        footerLeft = Objects.requireNonNullElse(footerLeft, ""); footerCenter = Objects.requireNonNullElse(footerCenter, ""); footerRight = Objects.requireNonNullElse(footerRight, "");
        rowBreaks = rowBreaks == null ? List.of() : List.copyOf(rowBreaks);
        columnBreaks = columnBreaks == null ? List.of() : List.copyOf(columnBreaks);
    }

    public double pageWidth() { return orientation == PageOrientation.LANDSCAPE ? paper.height() : paper.width(); }
    public double pageHeight() { return orientation == PageOrientation.LANDSCAPE ? paper.width() : paper.height(); }
}
