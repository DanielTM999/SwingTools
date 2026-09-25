package dtm.stools.component.grids;

/** Supplies conditional cell styles without replacing the Swing renderer. */
@FunctionalInterface
public interface GridStyleResolver<T> {
    GridCellStyle resolve(T rowObject, String fieldName, Object value);
}
