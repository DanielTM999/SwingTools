package dtm.stools.component.grids;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** A custom command shown in the row action column. */
public record GridRowAction<T>(String id, String label, Predicate<T> enabled, Consumer<T> handler) {
    public GridRowAction {
        Objects.requireNonNull(id);
        Objects.requireNonNull(label);
        Objects.requireNonNull(handler);
        if (id.isBlank() || label.isBlank()) throw new IllegalArgumentException("Action id and label must not be blank");
        if (enabled == null) enabled = row -> true;
    }

    public GridRowAction(String id, String label, Consumer<T> handler) {
        this(id, label, row -> true, handler);
    }
}
