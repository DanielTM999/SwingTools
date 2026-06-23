package dtm.stools.component.panels.editor.code.diagnostics;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

import java.util.List;

/**
 * Descreve uma edição pontual feita no documento, permitindo que um
 * {@link DiagnosticsProvider} produza diagnósticos de forma incremental em vez
 * de revalidar o texto inteiro.
 *
 * @param buffer              buffer atual (já refletindo a edição)
 * @param oldText             texto completo antes da edição
 * @param newText             texto completo após a edição
 * @param changeOffset        posição onde a edição começou
 * @param removedLength       quantidade de caracteres removidos a partir de {@code changeOffset}
 * @param insertedText        texto inserido em {@code changeOffset}
 * @param previousDiagnostics diagnósticos resultantes da última execução sobre {@code oldText}
 */
public record DiagnosticsChange(
        TextBuffer buffer,
        String oldText,
        String newText,
        int changeOffset,
        int removedLength,
        String insertedText,
        List<Diagnostic> previousDiagnostics
) {
}
