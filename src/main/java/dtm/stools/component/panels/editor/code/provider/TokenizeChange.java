package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.prototype.Token;

import java.util.Collection;

public record TokenizeChange(
        String oldText,
        String newText,
        int changeOffset,
        int removedLength,
        String insertedText,
        Collection<Token> previousTokens
) {
}
