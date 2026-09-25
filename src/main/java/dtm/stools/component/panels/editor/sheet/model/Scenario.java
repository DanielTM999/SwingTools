package dtm.stools.component.panels.editor.sheet.model;

import java.util.Map;
import java.util.Objects;

public record Scenario(String name, Map<CellAddress, CellValue> values, String comment) {
    public Scenario { Objects.requireNonNull(name); values = Map.copyOf(values); comment = Objects.requireNonNullElse(comment, ""); }
}
