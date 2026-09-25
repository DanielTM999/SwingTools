package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.formula.FormulaLexer;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.FormulaToken;
import dtm.stools.component.panels.editor.sheet.formula.RefNode;
import dtm.stools.component.panels.editor.sheet.formula.TokenType;
import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.awt.Color;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FormulaHighlighter {
    public static final Color[] COLORS = {new Color(0x4472C4), new Color(0xC0504D), new Color(0x7030A0), new Color(0x2E8B57), new Color(0xB8860B), new Color(0x00838F), new Color(0xD81B60), new Color(0x5D4037)};

    public record Span(int start, int end, Color color, TokenType type, String sheet, CellRange range) {}

    private FormulaHighlighter() {}

    public static List<Span> analyze(String text, FormulaLocale locale) {
        List<Span> spans = new ArrayList<>();
        if (text == null || !text.startsWith("=")) return spans;
        List<FormulaToken> tokens = new FormulaLexer(text.substring(1), locale).tokenizeLenient();
        Map<String, Color> colors = new LinkedHashMap<>();
        for (FormulaToken t : tokens) {
            int start = t.start() + 1, end = t.end() + 1;
            switch (t.type()) {
                case REF -> {
                    RefNode r = (RefNode) t.value();
                    String key = (r.sheet() == null ? "" : r.sheet().toUpperCase() + "!") + r.range().toA1();
                    Color c = colors.computeIfAbsent(key, k -> COLORS[colors.size() % COLORS.length]);
                    spans.add(new Span(start, end, c, t.type(), r.sheet(), r.range()));
                }
                case FUNCTION -> spans.add(new Span(start, end, new Color(0x444444), t.type(), null, null));
                case STRING -> spans.add(new Span(start, end, new Color(0xA31515), t.type(), null, null));
                case NUMBER, BOOL -> spans.add(new Span(start, end, new Color(0x098658), t.type(), null, null));
                case ERROR -> spans.add(new Span(start, end, new Color(0xCC0000), t.type(), null, null));
                default -> { }
            }
        }
        return spans;
    }

    public static int[] tokenAt(String text, int caret, FormulaLocale locale) {
        if (text == null || !text.startsWith("=")) return null;
        for (FormulaToken t : new FormulaLexer(text.substring(1), locale).tokenizeLenient()) {
            if (t.type() == TokenType.REF && caret >= t.start() + 1 && caret <= t.end() + 1) return new int[]{t.start() + 1, t.end() + 1};
        }
        return null;
    }

    public static boolean acceptsReference(String text, int caret) {
        if (text == null || !text.startsWith("=") || caret < 1) return false;
        int k = caret - 1;
        while (k >= 0 && text.charAt(k) == ' ') k--;
        if (k < 0) return false;
        char c = text.charAt(k);
        return "=(,;+-*/^&<>:%{\\".indexOf(c) >= 0;
    }

    public static String currentIdentifier(String text, int caret) {
        if (text == null || !text.startsWith("=")) return null;
        int k = caret;
        while (k > 1 && (Character.isLetterOrDigit(text.charAt(k - 1)) || text.charAt(k - 1) == '.' || text.charAt(k - 1) == '_')) k--;
        if (k == caret) return null;
        if (k > 0 && (text.charAt(k - 1) == '"' || Character.isDigit(text.charAt(k)))) return null;
        int quotes = 0;
        for (int i = 0; i < k; i++) if (text.charAt(i) == '"') quotes++;
        if (quotes % 2 == 1) return null;
        return text.substring(k, caret);
    }

    public record CallContext(String function, int argument) {}

    public static CallContext callAt(String text, int caret, FormulaLocale locale) {
        if (text == null || !text.startsWith("=")) return null;
        List<Object[]> stack = new ArrayList<>();
        boolean quoted = false;
        int depthBrace = 0;
        for (int i = 1; i < Math.min(caret, text.length()); i++) {
            char c = text.charAt(i);
            if (c == '"') { quoted = !quoted; continue; }
            if (quoted) continue;
            if (c == '{') depthBrace++;
            else if (c == '}') depthBrace--;
            else if (c == '(') {
                int k = i;
                while (k > 1 && (Character.isLetterOrDigit(text.charAt(k - 1)) || text.charAt(k - 1) == '.' || text.charAt(k - 1) == '_')) k--;
                stack.add(new Object[]{text.substring(k, i), 0});
            } else if (c == ')') { if (!stack.isEmpty()) stack.removeLast(); }
            else if (c == locale.argumentSeparator() && depthBrace == 0 && !stack.isEmpty()) stack.getLast()[1] = (int) stack.getLast()[1] + 1;
        }
        if (stack.isEmpty()) return null;
        Object[] top = stack.getLast();
        String name = (String) top[0];
        if (name.isEmpty()) return null;
        return new CallContext(name, (int) top[1]);
    }
}
