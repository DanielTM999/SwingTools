package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record PivotValueField(String field, PivotAggregation aggregation, PivotShowAs showAs, String caption, String numberFormat) {
    public PivotValueField {
        Objects.requireNonNull(field);
        aggregation = Objects.requireNonNullElse(aggregation, PivotAggregation.SUM);
        showAs = Objects.requireNonNullElse(showAs, PivotShowAs.NORMAL);
        caption = caption == null || caption.isBlank() ? aggregation.label() + " de " + field : caption;
    }

    public static PivotValueField of(String field, PivotAggregation aggregation) { return new PivotValueField(field, aggregation, PivotShowAs.NORMAL, null, null); }
}
