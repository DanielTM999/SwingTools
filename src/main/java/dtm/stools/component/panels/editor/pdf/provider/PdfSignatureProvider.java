package dtm.stools.component.panels.editor.pdf.provider;

import dtm.stools.component.panels.editor.pdf.api.PdfSignatureValidation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public interface PdfSignatureProvider extends PdfProvider {
    void sign(Path source, Path destination, Path pkcs12, char[] password, String reason) throws IOException;
    List<PdfSignatureValidation> validate(Path source, PdfTrustProvider trust) throws IOException;
}
