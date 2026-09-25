package dtm.stools.component.panels.editor.sheet.model;

import java.util.Arrays;
import java.util.List;
import java.util.function.UnaryOperator;

public final class ArrayValue implements CellValue {
    private final int rows, columns;
    private final CellValue[] values;

    public ArrayValue(int rows, int columns, CellValue[] values) {
        if (rows <= 0 || columns <= 0 || values.length != rows * columns) throw new IllegalArgumentException("Invalid array " + rows + "x" + columns);
        this.rows = rows; this.columns = columns; this.values = values;
    }

    public static ArrayValue of(int rows, int columns) {
        CellValue[] v = new CellValue[rows * columns];
        Arrays.fill(v, CellValue.EMPTY);
        return new ArrayValue(rows, columns, v);
    }

    public static ArrayValue column(List<? extends CellValue> values) {
        return new ArrayValue(values.size(), 1, values.toArray(CellValue[]::new));
    }

    public static ArrayValue row(List<? extends CellValue> values) {
        return new ArrayValue(1, values.size(), values.toArray(CellValue[]::new));
    }

    public static ArrayValue of(List<List<CellValue>> rows) {
        int r = rows.size(), c = rows.stream().mapToInt(List::size).max().orElse(0);
        ArrayValue a = of(r, c);
        for (int i = 0; i < r; i++) for (int j = 0; j < rows.get(i).size(); j++) a.values[i * c + j] = rows.get(i).get(j);
        return a;
    }

    public int rows() { return rows; }
    public int columns() { return columns; }
    public int size() { return values.length; }
    public CellValue get(int row, int column) { return values[row * columns + column]; }
    public CellValue at(int index) { return values[index]; }
    public void set(int row, int column, CellValue value) { values[row * columns + column] = value == null ? CellValue.EMPTY : value; }

    public CellValue broadcast(int row, int column) {
        int r = rows == 1 ? 0 : row, c = columns == 1 ? 0 : column;
        if (r >= rows || c >= columns) return CellValue.error(CellError.NA);
        return get(r, c);
    }

    public ArrayValue map(UnaryOperator<CellValue> f) {
        CellValue[] v = new CellValue[values.length];
        for (int i = 0; i < v.length; i++) v[i] = f.apply(values[i]);
        return new ArrayValue(rows, columns, v);
    }

    public ArrayValue transpose() {
        ArrayValue t = of(columns, rows);
        for (int r = 0; r < rows; r++) for (int c = 0; c < columns; c++) t.set(c, r, get(r, c));
        return t;
    }

    public List<CellValue> list() { return List.of(values); }
    public CellValue[] toArray() { return values.clone(); }

    @Override public boolean equals(Object o) {
        return o instanceof ArrayValue a && a.rows == rows && a.columns == columns && Arrays.equals(a.values, values);
    }
    @Override public int hashCode() { return 31 * (31 * rows + columns) + Arrays.hashCode(values); }
    @Override public String toString() { return "{" + rows + "x" + columns + "}"; }
}
