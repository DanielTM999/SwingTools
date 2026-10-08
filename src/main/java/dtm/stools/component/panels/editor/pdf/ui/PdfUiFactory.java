package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;

import javax.swing.JComponent;

@FunctionalInterface
public interface PdfUiFactory {
    PdfCanvas createCanvas(PdfEditor editor);
    default JComponent createRibbon(PdfEditor editor) { return new PdfRibbon(editor); }
    default PdfSidebar createSidebar(PdfEditor editor) { return new PdfSidebar(editor); }
    default PdfStatusBar createStatusBar(PdfEditor editor) { return new PdfStatusBar(editor); }
    default PdfSelectionController createSelectionController(PdfEditor editor, PdfCanvas canvas) {
        return new PdfSelectionController(editor, canvas);
    }
    static PdfUiFactory defaults() { return PdfCanvas::new; }
}
