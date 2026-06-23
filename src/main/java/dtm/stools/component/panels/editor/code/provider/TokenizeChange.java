package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.prototype.Token;

import java.util.Collection;

/**
 * Descreve uma edição pontual feita no documento, permitindo que um
 * {@link TokenizerCodeEditorProvider} faça tokenização incremental em vez de
 * reprocessar o texto inteiro.
 *
 * @param oldText        texto completo antes da edição
 * @param newText        texto completo após a edição
 * @param changeOffset   posição onde a edição começou
 * @param removedLength  quantidade de caracteres removidos a partir de {@code changeOffset}
 * @param insertedText   texto inserido em {@code changeOffset}
 * @param previousTokens tokens resultantes da última tokenização de {@code oldText}
 */
public record TokenizeChange(
        String oldText,
        String newText,
        int changeOffset,
        int removedLength,
        String insertedText,
        Collection<Token> previousTokens
) {
}
