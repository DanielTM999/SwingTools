package dtm.stools.component.grids.model;

import dtm.stools.component.grids.annotations.GridColumn;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

public class ReflectionTableModel<T> extends GridTableModel<T> {

    public ReflectionTableModel(List<T> dataList, Class<T> clazz, Supplier<Boolean> allowEditGetter) {
        super(dataList, clazz, columnsOf(clazz), allowEditGetter);
    }

    private static List<ColumnDefinition> columnsOf(Class<?> clazz) {
        List<ColumnDefinition> columns = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(GridColumn.class)) {
                columns.add(new ColumnDefinition(field, field.getAnnotation(GridColumn.class)));
            }
        }
        return columns;
    }

    @Override
    protected Object read(T item, ColumnDefinition column) throws IllegalAccessException {
        return column.getField().get(item);
    }

    @Override
    protected Object readStored(T item, ColumnDefinition column) throws IllegalAccessException {
        return targetField(column).get(item);
    }

    @Override
    protected Object convert(T item, ColumnDefinition column, Object value) {
        return convertValue(value, targetField(column).getType());
    }

    @Override
    protected void store(T item, ColumnDefinition column, Object converted) throws IllegalAccessException {
        targetField(column).set(item, converted);
    }

    public Field getFieldForColumn(int column) {
        ColumnDefinition definition = getColumnDefinition(column);
        return definition == null ? null : definition.getField();
    }

    public int findColumnIndexByFieldName(String fieldName) {
        return findColumnIndexByKey(fieldName);
    }

    private Field targetField(ColumnDefinition column) {
        return getFieldRefSetter(column.getNameToSetter(), getItemClass(), column.getField());
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
}
