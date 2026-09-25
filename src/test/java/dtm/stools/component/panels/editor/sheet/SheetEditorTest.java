package dtm.stools.component.panels.editor.sheet;

import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.controller.PasteOptions;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.FilterCriteria;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.ui.RibbonGroup;
import dtm.stools.component.panels.editor.sheet.ui.RibbonItem;
import dtm.stools.component.panels.editor.sheet.ui.RibbonTab;
import dtm.stools.component.panels.editor.sheet.ui.SheetRibbon;
import dtm.stools.examples.SheetEditorExample;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.Action;
import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class SheetEditorTest {
    private SheetEditor editor;

    static <T> T edt(Callable<T> callable) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { result.set(callable.call()); } catch (Throwable e) { failure.set(e); }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
        return result.get();
    }

    static void run(Runnable r) throws Exception { edt(() -> { r.run(); return null; }); }

    @BeforeEach
    void setUp() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        editor = edt(() -> {
            SheetEditor e = new SheetEditor();
            e.setErrorHandler(error -> { throw new AssertionError(error); });
            e.setSize(1200, 800);
            return e;
        });
    }

    @AfterEach
    void tearDown() throws Exception {
        if (editor != null) run(editor::close);
    }

    private double number(String a1) throws Exception {
        CellValue v = edt(() -> editor.getValue(a1));
        assertInstanceOf(NumberValue.class, v, a1 + " = " + v);
        return ((NumberValue) v).value();
    }

    @Test
    void typesValuesAndPortugueseFormulasWithUndo() throws Exception {
        run(() -> {
            editor.input("A1", "10");
            editor.input("A2", "1.234,5");
            editor.input("A3", "=SOMA(A1:A2)");
            editor.input("B1", "25%");
            editor.input("B2", "24/09/2026");
        });
        assertEquals(1244.5, number("A3"), 1e-9);
        assertEquals(0.25, number("B1"), 1e-9);
        assertEquals("=SOMA(A1:A2)", edt(() -> editor.getFormula("A3")));
        assertEquals("0%", edt(() -> editor.getWorkbook().style(editor.activeSheet().cell(CellAddress.parse("B1")).style()).numberFormat()));
        assertEquals("24/09/2026", edt(() -> editor.getText("B2")));
        run(editor::undo);
        assertTrue(edt(() -> editor.getValue("B2").isEmpty()));
        run(editor::redo);
        assertFalse(edt(() -> editor.getValue("B2").isEmpty()));
        run(() -> editor.setFormula("C1", "=A1*2"));
        assertEquals(20, number("C1"), 1e-9);
    }

    @Test
    void everyRibbonCommandIsRegistered() throws Exception {
        List<String> missing = edt(() -> {
            List<String> out = new ArrayList<>();
            Map<String, Action> commands = editor.getCommands();
            for (RibbonTab tab : editor.getDefaultRibbon().tabs())
                for (RibbonGroup g : tab.groups())
                    for (RibbonItem item : g.items()) if (item.command() != null && !commands.containsKey(item.command())) out.add(item.command());
            for (Map.Entry<String, Action> e : commands.entrySet())
                if (e.getValue().getValue(SheetRibbon.MENU) instanceof List<?> ids)
                    for (Object id : ids) if (!"-".equals(id) && !commands.containsKey(String.valueOf(id))) out.add(e.getKey() + " -> " + id);
            return out;
        });
        assertTrue(missing.isEmpty(), "Comandos ausentes: " + missing);
        assertTrue(edt(() -> editor.getCommands().size()) > 250);
    }

    @Test
    void structuralEditsAdjustFormulas() throws Exception {
        run(() -> {
            editor.setValue("A1", 5);
            editor.setFormula("B1", "=A1*2");
            editor.select(SheetSelection.of(CellRange.columns(0, 0)));
            editor.structure().insertColumns();
        });
        assertEquals("=B1*2", edt(() -> editor.getFormula("C1")));
        assertEquals(10, number("C1"), 1e-9);
        run(() -> {
            editor.select(SheetSelection.of(CellRange.rows(0, 0)));
            editor.structure().insertRows();
        });
        assertEquals("=B2*2", edt(() -> editor.getFormula("C2")));
    }

    @Test
    void internalPasteShiftsRelativeReferences() throws Exception {
        run(() -> {
            editor.setValue("A1", 5);
            editor.setValue("A2", 6);
            editor.setFormula("B1", "=A1+1");
            editor.select(SheetSelection.of(CellAddress.parse("B1")));
            editor.clipboard().copy();
            editor.select(SheetSelection.of(CellAddress.parse("B2")));
            editor.clipboard().paste();
        });
        assertEquals("=A2+1", edt(() -> editor.getFormula("B2")));
        assertEquals(7, number("B2"), 1e-9);
        run(() -> {
            editor.select(SheetSelection.of(CellAddress.parse("B2")));
            editor.clipboard().copy();
            editor.select(SheetSelection.of(CellAddress.parse("C3")));
            editor.clipboard().paste(PasteOptions.of(PasteOptions.What.VALUES));
        });
        assertEquals(7, number("C3"), 1e-9);
        assertEquals(null, edt(() -> editor.getFormula("C3")));
    }

    @Test
    void sortFillAndFilter() throws Exception {
        run(() -> {
            editor.input("A1", "Nome");
            editor.input("B1", "Valor");
            String[] names = {"Carla", "Ana", "Bruno"};
            int[] values = {30, 10, 20};
            for (int i = 0; i < 3; i++) { editor.input("A" + (i + 2), names[i]); editor.setValue("B" + (i + 2), values[i]); }
            editor.getSession().getWorkbook().sheet(0).setProperties(editor.activeSheet().properties());
            editor.select(SheetSelection.of(CellAddress.parse("B2")));
            editor.data().sort(true);
        });
        assertEquals("Carla", ((TextValue) edt(() -> editor.getValue("A2"))).value());
        assertEquals(10, number("B4"), 1e-9);
        run(() -> {
            editor.select(SheetSelection.of(CellAddress.parse("A1")));
            editor.data().toggleFilter();
            editor.data().setColumnFilter(null, 0, FilterCriteria.values(Set.of("Ana"), false));
        });
        assertTrue(edt(() -> editor.activeSheet().rows().isHidden(1)));
        assertFalse(edt(() -> editor.activeSheet().rows().isHidden(3)));
        assertEquals("1 de 3 registros encontrados", edt(() -> editor.data().filterStatus()));
        run(() -> {
            editor.setValue("D1", 1);
            editor.setValue("D2", 2);
            editor.fill(CellRange.parse("D1:D2"), CellRange.parse("D1:D6"), false);
        });
        assertEquals(6, number("D6"), 1e-9);
    }

    @Test
    void tablesValidationConditionalAndCheckbox() throws Exception {
        run(() -> {
            editor.input("A1", "Item");
            editor.input("B1", "Qtd");
            editor.input("A2", "Caneta");
            editor.setValue("B2", 3);
            editor.select(SheetSelection.of(CellAddress.parse("A1")));
            editor.data().createTable(null);
            editor.input("A3", "Lápis");
        });
        assertEquals(1, (int) edt(() -> editor.activeSheet().properties().tables().size()));
        assertEquals(CellRange.parse("A1:B3"), edt(() -> editor.activeSheet().properties().tables().getFirst().range()));
        run(() -> {
            editor.select(SheetSelection.of(CellAddress.parse("D1")));
            editor.data().insertCheckbox();
            editor.toggleCheckbox(CellAddress.parse("D1"));
        });
        assertEquals(CellValue.TRUE, edt(() -> editor.getValue("D1")));
        assertTrue(edt(() -> editor.isCheckbox(CellAddress.parse("D1"))));
    }

    @Test
    void saveReopenExportAndPrintLayout(@TempDir Path dir) throws Exception {
        run(() -> editor.load(SheetEditorExample.demoWorkbook()));
        run(() -> editor.objects().refreshAllPivots());
        double total = number("F2");
        Path xlsx = dir.resolve("demo.xlsx");
        Path saved = edt(() -> editor.save(xlsx)).future().get(30, TimeUnit.SECONDS);
        assertEquals(xlsx, saved);
        assertFalse(edt(editor::isDirty));
        run(() -> editor.newWorkbook());
        Path opened = edt(() -> editor.open(xlsx, true)).future().get(30, TimeUnit.SECONDS);
        assertEquals(xlsx, opened);
        assertEquals(total, number("F2"), 1e-9);
        assertEquals(3, (int) edt(() -> editor.getWorkbook().sheetCount()));
        Path pdf = dir.resolve("demo.pdf");
        edt(() -> editor.export(pdf, SheetEditor.ExportFormat.PDF)).future().get(60, TimeUnit.SECONDS);
        byte[] bytes = Files.readAllBytes(pdf);
        assertEquals("%PDF", new String(bytes, 0, 4));
        Path html = dir.resolve("demo.html");
        edt(() -> editor.export(html, SheetEditor.ExportFormat.HTML)).future().get(30, TimeUnit.SECONDS);
        assertTrue(Files.readString(html).contains("Notebook"));
        Path csv = dir.resolve("demo.csv");
        edt(() -> editor.export(csv, SheetEditor.ExportFormat.CSV)).future().get(30, TimeUnit.SECONDS);
        assertTrue(Files.size(csv) > 50);
        assertNotNull(edt(() -> editor.getCommands().get("sheet.file.print")));
    }

    @Test
    void pivotAndDynamicArraysFromDemo() throws Exception {
        run(() -> { editor.load(SheetEditorExample.demoWorkbook()); editor.objects().refreshAllPivots(); });
        CellValue label = edt(() -> editor.getEngine().valueAt(2, 3, 0));
        assertFalse(label.isEmpty(), "pivot deve escrever as linhas");
        CellValue seq = edt(() -> editor.getEngine().valueAt(1, 4, 0));
        assertEquals(40, ((NumberValue) seq).value(), 1e-9);
        CellValue unique = edt(() -> editor.getEngine().valueAt(1, 1, 2));
        assertEquals("Centro-Oeste", ((TextValue) unique).value());
    }
}
