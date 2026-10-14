package dtm.stools.component.inputfields.textarea.diagnostic;

import lombok.With;

import java.util.List;
import java.util.Objects;

@With
public record TextDiagnostic(int start,
                             int end,
                             DiagnosticSeverity severity,
                             String message,
                             String source,
                             List<TextDiagnosticAction> actions) {

    public TextDiagnostic {
        if (start < 0) throw new IllegalArgumentException("start must be >= 0");
        if (end < start) throw new IllegalArgumentException("end must be >= start");
        Objects.requireNonNull(severity, "severity");
        message = message == null ? "" : message;
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    public TextDiagnostic(int start, int end, DiagnosticSeverity severity, String message) {
        this(start, end, severity, message, null, List.of());
    }

    public static TextDiagnostic error(int start, int end, String message) {
        return new TextDiagnostic(start, end, DiagnosticSeverity.ERROR, message);
    }

    public static TextDiagnostic warning(int start, int end, String message) {
        return new TextDiagnostic(start, end, DiagnosticSeverity.WARNING, message);
    }

    public static TextDiagnostic info(int start, int end, String message) {
        return new TextDiagnostic(start, end, DiagnosticSeverity.INFO, message);
    }

    public static TextDiagnostic hint(int start, int end, String message) {
        return new TextDiagnostic(start, end, DiagnosticSeverity.HINT, message);
    }

    public TextDiagnostic withRange(int start, int end) {
        return new TextDiagnostic(start, end, severity, message, source, actions);
    }

    public TextDiagnostic withActions(List<TextDiagnosticAction> actions) {
        return new TextDiagnostic(start, end, severity, message, source, actions);
    }

    public TextDiagnostic withActions(TextDiagnosticAction... actions) {
        return withActions(List.of(actions));
    }

    public int length() {
        return end - start;
    }

    public boolean contains(int offset) {
        return offset >= start && offset <= end;
    }
}
