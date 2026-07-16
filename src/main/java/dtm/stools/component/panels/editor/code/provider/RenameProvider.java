package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.TextEdit;

import java.util.List;

@FunctionalInterface
public interface RenameProvider extends CodeEditorProvider {

    List<TextEdit> computeRenameEdits(RenameContext context);
}
