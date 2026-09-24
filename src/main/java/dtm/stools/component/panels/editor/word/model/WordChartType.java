package dtm.stools.component.panels.editor.word.model;

public enum WordChartType {
    COLUMN_CLUSTERED("Colunas agrupadas"),
    COLUMN_STACKED("Colunas empilhadas"),
    COLUMN_PERCENT("Colunas 100% empilhadas"),
    BAR_CLUSTERED("Barras agrupadas"),
    BAR_STACKED("Barras empilhadas"),
    LINE("Linhas"),
    LINE_MARKERS("Linhas com marcadores"),
    AREA("Área"),
    AREA_STACKED("Área empilhada"),
    PIE("Pizza"),
    DOUGHNUT("Rosca"),
    SCATTER("Dispersão"),
    RADAR("Radar");

    private final String displayName;
    WordChartType(String displayName) { this.displayName = displayName; }
    public String displayName() { return displayName; }
    public boolean isBar() { return name().startsWith("BAR"); }
    public boolean isColumnOrBar() { return name().startsWith("COLUMN") || isBar(); }
    public boolean isStacked() { return name().endsWith("STACKED") || this == COLUMN_PERCENT; }
    public boolean isPercent() { return this == COLUMN_PERCENT; }
    public boolean isCircular() { return this == PIE || this == DOUGHNUT; }
    public boolean isLine() { return this == LINE || this == LINE_MARKERS; }
    public boolean isArea() { return this == AREA || this == AREA_STACKED; }
    @Override public String toString() { return displayName; }
}
