package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.command.SheetOperations;
import dtm.stools.component.panels.editor.sheet.command.SheetTransaction;
import dtm.stools.component.panels.editor.sheet.data.AutoFill;
import dtm.stools.component.panels.editor.sheet.data.DataTools;
import dtm.stools.component.panels.editor.sheet.data.FilterEngine;
import dtm.stools.component.panels.editor.sheet.data.FlashFill;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.model.AutoFilter;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ComparisonOperator;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.FilterCondition;
import dtm.stools.component.panels.editor.sheet.model.FilterCriteria;
import dtm.stools.component.panels.editor.sheet.model.FilterOperator;
import dtm.stools.component.panels.editor.sheet.model.FilterView;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.Scenario;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.SortKey;
import dtm.stools.component.panels.editor.sheet.model.TableColumn;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.model.TotalsFunction;
import dtm.stools.component.panels.editor.sheet.model.ValidationType;
import dtm.stools.component.panels.editor.sheet.ui.popup.DataValidationPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.FilterMenuPopup;
import dtm.stools.component.panels.editor.sheet.ui.popup.FilterMenuRequest;
import dtm.stools.component.panels.editor.sheet.ui.popup.SheetForm;
import dtm.stools.component.panels.editor.sheet.ui.popup.SortPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.ValidationListPopup;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class DataController {
    private final SheetEditor editor;
    private final Map<String, Map<Integer, FilterCriteria>> tableFilters = new HashMap<>();

    public DataController(SheetEditor editor) { this.editor = editor; }

    private int sheet() { return editor.activeSheetIndex(); }
    private SheetWorksheet ws() { return editor.activeSheet(); }

    public void workbookLoaded() { tableFilters.clear(); }

    public SheetTable tableAt(int sheet, CellAddress a) {
        for (SheetTable t : editor.getWorkbook().sheet(sheet).properties().tables()) if (t.range().contains(a)) return t;
        return null;
    }

    public boolean isTableColumnFiltered(SheetTable table, int column) {
        Map<Integer, FilterCriteria> m = tableFilters.get(table.name().toUpperCase(Locale.ROOT));
        return m != null && m.containsKey(column);
    }

    private CellRange region() {
        SheetSelection sel = editor.getSelection();
        CellRange r = sel.range();
        if (!r.isSingleCell()) return editor.clipboard().bounded(ws(), r);
        SheetTable t = tableAt(sheet(), sel.active());
        if (t != null) return t.range();
        CellRange region = FilterEngine.detectRegion(ws(), sel.active().row(), sel.active().column());
        return region == null ? r : region;
    }

    private boolean detectHeader(CellRange r) {
        if (r.rowCount() < 2) return false;
        int s = sheet();
        boolean anyText = false;
        for (int c = r.firstColumn(); c <= r.lastColumn(); c++) {
            CellValue top = editor.getEngine().valueAt(s, r.firstRow(), c), next = editor.getEngine().valueAt(s, r.firstRow() + 1, c);
            if (top instanceof TextValue) anyText = true;
            if (top instanceof TextValue && !(next instanceof TextValue) && !next.isEmpty()) return true;
            if (top.isEmpty()) return false;
        }
        if (!anyText) return false;
        return editor.getWorkbook().style(ws().cell(r.first()).style()).bold();
    }

    public Comparator<CellValue> comparator(boolean caseSensitive) { return (a, b) -> Coerce.compare(a, b, caseSensitive); }

    public void sort(boolean descending) {
        CellRange r = region();
        SheetTable t = tableAt(sheet(), editor.getSelection().active());
        boolean header = t != null ? t.headerRow() : detectHeader(r);
        CellRange body = t != null && t.totalsRow() ? new CellRange(r.firstRow(), r.firstColumn(), r.lastRow() - 1, r.lastColumn()) : r;
        sort(body, List.of(SortKey.of(editor.getSelection().active().column(), descending)), header, false);
    }

    public void sort(CellRange r, List<SortKey> keys, boolean header, boolean caseSensitive) {
        int s = sheet();
        if (!editor.review().canEdit(s, r)) { editor.review().warnProtected(); return; }
        editor.edit("Classificar", tx -> SheetOperations.sort(tx, s, r, keys, header, false, (row, col) -> editor.getEngine().valueAt(s, row, col), comparator(caseSensitive), Map.of()));
    }

    public void customSort() {
        CellRange r = region();
        boolean header = detectHeader(r);
        List<Integer> cols = new ArrayList<>();
        for (int c = r.firstColumn(); c <= r.lastColumn(); c++) cols.add(c);
        int s = sheet();
        SortPanel panel = new SortPanel(cols, h -> {
            List<String> names = new ArrayList<>();
            for (int c : cols) {
                String n = h ? editor.displayText(s, new CellAddress(r.firstRow(), c)) : "";
                names.add(n.isBlank() ? "Coluna " + CellAddress.columnName(c) : n);
            }
            return names;
        }, header, editor.getSelection().active().column());
        editor.popups().dialog("sheet.sort", "Classificar", panel, () -> panel).ifPresent(p -> sort(r, p.keys(), p.hasHeader(), p.caseSensitive()));
    }

    public void toggleFilter() {
        int s = sheet();
        SheetTable t = tableAt(s, editor.getSelection().active());
        if (t != null) {
            editor.edit("Filtro", tx -> tx.updateProperties(s, p -> p.replaceTable(t.name(), x -> x.withFilterButton(!x.filterButton()))));
            if (t.filterButton()) clearTableFilters(t);
            return;
        }
        AutoFilter af = ws().properties().autoFilter();
        if (af != null) {
            CellRange range = af.range();
            editor.edit("Remover filtro", tx -> {
                tx.updateProperties(s, p -> p.withAutoFilter(null));
                tx.updateAxis(s, true, axis -> { for (int row = range.firstRow() + 1; row <= range.lastRow(); row++) if (axis.isHidden(row)) axis.setHidden(row, false); });
            });
            return;
        }
        CellRange r = region();
        if (r.isSingleCell() && ws().cell(r.first()).value().isEmpty()) { editor.popups().warn("Filtro", "Selecione uma célula dentro de um intervalo de dados."); return; }
        editor.edit("Filtro", tx -> tx.updateProperties(s, p -> p.withAutoFilter(AutoFilter.of(r))));
    }

    private void applyFilter(int s, AutoFilter filter, String label) {
        Set<Integer> hidden = editor.filterEngine().hiddenRows(s, filter);
        CellRange range = filter.range();
        int last = Math.min(range.lastRow(), Math.max(range.firstRow(), editor.getWorkbook().sheet(s).usedRange() == null ? range.firstRow() : editor.getWorkbook().sheet(s).usedRange().lastRow()));
        editor.edit(label, tx -> {
            if (filter == editor.getWorkbook().sheet(s).properties().autoFilter() || isSheetFilter(s, filter)) tx.updateProperties(s, p -> p.withAutoFilter(filter));
            tx.updateAxis(s, true, axis -> { for (int row = range.firstRow() + 1; row <= last; row++) axis.setHidden(row, hidden.contains(row)); });
        });
    }

    private boolean isSheetFilter(int s, AutoFilter f) {
        AutoFilter cur = editor.getWorkbook().sheet(s).properties().autoFilter();
        return cur != null && cur.range().equals(f.range());
    }

    public void setColumnFilter(SheetTable table, int column, FilterCriteria criteria) {
        int s = sheet();
        if (table == null) {
            AutoFilter af = ws().properties().autoFilter();
            if (af == null) return;
            Map<Integer, FilterCriteria> m = new HashMap<>(af.criteria());
            if (criteria == null) m.remove(column); else m.put(column, criteria);
            applyFilter(s, new AutoFilter(af.range(), m, af.sort()), "Filtrar");
            return;
        }
        String key = table.name().toUpperCase(Locale.ROOT);
        Map<Integer, FilterCriteria> m = new HashMap<>(tableFilters.getOrDefault(key, Map.of()));
        if (criteria == null) m.remove(column); else m.put(column, criteria);
        if (m.isEmpty()) tableFilters.remove(key); else tableFilters.put(key, m);
        CellRange body = table.totalsRow() ? new CellRange(table.range().firstRow(), table.range().firstColumn(), table.range().lastRow() - 1, table.range().lastColumn()) : table.range();
        applyFilter(s, new AutoFilter(body, m, null), "Filtrar tabela");
    }

    private void clearTableFilters(SheetTable t) {
        tableFilters.remove(t.name().toUpperCase(Locale.ROOT));
        CellRange range = t.range();
        int s = sheet();
        editor.edit("Limpar filtro", tx -> tx.updateAxis(s, true, axis -> { for (int row = range.firstRow() + 1; row <= range.lastRow(); row++) axis.setHidden(row, false); }));
    }

    public void clearFilters() {
        SheetTable t = tableAt(sheet(), editor.getSelection().active());
        if (t != null && tableFilters.containsKey(t.name().toUpperCase(Locale.ROOT))) { clearTableFilters(t); return; }
        AutoFilter af = ws().properties().autoFilter();
        if (af != null) applyFilter(sheet(), new AutoFilter(af.range(), Map.of(), af.sort()), "Limpar filtro");
    }

    public void reapplyFilter() {
        AutoFilter af = ws().properties().autoFilter();
        if (af != null) applyFilter(sheet(), af, "Reaplicar");
        SheetTable t = tableAt(sheet(), editor.getSelection().active());
        if (t != null && tableFilters.containsKey(t.name().toUpperCase(Locale.ROOT))) {
            CellRange range = t.range();
            applyFilter(sheet(), new AutoFilter(range, tableFilters.get(t.name().toUpperCase(Locale.ROOT)), null), "Reaplicar");
        }
    }

    public void filterBySelectedValue() {
        CellAddress a = editor.getSelection().active();
        if (ws().properties().autoFilter() == null && tableAt(sheet(), a) == null) toggleFilter();
        SheetTable t = tableAt(sheet(), a);
        String v = editor.displayText(a);
        setColumnFilter(t, a.column(), FilterCriteria.values(Set.of(v), v.isEmpty()));
    }

    public void showFilterMenu(SheetTable table, int column, Rectangle anchor) {
        int s = sheet();
        AutoFilter base = table == null ? ws().properties().autoFilter() : new AutoFilter(table.range(), tableFilters.getOrDefault(table.name().toUpperCase(Locale.ROOT), Map.of()), null);
        if (base == null) return;
        List<String> values = editor.filterEngine().distinctValues(s, new AutoFilter(base.range().resize(Math.min(base.range().rowCount(), 100_000), base.range().columnCount()), Map.of(), null), column);
        boolean numeric = true, dates = true;
        int checked = 0;
        for (int row = base.range().firstRow() + 1; row <= base.range().lastRow() && checked < 200; row++) {
            CellValue v = editor.getEngine().valueAt(s, row, column);
            if (v.isEmpty()) continue;
            checked++;
            if (!(v instanceof NumberValue)) { numeric = false; dates = false; }
            else if (!editor.formatter().isDateFormat(editor.getWorkbook().style(ws().cell(row, column).style()).numberFormat())) dates = false;
        }
        if (checked == 0) { numeric = false; dates = false; }
        String header = editor.displayText(new CellAddress(base.range().firstRow(), column));
        String name = header.isBlank() ? "Coluna " + CellAddress.columnName(column) : header;
        boolean isNumeric = numeric && !dates;
        FilterCriteria current = base.criteria().get(column);
        FilterMenuPopup.show(editor.getCanvas(), anchor, new FilterMenuRequest(name, values, current, isNumeric, dates,
                c -> setColumnFilter(table, column, c),
                () -> sortColumn(base.range(), column, false),
                () -> sortColumn(base.range(), column, true),
                () -> setColumnFilter(table, column, null),
                () -> customFilter(table, column, name, isNumeric),
                () -> topFilter(table, column)));
    }

    private void sortColumn(CellRange range, int column, boolean descending) {
        CellRange used = ws().usedRange();
        CellRange r = used == null ? range : new CellRange(range.firstRow(), range.firstColumn(), Math.min(range.lastRow(), used.lastRow()), range.lastColumn());
        sort(r, List.of(SortKey.of(column, descending)), true, false);
    }

    private static final FilterOperator[] OPS = {FilterOperator.EQUAL, FilterOperator.NOT_EQUAL, FilterOperator.GREATER, FilterOperator.GREATER_OR_EQUAL, FilterOperator.LESS, FilterOperator.LESS_OR_EQUAL,
            FilterOperator.BEGINS_WITH, FilterOperator.ENDS_WITH, FilterOperator.CONTAINS, FilterOperator.NOT_CONTAINS};
    private static final String[] OP_NAMES = {"é igual a", "é diferente de", "é maior do que", "é maior ou igual a", "é menor do que", "é menor ou igual a", "começa com", "termina com", "contém", "não contém"};

    private void customFilter(SheetTable table, int column, String name, boolean numeric) {
        SheetForm f = new SheetForm();
        f.section("Mostrar linhas onde: " + name);
        JComboBox<String> op1 = SheetForm.combo(OP_NAMES), op2 = SheetForm.combo(OP_NAMES);
        op1.setSelectedIndex(numeric ? 2 : 8);
        JTextField v1 = SheetForm.text("", 18), v2 = SheetForm.text("", 18);
        JComboBox<String> join = SheetForm.combo("E", "Ou");
        f.add("Condição 1:", op1);
        f.add("Valor:", v1);
        f.add("", join);
        f.add("Condição 2:", op2);
        f.add("Valor:", v2);
        editor.popups().dialog("sheet.customFilter", "Personalizar AutoFiltro", f, () -> {
            List<FilterCondition> list = new ArrayList<>();
            if (!v1.getText().isBlank()) list.add(new FilterCondition(OPS[op1.getSelectedIndex()], v1.getText().strip()));
            if (!v2.getText().isBlank()) list.add(new FilterCondition(OPS[op2.getSelectedIndex()], v2.getText().strip()));
            return FilterCriteria.conditions(list, join.getSelectedIndex() == 0);
        }).ifPresent(c -> setColumnFilter(table, column, c.conditions().isEmpty() ? null : c));
    }

    private void topFilter(SheetTable table, int column) {
        SheetForm f = new SheetForm();
        JComboBox<String> which = SheetForm.combo("Superiores", "Inferiores");
        JSpinner n = SheetForm.integer(10, 1, 500);
        JComboBox<String> unit = SheetForm.combo("Itens", "Por cento");
        f.add("Mostrar:", which);
        f.add("Quantidade:", n);
        f.add("", unit);
        editor.popups().dialog("sheet.top10", "AutoFiltro dos 10 Primeiros", f, () -> FilterCriteria.top(SheetForm.integer(n), unit.getSelectedIndex() == 1, which.getSelectedIndex() == 1))
                .ifPresent(c -> setColumnFilter(table, column, c));
    }

    public String filterStatus() {
        AutoFilter af = ws().properties().autoFilter();
        if (af == null || !af.isFiltered()) return null;
        CellRange r = af.range();
        CellRange used = ws().usedRange();
        int last = used == null ? r.lastRow() : Math.min(r.lastRow(), used.lastRow());
        int total = Math.max(0, last - r.firstRow()), shown = 0;
        for (int row = r.firstRow() + 1; row <= last; row++) if (!ws().rows().isHidden(row)) shown++;
        return shown + " de " + total + " registros encontrados";
    }

    public void createFilterView() {
        AutoFilter af = ws().properties().autoFilter();
        if (af == null) { toggleFilter(); af = ws().properties().autoFilter(); }
        if (af == null) return;
        AutoFilter filter = af;
        int s = sheet();
        editor.popups().prompt("Nova Visão de Filtro", "Nome da visão:", "Filtro " + (ws().properties().filterViews().size() + 1)).filter(n -> !n.isBlank())
                .ifPresent(n -> editor.edit("Visão de filtro", tx -> tx.updateProperties(s, p -> p.withFilterViews(SheetProperties.add(p.filterViews(), new FilterView(UUID.randomUUID().toString(), n, filter))))));
    }

    public void manageFilterViews() {
        List<FilterView> views = ws().properties().filterViews();
        if (views.isEmpty()) { editor.popups().info("Visões de Filtro", "Nenhuma visão de filtro nesta planilha."); return; }
        JList<String> list = new JList<>(views.stream().map(FilterView::name).toArray(String[]::new));
        list.setSelectedIndex(0);
        JCheckBox delete = SheetForm.check("Excluir a visão selecionada", false);
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        p.add(delete, BorderLayout.SOUTH);
        p.setPreferredSize(new Dimension(320, 200));
        int s = sheet();
        editor.popups().dialog("sheet.filterViews", "Visões de Filtro", p, list::getSelectedIndex).filter(i -> i >= 0).ifPresent(i -> {
            FilterView v = views.get(i);
            if (delete.isSelected()) editor.edit("Excluir visão", tx -> tx.updateProperties(s, q -> q.withFilterViews(q.filterViews().stream().filter(x -> !x.id().equals(v.id())).toList())));
            else applyFilter(s, v.filter(), "Aplicar visão de filtro");
        });
    }

    public void showValidationList(CellAddress cell, Rectangle anchor) {
        int s = sheet();
        Optional<DataValidation> v = editor.validationEvaluator().find(s, cell.row(), cell.column());
        if (v.isEmpty() || v.get().type() != ValidationType.LIST) return;
        List<String> items = editor.validationEvaluator().listItems(s, v.get(), cell.row(), cell.column());
        if (items.isEmpty()) return;
        Rectangle r = editor.getCanvas().geometry().rangeRect(editor.getCanvas().mergeAt(cell));
        ValidationListPopup.show(editor.getCanvas(), r, items, editor.displayText(cell), item -> editor.editing().write(s, List.of(CellRange.of(cell)), cell, item, true));
    }

    public void pickFromList() {
        CellAddress a = editor.getSelection().active();
        showValidationList(a, editor.popups().activeCellRect());
    }

    public void toggleCheckbox(CellAddress cell) {
        int s = sheet();
        if (!editor.review().canEdit(s, cell)) { editor.review().warnProtected(); return; }
        CellValue v = ws().cell(cell).value();
        boolean next = !(v instanceof BoolValue b && b.value());
        editor.edit("Caixa de seleção", tx -> tx.updateCell(s, cell.row(), cell.column(), c -> c.withValue(CellValue.of(next))));
    }

    public void insertCheckbox() {
        int s = sheet();
        List<CellRange> ranges = editor.getSelection().ranges();
        editor.edit("Inserir caixa de seleção", tx -> tx.updateProperties(s, p -> {
            List<DataValidation> list = new ArrayList<>(p.validations());
            list.add(DataValidation.checkbox(ranges.getFirst()).withRanges(ranges));
            return p.withValidations(list);
        }));
        editor.edit("Caixa de seleção", tx -> {
            for (CellRange r : ranges) for (CellAddress a : editor.clipboard().bounded(ws(), r)) tx.updateCell(s, a.row(), a.column(), c -> c.value() instanceof BoolValue ? c : c.withValue(CellValue.FALSE));
        });
    }

    public void insertDropdown() {
        editor.popups().prompt("Lista suspensa", "Itens (separados por ponto e vírgula):", "Opção 1; Opção 2; Opção 3").filter(t -> !t.isBlank()).ifPresent(t -> {
            int s = sheet();
            List<CellRange> ranges = editor.getSelection().ranges();
            String source = "\"" + String.join(",", t.split("\\s*[;,]\\s*")) + "\"";
            editor.edit("Lista suspensa", tx -> tx.updateProperties(s, p -> p.withValidations(SheetProperties.add(p.validations(), DataValidation.list(ranges.getFirst(), source).withRanges(ranges)))));
        });
    }

    public void validationDialog() {
        int s = sheet();
        CellAddress a = editor.getSelection().active();
        List<CellRange> ranges = editor.getSelection().ranges();
        DataValidation current = editor.validationEvaluator().find(s, a.row(), a.column()).orElse(null);
        DataValidationPanel panel = new DataValidationPanel(ranges, current,
                canonical -> canonical.startsWith("\"") ? canonical.substring(1, canonical.length() - 1).replace(",", "; ") : "=" + Formulas.toDisplay(canonical, editor.formulaLocale(), ranges.getFirst().first(), false),
                text -> toCanonicalCriterion(text, ranges.getFirst().first()));
        editor.popups().dialog("sheet.validation", "Validação de Dados", panel, panel::result).ifPresent(v -> editor.edit("Validação de dados", tx -> tx.updateProperties(s, p -> {
            List<DataValidation> list = new ArrayList<>();
            for (DataValidation old : p.validations()) {
                List<CellRange> keep = new ArrayList<>();
                for (CellRange r : old.ranges()) if (ranges.stream().noneMatch(x -> x.contains(r))) keep.add(r);
                if (!keep.isEmpty()) list.add(old.withRanges(keep));
            }
            if (v.type() != ValidationType.ANY) list.add(v);
            return p.withValidations(list);
        })));
    }

    private String toCanonicalCriterion(String text, CellAddress host) {
        if (text.startsWith("=")) return editor.editing().canonicalize(text, host);
        CellValue v = editor.valueParser().parse(text).value();
        if (v instanceof NumberValue n) return NumberValue.general(n.value());
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    public void clearValidation() {
        int s = sheet();
        List<CellRange> ranges = editor.getSelection().ranges();
        editor.edit("Limpar validação", tx -> tx.updateProperties(s, p -> {
            List<DataValidation> list = new ArrayList<>();
            for (DataValidation old : p.validations()) {
                List<CellRange> keep = old.ranges().stream().filter(r -> ranges.stream().noneMatch(x -> x.contains(r))).toList();
                if (!keep.isEmpty()) list.add(old.withRanges(keep));
            }
            return p.withValidations(list);
        }));
    }

    public void fill(CellRange source, CellRange target, boolean alternate) {
        int s = sheet();
        if (!editor.review().canEdit(s, target)) { editor.review().warnProtected(); return; }
        boolean singleNumber = source.isSingleCell() && ws().cell(source.first()).value() instanceof NumberValue && !ws().cell(source.first()).hasFormula()
                && !editor.formatter().isDateFormat(editor.getWorkbook().style(ws().cell(source.first()).style()).numberFormat());
        AutoFill.Mode mode = singleNumber ? (alternate ? AutoFill.Mode.SERIES : AutoFill.Mode.COPY) : (alternate ? AutoFill.Mode.COPY : AutoFill.Mode.SERIES);
        CellRange full = source.union(target);
        if (target.contains(source) && !target.equals(source)) full = target;
        CellRange dest = full;
        if (dest.equals(source)) return;
        boolean ok;
        if (source.contains(target)) {
            CellRange clear = difference(source, target);
            ok = clear != null && editor.edit("Limpar", tx -> SheetOperations.clear(tx, s, clear, SheetOperations.ClearMode.CONTENTS));
            if (ok) editor.select(new SheetSelection(target.first(), target.first(), List.of(target)));
            return;
        }
        ok = editor.edit("Preenchimento automático", tx -> AutoFill.fill(tx, s, source, dest, mode, editor.formatter()));
        if (ok) editor.select(new SheetSelection(dest.first(), dest.first(), List.of(dest)));
    }

    private static CellRange difference(CellRange outer, CellRange inner) {
        if (inner.lastRow() < outer.lastRow() && inner.columnCount() == outer.columnCount()) return new CellRange(inner.lastRow() + 1, outer.firstColumn(), outer.lastRow(), outer.lastColumn());
        if (inner.lastColumn() < outer.lastColumn() && inner.rowCount() == outer.rowCount()) return new CellRange(outer.firstRow(), inner.lastColumn() + 1, outer.lastRow(), outer.lastColumn());
        return null;
    }

    public void fillDownToAdjacent() {
        CellRange src = editor.getSelection().range();
        int s = sheet();
        int col = src.firstColumn() > 0 && !editor.getEngine().valueAt(s, src.lastRow() + 1, src.firstColumn() - 1).isEmpty() ? src.firstColumn() - 1
                : !editor.getEngine().valueAt(s, src.lastRow() + 1, src.lastColumn() + 1).isEmpty() ? src.lastColumn() + 1 : -1;
        if (col < 0) return;
        int last = src.lastRow();
        while (last + 1 < ws().rows().count() && !editor.getEngine().valueAt(s, last + 1, col).isEmpty() && last - src.lastRow() < 1_048_576) last++;
        if (last == src.lastRow()) return;
        fill(src, new CellRange(src.firstRow(), src.firstColumn(), last, src.lastColumn()), false);
    }

    public void fillDirection(AutoFill.Direction dir) {
        CellRange r = editor.getSelection().range();
        int s = sheet();
        CellRange source, target = r;
        switch (dir) {
            case DOWN -> {
                if (r.rowCount() == 1) { if (r.firstRow() == 0) return; target = new CellRange(r.firstRow() - 1, r.firstColumn(), r.lastRow(), r.lastColumn()); }
                source = new CellRange(target.firstRow(), target.firstColumn(), target.firstRow(), target.lastColumn());
            }
            case RIGHT -> {
                if (r.columnCount() == 1) { if (r.firstColumn() == 0) return; target = new CellRange(r.firstRow(), r.firstColumn() - 1, r.lastRow(), r.lastColumn()); }
                source = new CellRange(target.firstRow(), target.firstColumn(), target.lastRow(), target.firstColumn());
            }
            case UP -> source = new CellRange(r.lastRow(), r.firstColumn(), r.lastRow(), r.lastColumn());
            default -> source = new CellRange(r.firstRow(), r.lastColumn(), r.lastRow(), r.lastColumn());
        }
        CellRange t = target;
        if (!editor.review().canEdit(s, t)) { editor.review().warnProtected(); return; }
        editor.edit("Preencher", tx -> AutoFill.fill(tx, s, source, t, AutoFill.Mode.COPY, editor.formatter()));
    }

    public void flashFill() {
        CellAddress a = editor.getSelection().active();
        int s = sheet();
        int col = a.column();
        if (col == 0) { editor.popups().info("Preenchimento Relâmpago", "Digite um exemplo ao lado dos dados de origem."); return; }
        CellRange region = FilterEngine.detectRegion(ws(), a.row(), col - 1);
        if (region == null) return;
        int top = region.firstRow(), bottom = region.lastRow();
        List<String> sources = new ArrayList<>(), examples = new ArrayList<>();
        List<Integer> targets = new ArrayList<>();
        for (int r = top; r <= bottom; r++) {
            StringBuilder b = new StringBuilder();
            for (int c = region.firstColumn(); c < col; c++) { if (c > region.firstColumn()) b.append(' '); b.append(editor.displayText(s, new CellAddress(r, c))); }
            String here = editor.displayText(s, new CellAddress(r, col));
            if (!here.isEmpty()) { sources.add(b.toString()); examples.add(here); } else targets.add(r);
        }
        if (examples.isEmpty() || targets.isEmpty()) { editor.popups().info("Preenchimento Relâmpago", "Nenhum padrão para preencher."); return; }
        List<String> exampleSources = List.copyOf(sources);
        List<String> all = new ArrayList<>();
        for (int r : targets) {
            StringBuilder b = new StringBuilder();
            for (int c = region.firstColumn(); c < col; c++) { if (c > region.firstColumn()) b.append(' '); b.append(editor.displayText(s, new CellAddress(r, c))); }
            all.add(b.toString());
        }
        List<String> combined = new ArrayList<>(exampleSources);
        combined.addAll(all);
        Optional<List<String>> inferred = FlashFill.infer(combined, examples);
        if (inferred.isEmpty()) { editor.popups().info("Preenchimento Relâmpago", "O Excel não reconheceu um padrão. Digite mais exemplos."); return; }
        List<String> out = inferred.get();
        int offset = examples.size();
        editor.edit("Preenchimento Relâmpago", tx -> {
            for (int k = 0; k < targets.size() && offset + k < out.size(); k++) editor.editing().writeInto(tx, s, new CellAddress(targets.get(k), col), out.get(offset + k));
        });
    }

    public void seriesDialog() {
        SheetForm f = new SheetForm();
        JComboBox<String> dir = SheetForm.combo("Colunas", "Linhas");
        CellRange r = editor.getSelection().range();
        dir.setSelectedIndex(r.rowCount() >= r.columnCount() ? 0 : 1);
        JComboBox<String> type = SheetForm.combo("Linear", "Crescimento", "Data", "AutoPreenchimento");
        JComboBox<String> unit = SheetForm.combo("Dia", "Dia da semana", "Mês", "Ano");
        JTextField step = SheetForm.text("1", 10), stop = SheetForm.text("", 10);
        f.add("Série em:", dir);
        f.add("Tipo:", type);
        f.add("Unidade de data:", unit);
        f.add("Incremento:", step);
        f.add("Limite:", stop);
        int s = sheet();
        boolean d1904 = editor.getWorkbook().properties().date1904();
        editor.popups().dialog("sheet.series", "Série", f, () -> new Object[]{dir.getSelectedIndex(), type.getSelectedIndex(), unit.getSelectedIndex(), parse(step.getText()), stop.getText().isBlank() ? null : parse(stop.getText())}).ifPresent(o -> {
            AutoFill.SeriesType t = AutoFill.SeriesType.values()[(int) o[1]];
            AutoFill.DateUnit u = AutoFill.DateUnit.values()[(int) o[2]];
            editor.edit("Série", tx -> AutoFill.series(tx, s, r, (int) o[0] == 1, t, u, (Double) o[3], (Double) o[4], d1904));
        });
    }

    private double parse(String text) {
        CellValue v = editor.valueParser().parse(text.strip()).value();
        if (v instanceof NumberValue n) return n.value();
        throw new IllegalArgumentException("Número inválido: " + text);
    }

    public void removeDuplicates(CellRange range) {
        CellRange r = range == null ? region() : range;
        boolean header = detectHeader(r);
        JCheckBox headerBox = SheetForm.check("Meus dados contêm cabeçalhos", header);
        JPanel cols = new JPanel();
        cols.setLayout(new javax.swing.BoxLayout(cols, javax.swing.BoxLayout.Y_AXIS));
        List<JCheckBox> boxes = new ArrayList<>();
        int s = sheet();
        for (int c = r.firstColumn(); c <= r.lastColumn(); c++) {
            String name = editor.displayText(s, new CellAddress(r.firstRow(), c));
            JCheckBox b = SheetForm.check(header && !name.isBlank() ? name : "Coluna " + CellAddress.columnName(c), true);
            boxes.add(b);
            cols.add(b);
        }
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(headerBox, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(cols);
        scroll.setPreferredSize(new Dimension(320, 180));
        p.add(scroll, BorderLayout.CENTER);
        editor.popups().dialog("sheet.removeDuplicates", "Remover Duplicatas", p, () -> {
            List<Integer> selected = new ArrayList<>();
            for (int i = 0; i < boxes.size(); i++) if (boxes.get(i).isSelected()) selected.add(r.firstColumn() + i);
            if (selected.isEmpty()) throw new IllegalArgumentException("Selecione ao menos uma coluna.");
            return selected;
        }).ifPresent(selected -> {
            int[] removed = {0};
            editor.edit("Remover duplicatas", tx -> removed[0] = DataTools.removeDuplicates(tx, editor.getEngine(), s, r, selected, headerBox.isSelected()));
            editor.popups().info("Remover Duplicatas", removed[0] == 0 ? "Nenhum valor duplicado encontrado." : removed[0] + " valores duplicados encontrados e removidos.");
        });
    }

    public void textToColumns() {
        CellRange r = editor.clipboard().bounded(ws(), editor.getSelection().range());
        if (r.columnCount() != 1) { editor.popups().warn("Texto para Colunas", "Selecione apenas uma coluna."); return; }
        SheetForm f = new SheetForm();
        f.section("Delimitadores");
        JCheckBox tab = SheetForm.check("Tabulação", true), semi = SheetForm.check("Ponto e vírgula", false), comma = SheetForm.check("Vírgula", false), space = SheetForm.check("Espaço", false);
        JTextField other = SheetForm.text("", 4);
        JCheckBox consecutive = SheetForm.check("Considerar delimitadores consecutivos como um só", false);
        JComboBox<String> qualifier = SheetForm.combo("\"", "'", "{nenhum}");
        f.full(tab); f.full(semi); f.full(comma); f.full(space);
        f.add("Outros:", other);
        f.full(consecutive);
        f.add("Qualificador de texto:", qualifier);
        int s = sheet();
        editor.popups().dialog("sheet.textToColumns", "Converter Texto em Colunas", f, () -> {
            List<String> d = new ArrayList<>();
            if (tab.isSelected()) d.add("\t");
            if (semi.isSelected()) d.add(";");
            if (comma.isSelected()) d.add(",");
            if (space.isSelected()) d.add(" ");
            if (!other.getText().isEmpty()) d.add(other.getText());
            if (d.isEmpty()) throw new IllegalArgumentException("Escolha ao menos um delimitador.");
            return d;
        }).ifPresent(d -> {
            char q = qualifier.getSelectedIndex() == 2 ? '\0' : qualifier.getSelectedItem().toString().charAt(0);
            editor.edit("Texto para colunas", tx -> DataTools.textToColumns(tx, s, r, d, consecutive.isSelected(), q, editor.valueParser()));
        });
    }

    public void goalSeek() {
        SheetForm f = new SheetForm();
        JTextField set = SheetForm.text(editor.getSelection().active().toA1(), 10), to = SheetForm.text("", 10), change = SheetForm.text("", 10);
        f.add("Definir célula:", set);
        f.add("Para valor:", to);
        f.add("Alternando célula:", change);
        int s = sheet();
        editor.popups().dialog("sheet.goalSeek", "Atingir Meta", f, () -> new Object[]{CellAddress.parse(set.getText().strip().replace("$", "")), parse(to.getText()), CellAddress.parse(change.getText().strip().replace("$", ""))}).ifPresent(o -> {
            CellAddress formula = (CellAddress) o[0], changing = (CellAddress) o[2];
            if (!ws().cell(formula).hasFormula()) { editor.popups().warn("Atingir Meta", "A célula deve conter uma fórmula."); return; }
            DataTools.GoalSeekResult[] result = new DataTools.GoalSeekResult[1];
            editor.edit("Atingir meta", tx -> result[0] = DataTools.goalSeek(tx, editor.getEngine(), s, formula, (Double) o[1], changing));
            if (result[0] != null) editor.popups().info("Status do Atingir Meta", (result[0].converged() ? "Foi encontrada uma solução." : "Talvez não tenha sido encontrada uma solução.")
                    + "\n\nValor de destino: " + o[1] + "\nValor atual: " + editor.displayText(formula));
        });
    }

    public void dataTable() {
        CellRange r = editor.getSelection().range();
        SheetForm f = new SheetForm();
        JTextField row = SheetForm.text("", 10), col = SheetForm.text("", 10);
        f.add("Célula de entrada da linha:", row);
        f.add("Célula de entrada da coluna:", col);
        int s = sheet();
        editor.popups().dialog("sheet.dataTable", "Tabela de Dados", f, () -> new CellAddress[]{row.getText().isBlank() ? null : CellAddress.parse(row.getText().strip().replace("$", "")), col.getText().isBlank() ? null : CellAddress.parse(col.getText().strip().replace("$", ""))})
                .ifPresent(a -> editor.edit("Tabela de dados", tx -> DataTools.dataTable(tx, editor.getEngine(), s, r, a[0], a[1])));
    }

    public void scenarios() {
        List<Scenario> list = ws().properties().scenarios();
        JList<String> names = new JList<>(list.stream().map(Scenario::name).toArray(String[]::new));
        JComboBox<String> action = SheetForm.combo("Mostrar cenário selecionado", "Adicionar cenário com as células selecionadas", "Excluir cenário selecionado");
        JTextField name = SheetForm.text("Cenário " + (list.size() + 1), 18);
        SheetForm f = new SheetForm();
        JScrollPane scroll = new JScrollPane(names);
        scroll.setPreferredSize(new Dimension(300, 140));
        f.grow(scroll);
        f.add("Ação:", action);
        f.add("Nome do novo cenário:", name);
        int s = sheet();
        editor.popups().dialog("sheet.scenarios", "Gerenciador de Cenários", f, () -> new Object[]{action.getSelectedIndex(), names.getSelectedIndex(), name.getText().strip()}).ifPresent(o -> {
            int act = (int) o[0], idx = (int) o[1];
            if (act == 1) {
                Map<CellAddress, CellValue> values = new LinkedHashMap<>();
                for (CellRange r : editor.getSelection().ranges()) for (CellAddress a : editor.clipboard().bounded(ws(), r)) values.put(a, ws().cell(a).value());
                Scenario sc = new Scenario((String) o[2], values, "Criado por " + editor.getSession().getAuthor());
                editor.edit("Adicionar cenário", tx -> tx.updateProperties(s, p -> p.withScenarios(SheetProperties.add(p.scenarios(), sc))));
            } else if (idx >= 0) {
                Scenario sc = list.get(idx);
                if (act == 0) editor.edit("Mostrar cenário", tx -> DataTools.applyScenario(tx, s, sc));
                else editor.edit("Excluir cenário", tx -> tx.updateProperties(s, p -> p.withScenarios(p.scenarios().stream().filter(x -> x != sc).toList())));
            }
        });
    }

    public void subtotal() {
        CellRange r = region();
        int s = sheet();
        List<String> names = new ArrayList<>();
        for (int c = r.firstColumn(); c <= r.lastColumn(); c++) { String n = editor.displayText(s, new CellAddress(r.firstRow(), c)); names.add(n.isBlank() ? "Coluna " + CellAddress.columnName(c) : n); }
        SheetForm f = new SheetForm();
        JComboBox<String> group = new JComboBox<>(names.toArray(String[]::new));
        TotalsFunction[] fns = {TotalsFunction.SUM, TotalsFunction.COUNTA, TotalsFunction.AVERAGE, TotalsFunction.MAX, TotalsFunction.MIN, TotalsFunction.COUNT, TotalsFunction.STDDEV, TotalsFunction.VAR};
        JComboBox<String> fn = SheetForm.combo("Soma", "Contagem", "Média", "Máx", "Mín", "Contar Números", "DesvPad", "Var");
        JComboBox<String> total = new JComboBox<>(names.toArray(String[]::new));
        total.setSelectedIndex(names.size() - 1);
        f.add("A cada alteração em:", group);
        f.add("Usar função:", fn);
        f.add("Adicionar subtotal a:", total);
        editor.popups().dialog("sheet.subtotal", "Subtotais", f, () -> new int[]{group.getSelectedIndex(), fn.getSelectedIndex(), total.getSelectedIndex()})
                .ifPresent(o -> editor.edit("Subtotal", tx -> DataTools.subtotals(tx, editor.getEngine(), s, r, r.firstColumn() + o[0], fns[o[1]], List.of(r.firstColumn() + o[2]), true)));
    }

    public void consolidate() {
        SheetForm f = new SheetForm();
        JComboBox<String> fn = SheetForm.combo("Soma", "Contagem", "Média", "Máx", "Mín");
        TotalsFunction[] fns = {TotalsFunction.SUM, TotalsFunction.COUNTA, TotalsFunction.AVERAGE, TotalsFunction.MAX, TotalsFunction.MIN};
        JTextField refs = SheetForm.text("", 30);
        refs.setToolTipText("Referências separadas por ponto e vírgula, ex.: Plan1!A1:C10; Plan2!A1:C10");
        JCheckBox labels = SheetForm.check("Usar rótulos na linha superior e coluna esquerda", true);
        f.add("Função:", fn);
        f.add("Referências:", refs);
        f.full(labels);
        int s = sheet();
        CellAddress target = editor.getSelection().active();
        editor.popups().dialog("sheet.consolidate", "Consolidar", f, () -> {
            List<Integer> sheets = new ArrayList<>();
            List<CellRange> ranges = new ArrayList<>();
            for (String part : refs.getText().split(";")) {
                if (part.isBlank()) continue;
                NavigationController.Target t = editor.navigation().resolve(part.strip()).orElseThrow(() -> new IllegalArgumentException("Referência inválida: " + part));
                sheets.add(t.sheet());
                ranges.add(t.ranges().getFirst());
            }
            if (ranges.isEmpty()) throw new IllegalArgumentException("Informe ao menos uma referência.");
            return new Object[]{sheets, ranges};
        }).ifPresent(o -> {
            @SuppressWarnings("unchecked") List<Integer> sheets = (List<Integer>) o[0];
            @SuppressWarnings("unchecked") List<CellRange> ranges = (List<CellRange>) o[1];
            editor.edit("Consolidar", tx -> DataTools.consolidate(tx, editor.getEngine(), s, target, sheets, ranges, fns[fn.getSelectedIndex()], labels.isSelected()));
        });
    }

    public void forecast() {
        CellRange r = region();
        if (r.columnCount() < 2 || r.rowCount() < 3) { editor.popups().warn("Planilha de Previsão", "Selecione duas colunas: datas/períodos e valores."); return; }
        JSpinner periods = SheetForm.integer(12, 1, 1000);
        SheetForm f = new SheetForm();
        f.add("Períodos a prever:", periods);
        int s = sheet();
        String src = editor.activeSheet().name();
        editor.popups().dialog("sheet.forecast", "Criar Planilha de Previsão", f, () -> SheetForm.integer(periods)).ifPresent(n -> editor.edit("Planilha de previsão", tx -> {
            int idx = SheetOperations.addSheet(tx, s + 1, null);
            boolean header = detectHeader(r);
            int first = r.firstRow() + (header ? 1 : 0);
            String xRef = NavigationController.reference(src, new CellRange(first, r.firstColumn(), r.lastRow(), r.firstColumn()), true);
            String yRef = NavigationController.reference(src, new CellRange(first, r.firstColumn() + 1, r.lastRow(), r.firstColumn() + 1), true);
            tx.setCell(idx, 0, 0, dtm.stools.component.panels.editor.sheet.model.SheetCell.of(CellValue.of("Período")));
            tx.setCell(idx, 0, 1, dtm.stools.component.panels.editor.sheet.model.SheetCell.of(CellValue.of("Previsão")));
            int rows = r.lastRow() - first + 1;
            for (int k = 0; k < rows; k++) {
                tx.setCell(idx, k + 1, 0, dtm.stools.component.panels.editor.sheet.model.SheetCell.formula(NavigationController.reference(src, CellRange.of(first + k, r.firstColumn()), false)));
                tx.setCell(idx, k + 1, 1, dtm.stools.component.panels.editor.sheet.model.SheetCell.formula(NavigationController.reference(src, CellRange.of(first + k, r.firstColumn() + 1), false)));
            }
            for (int k = 0; k < n; k++) {
                int row = rows + 1 + k;
                tx.setCell(idx, row, 0, dtm.stools.component.panels.editor.sheet.model.SheetCell.formula("A" + row + "+(A" + row + "-A" + (row - 1) + ")"));
                tx.setCell(idx, row, 1, dtm.stools.component.panels.editor.sheet.model.SheetCell.formula("FORECAST.LINEAR(A" + (row + 1) + "," + yRef + "," + xRef + ")"));
            }
            tx.select(idx, SheetSelection.home());
        }));
    }

    public void refreshAll() {
        editor.getEngine().calculateFull();
        editor.objects().refreshAllPivots();
        reapplyFilter();
        editor.getCanvas().repaint();
    }

    public void createTable(String style) {
        int s = sheet();
        CellAddress a = editor.getSelection().active();
        SheetTable existing = tableAt(s, a);
        if (existing != null) {
            if (style != null) editor.edit("Estilo de tabela", tx -> tx.updateProperties(s, p -> p.replaceTable(existing.name(), t -> t.withStyle(style))));
            return;
        }
        CellRange r = region();
        for (SheetTable t : ws().properties().tables()) if (t.range().intersects(r)) { editor.popups().warn("Tabela", "O intervalo selecionado se sobrepõe a outra tabela."); return; }
        if (ws().properties().merges().stream().anyMatch(m -> m.intersects(r))) { editor.popups().warn("Tabela", "Tabelas não podem conter células mescladas."); return; }
        JTextField ref = SheetForm.text("=" + NavigationController.absolute(r), 18);
        JCheckBox headerBox = SheetForm.check("Minha tabela tem cabeçalhos", detectHeader(r) || ws().cell(r.first()).value() instanceof TextValue);
        SheetForm f = new SheetForm();
        f.add("Onde estão os dados da tabela?", ref);
        f.full(headerBox);
        Optional<Object[]> answer = editor.isShowing() ? editor.popups().dialog("sheet.createTable", "Criar Tabela", f, () -> new Object[]{editor.navigation().resolve(ref.getText()).orElseThrow(() -> new IllegalArgumentException("Referência inválida.")).ranges().getFirst(), headerBox.isSelected()})
                : Optional.of(new Object[]{r, true});
        answer.ifPresent(o -> {
            CellRange range = (CellRange) o[0];
            boolean header = (boolean) o[1];
            int id = editor.getWorkbook().nextTableId();
            String name = uniqueTableName("Tabela" + id);
            editor.edit("Criar tabela", tx -> {
                CellRange tr = range;
                if (!header) {
                    SheetOperations.insertCells(tx, s, new CellRange(range.firstRow(), range.firstColumn(), range.firstRow(), range.lastColumn()), true);
                    tr = new CellRange(range.firstRow(), range.firstColumn(), range.lastRow() + 1, range.lastColumn());
                    for (int c = range.firstColumn(); c <= range.lastColumn(); c++)
                        tx.setCell(s, range.firstRow(), c, dtm.stools.component.panels.editor.sheet.model.SheetCell.of(CellValue.of("Coluna" + (c - range.firstColumn() + 1))));
                }
                List<String> headers = new ArrayList<>();
                for (int c = tr.firstColumn(); c <= tr.lastColumn(); c++) {
                    String h = editor.getWorkbook().sheet(s).cell(tr.firstRow(), c).value().display();
                    if (h.isBlank() || headers.stream().anyMatch(h::equalsIgnoreCase)) h = "Coluna" + (c - tr.firstColumn() + 1);
                    headers.add(h);
                    tx.setCell(s, tr.firstRow(), c, tx.cell(s, tr.firstRow(), c).withValue(CellValue.of(h)));
                }
                SheetTable t = SheetTable.create(id, name, tr, headers);
                if (style != null) t = t.withStyle(style);
                SheetTable table = t;
                tx.updateProperties(s, p -> p.withTables(SheetProperties.add(p.tables(), table)));
            });
        });
    }

    private String uniqueTableName(String base) {
        String name = base;
        int n = 1;
        while (editor.getWorkbook().table(name).isPresent()) name = base + "_" + (++n);
        return name;
    }

    public SheetTable activeTable() { return tableAt(sheet(), editor.getSelection().active()); }

    public void updateActiveTable(String label, java.util.function.UnaryOperator<SheetTable> change) {
        SheetTable t = activeTable();
        if (t == null) return;
        int s = sheet();
        editor.edit(label, tx -> tx.updateProperties(s, p -> p.replaceTable(t.name(), change)));
    }

    public void toggleTotals() {
        SheetTable t = activeTable();
        if (t == null) return;
        int s = sheet();
        if (t.totalsRow()) {
            CellRange totals = t.totalsRange();
            editor.edit("Linha de totais", tx -> {
                SheetOperations.clear(tx, s, totals, SheetOperations.ClearMode.ALL);
                tx.updateProperties(s, p -> p.replaceTable(t.name(), x -> x.withTotalsRow(false).withRange(new CellRange(x.range().firstRow(), x.range().firstColumn(), x.range().lastRow() - 1, x.range().lastColumn()))));
            });
            return;
        }
        int row = t.range().lastRow() + 1;
        editor.edit("Linha de totais", tx -> {
            List<TableColumn> cols = new ArrayList<>(t.columns());
            for (int i = 0; i < cols.size(); i++) {
                int c = t.range().firstColumn() + i;
                if (i == 0) { tx.setCell(s, row, c, tx.cell(s, row, c).withValue(CellValue.of("Total"))); continue; }
                if (i == cols.size() - 1) {
                    cols.set(i, cols.get(i).withTotals(TotalsFunction.SUM));
                    String col = cols.get(i).name().replace("]", "']").replace("[", "'[");
                    tx.setCell(s, row, c, tx.cell(s, row, c).withFormula("SUBTOTAL(109," + t.name() + "[" + col + "])", CellValue.EMPTY));
                }
            }
            tx.updateProperties(s, p -> p.replaceTable(t.name(), x -> x.withTotalsRow(true).withColumns(cols).withRange(new CellRange(x.range().firstRow(), x.range().firstColumn(), row, x.range().lastColumn()))));
        });
    }

    public void convertToRange() {
        SheetTable t = activeTable();
        if (t == null) return;
        if (!editor.popups().confirm("Converter em Intervalo", "Deseja converter a tabela em um intervalo normal?") && editor.isShowing()) return;
        int s = sheet();
        editor.edit("Converter em intervalo", tx -> tx.updateProperties(s, p -> p.replaceTable(t.name(), x -> null)));
    }

    public void renameTable() {
        SheetTable t = activeTable();
        if (t == null) return;
        editor.popups().prompt("Nome da Tabela", "Nome:", t.name()).filter(n -> !n.isBlank() && !n.equals(t.name())).ifPresent(n -> {
            if (!n.matches("[\\p{L}_\\\\][\\p{L}\\p{N}_.]*")) { editor.popups().warn("Nome da Tabela", "Nome de tabela inválido."); return; }
            if (editor.getWorkbook().table(n).isPresent() || editor.getWorkbook().name(n, null).isPresent()) { editor.popups().warn("Nome da Tabela", "Já existe uma tabela ou nome com esse nome."); return; }
            int s = sheet();
            String old = t.name();
            editor.edit("Renomear tabela", tx -> {
                tx.updateProperties(s, p -> p.replaceTable(old, x -> x.withName(n)));
                SheetOperations.rewriteFormulas(tx, f -> f.replaceAll("(?i)\\b" + java.util.regex.Pattern.quote(old) + "\\[", java.util.regex.Matcher.quoteReplacement(n) + "["));
            });
        });
    }

    public void resizeTable() {
        SheetTable t = activeTable();
        if (t == null) return;
        editor.popups().prompt("Redimensionar Tabela", "Novo intervalo de dados:", NavigationController.absolute(t.range())).flatMap(editor.navigation()::resolve).ifPresent(target -> {
            CellRange r = target.ranges().getFirst();
            if (r.firstRow() != t.range().firstRow()) { editor.popups().warn("Redimensionar", "Os cabeçalhos devem permanecer na mesma linha."); return; }
            int s = sheet();
            editor.edit("Redimensionar tabela", tx -> tx.updateProperties(s, p -> p.replaceTable(t.name(), x -> {
                List<TableColumn> cols = new ArrayList<>();
                for (int c = r.firstColumn(); c <= r.lastColumn(); c++) {
                    int old = c - x.range().firstColumn();
                    if (old >= 0 && old < x.columns().size()) cols.add(x.columns().get(old));
                    else { String h = editor.getWorkbook().sheet(s).cell(r.firstRow(), c).value().display(); cols.add(TableColumn.of(h.isBlank() ? "Coluna" + (c - r.firstColumn() + 1) : h)); }
                }
                return x.toBuilder().range(r).columns(cols).build();
            })));
        });
    }

    public void autoExpandTable(SheetTransaction tx, int s, CellAddress host) {
        SheetWorksheet w = editor.getWorkbook().sheet(s);
        for (SheetTable t : w.properties().tables()) {
            CellRange r = t.range();
            if (t.totalsRow()) continue;
            if (host.row() == r.lastRow() + 1 && host.column() >= r.firstColumn() && host.column() <= r.lastColumn()) {
                tx.updateProperties(s, p -> p.replaceTable(t.name(), x -> x.withRange(new CellRange(r.firstRow(), r.firstColumn(), r.lastRow() + 1, r.lastColumn()))));
                for (int i = 0; i < t.columns().size(); i++) {
                    String f = t.columns().get(i).calculatedFormula();
                    int c = r.firstColumn() + i;
                    if (f != null && c != host.column()) tx.updateCell(s, host.row(), c, cell -> cell.hasContent() ? cell : cell.withFormula(f, CellValue.EMPTY));
                }
                return;
            }
            if (t.headerRow() && host.column() == r.lastColumn() + 1 && host.row() == r.firstRow()) {
                String h = w.cell(host).value().display();
                tx.updateProperties(s, p -> p.replaceTable(t.name(), x -> x.toBuilder().range(new CellRange(r.firstRow(), r.firstColumn(), r.lastRow(), r.lastColumn() + 1))
                        .columns(SheetProperties.add(x.columns(), TableColumn.of(h.isBlank() ? "Coluna" + (x.columns().size() + 1) : h))).build()));
                return;
            }
        }
    }

    public String selectionAggregates(Map<String, Boolean> shown) {
        SheetSelection sel = editor.getSelection();
        if (sel.isSingleCell()) return "";
        int s = sheet();
        CellRange used = editor.getEngine().usedRange(s);
        if (used == null) return "";
        double sum = 0, min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        long count = 0, numbers = 0, visited = 0;
        boolean error = false;
        for (CellRange r0 : sel.ranges()) {
            CellRange r = r0.intersection(used);
            if (r == null) continue;
            for (int row = r.firstRow(); row <= r.lastRow(); row++) {
                if (ws().rows().isHidden(row)) continue;
                for (int col = r.firstColumn(); col <= r.lastColumn(); col++) {
                    if (++visited > 2_000_000) break;
                    CellValue v = editor.getEngine().valueAt(s, row, col);
                    if (v.isEmpty()) continue;
                    count++;
                    if (v.isError()) error = true;
                    if (v instanceof NumberValue n) { numbers++; sum += n.value(); min = Math.min(min, n.value()); max = Math.max(max, n.value()); }
                }
            }
        }
        if (count == 0) return "";
        List<String> parts = new ArrayList<>();
        String fmt = editor.getWorkbook().style(ws().cell(sel.active()).style()).numberFormat();
        java.util.function.DoubleFunction<String> f = d -> editor.formatter().format(CellValue.of(d), fmt == null || editor.formatter().isTextFormat(fmt) ? "General" : fmt).text();
        if (numbers > 0 && !error) {
            if (shown.getOrDefault("Média", true)) parts.add("Média: " + f.apply(sum / numbers));
        }
        if (shown.getOrDefault("Contagem", true)) parts.add("Contagem: " + count);
        if (shown.getOrDefault("Contagem Numérica", false) && numbers > 0) parts.add("Contagem Numérica: " + numbers);
        if (numbers > 0 && !error) {
            if (shown.getOrDefault("Mín", false)) parts.add("Mín: " + f.apply(min));
            if (shown.getOrDefault("Máx", false)) parts.add("Máx: " + f.apply(max));
            if (shown.getOrDefault("Soma", true)) parts.add("Soma: " + f.apply(sum));
        }
        return String.join("    ", parts);
    }

}
