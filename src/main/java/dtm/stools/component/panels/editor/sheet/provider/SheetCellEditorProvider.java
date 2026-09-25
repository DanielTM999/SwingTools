package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;

import javax.swing.JComponent;
import java.util.Optional;
import java.util.function.Consumer;

public interface SheetCellEditorProvider extends SheetProvider {
    Optional<JComponent> editor(SheetEditor editor, int sheet, CellAddress cell, String currentText, Consumer<String> commit, Runnable cancel);
}
