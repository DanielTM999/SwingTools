package dtm.stools.component.panels.editor.code.provider;


@FunctionalInterface
public interface DefinitionProvider extends CodeEditorProvider {
    void onDefinitionsRequest(DefinitionContext context);
}
