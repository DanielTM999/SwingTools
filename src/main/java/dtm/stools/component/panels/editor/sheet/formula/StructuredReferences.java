package dtm.stools.component.panels.editor.sheet.formula;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StructuredReferences {
    private StructuredReferences() {}

    public static StructuredRefNode parse(String table, String body, FormulaLocale locale) {
        String b = body.strip();
        List<String> items = new ArrayList<>();
        String first = null, last = null;
        boolean thisRow = false;
        if (b.startsWith("@")) {
            thisRow = true;
            String rest = b.substring(1).strip();
            if (rest.startsWith("[")) rest = rest.substring(1, rest.lastIndexOf(']'));
            if (!rest.isEmpty()) first = unescape(rest);
            return new StructuredRefNode(table, List.of("#This Row"), first, null, true);
        }
        if (!b.contains("[")) {
            if (b.startsWith("#")) items.add(special(b));
            else if (!b.isEmpty()) first = unescape(b);
            return new StructuredRefNode(table, items, first, null, false);
        }
        List<String> parts = new ArrayList<>();
        int depth = 0, start = -1;
        for (int i = 0; i < b.length(); i++) {
            char c = b.charAt(i);
            if (c == '\'' && i + 1 < b.length()) { i++; continue; }
            if (c == '[') { if (depth == 0) start = i + 1; depth++; }
            else if (c == ']') { depth--; if (depth == 0) parts.add(b.substring(start, i)); }
            else if (depth == 0 && c == ':' && !parts.isEmpty()) parts.add(":");
        }
        for (int i = 0; i < parts.size(); i++) {
            String p = parts.get(i).strip();
            if (p.equals(":")) continue;
            if (p.startsWith("#")) {
                String s = special(p);
                if (s.equals("#This Row")) thisRow = true;
                items.add(s);
            } else if (p.startsWith("@")) {
                thisRow = true;
                items.add("#This Row");
                if (p.length() > 1) first = unescape(p.substring(1));
            } else if (first == null) first = unescape(p);
            else last = unescape(p);
        }
        return new StructuredRefNode(table, items, first, last, thisRow);
    }

    private static String special(String s) {
        String u = s.toUpperCase(Locale.ROOT).replace(" ", "");
        return switch (u) {
            case "#ALL", "#TUDO" -> "#All";
            case "#DATA", "#DADOS" -> "#Data";
            case "#HEADERS", "#CABEÇALHOS" -> "#Headers";
            case "#TOTALS", "#TOTAIS" -> "#Totals";
            case "#THISROW", "#ESTALINHA" -> "#This Row";
            default -> s;
        };
    }

    private static String unescape(String s) { return s.replace("'[", "[").replace("']", "]").replace("'#", "#").replace("''", "'"); }

    public static String escape(String s) { return s.replace("'", "''").replace("[", "'[").replace("]", "']").replace("#", "'#"); }

    public static String print(StructuredRefNode n) {
        StringBuilder b = new StringBuilder(n.table());
        if (n.thisRow() && n.lastColumn() == null) {
            if (n.firstColumn() == null) return b.append("[#This Row]").toString();
            String col = escape(n.firstColumn());
            return b.append("[@").append(col.matches("[\\p{L}\\p{N}_]+") ? col : "[" + col + "]").append("]").toString();
        }
        List<String> parts = new ArrayList<>();
        for (String i : n.items()) parts.add("[" + i + "]");
        if (n.firstColumn() != null) {
            String col = "[" + escape(n.firstColumn()) + "]";
            if (n.lastColumn() != null) col += ":[" + escape(n.lastColumn()) + "]";
            parts.add(col);
        }
        if (parts.isEmpty()) return b.append("[]").toString();
        if (parts.size() == 1 && n.lastColumn() == null) {
            String only = parts.getFirst();
            return b.append(only).toString();
        }
        return b.append("[").append(String.join(",", parts)).append("]").toString();
    }
}
