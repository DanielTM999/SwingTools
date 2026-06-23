package dtm.stools.component.panels.editor.code.api;


public record TextEdit(Range range, String newText) {

    public static TextEdit replace(Range range, String newText) {
        return new TextEdit(range, newText == null ? "" : newText);
    }

    public static TextEdit insert(Position at, String text) {
        return new TextEdit(new Range(at, at), text == null ? "" : text);
    }

    public static TextEdit delete(Range range) {
        return new TextEdit(range, "");
    }
}
