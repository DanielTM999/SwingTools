package dtm.stools.component.panels.editor.sheet.model;

public enum PivotAggregation {
    SUM("sum", "Soma"), COUNT("count", "Contagem"), AVERAGE("average", "Média"), MAX("max", "Máx"), MIN("min", "Mín"), PRODUCT("product", "Produto"),
    COUNT_NUMS("countNums", "Contar números"), STDDEV("stdDev", "DesvPad"), STDDEVP("stdDevp", "DesvPadP"), VAR("var", "Var"), VARP("varp", "VarP"),
    COUNT_DISTINCT("countDistinct", "Contagem distinta"), MEDIAN("median", "Mediana");

    private final String xml, label;
    PivotAggregation(String xml, String label) { this.xml = xml; this.label = label; }
    public String xml() { return xml; }
    public String label() { return label; }
    public static PivotAggregation fromXml(String v) { for (PivotAggregation a : values()) if (a.xml.equals(v)) return a; return SUM; }
}
