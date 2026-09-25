package dtm.stools.component.panels.editor.sheet.provider;

public interface SheetPopupHandle {
    boolean isOpen();
    void toFront();
    void close();

    static SheetPopupHandle closed() {
        return new SheetPopupHandle() {
            @Override public boolean isOpen() { return false; }
            @Override public void toFront() { }
            @Override public void close() { }
        };
    }
}
