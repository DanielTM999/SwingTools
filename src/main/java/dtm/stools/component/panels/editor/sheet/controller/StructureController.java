package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.command.SheetOperations;
import dtm.stools.component.panels.editor.sheet.data.DataTools;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.FreezePane;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.SheetVisibility;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.store.AxisIndex;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class StructureController {
    private final SheetEditor editor;
    private final BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

    public StructureController(SheetEditor editor) { this.editor = editor; }

    private int sheet() { return editor.activeSheetIndex(); }
    private SheetWorksheet ws() { return editor.activeSheet(); }

    private List<int[]> rowSpans() {
        List<int[]> spans = new ArrayList<>();
        for (CellRange r : editor.getSelection().ranges()) spans.add(new int[]{r.firstRow(), r.rowCount()});
        spans.sort((a, b) -> Integer.compare(b[0], a[0]));
        return spans;
    }

    private List<int[]> columnSpans() {
        List<int[]> spans = new ArrayList<>();
        for (CellRange r : editor.getSelection().ranges()) spans.add(new int[]{r.firstColumn(), r.columnCount()});
        spans.sort((a, b) -> Integer.compare(b[0], a[0]));
        return spans;
    }

    private boolean allowed(boolean rows, boolean insert) {
        var p = ws().properties().protection();
        if (!p.enabled()) return true;
        boolean ok = rows ? (insert ? p.insertRows() : p.deleteRows()) : (insert ? p.insertColumns() : p.deleteColumns());
        if (!ok) editor.review().warnProtected();
        return ok;
    }

    public void insertRows() {
        if (!allowed(true, true)) return;
        List<int[]> spans = rowSpans();
        int s = sheet();
        editor.edit("Inserir linhas", tx -> { for (int[] sp : spans) SheetOperations.insertRows(tx, s, sp[0], sp[1]); });
    }

    public void deleteRows() {
        if (!allowed(true, false)) return;
        List<int[]> spans = rowSpans();
        int s = sheet();
        editor.edit("Excluir linhas", tx -> { for (int[] sp : spans) SheetOperations.deleteRows(tx, s, sp[0], sp[1]); });
    }

    public void insertColumns() {
        if (!allowed(false, true)) return;
        List<int[]> spans = columnSpans();
        int s = sheet();
        editor.edit("Inserir colunas", tx -> { for (int[] sp : spans) SheetOperations.insertColumns(tx, s, sp[0], sp[1]); });
    }

    public void deleteColumns() {
        if (!allowed(false, false)) return;
        List<int[]> spans = columnSpans();
        int s = sheet();
        editor.edit("Excluir colunas", tx -> { for (int[] sp : spans) SheetOperations.deleteColumns(tx, s, sp[0], sp[1]); });
    }

    public void insertCellsDialog() {
        CellRange r = editor.getSelection().range();
        if (r.isWholeRow()) { insertRows(); return; }
        if (r.isWholeColumn()) { insertColumns(); return; }
        JComboBox<String> mode = new JComboBox<>(new String[]{"Deslocar células para a direita", "Deslocar células para baixo", "Linha inteira", "Coluna inteira"});
        mode.setSelectedIndex(1);
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JLabel("Inserir:"), BorderLayout.NORTH);
        p.add(mode, BorderLayout.CENTER);
        editor.popups().dialog("sheet.insertCells", "Inserir", p, mode::getSelectedIndex).ifPresent(i -> {
            switch (i) {
                case 0 -> insertCells(false);
                case 1 -> insertCells(true);
                case 2 -> insertRows();
                default -> insertColumns();
            }
        });
    }

    public void deleteCellsDialog() {
        CellRange r = editor.getSelection().range();
        if (r.isWholeRow()) { deleteRows(); return; }
        if (r.isWholeColumn()) { deleteColumns(); return; }
        JComboBox<String> mode = new JComboBox<>(new String[]{"Deslocar células para a esquerda", "Deslocar células para cima", "Linha inteira", "Coluna inteira"});
        mode.setSelectedIndex(1);
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JLabel("Excluir:"), BorderLayout.NORTH);
        p.add(mode, BorderLayout.CENTER);
        editor.popups().dialog("sheet.deleteCells", "Excluir", p, mode::getSelectedIndex).ifPresent(i -> {
            switch (i) {
                case 0 -> deleteCells(false);
                case 1 -> deleteCells(true);
                case 2 -> deleteRows();
                default -> deleteColumns();
            }
        });
    }

    public void insertCells(boolean shiftDown) {
        CellRange r = editor.getSelection().range();
        int s = sheet();
        editor.edit("Inserir células", tx -> SheetOperations.insertCells(tx, s, r, shiftDown));
    }

    public void deleteCells(boolean shiftUp) {
        CellRange r = editor.getSelection().range();
        int s = sheet();
        editor.edit("Excluir células", tx -> SheetOperations.deleteCells(tx, s, r, shiftUp));
    }

    public void clear(SheetOperations.ClearMode mode) {
        int s = sheet();
        List<CellRange> ranges = new ArrayList<>();
        for (CellRange r : editor.getSelection().ranges()) ranges.add(editor.clipboard().bounded(ws(), r));
        for (CellRange r : ranges) if (!editor.review().canEdit(s, r)) { editor.review().warnProtected(); return; }
        String label = switch (mode) { case ALL -> "Limpar tudo"; case CONTENTS -> "Limpar conteúdo"; case FORMATS -> "Limpar formatos"; case NOTES -> "Limpar anotações"; case LINKS -> "Limpar hiperlinks"; };
        editor.edit(label, tx -> { for (CellRange r : ranges) SheetOperations.clear(tx, s, r, mode); });
    }

    public void setHidden(boolean rows, boolean hidden) {
        int s = sheet();
        List<int[]> spans = rows ? rowSpans() : columnSpans();
        CellRange sel = editor.getSelection().range();
        editor.edit(hidden ? (rows ? "Ocultar linhas" : "Ocultar colunas") : (rows ? "Reexibir linhas" : "Reexibir colunas"), tx -> tx.updateAxis(s, rows, axis -> {
            for (int[] sp : spans) {
                int from = sp[0], to = sp[0] + sp[1] - 1;
                if (!hidden && from == to) { from = Math.max(0, from - 1); to = to + 1; }
                if (!hidden && (rows ? sel.firstRow() == 0 : sel.firstColumn() == 0)) from = 0;
                for (int i = from; i <= Math.min(to, axis.count() - 1); i++) axis.setHidden(i, hidden);
            }
        }));
    }

    public void promptSize(boolean rows) {
        AxisIndex axis = rows ? ws().rows() : ws().columns();
        CellAddress a = editor.getSelection().active();
        int current = axis.rawSize(rows ? a.row() : a.column());
        double shown = rows ? current * 0.75 : current / 7.0;
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.round(shown * 100) / 100.0, 0, rows ? 409 : 255, rows ? 0.75 : 0.5));
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(new JLabel(rows ? "Altura da linha (pontos):" : "Largura da coluna (caracteres):"), BorderLayout.WEST);
        p.add(spinner, BorderLayout.CENTER);
        editor.popups().dialog(rows ? "sheet.rowHeight" : "sheet.columnWidth", rows ? "Altura da Linha" : "Largura da Coluna", p, () -> ((Number) spinner.getValue()).doubleValue()).ifPresent(v -> {
            int px = rows ? (int) Math.round(v / 0.75) : (int) Math.round(v * 7);
            setSize(rows, rows ? rowSpans() : columnSpans(), px);
        });
    }

    public void setSize(boolean rows, List<int[]> spans, int px) {
        int s = sheet();
        editor.edit(rows ? "Altura da linha" : "Largura da coluna", tx -> tx.updateAxis(s, rows, axis -> {
            for (int[] sp : spans) for (int i = sp[0]; i < sp[0] + sp[1] && i < axis.count(); i++) {
                if (px <= 0) axis.setHidden(i, true); else { axis.setHidden(i, false); axis.setSize(i, px); }
            }
        }));
    }

    public void defaultWidth() {
        int current = ws().columns().defaultSize();
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(current / 7.0, 0.5, 255, 0.5));
        JPanel p = new JPanel(new BorderLayout(6, 0));
        p.add(new JLabel("Largura padrão da coluna:"), BorderLayout.WEST);
        p.add(spinner, BorderLayout.CENTER);
        int s = sheet();
        editor.popups().dialog("sheet.defaultWidth", "Largura Padrão", p, () -> ((Number) spinner.getValue()).doubleValue())
                .ifPresent(v -> editor.edit("Largura padrão", tx -> tx.updateAxis(s, false, axis -> axis.setDefaultSize((int) Math.round(v * 7)))));
    }

    public void previewColumnWidth(int index, int size) { ws().columns().setSize(index, Math.max(0, size)); }
    public void previewRowHeight(int index, int size) { ws().rows().setSize(index, Math.max(0, size)); }

    public void commitColumnWidth(List<Integer> columns, int size, int original, int index) { commitSize(false, columns, size, original, index); }
    public void commitRowHeight(List<Integer> rows, int size, int original, int index) { commitSize(true, rows, size, original, index); }

    private void commitSize(boolean rows, List<Integer> indexes, int size, int original, int index) {
        AxisIndex axis = rows ? ws().rows() : ws().columns();
        axis.setSize(index, original);
        if (editor.isReadOnlyView()) { editor.getCanvas().invalidateGeometry(); editor.getCanvas().repaint(); return; }
        int s = sheet();
        editor.edit(rows ? "Altura da linha" : "Largura da coluna", tx -> tx.updateAxis(s, rows, a -> {
            for (int i : indexes) { if (size <= 1) a.setHidden(i, true); else a.setSize(i, size); }
        }));
        editor.getCanvas().invalidateGeometry();
        editor.getCanvas().repaint();
    }

    private Graphics2D graphics() { return scratch.createGraphics(); }

    public int measureColumn(int column, int fromRow, int toRow) {
        SheetWorksheet w = ws();
        SheetWorkbook wb = editor.getWorkbook();
        int s = sheet();
        int[] max = {0};
        Graphics2D g = graphics();
        try {
            CellRange range = new CellRange(fromRow, column, toRow, column);
            int[] seen = {0};
            w.cells().forEach(range, (row, col, cell) -> {
                if (seen[0]++ > 20_000 || w.rows().isHidden(row) || w.properties().mergeAt(row, col) != null) return;
                CellStyle style = wb.style(cell.style());
                String text = editor.displayText(s, new CellAddress(row, col));
                if (text.isEmpty()) return;
                int width = style.wrap() ? 0 : editor.renderer().measureWidth(g, style, text, 1) + style.indent() * 9;
                max[0] = Math.max(max[0], width);
            });
        } finally {
            g.dispose();
        }
        return max[0] == 0 ? 0 : max[0] + 10;
    }

    public int measureRow(int row) {
        SheetWorksheet w = ws();
        SheetWorkbook wb = editor.getWorkbook();
        int s = sheet();
        int[] max = {0};
        Graphics2D g = graphics();
        try {
            CellRange used = w.usedRange();
            if (used == null) return SheetWorksheet.DEFAULT_ROW_HEIGHT;
            w.cells().forEach(new CellRange(row, 0, row, used.lastColumn()), (r, col, cell) -> {
                if (w.properties().mergeAt(r, col) != null) return;
                CellStyle style = wb.style(cell.style());
                String text = editor.displayText(s, new CellAddress(r, col));
                int width = style.wrap() ? w.columns().size(col) - 6 : Integer.MAX_VALUE / 4;
                int h = editor.renderer().measureHeight(g, style, text.isEmpty() ? "X" : text, width, 1) + 6;
                max[0] = Math.max(max[0], h);
            });
        } finally {
            g.dispose();
        }
        return Math.max(SheetWorksheet.DEFAULT_ROW_HEIGHT, max[0]);
    }

    public void autoFitColumns(List<Integer> columns) {
        if (editor.isReadOnlyView()) return;
        CellRange used = ws().usedRange();
        if (used == null) return;
        SheetSelection sel = editor.getSelection();
        boolean limited = sel.ranges().size() == 1 && !sel.range().isWholeColumn();
        int from = limited ? sel.range().firstRow() : 0, to = limited ? sel.range().lastRow() : used.lastRow();
        List<int[]> sizes = new ArrayList<>();
        for (int c : columns) { int w = measureColumn(c, from, to); if (w > 0) sizes.add(new int[]{c, w}); }
        if (sizes.isEmpty()) return;
        int s = sheet();
        editor.edit("AutoAjuste da largura", tx -> tx.updateAxis(s, false, axis -> { for (int[] x : sizes) { axis.setHidden(x[0], false); axis.setSize(x[0], x[1]); } }));
    }

    public void autoFitRows(List<Integer> rows) {
        if (editor.isReadOnlyView()) return;
        List<int[]> sizes = new ArrayList<>();
        for (int r : rows) sizes.add(new int[]{r, measureRow(r)});
        int s = sheet();
        editor.edit("AutoAjuste da altura", tx -> tx.updateAxis(s, true, axis -> { for (int[] x : sizes) { axis.setHidden(x[0], false); axis.setSize(x[0], x[1]); } }));
    }

    public void autoFitSelectionColumns() {
        List<Integer> cols = new ArrayList<>();
        CellRange r = editor.clipboard().bounded(ws(), editor.getSelection().range());
        for (int c = r.firstColumn(); c <= r.lastColumn() && cols.size() < 2000; c++) cols.add(c);
        autoFitColumns(cols);
    }

    public void autoFitSelectionRows() {
        List<Integer> rows = new ArrayList<>();
        CellRange r = editor.clipboard().bounded(ws(), editor.getSelection().range());
        for (int i = r.firstRow(); i <= r.lastRow() && rows.size() < 20_000; i++) rows.add(i);
        autoFitRows(rows);
    }

    public void merge(boolean center, boolean across) {
        int s = sheet();
        CellRange r = editor.getSelection().range();
        if (r.isSingleCell()) return;
        if (!editor.review().canFormat(s, r)) { editor.review().warnProtected(); return; }
        long filled = 0;
        for (CellAddress a : ws().cells().addresses(r)) if (ws().cell(a).hasContent()) filled++;
        if (filled > 1 && !across && !editor.popups().confirm("Mesclar", "Mesclar células mantém apenas o valor superior esquerdo e descarta os demais. Continuar?")) return;
        editor.edit("Mesclar células", tx -> {
            SheetOperations.merge(tx, s, r, across);
            if (center) tx.setStyle(s, r.firstRow(), r.firstColumn(), st -> st.withHorizontal(HorizontalAlignment.CENTER));
        });
    }

    public void unmerge() {
        int s = sheet();
        CellRange r = editor.getSelection().range();
        editor.edit("Desfazer mesclagem", tx -> SheetOperations.unmerge(tx, s, r));
    }

    public void toggleMergeCenter() {
        CellRange r = editor.getSelection().range();
        if (ws().properties().merges().stream().anyMatch(m -> m.intersects(r))) unmerge(); else merge(true, false);
    }

    public void freeze(int rows, int columns) {
        SheetWorksheet w = ws();
        w.setProperties(w.properties().withFreeze(new FreezePane(rows, columns)));
        editor.getCanvas().resetScroll();
        editor.getCanvas().invalidateGeometry();
        editor.getSession().markDirty();
        editor.refreshAll();
    }

    public void freezeAtSelection() {
        FreezePane f = ws().properties().freeze();
        if (f.rows() > 0 || f.columns() > 0) { freeze(0, 0); return; }
        SheetSelection sel = editor.getSelection();
        CellAddress a = sel.active();
        freeze(a.row(), a.column());
    }

    public void addSheet() {
        int index = sheet() + 1;
        editor.edit("Inserir planilha", tx -> {
            int i = SheetOperations.addSheet(tx, index, null);
            tx.select(i, SheetSelection.home());
        });
    }

    public void deleteSheet() { deleteSheet(sheet()); }

    public void deleteSheet(int index) {
        SheetWorkbook wb = editor.getWorkbook();
        long visible = wb.sheets().stream().filter(x -> x.properties().visibility() == SheetVisibility.VISIBLE).count();
        if (visible <= 1) { editor.popups().warn("Excluir planilha", "Uma pasta de trabalho deve conter pelo menos uma planilha visível."); return; }
        if (wb.properties().protectStructure()) { editor.review().warnProtected(); return; }
        if (wb.sheet(index).usedRange() != null && !editor.popups().confirm("Excluir planilha", "A planilha \"" + wb.sheet(index).name() + "\" será excluída permanentemente. Continuar?") && editor.isShowing()) return;
        editor.edit("Excluir planilha", tx -> SheetOperations.deleteSheet(tx, index));
    }

    public void renameSheet(int index, String name) {
        if (name == null || name.isBlank()) return;
        try {
            SheetWorkbook.validateSheetName(name.strip());
        } catch (IllegalArgumentException failure) {
            editor.popups().warn("Renomear", failure.getMessage());
            return;
        }
        if (editor.getWorkbook().properties().protectStructure()) { editor.review().warnProtected(); return; }
        editor.edit("Renomear planilha", tx -> SheetOperations.renameSheet(tx, index, name.strip()));
    }

    public void promptRename() {
        int i = sheet();
        editor.getTabBar().startRename(i);
    }

    public void duplicateSheet() {
        int i = sheet();
        editor.edit("Duplicar planilha", tx -> { int n = SheetOperations.duplicateSheet(tx, i); tx.select(n, SheetSelection.home()); });
    }

    public void moveSheet(int from, int to) {
        if (from == to || editor.getWorkbook().properties().protectStructure()) return;
        editor.edit("Mover planilha", tx -> { SheetOperations.moveSheet(tx, from, to); tx.select(to, tx.selection()); });
    }

    public void tabColor(Integer argb) {
        int s = sheet();
        editor.edit("Cor da guia", tx -> tx.updateProperties(s, p -> p.withTabColor(argb)));
    }

    public void hideSheet() {
        int s = sheet();
        long visible = editor.getWorkbook().sheets().stream().filter(x -> x.properties().visibility() == SheetVisibility.VISIBLE).count();
        if (visible <= 1) { editor.popups().warn("Ocultar", "Uma pasta de trabalho deve conter pelo menos uma planilha visível."); return; }
        int next = -1;
        for (int i = 0; i < editor.getWorkbook().sheetCount(); i++) if (i != s && editor.getWorkbook().sheet(i).properties().visibility() == SheetVisibility.VISIBLE) { next = i; break; }
        int target = next;
        editor.edit("Ocultar planilha", tx -> { tx.updateProperties(s, p -> p.withVisibility(SheetVisibility.HIDDEN)); tx.select(target, SheetSelection.home()); });
    }

    public void unhideSheet() {
        List<String> hidden = new ArrayList<>();
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < editor.getWorkbook().sheetCount(); i++) if (editor.getWorkbook().sheet(i).properties().visibility() == SheetVisibility.HIDDEN) { hidden.add(editor.getWorkbook().sheet(i).name()); idx.add(i); }
        if (hidden.isEmpty()) { editor.popups().info("Reexibir", "Não há planilhas ocultas."); return; }
        JComboBox<String> box = new JComboBox<>(hidden.toArray(String[]::new));
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JLabel("Reexibir planilha:"), BorderLayout.NORTH);
        p.add(box, BorderLayout.CENTER);
        Optional<Integer> choice = editor.popups().dialog("sheet.unhide", "Reexibir", p, box::getSelectedIndex);
        choice.ifPresent(k -> {
            int s = idx.get(k);
            editor.edit("Reexibir planilha", tx -> { tx.updateProperties(s, q -> q.withVisibility(SheetVisibility.VISIBLE)); tx.select(s, SheetSelection.home()); });
        });
    }

    public void group(boolean rows, int delta) {
        int s = sheet();
        CellRange r = editor.getSelection().range();
        int from = rows ? r.firstRow() : r.firstColumn(), to = rows ? r.lastRow() : r.lastColumn();
        if (!rows && !r.isWholeColumn() && !r.isWholeRow()) rows = r.rowCount() >= r.columnCount() || r.isWholeRow();
        boolean byRows = r.isWholeColumn() && !r.isWholeRow() ? false : r.isWholeRow() || rows;
        int f = byRows ? r.firstRow() : r.firstColumn(), t = byRows ? r.lastRow() : r.lastColumn();
        editor.edit(delta > 0 ? "Agrupar" : "Desagrupar", tx -> DataTools.group(tx, s, f, t, byRows, delta));
    }

    public void setCollapsed(boolean collapsed) {
        int s = sheet();
        CellRange r = editor.getSelection().range();
        boolean rows = !(r.isWholeColumn() && !r.isWholeRow());
        int f = rows ? r.firstRow() : r.firstColumn(), t = rows ? r.lastRow() : r.lastColumn();
        editor.edit(collapsed ? "Ocultar detalhe" : "Mostrar detalhe", tx -> DataTools.setCollapsed(tx, s, f, t, rows, collapsed));
    }
}
