package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.WorkbookProperties;
import dtm.stools.component.panels.editor.sheet.store.AxisIndex;
import dtm.stools.component.panels.editor.sheet.store.CellStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public final class SheetTransaction {
    private final SheetWorkbook workbook;
    private final List<SheetEdit> edits = new ArrayList<>();
    private final Map<String, List<CellRange>> touched = new LinkedHashMap<>();
    private final Map<Long, Integer> cellIndex = new HashMap<>();
    private boolean structural;
    private int activeSheet;
    private SheetSelection selection;

    public SheetTransaction(SheetWorkbook workbook, int activeSheet, SheetSelection selection) {
        this.workbook = Objects.requireNonNull(workbook);
        this.activeSheet = activeSheet;
        this.selection = selection;
    }

    public SheetWorkbook workbook() { return workbook; }
    public int activeSheet() { return activeSheet; }
    public SheetSelection selection() { return selection; }
    public void select(int sheet, SheetSelection value) { activeSheet = sheet; selection = Objects.requireNonNull(value); }
    public void select(SheetSelection value) { selection = Objects.requireNonNull(value); }
    public SheetWorksheet sheet(int index) { return workbook.sheet(index); }
    public SheetWorksheet active() { return workbook.sheet(activeSheet); }
    public List<SheetEdit> edits() { return List.copyOf(edits); }
    public boolean isEmpty() { return edits.isEmpty(); }
    public boolean structural() { return structural; }
    public Map<String, List<CellRange>> touched() { return touched; }

    public SheetCell cell(int sheet, int row, int column) { return workbook.sheet(sheet).cell(row, column); }

    public void setCell(int sheet, int row, int column, SheetCell cell) {
        SheetWorksheet s = workbook.sheet(sheet);
        if (cell != null && cell.isBlank()) cell = null;
        SheetCell before = s.cells().get(row, column);
        if (Objects.equals(before, cell)) return;
        s.put(row, column, cell);
        long key = ((long) sheet << 34) ^ ((long) row << 14) ^ column;
        Integer existing = cellIndex.get(key);
        if (existing != null && edits.get(existing) instanceof CellEdit e && e.sheetId().equals(s.id())) {
            edits.set(existing, new CellEdit(s.id(), row, column, e.before(), cell));
        } else {
            cellIndex.put(key, edits.size());
            edits.add(new CellEdit(s.id(), row, column, before, cell));
        }
        touch(s.id(), CellRange.of(row, column));
    }

    public void updateCell(int sheet, int row, int column, UnaryOperator<SheetCell> change) {
        setCell(sheet, row, column, change.apply(cell(sheet, row, column)));
    }

    public void setStyle(int sheet, int row, int column, UnaryOperator<CellStyle> change) {
        SheetCell c = cell(sheet, row, column);
        int style = workbook.styles().derive(c.style(), change);
        if (style != c.style()) setCell(sheet, row, column, c.withStyle(style));
    }

    public void updateProperties(int sheet, UnaryOperator<SheetProperties> change) {
        SheetWorksheet s = workbook.sheet(sheet);
        SheetProperties before = s.properties(), after = Objects.requireNonNull(change.apply(before));
        if (before.equals(after)) return;
        s.setProperties(after);
        PropertiesEdit edit = new PropertiesEdit(s.id(), before, after);
        edits.add(edit);
        if (edit.structural()) structural = true;
        touch(s.id(), null);
    }

    public void updateAxis(int sheet, boolean rows, Consumer<AxisIndex> change) {
        SheetWorksheet s = workbook.sheet(sheet);
        AxisIndex current = rows ? s.rows() : s.columns();
        AxisIndex before = current.copy();
        change.accept(current);
        edits.add(new AxisEdit(s.id(), rows, before, current.copy()));
        touch(s.id(), null);
    }

    public void replaceCells(int sheet, CellStore store) {
        SheetWorksheet s = workbook.sheet(sheet);
        CellStore before = s.cells();
        s.setCells(store);
        edits.add(new StoreEdit(s.id(), before, store));
        cellIndex.clear();
        structural = true;
        touch(s.id(), null);
    }

    public void updateWorkbook(UnaryOperator<WorkbookProperties> change) {
        WorkbookProperties before = workbook.properties(), after = Objects.requireNonNull(change.apply(before));
        if (before.equals(after)) return;
        workbook.setProperties(after);
        WorkbookEdit edit = new WorkbookEdit(before, after);
        edits.add(edit);
        if (edit.structural()) structural = true;
    }

    public void replaceSheets(List<SheetWorksheet> sheets) {
        List<SheetWorksheet> before = workbook.replaceSheets(sheets);
        edits.add(new SheetsEdit(before, sheets));
        cellIndex.clear();
        structural = true;
    }

    public void markStructural() { structural = true; }

    public void touch(String sheetId, CellRange range) {
        List<CellRange> list = touched.computeIfAbsent(sheetId, k -> new ArrayList<>());
        if (range == null) return;
        if (!list.isEmpty()) {
            CellRange last = list.getLast();
            if (last.firstRow() == range.firstRow() && last.lastRow() == range.lastRow() && last.lastColumn() + 1 == range.firstColumn()) { list.set(list.size() - 1, last.union(range)); return; }
            if (last.firstColumn() == range.firstColumn() && last.lastColumn() == range.lastColumn() && last.lastRow() + 1 == range.firstRow()) { list.set(list.size() - 1, last.union(range)); return; }
        }
        list.add(range);
    }

    public void rollback() {
        for (int i = edits.size() - 1; i >= 0; i--) edits.get(i).undo(workbook);
        edits.clear();
    }
}
