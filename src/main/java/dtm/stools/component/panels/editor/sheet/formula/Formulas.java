package dtm.stools.component.panels.editor.sheet.formula;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;

import java.util.LinkedHashMap;
import java.util.Map;

public final class Formulas {
    private static final int CACHE_SIZE = 8192;
    private static final Map<String, FormulaNode> CACHE = new LinkedHashMap<>(256, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, FormulaNode> eldest) { return size() > CACHE_SIZE; }
    };

    private Formulas() {}

    public static FormulaNode parseCanonical(String canonical) {
        synchronized (CACHE) {
            FormulaNode n = CACHE.get(canonical);
            if (n != null) return n;
        }
        FormulaNode node = FormulaParser.parse(canonical, FormulaLocale.EN);
        synchronized (CACHE) { CACHE.put(canonical, node); }
        return node;
    }

    public static String toCanonical(String text, FormulaLocale locale) { return toCanonical(text, locale, null, false); }

    public static String toCanonical(String text, FormulaLocale locale, CellAddress host, boolean r1c1) {
        String t = text.startsWith("=") ? text.substring(1) : text;
        if (locale == FormulaLocale.EN && !r1c1) {
            FormulaNode n = FormulaParser.parse(t, locale);
            return FormulaPrinter.canonical(n);
        }
        return FormulaPrinter.canonical(FormulaParser.parse(t, locale, host, r1c1));
    }

    public static String toDisplay(String canonical, FormulaLocale locale) { return toDisplay(canonical, locale, null, false); }

    public static String toDisplay(String canonical, FormulaLocale locale, CellAddress host, boolean r1c1) {
        if (locale == FormulaLocale.EN && !r1c1) return canonical;
        try { return new FormulaPrinter(locale, host, r1c1).print(parseCanonical(canonical)); }
        catch (RuntimeException e) { return canonical; }
    }

    public static boolean isFormula(String input) { return input != null && input.length() > 1 && input.charAt(0) == '='; }
}
