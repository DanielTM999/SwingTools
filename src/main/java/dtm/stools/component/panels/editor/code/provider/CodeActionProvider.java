package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.CodeAction;

import java.util.List;

/**
 * Returns available quick fixes / refactorings / source actions for a range.
 * The editor shows them in a popup (Alt+Enter by default) and applies the chosen one.
 */
@FunctionalInterface
public interface CodeActionProvider extends CodeEditorProvider {

    List<CodeAction> getCodeActions(CodeActionContext context);
}
