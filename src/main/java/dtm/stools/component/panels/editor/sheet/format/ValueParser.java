package dtm.stools.component.panels.editor.sheet.format;

import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.text.DecimalFormatSymbols;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ValueParser {
    private static final ThreadLocal<ValueParser> CURRENT = ThreadLocal.withInitial(() -> new ValueParser(Locale.forLanguageTag("pt-BR"), false));
    private static final Pattern TIME = Pattern.compile("(\\d{1,2}):(\\d{1,2})(?::(\\d{1,2})(?:[.,](\\d+))?)?\\s*([AaPp][Mm]?)?");
    private static final Pattern ISO_DATE = Pattern.compile("(\\d{4})-(\\d{1,2})-(\\d{1,2})");
    private static final Pattern SLASH_DATE = Pattern.compile("(\\d{1,4})[/.-](\\d{1,2})(?:[/.-](\\d{1,4}))?");
    private static final Pattern NAMED_DATE = Pattern.compile("(\\d{1,2})[- /](?:de )?([\\p{L}]{3,})\\.?(?:[- /](?:de )?(\\d{2,4}))?", Pattern.CASE_INSENSITIVE);

    private final Locale locale;
    private final boolean date1904;
    private final char decimal, group;
    private final boolean dayFirst;

    public ValueParser(Locale locale, boolean date1904) {
        this.locale = locale;
        this.date1904 = date1904;
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(locale);
        this.decimal = sym.getDecimalSeparator();
        char g = sym.getGroupingSeparator();
        this.group = g == ' ' || g == ' ' ? ' ' : g;
        this.dayFirst = !locale.getCountry().equals("US");
    }

    public static void setCurrent(ValueParser parser) { CURRENT.set(parser); }
    public static ValueParser current() { return CURRENT.get(); }
    public Locale locale() { return locale; }

    public static Double parseNumberLenient(String text) {
        String t = text.strip();
        if (t.isEmpty()) return null;
        try { return Double.parseDouble(t.replace(" ", "")); } catch (NumberFormatException ignored) { }
        ParsedInput p = current().parse(t);
        return p.value() != null && p.value().isNumber() ? ((dtm.stools.component.panels.editor.sheet.model.NumberValue) p.value()).value() : null;
    }

    public ParsedInput parse(String input) {
        if (input == null || input.isEmpty()) return ParsedInput.of(CellValue.EMPTY);
        if (input.startsWith("'")) return ParsedInput.of(CellValue.of(input.substring(1)));
        if (input.length() > 1 && input.charAt(0) == '=') return new ParsedInput(CellValue.EMPTY, null, input.substring(1));
        String t = input.strip();
        if (t.isEmpty()) return ParsedInput.of(CellValue.of(input));
        String upper = t.toUpperCase(locale);
        if (upper.equals("TRUE") || upper.equals("VERDADEIRO")) return ParsedInput.of(CellValue.TRUE);
        if (upper.equals("FALSE") || upper.equals("FALSO")) return ParsedInput.of(CellValue.FALSE);
        Optional<CellError> error = CellError.parse(t);
        if (error.isPresent()) return ParsedInput.of(CellValue.error(error.get()));
        ParsedInput n = number(t);
        if (n != null) return n;
        ParsedInput d = dateTime(t);
        if (d != null) return d;
        return ParsedInput.of(CellValue.of(input));
    }

    private ParsedInput number(String t) {
        String s = t;
        boolean negative = false, percent = false;
        String currency = null;
        if (s.startsWith("(") && s.endsWith(")")) { negative = true; s = s.substring(1, s.length() - 1).strip(); }
        if (s.startsWith("-")) { negative = !negative; s = s.substring(1).strip(); }
        else if (s.startsWith("+")) s = s.substring(1).strip();
        for (String c : new String[]{"R$", "US$", "$", "€", "£", "¥"}) {
            if (s.startsWith(c)) { currency = c; s = s.substring(c.length()).strip(); break; }
            if (s.endsWith(c)) { currency = c; s = s.substring(0, s.length() - c.length()).strip(); break; }
        }
        if (s.startsWith("-") && !negative) { negative = true; s = s.substring(1).strip(); }
        if (s.endsWith("%")) { percent = true; s = s.substring(0, s.length() - 1).strip(); }
        if (s.isEmpty()) return null;
        Matcher frac = Pattern.compile("(\\d+) (\\d+)/(\\d+)").matcher(s);
        if (frac.matches() && currency == null && !percent) {
            double den = Double.parseDouble(frac.group(3));
            if (den == 0) return null;
            double v = Double.parseDouble(frac.group(1)) + Double.parseDouble(frac.group(2)) / den;
            return new ParsedInput(CellValue.of(negative ? -v : v), den < 10 ? "# ?/?" : "# ??/??", null);
        }
        Double v = plainNumber(s);
        if (v == null) return null;
        boolean grouped = s.indexOf(group) >= 0 && group != decimal;
        int decimals = s.indexOf(decimal) >= 0 ? s.length() - s.indexOf(decimal) - 1 : 0;
        if (s.toUpperCase(Locale.ROOT).contains("E")) decimals = 0;
        double value = negative ? -v : v;
        String format = null;
        if (percent) { value /= 100; format = decimals > 0 ? "0." + "0".repeat(Math.min(decimals, 10)) + "%" : "0%"; }
        else if (currency != null) {
            String sym = currency.equals("R$") ? "\"R$\" " : currency.equals("$") ? "\"$\"" : "\"" + currency + "\" ";
            format = sym + "#,##0" + (decimals > 0 || currency.equals("R$") ? ".00" : "");
        } else if (grouped) format = "#,##0" + (decimals > 0 ? "." + "0".repeat(Math.min(decimals, 10)) : "");
        else if (s.toUpperCase(Locale.ROOT).contains("E") && s.matches(".*[0-9][eE][+-]?[0-9]+")) format = "0.00E+00";
        return new ParsedInput(CellValue.of(value), format, null);
    }

    private Double plainNumber(String s) {
        String decimalPattern = Pattern.quote(String.valueOf(decimal));
        String groupPattern = Pattern.quote(String.valueOf(group));
        String grouped = "\\d{1,3}(?:" + groupPattern + "\\d{3})+(?:" + decimalPattern + "\\d*)?";
        String plain = "\\d+(?:" + decimalPattern + "\\d*)?(?:[eE][+-]?\\d+)?|" + decimalPattern + "\\d+(?:[eE][+-]?\\d+)?";
        if (s.matches(grouped) || s.matches(plain)) {
            String n = s.replace(String.valueOf(group), "").replace(decimal, '.');
            try { return Double.parseDouble(n); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }

    private ParsedInput dateTime(String t) {
        String datePart = t, timePart = null;
        Matcher tm = TIME.matcher(t);
        if (tm.matches()) {
            Double time = time(tm);
            if (time == null) return null;
            String fmt = tm.group(5) != null ? (tm.group(3) != null ? "h:mm:ss AM/PM" : "h:mm AM/PM") : (tm.group(3) != null ? "hh:mm:ss" : "hh:mm");
            return new ParsedInput(CellValue.of(time), fmt, null);
        }
        int space = t.lastIndexOf(' ');
        if (space > 0) {
            Matcher m2 = TIME.matcher(t.substring(space + 1));
            if (m2.matches()) { datePart = t.substring(0, space).strip(); timePart = t.substring(space + 1); }
        }
        LocalDate date = null;
        String format = null;
        Matcher iso = ISO_DATE.matcher(datePart);
        Matcher sl = SLASH_DATE.matcher(datePart);
        Matcher named = NAMED_DATE.matcher(datePart);
        try {
            if (iso.matches()) {
                date = LocalDate.of(Integer.parseInt(iso.group(1)), Integer.parseInt(iso.group(2)), Integer.parseInt(iso.group(3)));
                format = "yyyy-mm-dd";
            } else if (sl.matches()) {
                int a = Integer.parseInt(sl.group(1)), b = Integer.parseInt(sl.group(2));
                String c = sl.group(3);
                if (sl.group(1).length() == 4) { date = LocalDate.of(a, b, Integer.parseInt(c == null ? "1" : c)); format = "yyyy-mm-dd"; }
                else {
                    int day = dayFirst ? a : b, month = dayFirst ? b : a;
                    int year = c == null ? LocalDate.now().getYear() : year(c);
                    date = LocalDate.of(year, month, day);
                    format = c == null ? "d-mmm" : dayFirst ? "dd/mm/yyyy" : "m/d/yyyy";
                }
            } else if (named.matches()) {
                Month m = month(named.group(2));
                if (m == null) return null;
                int year = named.group(3) == null ? LocalDate.now().getYear() : year(named.group(3));
                date = LocalDate.of(year, m, Integer.parseInt(named.group(1)));
                format = named.group(3) == null ? "d-mmm" : "d-mmm-yy";
            }
        } catch (DateTimeException | NumberFormatException e) {
            return null;
        }
        if (date == null) return null;
        double serial = DateSerial.toSerial(date, date1904);
        if (serial < 0) return null;
        if (timePart != null) {
            Matcher m = TIME.matcher(timePart);
            if (!m.matches()) return null;
            Double time = time(m);
            if (time == null) return null;
            serial += time;
            format = (dayFirst ? "dd/mm/yyyy" : "m/d/yyyy") + " hh:mm" + (m.group(3) != null ? ":ss" : "");
        }
        return new ParsedInput(CellValue.of(serial), format, null);
    }

    private static int year(String y) {
        int v = Integer.parseInt(y);
        if (y.length() <= 2) v += v < 30 ? 2000 : 1900;
        return v;
    }

    private static Double time(Matcher m) {
        int h = Integer.parseInt(m.group(1)), min = Integer.parseInt(m.group(2));
        int s = m.group(3) == null ? 0 : Integer.parseInt(m.group(3));
        double frac = m.group(4) == null ? 0 : Double.parseDouble("0." + m.group(4));
        String ap = m.group(5);
        if (ap != null) {
            if (h < 1 || h > 12) return null;
            boolean pm = Character.toLowerCase(ap.charAt(0)) == 'p';
            if (h == 12) h = 0;
            if (pm) h += 12;
        }
        if (min > 59 || s > 59) return null;
        if (h > 9999) return null;
        return (h * 3600 + min * 60 + s + frac) / 86400.0;
    }

    private Month month(String name) {
        String n = name.toLowerCase(locale).replace(".", "");
        for (Month m : Month.values()) {
            for (Locale l : new Locale[]{locale, Locale.US}) {
                String full = m.getDisplayName(TextStyle.FULL, l).toLowerCase(l), shrt = m.getDisplayName(TextStyle.SHORT, l).toLowerCase(l).replace(".", "");
                if (n.equals(full) || n.equals(shrt) || n.length() >= 3 && full.startsWith(n)) return m;
            }
        }
        return null;
    }

    public LocalTime parseTime(String text) {
        Matcher m = TIME.matcher(text.strip());
        if (!m.matches()) return null;
        Double t = time(m);
        return t == null ? null : LocalTime.ofNanoOfDay(Math.round(t * 86_400_000_000_000.0) % 86_400_000_000_000L);
    }
}
