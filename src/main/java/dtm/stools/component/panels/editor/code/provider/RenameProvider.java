package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.TextEdit;

import java.util.List;

/**
 * Computes the edits to perform a rename of the symbol at the caret.
 * The editor applies the returned edits atomically as a single undo unit.
 * Return an empty list to veto the rename.
 */
@FunctionalInterface
public interface RenameProvider extends CodeEditorProvider {

    List<TextEdit> computeRenameEdits(RenameContext context);
}
