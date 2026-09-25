package dtm.stools.component.panels.editor.sheet.model;

public record ObjectAnchor(int row, int column, int offsetX, int offsetY, int width, int height) {
    public ObjectAnchor {
        if (row < 0 || column < 0) throw new IllegalArgumentException("Invalid anchor");
        width = Math.max(8, width); height = Math.max(8, height);
    }

    public static ObjectAnchor at(CellAddress a, int width, int height) { return new ObjectAnchor(a.row(), a.column(), 0, 0, width, height); }
    public ObjectAnchor moved(int r, int c, int ox, int oy) { return new ObjectAnchor(r, c, ox, oy, width, height); }
    public ObjectAnchor resized(int w, int h) { return new ObjectAnchor(row, column, offsetX, offsetY, w, h); }
}
