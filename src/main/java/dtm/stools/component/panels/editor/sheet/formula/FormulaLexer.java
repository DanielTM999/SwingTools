package dtm.stools.component.panels.editor.sheet.formula;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellError;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FormulaLexer {
    private static final String SHEET_UNQUOTED = "[\\p{L}_\\\\][\\p{L}\\p{N}_.]*";
    private static final String SHEET_QUOTED = "'(?:[^']|'')+'";
    private static final Pattern PREFIX = Pattern.compile("(?:\\[([^\\]]+)\\])?(" + SHEET_QUOTED + "|" + SHEET_UNQUOTED + "(?::" + SHEET_UNQUOTED + ")?)!");
    private static final Pattern CELL = Pattern.compile("(\\$?)([A-Za-z]{1,3})(\\$?)([0-9]{1,7})");
    private static final Pattern AREA_A1 = Pattern.compile("(\\$?)([A-Za-z]{1,3})(\\$?)([0-9]{1,7})(?::(\\$?)([A-Za-z]{1,3})(\\$?)([0-9]{1,7}))?");
    private static final Pattern COLUMNS = Pattern.compile("(\\$?)([A-Za-z]{1,3}):(\\$?)([A-Za-z]{1,3})");
    private static final Pattern ROWS = Pattern.compile("(\\$?)([0-9]{1,7}):(\\$?)([0-9]{1,7})");
    private static final Pattern R1C1 = Pattern.compile("[Rr](\\[-?[0-9]+\\]|[0-9]+)?[Cc](\\[-?[0-9]+\\]|[0-9]+)?(?::[Rr](\\[-?[0-9]+\\]|[0-9]+)?[Cc](\\[-?[0-9]+\\]|[0-9]+)?)?");
    private static final Pattern IDENT = Pattern.compile("[\\p{L}_\\\\][\\p{L}\\p{N}_.\\\\?]*");
    private static final Pattern NUMBER_EN = Pattern.compile("[0-9]+(?:\\.[0-9]*)?(?:[eE][+-]?[0-9]+)?|\\.[0-9]+(?:[eE][+-]?[0-9]+)?");
    private static final Pattern NUMBER_COMMA = Pattern.compile("[0-9]+(?:,[0-9]*)?(?:[eE][+-]?[0-9]+)?|,[0-9]+(?:[eE][+-]?[0-9]+)?");

    private final String text;
    private final FormulaLocale locale;
    private final CellAddress host;
    private final boolean r1c1;
    private final List<FormulaToken> tokens = new ArrayList<>();
    private int pos;

    public FormulaLexer(String text, FormulaLocale locale) { this(text, locale, null, false); }

    public FormulaLexer(String text, FormulaLocale locale, CellAddress host, boolean r1c1) {
        this.text = text;
        this.locale = locale;
        this.host = host;
        this.r1c1 = r1c1 && host != null;
    }

    public static List<FormulaToken> tokenize(String text, FormulaLocale locale) { return new FormulaLexer(text, locale).tokenize(); }

    public List<FormulaToken> tokenize() {
        tokens.clear();
        pos = 0;
        while (pos < text.length()) next();
        tokens.add(new FormulaToken(TokenType.END, "", text.length(), text.length(), null));
        return tokens;
    }

    public List<FormulaToken> tokenizeLenient() {
        tokens.clear();
        pos = 0;
        while (pos < text.length()) {
            int start = pos;
            try { next(); } catch (FormulaException e) {
                pos = Math.max(start + 1, pos);
                tokens.add(new FormulaToken(TokenType.NAME, text.substring(start, Math.min(pos, text.length())), start, Math.min(pos, text.length()), null));
            }
        }
        tokens.add(new FormulaToken(TokenType.END, "", text.length(), text.length(), null));
        return tokens;
    }

    private void add(TokenType type, int start, Object value) { tokens.add(new FormulaToken(type, text.substring(start, pos), start, pos, value)); }

    private void next() {
        char c = text.charAt(pos);
        int start = pos;
        if (Character.isWhitespace(c)) {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
            add(TokenType.SPACE, start, null);
            return;
        }
        if (c == '"') { string(); return; }
        if (c == '#') {
            if (error()) return;
            pos++;
            add(TokenType.OPERATOR, start, "#");
            return;
        }
        if (c == locale.argumentSeparator()) { pos++; add(TokenType.SEPARATOR, start, null); return; }
        if (c == ';') { pos++; add(TokenType.ROW_SEPARATOR, start, null); return; }
        if (c == '\\' && locale.arrayColumnSeparator() == '\\' && inBraces()) { pos++; add(TokenType.COLUMN_SEPARATOR, start, null); return; }
        if (Character.isDigit(c) || c == locale.decimalSeparator() && pos + 1 < text.length() && Character.isDigit(text.charAt(pos + 1))) {
            if (reference(null, null, null)) return;
            number();
            return;
        }
        if (c == '\'' || c == '[' || c == '$' || Character.isLetter(c) || c == '_' || c == '\\') {
            if (c == '[' && structured(null, start)) return;
            if (prefixed()) return;
            if (c != '\'' && reference(null, null, null)) return;
            if (c != '\'' && c != '[' && identifier()) return;
            throw new FormulaException("Unexpected character '" + c + "'", pos);
        }
        switch (c) {
            case '(' -> { pos++; add(TokenType.LPAREN, start, null); }
            case ')' -> { pos++; add(TokenType.RPAREN, start, null); }
            case '{' -> { pos++; add(TokenType.LBRACE, start, null); }
            case '}' -> { pos++; add(TokenType.RBRACE, start, null); }
            case ',' -> { pos++; add(inBraces() ? TokenType.COLUMN_SEPARATOR : TokenType.SEPARATOR, start, null); }
            case '<' -> { pos++; if (pos < text.length() && (text.charAt(pos) == '=' || text.charAt(pos) == '>')) pos++; add(TokenType.OPERATOR, start, text.substring(start, pos)); }
            case '>' -> { pos++; if (pos < text.length() && text.charAt(pos) == '=') pos++; add(TokenType.OPERATOR, start, text.substring(start, pos)); }
            case '+', '-', '*', '/', '^', '&', '=', '%', ':', '@' -> { pos++; add(TokenType.OPERATOR, start, String.valueOf(c)); }
            default -> throw new FormulaException("Unexpected character '" + c + "'", pos);
        }
    }

    private boolean inBraces() {
        int depth = 0;
        for (FormulaToken t : tokens) { if (t.type() == TokenType.LBRACE) depth++; else if (t.type() == TokenType.RBRACE) depth--; }
        return depth > 0;
    }

    private void string() {
        int start = pos;
        pos++;
        StringBuilder b = new StringBuilder();
        while (true) {
            if (pos >= text.length()) throw new FormulaException("Unterminated string", start);
            char ch = text.charAt(pos);
            if (ch == '"') {
                if (pos + 1 < text.length() && text.charAt(pos + 1) == '"') { b.append('"'); pos += 2; continue; }
                pos++;
                break;
            }
            b.append(ch);
            pos++;
        }
        add(TokenType.STRING, start, b.toString());
    }

    private boolean error() {
        String rest = text.substring(pos).toUpperCase(Locale.ROOT);
        CellError best = null;
        int len = 0;
        for (CellError e : CellError.values()) {
            for (String form : new String[]{e.text(), e.localized().toUpperCase(Locale.ROOT)}) {
                if (rest.startsWith(form) && form.length() > len) { best = e; len = form.length(); }
            }
        }
        if (best == null) return false;
        int start = pos;
        pos += len;
        add(TokenType.ERROR, start, best);
        return true;
    }

    private void number() {
        int start = pos;
        Matcher m = (locale.decimalSeparator() == ',' ? NUMBER_COMMA : NUMBER_EN).matcher(text).region(pos, text.length());
        if (!m.lookingAt()) throw new FormulaException("Invalid number", pos);
        pos = m.end();
        String n = m.group().replace(locale.decimalSeparator(), '.');
        add(TokenType.NUMBER, start, Double.parseDouble(n));
    }

    private boolean prefixed() {
        Matcher m = PREFIX.matcher(text).region(pos, text.length());
        if (!m.lookingAt()) return false;
        String external = m.group(1);
        String sheetText = m.group(2);
        String sheet, sheetEnd = null;
        if (sheetText.startsWith("'")) {
            String inner = sheetText.substring(1, sheetText.length() - 1).replace("''", "'");
            int colon = inner.indexOf(':');
            if (colon > 0 && !inner.substring(colon + 1).isEmpty()) { sheet = inner.substring(0, colon); sheetEnd = inner.substring(colon + 1); }
            else sheet = inner;
        } else {
            int colon = sheetText.indexOf(':');
            if (colon > 0) { sheet = sheetText.substring(0, colon); sheetEnd = sheetText.substring(colon + 1); }
            else sheet = sheetText;
        }
        int start = pos;
        int save = pos;
        pos = m.end();
        if (pos < text.length() && text.charAt(pos) == '#') {
            if (error()) {
                FormulaToken e = tokens.removeLast();
                tokens.add(new FormulaToken(TokenType.ERROR, text.substring(start, pos), start, pos, e.value()));
                return true;
            }
        }
        if (reference(sheet, sheetEnd, external, start)) return true;
        Matcher id = IDENT.matcher(text).region(pos, text.length());
        if (id.lookingAt()) {
            pos = id.end();
            tokens.add(new FormulaToken(TokenType.NAME, text.substring(start, pos), start, pos, new NameNode(sheet, id.group())));
            return true;
        }
        pos = save;
        return false;
    }

    private boolean reference(String sheet, String sheetEnd, String external) { return reference(sheet, sheetEnd, external, pos); }

    private boolean reference(String sheet, String sheetEnd, String external, int tokenStart) {
        if (r1c1) {
            Matcher m = R1C1.matcher(text).region(pos, text.length());
            if (m.lookingAt() && m.end() > pos + 1 && boundary(m.end(), false)) {
                RefPart a = r1c1Part(m.group(1), m.group(2)), b = m.group(3) != null || m.group(4) != null ? r1c1Part(m.group(3), m.group(4)) : null;
                if (m.group().indexOf(':') >= 0 && b == null) b = r1c1Part(null, null);
                pos = m.end();
                tokens.add(new FormulaToken(TokenType.REF, text.substring(tokenStart, pos), tokenStart, pos, new RefNode(sheet, sheetEnd, a, b, external)));
                return true;
            }
        }
        Matcher m = AREA_A1.matcher(text).region(pos, text.length());
        if (m.lookingAt() && boundary(m.end(), true)) {
            RefPart a = part(m.group(1), m.group(2), m.group(3), m.group(4));
            RefPart b = m.group(6) != null ? part(m.group(5), m.group(6), m.group(7), m.group(8)) : null;
            if (a != null && (m.group(6) == null || b != null)) {
                pos = m.end();
                tokens.add(new FormulaToken(TokenType.REF, text.substring(tokenStart, pos), tokenStart, pos, new RefNode(sheet, sheetEnd, a, b, external)));
                return true;
            }
        }
        m = COLUMNS.matcher(text).region(pos, text.length());
        if (m.lookingAt() && boundary(m.end(), true)) {
            try {
                int c1 = CellAddress.columnIndex(m.group(2)), c2 = CellAddress.columnIndex(m.group(4));
                pos = m.end();
                tokens.add(new FormulaToken(TokenType.REF, text.substring(tokenStart, pos), tokenStart, pos, new RefNode(sheet, sheetEnd, RefPart.column(c1, !m.group(1).isEmpty()), RefPart.column(c2, !m.group(3).isEmpty()), external)));
                return true;
            } catch (IllegalArgumentException ignored) {}
        }
        m = ROWS.matcher(text).region(pos, text.length());
        if (m.lookingAt() && boundary(m.end(), true)) {
            int r1 = Integer.parseInt(m.group(2)) - 1, r2 = Integer.parseInt(m.group(4)) - 1;
            if (r1 >= 0 && r2 >= 0 && r1 < CellAddress.MAX_ROWS && r2 < CellAddress.MAX_ROWS) {
                pos = m.end();
                tokens.add(new FormulaToken(TokenType.REF, text.substring(tokenStart, pos), tokenStart, pos, new RefNode(sheet, sheetEnd, RefPart.row(r1, !m.group(1).isEmpty()), RefPart.row(r2, !m.group(3).isEmpty()), external)));
                return true;
            }
        }
        return false;
    }

    private RefPart r1c1Part(String r, String c) {
        int row, col;
        boolean rowAbs = false, colAbs = false;
        if (r == null) row = host.row(); else if (r.startsWith("[")) row = host.row() + Integer.parseInt(r.substring(1, r.length() - 1)); else { row = Integer.parseInt(r) - 1; rowAbs = true; }
        if (c == null) col = host.column(); else if (c.startsWith("[")) col = host.column() + Integer.parseInt(c.substring(1, c.length() - 1)); else { col = Integer.parseInt(c) - 1; colAbs = true; }
        if (row < 0 || col < 0 || row >= CellAddress.MAX_ROWS || col >= CellAddress.MAX_COLUMNS) throw new FormulaException("Invalid R1C1 reference", pos);
        return RefPart.cell(row, col, rowAbs, colAbs);
    }

    private boolean boundary(int end, boolean allowParen) {
        if (end >= text.length()) return true;
        char n = text.charAt(end);
        return !(Character.isLetterOrDigit(n) || n == '_' || n == '.' || n == '(' || n == '[' || n == '!' || n == '\\' && locale.arrayColumnSeparator() != '\\');
    }

    private static RefPart part(String colAbs, String col, String rowAbs, String row) {
        try {
            int c = CellAddress.columnIndex(col);
            int r = Integer.parseInt(row) - 1;
            if (r < 0 || r >= CellAddress.MAX_ROWS) return null;
            return RefPart.cell(r, c, !rowAbs.isEmpty(), !colAbs.isEmpty());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean identifier() {
        Matcher m = IDENT.matcher(text).region(pos, text.length());
        if (!m.lookingAt()) return false;
        int start = pos;
        String id = m.group();
        int end = m.end();
        if (end < text.length() && text.charAt(end) == '(') {
            pos = end;
            add(TokenType.FUNCTION, start, locale.canonicalFunction(id));
            return true;
        }
        if (end < text.length() && text.charAt(end) == '[') {
            pos = end;
            return structured(id, start);
        }
        pos = end;
        String upper = id.toUpperCase(Locale.ROOT);
        if (upper.equals("TRUE") || upper.equals(locale.trueText())) { add(TokenType.BOOL, start, Boolean.TRUE); return true; }
        if (upper.equals("FALSE") || upper.equals(locale.falseText())) { add(TokenType.BOOL, start, Boolean.FALSE); return true; }
        add(TokenType.NAME, start, new NameNode(null, id));
        return true;
    }

    private boolean structured(String table, int start) {
        if (pos >= text.length() || text.charAt(pos) != '[') return false;
        int depth = 0, i = pos;
        for (; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '\'' && i + 1 < text.length()) { i++; continue; }
            if (ch == '[') depth++;
            else if (ch == ']') { depth--; if (depth == 0) break; }
        }
        if (depth != 0) {
            if (table == null) return false;
            throw new FormulaException("Unterminated structured reference", start);
        }
        String body = text.substring(pos + 1, i);
        if (table == null && i + 1 < text.length() && text.charAt(i + 1) != ']' && Character.isLetterOrDigit(text.charAt(i + 1)) && body.chars().allMatch(Character::isDigit)) return false;
        pos = i + 1;
        tokens.add(new FormulaToken(TokenType.STRUCTURED, text.substring(start, pos), start, pos, StructuredReferences.parse(table, body, locale)));
        return true;
    }
}
