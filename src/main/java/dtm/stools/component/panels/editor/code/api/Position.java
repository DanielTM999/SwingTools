package dtm.stools.component.panels.editor.code.api;

public record Position(int line, int col) {
    public static Position of(int line, int col) {
        return new Position(line, col);
    }
}
