package dtm.stools.component.panels.editor.code.listeners;

public interface DocumentEditListener {
    default void onInsert(int offset, String text) {}
    default void onDelete(int offset, String removed) {}
    default void onTextChanged() {}
}