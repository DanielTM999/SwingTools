package dtm.stools.component.panels.editor.pdf.provider;

@FunctionalInterface
public interface PdfProviderRegistration extends AutoCloseable {
    @Override
    void close();

    static PdfProviderRegistration none() { return () -> {}; }
}
