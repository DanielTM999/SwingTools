package dtm.stools.component.grids;

import java.util.Map;

@FunctionalInterface
public interface GridRowSaveHandler<T> {
    void save(T row, Map<String, Object> previousValues, Map<String, Object> newValues) throws Exception;
}
