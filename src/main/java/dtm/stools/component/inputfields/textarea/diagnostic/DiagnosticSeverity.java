package dtm.stools.component.inputfields.textarea.diagnostic;

public enum DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO,
    HINT;

    public boolean isMoreSevereThan(DiagnosticSeverity other) {
        return other == null || ordinal() < other.ordinal();
    }
}
