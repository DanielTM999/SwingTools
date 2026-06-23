package dtm.stools.component.panels.editor.code.autocomplete;

import dtm.stools.component.panels.editor.code.api.TextEdit;

import javax.swing.Icon;
import java.util.List;

public record AutoCompleteItem(
        String insertText,
        String label,
        String detail,
        String description,
        Icon icon,
        Kind kind,
        List<TextEdit> additionalTextEdits
) {

    public enum Kind { TEXT, SNIPPET }

    public AutoCompleteItem {
        additionalTextEdits = additionalTextEdits == null ? List.of() : List.copyOf(additionalTextEdits);
    }

    public AutoCompleteItem(String text) {
        this(text, text, null, null, null, Kind.TEXT, List.of());
    }

    public AutoCompleteItem(String insertText, String label) {
        this(insertText, label, null, null, null, Kind.TEXT, List.of());
    }

    public AutoCompleteItem(String insertText, String label, String detail) {
        this(insertText, label, detail, null, null, Kind.TEXT, List.of());
    }

    public AutoCompleteItem(String insertText, String label, String detail, String description, Icon icon) {
        this(insertText, label, detail, description, icon, Kind.TEXT, List.of());
    }

    public AutoCompleteItem(String insertText, String label, String detail, String description, Icon icon, Kind kind) {
        this(insertText, label, detail, description, icon, kind, List.of());
    }

    public boolean isSnippet() {
        return kind == Kind.SNIPPET;
    }

    public boolean hasAdditionalTextEdits() {
        return additionalTextEdits != null && !additionalTextEdits.isEmpty();
    }

    public static AutoCompleteItem snippet(String label, String body) {
        return new AutoCompleteItem(body, label, "snippet", null, null, Kind.SNIPPET, List.of());
    }

    public static AutoCompleteItem snippet(String label, String body, String description) {
        return new AutoCompleteItem(body, label, "snippet", description, null, Kind.SNIPPET, List.of());
    }
}
