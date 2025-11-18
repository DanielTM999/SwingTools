package dtm.stools.component.grids.model;

import dtm.stools.component.grids.annotations.GridColumn;
import dtm.stools.component.inputfields.selectfield.DropdownField;

import javax.swing.table.AbstractTableModel;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

public class ReflectionTableModel<T> extends AbstractTableModel {
    private final Map<CellKey, Object> referenceValueTableMap;
    private List<T> dataList;
    private final Class<T> clazz;
    private final List<ColumnDefinition> columns;
    private final Supplier<Boolean> allowEditGetter;

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
        this.referenceValueTableMap = Collections.synchronizedMap(new WeakHashMap<>());
    }

    @Override
    public int getRowCount() { return dataList.size(); }

    @Override
    public int getColumnCount() { return columns.size(); }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        try {
            if (columnIndex >= columns.size()) return;
            T item = dataList.get(rowIndex);
            Field field = columns.get(columnIndex).getField();
            field.setAccessible(true);
            CellKey key = new CellKey(item, field);
            Object refObj = referenceValueTableMap.get(key);
            if (refObj instanceof DropdownField dropdown) {
                return;
            }
            field.set(item, aValue);
            referenceValueTableMap.remove(key);
            fireTableCellUpdated(rowIndex, columnIndex);

        } catch (Exception e) {
            e.printStackTrace();
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

        return referenceValueTableMap.computeIfAbsent(key, k -> {
            if (value instanceof Collection<?> col) {
                return new DropdownField(col);
            }

            if (value != null && value.getClass().isArray()) {
                return new DropdownField((Object[]) value);
            }

            return value;
        });
    }

}
