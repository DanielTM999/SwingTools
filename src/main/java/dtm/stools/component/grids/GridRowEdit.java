package dtm.stools.component.grids;

import java.util.Map;

/** Values reported after a row form is saved successfully. */
public record GridRowEdit<T>(T row, Map<String, Object> previousValues, Map<String, Object> newValues) {}
