package dtm.stools.component.panels.editor.sheet.model;

import java.util.List;
import java.util.Objects;

public record ProtectedRange(String name, List<CellRange> ranges, List<String> editors, String passwordHash, String description) {
    public ProtectedRange {
        Objects.requireNonNull(name);
        ranges = List.copyOf(ranges);
        editors = editors == null ? List.of() : List.copyOf(editors);
        description = Objects.requireNonNullElse(description, "");
    }

    public boolean covers(int row, int column) { for (CellRange r : ranges) if (r.contains(row, column)) return true; return false; }
    public boolean canEdit(String user) { return editors.isEmpty() || user != null && editors.stream().anyMatch(e -> e.equalsIgnoreCase(user)); }
}
