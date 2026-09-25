package dtm.stools.component.panels.editor.sheet.data;

import dtm.stools.component.panels.editor.sheet.command.SheetTransaction;
import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.formula.ReferenceAdjuster;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.CustomList;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AutoFill {
    public enum Direction { DOWN, UP, RIGHT, LEFT }
    public enum Mode { SERIES, COPY, FORMATS_ONLY, VALUES_WITHOUT_FORMAT }
    public enum SeriesType { LINEAR, GROWTH, DATE, AUTO }
    public enum DateUnit { DAY, WEEKDAY, MONTH, YEAR }

    private static final Pattern TRAILING_NUMBER = Pattern.compile("^(.*?)(\\d+)(\\D*)$");

    private AutoFill() {}

    public static void fill(SheetTransaction tx, int sheet, CellRange source, CellRange target, Mode mode, NumberFormatter formatter) {
        SheetWorkbook wb = tx.workbook();
        boolean vertical = target.columnCount() == source.columnCount() && target.firstColumn() == source.firstColumn();
        Direction dir = vertical ? (target.firstRow() >= source.firstRow() ? Direction.DOWN : Direction.UP) : (target.firstColumn() >= source.firstColumn() ? Direction.RIGHT : Direction.LEFT);
        int lines = vertical ? source.columnCount() : source.rowCount();
        int seedLength = vertical ? source.rowCount() : source.columnCount();
        int fillLength = vertical ? target.rowCount() - source.rowCount() : target.columnCount() - source.columnCount();
        if (fillLength <= 0) return;
        List<CustomList> lists = wb.properties().customLists();
        for (int line = 0; line < lines; line++) {
            List<SheetCell> seed = new ArrayList<>();
            List<CellAddress> seedAddresses = new ArrayList<>();
            for (int k = 0; k < seedLength; k++) {
                int r = vertical ? source.firstRow() + k : source.firstRow() + line, c = vertical ? source.firstColumn() + line : source.firstColumn() + k;
                seed.add(tx.cell(sheet, r, c));
                seedAddresses.add(new CellAddress(r, c));
            }
            boolean backwards = dir == Direction.UP || dir == Direction.LEFT;
            List<CellValue> generated = mode == Mode.SERIES ? series(seed, fillLength, backwards, lists, formatter) : null;
            if (generated != null && seed.size() == 1 && seed.getFirst().value() instanceof NumberValue n && formatter != null && formatter.isDateFormat(wb.style(seed.getFirst().style()).numberFormat())) {
                generated = new ArrayList<>();
                for (int k = 1; k <= fillLength; k++) generated.add(CellValue.of(n.value() + (backwards ? -k : k)));
            }
            for (int k = 0; k < fillLength; k++) {
                int offset = backwards ? -(k + 1) : seedLength + k;
                int baseR = vertical ? source.firstRow() : source.firstRow() + line, baseC = vertical ? source.firstColumn() + line : source.firstColumn();
                int r = vertical ? baseR + offset : baseR, c = vertical ? baseC : baseC + offset;
                if (r < 0 || c < 0 || r >= CellAddress.MAX_ROWS || c >= CellAddress.MAX_COLUMNS) continue;
                int seedIndex = backwards ? Math.floorMod(seedLength - 1 - k, seedLength) : k % seedLength;
                SheetCell s = seed.get(seedIndex);
                CellAddress from = seedAddresses.get(seedIndex);
                SheetCell existing = tx.cell(sheet, r, c);
                SheetCell out;
                if (mode == Mode.FORMATS_ONLY) out = existing.withStyle(s.style());
                else if (s.hasFormula()) {
                    String f;
                    try { f = ReferenceAdjuster.shift(s.formula(), r - from.row(), c - from.column()); } catch (RuntimeException e) { f = s.formula(); }
                    out = new SheetCell(CellValue.EMPTY, f, mode == Mode.VALUES_WITHOUT_FORMAT ? existing.style() : s.style());
                } else {
                    CellValue v = generated != null && generated.get(k) != null ? generated.get(k) : s.value();
                    out = new SheetCell(v, null, mode == Mode.VALUES_WITHOUT_FORMAT ? existing.style() : s.style());
                }
                tx.setCell(sheet, r, c, out);
            }
        }
    }

    static List<CellValue> series(List<SheetCell> seed, int count, boolean backwards, List<CustomList> lists, NumberFormatter formatter) {
        List<CellValue> out = new ArrayList<>();
        List<CellValue> values = seed.stream().map(SheetCell::value).toList();
        if (seed.stream().anyMatch(SheetCell::hasFormula)) { for (int k = 0; k < count; k++) out.add(null); return out; }
        boolean allNumbers = values.stream().allMatch(v -> v instanceof NumberValue);
        if (allNumbers) {
            double[] d = values.stream().mapToDouble(v -> ((NumberValue) v).value()).toArray();
            if (d.length == 1) {
                for (int k = 0; k < count; k++) out.add(null);
                return out;
            }
            double[] fit = linearFit(d);
            for (int k = 0; k < count; k++) {
                double x = backwards ? -(k + 1) : d.length + k;
                out.add(CellValue.of(fit[0] * x + fit[1]));
            }
            return out;
        }
        boolean allText = values.stream().allMatch(v -> v instanceof TextValue);
        if (allText) {
            for (CustomList list : lists) {
                int first = list.indexOf(values.getFirst().display());
                if (first < 0) continue;
                int step = values.size() > 1 ? Math.floorMod(list.indexOf(values.get(1).display()) - first, list.items().size()) : 1;
                if (values.size() > 1 && list.indexOf(values.get(1).display()) < 0) continue;
                if (step == 0) step = 1;
                int last = list.indexOf(values.getLast().display());
                boolean upper = Character.isUpperCase(values.getFirst().display().charAt(0));
                for (int k = 0; k < count; k++) {
                    int idx = Math.floorMod(backwards ? first - step * (k + 1) : last + step * (k + 1), list.items().size());
                    String item = list.items().get(idx);
                    out.add(CellValue.of(upper ? Character.toUpperCase(item.charAt(0)) + item.substring(1) : item));
                }
                return out;
            }
            List<Matcher> matchers = new ArrayList<>();
            for (CellValue v : values) { Matcher m = TRAILING_NUMBER.matcher(v.display()); if (!m.matches()) { matchers = null; break; } matchers.add(m); }
            if (matchers != null && !matchers.isEmpty()) {
                String prefix = matchers.getFirst().group(1), suffix = matchers.getFirst().group(3);
                int width = matchers.getFirst().group(2).length();
                long[] nums = matchers.stream().mapToLong(m -> Long.parseLong(m.group(2))).toArray();
                long step = nums.length > 1 ? nums[nums.length - 1] - nums[nums.length - 2] : 1;
                long last = nums[nums.length - 1], first = nums[0];
                for (int k = 0; k < count; k++) {
                    long v = backwards ? first - step * (k + 1) : last + step * (k + 1);
                    String digits = String.valueOf(Math.abs(v));
                    while (digits.length() < width && matchers.getFirst().group(2).startsWith("0")) digits = "0" + digits;
                    out.add(CellValue.of(prefix + (v < 0 ? "-" : "") + digits + suffix));
                }
                return out;
            }
        }
        for (int k = 0; k < count; k++) out.add(null);
        return out;
    }

    public static List<CellValue> incrementSingle(CellValue v, int count, boolean dateLike) {
        List<CellValue> out = new ArrayList<>();
        if (v instanceof NumberValue n) for (int k = 1; k <= count; k++) out.add(CellValue.of(n.value() + k));
        return out;
    }

    static double[] linearFit(double[] y) {
        int n = y.length;
        double mx = (n - 1) / 2.0, my = 0;
        for (double v : y) my += v;
        my /= n;
        double sxy = 0, sxx = 0;
        for (int i = 0; i < n; i++) { sxy += (i - mx) * (y[i] - my); sxx += (i - mx) * (i - mx); }
        double slope = sxx == 0 ? 0 : sxy / sxx;
        return new double[]{slope, my - slope * mx};
    }

    public static void series(SheetTransaction tx, int sheet, CellRange range, boolean byRows, SeriesType type, DateUnit unit, double step, Double stop, boolean date1904) {
        int lines = byRows ? range.rowCount() : range.columnCount();
        int length = byRows ? range.columnCount() : range.rowCount();
        for (int line = 0; line < lines; line++) {
            int r0 = byRows ? range.firstRow() + line : range.firstRow(), c0 = byRows ? range.firstColumn() : range.firstColumn() + line;
            CellValue start = tx.cell(sheet, r0, c0).value();
            if (!(start instanceof NumberValue sn)) continue;
            double current = sn.value();
            for (int k = 1; k < length; k++) {
                double next = switch (type) {
                    case GROWTH -> current * step;
                    case DATE -> dateStep(current, unit, step, date1904);
                    default -> current + step;
                };
                if (stop != null && (step >= 0 ? next > stop : next < stop)) break;
                current = next;
                int r = byRows ? r0 : r0 + k, c = byRows ? c0 + k : c0;
                SheetCell existing = tx.cell(sheet, r, c);
                tx.setCell(sheet, r, c, new SheetCell(CellValue.of(current), null, existing.style() == 0 ? tx.cell(sheet, r0, c0).style() : existing.style()));
            }
        }
    }

    private static double dateStep(double serial, DateUnit unit, double step, boolean date1904) {
        LocalDate d = DateSerial.toDate(serial, date1904);
        long s = (long) step;
        LocalDate next = switch (unit) {
            case DAY -> d.plusDays(s);
            case MONTH -> d.plusMonths(s);
            case YEAR -> d.plusYears(s);
            case WEEKDAY -> {
                LocalDate x = d;
                long left = Math.abs(s);
                while (left > 0) { x = x.plusDays(s > 0 ? 1 : -1); if (x.getDayOfWeek().getValue() < 6) left--; }
                yield x;
            }
        };
        return DateSerial.toSerial(next, date1904) + (serial - Math.floor(serial));
    }
}
