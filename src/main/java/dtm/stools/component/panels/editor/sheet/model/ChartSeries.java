package dtm.stools.component.panels.editor.sheet.model;

public record ChartSeries(String name, String nameRef, String categoriesRef, String valuesRef, String sizesRef, Integer color, ChartType type, boolean secondaryAxis) {
    public static ChartSeries of(String name, String categoriesRef, String valuesRef) { return new ChartSeries(name, null, categoriesRef, valuesRef, null, null, null, false); }
    public ChartSeries withColor(Integer c) { return new ChartSeries(name, nameRef, categoriesRef, valuesRef, sizesRef, c, type, secondaryAxis); }
    public ChartSeries withType(ChartType t) { return new ChartSeries(name, nameRef, categoriesRef, valuesRef, sizesRef, color, t, secondaryAxis); }
    public ChartSeries withName(String n) { return new ChartSeries(n, nameRef, categoriesRef, valuesRef, sizesRef, color, type, secondaryAxis); }
}
