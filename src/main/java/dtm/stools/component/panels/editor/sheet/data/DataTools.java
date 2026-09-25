package dtm.stools.component.panels.editor.sheet.data;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.command.SheetOperations;
import dtm.stools.component.panels.editor.sheet.command.SheetTransaction;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.formula.FormulaPrinter;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.Scenario;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.TotalsFunction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;

public final class DataTools {
    public record GoalSeekResult(boolean converged, double input, double output, int iterations) {}

    private DataTools() {}

    public static int removeDuplicates(SheetTransaction tx, CalcEngine engine, int sheet, CellRange range, List<Integer> columns, boolean hasHeader) {
        int start = range.firstRow() + (hasHeader ? 1 : 0);
        Set<String> seen = new HashSet<>();
        List<Integer> keep = new ArrayList<>();
        for (int r = start; r <= range.lastRow(); r++) {
            StringBuilder key = new StringBuilder();
            for (int c : columns) key.append(engine.valueAt(sheet, r, c).display().toLowerCase(Locale.ROOT)).append('\u0001');
            if (seen.add(key.toString())) keep.add(r);
        }
        int removed = (range.lastRow() - start + 1) - keep.size();
        if (removed == 0) return 0;
        List<SheetCell[]> rows = new ArrayList<>();
        for (int r : keep) {
            SheetCell[] line = new SheetCell[range.columnCount()];
            for (int c = 0; c < line.length; c++) line[c] = tx.cell(sheet, r, range.firstColumn() + c);
            rows.add(line);
        }
        for (int r = start; r <= range.lastRow(); r++) {
            int idx = r - start;
            for (int c = 0; c < range.columnCount(); c++) tx.setCell(sheet, r, range.firstColumn() + c, idx < rows.size() ? rows.get(idx)[c] : null);
        }
        return removed;
    }

    public static void textToColumns(SheetTransaction tx, int sheet, CellRange column, List<String> delimiters, boolean treatConsecutive, char qualifier, ValueParser parser) {
        int col = column.firstColumn();
        for (int r = column.firstRow(); r <= column.lastRow(); r++) {
            SheetCell cell = tx.cell(sheet, r, col);
            if (!cell.value().isText()) continue;
            List<String> parts = split(cell.value().display(), delimiters, treatConsecutive, qualifier);
            for (int k = 0; k < parts.size(); k++) {
                CellValue v = parser.parse(parts.get(k)).value();
                SheetCell existing = tx.cell(sheet, r, col + k);
                tx.setCell(sheet, r, col + k, new SheetCell(v, null, k == 0 ? cell.style() : existing.style()));
            }
        }
    }

    public static List<String> split(String s, List<String> delimiters, boolean consecutive, char qualifier) {
        List<String> out = new ArrayList<>();
        StringBuilder b = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == qualifier && qualifier != 0) { quoted = !quoted; continue; }
            boolean delim = false;
            if (!quoted) for (String d : delimiters) if (!d.isEmpty() && s.startsWith(d, i)) { delim = true; i += d.length() - 1; break; }
            if (delim) {
                if (!(consecutive && b.isEmpty() && !out.isEmpty())) out.add(b.toString());
                b.setLength(0);
            } else b.append(c);
        }
        out.add(b.toString());
        return out;
    }

    public static List<String> fixedWidth(String s, List<Integer> breaks) {
        List<String> out = new ArrayList<>();
        int prev = 0;
        for (int b : breaks) { if (b > s.length()) break; out.add(s.substring(prev, b).strip()); prev = b; }
        out.add(prev < s.length() ? s.substring(prev).strip() : "");
        return out;
    }

    public static GoalSeekResult goalSeek(DoubleUnaryOperator model, double target, double start) {
        double x = start, fx = model.applyAsDouble(x) - target;
        if (Math.abs(fx) < 1e-9) return new GoalSeekResult(true, x, fx + target, 0);
        double x1 = x == 0 ? 0.01 : x * 1.01, f1 = model.applyAsDouble(x1) - target;
        for (int k = 1; k <= 200; k++) {
            if (!Double.isFinite(f1) || f1 == fx) break;
            double next = x1 - f1 * (x1 - x) / (f1 - fx);
            x = x1; fx = f1; x1 = next;
            f1 = model.applyAsDouble(x1) - target;
            if (Math.abs(f1) < 1e-9 * Math.max(1, Math.abs(target))) return new GoalSeekResult(true, x1, f1 + target, k);
        }
        double lo = -1e6, hi = 1e6, flo = model.applyAsDouble(lo) - target, fhi = model.applyAsDouble(hi) - target;
        if (Double.isFinite(flo) && Double.isFinite(fhi) && flo * fhi < 0) {
            for (int k = 0; k < 300; k++) {
                double m = (lo + hi) / 2, fm = model.applyAsDouble(m) - target;
                if (Math.abs(fm) < 1e-9) return new GoalSeekResult(true, m, fm + target, 200 + k);
                if (flo * fm < 0) { hi = m; fhi = fm; } else { lo = m; flo = fm; }
            }
        }
        return new GoalSeekResult(false, x1, f1 + target, 200);
    }

    public static GoalSeekResult goalSeek(SheetTransaction tx, CalcEngine engine, int sheet, CellAddress formulaCell, double target, CellAddress changing) {
        SheetCell original = tx.cell(sheet, changing.row(), changing.column());
        double start = original.value() instanceof NumberValue n ? n.value() : 0;
        DoubleUnaryOperator model = x -> {
            tx.setCell(sheet, changing.row(), changing.column(), original.withValue(CellValue.of(x)));
            engine.cellsChanged(sheet, List.of(changing));
            CellValue v = engine.valueAt(sheet, formulaCell);
            return v instanceof NumberValue n ? n.value() : Double.NaN;
        };
        GoalSeekResult r = goalSeek(model, target, start);
        tx.setCell(sheet, changing.row(), changing.column(), original.withValue(CellValue.of(r.input())));
        return r;
    }

    public static void dataTable(SheetTransaction tx, CalcEngine engine, int sheet, CellRange table, CellAddress rowInput, CellAddress columnInput) {
        boolean twoWay = rowInput != null && columnInput != null;
        Map<CellAddress, SheetCell> originals = new LinkedHashMap<>();
        if (rowInput != null) originals.put(rowInput, tx.cell(sheet, rowInput.row(), rowInput.column()));
        if (columnInput != null) originals.put(columnInput, tx.cell(sheet, columnInput.row(), columnInput.column()));
        List<Object[]> results = new ArrayList<>();
        for (int r = table.firstRow() + 1; r <= table.lastRow(); r++) {
            for (int c = table.firstColumn() + 1; c <= table.lastColumn(); c++) {
                if (columnInput != null) set(tx, engine, sheet, columnInput, engine.valueAt(sheet, r, table.firstColumn()));
                if (rowInput != null) set(tx, engine, sheet, rowInput, engine.valueAt(sheet, table.firstRow(), c));
                CellAddress formula = twoWay ? table.first() : columnInput != null ? new CellAddress(table.firstRow(), c) : new CellAddress(r, table.firstColumn());
                results.add(new Object[]{r, c, engine.valueAt(sheet, formula)});
            }
        }
        for (Map.Entry<CellAddress, SheetCell> e : originals.entrySet()) { tx.setCell(sheet, e.getKey().row(), e.getKey().column(), e.getValue()); engine.cellsChanged(sheet, List.of(e.getKey())); }
        for (Object[] res : results) {
            SheetCell existing = tx.cell(sheet, (int) res[0], (int) res[1]);
            tx.setCell(sheet, (int) res[0], (int) res[1], new SheetCell((CellValue) res[2], null, existing.style()));
        }
    }

    private static void set(SheetTransaction tx, CalcEngine engine, int sheet, CellAddress a, CellValue v) {
        tx.setCell(sheet, a.row(), a.column(), tx.cell(sheet, a.row(), a.column()).withValue(v));
        engine.cellsChanged(sheet, List.of(a));
    }

    public static void applyScenario(SheetTransaction tx, int sheet, Scenario scenario) {
        for (Map.Entry<CellAddress, CellValue> e : scenario.values().entrySet()) {
            SheetCell existing = tx.cell(sheet, e.getKey().row(), e.getKey().column());
            tx.setCell(sheet, e.getKey().row(), e.getKey().column(), existing.withValue(e.getValue()));
        }
    }

    public static int subtotals(SheetTransaction tx, CalcEngine engine, int sheet, CellRange range, int groupColumn, TotalsFunction function, List<Integer> totalColumns, boolean hasHeader) {
        int start = range.firstRow() + (hasHeader ? 1 : 0);
        List<int[]> groups = new ArrayList<>();
        String current = null;
        int groupStart = start;
        for (int r = start; r <= range.lastRow() + 1; r++) {
            String key = r <= range.lastRow() ? engine.valueAt(sheet, r, groupColumn).display() : null;
            if (current != null && !current.equals(key)) groups.add(new int[]{groupStart, r - 1});
            if (current == null || !current.equals(key)) groupStart = r;
            current = key;
        }
        int inserted = 0;
        String sheetName = tx.sheet(sheet).name();
        List<int[]> placed = new ArrayList<>();
        for (int[] g : groups) {
            int s = g[0] + inserted, e = g[1] + inserted;
            SheetOperations.insertRows(tx, sheet, e + 1, 1);
            String label = engine.valueAt(sheet, g[0], groupColumn).display();
            tx.setCell(sheet, e + 1, groupColumn, SheetCell.of(CellValue.of("Total " + label)));
            for (int c : totalColumns) {
                String ref = new CellRange(s, c, e, c).toA1();
                tx.setCell(sheet, e + 1, c, SheetCell.formula("SUBTOTAL(" + function.subtotal() + "," + ref + ")"));
            }
            placed.add(new int[]{s, e + 1});
            inserted++;
        }
        int last = range.lastRow() + inserted + 1;
        SheetOperations.insertRows(tx, sheet, last, 1);
        tx.setCell(sheet, last, groupColumn, SheetCell.of(CellValue.of("Total Geral")));
        for (int c : totalColumns) tx.setCell(sheet, last, c, SheetCell.formula("SUBTOTAL(" + function.subtotal() + "," + new CellRange(start, c, last - 1, c).toA1() + ")"));
        for (int[] p : placed) group(tx, sheet, p[0], p[1] - 1, true, 1);
        return groups.size();
    }

    public static void group(SheetTransaction tx, int sheet, int from, int to, boolean rows, int delta) {
        tx.updateAxis(sheet, rows, axis -> { for (int i = from; i <= to; i++) axis.setOutlineLevel(i, Math.max(0, axis.outlineLevel(i) + delta)); });
    }

    public static void setCollapsed(SheetTransaction tx, int sheet, int from, int to, boolean rows, boolean collapsed) {
        tx.updateAxis(sheet, rows, axis -> { for (int i = from; i <= to; i++) axis.setHidden(i, collapsed); });
    }

    public static void consolidate(SheetTransaction tx, CalcEngine engine, int targetSheet, CellAddress target, List<Integer> sheets, List<CellRange> sources, TotalsFunction function, boolean useLabels) {
        if (!useLabels) {
            CellRange first = sources.getFirst();
            for (int i = 0; i < first.rowCount(); i++) for (int j = 0; j < first.columnCount(); j++) {
                List<Double> values = new ArrayList<>();
                for (int k = 0; k < sources.size(); k++) {
                    CellRange s = sources.get(k);
                    if (i < s.rowCount() && j < s.columnCount() && engine.valueAt(sheets.get(k), s.firstRow() + i, s.firstColumn() + j) instanceof NumberValue n) values.add(n.value());
                }
                tx.setCell(targetSheet, target.row() + i, target.column() + j, SheetCell.of(CellValue.of(aggregate(values, function))));
            }
            return;
        }
        Map<String, Map<String, List<Double>>> table = new LinkedHashMap<>();
        List<String> columns = new ArrayList<>();
        for (int k = 0; k < sources.size(); k++) {
            CellRange s = sources.get(k);
            int sh = sheets.get(k);
            for (int j = s.firstColumn() + 1; j <= s.lastColumn(); j++) { String h = engine.valueAt(sh, s.firstRow(), j).display(); if (!columns.contains(h)) columns.add(h); }
            for (int i = s.firstRow() + 1; i <= s.lastRow(); i++) {
                String label = engine.valueAt(sh, i, s.firstColumn()).display();
                for (int j = s.firstColumn() + 1; j <= s.lastColumn(); j++) {
                    String h = engine.valueAt(sh, s.firstRow(), j).display();
                    if (engine.valueAt(sh, i, j) instanceof NumberValue n) table.computeIfAbsent(label, x -> new LinkedHashMap<>()).computeIfAbsent(h, x -> new ArrayList<>()).add(n.value());
                }
            }
        }
        for (int j = 0; j < columns.size(); j++) tx.setCell(targetSheet, target.row(), target.column() + 1 + j, SheetCell.of(CellValue.of(columns.get(j))));
        int i = 1;
        for (Map.Entry<String, Map<String, List<Double>>> e : table.entrySet()) {
            tx.setCell(targetSheet, target.row() + i, target.column(), SheetCell.of(CellValue.of(e.getKey())));
            for (int j = 0; j < columns.size(); j++) {
                List<Double> vals = e.getValue().get(columns.get(j));
                if (vals != null) tx.setCell(targetSheet, target.row() + i, target.column() + 1 + j, SheetCell.of(CellValue.of(aggregate(vals, function))));
            }
            i++;
        }
    }

    static double aggregate(List<Double> v, TotalsFunction f) {
        if (v.isEmpty()) return 0;
        double sum = 0, max = Double.NEGATIVE_INFINITY, min = Double.POSITIVE_INFINITY;
        for (double d : v) { sum += d; max = Math.max(max, d); min = Math.min(min, d); }
        double mean = sum / v.size();
        return switch (f) {
            case AVERAGE -> mean;
            case COUNT, COUNTA -> v.size();
            case MAX -> max;
            case MIN -> min;
            case STDDEV, VAR -> {
                if (v.size() < 2) throw EvalError.div0();
                double ss = 0;
                for (double d : v) ss += (d - mean) * (d - mean);
                yield f == TotalsFunction.VAR ? ss / (v.size() - 1) : Math.sqrt(ss / (v.size() - 1));
            }
            default -> sum;
        };
    }

    static String quote(String sheet) { return FormulaPrinter.sheet(sheet); }
}
