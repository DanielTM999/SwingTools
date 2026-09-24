package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record WordTable(UUID id, List<WordTableRow> rows, List<Float> columnWidths, WordBorder border, Alignment alignment,
                        float cellPadding, String styleId, List<String> extras) implements WordBlock {
    public enum Alignment { LEFT, CENTER, RIGHT }
    public WordTable {
        Objects.requireNonNull(id); Objects.requireNonNull(border); Objects.requireNonNull(alignment);
        rows = List.copyOf(rows); columnWidths = List.copyOf(columnWidths);
        if (rows.isEmpty() || columnWidths.isEmpty()) throw new IllegalArgumentException("A table needs rows and columns");
        for (Float width : columnWidths) if (width == null || !Float.isFinite(width) || width < 4 || width > 14400) throw new IllegalArgumentException("Invalid column width");
        for (WordTableRow row : rows) if (row.gridColumns() > columnWidths.size()) throw new IllegalArgumentException("Row spans more columns than the grid");
        if (!Float.isFinite(cellPadding) || cellPadding < 0 || cellPadding > 72) throw new IllegalArgumentException("Invalid padding");
        if (styleId != null && styleId.isBlank()) styleId = null;
        extras = extras == null ? List.of() : List.copyOf(extras);
    }
    public static WordTable create(int rowCount, int columnCount, float totalWidth) {
        if (rowCount < 1 || columnCount < 1 || rowCount > 500 || columnCount > 63) throw new IllegalArgumentException("Invalid table size");
        List<WordTableRow> rows = new ArrayList<>();
        for (int r = 0; r < rowCount; r++) {
            List<WordTableCell> cells = new ArrayList<>();
            for (int c = 0; c < columnCount; c++) cells.add(WordTableCell.of(""));
            rows.add(WordTableRow.of(cells));
        }
        return new WordTable(UUID.randomUUID(),rows,Collections.nCopies(columnCount,Math.max(12,totalWidth/columnCount)),WordBorder.DEFAULT,Alignment.LEFT,5.4f,null,List.of());
    }
    public int gridColumns() { return columnWidths.size(); }
    public float width() { float w = 0; for (float c : columnWidths) w += c; return w; }
    public float columnX(int gridColumn) { float x = 0; for (int i = 0; i < gridColumn; i++) x += columnWidths.get(i); return x; }
    public float spanWidth(int gridColumn, int span) { float w = 0; for (int i = gridColumn; i < Math.min(columnWidths.size(),gridColumn+span); i++) w += columnWidths.get(i); return w; }
    public WordTable withRows(List<WordTableRow> value) { return new WordTable(id,value,columnWidths,border,alignment,cellPadding,styleId,extras); }
    public WordTable withColumnWidths(List<Float> value) { return new WordTable(id,rows,value,border,alignment,cellPadding,styleId,extras); }
    public WordTable withBorder(WordBorder value) { return new WordTable(id,rows,columnWidths,value,alignment,cellPadding,styleId,extras); }
    public WordTable withAlignment(Alignment value) { return new WordTable(id,rows,columnWidths,border,value,cellPadding,styleId,extras); }
    public WordTable withCellPadding(float value) { return new WordTable(id,rows,columnWidths,border,alignment,value,styleId,extras); }
    public WordTable withId(UUID value) { return new WordTable(value,rows,columnWidths,border,alignment,cellPadding,styleId,extras); }
    public WordTable withRow(int index, WordTableRow row) { List<WordTableRow> next = new ArrayList<>(rows); next.set(index,row); return withRows(next); }
    public WordTableCell cell(int row, int gridColumn) {
        WordTableRow r = rows.get(row); int index = r.cellAt(gridColumn); return index < 0 ? null : r.cells().get(index);
    }
    @Override public String plainText() {
        StringBuilder b = new StringBuilder();
        for (WordTableRow row : rows) {
            if (!b.isEmpty()) b.append('\n');
            for (int i = 0; i < row.cells().size(); i++) { if (i > 0) b.append('\t'); b.append(row.cells().get(i).plainText().replace('\n',' ')); }
        }
        return b.toString();
    }
}
