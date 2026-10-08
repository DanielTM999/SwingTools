package dtm.stools.component.panels.editor.pdf.provider;

import dtm.stools.component.panels.editor.pdf.PdfEditor;

import javax.swing.JComponent;

public interface PdfToolbarProvider extends PdfProvider {
    JComponent createToolbar(PdfEditor editor);
}
