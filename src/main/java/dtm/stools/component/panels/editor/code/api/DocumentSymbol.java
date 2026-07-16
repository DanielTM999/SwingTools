package dtm.stools.component.panels.editor.code.api;

import java.util.Collections;
import java.util.List;

public record DocumentSymbol(
        String name,
        String detail,
        SymbolKind kind,
        Range range,
        Range selectionRange,
        List<DocumentSymbol> children
) {
    public DocumentSymbol {
        if (kind == null) kind = SymbolKind.OTHER;
        if (children == null) children = Collections.emptyList();
        else children = List.copyOf(children);
    }

    public static DocumentSymbol leaf(String name, SymbolKind kind, Range range) {
        return new DocumentSymbol(name, null, kind, range, range, Collections.emptyList());
    }
}
