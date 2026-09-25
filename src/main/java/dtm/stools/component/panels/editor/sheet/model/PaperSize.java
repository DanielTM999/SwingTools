package dtm.stools.component.panels.editor.sheet.model;

public enum PaperSize {
    LETTER(1, 612, 792), LEGAL(5, 612, 1008), A4(9, 595, 842), A3(8, 842, 1191), A5(11, 420, 595), EXECUTIVE(7, 522, 756);

    private final int code;
    private final double width, height;
    PaperSize(int code, double width, double height) { this.code = code; this.width = width; this.height = height; }
    public int code() { return code; }
    public double width() { return width; }
    public double height() { return height; }
    public static PaperSize fromCode(int code) { for (PaperSize p : values()) if (p.code == code) return p; return A4; }
}
