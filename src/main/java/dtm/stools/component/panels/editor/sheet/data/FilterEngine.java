package dtm.stools.component.panels.editor.sheet.data;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.model.AutoFilter;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.FilterCondition;
import dtm.stools.component.panels.editor.sheet.model.FilterCriteria;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

public final class FilterEngine {
    private final CalcEngine engine;
    private final NumberFormatter formatter;

    public FilterEngine(CalcEngine engine, NumberFormatter formatter) { this.engine = engine; this.formatter = formatter; }

    public String displayText(int sheet, int row, int column) {
        CellValue v = engine.valueAt(sheet, row, column);
        CellStyle style = engine.workbook().style(engine.workbook().sheet(sheet).cell(row, column).style());
        return formatter.text(v, style.numberFormat());
    }

    public List<String> distinctValues(int sheet, AutoFilter filter, int column) {
        CellRange r = filter.range();
        Set<String> values = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        int last = Math.min(r.lastRow(), lastRow(sheet, r));
        for (int row = r.firstRow() + 1; row <= last; row++) values.add(displayText(sheet, row, column));
        return new ArrayList<>(values);
    }

    private int lastRow(int sheet, CellRange r) {
        CellRange used = engine.usedRange(sheet);
        return used == null ? r.firstRow() : used.lastRow();
    }

    public Set<Integer> hiddenRows(int sheet, AutoFilter filter) {
        Set<Integer> hidden = new LinkedHashSet<>();
        if (filter == null || filter.criteria().isEmpty()) return hidden;
        CellRange r = filter.range();
        int last = Math.min(r.lastRow(), lastRow(sheet, r));
        for (Map.Entry<Integer, FilterCriteria> e : filter.criteria().entrySet()) {
            int col = e.getKey();
            FilterCriteria c = e.getValue();
            double[] numbers = null;
            if (c.top() != null || "aboveAverage".equals(c.dynamic()) || "belowAverage".equals(c.dynamic())) {
                List<Double> list = new ArrayList<>();
                for (int row = r.firstRow() + 1; row <= last; row++) if (engine.valueAt(sheet, row, col) instanceof NumberValue n) list.add(n.value());
                numbers = list.stream().mapToDouble(Double::doubleValue).sorted().toArray();
            }
            for (int row = r.firstRow() + 1; row <= last; row++) if (!matches(sheet, row, col, c, numbers)) hidden.add(row);
        }
        return hidden;
    }

    boolean matches(int sheet, int row, int column, FilterCriteria c, double[] sortedNumbers) {
        CellValue v = engine.valueAt(sheet, row, column);
        String text = displayText(sheet, row, column);
        if (c.values() != null) {
            if (text.isEmpty()) return c.includeBlanks();
            boolean found = false;
            for (String s : c.values()) if (s.equalsIgnoreCase(text)) { found = true; break; }
            if (!found) return false;
        }
        if (!c.conditions().isEmpty()) {
            boolean result = c.and();
            for (FilterCondition cond : c.conditions()) {
                boolean m = condition(v, text, cond);
                result = c.and() ? result && m : result || m;
            }
            if (!result) return false;
        }
        if (c.top() != null) {
            if (!(v instanceof NumberValue n) || sortedNumbers.length == 0) return false;
            int k = c.topPercent() ? Math.max(1, (int) Math.round(sortedNumbers.length * c.top() / 100.0)) : c.top();
            k = Math.min(k, sortedNumbers.length);
            double threshold = c.bottom() ? sortedNumbers[k - 1] : sortedNumbers[sortedNumbers.length - k];
            if (c.bottom() ? n.value() > threshold : n.value() < threshold) return false;
        }
        if (c.color() != null) {
            CellStyle style = engine.workbook().style(engine.workbook().sheet(sheet).cell(row, column).style());
            Integer actual = c.fontColor() ? style.fontColor() : style.fill().primaryColor();
            if (!Objects.equals(actual, c.color())) return false;
        }
        if (c.dynamic() != null) {
            switch (c.dynamic()) {
                case "aboveAverage" -> { if (!(v instanceof NumberValue n) || n.value() <= Arrays.stream(sortedNumbers).average().orElse(0)) return false; }
                case "belowAverage" -> { if (!(v instanceof NumberValue n) || n.value() >= Arrays.stream(sortedNumbers).average().orElse(0)) return false; }
                default -> { }
            }
        }
        return true;
    }

    private boolean condition(CellValue v, String text, FilterCondition cond) {
        String operand = cond.value();
        Double number = ValueParser.parseNumberLenient(operand);
        String lt = text.toLowerCase(Locale.ROOT), lo = operand.toLowerCase(Locale.ROOT);
        return switch (cond.operator()) {
            case EQUAL -> number != null && v instanceof NumberValue n ? n.value() == number : lt.equals(lo) || wildcard(lo).matcher(lt).matches();
            case NOT_EQUAL -> !(number != null && v instanceof NumberValue n ? n.value() == number : lt.equals(lo));
            case GREATER -> compare(v, text, operand, number) > 0;
            case GREATER_OR_EQUAL -> compare(v, text, operand, number) >= 0;
            case LESS -> compare(v, text, operand, number) < 0;
            case LESS_OR_EQUAL -> compare(v, text, operand, number) <= 0;
            case BEGINS_WITH -> lt.startsWith(lo);
            case ENDS_WITH -> lt.endsWith(lo);
            case CONTAINS -> lt.contains(lo);
            case NOT_CONTAINS -> !lt.contains(lo);
        };
    }

    private static int compare(CellValue v, String text, String operand, Double number) {
        if (number != null && v instanceof NumberValue n) return Double.compare(n.value(), number);
        if (number != null) return v instanceof TextValue ? 1 : -1;
        return Coerce.compareText(text, operand);
    }

    private static java.util.regex.Pattern wildcard(String s) {
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) b.append(c == '*' ? ".*" : c == '?' ? "." : java.util.regex.Pattern.quote(String.valueOf(c)));
        return java.util.regex.Pattern.compile(b.toString());
    }

    public static CellRange detectRegion(SheetWorksheet ws, int row, int column) {
        int top = row, bottom = row, left = column, right = column;
        boolean grew = true;
        while (grew) {
            grew = false;
            if (top > 0 && rowHasContent(ws, top - 1, left, right)) { top--; grew = true; }
            if (bottom < 1_048_575 && rowHasContent(ws, bottom + 1, left, right)) { bottom++; grew = true; }
            if (left > 0 && columnHasContent(ws, left - 1, top, bottom)) { left--; grew = true; }
            if (right < 16_383 && columnHasContent(ws, right + 1, top, bottom)) { right++; grew = true; }
        }
        return new CellRange(top, left, bottom, right);
    }

    private static boolean rowHasContent(SheetWorksheet ws, int row, int from, int to) {
        for (int c = Math.max(0, from - 1); c <= Math.min(16_383, to + 1); c++) if (ws.cell(row, c).hasContent()) return true;
        return false;
    }

    private static boolean columnHasContent(SheetWorksheet ws, int column, int from, int to) {
        for (int r = Math.max(0, from - 1); r <= Math.min(1_048_575, to + 1); r++) if (ws.cell(r, column).hasContent()) return true;
        return false;
    }
}
