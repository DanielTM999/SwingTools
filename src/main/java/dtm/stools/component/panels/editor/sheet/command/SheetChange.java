package dtm.stools.component.panels.editor.sheet.command;

import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.List;
import java.util.Map;

public record SheetChange(long id, String label, List<SheetEdit> edits, int sheetBefore, SheetSelection selectionBefore, int sheetAfter,
                          SheetSelection selectionAfter, Map<String, List<CellRange>> touched, boolean structural) {
    public SheetChange { edits = List.copyOf(edits); touched = Map.copyOf(touched); }
}
