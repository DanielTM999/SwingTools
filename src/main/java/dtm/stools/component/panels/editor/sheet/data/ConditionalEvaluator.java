package dtm.stools.component.panels.editor.sheet.data;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.formula.ReferenceAdjuster;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ConditionalFormat;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRule;
import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.model.Threshold;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ConditionalEvaluator {
    private record Stats(double[] sorted, double min, double max, double mean, double sd, Map<String, Integer> counts) {}

    private final CalcEngine engine;
    private final Map<ConditionalRule, Stats> stats = new IdentityHashMap<>();
    private final Map<String, CellValue> formulaCache = new HashMap<>();

    public ConditionalEvaluator(CalcEngine engine) { this.engine = engine; }

    public void invalidate() { stats.clear(); formulaCache.clear(); }

    public ConditionalResult evaluate(int sheet, int row, int column) {
        List<ConditionalFormat> formats = engine.workbook().sheet(sheet).properties().conditionalFormats();
        if (formats.isEmpty()) return ConditionalResult.NONE;
        CellValue value = engine.valueAt(sheet, row, column);
        DifferentialStyle style = null;
        Integer scale = null, barColor = null;
        Double bar = null;
        boolean gradient = false, hide = false;
        dtm.stools.component.panels.editor.sheet.model.IconSetType icons = null;
        int iconIndex = -1;
        List<ConditionalRule> ordered = new ArrayList<>();
        Map<ConditionalRule, ConditionalFormat> owner = new IdentityHashMap<>();
        for (ConditionalFormat f : formats) {
            if (!f.appliesTo(row, column)) continue;
            for (ConditionalRule r : f.rules()) { ordered.add(r); owner.put(r, f); }
        }
        ordered.sort((a, b) -> Integer.compare(a.priority(), b.priority()));
        for (ConditionalRule rule : ordered) {
            ConditionalFormat f = owner.get(rule);
            switch (rule.type()) {
                case COLOR_SCALE -> { if (scale == null && value instanceof NumberValue n) scale = colorScale(sheet, f, rule, n.value()); }
                case DATA_BAR -> {
                    if (bar == null && value instanceof NumberValue n) {
                        double[] mm = bounds(sheet, f, rule);
                        bar = mm[1] == mm[0] ? 1.0 : Math.max(0, Math.min(1, (n.value() - mm[0]) / (mm[1] - mm[0])));
                        barColor = rule.colors().isEmpty() ? 0xFF638EC6 : rule.colors().getFirst();
                        gradient = rule.gradient();
                        hide |= !rule.showValue();
                    }
                }
                case ICON_SET -> {
                    if (icons == null && value instanceof NumberValue n) {
                        icons = rule.iconSet();
                        iconIndex = iconIndex(sheet, f, rule, n.value());
                        hide |= !rule.showValue();
                    }
                }
                default -> {
                    if (matches(sheet, f, rule, row, column, value)) {
                        style = style == null ? rule.style() : merge(style, rule.style());
                        if (rule.stopIfTrue()) return new ConditionalResult(style, scale, bar, barColor, gradient, icons, iconIndex, hide);
                    }
                }
            }
        }
        if (style == null && scale == null && bar == null && icons == null) return ConditionalResult.NONE;
        return new ConditionalResult(style, scale, bar, barColor, gradient, icons, iconIndex, hide);
    }

    private static DifferentialStyle merge(DifferentialStyle first, DifferentialStyle next) {
        return new DifferentialStyle(first.fontColor() != null ? first.fontColor() : next.fontColor(), first.bold() != null ? first.bold() : next.bold(),
                first.italic() != null ? first.italic() : next.italic(), first.underline() != null ? first.underline() : next.underline(),
                first.strikethrough() != null ? first.strikethrough() : next.strikethrough(), first.fillColor() != null ? first.fillColor() : next.fillColor(),
                first.borderColor() != null ? first.borderColor() : next.borderColor(), first.numberFormat() != null ? first.numberFormat() : next.numberFormat());
    }

    private boolean matches(int sheet, ConditionalFormat f, ConditionalRule rule, int row, int column, CellValue value) {
        CellRange anchor = f.ranges().getFirst();
        int dr = row - anchor.firstRow(), dc = column - anchor.firstColumn();
        switch (rule.type()) {
            case CELL_VALUE -> {
                CellValue a = formula(sheet, rule.formula1(), dr, dc, row, column);
                CellValue b = rule.operator().twoOperands() ? formula(sheet, rule.formula2(), dr, dc, row, column) : CellValue.EMPTY;
                if (value instanceof ErrorValue || value.isEmpty() && !(a.isEmpty())) return false;
                try {
                    int ca = Coerce.compare(value, a), cb = rule.operator().twoOperands() ? Coerce.compare(value, b) : 0;
                    return switch (rule.operator()) {
                        case BETWEEN -> ca >= 0 && cb <= 0 || ca <= 0 && cb >= 0 && Coerce.compare(a, b) > 0;
                        case NOT_BETWEEN -> !(ca >= 0 && cb <= 0);
                        case EQUAL -> ca == 0;
                        case NOT_EQUAL -> ca != 0;
                        case GREATER -> ca > 0;
                        case LESS -> ca < 0;
                        case GREATER_OR_EQUAL -> ca >= 0;
                        case LESS_OR_EQUAL -> ca <= 0;
                    };
                } catch (RuntimeException e) { return false; }
            }
            case EXPRESSION -> {
                CellValue v = formula(sheet, rule.formula1(), dr, dc, row, column);
                try { return !(v instanceof ErrorValue) && Coerce.bool(v); } catch (RuntimeException e) { return false; }
            }
            case TOP_BOTTOM -> {
                if (!(value instanceof NumberValue n)) return false;
                Stats s = stats(sheet, f, rule);
                if (s.sorted.length == 0) return false;
                int k = rule.percent() ? Math.max(1, (int) Math.floor(s.sorted.length * rule.rank() / 100.0)) : Math.max(1, rule.rank());
                k = Math.min(k, s.sorted.length);
                return rule.bottom() ? n.value() <= s.sorted[k - 1] : n.value() >= s.sorted[s.sorted.length - k];
            }
            case ABOVE_AVERAGE -> {
                if (!(value instanceof NumberValue n)) return false;
                Stats s = stats(sheet, f, rule);
                double limit = s.mean + (rule.below() ? -1 : 1) * rule.stdDev() * s.sd;
                if (rule.below()) return rule.equalAverage() ? n.value() <= limit : n.value() < limit;
                return rule.equalAverage() ? n.value() >= limit : n.value() > limit;
            }
            case DUPLICATE, UNIQUE -> {
                if (value.isEmpty()) return false;
                Integer count = stats(sheet, f, rule).counts.get(key(value));
                return rule.type() == dtm.stools.component.panels.editor.sheet.model.ConditionalRuleType.DUPLICATE ? count != null && count > 1 : count != null && count == 1;
            }
            case CONTAINS_TEXT -> { return text(value).contains(rule.text().toLowerCase(Locale.ROOT)); }
            case NOT_CONTAINS_TEXT -> { return !text(value).contains(rule.text().toLowerCase(Locale.ROOT)); }
            case BEGINS_WITH -> { return text(value).startsWith(rule.text().toLowerCase(Locale.ROOT)); }
            case ENDS_WITH -> { return text(value).endsWith(rule.text().toLowerCase(Locale.ROOT)); }
            case BLANKS -> { return value.isEmpty() || value instanceof TextValue t && t.value().isBlank(); }
            case NO_BLANKS -> { return !(value.isEmpty() || value instanceof TextValue t && t.value().isBlank()); }
            case ERRORS -> { return value instanceof ErrorValue; }
            case NO_ERRORS -> { return !(value instanceof ErrorValue); }
            case TIME_PERIOD -> { return value instanceof NumberValue n && inPeriod(n.value(), rule); }
            default -> { return false; }
        }
    }

    private boolean inPeriod(double serial, ConditionalRule rule) {
        boolean d1904 = engine.workbook().properties().date1904();
        LocalDate d = DateSerial.toDate(serial, d1904), today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() % 7);
        return switch (rule.timePeriod()) {
            case TODAY -> d.equals(today);
            case YESTERDAY -> d.equals(today.minusDays(1));
            case TOMORROW -> d.equals(today.plusDays(1));
            case LAST_7_DAYS -> !d.isAfter(today) && d.isAfter(today.minusDays(7));
            case THIS_WEEK -> !d.isBefore(weekStart) && d.isBefore(weekStart.plusDays(7));
            case LAST_WEEK -> !d.isBefore(weekStart.minusDays(7)) && d.isBefore(weekStart);
            case NEXT_WEEK -> !d.isBefore(weekStart.plusDays(7)) && d.isBefore(weekStart.plusDays(14));
            case THIS_MONTH -> d.getYear() == today.getYear() && d.getMonth() == today.getMonth();
            case LAST_MONTH -> { LocalDate m = today.minusMonths(1); yield d.getYear() == m.getYear() && d.getMonth() == m.getMonth(); }
            case NEXT_MONTH -> { LocalDate m = today.plusMonths(1); yield d.getYear() == m.getYear() && d.getMonth() == m.getMonth(); }
        };
    }

    static DayOfWeek first() { return DayOfWeek.SUNDAY; }

    private static String text(CellValue v) { return v.isEmpty() ? "" : v.display().toLowerCase(Locale.ROOT); }

    private CellValue formula(int sheet, String formula, int dr, int dc, int row, int column) {
        if (formula == null || formula.isBlank()) return CellValue.EMPTY;
        String key = sheet + ":" + row + ":" + column + ":" + formula;
        CellValue cached = formulaCache.get(key);
        if (cached != null) return cached;
        CellValue v;
        try {
            String shifted = ReferenceAdjuster.shift(formula.startsWith("=") ? formula.substring(1) : formula, dr, dc);
            v = engine.evaluate(sheet, new CellAddress(row, column), shifted);
        } catch (RuntimeException e) {
            v = CellValue.error(dtm.stools.component.panels.editor.sheet.model.CellError.NAME);
        }
        if (formulaCache.size() > 100_000) formulaCache.clear();
        formulaCache.put(key, v);
        return v;
    }

    private Stats stats(int sheet, ConditionalFormat f, ConditionalRule rule) {
        return stats.computeIfAbsent(rule, r -> {
            List<Double> nums = new ArrayList<>();
            Map<String, Integer> counts = new HashMap<>();
            for (CellRange range : f.ranges()) {
                CellRange used = engine.usedRange(sheet);
                if (used == null) continue;
                CellRange clipped = range.intersection(used);
                if (clipped == null) continue;
                for (int row = clipped.firstRow(); row <= clipped.lastRow(); row++)
                    for (int col = clipped.firstColumn(); col <= clipped.lastColumn(); col++) {
                        CellValue v = engine.valueAt(sheet, row, col);
                        if (v instanceof NumberValue n) nums.add(n.value());
                        if (!v.isEmpty()) counts.merge(key(v), 1, Integer::sum);
                    }
            }
            double[] sorted = nums.stream().mapToDouble(Double::doubleValue).sorted().toArray();
            double mean = Arrays.stream(sorted).average().orElse(0);
            double sd = 0;
            for (double d : sorted) sd += (d - mean) * (d - mean);
            sd = sorted.length > 1 ? Math.sqrt(sd / (sorted.length - 1)) : 0;
            return new Stats(sorted, sorted.length == 0 ? 0 : sorted[0], sorted.length == 0 ? 0 : sorted[sorted.length - 1], mean, sd, counts);
        });
    }

    private static String key(CellValue v) { return v instanceof TextValue t ? "t:" + t.value().toLowerCase(Locale.ROOT) : v instanceof BoolValue b ? "b:" + b.value() : "v:" + v.display(); }

    private double threshold(int sheet, ConditionalFormat f, ConditionalRule rule, Threshold t) {
        Stats s = stats(sheet, f, rule);
        return switch (t.type()) {
            case MIN, AUTO_MIN -> s.min;
            case MAX, AUTO_MAX -> s.max;
            case PERCENT -> s.min + (s.max - s.min) * parse(t.value()) / 100.0;
            case PERCENTILE -> {
                if (s.sorted.length == 0) yield 0;
                double p = parse(t.value()) / 100.0, rank = p * (s.sorted.length - 1);
                int k = (int) Math.floor(rank);
                yield k + 1 < s.sorted.length ? s.sorted[k] + (rank - k) * (s.sorted[k + 1] - s.sorted[k]) : s.sorted[s.sorted.length - 1];
            }
            case FORMULA -> {
                CellRange a = f.ranges().getFirst();
                CellValue v = formula(sheet, t.value(), 0, 0, a.firstRow(), a.firstColumn());
                yield v instanceof NumberValue n ? n.value() : 0;
            }
            default -> parse(t.value());
        };
    }

    private static double parse(String s) { try { return Double.parseDouble(s.strip()); } catch (RuntimeException e) { return 0; } }

    private double[] bounds(int sheet, ConditionalFormat f, ConditionalRule rule) {
        List<Threshold> t = rule.thresholds();
        double lo = t.isEmpty() ? stats(sheet, f, rule).min : threshold(sheet, f, rule, t.getFirst());
        double hi = t.size() < 2 ? stats(sheet, f, rule).max : threshold(sheet, f, rule, t.getLast());
        if (lo > 0 && (t.isEmpty() || t.getFirst().type() == dtm.stools.component.panels.editor.sheet.model.ThresholdType.AUTO_MIN)) lo = 0;
        return new double[]{lo, hi};
    }

    private Integer colorScale(int sheet, ConditionalFormat f, ConditionalRule rule, double v) {
        List<Threshold> t = rule.thresholds();
        List<Integer> colors = rule.colors();
        if (t.size() < 2 || colors.size() < t.size()) return null;
        double[] points = new double[t.size()];
        for (int k = 0; k < t.size(); k++) points[k] = threshold(sheet, f, rule, t.get(k));
        if (v <= points[0]) return colors.getFirst();
        if (v >= points[points.length - 1]) return colors.get(points.length - 1);
        for (int k = 0; k + 1 < points.length; k++) {
            if (v >= points[k] && v <= points[k + 1]) {
                double frac = points[k + 1] == points[k] ? 0 : (v - points[k]) / (points[k + 1] - points[k]);
                return blend(colors.get(k), colors.get(k + 1), frac);
            }
        }
        return colors.getLast();
    }

    private int iconIndex(int sheet, ConditionalFormat f, ConditionalRule rule, double v) {
        List<Threshold> t = rule.thresholds();
        int index = 0;
        for (int k = 0; k < t.size(); k++) {
            double th = threshold(sheet, f, rule, t.get(k));
            if (t.get(k).greaterOrEqual() ? v >= th : v > th) index = k;
        }
        return rule.reverseIcons() ? t.size() - 1 - index : index;
    }

    public static int blend(int a, int b, double t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        int r = (int) Math.round(ar + (br - ar) * t), g = (int) Math.round(ag + (bg - ag) * t), bl = (int) Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
