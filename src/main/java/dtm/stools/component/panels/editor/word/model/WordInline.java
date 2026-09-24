package dtm.stools.component.panels.editor.word.model;

public interface WordInline {
    String text();
    WordTextStyle style();
    WordInline withStyle(WordTextStyle style);
    default int length() { return text().length(); }
}
