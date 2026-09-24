package dtm.stools.component.panels.editor.word.provider;

public record WordSearchOptions(boolean matchCase, boolean wholeWord, boolean regex) {
    public static final WordSearchOptions DEFAULT = new WordSearchOptions(false,false,false);
}
