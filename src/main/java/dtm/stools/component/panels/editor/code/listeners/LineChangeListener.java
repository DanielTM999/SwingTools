package dtm.stools.component.panels.editor.code.listeners;

public interface LineChangeListener {
    void onLinesInserted(int atLine, int count);
    void onLinesRemoved(int atLine, int count);
}