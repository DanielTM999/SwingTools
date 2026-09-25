package dtm.stools.component.panels.editor.sheet.provider;

import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.List;
import java.util.Map;

public record SheetCollaborationEvent(long id, String label, String author, Map<String, List<CellRange>> ranges, boolean structural) {
    public SheetCollaborationEvent { ranges = Map.copyOf(ranges); }
}
