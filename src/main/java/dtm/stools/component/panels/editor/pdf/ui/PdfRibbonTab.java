package dtm.stools.component.panels.editor.pdf.ui;

import java.util.List;

public record PdfRibbonTab(String id, String title, List<PdfRibbonGroup> groups, boolean contextual) {
    public PdfRibbonTab { groups = List.copyOf(groups); }
}
