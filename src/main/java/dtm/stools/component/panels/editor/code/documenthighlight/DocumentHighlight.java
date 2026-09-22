package dtm.stools.component.panels.editor.code.documenthighlight;

import dtm.stools.component.panels.editor.code.api.Range;

public record DocumentHighlight(Range range, Kind kind) {

    public enum Kind {
        TEXT,
        READ,
        WRITE
    }

    public DocumentHighlight {
        kind = kind == null ? Kind.TEXT : kind;
    }

    public static DocumentHighlight text(Range range) {
        return new DocumentHighlight(range, Kind.TEXT);
    }

    public static DocumentHighlight read(Range range) {
        return new DocumentHighlight(range, Kind.READ);
    }

    public static DocumentHighlight write(Range range) {
        return new DocumentHighlight(range, Kind.WRITE);
    }
}
