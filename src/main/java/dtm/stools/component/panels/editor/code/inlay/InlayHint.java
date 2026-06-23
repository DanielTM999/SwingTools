package dtm.stools.component.panels.editor.code.inlay;

import java.awt.Color;

public record InlayHint(
        int line,
        int col,
        String text,
        InlayHintKind kind,
        boolean paddingLeft,
        boolean paddingRight,
        Color backgroundColor,
        Color foregroundColor,
        boolean pushText,
        boolean mouseTransparent,
        boolean hideOnCaretOrSelection
) {
    public InlayHint(int line, int col, String text) {
        this(line, col, text, InlayHintKind.OTHER, false, false, null, null, false, false, false);
    }

    public InlayHint(int line, int col, String text, InlayHintKind kind) {
        this(line, col, text, kind, kind == InlayHintKind.PARAMETER, false, null, null,
                kind == InlayHintKind.PARAMETER,
                kind == InlayHintKind.PARAMETER,
                kind == InlayHintKind.PARAMETER);
    }

    public InlayHint(int line,
                     int col,
                     String text,
                     InlayHintKind kind,
                     boolean paddingLeft,
                     boolean paddingRight,
                     Color backgroundColor,
                     Color foregroundColor) {
        this(line, col, text, kind, paddingLeft, paddingRight, backgroundColor, foregroundColor,
                false, false, false);
    }

    public InlayHint(int line,
                     int col,
                     String text,
                     InlayHintKind kind,
                     boolean paddingLeft,
                     boolean paddingRight,
                     Color backgroundColor,
                     Color foregroundColor,
                     boolean pushText) {
        this(line, col, text, kind, paddingLeft, paddingRight, backgroundColor, foregroundColor,
                pushText, false, false);
    }

    public InlayHint(int line,
                     int col,
                     String text,
                     InlayHintKind kind,
                     boolean paddingLeft,
                     boolean paddingRight,
                     Color backgroundColor,
                     Color foregroundColor,
                     boolean pushText,
                     boolean mouseTransparent) {
        this(line, col, text, kind, paddingLeft, paddingRight, backgroundColor, foregroundColor,
                pushText, mouseTransparent, false);
    }
}
