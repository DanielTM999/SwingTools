package dtm.stools.component.panels.editor.sheet;

import dtm.stools.component.panels.editor.sheet.api.SheetSession;
import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.command.SheetOperations;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.io.CsvCodec;
import dtm.stools.component.panels.editor.sheet.io.CsvDialect;
import dtm.stools.component.panels.editor.sheet.io.OdsCodec;
import dtm.stools.component.panels.editor.sheet.io.SheetHtmlExporter;
import dtm.stools.component.panels.editor.sheet.io.SheetImportResult;
import dtm.stools.component.panels.editor.sheet.io.XlsxCodec;
import dtm.stools.component.panels.editor.sheet.io.XlsxFormulas;
import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetPackage;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ChartSeries;
import dtm.stools.component.panels.editor.sheet.model.ChartType;
import dtm.stools.component.panels.editor.sheet.model.ConditionalFormat;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRule;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.DefinedName;
import dtm.stools.component.panels.editor.sheet.model.FreezePane;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetFill;
import dtm.stools.component.panels.editor.sheet.model.SheetNote;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class SheetIoTest {
    private static SheetWorkbook sample() {
        SheetWorkbook wb = SheetWorkbook.create("Vendas");
        SheetWorksheet ws = wb.sheet(0);
        int bold = wb.styles().intern(CellStyle.DEFAULT.withBold(true).withFill(SheetFill.solid(0xFFFFFF00)));
        int money = wb.styles().intern(CellStyle.DEFAULT.withNumberFormat("\"R$\" #,##0.00"));
        ws.put(0, 0, new SheetCell(CellValue.of("Produto"), null, bold));
        ws.put(0, 1, new SheetCell(CellValue.of("Valor"), null, bold));
        ws.put(1, 0, SheetCell.of(CellValue.of("Maçã")));
        ws.put(1, 1, new SheetCell(CellValue.of(10.5), null, money));
        ws.put(2, 0, SheetCell.of(CellValue.of("Pera")));
        ws.put(2, 1, new SheetCell(CellValue.of(20), null, money));
        ws.put(3, 1, SheetCell.formula("SUM(B2:B3)"));
        ws.put(0, 3, SheetCell.formula("SEQUENCE(3)"));
        ws.put(5, 0, SheetCell.formula("XLOOKUP(\"Pera\",A2:A3,B2:B3)"));
        ws.put(6, 0, SheetCell.formula("LET(x,2,x*3)"));
        ws.columns().setSize(0, 150);
        ws.rows().setSize(0, 30);
        ws.setProperties(ws.properties().withFreeze(new FreezePane(1, 0)).addMerge(new CellRange(8, 0, 8, 2))
                .addValidation(DataValidation.list(CellRange.of(1, 2), "\"A,B,C\""))
                .addConditionalFormat(ConditionalFormat.of(new CellRange(1, 1, 2, 1), ConditionalRule.twoColorScale(0xFFF8696B, 0xFF63BE7B)))
                .withNote(new CellAddress(1, 0), new SheetNote("Ana", "Fruta vermelha", false))
                .addTable(SheetTable.create(1, "Tabela1", new CellRange(0, 0, 2, 1), List.of("Produto", "Valor")))
                .addObject(SheetChart.builder().type(ChartType.COLUMN).anchor(new ObjectAnchor(10, 0, 0, 0, 400, 240)).title("Vendas")
                        .series(List.of(ChartSeries.of("Valor", "A2:A3", "B2:B3"))).build()));
        wb.setProperties(wb.properties().withNames(List.of(DefinedName.of("Total", "Vendas!$B$4"))));
        return wb;
    }

    @Test void xlsxRoundTrip() throws Exception {
        SheetWorkbook wb = sample();
        CalcEngine engine = new CalcEngine(SheetTestSupport.REGISTRY);
        engine.setLocale(Locale.US);
        engine.attach(wb);
        byte[] bytes = new XlsxCodec().toBytes(wb, engine);
        SheetPackage pkg = SheetPackage.read(bytes, SheetPackage.Limits.DEFAULT);
        assertTrue(pkg.contains("xl/metadata.xml"));
        assertTrue(new String(pkg.part("xl/worksheets/sheet1.xml"), StandardCharsets.UTF_8).contains("_xlfn.XLOOKUP"));
        assertTrue(new String(pkg.part("xl/worksheets/sheet1.xml"), StandardCharsets.UTF_8).contains("_xlpm.x"));
        SheetImportResult read = new XlsxCodec().read(bytes);
        SheetWorkbook back = read.workbook();
        SheetWorksheet ws = back.sheet(0);
        assertEquals("Vendas", ws.name());
        assertEquals("SUM(B2:B3)", ws.cell(3, 1).formula());
        assertEquals("XLOOKUP(\"Pera\",A2:A3,B2:B3)", ws.cell(5, 0).formula());
        assertEquals("LET(x,2,x*3)", ws.cell(6, 0).formula());
        assertEquals(30.5, ((NumberValue) ws.cell(3, 1).value()).value(), 1e-9);
        assertTrue(back.style(ws.cell(0, 0).style()).bold());
        assertEquals("\"R$\" #,##0.00", back.style(ws.cell(1, 1).style()).numberFormat());
        assertEquals(150, ws.columns().size(0), 2);
        assertEquals(30, ws.rows().size(0), 1);
        assertEquals(1, ws.properties().freeze().rows());
        assertEquals(1, ws.properties().merges().size());
        assertEquals(1, ws.properties().validations().size());
        assertEquals(1, ws.properties().conditionalFormats().size());
        assertEquals("Fruta vermelha", ws.properties().notes().get(new CellAddress(1, 0)).text());
        assertEquals("Tabela1", ws.properties().tables().getFirst().name());
        assertEquals(1, ws.properties().objects().size());
        assertEquals("Total", back.properties().names().getFirst().name());
        CalcEngine e2 = new CalcEngine(SheetTestSupport.REGISTRY);
        e2.attach(back);
        assertEquals(20, ((NumberValue) e2.valueAt(0, 5, 0)).value());
        assertEquals(3, ((NumberValue) e2.valueAt(0, 2, 3)).value());
    }

    @Test void csvAndOdsRoundTrip() throws Exception {
        CsvCodec csv = new CsvCodec();
        byte[] data = "Nome;Valor;Data\nAna;1.234,5;01/02/2025\n\"Bia; Silva\";2;\n".getBytes(StandardCharsets.UTF_8);
        CsvDialect d = csv.detect(data, Locale.forLanguageTag("pt-BR"));
        assertEquals(';', d.delimiter());
        SheetWorkbook wb = csv.read(data, d, "Dados", false).workbook();
        assertEquals(1234.5, ((NumberValue) wb.sheet(0).cell(1, 1).value()).value(), 1e-9);
        assertEquals("Bia; Silva", wb.sheet(0).cell(2, 0).value().display());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        csv.write(wb, 0, null, d, out);
        assertTrue(out.toString(StandardCharsets.UTF_8).contains("\"Bia; Silva\""));

        SheetWorkbook sample = sample();
        ByteArrayOutputStream ods = new ByteArrayOutputStream();
        new OdsCodec().write(sample, null, ods);
        SheetWorkbook back = new OdsCodec().read(ods.toByteArray()).workbook();
        assertEquals("SUM(B2:B3)", back.sheet(0).cell(3, 1).formula());
        assertEquals("Maçã", back.sheet(0).cell(1, 0).value().display());
        assertEquals(1, back.sheet(0).properties().merges().size());
        String html = new SheetHtmlExporter().html(sample, null, new NumberFormatter(), 0, null, false);
        assertTrue(html.contains("R$ 10,50"));
    }

    @Test void futureFunctionsArePrefixed() {
        assertEquals("_xlfn._xlws.FILTER(A1:A3,A1:A3>1)", XlsxFormulas.toFile("FILTER(A1:A3,A1:A3>1)"));
        assertEquals("FILTER(A1:A3,A1:A3>1)", XlsxFormulas.fromFile("_xlfn._xlws.FILTER(A1:A3,A1:A3>1)"));
        assertEquals("_xlfn.LAMBDA(_xlpm.a,_xlpm.a+1)(2)", XlsxFormulas.toFile("LAMBDA(a,a+1)(2)"));
    }

    @Test void structuralEditsAdjustReferences() {
        SheetSession session = new SheetSession();
        SheetWorkbook wb = sample();
        session.load(wb);
        session.execute("Inserir linhas", tx -> SheetOperations.insertRows(tx, 0, 1, 2));
        SheetWorksheet ws = session.getWorkbook().sheet(0);
        assertEquals("SUM(B4:B5)", ws.cell(5, 1).formula());
        assertEquals("Vendas!$B$6", session.getWorkbook().properties().names().getFirst().formula());
        assertEquals(new CellRange(0, 0, 4, 1), ws.properties().tables().getFirst().range());
        session.undo();
        assertEquals("SUM(B2:B3)", session.getWorkbook().sheet(0).cell(3, 1).formula());
        session.execute("Excluir coluna", tx -> SheetOperations.deleteColumns(tx, 0, 0, 1));
        assertEquals("SUM(A2:A3)", session.getWorkbook().sheet(0).cell(3, 0).formula());
        session.execute("Renomear", tx -> SheetOperations.renameSheet(tx, 0, "Receita"));
        assertEquals("Receita!$A$4", session.getWorkbook().properties().names().getFirst().formula());
    }
}
