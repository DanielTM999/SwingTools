package dtm.stools.component.panels.editor.code.listeners;

import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;

import java.util.List;

/**
 * Notificado sempre que a lista de diagnósticos do editor é recalculada
 * (seja por um provider, seja ao ser limpa). Sempre disparado na EDT.
 */
@FunctionalInterface
public interface DiagnosticsChangeListener {
    void onDiagnosticsChanged(List<Diagnostic> diagnostics);
}
