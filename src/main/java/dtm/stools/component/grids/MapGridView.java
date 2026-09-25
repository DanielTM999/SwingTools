package dtm.stools.component.grids;

import dtm.stools.component.grids.model.ColumnDefinition;
import dtm.stools.component.grids.model.MapTableModel;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MapGridView extends GridView<Map<String, Object>> {

    public MapGridView() { this(List.of()); }

    public MapGridView(String... keys) { this(columnsOf(keys)); }

    public MapGridView(List<ColumnDefinition> columns) { this(columns, TableGridMode.BATCH); }

    public MapGridView(TableGridMode mode) { this(List.of(), mode); }

    public MapGridView(List<ColumnDefinition> columns, TableGridMode mode) {
        super(MapTableModel.rowClass(), mode, allowEdit -> new MapTableModel(List.copyOf(Objects.requireNonNull(columns, "columns")), allowEdit));
    }

    private static List<ColumnDefinition> columnsOf(String... keys) {
        return Arrays.stream(Objects.requireNonNull(keys, "keys")).map(key -> ColumnDefinition.builder().key(key).build()).toList();
    }
}
