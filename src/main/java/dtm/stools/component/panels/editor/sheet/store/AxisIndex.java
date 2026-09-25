package dtm.stools.component.panels.editor.sheet.store;

import java.util.Arrays;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

public final class AxisIndex {
    private final int count;
    private int defaultSize;
    private final TreeMap<Integer, Integer> sizes = new TreeMap<>();
    private final TreeSet<Integer> hidden = new TreeSet<>();
    private final TreeMap<Integer, Integer> outline = new TreeMap<>();
    private final TreeMap<Integer, Integer> styles = new TreeMap<>();
    private int[] keys = new int[0];
    private long[] prefix = new long[1];
    private boolean dirty;

    public AxisIndex(int count, int defaultSize) { this.count = count; this.defaultSize = defaultSize; }

    public synchronized int count() { return count; }
    public synchronized int defaultSize() { return defaultSize; }
    public synchronized void setDefaultSize(int value) { defaultSize = Math.max(1, value); dirty = true; }

    public synchronized int rawSize(int index) { return sizes.getOrDefault(index, defaultSize); }
    public synchronized boolean hasCustomSize(int index) { return sizes.containsKey(index); }
    public synchronized int size(int index) { return hidden.contains(index) ? 0 : sizes.getOrDefault(index, defaultSize); }
    public synchronized boolean isHidden(int index) { return hidden.contains(index); }
    public synchronized Map<Integer, Integer> customSizes() { return Map.copyOf(sizes); }
    public synchronized NavigableSet<Integer> hiddenIndexes() { return new TreeSet<>(hidden); }
    public synchronized int outlineLevel(int index) { return outline.getOrDefault(index, 0); }
    public synchronized Map<Integer, Integer> outlineLevels() { return Map.copyOf(outline); }
    public synchronized int style(int index) { return styles.getOrDefault(index, 0); }
    public synchronized Map<Integer, Integer> styles() { return Map.copyOf(styles); }

    public synchronized void setSize(int index, int size) {
        if (size < 0) throw new IllegalArgumentException("Negative size");
        if (size == 0) { hidden.add(index); }
        else if (size == defaultSize) sizes.remove(index);
        else sizes.put(index, size);
        dirty = true;
    }

    public synchronized void resetSize(int index) { sizes.remove(index); dirty = true; }
    public synchronized void setHidden(int index, boolean value) { if (value) hidden.add(index); else hidden.remove(index); dirty = true; }
    public synchronized void setOutlineLevel(int index, int level) { if (level <= 0) outline.remove(index); else outline.put(index, Math.min(7, level)); }
    public synchronized void setStyle(int index, int style) { if (style <= 0) styles.remove(index); else styles.put(index, style); }

    private void rebuild() {
        if (!dirty && keys.length == sizes.size() + hidden.size()) return;
        TreeSet<Integer> all = new TreeSet<>(sizes.keySet());
        all.addAll(hidden);
        keys = all.stream().mapToInt(Integer::intValue).toArray();
        prefix = new long[keys.length + 1];
        for (int i = 0; i < keys.length; i++) prefix[i + 1] = prefix[i] + (size(keys[i]) - defaultSize);
        dirty = false;
    }

    public synchronized long position(int index) {
        rebuild();
        int i = Arrays.binarySearch(keys, index);
        int before = i >= 0 ? i : -i - 1;
        return (long) index * defaultSize + prefix[before];
    }

    public synchronized long totalSize() { return position(count); }

    public synchronized int indexAt(long position) {
        if (position <= 0) return 0;
        int lo = 0, hi = count - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (position(mid) <= position) lo = mid; else hi = mid - 1;
        }
        while (lo > 0 && size(lo) == 0 && position(lo) >= position) lo--;
        return lo;
    }

    public synchronized int nextVisible(int index, int direction) {
        int i = index;
        do { i += direction; } while (i >= 0 && i < count && hidden.contains(i));
        return Math.max(0, Math.min(count - 1, i));
    }

    public synchronized void shift(int at, int delta) {
        shiftMap(sizes, at, delta);
        shiftMap(outline, at, delta);
        shiftMap(styles, at, delta);
        TreeSet<Integer> h = new TreeSet<>();
        for (int i : hidden) { int n = shifted(i, at, delta); if (n >= 0) h.add(n); }
        hidden.clear(); hidden.addAll(h);
        dirty = true;
    }

    private int shifted(int i, int at, int delta) {
        if (i < at) return i;
        if (delta < 0 && i < at - delta) return -1;
        int n = i + delta;
        return n >= count ? -1 : n;
    }

    private void shiftMap(TreeMap<Integer, Integer> map, int at, int delta) {
        TreeMap<Integer, Integer> copy = new TreeMap<>();
        for (Map.Entry<Integer, Integer> e : map.entrySet()) { int n = shifted(e.getKey(), at, delta); if (n >= 0) copy.put(n, e.getValue()); }
        map.clear(); map.putAll(copy);
    }

    public synchronized AxisIndex copy() {
        AxisIndex a = new AxisIndex(count, defaultSize);
        a.sizes.putAll(sizes); a.hidden.addAll(hidden); a.outline.putAll(outline); a.styles.putAll(styles); a.dirty = true;
        return a;
    }
}
