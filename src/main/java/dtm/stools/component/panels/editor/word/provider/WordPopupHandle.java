package dtm.stools.component.panels.editor.word.provider;

public interface WordPopupHandle {
    boolean isOpen();
    void toFront();
    void close();
    static WordPopupHandle closed() {
        return new WordPopupHandle() {
            public boolean isOpen() { return false; }
            public void toFront() {}
            public void close() {}
        };
    }
}
