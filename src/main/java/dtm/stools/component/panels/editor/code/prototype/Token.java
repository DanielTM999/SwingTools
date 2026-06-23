package dtm.stools.component.panels.editor.code.prototype;

import dtm.stools.component.panels.editor.code.prototype.constants.TokenType;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Token {

    private final int startOffset;
    private final int endOffset;
    private final String type;
    private final String text;

    public Token(int startOffset, int endOffset, String type, String text) {
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.type = type;
        this.text = text;
    }

    public int length() {
        return endOffset - startOffset;
    }

}
