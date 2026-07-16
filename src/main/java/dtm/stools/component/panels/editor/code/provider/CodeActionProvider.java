package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.CodeAction;

import java.util.List;

@FunctionalInterface
public interface CodeActionProvider extends CodeEditorProvider {

    List<CodeAction> getCodeActions(CodeActionContext context);
}
