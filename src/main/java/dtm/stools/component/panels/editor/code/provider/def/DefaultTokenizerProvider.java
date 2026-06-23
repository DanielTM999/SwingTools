package dtm.stools.component.panels.editor.code.provider.def;

import dtm.stools.component.panels.editor.code.prototype.Token;
import dtm.stools.component.panels.editor.code.prototype.constants.TokenType;
import dtm.stools.component.panels.editor.code.provider.TokenClassifierCodeEditorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenizerCodeEditorProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DefaultTokenizerProvider implements TokenizerCodeEditorProvider {

    @Override
    public Collection<Token> tokenize(String text, TokenClassifierCodeEditorProvider classifier) {
        if (text == null) {
            return new ArrayList<>();
        }
        State state = new State(text);
        state.run(classifier);
        return state.tokens;
    }

    private static final class State {

        private final String text;
        private final int len;
        private final List<Token> tokens;
        private int i;

        private State(String text) {
            this.text = text;
            this.len = text.length();
            this.tokens = new ArrayList<>();
            this.i = 0;
        }

        private void run(TokenClassifierCodeEditorProvider classifier) {
            while (i < len) {
                char c = peek();

                if (c == '\n') {
                    int start = i;
                    i++;
                    tokens.add(new Token(start, i, "NEWLINE", "\n"));
                    continue;
                }

                if (Character.isWhitespace(c)) {
                    i++;
                    continue;
                }

                if (c == '"') {
                    tokenizeString(classifier);
                    continue;
                }

                if (Character.isDigit(c)) {
                    tokenizeNumber(classifier);
                    continue;
                }

                if (c == ';') {
                    tokenizeComment(classifier);
                    continue;
                }

                if (isIdentifierStart(c)) {
                    tokenizeIdentifier(classifier);
                    continue;
                }

                tokenizeSymbol();
            }
        }

        private void tokenizeString(TokenClassifierCodeEditorProvider classifier) {
            int start = i++;
            while (i < len) {
                char c = peek();

                if (c == '\\' && i + 1 < len) {
                    i += 2;
                    continue;
                }

                if (c == '"') {
                    i++;
                    break;
                }

                i++;
            }

            addToken(start, i, TokenType.STRING, classifier);
        }

        private void tokenizeNumber(TokenClassifierCodeEditorProvider classifier) {
            int start = i;

            while (i < len && Character.isDigit(peek())) {
                i++;
            }

            addToken(start, i, TokenType.NUMBER, classifier);
        }

        private void tokenizeIdentifier(TokenClassifierCodeEditorProvider classifier) {
            int start = i;

            while (i < len && isIdentifierChar(peek())) {
                i++;
            }

            addToken(start, i, TokenType.IDENTIFIER, classifier);
        }

        private void tokenizeComment(TokenClassifierCodeEditorProvider classifier) {
            int start = i;

            while (i < len && peek() != '\n') {
                i++;
            }

            addToken(start, i, TokenType.COMMENT, classifier);
        }

        private void tokenizeSymbol() {
            int start = i;
            char c = text.charAt(i);
            i++;

            tokens.add(new Token(start, start + 1, TokenType.SYMBOL, String.valueOf(c)));
        }

        private char peek() {
            return text.charAt(i);
        }

        private void addToken(int start, int end,
                              String type,
                              TokenClassifierCodeEditorProvider classifier) {

            String value = text.substring(start, end);

            if (classifier != null) {
                String classified = classifier.classify(value);
                if (classified != null) {
                    type = classified;
                }
            }

            tokens.add(new Token(start, end, type, value));
        }

        private boolean isIdentifierStart(char c) {
            return Character.isLetter(c) || c == '_';
        }

        private boolean isIdentifierChar(char c) {
            return Character.isLetterOrDigit(c) || c == '_';
        }

    }
}
