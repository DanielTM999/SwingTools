package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.inputfields.osfilepicker.DeFilter;
import dtm.stools.component.inputfields.osfilepicker.OsFilePicker;
import dtm.stools.component.panels.editor.sheet.provider.SheetFileDialogProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetFileDialogRequest;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class DefaultFileDialogProvider implements SheetFileDialogProvider {
    @Override public String id() { return "sheet.popup.files.default"; }

    @Override
    public Optional<Path> choose(SheetFileDialogRequest request) {
        File directory = request.initialDirectory() == null ? null : request.initialDirectory().toFile();
        DeFilter[] filters = request.filters().stream().map(f -> DeFilter.of(f.description(), f.extensions().toArray(String[]::new))).toArray(DeFilter[]::new);
        boolean saving = request.mode() == SheetFileDialogRequest.Mode.SAVE || request.mode() == SheetFileDialogRequest.Mode.EXPORT;
        File selected = saving ? OsFilePicker.saveFile(request.title(), directory, request.suggestedName(), filters) : OsFilePicker.openFile(request.title(), directory, filters);
        if (selected == null) return Optional.empty();
        Path path = selected.toPath();
        if (saving && !request.filters().isEmpty() && !request.filters().getFirst().extensions().isEmpty()) {
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            boolean known = request.filters().stream().flatMap(f -> f.extensions().stream()).anyMatch(ext -> name.endsWith("." + ext.toLowerCase(Locale.ROOT)));
            if (!known) path = path.resolveSibling(path.getFileName() + "." + request.filters().getFirst().extensions().getFirst());
        }
        return Optional.of(path);
    }
}
