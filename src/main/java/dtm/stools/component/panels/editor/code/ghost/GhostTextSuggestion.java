package dtm.stools.component.panels.editor.code.ghost;

import dtm.stools.component.panels.editor.code.api.TextEdit;

import java.util.List;
import java.util.Objects;

public record GhostTextSuggestion(String text, List<TextEdit> additionalTextEdits) {

    public GhostTextSuggestion {
        additionalTextEdits = additionalTextEdits == null
                ? List.of()
                : additionalTextEdits.stream().filter(Objects::nonNull).toList();
    }

    public static GhostTextSuggestion of(String text) {
        return text == null || text.isEmpty() ? null : new GhostTextSuggestion(text, List.of());
    }

    public boolean isEmpty() {
        return text == null || text.isEmpty();
    }

    public boolean hasAdditionalTextEdits() {
        return !additionalTextEdits.isEmpty();
    }
}
