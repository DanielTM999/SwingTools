package dtm.stools.component.panels.editor.pdf.provider;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.ui.PdfRibbonGroup;
import dtm.stools.component.panels.editor.pdf.ui.PdfRibbonItem;

import java.util.List;

public interface PdfRibbonContributor extends PdfProvider {
    String tab();
    String group();
    List<String> commandIds(PdfEditor editor);
    default String icon() { return "more"; }
    default int groupPriority() { return 10; }
    default PdfRibbonGroup ribbonGroup(PdfEditor editor) {
        return new PdfRibbonGroup("provider." + id(), group(), groupPriority(), icon(),
                commandIds(editor).stream().map(PdfRibbonItem::small).toList());
    }
}
