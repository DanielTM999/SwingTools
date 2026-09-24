package dtm.stools.component.panels.editor.word.provider;

import java.nio.file.Path;
import java.util.Optional;

public interface WordFileDialogProvider extends WordProvider {
    Optional<Path> choose(WordFileDialogRequest request);
}
