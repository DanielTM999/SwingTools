package dtm.stools.component.panels.editor.sheet.format;

import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.EmptyValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormatSymbols;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class NumberFormatter {
    private static final Map<String, Integer> COLORS = Map.of("BLACK", 0xFF000000, "BLUE", 0xFF0000FF, "CYAN", 0xFF00FFFF, "GREEN", 0xFF00FF00,
            "MAGENTA", 0xFFFF00FF, "RED", 0xFFFF0000, "WHITE", 0xFFFFFFFF, "YELLOW", 0xFFFFFF00);
    private static final Map<String, Integer> COLORS_PT = Map.of("PRETO", 0xFF000000, "AZUL", 0xFF0000FF, "CIANO", 0xFF00FFFF, "VERDE", 0xFF00FF00,
            "MAGENTA", 0xFFFF00FF, "VERMELHO", 0xFFFF0000, "BRANCO", 0xFFFFFFFF, "AMARELO", 0xFFFFFF00);
    private static final int[] PALETTE = {0xFF000000, 0xFFFFFFFF, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFFFF00FF, 0xFF00FFFF, 0xFF800000, 0xFF008000,
            0xFF000080, 0xFF808000, 0xFF800080, 0xFF008080, 0xFFC0C0C0, 0xFF808080, 0xFF9999FF, 0xFF993366, 0xFFFFFFCC, 0xFFCCFFFF, 0xFF660066, 0xFFFF8080,
            0xFF0066CC, 0xFFCCCCFF, 0xFF000080, 0xFFFF00FF, 0xFFFFFF00, 0xFF00FFFF, 0xFF800080, 0xFF800000, 0xFF008080, 0xFF0000FF, 0xFF00CCFF, 0xFFCCFFFF,
            0xFFCCFFCC, 0xFFFFFF99, 0xFF99CCFF, 0xFFFF99CC, 0xFFCC99FF, 0xFFFFCC99, 0xFF3366FF, 0xFF33CCCC, 0xFF99CC00, 0xFFFFCC00, 0xFFFF9900, 0xFFFF6600,
            0xFF666699, 0xFF969696, 0xFF003366, 0xFF339966, 0xFF003300, 0xFF333300, 0xFF993300, 0xFF993366, 0xFF333399, 0xFF333333};

    private final Locale locale;
    private final boolean date1904;
    private final Map<String, Format> cache = new LinkedHashMap<>(64, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Format> e) { return size() > 512; }
    };

    public NumberFormatter() { this(Locale.forLanguageTag("pt-BR"), false); }
    public NumberFormatter(Locale locale, boolean date1904) { this.locale = locale; this.date1904 = date1904; }

    public Locale locale() { return locale; }
    public boolean date1904() { return date1904; }
    public NumberFormatter withDate1904(boolean value) { return value == date1904 ? this : new NumberFormatter(locale, value); }
    public NumberFormatter withLocale(Locale value) { return value.equals(locale) ? this : new NumberFormatter(value, date1904); }

    public char decimalSeparator() { return DecimalFormatSymbols.getInstance(locale).getDecimalSeparator(); }
    public char groupingSeparator() { char c = DecimalFormatSymbols.getInstance(locale).getGroupingSeparator(); return c == ' ' || c == ' ' ? ' ' : c; }

    public FormattedValue format(CellValue value, String code) {
        String c = code == null || code.isBlank() ? "General" : code;
        return switch (value) {
            case EmptyValue e -> FormattedValue.of("");
            case ErrorValue e -> FormattedValue.of(locale.getLanguage().equals("pt") ? e.error().localized() : e.error().text());
            case BoolValue b -> FormattedValue.of(locale.getLanguage().equals("pt") ? (b.value() ? "VERDADEIRO" : "FALSO") : (b.value() ? "TRUE" : "FALSE"));
            case NumberValue n -> formatNumber(n.value(), c);
            case TextValue t -> formatText(t.value(), c);
            default -> FormattedValue.of(value.display());
        };
    }

    public String text(CellValue value, String code) { return format(value, code).text(); }

    public boolean isDateFormat(String code) {
        if (code == null) return false;
        Format f = parse(code);
        return !f.sections.isEmpty() && f.sections.getFirst().date;
    }

    public boolean isTextFormat(String code) { return code != null && code.strip().equals("@"); }

    public FormattedValue formatText(String text, String code) {
        Format f = parse(code);
        Section s = f.textSection();
        if (s == null) return FormattedValue.of(text);
        StringBuilder b = new StringBuilder();
        Character fill = null;
        for (Token t : s.tokens) {
            switch (t.kind) {
                case TEXT_AT -> b.append(text);
                case LITERAL -> b.append(t.text);
                case SPACE -> b.append(' ');
                case FILL -> fill = t.text.charAt(0);
                default -> { }
            }
        }
        return new FormattedValue(b.toString(), s.color, fill, false);
    }

    public FormattedValue formatNumber(double v, String code) {
        Format f = parse(code);
        if (f.general || f.sections.isEmpty()) return new FormattedValue(general(v), null, null, true);
        Section s = f.pick(v);
        if (s == null) return new FormattedValue("#".repeat(1), null, null, true);
        double value = v;
        boolean negate = false;
        if (f.explicitNegative(s, v)) value = Math.abs(v);
        else if (v < 0 && !s.date) { negate = true; value = -v; }
        String text;
        if (s.general) text = general(value);
        else if (s.date) {
            if (value < 0 || value > DateSerial.MAX) return new FormattedValue("#".repeat(8), null, null, true);
            text = formatDate(value, s);
        } else text = formatNumeric(value, s);
        if (negate && !isZeroText(text)) text = "-" + text;
        return new FormattedValue(text, s.color, s.fill, true);
    }

    private static boolean isZeroText(String t) { return t.chars().noneMatch(ch -> ch >= '1' && ch <= '9'); }

    public String general(double v) {
        String s = NumberValue.general(v);
        char d = decimalSeparator();
        return d == '.' ? s : s.replace('.', d);
    }

    public Format parse(String code) {
        synchronized (cache) {
            Format f = cache.get(code);
            if (f == null) { f = Format.parse(code); cache.put(code, f); }
            return f;
        }
    }

    private String formatNumeric(double value, Section s) {
        double v = value;
        if (s.percent > 0) v *= Math.pow(100, s.percent);
        if (s.scale > 0) v /= Math.pow(1000, s.scale);
        if (s.exponent) return formatScientific(v, s);
        if (s.fraction) return formatFraction(v, s);
        int decimals = s.decimalPlaceholders;
        BigDecimal bd = new BigDecimal(Double.toString(v)).setScale(decimals, RoundingMode.HALF_UP);
        String plain = bd.abs().toPlainString();
        String intDigits = plain.contains(".") ? plain.substring(0, plain.indexOf('.')) : plain;
        String fracDigits = plain.contains(".") ? plain.substring(plain.indexOf('.') + 1) : "";
        if (intDigits.equals("0")) intDigits = "";
        StringBuilder out = new StringBuilder();
        List<Token> intTokens = s.integerTokens, fracTokens = s.fractionTokens;
        int placeholders = 0;
        for (Token t : intTokens) if (t.kind == Kind.DIGIT) placeholders++;
        StringBuilder integer = new StringBuilder();
        int di = intDigits.length() - 1;
        int seen = 0;
        List<String> parts = new ArrayList<>();
        for (int i = intTokens.size() - 1; i >= 0; i--) {
            Token t = intTokens.get(i);
            if (t.kind == Kind.DIGIT) {
                seen++;
                if (di >= 0) {
                    if (seen == placeholders) { parts.add(intDigits.substring(0, di + 1)); di = -1; }
                    else parts.add(String.valueOf(intDigits.charAt(di--)));
                } else parts.add(t.text.equals("0") ? "0" : t.text.equals("?") ? " " : "");
            } else parts.add(literal(t));
        }
        if (placeholders == 0 && !intDigits.isEmpty() && s.decimalPoint) parts.add(intDigits);
        for (int i = parts.size() - 1; i >= 0; i--) integer.append(parts.get(i));
        String intText = integer.toString();
        if (s.grouping) intText = group(intText);
        out.append(intText);
        if (s.decimalPoint) {
            StringBuilder frac = new StringBuilder();
            int fi = 0;
            int[] marks = new int[fracTokens.size()];
            for (int k = 0; k < fracTokens.size(); k++) {
                Token t = fracTokens.get(k);
                if (t.kind == Kind.DIGIT) {
                    char ch = fi < fracDigits.length() ? fracDigits.charAt(fi) : '0';
                    fi++;
                    frac.append(ch);
                    marks[k] = frac.length() - 1;
                } else frac.append(literal(t));
            }
            StringBuilder trimmed = new StringBuilder(frac);
            for (int i = fracTokens.size() - 1; i >= 0; i--) {
                Token t = fracTokens.get(i);
                if (t.kind != Kind.DIGIT) continue;
                int mark = marks[i];
                if (mark < trimmed.length() && trimmed.charAt(mark) == '0' && !t.text.equals("0")) {
                    if (t.text.equals("#")) trimmed.deleteCharAt(mark); else trimmed.setCharAt(mark, ' ');
                } else break;
            }
            out.append(decimalSeparator()).append(trimmed);
        }
        StringBuilder full = new StringBuilder();
        for (Token t : s.prefix) full.append(literal(t));
        full.append(out);
        for (Token t : s.suffix) full.append(literal(t));
        return full.toString();
    }

    private String group(String digitsAndLiterals) {
        int start = 0;
        while (start < digitsAndLiterals.length() && !Character.isDigit(digitsAndLiterals.charAt(start))) start++;
        int end = start;
        while (end < digitsAndLiterals.length() && Character.isDigit(digitsAndLiterals.charAt(end))) end++;
        String digits = digitsAndLiterals.substring(start, end);
        StringBuilder b = new StringBuilder();
        char g = groupingSeparator();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) b.append(g);
            b.append(digits.charAt(i));
        }
        return digitsAndLiterals.substring(0, start) + b + digitsAndLiterals.substring(end);
    }

    private String literal(Token t) {
        return switch (t.kind) {
            case LITERAL -> t.text;
            case SPACE -> " ";
            case PERCENT -> "%";
            default -> "";
        };
    }

    private String formatScientific(double v, Section s) {
        int mantissaInt = Math.max(1, s.integerDigits);
        int decimals = s.decimalPlaceholders;
        if (v == 0) {
            StringBuilder b = new StringBuilder("0");
            if (decimals > 0) b.append(decimalSeparator()).append("0".repeat(decimals));
            b.append(s.exponentUpper ? "E" : "e").append(s.exponentPlus ? "+" : "").append("0".repeat(Math.max(1, s.exponentDigits)));
            return prefixSuffix(s, b.toString());
        }
        int exp = (int) Math.floor(Math.log10(Math.abs(v)));
        if (mantissaInt > 1) exp = Math.floorDiv(exp, mantissaInt) * mantissaInt;
        double mant = v / Math.pow(10, exp);
        BigDecimal m = new BigDecimal(mant).setScale(decimals, RoundingMode.HALF_UP);
        if (m.abs().compareTo(BigDecimal.TEN.pow(mantissaInt)) >= 0) { exp += mantissaInt; m = new BigDecimal(v / Math.pow(10, exp)).setScale(decimals, RoundingMode.HALF_UP); }
        String ms = m.toPlainString();
        if (decimalSeparator() != '.') ms = ms.replace('.', decimalSeparator());
        String es = String.valueOf(Math.abs(exp));
        while (es.length() < s.exponentDigits) es = "0" + es;
        String sign = exp < 0 ? "-" : s.exponentPlus ? "+" : "";
        return prefixSuffix(s, ms + (s.exponentUpper ? "E" : "e") + sign + es);
    }

    private String prefixSuffix(Section s, String body) {
        StringBuilder b = new StringBuilder();
        for (Token t : s.prefix) b.append(literal(t));
        b.append(body);
        for (Token t : s.suffix) b.append(literal(t));
        return b.toString();
    }

    private String formatFraction(double v, Section s) {
        long whole = s.integerDigits > 0 ? (long) Math.floor(v) : 0;
        double frac = v - whole;
        long num, den;
        if (s.fixedDenominator > 0) {
            den = s.fixedDenominator;
            num = Math.round(frac * den);
        } else {
            int maxDen = (int) Math.pow(10, Math.max(1, s.denominatorDigits)) - 1;
            long[] best = approximate(frac, maxDen);
            num = best[0]; den = best[1];
        }
        if (num == den && s.integerDigits > 0) { whole++; num = 0; }
        StringBuilder b = new StringBuilder();
        if (s.integerDigits > 0) {
            if (whole != 0 || num == 0) b.append(whole);
            if (num != 0) { if (whole != 0) b.append(' '); b.append(num).append('/').append(den); }
            else if (whole == 0) b.append("0");
        } else b.append(num).append('/').append(den);
        return prefixSuffix(s, b.toString());
    }

    private static long[] approximate(double x, int maxDen) {
        if (x == 0) return new long[]{0, 1};
        long bestN = 0, bestD = 1;
        double bestErr = Double.MAX_VALUE;
        for (int d = 1; d <= maxDen; d++) {
            long n = Math.round(x * d);
            double err = Math.abs(x - (double) n / d);
            if (err < bestErr - 1e-12) { bestErr = err; bestN = n; bestD = d; }
        }
        return new long[]{bestN, bestD};
    }

    private String formatDate(double v, Section s) {
        boolean localeEnglish = s.localeTag != null && s.localeTag.startsWith("en");
        Locale names = localeEnglish ? Locale.US : locale;
        int[] p = DateSerial.parts(v, date1904);
        double roundedSeconds = v;
        if (s.secondDecimals == 0) {
            double totalSeconds = Math.round(v * 86400.0);
            p = DateSerial.parts(totalSeconds / 86400.0, date1904);
            roundedSeconds = totalSeconds / 86400.0;
        }
        StringBuilder b = new StringBuilder();
        for (Token t : s.tokens) {
            switch (t.kind) {
                case YEAR -> b.append(t.text.length() <= 2 ? String.format("%02d", p[0] % 100) : String.valueOf(p[0]));
                case MONTH -> {
                    int n = t.text.length();
                    java.time.Month m = java.time.Month.of(Math.max(1, p[1]));
                    if (n == 1) b.append(p[1]);
                    else if (n == 2) b.append(String.format("%02d", p[1]));
                    else if (n == 3) b.append(shortMonth(m, names));
                    else if (n == 5) b.append(m.getDisplayName(TextStyle.FULL, names).substring(0, 1).toUpperCase(names));
                    else b.append(m.getDisplayName(TextStyle.FULL, names));
                }
                case DAY -> {
                    int n = t.text.length();
                    java.time.DayOfWeek dw = java.time.DayOfWeek.of(p[7] == 0 ? 7 : p[7]);
                    if (n == 1) b.append(p[2]);
                    else if (n == 2) b.append(String.format("%02d", p[2]));
                    else if (n == 3) b.append(shortDay(dw, names));
                    else b.append(dw.getDisplayName(TextStyle.FULL, names));
                }
                case HOUR -> {
                    int h = p[3];
                    if (s.ampm) { h = h % 12; if (h == 0) h = 12; }
                    b.append(t.text.length() >= 2 ? String.format("%02d", h) : String.valueOf(h));
                }
                case MINUTE -> b.append(t.text.length() >= 2 ? String.format("%02d", p[4]) : String.valueOf(p[4]));
                case SECOND -> b.append(t.text.length() >= 2 ? String.format("%02d", p[5]) : String.valueOf(p[5]));
                case SECOND_FRACTION -> {
                    int digits = t.text.length() - 1;
                    long ms = p[6];
                    String f = String.format("%03d", ms).substring(0, Math.min(3, digits));
                    b.append(decimalSeparator()).append(f);
                }
                case AMPM -> {
                    boolean pm = p[3] >= 12;
                    String up = t.text.toUpperCase(Locale.ROOT);
                    if (up.equals("A/P")) b.append(pm ? (Character.isUpperCase(t.text.charAt(2)) ? "P" : "p") : (Character.isUpperCase(t.text.charAt(0)) ? "A" : "a"));
                    else b.append(pm ? "PM" : "AM");
                }
                case ELAPSED_HOURS -> { long h = (long) Math.floor(roundedSeconds * 24 + 1e-9); b.append(pad(h, t.text.length() - 2)); }
                case ELAPSED_MINUTES -> { long m = (long) Math.floor(roundedSeconds * 1440 + 1e-9); b.append(pad(m, t.text.length() - 2)); }
                case ELAPSED_SECONDS -> { long sec = Math.round(v * 86400); b.append(pad(sec, t.text.length() - 2)); }
                case LITERAL -> b.append(t.text);
                case SPACE -> b.append(' ');
                default -> { }
            }
        }
        return b.toString();
    }

    private static String pad(long v, int width) { String s = String.valueOf(v); while (s.length() < width) s = "0" + s; return s; }

    private static String shortMonth(java.time.Month m, Locale l) {
        String s = m.getDisplayName(TextStyle.SHORT, l).replace(".", "");
        return s.length() > 3 && !l.getLanguage().equals("en") ? s.substring(0, 3) : s;
    }

    private static String shortDay(java.time.DayOfWeek d, Locale l) {
        String s = d.getDisplayName(TextStyle.SHORT, l).replace(".", "");
        return s.length() > 3 ? s.substring(0, 3) : s;
    }

    public static int paletteColor(int index) { return index >= 1 && index <= PALETTE.length ? PALETTE[index - 1] : 0xFF000000; }

    enum Kind { DIGIT, DECIMAL, GROUP, PERCENT, EXPONENT, SLASH, LITERAL, SPACE, FILL, TEXT_AT, YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, SECOND_FRACTION, AMPM, ELAPSED_HOURS, ELAPSED_MINUTES, ELAPSED_SECONDS, GENERAL }

    static final class Token {
        final Kind kind;
        final String text;
        Token(Kind kind, String text) { this.kind = kind; this.text = text; }
        @Override public String toString() { return kind + ":" + text; }
    }

    static final class Section {
        final List<Token> tokens = new ArrayList<>();
        final List<Token> prefix = new ArrayList<>(), suffix = new ArrayList<>(), integerTokens = new ArrayList<>(), fractionTokens = new ArrayList<>();
        Integer color;
        Character fill;
        String conditionOp;
        double conditionValue;
        boolean date, general, text, grouping, decimalPoint, exponent, exponentUpper, exponentPlus, fraction, ampm;
        int percent, scale, integerDigits, decimalPlaceholders, exponentDigits, denominatorDigits, fixedDenominator, secondDecimals;
        String localeTag;

        boolean hasCondition() { return conditionOp != null; }

        boolean matches(double v) {
            return switch (conditionOp) {
                case ">" -> v > conditionValue;
                case ">=" -> v >= conditionValue;
                case "<" -> v < conditionValue;
                case "<=" -> v <= conditionValue;
                case "=" -> v == conditionValue;
                case "<>" -> v != conditionValue;
                default -> true;
            };
        }
    }

    public static final class Format {
        final List<Section> sections = new ArrayList<>();
        boolean general;

        public boolean isDate() { return !sections.isEmpty() && sections.getFirst().date; }
        public boolean isGeneral() { return general; }
        public int sectionCount() { return sections.size(); }

        Section textSection() {
            if (sections.size() >= 4) return sections.get(3);
            for (Section s : sections) if (s.text) return s;
            if (sections.size() == 1 && sections.getFirst().text) return sections.getFirst();
            return null;
        }

        Section pick(double v) {
            List<Section> numeric = new ArrayList<>();
            for (int i = 0; i < sections.size(); i++) { Section s = sections.get(i); if (i < 3 && !(s.text && !s.date && s.integerTokens.isEmpty() && s.decimalPlaceholders == 0 && sections.size() > 1 && i == sections.size() - 1 && hasAt(s))) numeric.add(s); }
            if (numeric.isEmpty()) return sections.getFirst();
            boolean conditional = numeric.stream().anyMatch(Section::hasCondition);
            if (conditional) {
                for (Section s : numeric) if (s.hasCondition() && s.matches(v)) return s;
                for (Section s : numeric) if (!s.hasCondition()) return s;
                return null;
            }
            if (numeric.size() == 1) return numeric.getFirst();
            if (v > 0 || numeric.size() == 2 && v == 0) return numeric.get(0);
            if (v < 0) return numeric.get(1);
            return numeric.size() >= 3 ? numeric.get(2) : numeric.get(0);
        }

        private static boolean hasAt(Section s) { for (Token t : s.tokens) if (t.kind == Kind.TEXT_AT) return true; return false; }

        boolean explicitNegative(Section s, double v) {
            if (v >= 0) return false;
            int idx = sections.indexOf(s);
            if (s.hasCondition()) return true;
            return idx >= 1;
        }

        static Format parse(String code) {
            Format f = new Format();
            if (code.equalsIgnoreCase("General") || code.equalsIgnoreCase("Geral")) { f.general = true; return f; }
            for (String part : split(code)) f.sections.add(section(part));
            return f;
        }

        private static List<String> split(String code) {
            List<String> parts = new ArrayList<>();
            StringBuilder b = new StringBuilder();
            boolean quoted = false;
            for (int i = 0; i < code.length(); i++) {
                char c = code.charAt(i);
                if (c == '"') quoted = !quoted;
                if (c == '\\' && i + 1 < code.length() && !quoted) { b.append(c).append(code.charAt(++i)); continue; }
                if (c == ';' && !quoted) { parts.add(b.toString()); b.setLength(0); continue; }
                b.append(c);
            }
            parts.add(b.toString());
            return parts;
        }

        private static Section section(String code) {
            Section s = new Section();
            List<Token> t = s.tokens;
            int i = 0;
            while (i < code.length()) {
                char c = code.charAt(i);
                if (c == '[') {
                    int end = code.indexOf(']', i);
                    if (end < 0) { t.add(new Token(Kind.LITERAL, code.substring(i))); break; }
                    String inner = code.substring(i + 1, end);
                    bracket(s, inner);
                    i = end + 1;
                    continue;
                }
                if (c == '"') {
                    int end = code.indexOf('"', i + 1);
                    if (end < 0) end = code.length();
                    t.add(new Token(Kind.LITERAL, code.substring(i + 1, end)));
                    i = end + 1;
                    continue;
                }
                if (c == '\\' && i + 1 < code.length()) { t.add(new Token(Kind.LITERAL, String.valueOf(code.charAt(i + 1)))); i += 2; continue; }
                if (c == '_' && i + 1 < code.length()) { t.add(new Token(Kind.SPACE, " ")); i += 2; continue; }
                if (c == '*' && i + 1 < code.length()) { s.fill = code.charAt(i + 1); t.add(new Token(Kind.FILL, String.valueOf(code.charAt(i + 1)))); i += 2; continue; }
                if (c == '@') { s.text = true; t.add(new Token(Kind.TEXT_AT, "@")); i++; continue; }
                if (code.regionMatches(true, i, "General", 0, 7)) { s.general = true; t.add(new Token(Kind.GENERAL, "General")); i += 7; continue; }
                if (code.regionMatches(true, i, "AM/PM", 0, 5)) { s.ampm = true; s.date = true; t.add(new Token(Kind.AMPM, code.substring(i, i + 5))); i += 5; continue; }
                if (code.regionMatches(true, i, "A/P", 0, 3)) { s.ampm = true; s.date = true; t.add(new Token(Kind.AMPM, code.substring(i, i + 3))); i += 3; continue; }
                char lc = Character.toLowerCase(c);
                if (lc == 'y' || lc == 'a' && dateContext(code)) { int n = run(code, i, c); t.add(new Token(Kind.YEAR, "y".repeat(n))); s.date = true; i += n; continue; }
                if (lc == 'd') { int n = run(code, i, c); t.add(new Token(Kind.DAY, "d".repeat(Math.min(4, n)))); s.date = true; i += n; continue; }
                if (lc == 'h') { int n = run(code, i, c); t.add(new Token(Kind.HOUR, "h".repeat(n))); s.date = true; i += n; continue; }
                if (lc == 's') { int n = run(code, i, c); t.add(new Token(Kind.SECOND, "s".repeat(n))); s.date = true; i += n;
                    if (i < code.length() && code.charAt(i) == '.' && i + 1 < code.length() && code.charAt(i + 1) == '0') {
                        int z = 1; while (i + z < code.length() && code.charAt(i + z) == '0') z++;
                        t.add(new Token(Kind.SECOND_FRACTION, code.substring(i, i + z))); s.secondDecimals = z - 1; i += z;
                    }
                    continue; }
                if (lc == 'm') { int n = run(code, i, c); t.add(new Token(Kind.MONTH, "m".repeat(Math.min(5, n)))); s.date = true; i += n; continue; }
                if (lc == 'e' && i + 1 < code.length() && (code.charAt(i + 1) == '+' || code.charAt(i + 1) == '-') && !s.date) {
                    s.exponent = true; s.exponentUpper = c == 'E'; s.exponentPlus = code.charAt(i + 1) == '+';
                    t.add(new Token(Kind.EXPONENT, code.substring(i, i + 2)));
                    i += 2;
                    int z = 0; while (i < code.length() && (code.charAt(i) == '0' || code.charAt(i) == '#' || code.charAt(i) == '?')) { z++; i++; }
                    s.exponentDigits = z;
                    continue;
                }
                if (c == '0' || c == '#' || c == '?') { t.add(new Token(Kind.DIGIT, String.valueOf(c))); i++; continue; }
                if (c == '.') { t.add(new Token(Kind.DECIMAL, ".")); i++; continue; }
                if (c == ',') { t.add(new Token(Kind.GROUP, ",")); i++; continue; }
                if (c == '%') { s.percent++; t.add(new Token(Kind.PERCENT, "%")); i++; continue; }
                if (c == '/' && !s.date && hasDigitBefore(t)) { t.add(new Token(Kind.SLASH, "/")); s.fraction = true; i++; continue; }
                if (c == ' ') { t.add(new Token(Kind.LITERAL, " ")); i++; continue; }
                t.add(new Token(Kind.LITERAL, String.valueOf(c)));
                i++;
            }
            resolveMinutes(t);
            if (s.date) { s.fraction = false; return s; }
            analyzeNumeric(s);
            return s;
        }

        private static boolean dateContext(String code) {
            String l = code.toLowerCase(Locale.ROOT);
            return l.contains("d") || l.contains("m") || l.contains("aaaa") || l.contains("aa");
        }

        private static boolean hasDigitBefore(List<Token> t) { for (Token x : t) if (x.kind == Kind.DIGIT) return true; return false; }

        private static int run(String code, int i, char c) {
            int n = 0;
            while (i + n < code.length() && Character.toLowerCase(code.charAt(i + n)) == Character.toLowerCase(c)) n++;
            return n;
        }

        private static void bracket(Section s, String inner) {
            String u = inner.toUpperCase(Locale.ROOT);
            if (u.matches("H+") || u.matches("M+") || u.matches("S+")) {
                Kind k = u.charAt(0) == 'H' ? Kind.ELAPSED_HOURS : u.charAt(0) == 'M' ? Kind.ELAPSED_MINUTES : Kind.ELAPSED_SECONDS;
                s.tokens.add(new Token(k, "[" + inner + "]"));
                s.date = true;
                return;
            }
            if (u.startsWith("$")) {
                int dash = inner.indexOf('-');
                String symbol = dash >= 0 ? inner.substring(1, dash) : inner.substring(1);
                if (dash >= 0) {
                    String lcid = inner.substring(dash + 1);
                    s.localeTag = lcid.equalsIgnoreCase("409") ? "en-US" : lcid.equalsIgnoreCase("416") ? "pt-BR" : null;
                }
                if (!symbol.isEmpty()) s.tokens.add(new Token(Kind.LITERAL, symbol));
                return;
            }
            if (u.matches("[<>=]{1,2}-?[0-9.]+")) {
                int k = 0; while (k < u.length() && "<>=".indexOf(u.charAt(k)) >= 0) k++;
                s.conditionOp = u.substring(0, k);
                s.conditionValue = Double.parseDouble(u.substring(k));
                return;
            }
            if (u.startsWith("COLOR") || u.startsWith("COR")) {
                try { s.color = paletteColor(Integer.parseInt(u.replaceAll("[^0-9]", ""))); } catch (NumberFormatException ignored) { }
                return;
            }
            Integer c = COLORS.get(u);
            if (c == null) c = COLORS_PT.get(u);
            if (c != null) s.color = c;
        }

        private static void resolveMinutes(List<Token> t) {
            for (int i = 0; i < t.size(); i++) {
                Token tok = t.get(i);
                if (tok.kind != Kind.MONTH || tok.text.length() > 2) continue;
                boolean afterHour = false, beforeSecond = false;
                for (int j = i - 1; j >= 0; j--) {
                    Kind k = t.get(j).kind;
                    if (k == Kind.HOUR || k == Kind.ELAPSED_HOURS) { afterHour = true; break; }
                    if (k == Kind.DAY || k == Kind.YEAR || k == Kind.MONTH || k == Kind.SECOND) break;
                }
                for (int j = i + 1; j < t.size(); j++) {
                    Kind k = t.get(j).kind;
                    if (k == Kind.SECOND || k == Kind.ELAPSED_SECONDS) { beforeSecond = true; break; }
                    if (k == Kind.DAY || k == Kind.YEAR || k == Kind.MONTH || k == Kind.HOUR) break;
                }
                if (afterHour || beforeSecond) t.set(i, new Token(Kind.MINUTE, tok.text));
            }
        }

        private static void analyzeNumeric(Section s) {
            List<Token> t = s.tokens;
            int firstDigit = -1, lastDigit = -1;
            for (int i = 0; i < t.size(); i++) if (t.get(i).kind == Kind.DIGIT || t.get(i).kind == Kind.DECIMAL) { if (firstDigit < 0) firstDigit = i; lastDigit = i; }
            if (firstDigit < 0) {
                for (Token x : t) if (x.kind != Kind.GROUP) s.prefix.add(x);
                if (!s.text && s.prefix.stream().noneMatch(x -> x.kind == Kind.GENERAL)) s.integerDigits = 0;
                if (s.prefix.stream().anyMatch(x -> x.kind == Kind.GENERAL)) { s.general = true; s.prefix.removeIf(x -> x.kind == Kind.GENERAL); }
                return;
            }
            if (s.exponent || s.fraction) {
                boolean inFraction = false, afterExp = false, afterDecimal = false;
                int slashIndex = -1;
                for (int i = 0; i < t.size(); i++) if (t.get(i).kind == Kind.SLASH) slashIndex = i;
                for (int i = 0; i < t.size(); i++) {
                    Token x = t.get(i);
                    if (i < firstDigit) { if (x.kind != Kind.GROUP) s.prefix.add(x); continue; }
                    if (x.kind == Kind.EXPONENT) { afterExp = true; continue; }
                    if (x.kind == Kind.DECIMAL) { afterDecimal = true; s.decimalPoint = true; continue; }
                    if (x.kind == Kind.GROUP) { s.grouping = true; continue; }
                    if (s.fraction) {
                        if (x.kind == Kind.DIGIT && i < slashIndex) {
                            boolean numerator = true;
                            for (int j = i + 1; j < slashIndex; j++) if (t.get(j).kind == Kind.LITERAL && t.get(j).text.equals(" ")) numerator = false;
                            if (!numerator) s.integerDigits++;
                        }
                        if (i > slashIndex && x.kind == Kind.DIGIT) { s.denominatorDigits++; inFraction = true; }
                        if (i > slashIndex && x.kind == Kind.LITERAL && x.text.matches("[1-9]")) { StringBuilder d = new StringBuilder(); int j = i; while (j < t.size() && t.get(j).kind == Kind.LITERAL && t.get(j).text.matches("[0-9]")) d.append(t.get(j++).text); if (s.fixedDenominator == 0) s.fixedDenominator = Integer.parseInt(d.toString()); }
                        if (i > lastDigit && x.kind != Kind.DIGIT && !(x.kind == Kind.LITERAL && x.text.matches("[0-9]"))) s.suffix.add(x);
                        continue;
                    }
                    if (x.kind == Kind.DIGIT) { if (afterExp) continue; if (afterDecimal) s.decimalPlaceholders++; else s.integerDigits++; continue; }
                    if (i > lastDigit && !afterExp) s.suffix.add(x);
                    else if (afterExp && x.kind != Kind.DIGIT) s.suffix.add(x);
                }
                if (s.fraction) {
                    int fixed = 0;
                    boolean seenSlash = false;
                    StringBuilder den = new StringBuilder();
                    for (Token x : t) {
                        if (x.kind == Kind.SLASH) { seenSlash = true; continue; }
                        if (seenSlash && (x.kind == Kind.DIGIT && !x.text.equals("?") && !x.text.equals("#") || x.kind == Kind.LITERAL && x.text.matches("[0-9]"))) den.append(x.kind == Kind.DIGIT ? x.text : x.text);
                        else if (seenSlash && den.length() > 0) break;
                    }
                    if (den.length() > 0 && den.toString().matches("[0-9]+") && !den.toString().matches("0+")) fixed = Integer.parseInt(den.toString());
                    if (fixed > 0) s.fixedDenominator = fixed;
                }
                return;
            }
            boolean afterDecimal = false;
            int lastIntDigit = -1;
            for (int i = firstDigit; i <= lastDigit; i++) if (t.get(i).kind == Kind.DECIMAL) break; else if (t.get(i).kind == Kind.DIGIT) lastIntDigit = i;
            for (int i = 0; i < t.size(); i++) {
                Token x = t.get(i);
                if (i < firstDigit) { if (x.kind == Kind.GROUP) continue; s.prefix.add(x); continue; }
                if (i > lastDigit) {
                    if (x.kind == Kind.GROUP) { if (i == lastDigit + 1 || t.get(i - 1).kind == Kind.GROUP) s.scale++; continue; }
                    s.suffix.add(x);
                    continue;
                }
                if (x.kind == Kind.DECIMAL) { afterDecimal = true; s.decimalPoint = true; continue; }
                if (x.kind == Kind.GROUP) {
                    boolean between = i > firstDigit && i < (lastIntDigit < 0 ? lastDigit : lastIntDigit);
                    if (between && !afterDecimal) s.grouping = true;
                    else if (!afterDecimal) s.scale++;
                    continue;
                }
                if (afterDecimal) { if (x.kind == Kind.DIGIT) s.decimalPlaceholders++; s.fractionTokens.add(x); }
                else { if (x.kind == Kind.DIGIT) s.integerDigits++; s.integerTokens.add(x); }
            }
        }
    }
}
