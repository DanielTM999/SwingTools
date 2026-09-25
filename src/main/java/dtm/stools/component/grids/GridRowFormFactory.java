package dtm.stools.component.grids;

@FunctionalInterface
public interface GridRowFormFactory<T> {
    GridRowForm create(T row, GridView<T> grid);
}
