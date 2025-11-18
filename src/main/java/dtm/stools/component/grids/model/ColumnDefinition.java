package dtm.stools.component.grids.model;

import dtm.stools.component.grids.annotations.GridColumn;
import lombok.Getter;

import java.lang.reflect.Field;

@Getter
public class ColumnDefinition {
    private final Field field;
    private final String name;
    private final int order;
    private final int width;
    private final boolean editable;
    private final boolean visible;

    public ColumnDefinition(Field field, GridColumn ann) {
        this.field = field;
        this.field.setAccessible(true);
        String annName = ann.name();
        if (annName == null || annName.trim().isEmpty()) {
            String fieldName = field.getName();
            this.name = fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        } else {
            this.name = annName;
        }
        this.order = ann.order();
        this.width = ann.width();
        this.editable = ann.editable();
        this.visible = ann.visible();
    }

}
