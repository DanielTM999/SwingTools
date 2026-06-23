package dtm.stools.component.panels.editor.code.search;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchOptions {

    protected boolean caseSensitive = false;
    protected boolean wholeWord = false;
    protected boolean regex = false;
    protected boolean wrapAround = true;

    public SearchOptions() {}

    public SearchOptions(boolean caseSensitive, boolean wholeWord, boolean regex, boolean wrapAround) {
        this.caseSensitive = caseSensitive;
        this.wholeWord = wholeWord;
        this.regex = regex;
        this.wrapAround = wrapAround;
    }
}
