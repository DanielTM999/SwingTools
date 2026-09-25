package dtm.stools.component.panels.editor.sheet.provider;

import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public record SheetFileDialogRequest(Component owner, Mode mode, String title, List<Filter> filters, Path initialDirectory, String suggestedName) {
    public enum Mode { OPEN, SAVE, EXPORT, IMAGE, IMPORT }
    public record Filter(String description, List<String> extensions) {}

    public SheetFileDialogRequest { Objects.requireNonNull(owner); Objects.requireNonNull(mode); filters = List.copyOf(filters); }
}
