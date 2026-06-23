package dtm.stools.component.panels.editor.code.api;

/**
 * A zero-based (line, column) position within a document.
 */
public record Position(int line, int col) {
    public static Position of(int line, int col) {
        return new Position(line, col);
    }
}
