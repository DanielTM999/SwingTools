package dtm.stools.component.panels.editor.code.multicaret;

public final class Caret {

    public int line;
    public int col;
    public int anchorLine = -1;
    public int anchorCol = -1;
    public int desiredCol = -1;

    public Caret(int line, int col) {
        this.line = line;
        this.col = col;
    }

    public static Caret of(int line, int col) {
        return new Caret(line, col);
    }

    public Caret copy() {
        Caret c = new Caret(line, col);
        c.anchorLine = anchorLine;
        c.anchorCol = anchorCol;
        c.desiredCol = desiredCol;
        return c;
    }

    public boolean hasSelection() {
        return anchorLine >= 0 && (anchorLine != line || anchorCol != col);
    }

    public void startSelectionIfNeeded() {
        if (anchorLine < 0) {
            anchorLine = line;
            anchorCol = col;
        }
    }

    public void clearSelection() {
        anchorLine = -1;
        anchorCol = -1;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Caret c)) return false;
        return c.line == line && c.col == col;
    }

    @Override
    public int hashCode() {
        return line * 31 + col;
    }
}
