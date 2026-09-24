package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.provider.*;
import java.util.*;

public final class DefaultDialogProvider implements WordDialogProvider, AutoCloseable {
    private final Set<WordDialogActivity<?>> open = new HashSet<>();
    @Override public String id() { return "word.popup.dialog.default"; }
    @Override public <T> Optional<T> show(WordDialogRequest<T> request) {
        WordDialogActivity<T> activity = new WordDialogActivity<>(request);
        open.add(activity);
        activity.onClosed(() -> open.remove(activity));
        try { return activity.showResult(); } finally { activity.dispose(); }
    }
    @Override public void close() { for (var activity : List.copyOf(open)) activity.dispose(); }
}
