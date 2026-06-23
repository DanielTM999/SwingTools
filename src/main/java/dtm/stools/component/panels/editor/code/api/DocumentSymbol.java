package dtm.stools.component.panels.editor.code.api;

import java.util.Collections;
import java.util.List;

/**
 * A symbol declared inside a document, possibly containing nested children
 * (e.g. methods inside a class). {@code range} covers the full declaration
 * (including body), while {@code selectionRange} is the identifier span
 * that should be highlighted when navigating to the symbol.
 */
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
