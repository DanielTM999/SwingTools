package dtm.stools.component.panels.editor.code.codelens;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.List;

@FunctionalInterface
public interface CodeLensProvider extends CodeEditorProvider {

    List<CodeLens> getCodeLenses(CodeLensContext context);
}
