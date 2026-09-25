package dtm.stools.component.panels.editor.sheet.model;

public record BoolValue(boolean value) implements CellValue {
    @Override public String toString() { return value ? "TRUE" : "FALSE"; }
}
