package dtm.stools.component.panels.editor.powerpoint.provider;

import dtm.stools.component.panels.editor.powerpoint.model.PptObject;
import java.awt.Component;
import java.nio.file.Path;
import java.util.Optional;

public interface PowerPointFileDialogProvider extends PowerPointProvider {
    Optional<Path> chooseOpen(Component owner);
    Optional<Path> chooseSave(Component owner,Path current);
    Optional<Path> chooseMedia(Component owner,PptObject.Kind kind);
}
