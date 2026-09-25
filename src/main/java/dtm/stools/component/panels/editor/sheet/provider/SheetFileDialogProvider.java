package dtm.stools.component.panels.editor.sheet.provider;

import java.nio.file.Path;
import java.util.Optional;

public interface SheetFileDialogProvider extends SheetProvider {
    Optional<Path> choose(SheetFileDialogRequest request);
}
