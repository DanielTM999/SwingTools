package dtm.stools.component.panels.editor.pdf.backend;

import dtm.stools.component.panels.editor.pdf.api.PdfDocument;
import dtm.stools.component.panels.editor.pdf.provider.PdfBackendProvider;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;

import java.io.IOException;
import java.nio.file.Path;

public final class PdfBoxBackendProvider implements PdfBackendProvider {
    @Override
    public String id() { return "pdf.backend.pdfbox"; }
    @Override
    public PdfDocument create() {
        PDDocument document = new PDDocument();
        document.addPage(new PDPage(PDRectangle.A4));
        return new PdfBoxDocument(document);
    }
    @Override
    public PdfDocument open(Path path, char[] password) throws IOException {
        return new PdfBoxDocument(Loader.loadPDF(path.toFile(), password == null ? "" : new String(password)));
    }
    @Override
    public PdfDocument restore(byte[] snapshot) throws IOException {
        return new PdfBoxDocument(Loader.loadPDF(snapshot));
    }
    @Override
    public boolean isPasswordError(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause())
            if (cause instanceof InvalidPasswordException) return true;
        return false;
    }
}
