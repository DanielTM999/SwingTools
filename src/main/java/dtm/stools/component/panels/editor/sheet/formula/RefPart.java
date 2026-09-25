package dtm.stools.component.panels.editor.sheet.formula;

public record RefPart(int row, int column, boolean rowAbsolute, boolean columnAbsolute) {
    public static RefPart cell(int row, int column, boolean rowAbs, boolean colAbs) { return new RefPart(row, column, rowAbs, colAbs); }
    public static RefPart column(int column, boolean abs) { return new RefPart(-1, column, false, abs); }
    public static RefPart row(int row, boolean abs) { return new RefPart(row, -1, abs, false); }

    public boolean wholeColumn() { return row < 0; }
    public boolean wholeRow() { return column < 0; }
    public RefPart withRow(int r) { return new RefPart(r, column, rowAbsolute, columnAbsolute); }
    public RefPart withColumn(int c) { return new RefPart(row, c, rowAbsolute, columnAbsolute); }
    public RefPart withAbsolute(boolean r, boolean c) { return new RefPart(row, column, r, c); }
}
