package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.provider.SheetSearchContext;
import dtm.stools.component.panels.editor.sheet.provider.SheetSearchHit;
import dtm.stools.component.panels.editor.sheet.provider.SheetSearchOptions;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class SheetSearchSupport implements SheetSearchContext {
    private static final int LIMIT = 10_000;
    private final SheetEditor editor;

    SheetSearchSupport(SheetEditor editor) { this.editor = editor; }

    @Override public Component owner() { return editor; }

    private Pattern pattern(String query, SheetSearchOptions o) {
        String q = o.regex() ? query : wildcard(query);
        if (o.entireCell()) q = "^(?:" + q + ")$";
        try {
            return Pattern.compile(q, o.matchCase() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        } catch (PatternSyntaxException failure) {
            throw new IllegalArgumentException("Expressão regular inválida: " + failure.getDescription());
        }
    }

    private static String wildcard(String q) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < q.length(); i++) {
            char c = q.charAt(i);
            if (c == '~' && i + 1 < q.length()) { b.append(Pattern.quote(String.valueOf(q.charAt(++i)))); continue; }
            if (c == '*') b.append(".*");
            else if (c == '?') b.append('.');
            else b.append(Pattern.quote(String.valueOf(c)));
        }
        return b.toString();
    }

    private String content(int sheet, CellAddress a, SheetCell cell, boolean formulas) {
        if (formulas && cell.hasFormula()) return "=" + Formulas.toDisplay(cell.formula(), editor.formulaLocale(), a, editor.getConfig().r1c1());
        return editor.displayText(sheet, a);
    }

    @Override
    public List<SheetSearchHit> findAll(String query, SheetSearchOptions options) {
        List<SheetSearchHit> hits = new ArrayList<>();
        if (query == null || query.isEmpty()) return hits;
        Pattern p = pattern(query, options);
        int active = editor.activeSheetIndex();
        List<Integer> sheets = new ArrayList<>();
        if (options.workbook()) for (int i = 0; i < editor.getWorkbook().sheetCount(); i++) sheets.add(i); else sheets.add(active);
        SheetSelection sel = editor.getSelection();
        boolean inSelection = !options.workbook() && !sel.isSingleCell();
        for (int s : sheets) {
            SheetWorksheet ws = editor.getWorkbook().sheet(s);
            CellRange used = ws.usedRange();
            if (used == null) continue;
            List<SheetSearchHit> local = new ArrayList<>();
            ws.cells().forEach(used, (row, col, cell) -> {
                if (local.size() >= LIMIT || !cell.hasContent()) return;
                if (inSelection && !sel.contains(row, col)) return;
                CellAddress a = new CellAddress(row, col);
                String text = content(s, a, cell, options.formulas());
                if (p.matcher(text).find()) local.add(new SheetSearchHit(s, ws.name(), a, text));
            });
            local.sort(options.byColumns() ? Comparator.comparing((SheetSearchHit h) -> h.cell().column()).thenComparing(h -> h.cell().row())
                    : Comparator.comparing((SheetSearchHit h) -> h.cell().row()).thenComparing(h -> h.cell().column()));
            hits.addAll(local);
        }
        return hits;
    }

    @Override
    public boolean select(SheetSearchHit hit) {
        if (hit.sheet() != editor.activeSheetIndex()) editor.activateSheet(hit.sheet());
        editor.select(SheetSelection.of(hit.cell()));
        return true;
    }

    @Override
    public int replace(SheetSearchHit hit, String replacement, SheetSearchOptions options) {
        return replaceIn(List.of(hit), hit.text(), replacement, options, true);
    }

    @Override
    public int replaceAll(String query, String replacement, SheetSearchOptions options) {
        List<SheetSearchHit> hits = findAll(query, options);
        return replaceIn(hits, query, replacement, options, false);
    }

    private int replaceIn(List<SheetSearchHit> hits, String query, String replacement, SheetSearchOptions options, boolean single) {
        if (hits.isEmpty()) return 0;
        Pattern p = single ? null : pattern(query, options);
        int[] count = {0};
        UnaryOperator<String> change = text -> {
            if (p == null) return replacement;
            Matcher m = p.matcher(text);
            return options.regex() ? m.replaceAll(replacement) : m.replaceAll(Matcher.quoteReplacement(replacement));
        };
        editor.edit("Substituir", tx -> {
            for (SheetSearchHit h : hits) {
                String text = h.text();
                String next = change.apply(text);
                if (next.equals(text)) continue;
                if (!editor.review().canEdit(h.sheet(), h.cell())) continue;
                editor.editing().writeInto(tx, h.sheet(), h.cell(), next);
                count[0]++;
            }
        });
        return count[0];
    }

    static String lower(String s) { return s.toLowerCase(Locale.ROOT); }
}
