package dtm.stools.component.inputfields.textarea.diagnostic;

import dtm.stools.component.inputfields.textarea.SmartTextArea;

import java.util.function.BiConsumer;

record CallbackTextDiagnosticAction(String label, BiConsumer<SmartTextArea, TextDiagnostic> action) implements TextDiagnosticAction {

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void apply(SmartTextArea area, TextDiagnostic diagnostic) {
        if (action != null) action.accept(area, diagnostic);
    }
}
