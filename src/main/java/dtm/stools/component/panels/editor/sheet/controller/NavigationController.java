package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.data.FilterEngine;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.DefinedName;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.store.AxisIndex;
import dtm.stools.component.panels.editor.sheet.ui.SheetGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;

public final class NavigationController {
    public record Target(int sheet, List<CellRange> ranges) {}

    private static final Pattern NAME = Pattern.compile("[\\p{L}_\\\\][\\p{L}\\p{N}_.]*");
    private final SheetEditor editor;

    public NavigationController(SheetEditor editor) { this.editor = editor; }

    private SheetWorksheet ws() { return editor.activeSheet(); }
    private int maxRow() { return Math.min(editor.getConfig().limits().maxRows(), ws().rows().count()) - 1; }
    private int maxColumn() { return Math.min(editor.getConfig().limits().maxColumns(), ws().columns().count()) - 1; }

    public void select(SheetSelection selection) {
        editor.getSession().setSelection(selection);
    }

    private CellRange merge(CellAddress a) {
        CellRange m = ws().properties().mergeAt(a.row(), a.column());
        return m == null ? CellRange.of(a) : m;
    }

    private static int step(AxisIndex axis, int index, int dir, int max) {
        int i = index + dir;
        while (i >= 0 && i <= max && axis.isHidden(i)) i += dir;
        return i < 0 || i > max ? index : i;
    }

    private CellAddress stepCell(CellAddress from, int dr, int dc) {
        CellRange m = merge(from);
        int row = from.row(), col = from.column();
        if (dr > 0) row = m.lastRow(); else if (dr < 0) row = m.firstRow();
        if (dc > 0) col = m.lastColumn(); else if (dc < 0) col = m.firstColumn();
        if (dr != 0) row = step(ws().rows(), row, Integer.signum(dr), maxRow());
        if (dc != 0) col = step(ws().columns(), col, Integer.signum(dc), maxColumn());
        if (dr == 0) row = from.row();
        if (dc == 0) col = from.column();
        return new CellAddress(row, col);
    }

    private CellAddress focusOf(SheetSelection sel) {
        CellRange r = sel.range();
        CellAddress a = sel.anchor();
        return new CellAddress(a.row() <= r.firstRow() ? r.lastRow() : r.firstRow(), a.column() <= r.firstColumn() ? r.lastColumn() : r.firstColumn());
    }

    private void apply(CellAddress target, boolean extend) {
        SheetSelection sel = editor.getSelection();
        if (extend) {
            CellRange r = expand(CellRange.of(sel.anchor(), target));
            List<CellRange> ranges = new ArrayList<>(sel.ranges());
            ranges.set(ranges.size() - 1, r);
            select(new SheetSelection(sel.active(), sel.anchor(), ranges));
            editor.ensureVisible(target);
        } else {
            CellRange m = merge(target);
            select(new SheetSelection(m.first(), m.first(), List.of(m)));
            editor.ensureVisible(m.first());
        }
    }

    private CellRange expand(CellRange r) {
        CellRange out = r;
        boolean changed = true;
        List<CellRange> merges = ws().properties().merges();
        while (changed) {
            changed = false;
            for (CellRange m : merges) if (m.intersects(out) && !out.contains(m)) { out = out.union(m); changed = true; }
        }
        return out;
    }

    public void move(int dr, int dc, boolean extend) {
        SheetSelection sel = editor.getSelection();
        CellAddress from = extend ? focusOf(sel) : sel.active();
        apply(stepCell(from, dr, dc), extend);
    }

    private boolean has(int row, int col) { return !editor.getEngine().valueAt(editor.activeSheetIndex(), row, col).isEmpty(); }

    public void jump(int dr, int dc, boolean extend) {
        SheetSelection sel = editor.getSelection();
        CellAddress from = extend ? focusOf(sel) : sel.active();
        CellRange used = editor.getEngine().usedRange(editor.activeSheetIndex());
        int row = from.row(), col = from.column();
        if (dr != 0) row = jumpAxis(row, Integer.signum(dr), ws().rows(), maxRow(), used == null ? 0 : used.lastRow(), i -> has(i, from.column()));
        else col = jumpAxis(col, Integer.signum(dc), ws().columns(), maxColumn(), used == null ? 0 : used.lastColumn(), i -> has(from.row(), i));
        apply(new CellAddress(row, col), extend);
    }

    private static int jumpAxis(int start, int dir, AxisIndex axis, int max, int lastUsed, IntPredicate has) {
        int next = step(axis, start, dir, max);
        if (next == start) return start;
        if (has.test(start) && has.test(next)) {
            int cur = next;
            while (true) {
                int n = step(axis, cur, dir, max);
                if (n == cur || !has.test(n)) return cur;
                cur = n;
            }
        }
        int i = next;
        while (true) {
            if (has.test(i)) return i;
            if (dir > 0 && i > lastUsed) return max;
            int n = step(axis, i, dir, max);
            if (n == i) return i;
            i = n;
        }
    }

    public void home(boolean extend) {
        SheetSelection sel = editor.getSelection();
        CellAddress from = extend ? focusOf(sel) : sel.active();
        int first = ws().properties().freeze().columns();
        apply(new CellAddress(from.row(), from.column() > first ? first : 0), extend);
    }

    public void documentStart(boolean extend) {
        SheetProperties p = ws().properties();
        CellAddress a = new CellAddress(p.freeze().rows(), p.freeze().columns());
        SheetSelection sel = editor.getSelection();
        if (sel.active().equals(a)) a = new CellAddress(0, 0);
        apply(a, extend);
    }

    public void documentEnd(boolean extend) {
        CellRange used = editor.getEngine().usedRange(editor.activeSheetIndex());
        apply(used == null ? new CellAddress(0, 0) : used.last(), extend);
    }

    public void page(int direction, boolean horizontal, boolean extend) {
        SheetGeometry g = editor.getCanvas().geometry();
        SheetSelection sel = editor.getSelection();
        CellAddress from = extend ? focusOf(sel) : sel.active();
        if (horizontal) {
            int visible = Math.max(1, g.lastVisibleColumn() - g.firstScrollColumn());
            int col = Math.max(0, Math.min(maxColumn(), from.column() + direction * visible));
            editor.getCanvas().pageScroll(direction, true);
            apply(new CellAddress(from.row(), col), extend);
        } else {
            int visible = Math.max(1, g.lastVisibleRow() - g.firstScrollRow());
            int row = Math.max(0, Math.min(maxRow(), from.row() + direction * visible));
            editor.getCanvas().pageScroll(direction, false);
            apply(new CellAddress(row, from.column()), extend);
        }
    }

    public void moveWithinSelection(int dr, int dc) {
        if (dr == 0 && dc == 0) return;
        SheetSelection sel = editor.getSelection();
        if (sel.isSingleCell() || sel.ranges().size() == 1 && merge(sel.active()).equals(sel.range())) { move(dr, dc, false); return; }
        CellRange r = sel.range();
        CellAddress a = sel.active();
        int row = a.row(), col = a.column();
        int guard = 0;
        do {
            if (dc != 0) {
                col += dc;
                if (col > r.lastColumn()) { col = r.firstColumn(); row = row + 1 > r.lastRow() ? r.firstRow() : row + 1; }
                else if (col < r.firstColumn()) { col = r.lastColumn(); row = row - 1 < r.firstRow() ? r.lastRow() : row - 1; }
            } else {
                row += dr;
                if (row > r.lastRow()) { row = r.firstRow(); col = col + 1 > r.lastColumn() ? r.firstColumn() : col + 1; }
                else if (row < r.firstRow()) { row = r.lastRow(); col = col - 1 < r.firstColumn() ? r.lastColumn() : col - 1; }
            }
        } while (++guard < 1_000_000 && (ws().rows().isHidden(row) || ws().columns().isHidden(col)));
        CellAddress next = new CellAddress(row, col);
        select(sel.withActive(merge(next).first()));
        editor.ensureVisible(next);
    }

    public void selectAll() {
        SheetSelection sel = editor.getSelection();
        CellRange region = FilterEngine.detectRegion(ws(), sel.active().row(), sel.active().column());
        CellRange all = new CellRange(0, 0, maxRow(), maxColumn());
        if (region != null && !region.isSingleCell() && !sel.range().equals(region) && !sel.range().equals(all)) select(new SheetSelection(sel.active(), region.first(), List.of(region)));
        else select(new SheetSelection(sel.active(), new CellAddress(0, 0), List.of(all)));
    }

    public void selectColumns() {
        SheetSelection sel = editor.getSelection();
        CellRange r = sel.range();
        select(new SheetSelection(sel.active(), new CellAddress(0, r.firstColumn()), List.of(new CellRange(0, r.firstColumn(), maxRow(), r.lastColumn()))));
    }

    public void selectRows() {
        SheetSelection sel = editor.getSelection();
        CellRange r = sel.range();
        select(new SheetSelection(sel.active(), new CellAddress(r.firstRow(), 0), List.of(new CellRange(r.firstRow(), 0, r.lastRow(), maxColumn()))));
    }

    public void nextSheet(int delta) {
        int n = editor.getWorkbook().sheetCount();
        int i = editor.activeSheetIndex();
        for (int k = 0; k < n; k++) {
            i = Math.floorMod(i + delta, n);
            if (editor.getWorkbook().sheet(i).properties().visibility() == dtm.stools.component.panels.editor.sheet.model.SheetVisibility.VISIBLE) { editor.activateSheet(i); return; }
        }
    }

    public Optional<Target> resolve(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        String t = text.strip();
        if (t.startsWith("=")) t = t.substring(1);
        List<CellRange> ranges = new ArrayList<>();
        int sheet = -1;
        for (String part : splitTop(t)) {
            Optional<Target> one = resolveOne(part.strip());
            if (one.isEmpty()) return Optional.empty();
            if (sheet >= 0 && one.get().sheet() != sheet) return Optional.empty();
            sheet = one.get().sheet();
            ranges.addAll(one.get().ranges());
        }
        return ranges.isEmpty() ? Optional.empty() : Optional.of(new Target(sheet, ranges));
    }

    private static List<String> splitTop(String t) {
        List<String> parts = new ArrayList<>();
        boolean quoted = false;
        int start = 0;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '\'') quoted = !quoted;
            else if (!quoted && (c == ';' || c == ',')) { parts.add(t.substring(start, i)); start = i + 1; }
        }
        parts.add(t.substring(start));
        return parts;
    }

    private Optional<Target> resolveOne(String t) {
        int active = editor.activeSheetIndex();
        int bang = t.lastIndexOf('!');
        if (bang > 0) {
            String sheetName = t.substring(0, bang);
            if (sheetName.startsWith("'") && sheetName.endsWith("'") && sheetName.length() > 1) sheetName = sheetName.substring(1, sheetName.length() - 1).replace("''", "'");
            int s = editor.getWorkbook().indexOf(sheetName);
            if (s < 0) return Optional.empty();
            return parseRange(t.substring(bang + 1)).map(r -> new Target(s, List.of(r)));
        }
        Optional<CellRange> direct = parseRange(t);
        if (direct.isPresent()) return Optional.of(new Target(active, List.of(direct.get())));
        Optional<DefinedName> name = editor.getWorkbook().name(t, active);
        if (name.isEmpty()) name = editor.getWorkbook().name(t, null);
        if (name.isPresent()) {
            String f = name.get().formula();
            if (!f.equalsIgnoreCase(t)) return resolve(f).or(Optional::empty);
        }
        for (int s = 0; s < editor.getWorkbook().sheetCount(); s++)
            for (SheetTable table : editor.getWorkbook().sheet(s).properties().tables())
                if (table.nameMatches(t)) return Optional.of(new Target(s, List.of(table.range())));
        return Optional.empty();
    }

    private static Optional<CellRange> parseRange(String t) {
        try {
            String s = t.strip();
            if (s.isEmpty() || !s.replace("$", "").matches("[A-Za-z]{1,3}\\d+(:[A-Za-z]{1,3}\\d+)?|[A-Za-z]{1,3}:[A-Za-z]{1,3}|\\d+:\\d+")) return Optional.empty();
            return Optional.of(CellRange.parse(s.toUpperCase()));
        } catch (RuntimeException failure) {
            return Optional.empty();
        }
    }

    public boolean goTo(String reference) {
        Optional<Target> target = resolve(reference);
        if (target.isEmpty()) return false;
        Target t = target.get();
        if (t.sheet() != editor.activeSheetIndex()) editor.activateSheet(t.sheet());
        CellRange last = t.ranges().getLast();
        select(new SheetSelection(last.first(), last.first(), t.ranges()));
        editor.ensureVisible(last.first());
        return true;
    }

    public void goToOrDefine(String text) {
        if (goTo(text)) { editor.focusGrid(); return; }
        String name = text.strip();
        if (NAME.matcher(name).matches() && !name.replace("$", "").matches("[A-Za-z]{1,3}\\d+") && !editor.isReadOnlyView()) {
            editor.review().defineName(name, editor.getSelection().range(), null);
            editor.focusGrid();
            return;
        }
        editor.popups().warn("Caixa de Nome", "Referência ou nome inválido: " + text);
        editor.updateNameBox();
    }

    public String nameFor(SheetSelection sel) {
        int active = editor.activeSheetIndex();
        String sheet = editor.activeSheet().name();
        if (sel.ranges().size() == 1 && !sel.isSingleCell()) {
            CellRange r = sel.range();
            for (DefinedName n : editor.getWorkbook().properties().names()) {
                if (n.hidden() || n.sheetScope() != null && n.sheetScope() != active) continue;
                Optional<Target> t = resolveNameFormula(n.formula());
                if (t.isPresent() && t.get().sheet() == active && t.get().ranges().size() == 1 && t.get().ranges().getFirst().equals(r)) return n.name();
            }
            for (SheetTable table : editor.activeSheet().properties().tables()) if (table.range().equals(r)) return table.name();
        }
        return sheet.isEmpty() ? sel.active().toA1() : sel.active().toA1();
    }

    private Optional<Target> resolveNameFormula(String formula) {
        String f = formula.startsWith("=") ? formula.substring(1) : formula;
        int bang = f.lastIndexOf('!');
        if (bang <= 0) return Optional.empty();
        return resolveOne(f);
    }

    public static String reference(String sheetName, CellRange range, boolean absolute) {
        String quoted = sheetName.matches("[\\p{L}_][\\p{L}\\p{N}_.]*") && !sheetName.matches("[A-Za-z]{1,3}\\d+") ? sheetName : "'" + sheetName.replace("'", "''") + "'";
        return quoted + "!" + (absolute ? absolute(range) : range.toA1());
    }

    public static String absolute(CellRange r) {
        if (r.isWholeColumn() && !r.isWholeRow()) return "$" + CellAddress.columnName(r.firstColumn()) + ":$" + CellAddress.columnName(r.lastColumn());
        if (r.isWholeRow() && !r.isWholeColumn()) return "$" + (r.firstRow() + 1) + ":$" + (r.lastRow() + 1);
        return r.isSingleCell() ? r.first().toAbsolute() : r.first().toAbsolute() + ":" + r.last().toAbsolute();
    }
}
