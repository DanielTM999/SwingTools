package dtm.stools.component.panels.editor.word.provider;

import java.util.Optional;

/** Presents a transactional form on the EDT. Empty means cancelled, never an applied value. */
public interface WordDialogProvider extends WordProvider {
    <T> Optional<T> show(WordDialogRequest<T> request);
}
