package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;

import java.util.List;

public record CalcStatistics(int evaluated, long nanos, List<String> circular, boolean pending) {
    public CalcStatistics { circular = List.copyOf(circular); }
    public double millis() { return nanos / 1_000_000.0; }
    public static String label(String sheet, CellAddress a) { return sheet + "!" + a.toA1(); }
}
