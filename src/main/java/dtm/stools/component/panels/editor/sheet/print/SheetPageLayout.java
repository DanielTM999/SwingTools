package dtm.stools.component.panels.editor.sheet.print;

import dtm.stools.component.panels.editor.sheet.model.PrintSettings;

import java.util.List;

public record SheetPageLayout(PrintSettings settings, double scale, double pageWidth, double pageHeight, List<SheetPage> pages, List<Integer> rowBreaks, List<Integer> columnBreaks) {
    public SheetPageLayout {
        pages = List.copyOf(pages);
        rowBreaks = List.copyOf(rowBreaks);
        columnBreaks = List.copyOf(columnBreaks);
    }
}
