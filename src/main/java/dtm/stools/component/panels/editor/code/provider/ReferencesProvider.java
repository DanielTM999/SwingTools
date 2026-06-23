package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.Location;

import java.util.List;


@FunctionalInterface
public interface ReferencesProvider extends CodeEditorProvider {

    List<Location> findReferences(DefinitionContext context);
}
