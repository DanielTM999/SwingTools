package dtm.stools.component.panels.editor.sheet.model;

public record FreezePane(int rows, int columns) {
    public static final FreezePane NONE = new FreezePane(0, 0);

    public FreezePane {
        if (rows < 0 || columns < 0) throw new IllegalArgumentException("Negative freeze");
    }

    public boolean active() { return rows > 0 || columns > 0; }
}
