package dtm.stools.component.grids;

import javax.swing.JComponent;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/** A form and the values it proposes, keyed by annotated field name. */
public record GridRowForm(JComponent component, Supplier<Map<String, Object>> valueSupplier) {
    public GridRowForm {
        Objects.requireNonNull(component);
        Objects.requireNonNull(valueSupplier);
    }

    public Map<String, Object> values() { return Objects.requireNonNull(valueSupplier.get()); }
}
