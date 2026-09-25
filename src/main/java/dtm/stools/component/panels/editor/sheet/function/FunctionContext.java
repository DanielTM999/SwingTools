package dtm.stools.component.panels.editor.sheet.function;

import dtm.stools.component.panels.editor.sheet.calc.LambdaValue;
import dtm.stools.component.panels.editor.sheet.calc.ReferenceValue;
import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.formula.FormulaNode;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.provider.SheetExternalDataProvider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FunctionContext {
    CellValue evaluate(FormulaNode node);
    CellValue evaluate(FormulaNode node, Map<String, CellValue> bindings);
    CellValue deref(CellValue value);
    CellValue scalar(CellValue value);
    ArrayValue toArray(CellValue value);
    CellValue cell(int sheet, int row, int column);
    void forEachCell(ReferenceValue reference, CellConsumer consumer);
    CellRange usedRange(int sheet);
    CellRange clip(int sheet, CellRange range);
    int hostSheet();
    CellAddress host();
    SheetWorkbook workbook();
    boolean date1904();
    FormulaLocale formulaLocale();
    java.util.Locale locale();
    FunctionRegistry functions();
    double random();
    long now();
    CellValue callLambda(LambdaValue lambda, List<CellValue> args);
    ReferenceValue indirect(String text, boolean a1);
    Optional<String> formulaAt(int sheet, int row, int column);
    boolean isHiddenRow(int sheet, int row);
    boolean isSubtotalCell(int sheet, int row, int column);
    String formatNumber(double value, String format);
    String numberFormatAt(int sheet, int row, int column);
    SheetExternalDataProvider external();
    Map<String, CellValue> bindings();
    int sheetIndex(String name);
    String sheetName(int index);
}
