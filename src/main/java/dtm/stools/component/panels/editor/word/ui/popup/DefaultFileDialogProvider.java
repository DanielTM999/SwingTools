package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.provider.WordFileDialogProvider;
import dtm.stools.component.panels.editor.word.provider.WordFileDialogRequest;
import dtm.stools.component.inputfields.osfilepicker.DeFilter;
import dtm.stools.component.inputfields.osfilepicker.OsFilePicker;
import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class DefaultFileDialogProvider implements WordFileDialogProvider {
    @Override public String id() { return "word.popup.files.default"; }

    @Override public Optional<Path> choose(WordFileDialogRequest request) {
        File directory = request.directory() == null ? null : request.directory().toFile();
        // OsFilePicker adds the extension patterns to the displayed filter name.
        String description = request.description().replaceFirst("\\s*\\(\\*\\.[^)]*\\)$", "");
        DeFilter[] filters = request.extensions().isEmpty() ? new DeFilter[0] : new DeFilter[]{
                DeFilter.of(description.isBlank() ? "Arquivos" : description, request.extensions().toArray(String[]::new))};
        File selected = request.saving()
                ? OsFilePicker.saveFile(request.title(), directory, request.suggestedName(), filters)
                : OsFilePicker.openFile(request.title(), directory, filters);
        if (selected == null) return Optional.empty();
        Path path = selected.toPath();
        if (request.saving() && !request.extensions().isEmpty()) {
            String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (request.extensions().stream().noneMatch(ext -> name.endsWith("." + ext.toLowerCase(Locale.ROOT)))) path = path.resolveSibling(path.getFileName() + "." + request.extensions().getFirst());
        }
        return Optional.of(path);
    }
}
