package dtm.stools.component.panels.editor.sheet.api;

import dtm.stools.component.panels.editor.sheet.model.CellRange;

import java.util.List;
import java.util.Map;

public record SheetSessionEvent(Kind kind, long revision, String label, Map<String, List<CellRange>> touched, boolean structural) {
    public enum Kind { CONTENT, SELECTION, SHEET, STATE, LOAD, VALUES }

    public SheetSessionEvent { touched = touched == null ? Map.of() : Map.copyOf(touched); }
}
