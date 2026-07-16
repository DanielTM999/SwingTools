package dtm.stools.component.panels.editor.code.diagnostics;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.List;

@FunctionalInterface
public interface DiagnosticsProvider extends CodeEditorProvider {

    List<Diagnostic> getDiagnostics(DiagnosticsContext context);

    default boolean supportsIncremental() {
        return false;
    }

    default List<Diagnostic> getDiagnostics(DiagnosticsChange change) {
        return getDiagnostics(new DiagnosticsContext(change.buffer()));
    }
}
