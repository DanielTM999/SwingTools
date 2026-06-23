package dtm.stools.component.grids.event;

import java.util.List;

public interface EventGrid {

    default int getSelectedRowCount() {
        return getSelectedRows().size();
    }

    default List<Integer> getSelectedRows() {
        return List.of();
    }

    default int getRow() {
        return getSelectedRows().isEmpty() ? -1 : getSelectedRows().getFirst();
    }

    default List<Integer> getSelectedColumns() {
        return List.of();
    }

    default int getColumn() {
        return getSelectedColumns().isEmpty() ? -1 : getSelectedColumns().getFirst();
    }

    default Object getNewValue() {
        return null;
    }

    default Object getOldValue() {
        return null;
    }
}
