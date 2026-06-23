package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.Position;


public record DefinitionContext(String buffer, Position position, int offset) {
}
