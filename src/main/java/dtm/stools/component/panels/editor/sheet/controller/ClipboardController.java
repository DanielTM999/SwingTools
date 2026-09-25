package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.command.SheetOperations;
import dtm.stools.component.panels.editor.sheet.command.SheetTransaction;
import dtm.stools.component.panels.editor.sheet.formula.ReferenceAdjuster;
import dtm.stools.component.panels.editor.sheet.io.CsvCodec;
import dtm.stools.component.panels.editor.sheet.io.SheetHtmlExporter;
import dtm.stools.component.panels.editor.sheet.io.SheetTextExporter;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetNote;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClipboardController {
    private final SheetEditor editor;
    private CellRange source;
    private int sourceSheet = -1;
    private boolean cut;
    private String payload;
    private SheetObject copiedObject;

    public ClipboardController(SheetEditor editor) { this.editor = editor; }

    public CellRange source() { return source; }
    public int sourceSheet() { return sourceSheet; }
    public boolean isCut() { return cut; }

    public void clearSource() {
        source = null;
        sourceSheet = -1;
        cut = false;
        editor.getCanvas().repaint();
    }

    private Clipboard system() {
        try { return Toolkit.getDefaultToolkit().getSystemClipboard(); } catch (RuntimeException e) { return null; }
    }

    public void copy() { copyOrCut(false); }
    public void cut() { if (!editor.isReadOnlyView()) copyOrCut(true); }

    private void copyOrCut(boolean isCut) {
        SheetObject object = editor.objects().selectedObject();
        if (object != null) { copiedObject = object; if (isCut) editor.objects().deleteSelected(); return; }
        copiedObject = null;
        SheetSelection sel = editor.getSelection();
        if (sel.ranges().size() > 1) { editor.popups().warn("Copiar", "Não é possível executar este comando em várias seleções."); return; }
        int s = editor.activeSheetIndex();
        CellRange r = bounded(editor.activeSheet(), sel.range());
        source = r;
        sourceSheet = s;
        cut = isCut;
        String text = new SheetTextExporter().text(editor.getWorkbook(), editor.getEngine(), editor.formatter(), s, r);
        String html = new SheetHtmlExporter().html(editor.getWorkbook(), editor.getEngine(), editor.formatter(), s, r, true);
        payload = text;
        Clipboard c = system();
        if (c != null) {
            try { c.setContents(new SheetTransferable(text, html), null); } catch (IllegalStateException ignored) { }
        }
        editor.getCanvas().repaint();
    }

    public CellRange bounded(SheetWorksheet ws, CellRange r) {
        if (!r.isWholeColumn() && !r.isWholeRow()) return r;
        CellRange used = ws.usedRange();
        if (used == null) return CellRange.of(r.first());
        CellRange union = used.union(CellRange.of(r.first()));
        CellRange i = r.intersection(union);
        return i == null ? CellRange.of(r.first()) : i;
    }

    private String clipboardText() {
        Clipboard c = system();
        if (c == null) return null;
        try {
            Transferable t = c.getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) return (String) t.getTransferData(DataFlavor.stringFlavor);
        } catch (Exception ignored) { }
        return null;
    }

    private Image clipboardImage() {
        Clipboard c = system();
        if (c == null) return null;
        try {
            Transferable t = c.getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.imageFlavor)) return (Image) t.getTransferData(DataFlavor.imageFlavor);
        } catch (Exception ignored) { }
        return null;
    }

    public boolean hasInternal() {
        if (source == null) return false;
        String text = clipboardText();
        return text == null || text.equals(payload);
    }

    public void paste() { paste(PasteOptions.ALL); }

    public void paste(PasteOptions options) {
        if (editor.isReadOnlyView()) return;
        if (copiedObject != null) { editor.objects().pasteObject(copiedObject); return; }
        if (hasInternal() && sourceSheet < editor.getWorkbook().sheetCount()) { pasteInternal(options); return; }
        String text = clipboardText();
        if (text == null) {
            Image image = clipboardImage();
            if (image != null) editor.objects().insertImage(image);
            return;
        }
        pasteText(text, options.transpose());
    }

    private List<CellAddress> targets(CellRange src, boolean transpose) {
        SheetSelection sel = editor.getSelection();
        CellRange dest = sel.range();
        int h = transpose ? src.columnCount() : src.rowCount(), w = transpose ? src.rowCount() : src.columnCount();
        List<CellAddress> list = new ArrayList<>();
        if (!dest.isWholeColumn() && !dest.isWholeRow() && dest.rowCount() % h == 0 && dest.columnCount() % w == 0 && dest.cellCount() > (long) h * w && dest.cellCount() <= 1_000_000) {
            for (int r = dest.firstRow(); r <= dest.lastRow(); r += h) for (int c = dest.firstColumn(); c <= dest.lastColumn(); c += w) list.add(new CellAddress(r, c));
        } else list.add(dest.first());
        return list;
    }

    private void pasteInternal(PasteOptions o) {
        int to = editor.activeSheetIndex();
        CellRange src = source;
        int from = sourceSheet;
        List<CellAddress> tiles = targets(src, o.transpose());
        CellAddress first = tiles.getFirst();
        int h = o.transpose() ? src.columnCount() : src.rowCount(), w = o.transpose() ? src.rowCount() : src.columnCount();
        if (first.row() + h > editor.activeSheet().rows().count() || first.column() + w > editor.activeSheet().columns().count()) { editor.popups().warn("Colar", "A área de colagem ultrapassa os limites da planilha."); return; }
        for (CellAddress t : tiles) if (!editor.review().canEdit(to, new CellRange(t.row(), t.column(), t.row() + h - 1, t.column() + w - 1))) { editor.review().warnProtected(); return; }
        boolean wasCut = cut;
        boolean ok = editor.edit(wasCut ? "Recortar e colar" : "Colar", tx -> {
            if (wasCut && o.what() == PasteOptions.What.ALL && !o.transpose()) {
                if (from == to) SheetOperations.move(tx, to, src, first);
                else {
                    SheetOperations.copy(tx, from, src, to, first, true, true, true);
                    SheetOperations.clear(tx, from, src, SheetOperations.ClearMode.ALL);
                }
                return;
            }
            for (CellAddress t : tiles) pasteBlock(tx, from, src, to, t, o);
        });
        if (!ok) return;
        CellRange pasted = new CellRange(first.row(), first.column(), tiles.getLast().row() + h - 1, tiles.getLast().column() + w - 1);
        editor.select(new SheetSelection(first, first, List.of(pasted)));
        if (wasCut) clearSource();
    }

    private void pasteBlock(SheetTransaction tx, int from, CellRange src, int to, CellAddress target, PasteOptions o) {
        PasteOptions.What what = o.what();
        if (what == PasteOptions.What.ALL && o.operation() == PasteOptions.Operation.NONE && !o.skipBlanks() && !o.transpose() && !o.link()) {
            SheetOperations.copy(tx, from, src, to, target, true, true, true);
            copyExtras(tx, from, src, to, target, true, true);
            copyMerges(tx, from, src, to, target);
            return;
        }
        if (what == PasteOptions.What.COLUMN_WIDTHS) {
            SheetWorksheet ws = tx.sheet(from);
            List<Integer> widths = new ArrayList<>();
            for (int c = src.firstColumn(); c <= src.lastColumn(); c++) widths.add(ws.columns().rawSize(c));
            tx.updateAxis(to, false, axis -> { for (int k = 0; k < widths.size(); k++) axis.setSize(target.column() + k, widths.get(k)); });
            return;
        }
        SheetWorksheet ws = tx.sheet(from);
        Map<CellAddress, SheetCell> snapshot = new HashMap<>();
        Map<CellAddress, CellValue> values = new HashMap<>();
        for (int r = src.firstRow(); r <= src.lastRow(); r++) for (int c = src.firstColumn(); c <= src.lastColumn(); c++) {
            CellAddress a = new CellAddress(r, c);
            snapshot.put(a, ws.cell(a));
            values.put(a, editor.getEngine().valueAt(from, a));
        }
        String fromName = ws.name();
        boolean crossSheet = from != to;
        for (int r = src.firstRow(); r <= src.lastRow(); r++) for (int c = src.firstColumn(); c <= src.lastColumn(); c++) {
            CellAddress a = new CellAddress(r, c);
            int dr = r - src.firstRow(), dc = c - src.firstColumn();
            int tr = target.row() + (o.transpose() ? dc : dr), tc = target.column() + (o.transpose() ? dr : dc);
            SheetCell s = snapshot.get(a);
            if (o.skipBlanks() && !s.hasContent()) continue;
            SheetCell existing = tx.cell(to, tr, tc);
            SheetCell next = existing;
            if (o.link()) {
                String ref = (crossSheet ? NavigationController.reference(fromName, CellRange.of(a), false) : a.toA1());
                next = existing.withFormula(ref, CellValue.EMPTY);
                tx.setCell(to, tr, tc, next);
                continue;
            }
            boolean pasteContent = what == PasteOptions.What.ALL || what == PasteOptions.What.FORMULAS || what == PasteOptions.What.VALUES || what == PasteOptions.What.ALL_EXCEPT_BORDERS
                    || what == PasteOptions.What.FORMULAS_AND_NUMBER_FORMATS || what == PasteOptions.What.VALUES_AND_NUMBER_FORMATS;
            boolean asValues = what == PasteOptions.What.VALUES || what == PasteOptions.What.VALUES_AND_NUMBER_FORMATS;
            if (pasteContent) {
                if (!asValues && s.hasFormula() && o.operation() == PasteOptions.Operation.NONE) {
                    String f = ReferenceAdjuster.shift(s.formula(), tr - r, tc - c);
                    next = next.withFormula(f, CellValue.EMPTY);
                } else {
                    CellValue v = asValues || s.hasFormula() ? values.get(a) : s.value();
                    if (o.operation() != PasteOptions.Operation.NONE) v = combine(editor.getEngine().valueAt(to, tr, tc), v, o.operation());
                    next = next.withValue(v);
                }
            }
            int style = existing.style();
            switch (what) {
                case ALL -> style = s.style();
                case FORMATS -> style = s.style();
                case ALL_EXCEPT_BORDERS -> {
                    CellStyle st = editor.getWorkbook().style(s.style());
                    CellStyle cur = editor.getWorkbook().style(existing.style());
                    style = editor.getWorkbook().styles().intern(st.withTop(cur.top()).withBottom(cur.bottom()).withLeft(cur.left()).withRight(cur.right()));
                }
                case FORMULAS_AND_NUMBER_FORMATS, VALUES_AND_NUMBER_FORMATS -> {
                    String fmt = editor.getWorkbook().style(s.style()).numberFormat();
                    style = editor.getWorkbook().styles().derive(existing.style(), st -> st.withNumberFormat(fmt));
                }
                default -> { }
            }
            if (what != PasteOptions.What.COMMENTS && what != PasteOptions.What.VALIDATION) tx.setCell(to, tr, tc, next.withStyle(style));
        }
        if (what == PasteOptions.What.COMMENTS || what == PasteOptions.What.ALL) copyExtras(tx, from, src, to, target, true, false);
        if (what == PasteOptions.What.VALIDATION || what == PasteOptions.What.ALL) copyExtras(tx, from, src, to, target, false, true);
    }

    private static CellValue combine(CellValue existing, CellValue incoming, PasteOptions.Operation op) {
        if (!(incoming instanceof NumberValue b)) return existing.isEmpty() ? incoming : existing;
        double a = existing instanceof NumberValue n ? n.value() : existing.isEmpty() ? 0 : Double.NaN;
        if (Double.isNaN(a)) return existing;
        double v = switch (op) {
            case ADD -> a + b.value();
            case SUBTRACT -> a - b.value();
            case MULTIPLY -> a * b.value();
            case DIVIDE -> b.value() == 0 ? Double.NaN : a / b.value();
            case NONE -> b.value();
        };
        return Double.isNaN(v) ? CellValue.error(dtm.stools.component.panels.editor.sheet.model.CellError.DIV0) : CellValue.of(v);
    }

    private void copyExtras(SheetTransaction tx, int from, CellRange src, int to, CellAddress target, boolean notes, boolean validations) {
        SheetProperties p = tx.sheet(from).properties();
        int dr = target.row() - src.firstRow(), dc = target.column() - src.firstColumn();
        if (notes) {
            Map<CellAddress, SheetNote> add = new HashMap<>();
            p.notes().forEach((a, n) -> { if (src.contains(a)) add.put(a.offset(dr, dc), n); });
            if (!add.isEmpty()) tx.updateProperties(to, q -> { Map<CellAddress, SheetNote> m = new HashMap<>(q.notes()); m.putAll(add); return q.withNotes(m); });
        }
        if (validations) {
            List<DataValidation> add = new ArrayList<>();
            for (DataValidation v : p.validations()) {
                List<CellRange> parts = new ArrayList<>();
                for (CellRange r : v.ranges()) { CellRange i = r.intersection(src); if (i != null) parts.add(i.offset(dr, dc)); }
                if (!parts.isEmpty()) add.add(v.withRanges(parts));
            }
            if (!add.isEmpty()) tx.updateProperties(to, q -> { List<DataValidation> l = new ArrayList<>(q.validations()); l.addAll(add); return q.withValidations(l); });
        }
    }

    private void copyMerges(SheetTransaction tx, int from, CellRange src, int to, CellAddress target) {
        int dr = target.row() - src.firstRow(), dc = target.column() - src.firstColumn();
        List<CellRange> add = new ArrayList<>();
        for (CellRange m : tx.sheet(from).properties().merges()) if (src.contains(m)) add.add(m.offset(dr, dc));
        if (add.isEmpty()) return;
        tx.updateProperties(to, q -> {
            List<CellRange> merges = new ArrayList<>(q.merges());
            merges.removeIf(m -> add.stream().anyMatch(m::intersects));
            merges.addAll(add);
            return q.withMerges(merges);
        });
    }

    public void pasteText(String text, boolean transpose) {
        if (text.isEmpty()) return;
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.endsWith("\n")) normalized = normalized.substring(0, normalized.length() - 1);
        List<List<String>> rows = new CsvCodec().parse(normalized, '\t', '"');
        if (rows.isEmpty()) return;
        if (transpose) {
            int width = rows.stream().mapToInt(List::size).max().orElse(0);
            List<List<String>> t = new ArrayList<>();
            for (int c = 0; c < width; c++) { List<String> row = new ArrayList<>(); for (List<String> r : rows) row.add(c < r.size() ? r.get(c) : ""); t.add(row); }
            rows = t;
        }
        int s = editor.activeSheetIndex();
        CellAddress target = editor.getSelection().range().first();
        int width = rows.stream().mapToInt(List::size).max().orElse(1);
        CellRange area = new CellRange(target.row(), target.column(), Math.min(editor.activeSheet().rows().count() - 1, target.row() + rows.size() - 1), Math.min(editor.activeSheet().columns().count() - 1, target.column() + width - 1));
        if (!editor.review().canEdit(s, area)) { editor.review().warnProtected(); return; }
        List<List<String>> data = rows;
        boolean ok = editor.edit("Colar", tx -> {
            for (int r = 0; r < data.size(); r++) {
                List<String> row = data.get(r);
                for (int c = 0; c < row.size(); c++) {
                    int tr = target.row() + r, tc = target.column() + c;
                    if (tr > area.lastRow() || tc > area.lastColumn()) continue;
                    editor.editing().writeInto(tx, s, new CellAddress(tr, tc), row.get(c));
                }
            }
        });
        if (ok) editor.select(new SheetSelection(target, target, List.of(area)));
    }

    public void moveRange(CellRange src, CellAddress target, boolean copy) {
        if (editor.isReadOnlyView()) return;
        int s = editor.activeSheetIndex();
        CellRange dest = new CellRange(target.row(), target.column(), Math.min(editor.activeSheet().rows().count() - 1, target.row() + src.rowCount() - 1), Math.min(editor.activeSheet().columns().count() - 1, target.column() + src.columnCount() - 1));
        if (!editor.review().canEdit(s, dest) || !copy && !editor.review().canEdit(s, src)) { editor.review().warnProtected(); return; }
        if (!copy) {
            boolean occupied = false;
            for (CellAddress a : editor.activeSheet().cells().addresses(dest)) if (!src.contains(a) && editor.activeSheet().cell(a).hasContent()) { occupied = true; break; }
            if (occupied && !editor.popups().confirm("Mover", "Já existem dados aqui. Deseja substituí-los?")) return;
        }
        boolean ok = editor.edit(copy ? "Copiar células" : "Mover células", tx -> {
            if (copy) {
                SheetOperations.copy(tx, s, src, s, target, true, true, true);
                copyMerges(tx, s, src, s, target);
            } else SheetOperations.move(tx, s, src, target);
        });
        if (ok) editor.select(new SheetSelection(target, target, List.of(dest)));
    }
}
