package dtm.stools.component.panels.editor.code.listeners;

import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;

import java.util.List;

@FunctionalInterface
public interface DiagnosticsChangeListener {
    void onDiagnosticsChanged(List<Diagnostic> diagnostics);
}
