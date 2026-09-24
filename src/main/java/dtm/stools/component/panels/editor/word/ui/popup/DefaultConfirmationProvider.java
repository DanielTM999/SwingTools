package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.provider.*;
import javax.swing.JPanel;

public final class DefaultConfirmationProvider implements WordConfirmationProvider, AutoCloseable {
    private final DefaultDialogProvider dialogs = new DefaultDialogProvider();
    @Override public String id() { return "word.popup.confirmation.default"; }
    @Override public boolean confirm(WordConfirmationRequest request) {
        return dialogs.show(new WordDialogRequest<>(request.owner(), "word.confirmation.dialog", request.title(),
                request.message(), new JPanel(), () -> Boolean.TRUE, value -> {}, request.confirmText(), false, true)).orElse(false);
    }
    @Override public void close() { dialogs.close(); }
}
