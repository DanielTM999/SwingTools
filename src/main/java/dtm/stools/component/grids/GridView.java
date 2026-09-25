package dtm.stools.component.grids;

import dtm.stools.component.events.EventGridView;
import dtm.stools.component.feedback.pagination.PaginationPanel;
import dtm.stools.component.grids.annotations.GridColumn;
import dtm.stools.component.grids.event.EventGrid;
import dtm.stools.component.grids.model.ColumnDefinition;
import dtm.stools.component.grids.model.GridTableModel;
import dtm.stools.component.grids.model.MapTableModel;
import dtm.stools.component.grids.model.ReflectionTableModel;
import dtm.stools.component.inputfields.selectfield.DropdownField;
import dtm.stools.component.popup.ModernComponentDialog;
import dtm.stools.configs.UiTokens;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.Collator;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Function;
import java.util.function.Supplier;

public class GridView<T> extends DataTableListener {
    private record RowEntry<T>(int sourceIndex, T item, boolean detail) {
        RowEntry(int sourceIndex, T item) { this(sourceIndex, item, false); }
    }
    private record FieldWrite(String path, int rootColumn, Object parent, Field field,
                              Object previous, Object next) {}

    private final Class<T> modelClass;
    private GridTableModel<T> currentModel;
    private List<T> fullDataList = new ArrayList<>();
    private List<RowEntry<T>> visibleRows = List.of();
    private int filteredItems;
    private TableGridMode gridMode;
    private boolean allowEdit;
    private boolean cellEditingEnabled = true;
    private final Set<String> blockedEditColumns = new HashSet<>();
    private final Set<T> blockedEditRows = Collections.newSetFromMap(new IdentityHashMap<>());
    private final IdentityHashMap<T, Set<String>> blockedEditCells = new IdentityHashMap<>();
    private boolean paginationEnabled;
    private int pageSize = 20;
    private List<Integer> pageSizeOptions = List.of(10, 20, 50, 100, 125, 150, 175, 200);
    private int currentPage = 1;
    private String sortField;
    private SortOrder sortOrder = SortOrder.UNSORTED;
    private final Map<String, Predicate<Object>> columnFilters = new LinkedHashMap<>();
    private final Map<String, String> textFilters = new HashMap<>();
    private GridStyle gridStyle = GridStyle.standard();
    private final Map<String, GridCellStyle> columnStyles = new HashMap<>();
    private final IdentityHashMap<T, GridCellStyle> rowStyles = new IdentityHashMap<>();
    private final IdentityHashMap<T, Map<String, GridCellStyle>> cellStyles = new IdentityHashMap<>();
    private GridStyleResolver<T> styleResolver = (item, field, value) -> null;
    private final Map<Class<?>, TableCellRenderer> customDefaultRenderers = new HashMap<>();
    private final Map<Class<?>, TableCellEditor> customDefaultEditors = new HashMap<>();
    private JComponent paginationPanel;
    private PaginationPanel pageControl;
    private JComboBox<Integer> pageSizeControl;
    private JLabel pageSummary;
    private boolean updatingPager;
    private String emptyText = "Nenhum registro";
    private GridRowFormMode rowFormMode = GridRowFormMode.OFF;
    private List<GridRowAction<T>> rowActions = List.of();
    private GridRowFormFactory<T> rowFormFactory;
    private GridRowFormValidator<T> rowFormValidator = (row, values) -> {};
    private GridRowSaveHandler<T> rowSaveHandler = (row, previous, values) -> {};
    private int nestedEditDepth = -1;
    private final Map<String, Function<GridObjectContext<T>, ? extends Collection<?>>> objectChoices = new HashMap<>();
    private final Map<String, Function<GridObjectContext<T>, ?>> objectFactories = new HashMap<>();
    private T inlineItem;
    private JPanel inlinePanel;
    private int inlineRow = -1;
    private boolean rebuildingView;

    public GridView(Class<T> modelClass) { this(modelClass, TableGridMode.BATCH); }

    public GridView(Class<T> modelClass, TableGridMode mode) {
        this(modelClass, mode, allowEdit -> new ReflectionTableModel<>(List.of(), modelClass, allowEdit));
    }

    protected GridView(Class<T> modelClass, TableGridMode mode, Function<Supplier<Boolean>, GridTableModel<T>> modelFactory) {
        this.modelClass = Objects.requireNonNull(modelClass, "modelClass");
        setAutoCreateColumnsFromModel(false);
        currentModel = modelFactory.apply(this::isAllowEdit);
        currentModel.setEditListener(this::edited);
        super.setModel(currentModel);
        rebuildColumns();
        setGridMode(mode);
        applyGridStyle();
        installEvents();
        installRowActionClick();
        setTableHeaderToolTip();
    }

    public static MapGridView ofMaps() { return new MapGridView(); }

    public static MapGridView ofMaps(String... keys) { return new MapGridView(keys); }

    public static MapGridView ofMaps(List<ColumnDefinition> columns) { return new MapGridView(columns); }

    public static MapGridView ofMaps(List<ColumnDefinition> columns, TableGridMode mode) {
        return new MapGridView(columns, mode);
    }

    public boolean isStructured() { return currentModel instanceof ReflectionTableModel<?>; }

    @Override public void setModel(TableModel dataModel) {
        if (modelClass == null) { super.setModel(dataModel); return; }
        if (!(dataModel instanceof GridTableModel<?> model) || model.getItemClass() != modelClass)
            throw new IllegalArgumentException("GridView requires a GridTableModel for " + modelClass.getName());
        @SuppressWarnings("unchecked") GridTableModel<T> typed = (GridTableModel<T>) model;
        if (typed == currentModel) return;
        super.setModel(typed);
        currentModel = typed;
        currentModel.setEditListener(this::edited);
        fullDataList = new ArrayList<>(typed.getDataList());
        rowStyles.clear(); cellStyles.clear();
        retainKnownKeys();
        rebuildColumns();
        rebuildView();
    }

    @Override public void setDefaultRenderer(Class<?> columnClass, TableCellRenderer renderer) {
        super.setDefaultRenderer(columnClass, renderer);
        if (customDefaultRenderers != null) {
            if (renderer == null) customDefaultRenderers.remove(columnClass);
            else customDefaultRenderers.put(columnClass, renderer);
        }
    }

    @Override public void setDefaultEditor(Class<?> columnClass, TableCellEditor editor) {
        super.setDefaultEditor(columnClass, editor);
        if (customDefaultEditors != null) {
            if (editor == null) customDefaultEditors.remove(columnClass);
            else customDefaultEditors.put(columnClass, editor);
        }
    }

    @Override public void updateUI() {
        super.updateUI();
        if (gridStyle != null) applyGridStyle();
    }

    private void rebuildColumns() {
        TableColumnModel columns = getColumnModel();
        while (columns.getColumnCount() > 0) columns.removeColumn(columns.getColumn(0));
        for (int i = 0; i < currentModel.getColumnCount(); i++) {
            ColumnDefinition definition = currentModel.getColumnDefinition(i);
            if (!definition.isVisible()) continue;
            TableColumn column = new TableColumn(i);
            column.setHeaderValue(definition.getName());
            column.setPreferredWidth(Math.max(24, definition.getWidth()));
            columns.addColumn(column);
        }
        if (hasActionColumn()) {
            TableColumn actions = new TableColumn(currentModel.getColumnCount());
            actions.setHeaderValue("Ações");
            actions.setPreferredWidth(100);
            actions.setMinWidth(72);
            columns.addColumn(actions);
        }
    }

    private boolean hasActionColumn() { return rowFormMode != GridRowFormMode.OFF || !rowActions.isEmpty(); }
    private boolean isActionColumn(int viewColumn) {
        return hasActionColumn() && viewColumn >= 0 && viewColumn < getColumnCount()
                && convertColumnIndexToModel(viewColumn) == currentModel.getColumnCount();
    }

    @Override public Object getValueAt(int row, int column) {
        return currentModel != null && isActionColumn(column) ? "" : super.getValueAt(row, column);
    }
    @Override public Class<?> getColumnClass(int column) {
        return currentModel != null && isActionColumn(column) ? String.class : super.getColumnClass(column);
    }
    @Override public boolean isCellEditable(int row, int column) {
        if (currentModel == null) return super.isCellEditable(row, column);
        if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount()
                || isActionColumn(column) || isDetailRow(row) || !cellEditingEnabled
                || !super.isCellEditable(row, column)) return false;
        T item = getRowObject(row);
        if (item == null || blockedEditRows.contains(item)) return false;
        String field = currentModel.getKeyForColumn(convertColumnIndexToModel(column));
        if (blockedEditColumns.contains(field) || blockedEditCells.getOrDefault(item, Set.of()).contains(field)) return false;
        ColumnDefinition definition = currentModel.getColumnDefinition(convertColumnIndexToModel(column));
        if (definition.getField() == null) return true;
        Class<?> type = targetField(modelClass, definition).getType();
        Object value = getValueAt(row, column);
        return !isObjectType(type) || objectChoices.containsKey(field)
                || objectFactories.containsKey(field)
                || (nestedEditDepth != 0 && !annotatedFields(value == null ? type : value.getClass()).isEmpty());
    }
    private boolean isDetailRow(int row) {
        return row >= 0 && row < visibleRows.size() && visibleRows.get(convertRowIndexToModel(row)).detail;
    }

    @Override public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        if (isDetailRow(rowIndex)) return;
        super.changeSelection(rowIndex, columnIndex, toggle, extend);
    }

    @Override public void doLayout() {
        super.doLayout();
        positionInlinePanel();
    }

    public TableGridMode getGridMode() { return gridMode; }
    public void setGridMode(TableGridMode mode) {
        gridMode = Objects.requireNonNullElse(mode, TableGridMode.BATCH);
        setSelectionMode(gridMode == TableGridMode.SINGLE ? ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    }
    public boolean isAllowEdit() { return allowEdit; }
    public void setAllowEdit(boolean allowEdit) {
        this.allowEdit = allowEdit;
        cancelEditingIfBlocked();
        if (!allowEdit) closeInlineForm();
        repaint();
    }
    /** Controls editing directly in table cells without disabling row forms. */
    public boolean isCellEditingEnabled() { return cellEditingEnabled; }
    public void setCellEditingEnabled(boolean enabled) {
        cellEditingEnabled = enabled;
        cancelEditingIfBlocked();
        repaint();
    }
    public void setColumnCellEditingEnabled(String fieldName, boolean enabled) {
        fieldIndex(fieldName);
        if (enabled) blockedEditColumns.remove(fieldName); else blockedEditColumns.add(fieldName);
        cancelEditingIfBlocked();
        repaint();
    }
    public void setRowCellEditingEnabled(T item, boolean enabled) {
        Objects.requireNonNull(item);
        if (enabled) blockedEditRows.remove(item); else blockedEditRows.add(item);
        cancelEditingIfBlocked();
        repaint();
    }
    public void setCellEditingEnabled(T item, String fieldName, boolean enabled) {
        Objects.requireNonNull(item);
        fieldIndex(fieldName);
        if (enabled) {
            Set<String> cells = blockedEditCells.get(item);
            if (cells != null) {
                cells.remove(fieldName);
                if (cells.isEmpty()) blockedEditCells.remove(item);
            }
        } else blockedEditCells.computeIfAbsent(item, key -> new HashSet<>()).add(fieldName);
        cancelEditingIfBlocked();
        repaint();
    }
    private void cancelEditingIfBlocked() {
        if (isEditing() && !isCellEditable(getEditingRow(), getEditingColumn())) getCellEditor().cancelCellEditing();
    }
    public GridRowFormMode getRowFormMode() { return rowFormMode; }
    public void setRowFormMode(GridRowFormMode mode) {
        Objects.requireNonNull(mode);
        if (rowFormMode == mode) return;
        rowFormMode = mode;
        clearInlinePanel();
        rebuildColumns();
        rebuildView();
    }
    public void setRowActions(List<GridRowAction<T>> actions) {
        rowActions = List.copyOf(Objects.requireNonNull(actions));
        rebuildColumns();
        rebuildView();
    }
    public List<GridRowAction<T>> getRowActions() { return rowActions; }
    public void setRowFormFactory(GridRowFormFactory<T> factory) { rowFormFactory = factory; }
    public void setRowFormValidator(GridRowFormValidator<T> validator) {
        rowFormValidator = validator == null ? (row, values) -> {} : validator;
    }
    public void setRowSaveHandler(GridRowSaveHandler<T> handler) {
        rowSaveHandler = handler == null ? (row, before, after) -> {} : handler;
    }
    /** -1 expands without a depth limit; 0 permits only root properties. */
    public int getNestedEditDepth() { return nestedEditDepth; }
    public void setNestedEditDepth(int depth) {
        if (depth < -1) throw new IllegalArgumentException("depth must be -1 or greater");
        nestedEditDepth = depth;
    }
    public void setObjectChoices(String path, Function<GridObjectContext<T>, ? extends Collection<?>> provider) {
        validateObjectPath(path);
        if (provider == null) objectChoices.remove(path); else objectChoices.put(path, provider);
    }
    public void setObjectFactory(String path, Function<GridObjectContext<T>, ?> factory) {
        validateObjectPath(path);
        if (factory == null) objectFactories.remove(path); else objectFactories.put(path, factory);
    }
    public boolean isPaginationEnabled() { return paginationEnabled; }
    public int getPageSize() { return pageSize; }
    public List<Integer> getPageSizeOptions() { return pageSizeOptions; }
    public int getCurrentPage() { return currentPage; }
    public int getTotalItems() { return fullDataList.size(); }
    public int getFilteredItems() { return filteredItems; }
    public int getTotalPages() { return Math.max(1, (filteredItems + pageSize - 1) / pageSize); }
    public boolean hasNextPage() { return currentPage < getTotalPages(); }
    public boolean hasPreviousPage() { return currentPage > 1; }
    public String getSortField() { return sortField; }
    public SortOrder getSortOrder() { return sortOrder; }
    public GridStyle getGridStyle() { return gridStyle; }
    public String getEmptyText() { return emptyText; }
    public void setEmptyText(String text) { emptyText = Objects.requireNonNullElse(text, ""); repaint(); }

    public void setDataSource(Collection<T> data) {
        clearInlinePanel();
        fullDataList = data == null ? new ArrayList<>() : new ArrayList<>(data);
        if (currentModel instanceof MapTableModel mapModel && mapModel.inferColumns(fullDataList)) {
            retainKnownKeys();
            rebuildColumns();
        }
        retainStylesForSource();
        currentPage = 1;
        rebuildView();
    }

    private void retainStylesForSource() {
        Set<T> present = Collections.newSetFromMap(new IdentityHashMap<>());
        present.addAll(fullDataList);
        rowStyles.keySet().removeIf(item -> !present.contains(item));
        cellStyles.keySet().removeIf(item -> !present.contains(item));
        blockedEditRows.removeIf(item -> !present.contains(item));
        blockedEditCells.keySet().removeIf(item -> !present.contains(item));
    }

    private void retainKnownKeys() {
        Predicate<String> missing = key -> currentModel.findColumnIndexByKey(key) < 0;
        columnFilters.keySet().removeIf(missing);
        textFilters.keySet().removeIf(missing);
        columnStyles.keySet().removeIf(missing);
        blockedEditColumns.removeIf(missing);
        cellStyles.values().forEach(cells -> cells.keySet().removeIf(missing));
        cellStyles.values().removeIf(Map::isEmpty);
        blockedEditCells.values().forEach(cells -> cells.removeIf(missing));
        blockedEditCells.values().removeIf(Set::isEmpty);
        if (sortField != null && missing.test(sortField)) { sortField = null; sortOrder = SortOrder.UNSORTED; }
    }

    public List<T> getDataSource() { return Collections.unmodifiableList(new ArrayList<>(fullDataList)); }

    /** Reapplies filters, ordering and pagination after row objects change outside the grid. */
    public void refreshData() { rebuildView(); }

    public void setPaginationEnabled(boolean enabled) { paginationEnabled = enabled; currentPage = 1; rebuildView(); }
    public void setPageSize(int size) {
        if (size <= 0) throw new IllegalArgumentException("pageSize must be positive");
        pageSize = size; currentPage = 1; rebuildView();
    }
    public void setPageSizeOptions(List<Integer> options) {
        Objects.requireNonNull(options);
        if (options.isEmpty() || options.stream().anyMatch(size -> size == null || size <= 0))
            throw new IllegalArgumentException("pageSizeOptions must contain positive sizes");
        pageSizeOptions = List.copyOf(new LinkedHashSet<>(options));
        if (!pageSizeOptions.contains(pageSize)) { pageSize = pageSizeOptions.getFirst(); currentPage = 1; rebuildView(); }
        updatePager();
    }
    public void goToPage(int page) { currentPage = Math.max(1, Math.min(page, getTotalPages())); rebuildView(); }
    public void nextPage() { goToPage(currentPage + 1); }
    public void previousPage() { goToPage(currentPage - 1); }

    public void setColumnTextFilter(String fieldName, String text) {
        fieldIndex(fieldName);
        String query = text == null ? "" : text.strip();
        if (query.isEmpty()) { clearColumnFilter(fieldName); return; }
        textFilters.put(fieldName, query);
        String folded = query.toLowerCase(Locale.ROOT);
        columnFilters.put(fieldName, value -> (value == null ? "" : String.valueOf(unwrap(value))).toLowerCase(Locale.ROOT).contains(folded));
        currentPage = 1; rebuildView();
    }
    public void setColumnFilter(String fieldName, Predicate<Object> filter) {
        fieldIndex(fieldName);
        if (filter == null) { clearColumnFilter(fieldName); return; }
        textFilters.remove(fieldName);
        columnFilters.put(fieldName, filter);
        currentPage = 1; rebuildView();
    }
    public void clearColumnFilter(String fieldName) {
        fieldIndex(fieldName);
        columnFilters.remove(fieldName); textFilters.remove(fieldName);
        currentPage = 1; rebuildView();
    }
    public void clearFilters() { columnFilters.clear(); textFilters.clear(); currentPage = 1; rebuildView(); }
    public void setSort(String fieldName, SortOrder order) {
        fieldIndex(fieldName);
        sortOrder = Objects.requireNonNull(order);
        sortField = order == SortOrder.UNSORTED ? null : fieldName;
        currentPage = 1; rebuildView();
    }
    public void clearSort() { sortField = null; sortOrder = SortOrder.UNSORTED; currentPage = 1; rebuildView(); }

    private int fieldIndex(String fieldName) {
        Objects.requireNonNull(fieldName, "fieldName");
        int index = currentModel.findColumnIndexByKey(fieldName);
        if (index < 0) throw new IllegalArgumentException("Unknown grid field: " + fieldName);
        return index;
    }

    private static Object unwrap(Object value) {
        return value instanceof DropdownField dropdown ? dropdown.getSelectedItem() : value;
    }

    private Object fieldValue(T item, int column) {
        return item == null ? null : currentModel.readValue(item, column);
    }

    private void rebuildView() {
        if (currentModel == null) return;
        Set<T> selected = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int viewRow : getSelectedRows()) {
            T item = getRowObject(viewRow);
            if (item != null) selected.add(item);
        }
        List<RowEntry<T>> rows = new ArrayList<>();
        for (int source = 0; source < fullDataList.size(); source++) {
            T item = fullDataList.get(source);
            boolean included = true;
            for (var filter : columnFilters.entrySet()) {
                if (!filter.getValue().test(fieldValue(item, fieldIndex(filter.getKey())))) { included = false; break; }
            }
            if (included) rows.add(new RowEntry<>(source, item));
        }
        filteredItems = rows.size();
        if (sortField != null) {
            int column = fieldIndex(sortField);
            Collator collator = Collator.getInstance(getLocale());
            rows.sort((a, b) -> {
                int result = compareValues(fieldValue(a.item, column), fieldValue(b.item, column), collator);
                return result == 0 ? Integer.compare(a.sourceIndex, b.sourceIndex)
                        : sortOrder == SortOrder.DESCENDING ? -result : result;
            });
        }
        currentPage = Math.max(1, Math.min(currentPage, getTotalPages()));
        int from = paginationEnabled ? Math.min(rows.size(), (currentPage - 1) * pageSize) : 0;
        int to = paginationEnabled ? Math.min(rows.size(), from + pageSize) : rows.size();
        List<RowEntry<T>> pageRows = new ArrayList<>(rows.subList(from, to));
        inlineRow = -1;
        if (inlineItem != null) {
            for (int index = 0; index < pageRows.size(); index++) {
                if (pageRows.get(index).item == inlineItem) {
                    inlineRow = index + 1;
                    pageRows.add(inlineRow, new RowEntry<>(pageRows.get(index).sourceIndex, inlineItem, true));
                    break;
                }
            }
            if (inlineRow < 0) clearInlinePanel();
        }
        visibleRows = List.copyOf(pageRows);
        rebuildingView = true;
        try {
            currentModel.setDataList(visibleRows.stream().map(entry -> entry.detail ? null : entry.item).toList());
            clearSelection();
            for (int index = 0; index < visibleRows.size(); index++)
                if (!visibleRows.get(index).detail && selected.contains(visibleRows.get(index).item))
                    addRowSelectionInterval(index, index);
        } finally { rebuildingView = false; }
        positionInlinePanel();
        updatePager();
        if (getTableHeader() != null) getTableHeader().repaint();
        repaint();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int compareValues(Object left, Object right, Collator collator) {
        if (left == right) return 0;
        if (left == null) return 1;
        if (right == null) return -1;
        if (left instanceof Number a && right instanceof Number b) return Double.compare(a.doubleValue(), b.doubleValue());
        if (left instanceof String a && right instanceof String b) return collator.compare(a, b);
        if (left instanceof Comparable comparable && left.getClass().isInstance(right)) return comparable.compareTo(right);
        return collator.compare(String.valueOf(left), String.valueOf(right));
    }

    public GridRow<T> getRow(int viewRow) {
        if (viewRow < 0 || viewRow >= getRowCount() || isDetailRow(viewRow)) return GridRow.empty();
        RowEntry<T> entry = visibleRows.get(convertRowIndexToModel(viewRow));
        List<String> keys = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (int col = 0; col < getColumnCount(); col++) {
            if (isActionColumn(col)) continue;
            ColumnDefinition definition = currentModel.getColumnDefinition(convertColumnIndexToModel(col));
            keys.add(definition.getKey());
            names.add(definition.getName());
            values.add(unwrap(getValueAt(viewRow, col)));
        }
        Map<String, Object> extras = new LinkedHashMap<>();
        for (int column = 0; column < currentModel.getColumnCount(); column++) {
            String key = currentModel.getKeyForColumn(column);
            if (!keys.contains(key)) extras.put(key, fieldValue(entry.item, column));
        }
        if (entry.item instanceof Map<?, ?> map)
            for (Map.Entry<?, ?> cell : map.entrySet()) {
                String key = String.valueOf(cell.getKey());
                if (!keys.contains(key) && !extras.containsKey(key)) extras.put(key, cell.getValue());
            }
        return new GridRow<>(entry.item, viewRow, entry.sourceIndex, keys, names, values, extras);
    }

    public GridRow<T> getSelectedGridRow() {
        int viewRow = getSelectedRow();
        return viewRow < 0 || isDetailRow(viewRow) ? null : getRow(viewRow);
    }

    public List<GridRow<T>> getSelectedGridRows() {
        List<GridRow<T>> rows = new ArrayList<>();
        for (int viewRow : getSelectedRows()) if (!isDetailRow(viewRow)) rows.add(getRow(viewRow));
        return rows;
    }

    public T getSelectedRowObject() {
        int viewRow = getSelectedRow();
        return viewRow < 0 ? null : getRowObject(viewRow);
    }
    public T getRowObject(int viewRow) {
        if (viewRow < 0 || viewRow >= getRowCount()) return null;
        RowEntry<T> entry = visibleRows.get(convertRowIndexToModel(viewRow));
        return entry.detail ? null : entry.item;
    }

    @Override public TableCellEditor getCellEditor(int row, int column) {
        TableCellEditor custom = getColumnModel().getColumn(column).getCellEditor();
        if (custom != null) return custom;
        if (hasCustomEditor(getColumnClass(column))) return super.getCellEditor(row, column);
        Object value = getValueAt(row, column);
        if (value instanceof DropdownField dropdown) return new DefaultCellEditor(dropdown);
        if (value instanceof Collection<?> collection) return new DefaultCellEditor(new DropdownField(collection));
        if (value != null && value.getClass().isArray()) {
            Object[] array = new Object[Array.getLength(value)];
            for (int i = 0; i < array.length; i++) array[i] = Array.get(value, i);
            return new DefaultCellEditor(new DropdownField(array));
        }
        ColumnDefinition definition = currentModel.getColumnDefinition(convertColumnIndexToModel(column));
        if (definition.getField() != null && isObjectType(targetField(modelClass, definition).getType()))
            return new ObjectCellEditor(getRowObject(row), definition);
        return super.getCellEditor(row, column);
    }

    private final class ObjectCellEditor extends AbstractCellEditor implements TableCellEditor {
        private final T row;
        private final ColumnDefinition definition;

        private ObjectCellEditor(T row, ColumnDefinition definition) {
            this.row = row;
            this.definition = definition;
        }

        @Override public Object getCellEditorValue() { return null; }

        @Override public Component getTableCellEditorComponent(JTable table, Object value,
                                                                 boolean selected, int viewRow, int viewColumn) {
            JButton button = new JButton("Editar objeto...");
            SwingUtilities.invokeLater(() -> {
                cancelCellEditing();
                if (sourceIndexOf(row) < 0 || !allowEdit) return;
                GridRowForm form = createObjectCellForm(row, definition);
                ModernComponentDialog.builder(Boolean.class).parent(GridView.this)
                        .title("Editar " + definition.getName()).showIcon(false)
                        .confirmText("Salvar").cancelText("Cancelar")
                        .component(dialogFormComponent(form.component())).result(context -> Boolean.TRUE)
                        .onSubmit(context -> saveRowForm(row, form)).show();
            });
            return button;
        }
    }

    private boolean hasCustomEditor(Class<?> type) {
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass())
            if (customDefaultEditors.containsKey(cursor)) return true;
        return false;
    }

    private boolean hasCustomRenderer(Class<?> type) {
        for (Class<?> cursor = type; cursor != null; cursor = cursor.getSuperclass())
            if (customDefaultRenderers.containsKey(cursor)) return true;
        return false;
    }

    private void installRowActionClick() {
        addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) return;
                int row = rowAtPoint(event.getPoint());
                int column = columnAtPoint(event.getPoint());
                if (row >= 0 && isActionColumn(column) && !isDetailRow(row)) showRowActions(row, event.getX(), event.getY());
            }
        });
    }

    private void showRowActions(int viewRow, int x, int y) {
        T row = getRowObject(viewRow);
        if (row == null) return;
        int count = rowActions.size() + (rowFormMode == GridRowFormMode.OFF ? 0 : 1);
        if (count == 1) {
            if (rowFormMode != GridRowFormMode.OFF) openRowForm(viewRow);
            else if (rowActions.getFirst().enabled().test(row)) runRowAction(rowActions.getFirst(), row);
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        if (rowFormMode != GridRowFormMode.OFF) {
            JMenuItem edit = new JMenuItem("Editar");
            edit.setEnabled(allowEdit);
            edit.addActionListener(event -> openRowForm(viewRow));
            menu.add(edit);
        }
        for (GridRowAction<T> action : rowActions) {
            JMenuItem item = new JMenuItem(action.label());
            item.setEnabled(action.enabled().test(row));
            item.addActionListener(event -> runRowAction(action, row));
            menu.add(item);
        }
        menu.show(this, x, y);
    }

    private void runRowAction(GridRowAction<T> action, T row) {
        action.handler().accept(row);
        dispachEvent(EventGridView.ROW_ACTION, action.id());
    }

    /** Opens the edit form for a displayed data row. Returns false when editing is unavailable. */
    public boolean openRowForm(int viewRow) {
        if (!allowEdit || rowFormMode == GridRowFormMode.OFF) return false;
        T row = getRowObject(viewRow);
        if (row == null) return false;
        GridRowForm form = rowFormFactory == null ? createDefaultRowForm(row)
                : Objects.requireNonNull(rowFormFactory.create(row, this), "row form");
        if (rowFormMode == GridRowFormMode.DIALOG) {
            clearInlinePanel();
            ModernComponentDialog.builder(Boolean.class).parent(this).title("Editar linha")
                    .showIcon(false).confirmText("Salvar").cancelText("Cancelar")
                    .component(dialogFormComponent(form.component())).result(context -> Boolean.TRUE)
                    .onSubmit(context -> saveRowForm(row, form)).show();
        } else showInlineRowForm(row, form);
        return true;
    }

    private GridRowForm createDefaultRowForm(T row) {
        ModernComponentDialog.FormPanel panel = new ModernComponentDialog.FormPanel();
        List<java.util.function.Consumer<Map<String, Object>>> readers = new ArrayList<>();
        Set<Object> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        ancestors.add(row);
        for (int column = 0; column < currentModel.getColumnCount(); column++) {
            ColumnDefinition definition = currentModel.getColumnDefinition(column);
            addFormField(panel, row, row, definition, definition.getKey(), 0, ancestors, readers);
        }
        return new GridRowForm(panel, () -> {
            Map<String, Object> values = new LinkedHashMap<>();
            readers.forEach(reader -> reader.accept(values));
            return values;
        });
    }

    private GridRowForm createObjectCellForm(T row, ColumnDefinition definition) {
        ModernComponentDialog.FormPanel panel = new ModernComponentDialog.FormPanel();
        List<java.util.function.Consumer<Map<String, Object>>> readers = new ArrayList<>();
        Set<Object> ancestors = Collections.newSetFromMap(new IdentityHashMap<>());
        ancestors.add(row);
        addFormField(panel, row, row, definition, definition.getKey(), 0, ancestors, readers);
        return new GridRowForm(panel, () -> {
            Map<String, Object> values = new LinkedHashMap<>();
            readers.forEach(reader -> reader.accept(values));
            return values;
        });
    }

    private static JComponent dialogFormComponent(JComponent component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(480, Math.min(420, Math.max(170, component.getPreferredSize().height + 12))));
        return scroll;
    }

    private void addFormField(ModernComponentDialog.FormPanel panel, T row, Object parent,
                              ColumnDefinition definition, String path, int parentDepth, Set<Object> ancestors,
                              List<java.util.function.Consumer<Map<String, Object>>> readers) {
        if (!definition.isVisible()) return;
        if (definition.getField() == null) {
            addKeyFormField(panel, row, definition, path, readers);
            return;
        }
        Field field = targetField(parent.getClass(), definition);
        Object value = readField(field, parent);
        Class<?> type = field.getType();
        if (!definition.isEditable() || Modifier.isFinal(field.getModifiers())) {
            panel.field(path, definition.getName(), new JLabel(Objects.toString(value, "")));
            return;
        }
        if (isObjectType(type)) {
            if (annotatedFields(value == null ? type : value.getClass()).isEmpty()
                    && !objectChoices.containsKey(path) && !objectFactories.containsKey(path)) {
                panel.field(path, definition.getName(), new JLabel(Objects.toString(value, "")));
                return;
            }
            ObjectFormNode node = new ObjectFormNode(row, parent, path, type, value, parentDepth + 1, ancestors);
            panel.field(path, definition.getName(), node.panel);
            readers.add(node::collect);
            return;
        }
        java.util.function.Supplier<Object> reader = leafEditor(panel, path, definition.getName(), type, value);
        if (reader != null) readers.add(values -> values.put(path, reader.get()));
    }

    private void addKeyFormField(ModernComponentDialog.FormPanel panel, T row, ColumnDefinition definition, String path,
                                 List<java.util.function.Consumer<Map<String, Object>>> readers) {
        Object value = fieldValue(row, currentModel.findColumnIndexByKey(definition.getKey()));
        if (!definition.isEditable()) {
            panel.field(path, definition.getName(), new JLabel(Objects.toString(value, "")));
            return;
        }
        Class<?> type = definition.getType() != Object.class ? definition.getType()
                : value == null ? String.class : value.getClass();
        java.util.function.Supplier<Object> reader = leafEditor(panel, path, definition.getName(), type, value);
        if (reader != null) readers.add(values -> values.put(path, reader.get()));
    }

    private java.util.function.Supplier<Object> leafEditor(ModernComponentDialog.FormPanel panel,
                                                            String path, String label, Class<?> type, Object value) {
        if (type == Boolean.class) {
            JComboBox<Boolean> choice = new JComboBox<>(new Boolean[]{null, Boolean.TRUE, Boolean.FALSE});
            choice.setSelectedItem(value);
            panel.field(path, label, choice);
            return choice::getSelectedItem;
        }
        if (type == boolean.class) {
            JCheckBox check = new JCheckBox();
            check.setSelected(Boolean.TRUE.equals(value));
            panel.field(path, label, check);
            return check::isSelected;
        }
        if (type.isEnum()) {
            JComboBox<Object> choice = new JComboBox<>(type.getEnumConstants());
            choice.setSelectedItem(value);
            panel.field(path, label, choice);
            return choice::getSelectedItem;
        }
        if (type == String.class || type == char.class || type == Character.class
                || type.isPrimitive() || type == Byte.class || type == Short.class || type == Integer.class
                || type == Long.class || type == Float.class || type == Double.class
                || type == java.math.BigDecimal.class || type == java.math.BigInteger.class) {
            JTextField text = new JTextField(Objects.toString(value, ""), 24);
            panel.field(path, label, text);
            return text::getText;
        }
        panel.field(path, label, new JLabel(Objects.toString(value, "")));
        return null;
    }

    private static Object readField(Field field, Object parent) {
        try { return field.get(parent); }
        catch (IllegalAccessException error) { throw new IllegalStateException(error); }
    }

    private Field targetField(ColumnDefinition definition) { return targetField(modelClass, definition); }

    private static Field targetField(Class<?> owner, ColumnDefinition definition) {
        String setterRef = definition.getNameToSetter();
        if (setterRef == null || setterRef.isBlank()) return definition.getField();
        try {
            Field field = owner.getDeclaredField(setterRef);
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException ignored) { return definition.getField(); }
    }

    private static List<ColumnDefinition> annotatedFields(Class<?> type) {
        List<ColumnDefinition> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            GridColumn annotation = field.getAnnotation(GridColumn.class);
            if (annotation != null) fields.add(new ColumnDefinition(field, annotation));
        }
        fields.sort(Comparator.comparingInt(ColumnDefinition::getOrder));
        return fields;
    }

    private static boolean isObjectType(Class<?> type) {
        return !type.isPrimitive() && !type.isEnum() && !type.isArray()
                && !type.getName().startsWith("java.")
                && !Collection.class.isAssignableFrom(type) && !Map.class.isAssignableFrom(type);
    }

    private ColumnDefinition annotatedField(Class<?> type, String name) {
        for (ColumnDefinition definition : annotatedFields(type))
            if (definition.getField().getName().equals(name)) return definition;
        throw new IllegalArgumentException("Campo não anotado: " + type.getSimpleName() + "." + name);
    }

    private void validateObjectPath(String path) {
        Objects.requireNonNull(path, "path");
        if (!isStructured()) throw new IllegalStateException("Objetos aninhados exigem um GridView tipado");
        if (path.isBlank()) throw new IllegalArgumentException("Caminho vazio");
        Class<?> type = modelClass;
        String[] parts = path.split("\\.", -1);
        for (String part : parts) {
            ColumnDefinition definition = annotatedField(type, part);
            if (!definition.isVisible() || !definition.isEditable())
                throw new IllegalArgumentException("Campo não editável: " + path);
            type = targetField(type, definition).getType();
        }
        if (!isObjectType(type)) throw new IllegalArgumentException("Campo não contém objeto: " + path);
    }

    private static final class ObjectOption {
        final Object value;
        ObjectOption(Object value) { this.value = value; }
        @Override public String toString() { return value == null ? "(nenhum)" : String.valueOf(value); }
    }

    private final class ObjectFormNode {
        final T row;
        final Object parent;
        final String path;
        final Class<?> type;
        final Object original;
        final int depth;
        final Set<Object> ancestors;
        final JPanel panel = new JPanel(new BorderLayout(4, 4));
        final JPanel children = new JPanel(new BorderLayout());
        final JButton expandButton = new JButton("Propriedades");
        final JButton createButton = new JButton("Criar");
        final List<java.util.function.Consumer<Map<String, Object>>> childReaders = new ArrayList<>();
        final JLabel cycleLabel = new JLabel("Referência cíclica");
        Object selected;
        boolean expanded;
        JLabel summary;
        JComboBox<ObjectOption> choices;

        ObjectFormNode(T row, Object parent, String path, Class<?> type, Object value,
                       int depth, Set<Object> ancestors) {
            this.row = row;
            this.parent = parent;
            this.path = path;
            this.type = type;
            this.original = value;
            this.selected = value;
            this.depth = depth;
            this.ancestors = ancestors;
            JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEADING, 6, 0));
            Function<GridObjectContext<T>, ? extends Collection<?>> provider = objectChoices.get(path);
            if (provider != null) {
                choices = new JComboBox<>();
                choices.addItem(new ObjectOption(null));
                ObjectOption initial = null;
                if (value != null) {
                    initial = new ObjectOption(value);
                    choices.addItem(initial);
                }
                Collection<?> options = provider.apply(context(value));
                if (options != null) for (Object option : options) {
                    if (option != null && !type.isInstance(option))
                        throw new IllegalArgumentException("Opção incompatível com " + path);
                    boolean exists = false;
                    for (int index = 0; index < choices.getItemCount(); index++)
                        if (choices.getItemAt(index).value == option) { exists = true; break; }
                    if (!exists) choices.addItem(new ObjectOption(option));
                }
                choices.setSelectedItem(initial == null ? choices.getItemAt(0) : initial);
                choices.addActionListener(event -> {
                    ObjectOption option = (ObjectOption) choices.getSelectedItem();
                    select(option == null ? null : option.value);
                });
                controls.add(choices);
            } else {
                summary = new JLabel(Objects.toString(value, "(nenhum)"));
                controls.add(summary);
            }
            createButton.addActionListener(event -> {
                Object draft = createObject();
                if (choices != null) {
                    ObjectOption option = new ObjectOption(draft);
                    choices.addItem(option);
                    choices.setSelectedItem(option);
                } else select(draft);
                expanded = true;
                rebuildChildren();
            });
            expandButton.addActionListener(event -> {
                expanded = !expanded;
                rebuildChildren();
            });
            controls.add(createButton);
            controls.add(expandButton);
            panel.add(controls, BorderLayout.NORTH);
            panel.add(children, BorderLayout.CENTER);
            updateControls();
        }

        GridObjectContext<T> context(Object value) { return new GridObjectContext<>(row, parent, path, value, type); }

        private Object createObject() { return createNestedObject(row, parent, path, type); }

        private void select(Object value) {
            if (selected == value) return;
            selected = value;
            expanded = false;
            childReaders.clear();
            children.removeAll();
            updateControls();
            panel.revalidate();
            panel.repaint();
        }

        private void updateControls() {
            if (summary != null) summary.setText(Objects.toString(selected, "(nenhum)"));
            createButton.setVisible(selected == null);
            boolean canExpand = selected != null && (nestedEditDepth < 0 || depth <= nestedEditDepth)
                    && !ancestors.contains(selected) && !annotatedFields(selected.getClass()).isEmpty();
            expandButton.setVisible(canExpand);
            expandButton.setText(expanded ? "Recolher" : "Propriedades");
            if (selected != null && ancestors.contains(selected)) {
                children.removeAll();
                children.add(cycleLabel, BorderLayout.CENTER);
            }
        }

        private void rebuildChildren() {
            children.removeAll();
            childReaders.clear();
            updateControls();
            if (expanded && selected != null && !ancestors.contains(selected)
                    && (nestedEditDepth < 0 || depth <= nestedEditDepth)) {
                Set<Object> next = Collections.newSetFromMap(new IdentityHashMap<>());
                next.addAll(ancestors);
                next.add(selected);
                ModernComponentDialog.FormPanel properties = new ModernComponentDialog.FormPanel();
                for (ColumnDefinition definition : annotatedFields(selected.getClass()))
                    addFormField(properties, row, selected, definition,
                            path + "." + definition.getField().getName(), depth, next, childReaders);
                children.add(properties, BorderLayout.CENTER);
            }
            updateControls();
            panel.revalidate();
            panel.repaint();
        }

        void collect(Map<String, Object> values) {
            if (selected != original) values.put(path, selected);
            if (expanded) childReaders.forEach(reader -> reader.accept(values));
        }
    }

    private Object createNestedObject(T row, Object parent, String path, Class<?> type) {
        Object result;
        Function<GridObjectContext<T>, ?> factory = objectFactories.get(path);
        if (factory != null) result = factory.apply(new GridObjectContext<>(row, parent, path, null, type));
        else {
            try {
                Constructor<?> constructor = type.getDeclaredConstructor();
                constructor.setAccessible(true);
                result = constructor.newInstance();
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("Sem fábrica ou construtor vazio para " + path + " (" + type.getName() + ")", error);
            }
        }
        if (result == null || !type.isInstance(result))
            throw new IllegalStateException("Fábrica retornou tipo inválido para " + path);
        return result;
    }

    private void saveRowForm(T row, GridRowForm form) throws Exception {
        if (!allowEdit) throw new IllegalStateException("A edição está desabilitada");
        if (sourceIndexOf(row) < 0) throw new IllegalStateException("A linha não está mais na fonte de dados");
        List<FieldWrite> writes = new ArrayList<>();
        Map<String, Object> proposed = new LinkedHashMap<>();
        Map<String, Object> previous = new LinkedHashMap<>();
        Map<String, Object> stagedObjects = new HashMap<>();
        List<Map.Entry<String, Object>> entries = new ArrayList<>(form.values().entrySet());
        entries.sort(Comparator.comparingInt(entry -> entry.getKey().split("\\.", -1).length));
        for (var entry : entries) {
            String path = entry.getKey();
            String[] parts = path.split("\\.", -1);
            Object parent = row;
            Class<?> owner = modelClass;
            StringBuilder prefix = new StringBuilder();
            int rootColumn = fieldIndex(parts[0]);
            ColumnDefinition rootDefinition = currentModel.getColumnDefinition(rootColumn);
            if (rootDefinition.getField() == null) {
                if (parts.length > 1) throw new IllegalArgumentException("Caminho aninhado não suportado: " + path);
                if (!rootDefinition.isVisible() || !rootDefinition.isEditable())
                    throw new IllegalArgumentException("Campo somente leitura: " + path);
                Object old = currentModel.readValue(row, rootColumn);
                Object converted = currentModel.convertForColumn(row, rootColumn, entry.getValue());
                writes.add(new FieldWrite(path, rootColumn, row, null, old, converted));
                proposed.put(path, converted);
                previous.put(path, old);
                continue;
            }
            for (int index = 0; index < parts.length; index++) {
                if (prefix.length() != 0) prefix.append('.');
                prefix.append(parts[index]);
                String currentPath = prefix.toString();
                ColumnDefinition definition = annotatedField(owner, parts[index]);
                if (!definition.isVisible() || !definition.isEditable())
                    throw new IllegalArgumentException("Campo somente leitura: " + currentPath);
                Field field = targetField(owner, definition);
                if (Modifier.isFinal(field.getModifiers()))
                    throw new IllegalArgumentException("Campo imutável: " + currentPath);
                if (index == parts.length - 1) {
                    Object converted = GridTableModel.convertValue(entry.getValue(), field.getType());
                    if (converted == null && field.getType().isPrimitive())
                        throw new IllegalArgumentException("Campo obrigatório: " + currentPath);
                    Object old = readField(field, parent);
                    writes.add(new FieldWrite(path, rootColumn, parent, field, old, converted));
                    proposed.put(path, converted);
                    previous.put(path, old);
                    stagedObjects.put(path, converted);
                } else {
                    if (!isObjectType(field.getType()))
                        throw new IllegalArgumentException("Caminho não contém objeto: " + currentPath);
                    Object next = stagedObjects.containsKey(currentPath)
                            ? stagedObjects.get(currentPath) : readField(field, parent);
                    if (next == null) {
                        if (stagedObjects.containsKey(currentPath))
                            throw new IllegalArgumentException("Objeto nulo no caminho: " + currentPath);
                        next = createNestedObject(row, parent, currentPath, field.getType());
                        writes.add(new FieldWrite(currentPath, rootColumn, parent, field, null, next));
                        proposed.put(currentPath, next);
                        previous.put(currentPath, null);
                        stagedObjects.put(currentPath, next);
                    }
                    parent = next;
                    owner = next.getClass();
                }
            }
        }
        Map<String, Object> beforeView = Collections.unmodifiableMap(new LinkedHashMap<>(previous));
        Map<String, Object> afterView = Collections.unmodifiableMap(new LinkedHashMap<>(proposed));
        rowFormValidator.validate(row, afterView);
        List<FieldWrite> applied = new ArrayList<>();
        try {
            for (FieldWrite write : writes) {
                applyWrite(row, write, write.next);
                applied.add(write);
            }
            rowSaveHandler.save(row, beforeView, afterView);
        } catch (Exception error) {
            for (int index = applied.size() - 1; index >= 0; index--) {
                FieldWrite write = applied.get(index);
                try { applyWrite(row, write, write.previous); }
                catch (Exception rollbackError) { error.addSuppressed(rollbackError); }
            }
            throw error;
        }
        int sourceIndex = sourceIndexOf(row);
        for (FieldWrite write : writes) {
            if (Objects.equals(write.previous, write.next)) continue;
            int column = write.rootColumn;
            Object oldValue = write.previous, newValue = write.next;
            dispachEvent(EventGridView.CELL_EDIT, new EventGrid() {
                @Override public List<Integer> getSelectedRows() { return List.of(sourceIndex); }
                @Override public List<Integer> getSelectedColumns() { return List.of(column); }
                @Override public Object getOldValue() { return oldValue; }
                @Override public Object getNewValue() { return newValue; }
                @Override public String getFieldPath() { return write.path; }
            });
        }
        dispachEvent(EventGridView.ROW_EDIT_SAVED, new GridRowEdit<>(row, beforeView, afterView));
        if (inlineItem == row) clearInlinePanel();
        refreshData();
    }

    private void applyWrite(T row, FieldWrite write, Object value) throws IllegalAccessException {
        if (write.field == null) currentModel.writeValue(row, write.rootColumn, value);
        else write.field.set(write.parent, value);
    }

    private int sourceIndexOf(T row) {
        for (int index = 0; index < fullDataList.size(); index++) if (fullDataList.get(index) == row) return index;
        return -1;
    }

    private void showInlineRowForm(T row, GridRowForm form) {
        clearInlinePanel();
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTokens.border()),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        JScrollPane formScroll = new JScrollPane(form.component());
        formScroll.setBorder(null);
        panel.add(formScroll, BorderLayout.CENTER);
        JLabel error = new JLabel(" ");
        error.setForeground(new Color(175, 45, 40));
        JButton save = new JButton("Salvar");
        JButton cancel = new JButton("Cancelar");
        save.addActionListener(event -> {
            try { saveRowForm(row, form); }
            catch (Exception failure) { error.setText(Objects.toString(failure.getMessage(), failure.toString())); }
        });
        cancel.addActionListener(event -> closeInlineForm());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
        buttons.add(error); buttons.add(cancel); buttons.add(save);
        panel.add(buttons, BorderLayout.SOUTH);
        panel.setPreferredSize(new Dimension(Math.max(360, getWidth()),
                Math.min(360, Math.max(140, form.component().getPreferredSize().height + 75))));
        inlineItem = row;
        inlinePanel = panel;
        add(panel);
        rebuildView();
    }

    public void closeInlineForm() {
        if (inlinePanel == null) return;
        clearInlinePanel();
        rebuildView();
    }

    private void clearInlinePanel() {
        if (inlinePanel != null) remove(inlinePanel);
        inlinePanel = null;
        inlineItem = null;
        inlineRow = -1;
    }

    private void positionInlinePanel() {
        if (inlinePanel == null || inlineRow < 0 || inlineRow >= getRowCount()) return;
        int height = inlinePanel.getPreferredSize().height;
        if (getRowHeight(inlineRow) != height) setRowHeight(inlineRow, height);
        Rectangle cell = getCellRect(inlineRow, 0, true);
        inlinePanel.setBounds(0, cell.y, getWidth(), height);
        inlinePanel.revalidate();
        inlinePanel.repaint();
    }

    public void setGridStyle(GridStyle style) { gridStyle = Objects.requireNonNull(style); applyGridStyle(); repaint(); }
    public void setColumnStyle(String fieldName, GridCellStyle style) { fieldIndex(fieldName); putStyle(columnStyles, fieldName, style); repaint(); }
    public void setRowStyle(T item, GridCellStyle style) { Objects.requireNonNull(item); putStyle(rowStyles, item, style); repaint(); }
    public void setCellStyle(T item, String fieldName, GridCellStyle style) {
        Objects.requireNonNull(item); fieldIndex(fieldName);
        if (style == null) {
            Map<String, GridCellStyle> cells = cellStyles.get(item);
            if (cells != null) { cells.remove(fieldName); if (cells.isEmpty()) cellStyles.remove(item); }
        } else cellStyles.computeIfAbsent(item, key -> new HashMap<>()).put(fieldName, style);
        repaint();
    }
    private static <K> void putStyle(Map<K, GridCellStyle> map, K key, GridCellStyle style) {
        if (style == null) map.remove(key); else map.put(key, style);
    }
    public void setStyleResolver(GridStyleResolver<T> resolver) {
        styleResolver = resolver == null ? (item, field, value) -> null : resolver;
        repaint();
    }

    private void applyGridStyle() {
        UiTokens.refresh();
        GridStyle style = gridStyle;
        setBackground(style.background() != null ? style.background() : UiTokens.surface());
        setForeground(style.foreground() != null ? style.foreground() : UiTokens.foreground());
        setSelectionBackground(style.selectionBackground() != null ? style.selectionBackground() : UiTokens.accent());
        setSelectionForeground(style.selectionForeground() != null ? style.selectionForeground() : UiTokens.onColor(getSelectionBackground()));
        setGridColor(style.gridColor() != null ? style.gridColor() : UiTokens.border());
        setShowGrid(style.showGrid());
        setRowHeight(style.rowHeight() > 0 ? UiTokens.scale(style.rowHeight()) : UiTokens.scale(28));
        setFont(style.font() != null ? style.font() : UiTokens.font());
        JTableHeader header = getTableHeader();
        if (header != null) {
            header.setDefaultRenderer((table, value, selected, focus, row, column) -> {
                String field = column >= 0 && column < getColumnCount() && !isActionColumn(column)
                        ? currentModel.getKeyForColumn(convertColumnIndexToModel(column)) : "";
                String title = String.valueOf(value);
                if (field.equals(sortField)) title += sortOrder == SortOrder.ASCENDING ? "  ▲" : "  ▼";
                JLabel label = new JLabel(title);
                label.setOpaque(true);
                label.setBackground(style.headerBackground() != null ? style.headerBackground() : UiTokens.surfaceAlt());
                label.setForeground(style.headerForeground() != null ? style.headerForeground() : UiTokens.foreground());
                label.setFont(getFont().deriveFont(Font.BOLD));
                label.setBorder(BorderFactory.createEmptyBorder(0, UiTokens.scale(8), 0, UiTokens.scale(8)));
                return label;
            });
            header.repaint();
        }
    }

    @Override public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        if (isDetailRow(row)) {
            JLabel blank = new JLabel();
            blank.setOpaque(true);
            blank.setBackground(getBackground());
            return blank;
        }
        if (isActionColumn(column)) {
            String label = rowFormMode != GridRowFormMode.OFF && rowActions.isEmpty() ? "Editar"
                    : rowFormMode == GridRowFormMode.OFF && rowActions.size() == 1 ? rowActions.getFirst().label()
                    : "Ações ▾";
            JButton action = new JButton(label);
            action.setFocusable(false);
            action.setMargin(new Insets(2, 8, 2, 8));
            boolean enabled = rowFormMode != GridRowFormMode.OFF && rowActions.isEmpty() ? allowEdit
                    : rowFormMode == GridRowFormMode.OFF && rowActions.size() == 1
                    ? rowActions.getFirst().enabled().test(getRowObject(row)) : true;
            action.setEnabled(enabled);
            action.setFont(getFont());
            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
            wrapper.setBackground(isRowSelected(row) ? getSelectionBackground() : getBackground());
            wrapper.add(action);
            return wrapper;
        }
        Component component = super.prepareRenderer(renderer, row, column);
        if (getColumnModel().getColumn(column).getCellRenderer() != null || hasCustomRenderer(getColumnClass(column))) return component;
        T item = getRowObject(row);
        if (item == null) return component;
        int modelColumn = convertColumnIndexToModel(column);
        String fieldName = currentModel.getKeyForColumn(modelColumn);
        Object value = getValueAt(row, column);
        GridCellStyle style = GridCellStyle.empty().overlay(columnStyles.get(fieldName))
                .overlay(rowStyles.get(item))
                .overlay(cellStyles.getOrDefault(item, Map.of()).get(fieldName))
                .overlay(styleResolver.resolve(item, fieldName, unwrap(value)));
        Color background = style.background() != null ? style.background()
                : gridStyle.striped() && (convertRowIndexToModel(row) % 2 != 0)
                ? gridStyle.stripeBackground() != null ? gridStyle.stripeBackground() : UiTokens.surfaceAlt()
                : getBackground();
        component.setBackground(isCellSelected(row, column) ? getSelectionBackground() : background);
        component.setForeground(isCellSelected(row, column) ? getSelectionForeground()
                : style.foreground() != null ? style.foreground() : getForeground());
        component.setFont(style.font() != null ? style.font() : getFont());
        if (component instanceof JLabel label) {
            if (value instanceof DropdownField dropdown) label.setText(Objects.toString(dropdown.getSelectedItem(), ""));
            label.setHorizontalAlignment(style.alignment() != null ? style.alignment()
                    : unwrap(value) instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEADING);
            if (style.border() != null) label.setBorder(style.border());
        } else if (component instanceof JCheckBox checkBox) {
            checkBox.setHorizontalAlignment(style.alignment() != null ? style.alignment() : SwingConstants.CENTER);
            if (style.border() != null) checkBox.setBorder(style.border());
        } else if (component instanceof JComponent jc && style.border() != null) jc.setBorder(style.border());
        return component;
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        if (getRowCount() != 0 || emptyText.isBlank()) return;
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setFont(getFont()); g.setColor(UiTokens.muted());
            FontMetrics metrics = g.getFontMetrics();
            g.drawString(emptyText, Math.max(8, (getWidth() - metrics.stringWidth(emptyText)) / 2), Math.max(metrics.getAscent() + 8, getHeight() / 2));
        } finally { g.dispose(); }
    }

    private void setTableHeaderToolTip() {
        JTableHeader header = getTableHeader();
        header.setToolTipText("Clique para ordenar; botão direito para filtrar");
        header.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) return;
                int viewColumn = header.columnAtPoint(event.getPoint());
                if (viewColumn < 0 || isActionColumn(viewColumn)) return;
                String field = currentModel.getKeyForColumn(convertColumnIndexToModel(viewColumn));
                setSort(field, field.equals(sortField) && sortOrder == SortOrder.ASCENDING ? SortOrder.DESCENDING : SortOrder.ASCENDING);
            }
            @Override public void mousePressed(MouseEvent event) { showFilterMenu(event); }
            @Override public void mouseReleased(MouseEvent event) { showFilterMenu(event); }
        });
    }

    private void showFilterMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) return;
        JTableHeader header = getTableHeader();
        int viewColumn = header.columnAtPoint(event.getPoint());
        if (viewColumn < 0 || isActionColumn(viewColumn)) return;
        String field = currentModel.getKeyForColumn(convertColumnIndexToModel(viewColumn));
        JPopupMenu menu = new JPopupMenu();
        JTextField text = new JTextField(textFilters.getOrDefault(field, ""), 18);
        text.setToolTipText("Contém texto");
        menu.add(text);
        JMenuItem apply = new JMenuItem("Aplicar filtro");
        apply.addActionListener(e -> setColumnTextFilter(field, text.getText()));
        text.addActionListener(e -> { setColumnTextFilter(field, text.getText()); menu.setVisible(false); });
        menu.add(apply);
        JMenuItem clear = new JMenuItem("Limpar filtro");
        clear.setEnabled(columnFilters.containsKey(field));
        clear.addActionListener(e -> clearColumnFilter(field));
        menu.add(clear);
        menu.show(header, event.getX(), event.getY());
        text.requestFocusInWindow();
    }

    public JComponent getPaginationPanel() {
        if (paginationPanel != null) return paginationPanel;
        JPanel panel = new JPanel(new BorderLayout(UiTokens.space(2), 0));
        pageControl = new PaginationPanel(1);
        pageControl.addEventListener(PaginationPanel.PAGE_CHANGED, event -> goToPage(((Number) event.getValue()).intValue() + 1));
        pageSizeControl = new JComboBox<>();
        pageSizeControl.addActionListener(event -> {
            if (!updatingPager && pageSizeControl.getSelectedItem() instanceof Integer size) setPageSize(size);
        });
        pageSummary = new JLabel();
        JPanel right = new JPanel(new FlowLayout(FlowLayout.TRAILING, UiTokens.space(2), 0));
        right.add(pageSummary); right.add(pageSizeControl);
        panel.add(pageControl, BorderLayout.LINE_START);
        panel.add(right, BorderLayout.LINE_END);
        paginationPanel = panel;
        updatePager();
        return panel;
    }

    private void updatePager() {
        if (paginationPanel == null) return;
        updatingPager = true;
        try {
            pageControl.setPageCount(getTotalPages());
            pageControl.setCurrentPage(currentPage - 1, false);
            List<Integer> sizes = new ArrayList<>(pageSizeOptions);
            if (!sizes.contains(pageSize)) sizes.add(pageSize);
            pageSizeControl.setModel(new DefaultComboBoxModel<>(sizes.toArray(Integer[]::new)));
            pageSizeControl.setSelectedItem(pageSize);
            int first = filteredItems == 0 ? 0 : paginationEnabled ? (currentPage - 1) * pageSize + 1 : 1;
            int last = paginationEnabled ? Math.min(filteredItems, currentPage * pageSize) : filteredItems;
            pageSummary.setText(first + "–" + last + " de " + filteredItems + (filteredItems != getTotalItems() ? " (" + getTotalItems() + " total)" : ""));
            paginationPanel.setVisible(paginationEnabled);
        } finally { updatingPager = false; }
    }

    private void edited(GridTableModel.Edit<T> edit) {
        int sourceIndex = visibleRows.get(edit.row()).sourceIndex;
        String editedField = currentModel.getKeyForColumn(edit.column());
        dispachEvent(EventGridView.CELL_EDIT, new EventGrid() {
            @Override public List<Integer> getSelectedRows() { return List.of(sourceIndex); }
            @Override public List<Integer> getSelectedColumns() { return List.of(edit.column()); }
            @Override public Object getOldValue() { return edit.oldValue(); }
            @Override public Object getNewValue() { return edit.newValue(); }
            @Override public String getFieldPath() { return editedField; }
        });
        if (columnFilters.containsKey(editedField) || editedField.equals(sortField))
            SwingUtilities.invokeLater(this::rebuildView);
    }

    private void installEvents() {
        getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting() || rebuildingView) return;
            List<Integer> rows = new ArrayList<>();
            for (int row : getSelectedRows()) {
                int modelRow = convertRowIndexToModel(row);
                if (modelRow >= 0 && modelRow < visibleRows.size() && !visibleRows.get(modelRow).detail) rows.add(visibleRows.get(modelRow).sourceIndex);
            }
            List<Integer> columns = new ArrayList<>();
            for (int col : getSelectedColumns()) columns.add(convertColumnIndexToModel(col));
            EventGrid selection = new EventGrid() {
                @Override public List<Integer> getSelectedRows() { return List.copyOf(rows); }
                @Override public List<Integer> getSelectedColumns() { return List.copyOf(columns); }
            };
            dispachEvent(EventGridView.SELECTION_ROW, selection);
            dispachEvent(EventGridView.SELECTION_COLUMN, selection);
        });
    }
}
