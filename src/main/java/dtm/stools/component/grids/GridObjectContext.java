package dtm.stools.component.grids;

/** Context supplied to a nested-object choice provider or factory. */
public record GridObjectContext<T>(T row, Object parent, String path, Object value, Class<?> type) {}
