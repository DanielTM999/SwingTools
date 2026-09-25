package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;

import java.util.ArrayList;
import java.util.List;

public class SheetTextExporter {
    public List<List<String>> grid(SheetWorkbook wb, CalcEngine engine, NumberFormatter formatter, int sheet, CellRange range) {
        SheetWorksheet ws = wb.sheet(sheet);
        List<List<String>> out = new ArrayList<>();
        if (range == null) return out;
        for (int r = range.firstRow(); r <= range.lastRow(); r++) {
            List<String> row = new ArrayList<>();
            for (int c = range.firstColumn(); c <= range.lastColumn(); c++) {
                var v = engine != null ? engine.valueAt(sheet, r, c) : ws.cell(r, c).value();
                row.add(formatter.text(v, wb.style(ws.cell(r, c).style()).numberFormat()));
            }
            out.add(row);
        }
        return out;
    }

    public String text(SheetWorkbook wb, CalcEngine engine, NumberFormatter formatter, int sheet, CellRange range) {
        return CsvCodec.tsv(grid(wb, engine, formatter, sheet, range));
    }
}
