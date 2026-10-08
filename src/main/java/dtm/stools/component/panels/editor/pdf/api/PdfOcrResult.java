package dtm.stools.component.panels.editor.pdf.api;

import java.util.List;

public record PdfOcrResult(String text, List<PdfOcrWord> words) {
    public PdfOcrResult { words = List.copyOf(words); }
}
