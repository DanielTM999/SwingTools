package dtm.stools.component.panels.editor.pdf.provider;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfSelection;

import javax.swing.Action;
import java.util.List;

public interface PdfContextMenuProvider extends PdfProvider {
    List<Action> canvasActions(PdfEditor editor, int page, PdfSelection selection);
    default List<Action> thumbnailActions(PdfEditor editor, int page) { return List.of(); }
}
