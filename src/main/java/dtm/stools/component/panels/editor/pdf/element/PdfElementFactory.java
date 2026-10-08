package dtm.stools.component.panels.editor.pdf.element;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;
import dtm.stools.component.panels.editor.pdf.provider.PdfProvider;

import java.awt.Cursor;
import java.io.IOException;

public interface PdfElementFactory extends PdfProvider {
    String title();
    default String commandId() { return "pdf.element." + id(); }
    default String icon() { return "shape"; }
    default String tip() { return title(); }
    default String ribbonTab() { return "insert"; }
    default String ribbonGroup() { return "Extensões"; }
    default boolean large() { return false; }
    default boolean ribbonItem() { return true; }
    default PdfPlacementMode placementMode() { return PdfPlacementMode.CLICK; }
    default int cursor() { return Cursor.CROSSHAIR_CURSOR; }
    default boolean keepActive() { return false; }
    void insert(PdfEditor editor, PdfPlacement placement) throws IOException;
}
