package dtm.stools.component.panels.editor.sheet.store;

import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class CellStore {
    static final int SHIFT = 5, SIZE = 1 << SHIFT, MASK = SIZE - 1;

    private static final class Chunk {
        final SheetCell[] cells;
        int count;
        Object owner;
        Chunk(Object owner) { this.owner = owner; cells = new SheetCell[SIZE * SIZE]; }
        Chunk(Chunk other, Object owner) { this.owner = owner; cells = other.cells.clone(); count = other.count; }
    }

    private final HashMap<Long, Chunk> chunks;
    private Object owner = new Object();
    private CellRange bounds;
    private boolean boundsValid;
    private long cellCount;

    public CellStore() { chunks = new HashMap<>(); boundsValid = true; }

    private CellStore(HashMap<Long, Chunk> chunks, long cellCount) { this.chunks = chunks; this.cellCount = cellCount; }

    private static long chunkKey(int row, int column) { return ((long) (row >>> SHIFT) << 9) | (column >>> SHIFT); }
    private static int chunkRow(long key) { return (int) (key >>> 9); }
    private static int chunkColumn(long key) { return (int) (key & 511); }

    public synchronized SheetCell get(int row, int column) {
        Chunk c = chunks.get(chunkKey(row, column));
        return c == null ? null : c.cells[((row & MASK) << SHIFT) | (column & MASK)];
    }

    public SheetCell get(CellAddress a) { return get(a.row(), a.column()); }

    public SheetCell getOrBlank(int row, int column) { SheetCell c = get(row, column); return c == null ? SheetCell.BLANK : c; }

    public synchronized SheetCell set(int row, int column, SheetCell cell) {
        if (cell != null && cell.isBlank()) cell = null;
        long key = chunkKey(row, column);
        Chunk c = chunks.get(key);
        int slot = ((row & MASK) << SHIFT) | (column & MASK);
        if (c == null) {
            if (cell == null) return null;
            c = new Chunk(owner);
            chunks.put(key, c);
        } else if (c.owner != owner) {
            if (cell == null && c.cells[slot] == null) return null;
            c = new Chunk(c, owner);
            chunks.put(key, c);
        }
        SheetCell previous = c.cells[slot];
        c.cells[slot] = cell;
        if (previous == null && cell != null) { c.count++; cellCount++; }
        else if (previous != null && cell == null) { c.count--; cellCount--; }
        if (c.count == 0) chunks.remove(key);
        if (cell != null && boundsValid) bounds = bounds == null ? CellRange.of(row, column) : bounds.union(CellRange.of(row, column));
        else if (cell == null && previous != null) boundsValid = false;
        return previous;
    }

    public SheetCell set(CellAddress a, SheetCell cell) { return set(a.row(), a.column(), cell); }

    public synchronized CellStore snapshot() {
        owner = new Object();
        CellStore copy = new CellStore(new HashMap<>(chunks), cellCount);
        copy.bounds = bounds; copy.boundsValid = boundsValid;
        return copy;
    }

    public synchronized long size() { return cellCount; }
    public synchronized boolean isEmpty() { return cellCount == 0; }

    public synchronized CellRange usedRange() {
        if (!boundsValid) {
            bounds = null;
            forEachUnsafe(null, (r, c, cell) -> bounds = bounds == null ? CellRange.of(r, c) : bounds.union(CellRange.of(r, c)));
            boundsValid = true;
        }
        return bounds;
    }

    public synchronized void forEach(CellRange range, CellVisitor visitor) { forEachUnsafe(range, visitor); }

    public void forEach(CellVisitor visitor) { forEach(null, visitor); }

    private void forEachUnsafe(CellRange range, CellVisitor visitor) {
        if (chunks.isEmpty()) return;
        if (range != null) {
            long span = (long) ((range.lastRow() >>> SHIFT) - (range.firstRow() >>> SHIFT) + 1) * ((range.lastColumn() >>> SHIFT) - (range.firstColumn() >>> SHIFT) + 1);
            if (span <= chunks.size()) {
                for (int cr = range.firstRow() >>> SHIFT; cr <= range.lastRow() >>> SHIFT; cr++)
                    for (int cc = range.firstColumn() >>> SHIFT; cc <= range.lastColumn() >>> SHIFT; cc++) {
                        Chunk c = chunks.get(((long) cr << 9) | cc);
                        if (c != null) visitChunk(cr, cc, c, range, visitor);
                    }
                return;
            }
        }
        TreeMap<Long, Chunk> sorted = new TreeMap<>(chunks);
        for (Map.Entry<Long, Chunk> e : sorted.entrySet()) {
            int cr = chunkRow(e.getKey()), cc = chunkColumn(e.getKey());
            if (range != null && ((cr << SHIFT) + MASK < range.firstRow() || (cr << SHIFT) > range.lastRow() || (cc << SHIFT) + MASK < range.firstColumn() || (cc << SHIFT) > range.lastColumn())) continue;
            visitChunk(cr, cc, e.getValue(), range, visitor);
        }
    }

    private static void visitChunk(int cr, int cc, Chunk c, CellRange range, CellVisitor visitor) {
        SheetCell[] cells = c.cells;
        for (int i = 0; i < cells.length; i++) {
            SheetCell cell = cells[i];
            if (cell == null) continue;
            int r = (cr << SHIFT) | (i >>> SHIFT), col = (cc << SHIFT) | (i & MASK);
            if (range == null || range.contains(r, col)) visitor.visit(r, col, cell);
        }
    }

    public synchronized void forEachRowMajor(CellRange range, CellVisitor visitor) {
        TreeMap<Integer, TreeMap<Integer, Chunk>> bands = new TreeMap<>();
        for (Map.Entry<Long, Chunk> e : chunks.entrySet()) bands.computeIfAbsent(chunkRow(e.getKey()), k -> new TreeMap<>()).put(chunkColumn(e.getKey()), e.getValue());
        for (Map.Entry<Integer, TreeMap<Integer, Chunk>> band : bands.entrySet()) {
            int cr = band.getKey();
            for (int local = 0; local < SIZE; local++) {
                int row = (cr << SHIFT) | local;
                if (range != null && (row < range.firstRow() || row > range.lastRow())) continue;
                for (Map.Entry<Integer, Chunk> ce : band.getValue().entrySet()) {
                    int cc = ce.getKey();
                    SheetCell[] cells = ce.getValue().cells;
                    for (int lc = 0; lc < SIZE; lc++) {
                        SheetCell cell = cells[(local << SHIFT) | lc];
                        if (cell == null) continue;
                        int col = (cc << SHIFT) | lc;
                        if (range == null || range.contains(row, col)) visitor.visit(row, col, cell);
                    }
                }
            }
        }
    }

    public List<CellAddress> addresses(CellRange range) {
        List<CellAddress> list = new ArrayList<>();
        forEach(range, (r, c, cell) -> list.add(new CellAddress(r, c)));
        return list;
    }

    public synchronized void clear() { chunks.clear(); cellCount = 0; bounds = null; boundsValid = true; owner = new Object(); }

    public CellStore shiftRows(int at, int count) {
        CellStore result = new CellStore();
        forEach((r, c, cell) -> {
            if (r < at) result.set(r, c, cell);
            else if (count > 0) { if (r + count < CellAddress.MAX_ROWS) result.set(r + count, c, cell); }
            else if (r >= at - count) result.set(r + count, c, cell);
        });
        return result;
    }

    public CellStore shiftColumns(int at, int count) {
        CellStore result = new CellStore();
        forEach((r, c, cell) -> {
            if (c < at) result.set(r, c, cell);
            else if (count > 0) { if (c + count < CellAddress.MAX_COLUMNS) result.set(r, c + count, cell); }
            else if (c >= at - count) result.set(r, c + count, cell);
        });
        return result;
    }

    public int lastRowInColumn(int column) {
        int[] last = {-1};
        CellRange used = usedRange();
        if (used == null) return -1;
        forEach(new CellRange(0, column, used.lastRow(), column), (r, c, cell) -> { if (cell.hasContent()) last[0] = Math.max(last[0], r); });
        return last[0];
    }
}
