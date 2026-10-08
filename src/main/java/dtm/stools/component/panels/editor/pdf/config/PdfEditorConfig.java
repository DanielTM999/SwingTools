package dtm.stools.component.panels.editor.pdf.config;

import dtm.stools.component.panels.editor.pdf.api.PdfViewMode;
import lombok.Builder;
import lombok.With;

import java.util.Locale;
import java.util.Objects;

@With
@Builder(toBuilder = true)
public record PdfEditorConfig(boolean readOnly, boolean ribbonVisible, boolean thumbnailsVisible,
                              boolean statusVisible, double zoom, int cachePages, int historyLimit,
                              boolean pageReconstructionEnabled, Locale locale, PdfViewMode viewMode) {
    public PdfEditorConfig {
        Objects.requireNonNull(locale);
        Objects.requireNonNull(viewMode);
        if (!Double.isFinite(zoom) || zoom < .1 || zoom > 8 || cachePages < 1 || historyLimit < 0)
            throw new IllegalArgumentException("Configuração inválida do editor de PDF");
    }

    public static PdfEditorConfig defaults() {
        return new PdfEditorConfig(false, true, true, true, 1, 16, 30, true,
                Locale.forLanguageTag("pt-BR"), PdfViewMode.CONTINUOUS);
    }
}
