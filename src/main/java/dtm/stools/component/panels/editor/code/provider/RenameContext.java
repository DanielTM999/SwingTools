package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.Position;


public record RenameContext(String buffer, Position position, int offset, String newName) {
}
