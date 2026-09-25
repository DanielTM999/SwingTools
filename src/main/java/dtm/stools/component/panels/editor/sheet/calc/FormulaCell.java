package dtm.stools.component.panels.editor.sheet.calc;

import dtm.stools.component.panels.editor.sheet.formula.FormulaNode;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;

import java.util.List;

public final class FormulaCell {
    static final int CLEAN = 0, DIRTY = 1, VISITING = 2;

    final int sheet, row, column;
    final String text;
    final FormulaNode ast;
    final CellValue parseError;
    List<Dependency> dependencies = List.of();
    boolean volatileCell;
    CellValue value = CellValue.EMPTY;
    CellRange spill;
    CellRange desiredSpill;
    int state;
    int visit;

    FormulaCell(int sheet, int row, int column, String text, FormulaNode ast, CellValue parseError) {
        this.sheet = sheet; this.row = row; this.column = column; this.text = text; this.ast = ast; this.parseError = parseError;
    }

    long key() { return CellAddress.key(row, column); }
    public CellValue value() { return value; }
    public CellRange spillRange() { return spill; }
    public String text() { return text; }
    public int sheet() { return sheet; }
    public CellAddress address() { return new CellAddress(row, column); }
    public List<Dependency> dependencies() { return dependencies; }
    public boolean isVolatile() { return volatileCell; }
    @Override public String toString() { return sheet + "!" + address() + "=" + text; }
}
