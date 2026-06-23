package dtm.stools.component.panels.editor.code.api;


public record Range(Position start, Position end) {

    public static Range of(Position start, Position end) {
        return new Range(start, end);
    }

    public static Range of(int startLine, int startCol, int endLine, int endCol) {
        return new Range(new Position(startLine, startCol), new Position(endLine, endCol));
    }

    public static Range point(int line, int col) {
        Position p = new Position(line, col);
        return new Range(p, p);
    }

    public boolean isEmpty() {
        return start.equals(end);
    }

    public boolean contains(Position p) {
        if (p == null) return false;
        if (p.line() < start.line() || p.line() > end.line()) return false;
        if (p.line() == start.line() && p.col() < start.col()) return false;
        if (p.line() == end.line() && p.col() > end.col()) return false;
        return true;
    }
}
