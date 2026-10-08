package dtm.stools.component.panels.editor.pdf.api;

import java.io.IOException;

@FunctionalInterface
public interface PdfOperation {
    void run() throws IOException;
}
