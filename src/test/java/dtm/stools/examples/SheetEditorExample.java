package dtm.stools.examples;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.calc.Coerce;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.io.SheetFileRecoveryStore;
import dtm.stools.component.panels.editor.sheet.model.BorderStyle;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ChartSeries;
import dtm.stools.component.panels.editor.sheet.model.ChartType;
import dtm.stools.component.panels.editor.sheet.model.ComparisonOperator;
import dtm.stools.component.panels.editor.sheet.model.ConditionalFormat;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRule;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;
import dtm.stools.component.panels.editor.sheet.model.FreezePane;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.IconSetType;
import dtm.stools.component.panels.editor.sheet.model.LegendPosition;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.PivotAggregation;
import dtm.stools.component.panels.editor.sheet.model.PivotField;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.PivotValueField;
import dtm.stools.component.panels.editor.sheet.model.SheetBorder;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetFill;
import dtm.stools.component.panels.editor.sheet.model.SheetNote;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.Sparkline;
import dtm.stools.component.panels.editor.sheet.model.SparklineType;
import dtm.stools.component.panels.editor.sheet.provider.SheetFunctionProvider;
import dtm.stools.component.panels.editor.sheet.function.FunctionCategory;
import dtm.stools.component.panels.editor.sheet.function.FunctionDefinition;
import dtm.stools.component.panels.editor.sheet.function.SheetFunction;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;

public class SheetEditorExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            JFrame frame = new JFrame("SwingTools • Planilha");
            SheetEditor editor = new SheetEditor();
            editor.setErrorHandler(error -> JOptionPane.showMessageDialog(frame, error.getMessage() == null ? error.toString() : error.getMessage(), "Planilha", JOptionPane.WARNING_MESSAGE));
            editor.load(demoWorkbook());
            editor.objects().refreshAllPivots();
            editor.getSession().markSaved();
            editor.addProvider(new SheetFileRecoveryStore(Path.of(System.getProperty("java.io.tmpdir"), "swingtools-sheet-recovery")));
            editor.addProvider(new SheetFunctionProvider() {
                @Override public String id() { return "example.functions"; }
                @Override public List<SheetFunction> functions() { return List.of(ExampleFunctions.discount()); }
            });
            frame.add(editor, BorderLayout.CENTER);
            frame.setSize(1400, 900);
            frame.setLocationRelativeTo(null);
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() { @Override public void windowClosed(WindowEvent e) { editor.close(); } });
            frame.setVisible(true);
        });
    }

    private static String f(String localized) { return Formulas.toCanonical(localized, FormulaLocale.PT_BR); }

    private static void put(SheetWorkbook wb, SheetWorksheet ws, String a1, Object value, CellStyle style) {
        CellAddress a = CellAddress.parse(a1);
        int s = style == null ? 0 : wb.styles().intern(style);
        SheetCell cell;
        if (value instanceof String t && t.startsWith("=")) cell = SheetCell.formula(f(t));
        else if (value instanceof Number n) cell = SheetCell.of(CellValue.of(n.doubleValue()));
        else cell = SheetCell.of(CellValue.of(String.valueOf(value)));
        ws.put(a.row(), a.column(), cell.withStyle(s));
    }

    public static SheetWorkbook demoWorkbook() {
        SheetWorkbook wb = SheetWorkbook.create("Vendas");
        SheetWorksheet ws = wb.sheet(0);
        CellStyle header = CellStyle.DEFAULT.withBold(true).withFontColor(0xFFFFFFFF).withFill(SheetFill.solid(0xFF156082)).withHorizontal(HorizontalAlignment.CENTER);
        CellStyle money = CellStyle.DEFAULT.withNumberFormat("\"R$\" #,##0.00");
        CellStyle total = money.withBold(true).withTop(SheetBorder.of(BorderStyle.THIN)).withBottom(SheetBorder.of(BorderStyle.DOUBLE));
        String[] headers = {"Região", "Produto", "Jan", "Fev", "Mar", "Total", "Meta", "Status", "Tendência"};
        for (int c = 0; c < headers.length; c++) put(wb, ws, CellAddress.columnName(c) + "1", headers[c], header);
        Object[][] rows = {
                {"Norte", "Notebook", 12500, 13800, 15100, 40000},
                {"Nordeste", "Monitor", 8400, 7900, 9100, 27000},
                {"Sul", "Teclado", 3100, 3350, 2980, 9000},
                {"Sudeste", "Notebook", 25400, 27100, 26800, 75000},
                {"Centro-Oeste", "Mouse", 1900, 2250, 2600, 6000},
                {"Sul", "Monitor", 9800, 10250, 11200, 30000},
                {"Norte", "Headset", 4100, 3800, 4550, 13000},
                {"Sudeste", "Webcam", 5200, 6100, 5900, 18000}};
        for (int i = 0; i < rows.length; i++) {
            int r = i + 2;
            put(wb, ws, "A" + r, rows[i][0], null);
            put(wb, ws, "B" + r, rows[i][1], null);
            put(wb, ws, "C" + r, rows[i][2], money);
            put(wb, ws, "D" + r, rows[i][3], money);
            put(wb, ws, "E" + r, rows[i][4], money);
            put(wb, ws, "F" + r, "=SOMA(C" + r + ":E" + r + ")", money);
            put(wb, ws, "G" + r, rows[i][5], money);
            put(wb, ws, "H" + r, "=SE(F" + r + ">=G" + r + ";\"Atingida\";\"Abaixo\")", CellStyle.DEFAULT.withHorizontal(HorizontalAlignment.CENTER));
        }
        put(wb, ws, "A11", "Total", CellStyle.DEFAULT.withBold(true));
        for (String c : new String[]{"C", "D", "E", "F", "G"}) put(wb, ws, c + "11", "=SUBTOTAL(9;" + c + "2:" + c + "9)", total);
        put(wb, ws, "A13", "Maior venda", CellStyle.DEFAULT.withItalic(true));
        put(wb, ws, "B13", "=ÍNDICE(B2:B9;CORRESP(MÁXIMO(F2:F9);F2:F9;0))", null);
        put(wb, ws, "A14", "Média mensal", CellStyle.DEFAULT.withItalic(true));
        put(wb, ws, "B14", "=MÉDIA(C2:E9)", money);
        put(wb, ws, "A15", "Norte (PROCX)", CellStyle.DEFAULT.withItalic(true));
        put(wb, ws, "B15", "=PROCX(\"Norte\";A2:A9;F2:F9)", money);
        int[] widths = {110, 100, 112, 112, 132, 120, 112, 90, 110};
        for (int c = 0; c < widths.length; c++) ws.columns().setSize(c, widths[c]);
        SheetTable table = SheetTable.create(1, "TabelaVendas", new CellRange(0, 0, 8, 7), List.of("Região", "Produto", "Jan", "Fev", "Mar", "Total", "Meta", "Status")).withStyle("TableStyleMedium2");
        List<Sparkline> sparks = new java.util.ArrayList<>();
        for (int r = 1; r <= 8; r++) sparks.add(new Sparkline(new CellAddress(r, 8), "Vendas!C" + (r + 1) + ":E" + (r + 1), SparklineType.LINE, 0xFF156082, true, true, true, false));
        SheetChart chart = SheetChart.builder().id("grafico-vendas").type(ChartType.COLUMN).anchor(new ObjectAnchor(1, 10, 8, 6, 520, 300)).title("Vendas por trimestre")
                .series(List.of(new ChartSeries("Jan", "Vendas!$C$1", "Vendas!$B$2:$B$9", "Vendas!$C$2:$C$9", null, null, null, false),
                        new ChartSeries("Fev", "Vendas!$D$1", "Vendas!$B$2:$B$9", "Vendas!$D$2:$D$9", null, null, null, false),
                        new ChartSeries("Mar", "Vendas!$E$1", "Vendas!$B$2:$B$9", "Vendas!$E$2:$E$9", null, null, null, false)))
                .legend(LegendPosition.BOTTOM).xAxisTitle("").yAxisTitle("").dataLabels(false).gridlines(true).styleIndex(0).build();
        ws.setProperties(ws.properties().addTable(table).withFreeze(new FreezePane(1, 0))
                .withConditionalFormats(List.of(
                        new ConditionalFormat(List.of(CellRange.parse("F2:F9")), List.of(ConditionalRule.dataBar(0xFF63C384, true).withPriority(1))),
                        new ConditionalFormat(List.of(CellRange.parse("H2:H9")), List.of(ConditionalRule.cellValue(ComparisonOperator.EQUAL, "\"Abaixo\"", null, DifferentialStyle.LIGHT_RED).withPriority(2))),
                        new ConditionalFormat(List.of(CellRange.parse("E2:E9")), List.of(ConditionalRule.iconSet(IconSetType.ARROWS_3).withPriority(3)))))
                .withValidations(List.of(DataValidation.list(CellRange.parse("A2:A20"), "\"Norte,Nordeste,Sul,Sudeste,Centro-Oeste\"")))
                .withSparklines(sparks)
                .addObject(chart)
                .withNote(CellAddress.parse("G1"), new SheetNote("SwingTools", "Meta trimestral definida pela diretoria.", false)));
        SheetWorksheet analysis = new SheetWorksheet("Análise");
        wb.addSheet(1, analysis);
        CellStyle bold = CellStyle.DEFAULT.withBold(true);
        put(wb, analysis, "A1", "SEQUÊNCIA", bold);
        put(wb, analysis, "A2", "=SEQUÊNCIA(5;1;10;10)", null);
        put(wb, analysis, "C1", "Regiões únicas", bold);
        put(wb, analysis, "C2", "=CLASSIFICAR(ÚNICO(Vendas!A2:A9))", null);
        put(wb, analysis, "E1", "Acima da meta", bold);
        put(wb, analysis, "E2", "=FILTRO(Vendas!B2:B9;Vendas!F2:F9>=Vendas!G2:G9;\"Nenhum\")", null);
        put(wb, analysis, "G1", "LET / LAMBDA", bold);
        put(wb, analysis, "G2", "=LET(total;SOMA(Vendas!F2:F9);total/1000)", null);
        put(wb, analysis, "G3", "=MAPA(SEQUÊNCIA(3);LAMBDA(x;x*x))", null);
        put(wb, analysis, "I1", "Datas", bold);
        put(wb, analysis, "I2", "=HOJE()", CellStyle.DEFAULT.withNumberFormat("dd/mm/yyyy"));
        put(wb, analysis, "I3", "=DIATRABALHO(I2;10)", CellStyle.DEFAULT.withNumberFormat("dddd, d \"de\" mmmm"));
        put(wb, analysis, "I4", "=TEXTO(I2;\"mmmm/aaaa\")", null);
        put(wb, analysis, "K1", "Financeiro", bold);
        put(wb, analysis, "K2", "=PGTO(1,5%;36;-50000)", CellStyle.DEFAULT.withNumberFormat("\"R$\" #,##0.00"));
        put(wb, analysis, "K3", "=VPL(10%;-1000;300;400;500)", CellStyle.DEFAULT.withNumberFormat("\"R$\" #,##0.00"));
        put(wb, analysis, "A9", "QUERY (Google)", bold);
        put(wb, analysis, "A10", "=QUERY(Vendas!A1:F9;\"select A, sum(F) group by A order by sum(F) desc\";1)", null);
        for (int c = 0; c < 12; c++) analysis.columns().setSize(c, 110);
        analysis.columns().setSize(8, 190);
        SheetWorksheet pivotSheet = new SheetWorksheet("Dinâmica");
        wb.addSheet(2, pivotSheet);
        PivotTable pivot = PivotTable.create("TabelaDinâmica1", 0, CellRange.parse("A1:F9"), new CellAddress(2, 0)).toBuilder()
                .rows(List.of(PivotField.of("Região"))).columns(List.of()).values(List.of(PivotValueField.of("Total", PivotAggregation.SUM), PivotValueField.of("Jan", PivotAggregation.AVERAGE))).build();
        pivotSheet.setProperties(pivotSheet.properties().addPivot(pivot));
        put(wb, pivotSheet, "A1", "Resumo por região", CellStyle.DEFAULT.withBold(true).withFontSize(14));
        pivotSheet.columns().setSize(0, 140);
        pivotSheet.rows().setSize(0, 28);
        pivotSheet.columns().setSize(1, 140);
        pivotSheet.columns().setSize(2, 140);
        return wb;
    }

    static final class ExampleFunctions {
        private ExampleFunctions() {}

        static SheetFunction discount() {
            return FunctionDefinition.scalar("DESCONTO", FunctionCategory.CUSTOM, 2, 2, (ctx, args) -> CellValue.of(Coerce.number(args[0]) * (1 - Coerce.number(args[1]))))
                    .describe("Aplica um percentual de desconto a um valor.", "valor", "percentual").build();
        }
    }
}
