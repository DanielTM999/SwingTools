package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record FilterCondition(FilterOperator operator, String value) {
    public FilterCondition { Objects.requireNonNull(operator); value = Objects.requireNonNullElse(value, ""); }
}
