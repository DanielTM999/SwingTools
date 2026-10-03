package dtm.stools.component.panels.editor.code.autocomplete;

import dtm.stools.component.panels.editor.code.api.TextEdit;

import javax.swing.Icon;
import java.util.List;

public record AutoCompleteItem(String insertText, String label, String detail, String description, Icon icon, Kind kind, List<TextEdit> additionalTextEdits, boolean unused, Object data) {

    public static final String CARET_MARKER = "${0}";

    public enum Kind {
        TEXT,
        METHOD,
        FUNCTION,
        CONSTRUCTOR,
        FIELD,
        VARIABLE,
        CLASS,
        INTERFACE,
        MODULE,
        PROPERTY,
        PARAMETER,
        UNIT,
        VALUE,
        ENUM,
        KEYWORD,
        SNIPPET,
        COLOR,
        FILE,
        REFERENCE,
        FOLDER,
        ENUM_MEMBER,
        CONSTANT,
        STRUCT,
        EVENT,
        OPERATOR,
        TYPE_PARAMETER
    }

    public AutoCompleteItem {
        additionalTextEdits = additionalTextEdits == null ? List.of() : List.copyOf(additionalTextEdits);
    }

    public AutoCompleteItem(String insertText, String label, String detail, String description, Icon icon, Kind kind, List<TextEdit> additionalTextEdits, boolean unused) {
        this(insertText, label, detail, description, icon, kind, additionalTextEdits, unused, null);
    }

    public AutoCompleteItem(String insertText, String label, String detail, String description, Icon icon, Kind kind, List<TextEdit> additionalTextEdits) {
        this(insertText, label, detail, description, icon, kind, additionalTextEdits, false);
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

    public AutoCompleteItem withUnused(boolean unused) {
        if (this.unused == unused) return this;
        return new AutoCompleteItem(insertText, label, detail, description, icon, kind, additionalTextEdits, unused, data);
    }

    public AutoCompleteItem withData(Object data) {
        return new AutoCompleteItem(insertText, label, detail, description, icon, kind, additionalTextEdits, unused, data);
    }

    public boolean isSnippet() {
        return kind == Kind.SNIPPET || (insertText != null && insertText.contains(CARET_MARKER));
    }

    public static boolean supportsCaretMarker() {
        return true;
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
