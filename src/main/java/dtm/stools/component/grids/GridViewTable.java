package dtm.stools.component.grids;

import dtm.stools.component.grids.model.ColumnDefinition;
import dtm.stools.component.grids.model.ReflectionTableModel;
import dtm.stools.component.inputfields.selectfield.DropdownField;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;

public class GridViewTable<T> extends DataTableListener {

    private final Class<T> modelClass;

    @Getter
    private TableGridMode gridMode;

    @Getter
    @Setter
    private boolean allowEdit = false;

    @Getter
    private boolean paginationEnabled = false;
    @Getter
    private int pageSize = 20;
    @Getter
    private java.util.List<Integer> pageSizeOptions = java.util.List.of(10, 20, 50, 100, 125, 150, 175, 200);
    @Getter
    private int currentPage = 1;

    private java.util.List<T> fullDataList = new ArrayList<>();

    private ReflectionTableModel<T> currentModel;

    public GridViewTable(Class<T> modelClass) {
        this(modelClass, TableGridMode.BATCH);
    }

    public GridViewTable(Class<T> modelClass, TableGridMode mode) {
        super();
        this.modelClass = modelClass;
        setGridMode(mode);
        this.currentModel = new ReflectionTableModel<>(new ArrayList<>(), this.modelClass, this::isAllowEdit);
        super.setModel(this.currentModel);
        this.setAutoCreateRowSorter(true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void setModel(TableModel dataModel) {
        super.setModel(dataModel);
        try{
            if(dataModel instanceof ReflectionTableModel<?> model){
                this.currentModel = (ReflectionTableModel<T>)model;
            }
        } catch (Exception ignored) {}
        if (this.currentModel != null) {
            super.setModel(this.currentModel);
        }
    }

    @Override
    public TableCellEditor getCellEditor(int row, int column) {
        Object value = getValueAt(row, column);

        if (value instanceof Boolean) {
            return getDefaultEditor(Boolean.class);
        }

        if (value instanceof Collection<?> col) {
            return new DefaultCellEditor(new DropdownField(col));
        }

        if (value != null && value.getClass().isArray()) {
            return new DefaultCellEditor(new DropdownField((Object[]) value));
        }

        if(value instanceof DropdownField dropdownField) return new DefaultCellEditor(dropdownField);

        return super.getCellEditor(row, column);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        Object value = getValueAt(row, column);

        if (value instanceof Boolean) {
            return getDefaultRenderer(Boolean.class);
        }

        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {

                if(value instanceof DropdownField dropdownField){
                    value = dropdownField.getSelectedItem();
                    if(value == null) dropdownField.selectFirst();
                    value = dropdownField.getSelectedItem();
                }

                return super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );
            }
        };
    }

    public void setGridMode(TableGridMode mode) {
        if (mode == null) {
            mode = TableGridMode.BATCH;
        }
        this.gridMode = mode;
        if (mode == TableGridMode.SINGLE) {
            this.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        } else {
            this.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        }
    }

    public void setDataSource(Collection<T> data) {
        if (data == null) {
            data = new ArrayList<>();
        }

        this.fullDataList = new ArrayList<>(data);
        this.currentPage = 1;

        if (currentModel == null) {
            currentModel = new ReflectionTableModel<>(new ArrayList<>(), modelClass, this::isAllowEdit);
            super.setModel(currentModel);
        }

        updatePagedData();
    }

    private void updatePagedData() {
        if (currentModel == null) {
            return;
        }

        if (!paginationEnabled) {
            currentModel.setDataList(new ArrayList<>(this.fullDataList));
        } else {
            int totalItems = this.fullDataList.size();
            int totalPages = getTotalPages();

            if (this.currentPage < 1) {
                this.currentPage = 1;
            }
            if (this.currentPage > totalPages && totalPages > 0) {
                this.currentPage = totalPages;
            }

            int fromIndex = (this.currentPage - 1) * this.pageSize;
            int toIndex = Math.min(fromIndex + this.pageSize, totalItems);

            if (fromIndex > totalItems || fromIndex < 0) {
                fromIndex = 0;
                toIndex = 0;
            }

            java.util.List<T> pageData = (totalItems > 0 && fromIndex <= toIndex)
                    ? new ArrayList<>(this.fullDataList.subList(fromIndex, toIndex))
                    : new ArrayList<>();

            currentModel.setDataList(pageData);
        }

        currentModel.fireTableDataChanged();
    }

    public void setPaginationEnabled(boolean paginationEnabled) {
        this.paginationEnabled = paginationEnabled;
        this.currentPage = 1;
        updatePagedData();
    }

    public void setPageSize(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("O tamanho da página deve ser maior que 0");
        }
        this.pageSize = pageSize;
        this.currentPage = 1;
        updatePagedData();
    }

    public void setPageSizeOptions(java.util.List<Integer> pageSizeOptions) {
        this.pageSizeOptions = pageSizeOptions;
        if (!this.pageSizeOptions.isEmpty() && !this.pageSizeOptions.contains(this.pageSize)) {
            setPageSize(this.pageSizeOptions.getFirst());
        }
    }

    public int getTotalPages() {
        if (this.pageSize <= 0 || this.fullDataList.isEmpty()) {
            return 1;
        }
        return (int) Math.ceil((double) this.fullDataList.size() / this.pageSize);
    }

    public int getTotalItems() {
        return this.fullDataList.size();
    }

    public void goToPage(int page) {
        int totalPages = getTotalPages();
        int newPage = page;

        if (newPage < 1) {
            newPage = 1;
        }
        if (newPage > totalPages) {
            newPage = totalPages;
        }

        if (this.currentPage != newPage || !this.paginationEnabled) {
            this.currentPage = newPage;
            updatePagedData();
        }
    }

    public void nextPage() {
        goToPage(this.currentPage + 1);
    }

    public java.util.List<Object> getRow(int row){
        java.util.List<Object> rowValues = new ArrayList<>();
        TableModel model = getModel();

        if(model != null){
            int columnCount = model.getColumnCount();
            for (int col = 0; col < columnCount; col++) {
                int modelRow = convertRowIndexToModel(row);
                int modelCol = convertColumnIndexToModel(col);

                Object value = model.getValueAt(modelRow, modelCol);
                if(value instanceof DropdownField dropdownField){
                    value = dropdownField.getSelectedItem();
                }
                rowValues.add(value);
            }
        }

        return rowValues;
    }

    public void previousPage() {
        goToPage(this.currentPage - 1);
    }

    public boolean hasNextPage() {
        return this.currentPage < getTotalPages();
    }

    public boolean hasPreviousPage() {
        return this.currentPage > 1;
    }

}