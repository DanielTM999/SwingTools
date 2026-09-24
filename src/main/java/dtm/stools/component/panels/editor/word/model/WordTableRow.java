package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record WordTableRow(UUID id, List<WordTableCell> cells, float height, boolean header, boolean cantSplit, List<String> extras) {
    public WordTableRow {
        Objects.requireNonNull(id); cells = List.copyOf(cells);
        if (cells.isEmpty()) throw new IllegalArgumentException("A table row must contain a cell");
        if (!Float.isFinite(height) || height < 0 || height > 14400) throw new IllegalArgumentException("Invalid row height");
        extras = extras == null ? List.of() : List.copyOf(extras);
    }
    public static WordTableRow of(List<WordTableCell> cells) { return new WordTableRow(UUID.randomUUID(),cells,0,false,false,List.of()); }
    public WordTableRow withCells(List<WordTableCell> value) { return new WordTableRow(id,value,height,header,cantSplit,extras); }
    public WordTableRow withHeight(float value) { return new WordTableRow(id,cells,value,header,cantSplit,extras); }
    public WordTableRow withHeader(boolean value) { return new WordTableRow(id,cells,height,value,cantSplit,extras); }
    public WordTableRow withCantSplit(boolean value) { return new WordTableRow(id,cells,height,header,value,extras); }
    public WordTableRow withId(UUID value) { return new WordTableRow(value,cells,height,header,cantSplit,extras); }
    public WordTableRow withCell(int index, WordTableCell cell) { List<WordTableCell> next = new ArrayList<>(cells); next.set(index,cell); return withCells(next); }
    public int gridColumns() { int n = 0; for (WordTableCell c : cells) n += c.gridSpan(); return n; }
    public int columnOf(int cellIndex) { int n = 0; for (int i = 0; i < cellIndex; i++) n += cells.get(i).gridSpan(); return n; }
    public int cellAt(int gridColumn) {
        int n = 0;
        for (int i = 0; i < cells.size(); i++) { n += cells.get(i).gridSpan(); if (gridColumn < n) return i; }
        return -1;
    }
}
