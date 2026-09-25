package dtm.stools.component.panels.editor.sheet.store;

import dtm.stools.component.panels.editor.sheet.model.SheetCell;

@FunctionalInterface
public interface CellVisitor {
    void visit(int row, int column, SheetCell cell);
}
