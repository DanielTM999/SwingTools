package dtm.stools.component.panels.editor.code.documenthighlight;

import java.awt.Color;

public record DocumentHighlightPalette(Color text, Color read, Color write) {

    private static final Color DEFAULT_TEXT = new Color(125, 150, 255, 45);
    private static final Color DEFAULT_READ = new Color(125, 150, 255, 55);
    private static final Color DEFAULT_WRITE = new Color(255, 170, 95, 70);

    public DocumentHighlightPalette {
        text = text == null ? DEFAULT_TEXT : text;
        read = read == null ? DEFAULT_READ : read;
        write = write == null ? DEFAULT_WRITE : write;
    }

    public static DocumentHighlightPalette defaults() {
        return new DocumentHighlightPalette(DEFAULT_TEXT, DEFAULT_READ, DEFAULT_WRITE);
    }

    public Color colorFor(DocumentHighlight.Kind kind) {
        DocumentHighlight.Kind safeKind = kind == null ? DocumentHighlight.Kind.TEXT : kind;
        return switch (safeKind) {
            case READ -> read;
            case WRITE -> write;
            case TEXT -> text;
        };
    }
}
