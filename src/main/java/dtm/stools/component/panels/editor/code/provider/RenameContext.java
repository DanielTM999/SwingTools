package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.api.Position;

import java.util.Map;

public record RenameContext(String buffer, Position position, int offset, String newName, Map<String, Boolean> options) {

    public RenameContext {
        options = options == null ? Map.of() : Map.copyOf(options);
    }

    public RenameContext(String buffer, Position position, int offset, String newName) {
        this(buffer, position, offset, newName, Map.of());
    }

    public boolean option(String id) {
        return Boolean.TRUE.equals(options.get(id));
    }
}
