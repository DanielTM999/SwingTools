package dtm.stools.component.panels.editor.sheet.model;

import java.util.ArrayList;
import java.util.List;

public record SheetThread(List<SheetComment> comments, boolean resolved) {
    public SheetThread { comments = List.copyOf(comments); }

    public static SheetThread start(SheetComment first) { return new SheetThread(List.of(first), false); }
    public SheetThread reply(SheetComment comment) { List<SheetComment> l = new ArrayList<>(comments); l.add(comment); return new SheetThread(l, resolved); }
    public SheetThread withResolved(boolean value) { return new SheetThread(comments, value); }
}
