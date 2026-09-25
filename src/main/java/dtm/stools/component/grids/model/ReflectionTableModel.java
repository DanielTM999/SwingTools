package dtm.stools.component.grids.model;

import dtm.stools.component.grids.annotations.GridColumn;
import dtm.stools.component.inputfields.selectfield.DropdownField;

import javax.swing.table.AbstractTableModel;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.Consumer;

public class ReflectionTableModel<T> extends AbstractTableModel {
    public record Edit<T>(T item, int row, int column, Object oldValue, Object newValue) {}
    private final Map<CellKey, Object> referenceValueTableMap;
    private List<T> dataList;
    private final Class<T> clazz;
    private final List<ColumnDefinition> columns;
    private final Supplier<Boolean> allowEditGetter;
    private Consumer<Edit<T>> editListener = edit -> {};

    public ReflectionTableModel(List<T> dataList, Class<T> clazz, Supplier<Boolean> allowEditGetter) {
        this.dataList = dataList;
        this.clazz = clazz;
        this.allowEditGetter = allowEditGetter;

        List<ColumnDefinition> tempCols = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(GridColumn.class)) {
                GridColumn ann = field.getAnnotation(GridColumn.class);
                tempCols.add(new ColumnDefinition(field, ann));
            }
        }
        tempCols.sort(Comparator.comparingInt(ColumnDefinition::getOrder));
        this.columns = tempCols;
        this.referenceValueTableMap = new LinkedHashMap<>(64, 0.75f, true) {
            @Override protected boolean removeEldestEntry(Map.Entry<CellKey, Object> eldest) { return size() > 512; }
        };
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

            ColumnDefinition columnDefinition = columns.get(columnIndex);
            Field field = columnDefinition.getField();
            Field fieldToSet = getFieldRefSetter(columnDefinition.getNameToSetter(), clazz, field);

            fieldToSet.setAccessible(true);
            Object oldValue = fieldToSet.get(item);

            CellKey key = new CellKey(item, field);
            Object refObj = referenceValueTableMap.get(key);

            if (refObj instanceof DropdownField dropdown) {
                fieldToSet.set(item, convertValue(dropdown.getSelectedItem(), fieldToSet.getType()));
            } else {
                fieldToSet.set(item, convertValue(aValue, fieldToSet.getType()));
            }
            referenceValueTableMap.remove(key);
            Object newValue = fieldToSet.get(item);
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
        Class<?> type = columns.get(columnIndex).getField().getType();

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

            T obj = dataList.get(rowIndex);
            return getValueAtItem(obj, columnIndex);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (columnIndex >= columns.size()) return true;
        ColumnDefinition colDef = columns.get(columnIndex);
        return allowEditGetter.get() && colDef.isEditable();
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

    public Field getFieldForColumn(int column) {
        if (column < 0 || column >= columns.size()) return null;
        return columns.get(column).getField();
    }

    public ColumnDefinition getColumnDefinition(int modelIndex) {
        if (modelIndex < 0 || modelIndex >= columns.size()) return null;
        return columns.get(modelIndex);
    }

    public int findColumnIndexByFieldName(String fieldName) {
        for (int i = 0; i < columns.size(); i++) {
            if (columns.get(i).getField().getName().equals(fieldName)) {
                return i;
            }
        }
        return -1;
    }

    public Class<?> getItemClass() {
        return this.clazz;
    }

    public Object getValueAtItem(Object item, int columnIndex) throws IllegalAccessException {
        if (item == null) return null;

        Field field = columns.get(columnIndex).getField();
        Object value = field.get(item);

        CellKey key = new CellKey(item, field);

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

    private Field getFieldRefSetter(String name, Class<?> refClass, Field fieldBase){
        if (name == null || name.isEmpty()) {
            return fieldBase;
        }

        try {
            Field refField = refClass.getDeclaredField(name);
            refField.setAccessible(true);
            return refField;
        } catch (NoSuchFieldException e) {
            return fieldBase;
        }
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
