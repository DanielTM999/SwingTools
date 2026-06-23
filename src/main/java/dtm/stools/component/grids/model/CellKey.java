package dtm.stools.component.grids.model;

import lombok.Getter;

import java.lang.reflect.Field;

@Getter
public class CellKey {
    final Object item;
    final Field field;

    public CellKey(Object item, Field field) {
        this.item = item;
        this.field = field;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(item) * 31 + field.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CellKey ck)) return false;
        return ck.item == item && ck.field.equals(field);
    }
}
