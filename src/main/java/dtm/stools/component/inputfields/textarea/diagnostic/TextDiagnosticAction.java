package dtm.stools.component.inputfields.textarea.diagnostic;

import dtm.stools.component.inputfields.textarea.SmartTextArea;

import java.util.function.BiConsumer;

public interface TextDiagnosticAction {

    String getLabel();

    void apply(SmartTextArea area, TextDiagnostic diagnostic);

    static TextDiagnosticAction replaceWith(String replacement) {
        return new ReplaceTextDiagnosticAction(replacement == null ? "" : replacement);
    }

    static TextDiagnosticAction of(String label, BiConsumer<SmartTextArea, TextDiagnostic> action) {
        return new CallbackTextDiagnosticAction(label == null ? "" : label, action);
    }
}
