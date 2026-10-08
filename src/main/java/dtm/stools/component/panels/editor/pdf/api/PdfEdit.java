package dtm.stools.component.panels.editor.pdf.api;

import java.io.IOException;

@FunctionalInterface
public interface PdfEdit {
    void apply(PdfDocument document) throws IOException;
}
