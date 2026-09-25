package dtm.stools.component.panels.editor.sheet.function.library;

import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.calc.EvalError;
import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.format.ParsedInput;
import dtm.stools.component.panels.editor.sheet.format.ValueParser;
import dtm.stools.component.panels.editor.sheet.function.FunctionArgs;
import dtm.stools.component.panels.editor.sheet.function.FunctionContext;
import dtm.stools.component.panels.editor.sheet.function.FunctionDefinition;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.util.HashSet;
import java.util.Set;

import static dtm.stools.component.panels.editor.sheet.function.FunctionCategory.DATE_TIME;
import static dtm.stools.component.panels.editor.sheet.function.library.Lib.*;

final class DateTimeFunctions {
    private DateTimeFunctions() {}

    static void register(FunctionRegistry r) {
        scalar(r, "DATE", DATE_TIME, 3, 3, (c, a) -> num(date(c, n(a, 0), n(a, 1), n(a, 2))));
        scalar(r, "TIME", DATE_TIME, 3, 3, (c, a) -> {
            double h = Math.floor(n(a, 0)), m = Math.floor(n(a, 1)), s = Math.floor(n(a, 2));
            double total = h * 3600 + m * 60 + s;
            if (total < 0 || h > 32767 || m > 32767 || s > 32767) throw EvalError.num();
            return num((total % 86400) / 86400.0);
        });
        scalar(r, "YEAR", DATE_TIME, 1, 1, (c, a) -> num(parts(c, a, 0)[0]));
        scalar(r, "MONTH", DATE_TIME, 1, 1, (c, a) -> num(parts(c, a, 0)[1]));
        scalar(r, "DAY", DATE_TIME, 1, 1, (c, a) -> num(parts(c, a, 0)[2]));
        scalar(r, "HOUR", DATE_TIME, 1, 1, (c, a) -> num(timeParts(c, a)[0]));
        scalar(r, "MINUTE", DATE_TIME, 1, 1, (c, a) -> num(timeParts(c, a)[1]));
        scalar(r, "SECOND", DATE_TIME, 1, 1, (c, a) -> num(timeParts(c, a)[2]));
        r.register(FunctionDefinition.scalar("NOW", DATE_TIME, 0, 0, (c, a) -> num(now(c))).volatileFunction().build());
        r.register(FunctionDefinition.scalar("TODAY", DATE_TIME, 0, 0, (c, a) -> num(Math.floor(now(c)))).volatileFunction().build());
        scalar(r, "WEEKDAY", DATE_TIME, 1, 2, (c, a) -> {
            int w = parts(c, a, 0)[7];
            int type = i(a, 1, 1);
            return num(switch (type) {
                case 1, 17 -> w + 1;
                case 2, 11 -> (w + 6) % 7 + 1;
                case 3 -> (w + 6) % 7;
                case 12 -> (w + 5) % 7 + 1;
                case 13 -> (w + 4) % 7 + 1;
                case 14 -> (w + 3) % 7 + 1;
                case 15 -> (w + 2) % 7 + 1;
                case 16 -> (w + 1) % 7 + 1;
                default -> throw EvalError.num();
            });
        });
        scalar(r, "WEEKNUM", DATE_TIME, 1, 2, (c, a) -> {
            LocalDate d = localDate(c, n(a, 0));
            int type = i(a, 1, 1);
            if (type == 21) return num(d.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
            int start = switch (type) { case 1, 17 -> 7; case 2, 11 -> 1; case 12 -> 2; case 13 -> 3; case 14 -> 4; case 15 -> 5; case 16 -> 6; default -> throw EvalError.num(); };
            LocalDate jan1 = LocalDate.of(d.getYear(), 1, 1);
            int offset = (jan1.getDayOfWeek().getValue() - start + 7) % 7;
            return num((d.getDayOfYear() - 1 + offset) / 7 + 1);
        });
        scalar(r, "ISOWEEKNUM", DATE_TIME, 1, 1, (c, a) -> num(localDate(c, n(a, 0)).get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)));
        scalar(r, "EDATE", DATE_TIME, 2, 2, (c, a) -> {
            LocalDate d = localDate(c, n(a, 0)).plusMonths((long) n(a, 1));
            return num(serial(c, d));
        });
        scalar(r, "EOMONTH", DATE_TIME, 2, 2, (c, a) -> {
            LocalDate d = localDate(c, n(a, 0)).plusMonths((long) n(a, 1));
            return num(serial(c, d.withDayOfMonth(d.lengthOfMonth())));
        });
        scalar(r, "DAYS", DATE_TIME, 2, 2, (c, a) -> num(Math.floor(dateArg(c, arg(a, 0))) - Math.floor(dateArg(c, arg(a, 1)))));
        scalar(r, "DAYS360", DATE_TIME, 2, 3, (c, a) -> {
            LocalDate s = localDate(c, n(a, 0)), e = localDate(c, n(a, 1));
            boolean european = b(a, 2, false);
            return num(days360(s, e, european));
        });
        scalar(r, "DATEDIF", DATE_TIME, 3, 3, (c, a) -> {
            double s = n(a, 0), e = n(a, 1);
            if (s > e) throw EvalError.num();
            LocalDate sd = localDate(c, s), ed = localDate(c, e);
            return num(switch (t(a, 2).toUpperCase(java.util.Locale.ROOT)) {
                case "Y" -> ChronoUnit.YEARS.between(sd, ed);
                case "M" -> ChronoUnit.MONTHS.between(sd, ed);
                case "D" -> Math.floor(e) - Math.floor(s);
                case "MD" -> { int days = ed.getDayOfMonth() - sd.getDayOfMonth(); if (days < 0) days += ed.minusMonths(1).lengthOfMonth(); yield days; }
                case "YM" -> ChronoUnit.MONTHS.between(sd, ed) % 12;
                case "YD" -> { LocalDate shifted = sd.withYear(ed.getYear()); if (shifted.isAfter(ed)) shifted = shifted.minusYears(1); yield ChronoUnit.DAYS.between(shifted, ed); }
                default -> throw EvalError.num();
            });
        });
        scalar(r, "DATEVALUE", DATE_TIME, 1, 1, (c, a) -> {
            ParsedInput p = new ValueParser(c.locale(), c.date1904()).parse(t(a, 0).strip());
            if (p.value() instanceof NumberValue n && p.format() != null && !p.format().startsWith("h")) return num(Math.floor(n.value()));
            throw EvalError.value();
        });
        scalar(r, "TIMEVALUE", DATE_TIME, 1, 1, (c, a) -> {
            ParsedInput p = new ValueParser(c.locale(), c.date1904()).parse(t(a, 0).strip());
            if (p.value() instanceof NumberValue n && p.format() != null) return num(n.value() - Math.floor(n.value()));
            throw EvalError.value();
        });
        raw(r, "NETWORKDAYS", DATE_TIME, 2, 3, (c, a) -> num(networkDays(c, a.number(0), a.number(1), weekend(1), holidays(c, a, 2))));
        raw(r, "NETWORKDAYS.INTL", DATE_TIME, 2, 4, (c, a) -> num(networkDays(c, a.number(0), a.number(1), a.has(2) ? weekend(a.scalar(2)) : weekend(1), holidays(c, a, 3))));
        raw(r, "WORKDAY", DATE_TIME, 2, 3, (c, a) -> num(workday(c, a.number(0), a.number(1), weekend(1), holidays(c, a, 2))));
        raw(r, "WORKDAY.INTL", DATE_TIME, 2, 4, (c, a) -> num(workday(c, a.number(0), a.number(1), a.has(2) ? weekend(a.scalar(2)) : weekend(1), holidays(c, a, 3))));
        scalar(r, "YEARFRAC", DATE_TIME, 2, 3, (c, a) -> num(yearFrac(c, n(a, 0), n(a, 1), i(a, 2, 0))));
    }

    static double now(FunctionContext c) {
        LocalDateTime dt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(c.now()), ZoneId.systemDefault());
        return DateSerial.toSerial(dt, c.date1904());
    }

    static double date(FunctionContext c, double yv, double mv, double dv) {
        long y = (long) Math.floor(yv), m = (long) Math.floor(mv), d = (long) Math.floor(dv);
        if (y < 0 || y >= 10000) throw EvalError.num();
        if (y < 1900 && !c.date1904()) y += 1900;
        if (c.date1904() && y < 1904) y += 1900;
        long months = y * 12 + (m - 1);
        long ny = Math.floorDiv(months, 12), nm = Math.floorMod(months, 12) + 1;
        if (ny < 1900 || ny > 9999) throw EvalError.num();
        double base = DateSerial.toSerial(LocalDate.of((int) ny, (int) nm, 1), c.date1904());
        double serial = base + d - 1;
        if (serial < 0 || serial > DateSerial.MAX) throw EvalError.num();
        return serial;
    }

    static int[] parts(FunctionContext c, CellValue[] a, int index) {
        double v = dateArg(c, arg(a, index));
        if (v < 0 || v > DateSerial.MAX) throw EvalError.num();
        return DateSerial.parts(v, c.date1904());
    }

    private static int[] timeParts(FunctionContext c, CellValue[] a) {
        double v = dateArg(c, arg(a, 0));
        if (v < 0) throw EvalError.num();
        double frac = v - Math.floor(v);
        long seconds = Math.round(frac * 86400);
        if (seconds >= 86400) seconds = 0;
        return new int[]{(int) (seconds / 3600), (int) (seconds / 60 % 60), (int) (seconds % 60)};
    }

    static double dateArg(FunctionContext c, CellValue v) {
        if (v instanceof TextValue t) {
            ParsedInput p = new ValueParser(c.locale(), c.date1904()).parse(t.value().strip());
            if (p.value() instanceof NumberValue n) return n.value();
            throw EvalError.value();
        }
        if (v instanceof ErrorValue e) throw EvalError.of(e.error());
        return Coerce.number(v);
    }

    static LocalDate localDate(FunctionContext c, double serial) {
        if (serial < 0 || serial > DateSerial.MAX) throw EvalError.num();
        return DateSerial.toDate(serial, c.date1904());
    }

    static double serial(FunctionContext c, LocalDate d) {
        double s = DateSerial.toSerial(d, c.date1904());
        if (s < 0 || s > DateSerial.MAX) throw EvalError.num();
        return s;
    }

    static double days360(LocalDate s, LocalDate e, boolean european) {
        int sd = s.getDayOfMonth(), ed = e.getDayOfMonth();
        if (european) {
            if (sd == 31) sd = 30;
            if (ed == 31) ed = 30;
        } else {
            boolean sLast = s.getMonthValue() == 2 && sd == s.lengthOfMonth();
            if (sLast || sd == 31) sd = 30;
            if (ed == 31 && sd >= 30) ed = 30;
        }
        return (e.getYear() - s.getYear()) * 360.0 + (e.getMonthValue() - s.getMonthValue()) * 30.0 + (ed - sd);
    }

    static boolean[] weekend(int code) {
        boolean[] w = new boolean[7];
        switch (code) {
            case 1 -> { w[5] = true; w[6] = true; }
            case 2 -> { w[6] = true; w[0] = true; }
            case 3 -> { w[0] = true; w[1] = true; }
            case 4 -> { w[1] = true; w[2] = true; }
            case 5 -> { w[2] = true; w[3] = true; }
            case 6 -> { w[3] = true; w[4] = true; }
            case 7 -> { w[4] = true; w[5] = true; }
            case 11 -> w[6] = true;
            case 12 -> w[0] = true;
            case 13 -> w[1] = true;
            case 14 -> w[2] = true;
            case 15 -> w[3] = true;
            case 16 -> w[4] = true;
            case 17 -> w[5] = true;
            default -> throw EvalError.num();
        }
        return w;
    }

    static boolean[] weekend(CellValue v) {
        if (v instanceof TextValue t) {
            String s = t.value();
            if (!s.matches("[01]{7}") || s.equals("1111111")) throw EvalError.value();
            boolean[] w = new boolean[7];
            for (int k = 0; k < 7; k++) w[k] = s.charAt(k) == '1';
            return w;
        }
        return weekend((int) Coerce.number(v));
    }

    static Set<Long> holidays(FunctionContext c, FunctionArgs a, int index) {
        Set<Long> set = new HashSet<>();
        if (!a.has(index)) return set;
        for (CellValue v : flatten(c, a.value(index))) {
            if (v instanceof ErrorValue e) throw EvalError.of(e.error());
            if (v.isEmpty()) continue;
            set.add((long) Math.floor(dateArg(c, v)));
        }
        return set;
    }

    private static boolean isWorkday(FunctionContext c, long serial, boolean[] weekend, Set<Long> holidays) {
        if (holidays.contains(serial)) return false;
        DayOfWeek dow = localDate(c, serial).getDayOfWeek();
        return !weekend[dow.getValue() - 1];
    }

    static double networkDays(FunctionContext c, double start, double end, boolean[] weekend, Set<Long> holidays) {
        long s = (long) Math.floor(start), e = (long) Math.floor(end);
        int sign = 1;
        if (s > e) { long t = s; s = e; e = t; sign = -1; }
        long count = 0;
        for (long d = s; d <= e; d++) if (isWorkday(c, d, weekend, holidays)) count++;
        return sign * count;
    }

    static double workday(FunctionContext c, double start, double days, boolean[] weekend, Set<Long> holidays) {
        long d = (long) Math.floor(start);
        long remaining = (long) days;
        int step = remaining >= 0 ? 1 : -1;
        remaining = Math.abs(remaining);
        int guard = 0;
        while (remaining > 0) {
            d += step;
            if (isWorkday(c, d, weekend, holidays)) remaining--;
            if (++guard > 10_000_000) throw EvalError.num();
        }
        return d;
    }

    static double yearFrac(FunctionContext c, double start, double end, int basis) {
        if (start > end) { double t = start; start = end; end = t; }
        LocalDate s = localDate(c, start), e = localDate(c, end);
        return switch (basis) {
            case 0 -> days360(s, e, false) / 360.0;
            case 1 -> {
                if (s.getYear() == e.getYear()) yield (Math.floor(end) - Math.floor(start)) / s.lengthOfYear();
                LocalDate oneYear = s.plusYears(1);
                if (!e.isAfter(oneYear)) {
                    boolean leap = s.isLeapYear() && !s.isAfter(LocalDate.of(s.getYear(), 2, 29)) || e.isLeapYear() && !e.isBefore(LocalDate.of(e.getYear(), 2, 29));
                    yield (Math.floor(end) - Math.floor(start)) / (leap ? 366.0 : 365.0);
                }
                double total = 0;
                for (int y = s.getYear(); y <= e.getYear(); y++) total += LocalDate.of(y, 1, 1).lengthOfYear();
                yield (Math.floor(end) - Math.floor(start)) / (total / (e.getYear() - s.getYear() + 1));
            }
            case 2 -> (Math.floor(end) - Math.floor(start)) / 360.0;
            case 3 -> (Math.floor(end) - Math.floor(start)) / 365.0;
            case 4 -> days360(s, e, true) / 360.0;
            default -> throw EvalError.num();
        };
    }
}
