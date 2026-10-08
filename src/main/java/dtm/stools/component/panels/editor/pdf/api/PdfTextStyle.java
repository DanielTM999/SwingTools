package dtm.stools.component.panels.editor.pdf.api;

import lombok.With;

import java.awt.Color;
import java.util.List;
import java.util.Objects;

@With
public record PdfTextStyle(String family, float size, Color color, boolean bold, boolean italic) {
    public static final String HELVETICA = "Helvetica", TIMES = "Times", COURIER = "Courier";
    public static final List<String> FAMILIES = List.of(HELVETICA, TIMES, COURIER);
    public PdfTextStyle {
        Objects.requireNonNull(color);
        family = FAMILIES.contains(family) ? family : HELVETICA;
        if (!Float.isFinite(size) || size < 2 || size > 400) throw new IllegalArgumentException("Tamanho de fonte inválido");
    }
    public static PdfTextStyle defaults() { return new PdfTextStyle(HELVETICA, 14, Color.BLACK, false, false); }
}
