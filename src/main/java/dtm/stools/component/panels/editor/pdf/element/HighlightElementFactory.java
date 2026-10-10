package dtm.stools.component.panels.editor.pdf.element;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;

import java.awt.geom.Rectangle2D;
import java.io.IOException;

public final class HighlightElementFactory extends BasePdfElementFactory {
    public HighlightElementFactory() {
        super("pdf.factory.highlight", "pdf.highlight", "Destacar", "highlight",
                "Arraste sobre o trecho que deseja destacar", PdfPlacementMode.DRAG_RECT);
    }
    @Override
    public boolean keepActive() { return true; }
    @Override
    public void insert(PdfEditor editor, PdfPlacement placement) throws IOException {
        Rectangle2D.Float area = placement.dragged() ? placement.bounds()
                : new Rectangle2D.Float(placement.point().x, placement.point().y - 9, 120, 18);
        editor.addHighlight(placement.page(), area.x, area.y, area.width, area.height);
    }
}
