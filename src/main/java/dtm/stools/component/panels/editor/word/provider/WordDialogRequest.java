package dtm.stools.component.panels.editor.word.provider;

import javax.swing.JComponent;
import java.awt.Component;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** The result supplier and validator run only on confirmation; failures keep the form open. */
public record WordDialogRequest<T>(Component owner, String id, String title, String message,
                                   JComponent content, Supplier<T> result, Consumer<T> validate,
                                   String confirmText, boolean readOnly, boolean enterConfirms) {
    public WordDialogRequest {
        Objects.requireNonNull(owner); Objects.requireNonNull(id); Objects.requireNonNull(content);
        Objects.requireNonNull(result); Objects.requireNonNull(validate);
        title = Objects.requireNonNullElse(title, "Documento");
        message = Objects.requireNonNullElse(message, "");
        confirmText = Objects.requireNonNullElse(confirmText, "Aplicar");
    }
}
