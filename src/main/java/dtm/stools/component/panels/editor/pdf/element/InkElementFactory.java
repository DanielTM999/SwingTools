package dtm.stools.component.panels.editor.pdf.element;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;

import java.io.IOException;

public final class InkElementFactory extends BasePdfElementFactory {
    public InkElementFactory() {
        super("pdf.factory.ink", "pdf.draw", "Desenhar", "pen", "Arraste o mouse na página para desenhar à mão livre",
                PdfPlacementMode.FREEHAND);
    }
    @Override public boolean keepActive() { return true; }
    @Override public void insert(PdfEditor editor, PdfPlacement placement) throws IOException {
        float[] stroke = placement.stroke();
        if (stroke.length >= 4) editor.addInk(placement.page(), stroke, editor.getShapeStyle());
    }
}
