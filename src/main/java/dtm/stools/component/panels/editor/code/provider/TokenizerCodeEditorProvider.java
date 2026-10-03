package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.prototype.Token;

import java.util.Collection;

public interface TokenizerCodeEditorProvider extends CodeEditorProvider {

    /** Executado em thread de fundo pelo editor. */
    Collection<Token> tokenize(String text, TokenClassifierCodeEditorProvider tokenClassifierProvider);

    default boolean supportsIncremental() {
        return false;
    }

    /** Executado em thread de fundo pelo editor. */
    default Collection<Token> tokenize(TokenizeChange change,
                                       TokenClassifierCodeEditorProvider tokenClassifierProvider) {
        return tokenize(change.newText(), tokenClassifierProvider);
    }
}
