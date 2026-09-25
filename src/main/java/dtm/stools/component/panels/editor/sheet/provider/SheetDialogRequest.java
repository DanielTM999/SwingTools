package dtm.stools.component.panels.editor.sheet.provider;

import javax.swing.JComponent;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public record SheetDialogRequest<T>(Component owner, String id, String title, String message, JComponent content, Supplier<T> result, Consumer<T> validate,
                                    String confirmText, boolean readOnly, boolean enterConfirms, boolean modal) {
    public SheetDialogRequest {
        Objects.requireNonNull(owner); Objects.requireNonNull(id); Objects.requireNonNull(content); Objects.requireNonNull(result);
        validate = validate == null ? v -> {} : validate;
        title = Objects.requireNonNullElse(title, "Planilha");
        message = Objects.requireNonNullElse(message, "");
        confirmText = Objects.requireNonNullElse(confirmText, "OK");
    }

    public static <T> SheetDialogRequest<T> of(Component owner, String id, String title, JComponent content, Supplier<T> result) {
        return new SheetDialogRequest<>(owner, id, title, "", content, result, v -> {}, "OK", false, true, true);
    }

    public SheetDialogRequest<T> withValidation(Consumer<T> v) { return new SheetDialogRequest<>(owner, id, title, message, content, result, v, confirmText, readOnly, enterConfirms, modal); }
    public SheetDialogRequest<T> withConfirmText(String text) { return new SheetDialogRequest<>(owner, id, title, message, content, result, validate, text, readOnly, enterConfirms, modal); }
    public SheetDialogRequest<T> withMessage(String text) { return new SheetDialogRequest<>(owner, id, title, text, content, result, validate, confirmText, readOnly, enterConfirms, modal); }
    public SheetDialogRequest<T> modeless() { return new SheetDialogRequest<>(owner, id, title, message, content, result, validate, confirmText, readOnly, enterConfirms, false); }
}
