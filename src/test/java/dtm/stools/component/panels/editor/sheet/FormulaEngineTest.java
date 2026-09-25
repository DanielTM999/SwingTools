package dtm.stools.component.panels.editor.sheet;

import dtm.stools.component.panels.editor.sheet.api.SheetSession;
import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.FormulaParser;
import dtm.stools.component.panels.editor.sheet.formula.FormulaPrinter;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class FormulaEngineTest {
    private static double num(CellValue v) {
        assertInstanceOf(NumberValue.class, v, () -> "Expected number but was " + v);
        return ((NumberValue) v).value();
    }

    @Test void parsesAndPrintsCanonicalAndLocalized() {
        String canonical = Formulas.toCanonical("=SOMA(A1:B2;'Minha Planilha'!$C$3)*1,5", FormulaLocale.PT_BR);
        assertEquals("SUM(A1:B2,'Minha Planilha'!$C$3)*1.5", canonical);
        assertEquals("SOMA(A1:B2;'Minha Planilha'!$C$3)*1,5", Formulas.toDisplay(canonical, FormulaLocale.PT_BR));
        assertEquals("PROCV(A1;B:C;2;FALSO)", Formulas.toDisplay(Formulas.toCanonical("=VLOOKUP(A1,B:C,2,FALSE)", FormulaLocale.EN), FormulaLocale.PT_BR));
        assertEquals("{1,2;3,4}", FormulaPrinter.canonical(FormulaParser.parse("{1,2;3,4}", FormulaLocale.EN)));
        assertEquals("{1\\2;3\\4}", new FormulaPrinter(FormulaLocale.PT_BR).print(FormulaParser.parse("{1\\2;3\\4}", FormulaLocale.PT_BR)));
        assertEquals("Table1[[#Data],[Qty]]", FormulaPrinter.canonical(FormulaParser.parse("Table1[[#Data],[Qty]]", FormulaLocale.EN)));
        assertEquals("R[-1]C", new FormulaPrinter(FormulaLocale.EN, new CellAddress(5, 2), true).print(FormulaParser.parse("C5", FormulaLocale.EN)));
    }

    @Test void arithmeticPrecedenceAndCoercion() {
        SheetTestSupport t = new SheetTestSupport();
        assertEquals(4, num(t.eval("=-2^2")));
        assertEquals(7, num(t.eval("=1+2*3")));
        assertEquals(0.05, num(t.eval("=5%")), 1e-12);
        assertEquals("ab1", t.eval("=\"a\"&\"b\"&1").display());
        assertEquals(CellValue.TRUE, t.eval("=0.1+0.2=0.3"));
        assertEquals(CellError.DIV0, ((ErrorValue) t.eval("=1/0")).error());
        assertEquals(3, num(t.eval("=\"1\"+2")));
        assertEquals(CellError.VALUE, ((ErrorValue) t.eval("=\"x\"+2")).error());
    }

    @Test void recalculatesDependentsAndDetectsCycles() {
        SheetTestSupport t = new SheetTestSupport();
        t.set("A1", 2).set("A2", "=A1*10").set("A3", "=SUM(A1:A2)").recalc();
        assertEquals(22, num(t.value("A3")));
        t.workbook.sheet(0).put(0, 0, SheetCell.of(CellValue.of(5)));
        t.engine.cellsChanged(0, java.util.List.of(new CellAddress(0, 0)));
        assertEquals(55, num(t.value("A3")));
        t.set("B1", "=B2+1").set("B2", "=B1+1").recalc();
        assertFalse(t.engine.circularReferences().isEmpty());
    }

    @Test void dynamicArraysSpillAndBlock() {
        SheetTestSupport t = new SheetTestSupport();
        t.set("A1", "=SEQUENCE(3,2)").recalc();
        assertEquals(6, num(t.value("B3")));
        assertTrue(t.engine.isSpilled(0, 2, 1));
        t.set("C1", "=SUM(A1#)").recalc();
        assertEquals(21, num(t.value("C1")));
        t.set("A2", "bloqueio").recalc();
        assertEquals(CellError.SPILL, ((ErrorValue) t.value("A1")).error());
    }

    @Test void lookupAndTextFunctions() {
        SheetTestSupport t = new SheetTestSupport();
        t.set("A1", "Maçã").set("B1", 3).set("A2", "Pera").set("B2", 5).set("A3", "Uva").set("B3", 7);
        assertEquals(5, num(t.eval("=VLOOKUP(\"pera\",A1:B3,2,FALSE)")));
        assertEquals(7, num(t.eval("=XLOOKUP(\"Uva\",A1:A3,B1:B3)")));
        assertEquals(2, num(t.eval("=MATCH(5,B1:B3,0)")));
        assertEquals(12, num(t.eval("=SUMIF(B1:B3,\">3\")")));
        assertEquals(3, num(t.eval("=COUNTIFS(B1:B3,\">=3\",A1:A3,\"*a*\")")));
        assertEquals(1, num(t.eval("=COUNTIFS(B1:B3,\">3\",A1:A3,\"P*\")")));
        assertEquals("Maçã, Pera, Uva", t.eval("=TEXTJOIN(\", \",TRUE,A1:A3)").display());
        assertEquals("b", t.eval("=TEXTAFTER(\"a-b\",\"-\")").display());
        assertEquals("1,234.50", t.eval("=TEXT(1234.5,\"#,##0.00\")").display());
        assertEquals(2, num(t.eval("=ROWS(FILTER(A1:B3,B1:B3>4))")));
        assertEquals("Uva", ((ArrayValue) ((dtm.stools.component.panels.editor.sheet.calc.CalcEngine) t.engine).evaluate(0, new CellAddress(10, 10), "SORT(A1:A3,1,-1)")).get(0, 0).display());
        assertEquals(12, num(t.eval("=LET(x,5,y,7,x+y)")));
        assertEquals(9, num(t.eval("=LAMBDA(a,a*a)(3)")));
        assertEquals(15, num(t.eval("=REDUCE(0,B1:B3,LAMBDA(acc,v,acc+v))")));
    }

    @Test void dateStatisticsAndFinance() {
        SheetTestSupport t = new SheetTestSupport();
        assertEquals(45658, num(t.eval("=DATE(2025,1,1)")));
        assertEquals(60, num(t.eval("=DATE(1900,2,29)")));
        assertEquals(2025, num(t.eval("=YEAR(45658)")));
        assertEquals(4, num(t.eval("=WEEKDAY(45658)")));
        assertEquals(23, num(t.eval("=NETWORKDAYS(DATE(2025,1,1),DATE(2025,1,31))")));
        assertEquals(-1110.61, num(t.eval("=PMT(0.01,12,-12500)")) * -1, 0.01);
        assertEquals(0.5, num(t.eval("=NORM.S.DIST(0,TRUE)")), 1e-12);
        assertEquals(1.959963985, num(t.eval("=NORM.S.INV(0.975)")), 1e-8);
        assertEquals(3, num(t.eval("=MEDIAN(1,3,5)")));
        assertEquals(1.5811388300841898, num(t.eval("=STDEV.S(1,2,3,4,5)")), 1e-12);
        assertEquals(0.1, num(t.eval("=IRR({-100,110})")), 1e-9);
        assertEquals("1010", t.eval("=DEC2BIN(10)").display());
        assertEquals(0.3048, num(t.eval("=CONVERT(1,\"ft\",\"m\")")), 1e-12);
    }

    @Test void googleQuery() {
        SheetTestSupport t = new SheetTestSupport();
        t.set("A1", "Nome").set("B1", "Setor").set("C1", "Valor");
        t.set("A2", "Ana").set("B2", "TI").set("C2", 10);
        t.set("A3", "Bia").set("B3", "RH").set("C3", 20);
        t.set("A4", "Caio").set("B4", "TI").set("C4", 30);
        t.set("E1", "=QUERY(A1:C4,\"select B, sum(C) group by B order by sum(C) desc\",1)").recalc();
        assertEquals("TI", t.value("E2").display());
        assertEquals(40, num(t.value("F2")));
    }

    @Test void portugueseInputAndSession() {
        SheetSession session = new SheetSession();
        CalcEngine engine = new CalcEngine(SheetTestSupport.REGISTRY);
        engine.setLocale(Locale.forLanguageTag("pt-BR"));
        engine.attach(session.getWorkbook());
        session.addChangeListener(engine::onChange);
        session.execute("Editar", tx -> {
            tx.setCell(0, 0, 0, SheetCell.of(CellValue.of(1.5)));
            tx.setCell(0, 1, 0, SheetCell.formula(Formulas.toCanonical("=SE(A1>1;\"alto\";\"baixo\")", FormulaLocale.PT_BR)));
        });
        assertEquals(new TextValue("alto"), engine.valueAt(0, 1, 0));
        session.undo();
        assertEquals(CellValue.EMPTY, engine.valueAt(0, 1, 0));
        session.redo();
        assertEquals(new TextValue("alto"), engine.valueAt(0, 1, 0));
        assertTrue(session.isDirty());
    }

    @Test void chainedRecalculationIsFast() {
        SheetTestSupport t = new SheetTestSupport();
        t.set("A1", 1);
        for (int r = 2; r <= 100_000; r++) t.workbook.sheet(0).put(r - 1, 0, SheetCell.formula("A" + (r - 1) + "+1"));
        long start = System.nanoTime();
        t.recalc();
        long ms = (System.nanoTime() - start) / 1_000_000;
        assertEquals(100_000, num(t.value("A100000")));
        assertTrue(ms < 5000, "Recalculation took " + ms + "ms");
    }

    static Locale pt() { return Locale.forLanguageTag("pt-BR"); }
}
