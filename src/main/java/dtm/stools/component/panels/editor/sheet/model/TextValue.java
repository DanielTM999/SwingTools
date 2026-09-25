package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record TextValue(String value) implements CellValue {
    public TextValue { Objects.requireNonNull(value); }

    @Override public String toString() { return value; }
}
