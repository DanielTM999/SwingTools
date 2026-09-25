package dtm.stools.component.grids;

import dtm.stools.component.grids.model.GridTableModel;
import lombok.Getter;

import java.util.*;

public final class GridRow<T> extends AbstractList<Object> {
    @Getter private final T item;
    @Getter private final int viewRow;
    @Getter private final int sourceIndex;
    private final List<String> keys;
    private final List<String> names;
    private final List<Object> values;
    private final Map<String, Object> extras;

    GridRow(T item, int viewRow, int sourceIndex, List<String> keys, List<String> names,
            List<Object> values, Map<String, Object> extras) {
        this.item = item;
        this.viewRow = viewRow;
        this.sourceIndex = sourceIndex;
        this.keys = List.copyOf(keys);
        this.names = List.copyOf(names);
        this.values = Collections.unmodifiableList(new ArrayList<>(values));
        this.extras = Collections.unmodifiableMap(new LinkedHashMap<>(extras));
    }

    static <T> GridRow<T> empty() {
        return new GridRow<>(null, -1, -1, List.of(), List.of(), List.of(), Map.of());
    }

    public Object getCell(int index) {
        Objects.checkIndex(index, values.size());
        return values.get(index);
    }

    public Object getCell(String name) {
        Objects.requireNonNull(name, "name");
        int index = columnIndex(name);
        if (index >= 0) return values.get(index);
        if (extras.containsKey(name)) return extras.get(name);
        throw new IllegalArgumentException("Coluna inexistente: " + name);
    }

    @SuppressWarnings("unchecked")
    public <V> V getCell(String name, Class<V> type) {
        return (V) GridTableModel.convertValue(getCell(name), Objects.requireNonNull(type, "type"));
    }

    @SuppressWarnings("unchecked")
    public <V> V getCell(int index, Class<V> type) {
        return (V) GridTableModel.convertValue(getCell(index), Objects.requireNonNull(type, "type"));
    }

    public boolean hasCell(String name) {
        return name != null && (columnIndex(name) >= 0 || extras.containsKey(name));
    }

    public List<String> getKeys() { return keys; }

    public List<String> getColumnNames() { return names; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < keys.size(); index++) map.put(keys.get(index), values.get(index));
        return map;
    }

    private int columnIndex(String name) {
        int index = keys.indexOf(name);
        return index >= 0 ? index : names.indexOf(name);
    }

    @Override public Object get(int index) { return values.get(index); }

    @Override public int size() { return values.size(); }
}
