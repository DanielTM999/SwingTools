package dtm.stools.component.panels.editor.sheet.model;

import lombok.Builder;
import lombok.With;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@With
@Builder(toBuilder = true)
public record WorkbookProperties(List<DefinedName> names, int activeSheet, boolean date1904, SheetTheme theme, boolean protectStructure, String protectionHash,
                                 List<CustomList> customLists, String title, String author, Map<String, byte[]> preserved, List<String> diagnostics) {
    public static final WorkbookProperties DEFAULT = new WorkbookProperties(List.of(), 0, false, SheetTheme.OFFICE, false, null, CustomList.DEFAULTS, "", "", Map.of(), List.of());

    public WorkbookProperties {
        names = names == null ? List.of() : List.copyOf(names);
        theme = Objects.requireNonNullElse(theme, SheetTheme.OFFICE);
        customLists = customLists == null ? CustomList.DEFAULTS : List.copyOf(customLists);
        title = Objects.requireNonNullElse(title, "");
        author = Objects.requireNonNullElse(author, "");
        preserved = preserved == null ? Map.of() : Map.copyOf(preserved);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        if (activeSheet < 0) activeSheet = 0;
    }
}
