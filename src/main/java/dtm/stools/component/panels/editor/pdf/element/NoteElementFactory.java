package dtm.stools.component.panels.editor.pdf.element;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfPlacement;

import java.io.IOException;
import java.util.Optional;

public final class NoteElementFactory extends BasePdfElementFactory {
    public NoteElementFactory() {
        super("pdf.factory.note", "pdf.note", "Nota", "note", "Clique na página para fixar uma nota", PdfPlacementMode.CLICK);
    }
    @Override
    public void insert(PdfEditor editor, PdfPlacement placement) throws IOException {
        Optional<String> text = editor.getDialogs().input(editor, "Inserir nota", "Nota:");
        if (text.isPresent() && !text.get().isBlank())
            editor.addNote(placement.page(), text.get(), placement.point().x, placement.point().y - 24);
    }
}
