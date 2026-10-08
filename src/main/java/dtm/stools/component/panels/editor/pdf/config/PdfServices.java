package dtm.stools.component.panels.editor.pdf.config;

import dtm.stools.component.panels.editor.pdf.backend.JvmPdfTrustProvider;
import dtm.stools.component.panels.editor.pdf.backend.PdfBoxBackendProvider;
import dtm.stools.component.panels.editor.pdf.backend.PdfBoxSignatureProvider;
import dtm.stools.component.panels.editor.pdf.element.PdfElementFactories;
import dtm.stools.component.panels.editor.pdf.element.PdfElementFactory;
import dtm.stools.component.panels.editor.pdf.provider.PdfBackendProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfDialogProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfFileDialogProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfOcrProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfSignatureProvider;
import dtm.stools.component.panels.editor.pdf.provider.PdfTrustProvider;
import dtm.stools.component.panels.editor.pdf.ui.PdfUiFactory;
import dtm.stools.component.panels.editor.pdf.ui.popup.DefaultPdfDialogProvider;
import dtm.stools.component.panels.editor.pdf.ui.popup.DefaultPdfFileDialogProvider;
import lombok.With;

import java.util.List;
import java.util.Objects;

@With
public record PdfServices(PdfBackendProvider backend, PdfOcrProvider ocr,
                          PdfSignatureProvider signatures, PdfTrustProvider trust,
                          PdfDialogProvider dialogs, PdfFileDialogProvider files, PdfUiFactory uiFactory,
                          List<PdfElementFactory> elementFactories) {
    public PdfServices {
        Objects.requireNonNull(backend);
        Objects.requireNonNull(signatures);
        Objects.requireNonNull(trust);
        Objects.requireNonNull(dialogs);
        Objects.requireNonNull(files);
        Objects.requireNonNull(uiFactory);
        elementFactories = List.copyOf(elementFactories);
    }
    public PdfServices(PdfBackendProvider backend, PdfOcrProvider ocr, PdfSignatureProvider signatures,
                       PdfTrustProvider trust, PdfDialogProvider dialogs, PdfFileDialogProvider files) {
        this(backend, ocr, signatures, trust, dialogs, files, PdfUiFactory.defaults(), PdfElementFactories.defaults());
    }
    public static PdfServices defaults() {
        return new PdfServices(new PdfBoxBackendProvider(), null,
                new PdfBoxSignatureProvider(), new JvmPdfTrustProvider(),
                new DefaultPdfDialogProvider(), new DefaultPdfFileDialogProvider(), PdfUiFactory.defaults(),
                PdfElementFactories.defaults());
    }
}
