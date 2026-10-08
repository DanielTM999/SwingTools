package dtm.stools.component.panels.editor.pdf.provider;

import dtm.stools.component.panels.editor.pdf.PdfEditor;

public interface PdfProvider {
    String id();
    default int priority() { return 0; }
    default PdfProviderRegistration attach(PdfEditor editor) { return PdfProviderRegistration.none(); }
}
