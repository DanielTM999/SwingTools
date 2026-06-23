package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.prototype.Token;

import java.util.Collection;

public interface TokenizerCodeEditorProvider extends CodeEditorProvider {

    /**
     * Tokeniza o documento inteiro. Esse é o modo padrão e sempre deve ser
     * suportado.
     */
    Collection<Token> tokenize(String text, TokenClassifierCodeEditorProvider tokenClassifierProvider);

    /**
     * Indica se este provider sabe processar tokenização incremental via
     * {@link #tokenize(TokenizeChange, TokenClassifierCodeEditorProvider)}.
     * Quando {@code false}, o editor sempre usa {@link #tokenize(String, TokenClassifierCodeEditorProvider)}.
     */
    default boolean supportsIncremental() {
        return false;
    }

    /**
     * Tokeniza o documento de forma incremental, recebendo apenas a descrição da
     * edição feita ({@link TokenizeChange}). A implementação padrão simplesmente
     * delega para a tokenização completa, de modo que providers existentes
     * continuam funcionando sem alteração.
     */
    default Collection<Token> tokenize(TokenizeChange change,
                                       TokenClassifierCodeEditorProvider tokenClassifierProvider) {
        return tokenize(change.newText(), tokenClassifierProvider);
    }
}
