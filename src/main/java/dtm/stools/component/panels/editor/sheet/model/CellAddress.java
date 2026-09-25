package dtm.stools.component.panels.editor.sheet.model;

import java.util.Locale;

public record CellAddress(int row, int column) implements Comparable<CellAddress> {
    public static final int MAX_ROWS = 1_048_576;
    public static final int MAX_COLUMNS = 16_384;

    public CellAddress {
        if (row < 0 || column < 0 || row >= MAX_ROWS || column >= MAX_COLUMNS) throw new IllegalArgumentException("Invalid cell " + row + "," + column);
    }

    public static CellAddress of(int row, int column) { return new CellAddress(row, column); }

    public static CellAddress parse(String text) {
        String t = text.strip().replace("$", "").toUpperCase(Locale.ROOT);
        int i = 0;
        while (i < t.length() && Character.isLetter(t.charAt(i))) i++;
        if (i == 0 || i == t.length()) throw new IllegalArgumentException("Invalid cell reference: " + text);
        int column = columnIndex(t.substring(0, i));
        int row;
        try { row = Integer.parseInt(t.substring(i)) - 1; } catch (NumberFormatException e) { throw new IllegalArgumentException("Invalid cell reference: " + text, e); }
        return new CellAddress(row, column);
    }

    public static boolean isValid(String text) {
        try { parse(text); return true; } catch (RuntimeException e) { return false; }
    }

    public static String columnName(int index) {
        if (index < 0) throw new IllegalArgumentException("Negative column");
        StringBuilder b = new StringBuilder();
        int n = index + 1;
        while (n > 0) { n--; b.insert(0, (char) ('A' + n % 26)); n /= 26; }
        return b.toString();
    }

    public static int columnIndex(String letters) {
        if (letters.isEmpty() || letters.length() > 3) throw new IllegalArgumentException("Invalid column: " + letters);
        int n = 0;
        for (int i = 0; i < letters.length(); i++) {
            char c = Character.toUpperCase(letters.charAt(i));
            if (c < 'A' || c > 'Z') throw new IllegalArgumentException("Invalid column: " + letters);
            n = n * 26 + (c - 'A' + 1);
        }
        if (n > MAX_COLUMNS) throw new IllegalArgumentException("Column out of range: " + letters);
        return n - 1;
    }

    public long key() { return key(row, column); }
    public static long key(int row, int column) { return ((long) row << 14) | column; }
    public static int keyRow(long key) { return (int) (key >>> 14); }
    public static int keyColumn(long key) { return (int) (key & 0x3FFF); }
    public static CellAddress fromKey(long key) { return new CellAddress(keyRow(key), keyColumn(key)); }

    public CellAddress offset(int rows, int columns) { return new CellAddress(row + rows, column + columns); }
    public boolean canOffset(int rows, int columns) { int r = row + rows, c = column + columns; return r >= 0 && c >= 0 && r < MAX_ROWS && c < MAX_COLUMNS; }

    public String toA1() { return columnName(column) + (row + 1); }
    public String toAbsolute() { return "$" + columnName(column) + "$" + (row + 1); }

    @Override public int compareTo(CellAddress o) { return row != o.row ? Integer.compare(row, o.row) : Integer.compare(column, o.column); }
    @Override public String toString() { return toA1(); }
}
