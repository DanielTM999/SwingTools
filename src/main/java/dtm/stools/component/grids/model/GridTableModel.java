package dtm.stools.component.grids.model;

import dtm.stools.component.inputfields.selectfield.DropdownField;

import javax.swing.table.AbstractTableModel;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class GridTableModel<T> extends AbstractTableModel {
    public record Edit<T>(T item, int row, int column, Object oldValue, Object newValue) {}

    private record CacheKey(Object item, String key) {
        @Override public boolean equals(Object other) {
            return other instanceof CacheKey cell && cell.item == item && cell.key.equals(key);
        }
        @Override public int hashCode() { return System.identityHashCode(item) * 31 + key.hashCode(); }
    }

    private final Map<CacheKey, Object> referenceValueTableMap;
    private final Class<T> itemClass;
    private final Supplier<Boolean> allowEditGetter;
    private List<T> dataList;
    private List<ColumnDefinition> columns;
    private Consumer<Edit<T>> editListener = edit -> {};

    protected GridTableModel(List<T> dataList, Class<T> itemClass, List<ColumnDefinition> columns,
                             Supplier<Boolean> allowEditGetter) {
        this.dataList = dataList == null ? List.of() : dataList;
        this.itemClass = Objects.requireNonNull(itemClass, "itemClass");
        this.allowEditGetter = allowEditGetter == null ? () -> false : allowEditGetter;
        this.columns = validated(columns);
        this.referenceValueTableMap = new LinkedHashMap<>(64, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<CacheKey, Object> eldest) { return size() > 512; }
        };
    }

    protected abstract Object read(T item, ColumnDefinition column) throws ReflectiveOperationException;

    protected abstract void store(T item, ColumnDefinition column, Object converted) throws ReflectiveOperationException;

    protected Object readStored(T item, ColumnDefinition column) throws ReflectiveOperationException {
        return read(item, column);
    }

    protected Object convert(T item, ColumnDefinition column, Object value) throws ReflectiveOperationException {
        return convertValue(value, column.getType());
    }

    protected void setColumns(List<ColumnDefinition> columns) {
        this.columns = validated(columns);
        referenceValueTableMap.clear();
        fireTableStructureChanged();
    }

    private static List<ColumnDefinition> validated(List<ColumnDefinition> columns) {
        List<ColumnDefinition> copy = new ArrayList<>(columns == null ? List.of() : columns);
        Set<String> keys = new HashSet<>();
        for (ColumnDefinition column : copy) {
            Objects.requireNonNull(column, "column");
            if (!keys.add(column.getKey())) throw new IllegalArgumentException("Duplicated grid column: " + column.getKey());
        }
        copy.sort(Comparator.comparingInt(ColumnDefinition::getOrder));
        return List.copyOf(copy);
    }

    @Override
    public int getRowCount() { return dataList.size(); }

    @Override
    public int getColumnCount() { return columns.size(); }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            if (rowIndex < 0 || rowIndex >= dataList.size() || columnIndex < 0 || columnIndex >= columns.size()) return;
            T item = dataList.get(rowIndex);
            ColumnDefinition column = columns.get(columnIndex);
            Object oldValue = readStored(item, column);
            CacheKey key = new CacheKey(item, column.getKey());
            Object refObj = referenceValueTableMap.get(key);
            Object value = refObj instanceof DropdownField dropdown ? dropdown.getSelectedItem() : aValue;
            store(item, column, convert(item, column, value));
            referenceValueTableMap.remove(key);
            Object newValue = readStored(item, column);
            editListener.accept(new Edit<>(item, rowIndex, columnIndex, oldValue, newValue));
            fireTableCellUpdated(rowIndex, columnIndex);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to update grid cell", e);
        }
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).getName();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Class<?> type = columns.get(columnIndex).getType();
        if (type.isPrimitive()) {
            if (type == boolean.class) return Boolean.class;
            if (type == int.class) return Integer.class;
            if (type == double.class) return Double.class;
            if (type == long.class) return Long.class;
            if (type == float.class) return Float.class;
            if (type == short.class) return Short.class;
            if (type == byte.class) return Byte.class;
            if (type == char.class) return Character.class;
        }
        return type;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        try {
            if (columnIndex >= getColumnCount()) return "";
            return getValueAtItem(dataList.get(rowIndex), columnIndex);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex >= columns.size()) return true;
        return allowEditGetter.get() && columns.get(columnIndex).isEditable();
    }

    @SuppressWarnings("unchecked")
    public void setDataList(List<?> dataList) {
        this.dataList = (List<T>) dataList;
        referenceValueTableMap.clear();
        fireTableDataChanged();
    }

    public List<T> getDataList() { return Collections.unmodifiableList(new ArrayList<>(dataList)); }

    public void setEditListener(Consumer<Edit<T>> listener) { editListener = Objects.requireNonNull(listener); }

    public T getObjectAt(int modelRow) {
        if (modelRow < 0 || modelRow >= dataList.size()) return null;
        return dataList.get(modelRow);
    }

    public List<ColumnDefinition> getColumns() { return columns; }

    public ColumnDefinition getColumnDefinition(int modelIndex) {
        if (modelIndex < 0 || modelIndex >= columns.size()) return null;
        return columns.get(modelIndex);
    }

    public String getKeyForColumn(int column) {
        ColumnDefinition definition = getColumnDefinition(column);
        return definition == null ? null : definition.getKey();
    }

    public int findColumnIndexByKey(String key) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getKey().equals(key)) return i;
        }
        return -1;
    }

    public Class<T> getItemClass() { return itemClass; }

    public Object readValue(T item, int column) {
        if (item == null) return null;
        try {
            return read(item, columns.get(column));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to read grid field", error);
        }
    }

    public Object convertForColumn(T item, int column, Object value) {
        try {
            return convert(item, columns.get(column), value);
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to convert grid value", error);
        }
    }

    public void writeValue(T item, int column, Object value) {
        try {
            ColumnDefinition definition = columns.get(column);
            store(item, definition, convert(item, definition, value));
            referenceValueTableMap.remove(new CacheKey(item, definition.getKey()));
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unable to write grid field", error);
        }
    }

    @SuppressWarnings("unchecked")
    public Object getValueAtItem(Object item, int columnIndex) throws IllegalAccessException {
        if (item == null) return null;
        ColumnDefinition column = columns.get(columnIndex);
        Object value;
        try {
            value = read((T) item, column);
        } catch (IllegalAccessException error) {
            throw error;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
        CacheKey key = new CacheKey(item, column.getKey());
        if (value instanceof Collection<?> col)
            return referenceValueTableMap.computeIfAbsent(key, k -> new DropdownField(col));
        if (value != null && value.getClass().isArray())
            return referenceValueTableMap.computeIfAbsent(key, k -> {
                int length = java.lang.reflect.Array.getLength(value);
                Object[] choices = new Object[length];
                for (int i = 0; i < length; i++) choices[i] = java.lang.reflect.Array.get(value, i);
                return new DropdownField(choices);
            });
        return value;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object convertValue(Object value, Class<?> type) {
        Class<?> boxed = type.isPrimitive() ? switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            case "char" -> Character.class;
            default -> type;
        } : type;
        if (value == null || boxed.isInstance(value)) return value;
        if (boxed == String.class) return value.toString();
        if (value instanceof Number number) {
            if (boxed == Byte.class) return number.byteValue();
            if (boxed == Short.class) return number.shortValue();
            if (boxed == Integer.class) return number.intValue();
            if (boxed == Long.class) return number.longValue();
            if (boxed == Float.class) return number.floatValue();
            if (boxed == Double.class) return number.doubleValue();
        }
        if (value instanceof String text) {
            String trimmed = text.strip();
            if (boxed == BigDecimal.class) return new BigDecimal(trimmed);
            if (boxed == BigInteger.class) return new BigInteger(trimmed);
            if (boxed == Byte.class) return Byte.valueOf(trimmed);
            if (boxed == Short.class) return Short.valueOf(trimmed);
            if (boxed == Integer.class) return Integer.valueOf(trimmed);
            if (boxed == Long.class) return Long.valueOf(trimmed);
            if (boxed == Float.class) return Float.valueOf(trimmed);
            if (boxed == Double.class) return Double.valueOf(trimmed);
            if (boxed == Boolean.class && (trimmed.equalsIgnoreCase("true") || trimmed.equalsIgnoreCase("false")))
                return Boolean.valueOf(trimmed);
            if (boxed == Character.class && trimmed.length() == 1) return trimmed.charAt(0);
            if (boxed.isEnum()) return Enum.valueOf((Class<? extends Enum>) boxed, trimmed);
        }
        throw new IllegalArgumentException("Value is not compatible with " + type.getSimpleName());
    }
}
