package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.calc.FormulaCell;
import dtm.stools.component.panels.editor.sheet.data.FilterEngine;
import dtm.stools.component.panels.editor.sheet.function.FunctionCategory;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ConditionalFormat;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetBorder;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetTheme;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.provider.SheetNumberFormatProvider;
import dtm.stools.component.panels.editor.sheet.ui.SheetGeometry;
import dtm.stools.component.panels.editor.sheet.ui.popup.FormatCellsPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.GoToSpecialPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.InsertFunctionPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.PasteSpecialPanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.SheetForm;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class SheetCommandSupport {
    public static final List<SheetTheme> THEMES = List.of(
            SheetTheme.OFFICE,
            new SheetTheme("Office 2013", List.of(0xFF000000, 0xFFFFFFFF, 0xFF44546A, 0xFFE7E6E6, 0xFF4472C4, 0xFFED7D31, 0xFFA5A5A5, 0xFFFFC000, 0xFF5B9BD5, 0xFF70AD47, 0xFF0563C1, 0xFF954F72), "Calibri Light", "Calibri"),
            new SheetTheme("Azul", List.of(0xFF000000, 0xFFFFFFFF, 0xFF17406D, 0xFFDBEFF9, 0xFF0F6FC6, 0xFF009DD9, 0xFF0BD0D9, 0xFF10CF9B, 0xFF7CCA62, 0xFFA5C249, 0xFFF49100, 0xFF85DFD0), "Calibri", "Calibri"),
            new SheetTheme("Verde", List.of(0xFF000000, 0xFFFFFFFF, 0xFF455F51, 0xFFE3DED1, 0xFF549E39, 0xFF8AB833, 0xFFC0CF3A, 0xFF029676, 0xFF4AB5C4, 0xFF0989B1, 0xFF6B9F25, 0xFFBA6906), "Calibri", "Calibri"),
            new SheetTheme("Violeta", List.of(0xFF000000, 0xFFFFFFFF, 0xFF373545, 0xFFDCD8DC, 0xFFAD84C6, 0xFF8784C7, 0xFF5D739A, 0xFF6997AF, 0xFF84ACB6, 0xFF6F8183, 0xFF69A020, 0xFF8C8C8C), "Calibri", "Calibri"),
            new SheetTheme("Laranja", List.of(0xFF000000, 0xFFFFFFFF, 0xFF4E3B30, 0xFFFBEEC9, 0xFFE48312, 0xFFBD582C, 0xFF865640, 0xFF9B8357, 0xFFC2BC80, 0xFF94A088, 0xFF2998E3, 0xFF8C8C8C), "Calibri", "Calibri"));

    private SheetCommandSupport() {}

    public static void formatCells(SheetEditor editor, int tab) {
        CellAddress a = editor.getSelection().active();
        Map<String, List<String>> extra = new LinkedHashMap<>();
        for (SheetNumberFormatProvider p : editor.providers(SheetNumberFormatProvider.class)) p.formats().forEach((k, v) -> extra.merge(k, List.copyOf(v), (x, y) -> { List<String> l = new ArrayList<>(x); l.addAll(y); return l; }));
        FormatCellsPanel panel = new FormatCellsPanel(editor.format().activeStyle(), editor.formatter(), editor.getEngine().valueAt(editor.activeSheetIndex(), a), extra, tab);
        editor.popups().dialog(SheetDialogIds.FORMAT_CELLS, "Formatar Células", panel, () -> panel).ifPresent(p -> {
            editor.format().apply("Formatar células", p.result());
            SheetBorder outline = p.outlineBorder();
            if (outline != null) outline(editor, outline);
        });
    }

    public static void outline(SheetEditor editor, SheetBorder border) {
        int s = editor.activeSheetIndex();
        List<CellRange> ranges = new ArrayList<>();
        for (CellRange r : editor.getSelection().ranges()) ranges.add(editor.clipboard().bounded(editor.activeSheet(), r));
        editor.edit("Bordas", tx -> {
            for (CellRange r : ranges) for (CellAddress c : r) {
                if (c.row() == r.firstRow()) tx.setStyle(s, c.row(), c.column(), st -> st.withTop(border));
                if (c.row() == r.lastRow()) tx.setStyle(s, c.row(), c.column(), st -> st.withBottom(border));
                if (c.column() == r.firstColumn()) tx.setStyle(s, c.row(), c.column(), st -> st.withLeft(border));
                if (c.column() == r.lastColumn()) tx.setStyle(s, c.row(), c.column(), st -> st.withRight(border));
            }
        });
    }

    public static void insertFunction(SheetEditor editor, FunctionCategory category) {
        InsertFunctionPanel panel = new InsertFunctionPanel(editor.getEngine().functions(), editor.formulaLocale(), category);
        Optional<String> name = editor.popups().dialog(SheetDialogIds.INSERT_FUNCTION, "Inserir Função", panel, panel::result);
        if (name.isEmpty() || editor.isReadOnlyView()) return;
        if (editor.isEditing() && editor.editing().text().startsWith("=")) { editor.editing().insertAtCaret(name.get() + "("); return; }
        editor.startEditing("=" + name.get() + "(", true);
    }

    public static void pasteSpecial(SheetEditor editor) {
        PasteSpecialPanel panel = new PasteSpecialPanel();
        editor.popups().dialog(SheetDialogIds.PASTE_SPECIAL, "Colar Especial", panel, panel::result).ifPresent(o -> editor.clipboard().paste(o));
    }

    public static void goTo(SheetEditor editor) {
        editor.popups().prompt("Ir Para", "Referência:", "").filter(t -> !t.isBlank()).ifPresent(t -> {
            if (!editor.navigation().goTo(t)) editor.popups().warn("Ir Para", "Referência inválida: " + t);
        });
    }

    public static void goToSpecial(SheetEditor editor) {
        GoToSpecialPanel panel = new GoToSpecialPanel();
        editor.popups().dialog(SheetDialogIds.GO_TO_SPECIAL, "Ir para Especial", panel, panel::result).ifPresent(k -> selectSpecial(editor, k));
    }

    public static void selectSpecial(SheetEditor editor, GoToSpecialPanel.Kind kind) {
        int s = editor.activeSheetIndex();
        SheetWorksheet ws = editor.activeSheet();
        SheetSelection sel = editor.getSelection();
        CellRange scope = sel.isSingleCell() ? editor.getEngine().usedRange(s) : editor.clipboard().bounded(ws, sel.range());
        if (scope == null) { editor.popups().info("Ir para Especial", "Nenhuma célula foi encontrada."); return; }
        List<CellAddress> found = new ArrayList<>();
        CellAddress a = sel.active();
        switch (kind) {
            case NOTES -> { ws.properties().notes().keySet().stream().filter(scope::contains).forEach(found::add); ws.properties().threads().keySet().stream().filter(scope::contains).forEach(found::add); }
            case CONSTANTS -> ws.cells().forEach(scope, (r, c, cell) -> { if (!cell.hasFormula() && !cell.value().isEmpty()) found.add(new CellAddress(r, c)); });
            case FORMULAS -> ws.cells().forEach(scope, (r, c, cell) -> { if (cell.hasFormula()) found.add(new CellAddress(r, c)); });
            case ERRORS -> ws.cells().forEach(scope, (r, c, cell) -> { if (cell.hasFormula() && editor.getEngine().valueAt(s, r, c) instanceof ErrorValue) found.add(new CellAddress(r, c)); });
            case BLANKS -> { if (scope.cellCount() <= 200_000) for (CellAddress x : scope) if (editor.getEngine().valueAt(s, x).isEmpty()) found.add(x); }
            case CURRENT_REGION -> { CellRange r = FilterEngine.detectRegion(ws, a.row(), a.column()); if (r != null) { editor.select(new SheetSelection(a, r.first(), List.of(r))); return; } }
            case CURRENT_ARRAY -> {
                Optional<FormulaCell> anchor = editor.getEngine().spillAnchor(s, a.row(), a.column());
                if (anchor.isPresent() && anchor.get().spillRange() != null) { CellRange r = anchor.get().spillRange(); editor.select(new SheetSelection(r.first(), r.first(), List.of(r))); return; }
            }
            case OBJECTS -> {
                List<SheetObject> objects = ws.properties().objects();
                if (!objects.isEmpty()) { editor.objects().selected(objects.getFirst()); return; }
            }
            case PRECEDENTS -> {
                List<CellRange> ranges = new ArrayList<>();
                editor.getEngine().precedents(s, a.row(), a.column()).stream().filter(d -> d.sheet() == s).forEach(d -> ranges.add(d.range()));
                if (!ranges.isEmpty()) { editor.select(new SheetSelection(ranges.getFirst().first(), ranges.getFirst().first(), ranges)); return; }
            }
            case DEPENDENTS -> found.addAll(editor.getEngine().dependents(s, a.row(), a.column()));
            case LAST_CELL -> { editor.navigation().documentEnd(false); return; }
            case VISIBLE -> {
                List<CellRange> ranges = new ArrayList<>();
                int start = -1;
                for (int r = scope.firstRow(); r <= scope.lastRow() + 1; r++) {
                    boolean visible = r <= scope.lastRow() && !ws.rows().isHidden(r);
                    if (visible && start < 0) start = r;
                    if (!visible && start >= 0) { ranges.add(new CellRange(start, scope.firstColumn(), r - 1, scope.lastColumn())); start = -1; }
                }
                if (!ranges.isEmpty()) { editor.select(new SheetSelection(ranges.getFirst().first(), ranges.getFirst().first(), ranges)); return; }
            }
            case CONDITIONAL -> {
                List<CellRange> ranges = new ArrayList<>();
                for (ConditionalFormat f : ws.properties().conditionalFormats()) ranges.addAll(f.ranges());
                if (!ranges.isEmpty()) { editor.select(new SheetSelection(ranges.getFirst().first(), ranges.getFirst().first(), ranges)); return; }
            }
            case VALIDATION -> {
                List<CellRange> ranges = new ArrayList<>();
                for (DataValidation v : ws.properties().validations()) ranges.addAll(v.ranges());
                if (!ranges.isEmpty()) { editor.select(new SheetSelection(ranges.getFirst().first(), ranges.getFirst().first(), ranges)); return; }
            }
        }
        if (found.isEmpty()) { editor.popups().info("Ir para Especial", "Nenhuma célula foi encontrada."); return; }
        List<CellRange> ranges = new ArrayList<>();
        for (CellAddress x : found.stream().distinct().sorted().limit(10_000).toList()) ranges.add(CellRange.of(x));
        editor.select(new SheetSelection(ranges.getFirst().first(), ranges.getFirst().first(), ranges));
    }

    public static void zoomDialog(SheetEditor editor) {
        JPanel p = new JPanel(new GridLayout(0, 1, 2, 2));
        ButtonGroup g = new ButtonGroup();
        double[] values = {2, 1, 0.75, 0.5, 0.25};
        List<JRadioButton> buttons = new ArrayList<>();
        for (double v : values) { JRadioButton b = new JRadioButton(Math.round(v * 100) + "%", Math.abs(editor.effectiveZoom() - v) < 0.001); g.add(b); p.add(b); buttons.add(b); }
        JRadioButton fit = new JRadioButton("Ajustar à seleção"), custom = new JRadioButton("Personalizado:");
        g.add(fit);
        g.add(custom);
        p.add(fit);
        JSpinner spinner = SheetForm.integer((int) Math.round(editor.effectiveZoom() * 100), 10, 400);
        JPanel row = new JPanel(new GridLayout(1, 2, 4, 0));
        row.add(custom);
        row.add(spinner);
        p.add(row);
        if (buttons.stream().noneMatch(JRadioButton::isSelected)) custom.setSelected(true);
        spinner.addChangeListener(e -> custom.setSelected(true));
        editor.popups().dialog("sheet.zoom", "Zoom", p, () -> {
            for (int i = 0; i < buttons.size(); i++) if (buttons.get(i).isSelected()) return values[i];
            if (fit.isSelected()) return -1.0;
            return SheetForm.integer(spinner) / 100.0;
        }).ifPresent(z -> { if (z < 0) zoomToSelection(editor); else editor.setZoom(z); });
    }

    public static void zoomToSelection(SheetEditor editor) {
        CellRange r = editor.clipboard().bounded(editor.activeSheet(), editor.getSelection().range());
        SheetWorksheet ws = editor.activeSheet();
        SheetGeometry g = editor.getCanvas().geometry();
        double w = ws.columns().position(r.lastColumn() + 1) - ws.columns().position(r.firstColumn());
        double h = ws.rows().position(r.lastRow() + 1) - ws.rows().position(r.firstRow());
        double availW = editor.getCanvas().getWidth() - g.headerWidth() - 20, availH = editor.getCanvas().getHeight() - g.headerHeight() - 20;
        double z = Math.max(0.1, Math.min(4, Math.min(availW / Math.max(1, w), availH / Math.max(1, h))));
        editor.setZoom(Math.floor(z * 100) / 100.0);
        editor.getCanvas().scrollToTop(r.firstRow(), r.firstColumn());
    }

    public static void autosum(SheetEditor editor, String function) {
        if (editor.isReadOnlyView()) return;
        int s = editor.activeSheetIndex();
        SheetSelection sel = editor.getSelection();
        CellRange r = sel.range();
        String localized = editor.formulaLocale().localizeFunction(function);
        if (r.isSingleCell()) {
            CellAddress a = sel.active();
            CellRange source = adjacentNumbers(editor, s, a);
            String ref = source == null ? "" : source.toA1();
            editor.startEditing("=" + localized + "(" + ref + ")", true);
            return;
        }
        CellRange b = editor.clipboard().bounded(editor.activeSheet(), r);
        int targetRow = b.lastRow() + 1;
        boolean lastRowEmpty = true;
        for (int c = b.firstColumn(); c <= b.lastColumn(); c++) if (!editor.getEngine().valueAt(s, b.lastRow(), c).isEmpty()) lastRowEmpty = false;
        if (lastRowEmpty && b.rowCount() > 1) targetRow = b.lastRow();
        int tr = targetRow;
        int lastDataRow = lastRowEmpty && b.rowCount() > 1 ? b.lastRow() - 1 : b.lastRow();
        editor.edit("AutoSoma", tx -> {
            for (int c = b.firstColumn(); c <= b.lastColumn(); c++) {
                String range = new CellRange(b.firstRow(), c, lastDataRow, c).toA1();
                tx.updateCell(s, tr, c, cell -> cell.withFormula(function + "(" + range + ")", CellValue.EMPTY));
            }
        });
    }

    private static CellRange adjacentNumbers(SheetEditor editor, int s, CellAddress a) {
        int row = a.row() - 1;
        while (row >= 0 && editor.getEngine().valueAt(s, row, a.column()) instanceof NumberValue && !editor.activeSheet().cell(row, a.column()).hasFormula() || row >= 0 && editor.getEngine().valueAt(s, row, a.column()) instanceof NumberValue && row == a.row() - 1) row--;
        if (row < a.row() - 1) return new CellRange(row + 1, a.column(), a.row() - 1, a.column());
        int col = a.column() - 1;
        while (col >= 0 && editor.getEngine().valueAt(s, a.row(), col) instanceof NumberValue) col--;
        if (col < a.column() - 1) return new CellRange(a.row(), col + 1, a.row(), a.column() - 1);
        return null;
    }

    public static void insertNow(SheetEditor editor, boolean time) {
        String text = time ? LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")) : LocalDate.now().format(CellEditController.dateFormatter(editor.getConfig().locale()));
        if (editor.isEditing()) editor.editing().insertAtCaret(text); else editor.startEditing(text, true);
    }

    public static void clearAndEdit(SheetEditor editor) {
        CellAddress a = editor.getSelection().active();
        if (!editor.review().canEdit(editor.activeSheetIndex(), a)) { editor.review().warnProtected(); return; }
        editor.startEditing("", true);
    }

    public static void escape(SheetEditor editor) {
        if (editor.clipboard().source() != null) editor.clipboard().clearSource();
        editor.getCanvas().setHighlights(List.of());
        if (editor.objects().selectedObject() != null) editor.objects().selected(null);
    }

    public static void delete(SheetEditor editor) {
        if (editor.objects().selectedObject() != null) { editor.objects().deleteSelected(); return; }
        editor.structure().clear(dtm.stools.component.panels.editor.sheet.command.SheetOperations.ClearMode.CONTENTS);
    }

    public static boolean hasCellContent(SheetEditor editor) {
        SheetCell c = editor.activeSheet().cell(editor.getSelection().active());
        return c.hasContent();
    }
}
