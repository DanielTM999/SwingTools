package dtm.stools.component.panels.editor.code.provider;

import dtm.stools.component.panels.editor.code.provider.def.DefaultBracketMatcher;

public interface BracketMatcher extends CodeEditorProvider {

    int findMatch(String buffer, int offset);

    default char[][] pairs() {
        return new char[][]{{'(', ')'}, {'[', ']'}, {'{', '}'}};
    }

    static BracketMatcher defaultMatcher() {
        return DefaultBracketMatcher.INSTANCE;
    }
}
