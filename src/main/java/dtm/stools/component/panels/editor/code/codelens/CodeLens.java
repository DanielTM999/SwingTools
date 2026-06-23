package dtm.stools.component.panels.editor.code.codelens;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public record CodeLens(int line, int col, CodeLensPlacement placement, List<CodeLensItem> items) {

    public CodeLens {
        items = items == null ? Collections.emptyList() : List.copyOf(items);
        if (placement == null) placement = CodeLensPlacement.ABOVE;
    }

    public CodeLens(int line, List<CodeLensItem> items) {
        this(line, -1, CodeLensPlacement.ABOVE, items);
    }

    public static CodeLens of(int line, CodeLensItem... items) {
        return above(line, items);
    }

    public static CodeLens above(int line, CodeLensItem... items) {
        return new CodeLens(line, -1, CodeLensPlacement.ABOVE,
                items == null ? List.of() : Arrays.asList(items));
    }

    public static CodeLens above(int line, int col, CodeLensItem... items) {
        return new CodeLens(line, col, CodeLensPlacement.ABOVE,
                items == null ? List.of() : Arrays.asList(items));
    }

    public static CodeLens inline(int line, CodeLensItem... items) {
        return new CodeLens(line, -1, CodeLensPlacement.INLINE,
                items == null ? List.of() : Arrays.asList(items));
    }

    public static CodeLens inlineAt(int line, int col, CodeLensItem... items) {
        return new CodeLens(line, col, CodeLensPlacement.INLINE,
                items == null ? List.of() : Arrays.asList(items));
    }
}
