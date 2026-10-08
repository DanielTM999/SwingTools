package dtm.stools.component.panels.editor.pdf.provider;

import dtm.stools.component.panels.editor.pdf.api.PdfDocument;

import java.io.IOException;
import java.nio.file.Path;

public interface PdfBackendProvider extends PdfProvider {
    PdfDocument create() throws IOException;
    PdfDocument open(Path path, char[] password) throws IOException;
    PdfDocument restore(byte[] snapshot) throws IOException;
    default boolean isPasswordError(Throwable error) { return false; }
}
