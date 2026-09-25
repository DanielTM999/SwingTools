package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.formula.BinaryNode;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.FormulaNode;
import dtm.stools.component.panels.editor.sheet.formula.FormulaParser;
import dtm.stools.component.panels.editor.sheet.formula.NameNode;
import dtm.stools.component.panels.editor.sheet.formula.NumberNode;
import dtm.stools.component.panels.editor.sheet.formula.ParenNode;
import dtm.stools.component.panels.editor.sheet.formula.UnaryNode;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.PivotAggregation;
import dtm.stools.component.panels.editor.sheet.model.PivotCalculatedField;
import dtm.stools.component.panels.editor.sheet.model.PivotField;
import dtm.stools.component.panels.editor.sheet.model.PivotGrouping;
import dtm.stools.component.panels.editor.sheet.model.PivotShowAs;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.PivotValueField;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PivotEngine {
    public record Result(List<List<CellValue>> grid, List<String> fields, Map<String, CellValue> index, int headerRows, int labelColumns) {
        public ArrayValue array() { return ArrayValue.of(grid); }
        public CellValue lookup(String dataField, Map<String, String> items) {
            StringBuilder b = new StringBuilder(dataField.toLowerCase(Locale.ROOT));
            List<String> keys = new ArrayList<>(items.keySet());
            keys.sort(String::compareTo);
            for (String k : keys) b.append('|').append(k.toLowerCase(Locale.ROOT)).append('=').append(items.get(k).toLowerCase(Locale.ROOT));
            return index.get(b.toString());
        }
    }

    private record Item(String label, CellValue value, double sortKey, boolean numeric) {}

    private final boolean date1904;
    private final Locale locale;

    public PivotEngine(boolean date1904, Locale locale) { this.date1904 = date1904; this.locale = locale; }

    public List<String> fields(ArrayValue source) {
        List<String> names = new ArrayList<>();
        for (int c = 0; c < source.columns(); c++) {
            String n = source.get(0, c).display();
            names.add(n.isBlank() ? "Coluna" + (c + 1) : n);
        }
        return names;
    }

    public Result compute(PivotTable pivot, ArrayValue source) {
        List<String> names = fields(source);
        Map<String, Integer> index = new HashMap<>();
        for (int k = 0; k < names.size(); k++) index.putIfAbsent(names.get(k).toLowerCase(Locale.ROOT), k);
        List<CellValue[]> records = new ArrayList<>();
        outer:
        for (int r = 1; r < source.rows(); r++) {
            CellValue[] rec = new CellValue[source.columns()];
            boolean empty = true;
            for (int c = 0; c < source.columns(); c++) { rec[c] = source.get(r, c); if (!rec[c].isEmpty()) empty = false; }
            if (empty) continue;
            for (PivotField f : pivot.filters()) if (hidden(f, rec, index)) continue outer;
            for (PivotField f : pivot.rows()) if (hidden(f, rec, index)) continue outer;
            for (PivotField f : pivot.columns()) if (hidden(f, rec, index)) continue outer;
            records.add(rec);
        }
        List<PivotValueField> values = pivot.values().isEmpty() ? List.of() : pivot.values();
        List<List<Item>> rowKeys = keys(pivot.rows(), records, index);
        List<List<Item>> colKeys = keys(pivot.columns(), records, index);
        Map<String, List<CellValue[]>> cells = new HashMap<>();
        for (CellValue[] rec : records) {
            String rk = keyString(itemsOf(pivot.rows(), rec, index)), ck = keyString(itemsOf(pivot.columns(), rec, index));
            cells.computeIfAbsent(rk + "\u0002" + ck, k -> new ArrayList<>()).add(rec);
            cells.computeIfAbsent(rk + "\u0002*", k -> new ArrayList<>()).add(rec);
            cells.computeIfAbsent("*\u0002" + ck, k -> new ArrayList<>()).add(rec);
            cells.computeIfAbsent("*\u0002*", k -> new ArrayList<>()).add(rec);
        }
        int valueCount = Math.max(1, values.size());
        List<String> colLabels = new ArrayList<>();
        List<String> colKeyStrings = new ArrayList<>();
        for (List<Item> ck : colKeys) { colKeyStrings.add(keyString(ck)); colLabels.add(label(ck)); }
        if (pivot.columnGrandTotals() || colKeys.isEmpty()) { colKeyStrings.add("*"); colLabels.add(colKeys.isEmpty() ? "" : "Total Geral"); }
        List<String> rowKeyStrings = new ArrayList<>();
        for (List<Item> rk : rowKeys) rowKeyStrings.add(keyString(rk));
        if (pivot.rowGrandTotals() || rowKeys.isEmpty()) rowKeyStrings.add("*");
        double[][][] raw = new double[rowKeyStrings.size()][colKeyStrings.size()][valueCount];
        boolean[][][] present = new boolean[rowKeyStrings.size()][colKeyStrings.size()][valueCount];
        for (int i = 0; i < rowKeyStrings.size(); i++) for (int j = 0; j < colKeyStrings.size(); j++) {
            List<CellValue[]> subset = cells.get(rowKeyStrings.get(i) + "\u0002" + colKeyStrings.get(j));
            for (int v = 0; v < values.size(); v++) {
                if (subset == null || subset.isEmpty()) continue;
                Double agg = aggregate(values.get(v), subset, index, pivot.calculated(), names);
                if (agg != null) { raw[i][j][v] = agg; present[i][j][v] = true; }
            }
        }
        for (int v = 0; v < values.size(); v++) applyShowAs(values.get(v).showAs(), raw, present, v, rowKeyStrings, colKeyStrings);
        List<List<CellValue>> grid = new ArrayList<>();
        Map<String, CellValue> lookup = new HashMap<>();
        int labelCols = Math.max(1, pivot.rows().size());
        List<CellValue> header = new ArrayList<>();
        if (pivot.rows().isEmpty()) header.add(CellValue.of(""));
        else for (PivotField f : pivot.rows()) header.add(CellValue.of(pivot.compact() && pivot.rows().size() == 1 ? "Rótulos de Linha" : f.name()));
        for (int j = 0; j < colKeyStrings.size(); j++) for (int v = 0; v < valueCount; v++) {
            String colLabel = colLabels.get(j);
            String caption = values.isEmpty() ? "" : values.get(v).caption();
            String text = colKeys.isEmpty() ? caption : values.size() > 1 ? (colLabel + " - " + caption).strip() : colLabel;
            header.add(CellValue.of(text));
        }
        if (!pivot.columns().isEmpty()) {
            List<CellValue> top = new ArrayList<>();
            for (int k = 0; k < labelCols; k++) top.add(CellValue.of(k == 0 ? (values.size() == 1 ? values.getFirst().caption() : "Valores") : ""));
            top.add(CellValue.of("Rótulos de Coluna"));
            while (top.size() < header.size()) top.add(CellValue.EMPTY);
            grid.add(top);
        }
        grid.add(header);
        for (int i = 0; i < rowKeyStrings.size(); i++) {
            List<CellValue> line = new ArrayList<>();
            boolean total = rowKeyStrings.get(i).equals("*");
            if (total) { line.add(CellValue.of("Total Geral")); for (int k = 1; k < labelCols; k++) line.add(CellValue.EMPTY); }
            else if (pivot.rows().isEmpty()) line.add(CellValue.EMPTY);
            else for (Item it : rowKeys.get(i)) line.add(it.value);
            for (int j = 0; j < colKeyStrings.size(); j++) for (int v = 0; v < valueCount; v++) {
                CellValue cell = present[i][j][v] ? CellValue.of(raw[i][j][v]) : CellValue.EMPTY;
                line.add(cell);
                if (!values.isEmpty()) {
                    Map<String, String> items = new LinkedHashMap<>();
                    if (!total) for (int f = 0; f < pivot.rows().size(); f++) items.put(pivot.rows().get(f).name(), rowKeys.get(i).get(f).label);
                    if (!colKeyStrings.get(j).equals("*")) for (int f = 0; f < pivot.columns().size(); f++) items.put(pivot.columns().get(f).name(), colKeys.get(j).get(f).label);
                    lookup.put(indexKey(values.get(v), items), cell);
                }
            }
            grid.add(line);
        }
        int width = grid.stream().mapToInt(List::size).max().orElse(1);
        for (List<CellValue> line : grid) while (line.size() < width) line.add(CellValue.EMPTY);
        return new Result(grid, names, lookup, pivot.columns().isEmpty() ? 1 : 2, labelCols);
    }

    private static String indexKey(PivotValueField v, Map<String, String> items) {
        StringBuilder b = new StringBuilder();
        List<String> keys = new ArrayList<>(items.keySet());
        keys.sort(String::compareTo);
        String base = v.caption().toLowerCase(Locale.ROOT);
        for (String k : keys) b.append('|').append(k.toLowerCase(Locale.ROOT)).append('=').append(items.get(k).toLowerCase(Locale.ROOT));
        return base + b;
    }

    public static String fieldIndexKey(String field, Map<String, String> items) {
        StringBuilder b = new StringBuilder(field.toLowerCase(Locale.ROOT));
        List<String> keys = new ArrayList<>(items.keySet());
        keys.sort(String::compareTo);
        for (String k : keys) b.append('|').append(k.toLowerCase(Locale.ROOT)).append('=').append(items.get(k).toLowerCase(Locale.ROOT));
        return b.toString();
    }

    private boolean hidden(PivotField f, CellValue[] rec, Map<String, Integer> index) {
        if (f.hiddenItems().isEmpty()) return false;
        Integer col = index.get(f.name().toLowerCase(Locale.ROOT));
        if (col == null) return false;
        return f.hiddenItems().contains(item(f, rec[col]).label);
    }

    private List<Item> itemsOf(List<PivotField> fields, CellValue[] rec, Map<String, Integer> index) {
        List<Item> list = new ArrayList<>();
        for (PivotField f : fields) {
            Integer col = index.get(f.name().toLowerCase(Locale.ROOT));
            list.add(col == null ? new Item("(vazio)", CellValue.of("(vazio)"), 0, false) : item(f, rec[col]));
        }
        return list;
    }

    private List<List<Item>> keys(List<PivotField> fields, List<CellValue[]> records, Map<String, Integer> index) {
        if (fields.isEmpty()) return List.of();
        Map<String, List<Item>> unique = new LinkedHashMap<>();
        for (CellValue[] rec : records) { List<Item> items = itemsOf(fields, rec, index); unique.putIfAbsent(keyString(items), items); }
        List<List<Item>> list = new ArrayList<>(unique.values());
        list.sort((a, b) -> {
            for (int k = 0; k < a.size(); k++) {
                Item x = a.get(k), y = b.get(k);
                int c = x.numeric && y.numeric ? Double.compare(x.sortKey, y.sortKey) : x.numeric != y.numeric ? (x.numeric ? -1 : 1) : x.label.compareToIgnoreCase(y.label);
                if (fields.get(k).descending()) c = -c;
                if (c != 0) return c;
            }
            return 0;
        });
        return list;
    }

    private Item item(PivotField f, CellValue v) {
        if (v.isEmpty()) return new Item("(vazio)", CellValue.of("(vazio)"), Double.MAX_VALUE, false);
        if (v instanceof NumberValue n) {
            double d = n.value();
            PivotGrouping g = f.grouping();
            if (g == PivotGrouping.NUMBER_RANGE && f.groupSize() > 0) {
                double start = Math.floor(d / f.groupSize()) * f.groupSize();
                String label = NumberValue.general(start) + "-" + NumberValue.general(start + f.groupSize() - (f.groupSize() >= 1 ? 1 : 0));
                return new Item(label, CellValue.of(label), start, true);
            }
            if (g == PivotGrouping.YEARS || g == PivotGrouping.QUARTERS || g == PivotGrouping.MONTHS || g == PivotGrouping.DAYS) {
                int[] p = DateSerial.parts(d, date1904);
                return switch (g) {
                    case YEARS -> new Item(String.valueOf(p[0]), CellValue.of(p[0]), p[0], true);
                    case QUARTERS -> { String l = "Trim" + ((p[1] - 1) / 3 + 1); yield new Item(l, CellValue.of(l), (p[1] - 1) / 3, true); }
                    case MONTHS -> { String l = java.time.Month.of(p[1]).getDisplayName(java.time.format.TextStyle.SHORT, locale).replace(".", ""); yield new Item(l, CellValue.of(l), p[1], true); }
                    default -> { String l = p[2] + "-" + java.time.Month.of(p[1]).getDisplayName(java.time.format.TextStyle.SHORT, locale).replace(".", ""); yield new Item(l, CellValue.of(l), p[1] * 100 + p[2], true); }
                };
            }
            return new Item(NumberValue.general(d), v, d, true);
        }
        String s = v.display();
        return new Item(s, v instanceof TextValue ? v : CellValue.of(s), 0, false);
    }

    private static String keyString(List<Item> items) {
        StringBuilder b = new StringBuilder();
        for (Item i : items) b.append(i.label.toLowerCase(Locale.ROOT)).append('\u0001');
        return b.toString();
    }

    private static String label(List<Item> items) {
        List<String> l = new ArrayList<>();
        for (Item i : items) l.add(i.label);
        return String.join(" - ", l);
    }

    private Double aggregate(PivotValueField field, List<CellValue[]> subset, Map<String, Integer> index, List<PivotCalculatedField> calculated, List<String> names) {
        for (PivotCalculatedField cf : calculated) if (cf.name().equalsIgnoreCase(field.field())) return calculatedValue(cf, subset, index);
        Integer col = index.get(field.field().toLowerCase(Locale.ROOT));
        if (col == null) return null;
        List<Double> nums = new ArrayList<>();
        int count = 0;
        Set<String> distinct = new HashSet<>();
        for (CellValue[] rec : subset) {
            CellValue v = rec[col];
            if (v.isEmpty()) continue;
            count++;
            distinct.add(v.display().toLowerCase(Locale.ROOT));
            if (v instanceof NumberValue n) nums.add(n.value());
        }
        PivotAggregation a = field.aggregation();
        if (a == PivotAggregation.COUNT) return (double) count;
        if (a == PivotAggregation.COUNT_DISTINCT) return (double) distinct.size();
        if (a == PivotAggregation.COUNT_NUMS) return (double) nums.size();
        if (nums.isEmpty()) return a == PivotAggregation.SUM ? 0.0 : null;
        double sum = 0;
        for (double d : nums) sum += d;
        double mean = sum / nums.size();
        return switch (a) {
            case SUM -> sum;
            case AVERAGE -> mean;
            case MAX -> nums.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            case MIN -> nums.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            case PRODUCT -> nums.stream().mapToDouble(Double::doubleValue).reduce(1, (x, y) -> x * y);
            case MEDIAN -> { double[] s = nums.stream().mapToDouble(Double::doubleValue).sorted().toArray(); yield s.length % 2 == 1 ? s[s.length / 2] : (s[s.length / 2 - 1] + s[s.length / 2]) / 2; }
            case STDDEV, VAR -> { if (nums.size() < 2) yield null; double ss = 0; for (double d : nums) ss += (d - mean) * (d - mean); double var = ss / (nums.size() - 1); yield a == PivotAggregation.VAR ? var : Math.sqrt(var); }
            case STDDEVP, VARP -> { double ss = 0; for (double d : nums) ss += (d - mean) * (d - mean); double var = ss / nums.size(); yield a == PivotAggregation.VARP ? var : Math.sqrt(var); }
            default -> sum;
        };
    }

    private Double calculatedValue(PivotCalculatedField cf, List<CellValue[]> subset, Map<String, Integer> index) {
        FormulaNode node;
        try { node = FormulaParser.parse(cf.formula(), FormulaLocale.EN); } catch (RuntimeException e) {
            try { node = FormulaParser.parse(cf.formula(), FormulaLocale.PT_BR); } catch (RuntimeException e2) { return null; }
        }
        return calc(node, subset, index);
    }

    private Double calc(FormulaNode node, List<CellValue[]> subset, Map<String, Integer> index) {
        return switch (node) {
            case NumberNode n -> n.value();
            case NameNode n -> {
                Integer col = index.get(n.name().toLowerCase(Locale.ROOT).replace("_", " "));
                if (col == null) col = index.get(n.name().toLowerCase(Locale.ROOT));
                if (col == null) yield null;
                double s = 0;
                for (CellValue[] rec : subset) if (rec[col] instanceof NumberValue nv) s += nv.value();
                yield s;
            }
            case ParenNode p -> calc(p.inner(), subset, index);
            case UnaryNode u -> { Double v = calc(u.operand(), subset, index); yield v == null ? null : u.operator().equals("-") ? -v : u.operator().equals("%") ? v / 100 : v; }
            case BinaryNode b -> {
                Double l = calc(b.left(), subset, index), r = calc(b.right(), subset, index);
                if (l == null || r == null) yield null;
                yield switch (b.operator()) {
                    case "+" -> l + r;
                    case "-" -> l - r;
                    case "*" -> l * r;
                    case "/" -> r == 0 ? null : l / r;
                    case "^" -> Math.pow(l, r);
                    default -> null;
                };
            }
            default -> null;
        };
    }

    private static void applyShowAs(PivotShowAs mode, double[][][] raw, boolean[][][] present, int v, List<String> rows, List<String> cols) {
        if (mode == PivotShowAs.NORMAL) return;
        int nr = raw.length, nc = nr == 0 ? 0 : raw[0].length;
        int totalRow = rows.indexOf("*"), totalCol = cols.indexOf("*");
        double grand = totalRow >= 0 && totalCol >= 0 ? raw[totalRow][totalCol][v] : 0;
        double[][] copy = new double[nr][nc];
        for (int i = 0; i < nr; i++) for (int j = 0; j < nc; j++) copy[i][j] = raw[i][j][v];
        for (int i = 0; i < nr; i++) for (int j = 0; j < nc; j++) {
            if (!present[i][j][v]) continue;
            double x = copy[i][j];
            raw[i][j][v] = switch (mode) {
                case PERCENT_OF_GRAND_TOTAL -> grand == 0 ? 0 : x / grand;
                case PERCENT_OF_COLUMN -> totalRow < 0 || copy[totalRow][j] == 0 ? 0 : x / copy[totalRow][j];
                case PERCENT_OF_ROW -> totalCol < 0 || copy[i][totalCol] == 0 ? 0 : x / copy[i][totalCol];
                case RUNNING_TOTAL -> { if (rows.get(i).equals("*")) yield x; double s = 0; for (int k = 0; k <= i; k++) if (!rows.get(k).equals("*") && present[k][j][v]) s += copy[k][j]; yield s; }
                case DIFFERENCE_FROM_PREVIOUS -> i == 0 || rows.get(i).equals("*") ? 0 : x - copy[i - 1][j];
                case RANK_ASCENDING, RANK_DESCENDING -> {
                    if (rows.get(i).equals("*")) yield x;
                    int rank = 1;
                    for (int k = 0; k < nr; k++) if (!rows.get(k).equals("*") && present[k][j][v] && (mode == PivotShowAs.RANK_ASCENDING ? copy[k][j] < x : copy[k][j] > x)) rank++;
                    yield rank;
                }
                default -> x;
            };
        }
    }

    public static boolean sameShape(List<List<CellValue>> a, List<List<CellValue>> b) { return a.size() == b.size() && Objects.equals(a.isEmpty() ? 0 : a.getFirst().size(), b.isEmpty() ? 0 : b.getFirst().size()); }
    static String join(double[] d) { return Arrays.toString(d); }
    static Set<String> ordered() { return new LinkedHashSet<>(); }
}
