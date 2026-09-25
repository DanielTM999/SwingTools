package dtm.stools.component.panels.editor.sheet.model;

import java.util.List;

public record SortSpec(CellRange range, List<SortKey> keys, boolean hasHeader, boolean byColumns, boolean caseSensitive) {
    public SortSpec { keys = List.copyOf(keys); }

    public static SortSpec of(CellRange range, boolean hasHeader, SortKey... keys) { return new SortSpec(range, List.of(keys), hasHeader, false, false); }
}
