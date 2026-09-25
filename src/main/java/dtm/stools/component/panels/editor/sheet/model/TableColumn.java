package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record TableColumn(String name, TotalsFunction totals, String totalsLabel, String calculatedFormula) {
    public TableColumn {
        Objects.requireNonNull(name);
        totals = Objects.requireNonNullElse(totals, TotalsFunction.NONE);
    }

    public static TableColumn of(String name) { return new TableColumn(name, TotalsFunction.NONE, null, null); }
    public TableColumn withName(String n) { return new TableColumn(n, totals, totalsLabel, calculatedFormula); }
    public TableColumn withTotals(TotalsFunction t) { return new TableColumn(name, t, totalsLabel, calculatedFormula); }
    public TableColumn withFormula(String f) { return new TableColumn(name, totals, totalsLabel, f); }
}
