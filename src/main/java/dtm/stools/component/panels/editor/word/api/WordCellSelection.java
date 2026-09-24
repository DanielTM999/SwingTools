package dtm.stools.component.panels.editor.word.api;

import java.util.Objects;
import java.util.UUID;

public record WordCellSelection(UUID tableId, int anchorRow, int anchorColumn, int focusRow, int focusColumn, WordSelection range) implements WordContentSelection {
    public WordCellSelection {
        Objects.requireNonNull(tableId); Objects.requireNonNull(range);
        if (anchorRow < 0 || anchorColumn < 0 || focusRow < 0 || focusColumn < 0) throw new IllegalArgumentException("Invalid cell selection");
    }
    public int firstRow() { return Math.min(anchorRow,focusRow); }
    public int lastRow() { return Math.max(anchorRow,focusRow); }
    public int firstColumn() { return Math.min(anchorColumn,focusColumn); }
    public int lastColumn() { return Math.max(anchorColumn,focusColumn); }
    public boolean contains(int row, int column) { return row >= firstRow() && row <= lastRow() && column >= firstColumn() && column <= lastColumn(); }
    public int rowCount() { return lastRow() - firstRow() + 1; }
    public int columnCount() { return lastColumn() - firstColumn() + 1; }
}
