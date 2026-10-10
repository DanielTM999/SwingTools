package dtm.stools.component.panels.editor.pdf.element;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;

import java.awt.Cursor;

public final class TextElementFactory extends BasePdfElementFactory {
    public TextElementFactory() {
        super("pdf.factory.text", "pdf.addText", "Texto", "text",
                "Clique para escrever ou arraste para definir a largura da caixa de texto", PdfPlacementMode.DRAG_RECT);
    }
    @Override
    public int cursor() { return Cursor.TEXT_CURSOR; }
    @Override
    public void insert(PdfEditor editor, PdfPlacement placement) {
        editor.beginTextInput(placement);
    }
}
