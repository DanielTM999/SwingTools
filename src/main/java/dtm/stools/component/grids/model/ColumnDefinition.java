package dtm.stools.component.grids.model;

import dtm.stools.component.grids.annotations.GridColumn;
import lombok.Builder;
import lombok.Getter;

import java.lang.reflect.Field;

@Getter
public class ColumnDefinition {
    private final Field field;
    private final String key;
    private final Class<?> type;
    private final String name;
    private final String nameToSetter;
    private final int order;
    private final int width;
    private final boolean editable;
    private final boolean visible;

    public ColumnDefinition(Field field, GridColumn ann) {
        this.field = field;
        this.field.setAccessible(true);
        this.key = field.getName();
        this.type = field.getType();
        String annName = ann.name();
        if (annName == null || annName.trim().isEmpty()) {
            this.name = capitalize(field.getName());
        } else {
            this.name = annName;
        }
        this.order = ann.order();
        this.width = ann.width();
        this.editable = ann.editable();
        this.visible = ann.visible();
        this.nameToSetter = ann.setterRef();
    }

    @Builder
    private ColumnDefinition(String key, String name, Class<?> type, Integer order, Integer width,
                             Boolean editable, Boolean visible) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Column key is required");
        this.field = null;
        this.key = key;
        this.name = name == null || name.isBlank() ? capitalize(key) : name;
        this.type = type == null ? Object.class : type;
        this.order = order == null ? 999 : order;
        this.width = width == null ? 100 : width;
        this.editable = editable == null || editable;
        this.visible = visible == null || visible;
        this.nameToSetter = "";
    }

    private static String capitalize(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }
}
