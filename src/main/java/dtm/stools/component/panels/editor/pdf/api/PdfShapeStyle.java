package dtm.stools.component.panels.editor.pdf.api;

import lombok.With;

import java.awt.Color;
import java.util.Objects;

@With
public record PdfShapeStyle(Color stroke, Color fill, float lineWidth, float opacity) {
    public PdfShapeStyle {
        Objects.requireNonNull(stroke);
        if (!Float.isFinite(lineWidth) || lineWidth < 0 || lineWidth > 72) throw new IllegalArgumentException("Espessura inválida");
        if (!Float.isFinite(opacity) || opacity < 0 || opacity > 1) throw new IllegalArgumentException("Opacidade inválida");
    }
    public static PdfShapeStyle defaults() { return new PdfShapeStyle(new Color(0x1F6FD1), null, 2, 1); }
}
