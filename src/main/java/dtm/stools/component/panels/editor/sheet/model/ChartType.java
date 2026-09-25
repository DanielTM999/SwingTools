package dtm.stools.component.panels.editor.sheet.model;

public enum ChartType {
    COLUMN("Colunas"), STACKED_COLUMN("Colunas empilhadas"), PERCENT_COLUMN("Colunas 100%"), BAR("Barras"), STACKED_BAR("Barras empilhadas"),
    LINE("Linhas"), LINE_MARKERS("Linhas com marcadores"), STACKED_LINE("Linhas empilhadas"), AREA("Área"), STACKED_AREA("Área empilhada"),
    PIE("Pizza"), DOUGHNUT("Rosca"), SCATTER("Dispersão"), SCATTER_LINES("Dispersão com linhas"), BUBBLE("Bolhas"), RADAR("Radar"), FILLED_RADAR("Radar preenchido"),
    COMBO("Combinação"), STOCK("Ações"), HISTOGRAM("Histograma"), PARETO("Pareto"), WATERFALL("Cascata"), BOX_WHISKER("Caixa estreita"), FUNNEL("Funil"),
    TREEMAP("Treemap"), SUNBURST("Explosão solar");

    private final String label;
    ChartType(String label) { this.label = label; }
    public String label() { return label; }
    public boolean circular() { return this == PIE || this == DOUGHNUT; }
    public boolean xy() { return this == SCATTER || this == SCATTER_LINES || this == BUBBLE; }
    public boolean horizontal() { return this == BAR || this == STACKED_BAR || this == FUNNEL; }
    public boolean stacked() { return this == STACKED_COLUMN || this == STACKED_BAR || this == STACKED_AREA || this == STACKED_LINE || this == PERCENT_COLUMN; }
}
