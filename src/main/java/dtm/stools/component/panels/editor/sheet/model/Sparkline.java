package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record Sparkline(CellAddress location, String dataRef, SparklineType type, int color, boolean markers, boolean highPoint, boolean lowPoint, boolean negativePoints) {
    public Sparkline { Objects.requireNonNull(location); Objects.requireNonNull(dataRef); type = Objects.requireNonNullElse(type, SparklineType.LINE); }

    public static Sparkline of(CellAddress location, String dataRef, SparklineType type) { return new Sparkline(location, dataRef, type, 0xFF376092, false, false, false, true); }
}
