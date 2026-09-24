package dtm.stools.component.panels.editor.word.model;

public record WordTableLocation(WordTable table, int row, int cell, int gridColumn) {
    public WordTableCell cellValue() { return table.rows().get(row).cells().get(cell); }
}
