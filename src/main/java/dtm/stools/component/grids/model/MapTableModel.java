package dtm.stools.component.grids.model;

import java.util.*;
import java.util.function.Supplier;

public class MapTableModel extends GridTableModel<Map<String, Object>> {
    private final boolean autoColumns;

    public MapTableModel(List<ColumnDefinition> columns, Supplier<Boolean> allowEditGetter) {
        super(List.of(), rowClass(), columns, allowEditGetter);
        autoColumns = columns == null || columns.isEmpty();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Class<Map<String, Object>> rowClass() { return (Class) Map.class; }

    public boolean isAutoColumns() { return autoColumns; }

    public boolean inferColumns(Collection<?> rows) {
        if (!autoColumns) return false;
        Map<String, Class<?>> types = new LinkedHashMap<>();
        if (rows != null) for (Object row : rows) {
            if (!(row instanceof Map<?, ?> map)) continue;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Object value = entry.getValue();
                Class<?> known = types.get(key);
                if (value == null) types.putIfAbsent(key, null);
                else if (known == null) types.put(key, value.getClass());
                else if (known != value.getClass()) types.put(key, Object.class);
            }
        }
        List<ColumnDefinition> current = getColumns();
        if (current.size() == types.size()) {
            boolean same = true;
            int index = 0;
            for (Map.Entry<String, Class<?>> entry : types.entrySet()) {
                ColumnDefinition column = current.get(index++);
                Class<?> type = entry.getValue() == null ? Object.class : entry.getValue();
                if (!column.getKey().equals(entry.getKey()) || column.getType() != type) { same = false; break; }
            }
            if (same) return false;
        }
        List<ColumnDefinition> columns = new ArrayList<>();
        int order = 0;
        for (Map.Entry<String, Class<?>> entry : types.entrySet())
            columns.add(ColumnDefinition.builder().key(entry.getKey()).type(entry.getValue()).order(order++).build());
        setColumns(columns);
        return true;
    }

    @Override
    protected Object read(Map<String, Object> item, ColumnDefinition column) {
        return item.get(column.getKey());
    }

    @Override
    protected Object convert(Map<String, Object> item, ColumnDefinition column, Object value) {
        Object old = item.get(column.getKey());
        if (value == null) return null;
        if (column.getType() != Object.class) return convertValue(value, column.getType());
        if (old instanceof Collection<?> || (old != null && old.getClass().isArray())) return old;
        if (old == null || old.getClass().isInstance(value)) return value;
        try {
            return convertValue(value, old.getClass());
        } catch (RuntimeException ignored) {
            return value;
        }
    }

    @Override
    protected void store(Map<String, Object> item, ColumnDefinition column, Object converted) {
        try {
            item.put(column.getKey(), converted);
        } catch (UnsupportedOperationException error) {
            throw new IllegalStateException("Mapa da linha é imutável", error);
        }
    }
}
