package dtm.stools.component.panels.editor.word.provider;

import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record WordFileDialogRequest(Component owner, Mode mode, String title, String suggestedName, String description, List<String> extensions, Path directory) {
    public enum Mode { OPEN, SAVE, EXPORT, IMAGE }
    public WordFileDialogRequest {
        Objects.requireNonNull(mode); title = title == null ? "" : title; suggestedName = suggestedName == null ? "" : suggestedName;
        description = description == null ? "" : description; extensions = extensions == null ? List.of() : List.copyOf(extensions);
    }
    public boolean saving() { return mode == Mode.SAVE || mode == Mode.EXPORT; }
}
