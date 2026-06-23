package dtm.stools.component.panels.editor.code.api;

@FunctionalInterface
public interface WordCaretChangeListener {
    void onWordCaretChanged(WordCaretChangeEvent event);
}
