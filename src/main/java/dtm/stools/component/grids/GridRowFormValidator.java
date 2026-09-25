package dtm.stools.component.grids;

import java.util.Map;

@FunctionalInterface
public interface GridRowFormValidator<T> {
    void validate(T row, Map<String, Object> proposedValues) throws Exception;
}
