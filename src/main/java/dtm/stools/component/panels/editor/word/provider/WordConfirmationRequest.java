package dtm.stools.component.panels.editor.word.provider;

import java.awt.Component;
import java.util.Objects;

public record WordConfirmationRequest(Component owner, Kind kind, String title, String message, String confirmText) {
    public enum Kind { DISCARD_CHANGES, REPLACE_FILE, DELETE_CONTENT, OTHER }
    public WordConfirmationRequest {
        Objects.requireNonNull(kind); title = title == null ? "" : title; message = message == null ? "" : message;
        confirmText = confirmText == null || confirmText.isBlank() ? "Confirmar" : confirmText;
    }
}
