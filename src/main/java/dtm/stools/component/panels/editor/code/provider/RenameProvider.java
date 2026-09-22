package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.rename.RenamePrepareContext;
import dtm.stools.component.panels.editor.code.rename.RenamePreparation;

import java.util.List;

@FunctionalInterface
public interface RenameProvider extends CodeEditorProvider {

    List<TextEdit> computeRenameEdits(RenameContext context);

    default RenamePreparation prepareRename(RenamePrepareContext context) {
        return null;
    }

    default String validateNewName(RenamePrepareContext context, String newName) {
        return null;
    }

    default void onRenameApplied(RenameContext context, List<TextEdit> appliedEdits) {
    }
}
