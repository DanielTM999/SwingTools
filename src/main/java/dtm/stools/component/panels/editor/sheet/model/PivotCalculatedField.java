package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record PivotCalculatedField(String name, String formula) {
    public PivotCalculatedField { Objects.requireNonNull(name); Objects.requireNonNull(formula); }
}
