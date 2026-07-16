package dtm.stools.component.panels.editor.code.provider;

@FunctionalInterface
public interface WordDetector extends CodeEditorProvider {

    boolean isWordChar(char c);

    static WordDetector defaultDetector() {
        return c -> Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }
}
