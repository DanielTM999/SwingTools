package dtm.stools.component.panels.editor.pdf.ui;

import java.util.List;

public record PdfRibbonGroup(String id, String title, int priority, String icon, List<PdfRibbonItem> items) {
    public PdfRibbonGroup { items = List.copyOf(items); }
}
