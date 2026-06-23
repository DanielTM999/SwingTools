package dtm.stools.component.panels.editor.code.diagnostics;

import dtm.stools.component.panels.editor.code.provider.CodeEditorProvider;

import java.util.List;

@FunctionalInterface
public interface DiagnosticsProvider extends CodeEditorProvider {

    /**
     * Calcula os diagnósticos para o documento inteiro. Esse é o modo padrão e
     * sempre deve ser suportado.
     */
    List<Diagnostic> getDiagnostics(DiagnosticsContext context);

    /**
     * Indica se este provider sabe processar diagnósticos incrementais via
     * {@link #getDiagnostics(DiagnosticsChange)}. Quando {@code false}, o editor
     * sempre usa {@link #getDiagnostics(DiagnosticsContext)}.
     */
    default boolean supportsIncremental() {
        return false;
    }

    /**
     * Calcula os diagnósticos de forma incremental, recebendo a descrição da
     * edição feita ({@link DiagnosticsChange}) e os diagnósticos anteriores. A
     * implementação padrão delega para a versão completa, de modo que providers
     * existentes continuam funcionando sem alteração.
     */
    default List<Diagnostic> getDiagnostics(DiagnosticsChange change) {
        return getDiagnostics(new DiagnosticsContext(change.buffer()));
    }
}
