package dtm.stools.component.panels.editor.pdf.provider;

import dtm.stools.component.panels.editor.pdf.PdfEditor;

import javax.swing.Action;
import java.util.Map;

public interface PdfCommandProvider extends PdfProvider {
    Map<String, Action> commands(PdfEditor editor);
}
