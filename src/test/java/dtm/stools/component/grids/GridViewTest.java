package dtm.stools.component.grids;

import dtm.stools.component.events.EventGridView;
import dtm.stools.component.feedback.pagination.PaginationPanel;
import dtm.stools.component.grids.annotations.GridColumn;
import dtm.stools.component.grids.event.EventGrid;
import dtm.stools.component.grids.model.ColumnDefinition;
import dtm.stools.component.popup.ModernComponentDialog;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GridViewTest {
    static final class Row {
        @GridColumn(name = "ID", order = 1, width = 72) int id;
        @GridColumn(name = "Nome", order = 2, width = 190) String name;
        @GridColumn(name = "Ativo", order = 3) boolean active;
        @GridColumn(order = 4, visible = false) String internal;
        Row(int id, String name) { this.id = id; this.name = name; this.active = true; }
    }

    static final class OrderRow {
        @GridColumn(name = "Pedido", order = 1) String code;
        @GridColumn(name = "Cliente", order = 2) Customer customer;
        OrderRow(String code, Customer customer) { this.code = code; this.customer = customer; }
    }

    static final class Customer {
        @GridColumn(name = "Nome", order = 1) String name;
        @GridColumn(name = "Endereço", order = 2) Address address;
        @GridColumn(name = "Relacionado", order = 3) Customer related;
        Customer() {}
        Customer(String name) { this.name = name; }
        @Override public String toString() { return name == null ? "Novo cliente" : name; }
    }

    static final class Address {
        @GridColumn(name = "Cidade") String city;
        Address() {}
        Address(String city) { this.city = city; }
        @Override public String toString() { return city == null ? "Novo endereço" : city; }
    }

    static final class NoConstructor {
        @GridColumn(name = "Nome") String name;
        NoConstructor(String name) { this.name = name; }
    }

    static final class NoConstructorRow {
        @GridColumn(name = "Objeto") NoConstructor object;
    }

    private static void edt(Runnable action) throws Exception {
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> { try { action.run(); } catch (Throwable error) { failure.set(error); } });
        if (failure.get() != null) throw new AssertionError(failure.get());
    }

    private static <C extends Component> C find(Component root, Class<C> type) {
        if (type.isInstance(root)) return type.cast(root);
        if (root instanceof Container container)
            for (Component child : container.getComponents()) {
                C found = find(child, type);
                if (found != null) return found;
            }
        return null;
    }

    private static JButton button(Component root, String label) {
        if (root instanceof JButton button && label.equals(button.getText())) return button;
        if (root instanceof Container container)
            for (Component child : container.getComponents()) {
                JButton found = button(child, label);
                if (found != null) return found;
            }
        return null;
    }

    private static <C extends JComponent> C formField(Component root, String path, Class<C> type) {
        if (root instanceof ModernComponentDialog.FormPanel form && form.field(path) != null)
            return form.field(path, type);
        if (root instanceof Container container)
            for (Component child : container.getComponents()) {
                C found = formField(child, path, type);
                if (found != null) return found;
            }
        return null;
    }

    @Test void filtersAndSortsAllRowsBeforePaging() throws Exception {
        edt(() -> {
            GridView<Row> grid = new GridView<>(Row.class);
            Row ana = new Row(1, "Ana"), bruno = new Row(2, "Bruno"), carla = new Row(3, "Carla");
            grid.setDataSource(List.of(ana, bruno, carla));
            grid.setCellStyle(ana, "name", GridCellStyle.empty().withForeground(Color.BLUE));
            assertEquals(3, grid.getColumnCount());
            assertEquals(72, grid.getColumnModel().getColumn(0).getPreferredWidth());
            assertEquals(190, grid.getColumnModel().getColumn(1).getPreferredWidth());
            JTableHeader header = grid.getTableHeader();
            header.setSize(500, 30);
            header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1));
            assertEquals("id", grid.getSortField());
            header.dispatchEvent(new MouseEvent(header, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 10, 10, 1, false, MouseEvent.BUTTON1));
            assertEquals(SortOrder.DESCENDING, grid.getSortOrder());
            grid.setColumnTextFilter("name", "a");
            grid.setSort("id", SortOrder.DESCENDING);
            grid.setPaginationEnabled(true);
            grid.setPageSize(1);
            assertEquals(3, grid.getTotalItems());
            assertEquals(2, grid.getFilteredItems());
            assertEquals(2, grid.getTotalPages());
            assertSame(carla, grid.getRowObject(0));
            assertEquals(List.of(3, "Carla", true), grid.getRow(0));
            AtomicReference<EventGrid> selection = new AtomicReference<>();
            grid.addEventListener(EventGridView.SELECTION_ROW, event -> selection.set((EventGrid) event.getValue()));
            grid.setRowSelectionInterval(0, 0);
            assertEquals(List.of(2), selection.get().getSelectedRows());
            grid.nextPage();
            assertSame(ana, grid.getRowObject(0));
            grid.clearSelection();
            assertEquals(Color.BLUE, grid.prepareRenderer(grid.getCellRenderer(0, 1), 0, 1).getForeground());
            assertEquals(2, grid.getCurrentPage());
            grid.setColumnFilter("id", value -> ((Number) value).intValue() > 1);
            assertEquals(1, grid.getFilteredItems());
            assertSame(carla, grid.getRowObject(0));
            grid.clearFilters();
            assertEquals(3, grid.getFilteredItems());
            assertThrows(IllegalArgumentException.class, () -> grid.setColumnTextFilter("missing", "a"));
            assertThrows(IllegalArgumentException.class, () -> grid.setModel(new DefaultTableModel()));
        });
    }

    @Test void stylesAndExplicitRendererHavePredictablePriority() throws Exception {
        edt(() -> {
            GridView<Row> grid = new GridView<>(Row.class);
            Row ana = new Row(1, "Ana"), bruno = new Row(2, "Bruno");
            grid.setDataSource(List.of(ana, bruno));
            grid.setGridStyle(GridStyle.standard().withBackground(Color.WHITE).withSelectionBackground(Color.BLACK).withSelectionForeground(Color.WHITE));
            grid.setColumnStyle("name", GridCellStyle.empty().withForeground(Color.RED));
            grid.setRowStyle(ana, GridCellStyle.empty().withBackground(Color.YELLOW));
            grid.setCellStyle(ana, "name", GridCellStyle.empty().withForeground(Color.BLUE));
            grid.setStyleResolver((row, field, value) -> row == bruno && "name".equals(field)
                    ? GridCellStyle.empty().withForeground(Color.GREEN) : null);
            Component first = grid.prepareRenderer(grid.getCellRenderer(0, 1), 0, 1);
            assertEquals(Color.BLUE, first.getForeground());
            assertEquals(Color.YELLOW, first.getBackground());
            Component second = grid.prepareRenderer(grid.getCellRenderer(1, 1), 1, 1);
            assertEquals(Color.GREEN, second.getForeground());
            grid.setRowSelectionInterval(0, 0);
            Component selected = grid.prepareRenderer(grid.getCellRenderer(0, 1), 0, 1);
            assertEquals(Color.WHITE, selected.getForeground());
            assertEquals(Color.BLACK, selected.getBackground());
            grid.updateUI();
            Component afterThemeRefresh = grid.prepareRenderer(grid.getCellRenderer(0, 1), 0, 1);
            assertEquals(Color.WHITE, afterThemeRefresh.getForeground());
            DefaultTableCellRenderer external = new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                    Component result = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                    result.setForeground(Color.MAGENTA);
                    return result;
                }
            };
            grid.getColumnModel().getColumn(1).setCellRenderer(external);
            Component custom = grid.prepareRenderer(grid.getCellRenderer(0, 1), 0, 1);
            assertSame(external, custom);
            assertEquals(Color.MAGENTA, custom.getForeground());
            DefaultCellEditor customEditor = new DefaultCellEditor(new JTextField());
            grid.getColumnModel().getColumn(1).setCellEditor(customEditor);
            assertSame(customEditor, grid.getCellEditor(0, 1));
        });
    }

    @Test void checkboxKeepsItsPositionInRendererAndEditor() throws Exception {
        edt(() -> {
            GridView<Row> grid = new GridView<>(Row.class);
            grid.setDataSource(List.of(new Row(1, "Ana"), new Row(2, "Bruno")));
            grid.setAllowEdit(true);
            for (int row = 0; row < grid.getRowCount(); row++) {
                Component rendered = grid.prepareRenderer(grid.getCellRenderer(row, 2), row, 2);
                assertInstanceOf(JCheckBox.class, rendered);
                assertEquals(SwingConstants.CENTER, ((JCheckBox) rendered).getHorizontalAlignment());
            }
            Component editor = grid.getCellEditor(0, 2).getTableCellEditorComponent(grid, true, true, 0, 2);
            assertInstanceOf(JCheckBox.class, editor);
            assertEquals(((JCheckBox) editor).getHorizontalAlignment(),
                    ((JCheckBox) grid.prepareRenderer(grid.getCellRenderer(0, 2), 0, 2)).getHorizontalAlignment());
        });
    }

    @Test void directCellEditingCanBeBlockedWithoutDisablingRowForms() throws Exception {
        edt(() -> {
            GridView<Row> grid = new GridView<>(Row.class);
            Row ana = new Row(1, "Ana"), bruno = new Row(2, "Bruno");
            grid.setDataSource(List.of(ana, bruno));
            grid.setAllowEdit(true);
            assertTrue(grid.isCellEditingEnabled());
            assertTrue(grid.isCellEditable(0, 1));
            grid.setCellEditingEnabled(false);
            assertFalse(grid.isCellEditable(0, 1));
            grid.setRowFormMode(GridRowFormMode.INLINE);
            assertTrue(grid.openRowForm(0));
            grid.closeInlineForm();

            grid.setCellEditingEnabled(true);
            grid.setColumnCellEditingEnabled("name", false);
            grid.setRowCellEditingEnabled(ana, false);
            grid.setCellEditingEnabled(bruno, "active", false);
            grid.setSort("name", SortOrder.DESCENDING);
            assertSame(bruno, grid.getRowObject(0));
            assertFalse(grid.isCellEditable(0, 1));
            assertFalse(grid.isCellEditable(0, 2));
            assertTrue(grid.isCellEditable(0, 0));
            assertSame(ana, grid.getRowObject(1));
            assertFalse(grid.isCellEditable(1, 0));
            assertFalse(grid.isCellEditable(1, 2));

            grid.setColumnCellEditingEnabled("name", true);
            grid.setCellEditingEnabled(bruno, "active", true);
            grid.setRowCellEditingEnabled(ana, true);
            assertTrue(grid.isCellEditable(0, 1));
            assertTrue(grid.isCellEditable(0, 2));
            assertTrue(grid.isCellEditable(1, 0));
        });
    }

    @Test void editingCheckboxDoesNotRefreshTheWholeTableOrClearSelection() throws Exception {
        GridView<Row> grid = new GridView<>(Row.class);
        int[] fullRefreshes = {0};
        edt(() -> {
            Row ana = new Row(1, "Ana");
            grid.setDataSource(List.of(ana, new Row(2, "Bruno")));
            grid.setAllowEdit(true);
            grid.setRowSelectionInterval(0, 0);
            grid.getModel().addTableModelListener(event -> {
                if (event.getLastRow() == Integer.MAX_VALUE) fullRefreshes[0]++;
            });
            grid.getModel().setValueAt(false, 0, 2);
            assertFalse(ana.active);
            assertEquals(0, grid.getSelectedRow());
        });
        edt(() -> {
            assertEquals(0, grid.getSelectedRow());
            assertEquals(0, fullRefreshes[0]);
        });
    }

    @Test void editingReportsPreviousValueAndPagerTracksPages() throws Exception {
        GridView<Row> grid = new GridView<>(Row.class);
        AtomicReference<EventGrid> edit = new AtomicReference<>();
        edt(() -> {
            grid.setDataSource(List.of(new Row(1, "Ana"), new Row(2, "Bruno")));
            grid.setPaginationEnabled(true);
            grid.setPageSize(1);
            JComponent pager = grid.getPaginationPanel();
            assertTrue(pager.isVisible());
            PaginationPanel control = (PaginationPanel) pager.getComponent(0);
            assertEquals(2, control.getPageCount());
            control.setCurrentPage(1);
            assertEquals(2, grid.getCurrentPage());
            assertEquals(1, control.getCurrentPage());
            grid.previousPage();
            grid.setAllowEdit(true);
            grid.addEventListener(EventGridView.CELL_EDIT, event -> edit.set((EventGrid) event.getValue()));
            grid.getModel().setValueAt("Anabela", 0, 1);
            assertEquals("Ana", edit.get().getOldValue());
            assertEquals("Anabela", edit.get().getNewValue());
            assertEquals("name", edit.get().getFieldPath());
            assertEquals(List.of(0), edit.get().getSelectedRows());
            grid.getModel().setValueAt("5", 0, 0);
            assertEquals(5, grid.getRowObject(0).id);
        });
        edt(() -> assertEquals("Anabela", grid.getRowObject(0).name));
    }

    @Test void editedValueReappliesActiveFilter() throws Exception {
        GridView<Row> grid = new GridView<>(Row.class);
        edt(() -> {
            grid.setDataSource(List.of(new Row(1, "Ana"), new Row(2, "Bruno")));
            grid.setAllowEdit(true);
            grid.setColumnTextFilter("name", "Ana");
            assertEquals(1, grid.getRowCount());
            grid.getModel().setValueAt("Zoe", 0, 1);
        });
        edt(() -> {
            assertEquals(0, grid.getRowCount());
            assertEquals(0, grid.getFilteredItems());
            assertEquals(2, grid.getTotalItems());
        });
    }

    @Test void inlineFormCancelsAndSavesTheCorrectPagedRow() throws Exception {
        edt(() -> {
            GridView<Row> grid = new GridView<>(Row.class);
            Row ana = new Row(1, "Ana"), bruno = new Row(2, "Bruno");
            grid.setDataSource(List.of(ana, bruno));
            grid.setAllowEdit(true);
            grid.setPageSize(1);
            grid.setPaginationEnabled(true);
            grid.goToPage(2);
            grid.setRowFormMode(GridRowFormMode.INLINE);
            assertEquals(4, grid.getColumnCount());
            assertEquals(List.of(2, "Bruno", true), grid.getRow(0));
            assertNotNull(grid.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(
                    grid, "Ações", false, false, 0, grid.getColumnCount() - 1));
            AtomicReference<GridRowEdit<?>> saved = new AtomicReference<>();
            grid.addEventListener(EventGridView.ROW_EDIT_SAVED, event -> saved.set((GridRowEdit<?>) event.getValue()));
            assertTrue(grid.openRowForm(0));
            assertEquals(2, grid.getRowCount());
            assertNull(grid.getRowObject(1));
            ModernComponentDialog.FormPanel form = find(grid, ModernComponentDialog.FormPanel.class);
            assertNotNull(form);
            form.field("name", JTextField.class).setText("Breno");
            button(grid, "Cancelar").doClick();
            assertEquals("Bruno", bruno.name);
            assertEquals(1, grid.getRowCount());
            assertTrue(grid.openRowForm(0));
            form = find(grid, ModernComponentDialog.FormPanel.class);
            form.field("name", JTextField.class).setText("Breno");
            button(grid, "Salvar").doClick();
            assertEquals("Breno", bruno.name);
            assertEquals("Ana", ana.name);
            assertEquals(1, grid.getRowCount());
            assertSame(bruno, saved.get().row());
            assertEquals("Bruno", saved.get().previousValues().get("name"));
            assertEquals("Breno", saved.get().newValues().get("name"));
        });
    }

    @Test void formRollbackAndCustomRowAction() throws Exception {
        edt(() -> {
            GridView<Row> grid = new GridView<>(Row.class);
            Row ana = new Row(1, "Ana");
            grid.setDataSource(List.of(ana));
            grid.setAllowEdit(true);
            grid.setRowFormMode(GridRowFormMode.INLINE);
            grid.setRowFormFactory((row, owner) -> new GridRowForm(new JPanel(), () -> Map.of("name", "Alterado")));
            grid.setRowFormValidator((row, values) -> { throw new IllegalArgumentException("Inválido"); });
            grid.setRowSaveHandler((row, before, after) -> { throw new IllegalStateException("Falha ao salvar"); });
            assertTrue(grid.openRowForm(0));
            button(grid, "Salvar").doClick();
            assertEquals("Ana", ana.name);
            assertEquals(2, grid.getRowCount());
            grid.setRowFormValidator(null);
            button(grid, "Salvar").doClick();
            assertEquals("Ana", ana.name);
            assertEquals(2, grid.getRowCount());
            grid.closeInlineForm();
            grid.setRowFormMode(GridRowFormMode.OFF);
            AtomicReference<Row> acted = new AtomicReference<>();
            grid.setRowActions(List.of(new GridRowAction<>("inspect", "Inspecionar", acted::set)));
            grid.setSize(500, 200);
            grid.doLayout();
            Rectangle cell = grid.getCellRect(0, grid.getColumnCount() - 1, true);
            grid.dispatchEvent(new MouseEvent(grid, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    cell.x + 4, cell.y + 4, 1, false, MouseEvent.BUTTON1));
            assertSame(ana, acted.get());
        });
    }

    @Test void actionUsesVisibleRowAfterFilterSortAndPagination() throws Exception {
        edt(() -> {
            GridView<Row> grid = new GridView<>(Row.class);
            Row ana = new Row(1, "Ana"), bruno = new Row(2, "Bruno"), carla = new Row(3, "Carla");
            grid.setDataSource(List.of(ana, bruno, carla));
            grid.setColumnTextFilter("name", "a");
            grid.setSort("name", SortOrder.DESCENDING);
            grid.setPaginationEnabled(true);
            grid.setPageSize(1);
            grid.goToPage(2);
            assertSame(ana, grid.getRowObject(0));
            AtomicReference<Row> acted = new AtomicReference<>();
            grid.setRowActions(List.of(new GridRowAction<>("inspect", "Inspecionar", acted::set)));
            grid.setSize(500, 150);
            grid.doLayout();
            Rectangle cell = grid.getCellRect(0, grid.getColumnCount() - 1, true);
            grid.dispatchEvent(new MouseEvent(grid, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0,
                    cell.x + 4, cell.y + 4, 1, false, MouseEvent.BUTTON1));
            assertSame(ana, acted.get());
        });
    }

    @Test void inlineFormFollowsItsRowWhenSortChanges() throws Exception {
        edt(() -> {
            GridView<Row> grid = new GridView<>(Row.class);
            Row ana = new Row(1, "Ana"), bruno = new Row(2, "Bruno");
            grid.setDataSource(List.of(ana, bruno));
            grid.setAllowEdit(true);
            grid.setRowFormMode(GridRowFormMode.INLINE);
            assertTrue(grid.openRowForm(0));
            assertEquals(3, grid.getRowCount());
            grid.setSort("name", SortOrder.DESCENDING);
            assertSame(bruno, grid.getRowObject(0));
            assertSame(ana, grid.getRowObject(1));
            assertNull(grid.getRowObject(2));
            assertNotNull(find(grid, ModernComponentDialog.FormPanel.class));
        });
    }

    @Test void nestedFormEditsSharedInstanceAndSupportsReplacement() throws Exception {
        edt(() -> {
            Customer current = new Customer("Ana"), other = new Customer("Bruno");
            current.address = new Address("Salvador");
            OrderRow order = new OrderRow("P-1", current);
            GridView<OrderRow> grid = new GridView<>(OrderRow.class);
            grid.setDataSource(List.of(order));
            grid.setAllowEdit(true);
            grid.setRowFormMode(GridRowFormMode.INLINE);
            grid.setObjectChoices("customer", context -> List.of(other));
            assertEquals(-1, grid.getNestedEditDepth());
            assertTrue(grid.isCellEditable(0, 1));
            assertFalse(grid.getCellEditor(0, 1) instanceof DefaultCellEditor);
            AtomicReference<EventGrid> changed = new AtomicReference<>();
            grid.addEventListener(EventGridView.CELL_EDIT, event -> {
                EventGrid edit = (EventGrid) event.getValue();
                if ("customer.address.city".equals(edit.getFieldPath())) changed.set(edit);
            });
            assertTrue(grid.openRowForm(0));
            JPanel customerPanel = formField(grid, "customer", JPanel.class);
            button(customerPanel, "Propriedades").doClick();
            JPanel addressPanel = formField(grid, "customer.address", JPanel.class);
            button(addressPanel, "Propriedades").doClick();
            formField(grid, "customer.address.city", JTextField.class).setText("Recife");
            button(grid, "Salvar").doClick();
            assertSame(current, order.customer);
            assertEquals("Recife", current.address.city);
            assertEquals("Salvador", changed.get().getOldValue());
            assertEquals("Recife", changed.get().getNewValue());

            assertTrue(grid.openRowForm(0));
            customerPanel = formField(grid, "customer", JPanel.class);
            @SuppressWarnings("unchecked") JComboBox<Object> choices = find(customerPanel, JComboBox.class);
            choices.setSelectedIndex(2);
            button(grid, "Salvar").doClick();
            assertSame(other, order.customer);
            assertEquals("Recife", current.address.city);
        });
    }

    @Test void nestedDepthAndCyclesStopExpansion() throws Exception {
        edt(() -> {
            Customer customer = new Customer("Ana");
            customer.address = new Address("Salvador");
            customer.related = customer;
            GridView<OrderRow> grid = new GridView<>(OrderRow.class);
            grid.setDataSource(List.of(new OrderRow("P-1", customer)));
            grid.setAllowEdit(true);
            grid.setRowFormMode(GridRowFormMode.INLINE);
            assertTrue(grid.openRowForm(0));
            button(formField(grid, "customer", JPanel.class), "Propriedades").doClick();
            JPanel related = formField(grid, "customer.related", JPanel.class);
            assertNotNull(related);
            assertFalse(button(related, "Propriedades").isVisible());
            grid.closeInlineForm();

            grid.setNestedEditDepth(1);
            assertTrue(grid.openRowForm(0));
            button(formField(grid, "customer", JPanel.class), "Propriedades").doClick();
            JPanel address = formField(grid, "customer.address", JPanel.class);
            assertNotNull(address);
            assertFalse(button(address, "Propriedades").isVisible());
            grid.closeInlineForm();

            grid.setNestedEditDepth(0);
            assertTrue(grid.openRowForm(0));
            assertFalse(button(formField(grid, "customer", JPanel.class), "Propriedades").isVisible());
        });
    }

    @Test void nullNestedObjectUsesConstructorOrFactoryAndRollbackRestoresLeaves() throws Exception {
        edt(() -> {
            OrderRow order = new OrderRow("P-1", null);
            GridView<OrderRow> grid = new GridView<>(OrderRow.class);
            grid.setDataSource(List.of(order));
            grid.setAllowEdit(true);
            grid.setRowFormMode(GridRowFormMode.INLINE);
            assertTrue(grid.openRowForm(0));
            button(formField(grid, "customer", JPanel.class), "Criar").doClick();
            formField(grid, "customer.name", JTextField.class).setText("Novo");
            button(grid, "Cancelar").doClick();
            assertNull(order.customer);
            assertTrue(grid.openRowForm(0));
            button(formField(grid, "customer", JPanel.class), "Criar").doClick();
            formField(grid, "customer.name", JTextField.class).setText("Novo");
            button(grid, "Salvar").doClick();
            assertEquals("Novo", order.customer.name);

            Customer shared = order.customer;
            shared.address = new Address("Salvador");
            grid.setRowFormFactory((row, owner) -> new GridRowForm(new JPanel(),
                    () -> Map.of("customer.name", "Alterado", "customer.address.city", "Recife")));
            grid.setRowSaveHandler((row, before, after) -> { throw new IllegalStateException("Falha"); });
            assertTrue(grid.openRowForm(0));
            button(grid, "Salvar").doClick();
            assertSame(shared, order.customer);
            assertEquals("Novo", shared.name);
            assertEquals("Salvador", shared.address.city);
        });
    }

    private static Map<String, Object> record(Object... pairs) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) map.put((String) pairs[index], pairs[index + 1]);
        return map;
    }

    @Test void structuredRowsExposeCellsByFieldHeaderAndIndex() throws Exception {
        edt(() -> {
            GridView<Row> grid = new GridView<>(Row.class);
            Row ana = new Row(1, "Ana"), bruno = new Row(2, "Bruno");
            ana.internal = "segredo";
            grid.setDataSource(List.of(ana, bruno));
            GridRow<Row> row = grid.getRow(0);
            assertSame(ana, row.getItem());
            assertEquals("Ana", row.getCell("name"));
            assertEquals("Ana", row.getCell("Nome"));
            assertEquals(1, row.getCell(0));
            assertEquals(1, row.getCell("id", Integer.class));
            assertEquals("segredo", row.getCell("internal"));
            assertEquals(List.of("id", "name", "active"), row.getKeys());
            assertThrows(IllegalArgumentException.class, () -> row.getCell("missing"));
            assertTrue(grid.getRow(9).isEmpty());
            assertNull(grid.getSelectedGridRow());
            grid.setRowSelectionInterval(1, 1);
            assertEquals("Bruno", grid.getSelectedGridRow().getCell("name"));
            assertSame(bruno, grid.getSelectedRowObject());
            grid.addRowSelectionInterval(0, 0);
            assertEquals(2, grid.getSelectedGridRows().size());
            assertTrue(grid.isStructured());
        });
    }

    @Test void mapGridInfersColumnsAndSupportsSortFilterAndSelection() throws Exception {
        edt(() -> {
            GridView<Map<String, Object>> grid = GridView.ofMaps();
            assertFalse(grid.isStructured());
            Map<String, Object> ana = record("id", 1, "nome", "Ana", "ativo", true);
            Map<String, Object> bruno = record("id", 2, "nome", "Bruno", "ativo", false, "extra", "x");
            grid.setDataSource(List.of(ana, bruno));
            assertEquals(4, grid.getColumnCount());
            assertEquals("Nome", grid.getColumnModel().getColumn(1).getHeaderValue());
            assertEquals(Boolean.class, grid.getColumnClass(2));
            assertEquals("Ana", grid.getRow(0).getCell("nome"));
            assertEquals(1, grid.getRow(0).getCell(0));
            assertNull(grid.getRow(0).getCell("extra"));
            assertSame(ana, grid.getRow(0).getItem());
            grid.setSort("nome", SortOrder.DESCENDING);
            assertSame(bruno, grid.getRowObject(0));
            grid.setColumnTextFilter("nome", "an");
            assertEquals(1, grid.getRowCount());
            assertEquals("Ana", grid.getRow(0).getCell("nome"));
            grid.clearFilters();
            grid.setRowSelectionInterval(0, 0);
            assertEquals("Bruno", grid.getSelectedGridRow().getCell("nome"));
            assertEquals("x", grid.getSelectedGridRow().getCell("extra"));
            grid.setColumnStyle("nome", GridCellStyle.empty().withForeground(Color.RED));
            assertEquals(Color.RED, grid.prepareRenderer(grid.getCellRenderer(1, 1), 1, 1).getForeground());
            grid.setDataSource(List.of(record("codigo", "A1")));
            assertEquals(1, grid.getColumnCount());
            assertNull(grid.getSortField());
            assertEquals("A1", grid.getRow(0).getCell("codigo"));
            assertThrows(IllegalStateException.class, () -> grid.setObjectChoices("codigo", context -> List.of()));
        });
    }

    @Test void mapGridWithExplicitColumnsConvertsEditsAndSavesForms() throws Exception {
        edt(() -> {
            GridView<Map<String, Object>> grid = GridView.ofMaps(List.of(
                    ColumnDefinition.builder().key("id").name("Código").type(Integer.class).width(60).build(),
                    ColumnDefinition.builder().key("nome").build(),
                    ColumnDefinition.builder().key("obs").editable(false).build()));
            Map<String, Object> ana = record("id", 1, "nome", "Ana", "obs", "fixo");
            grid.setDataSource(List.of(ana));
            assertEquals("Código", grid.getColumnModel().getColumn(0).getHeaderValue());
            assertEquals(60, grid.getColumnModel().getColumn(0).getPreferredWidth());
            grid.setAllowEdit(true);
            assertTrue(grid.isCellEditable(0, 0));
            assertFalse(grid.isCellEditable(0, 2));
            AtomicReference<EventGrid> edit = new AtomicReference<>();
            grid.addEventListener(EventGridView.CELL_EDIT, event -> edit.set((EventGrid) event.getValue()));
            grid.getModel().setValueAt("7", 0, 0);
            assertEquals(7, ana.get("id"));
            assertEquals("id", edit.get().getFieldPath());
            assertEquals(1, edit.get().getOldValue());
            grid.getModel().setValueAt("Anabela", 0, 1);
            assertEquals("Anabela", ana.get("nome"));

            grid.setRowFormMode(GridRowFormMode.INLINE);
            AtomicReference<GridRowEdit<?>> saved = new AtomicReference<>();
            grid.addEventListener(EventGridView.ROW_EDIT_SAVED, event -> saved.set((GridRowEdit<?>) event.getValue()));
            assertTrue(grid.openRowForm(0));
            formField(grid, "id", JTextField.class).setText("9");
            formField(grid, "nome", JTextField.class).setText("Carla");
            button(grid, "Salvar").doClick();
            assertEquals(9, ana.get("id"));
            assertEquals("Carla", ana.get("nome"));
            assertEquals("fixo", ana.get("obs"));
            assertEquals(7, saved.get().previousValues().get("id"));

            grid.setRowSaveHandler((row, before, after) -> { throw new IllegalStateException("Falha"); });
            assertTrue(grid.openRowForm(0));
            formField(grid, "nome", JTextField.class).setText("Zoe");
            button(grid, "Salvar").doClick();
            assertEquals("Carla", ana.get("nome"));

            GridView<Map<String, Object>> immutable = GridView.ofMaps("nome");
            immutable.setDataSource(List.of(Map.of("nome", "Ana")));
            immutable.setAllowEdit(true);
            assertThrows(IllegalArgumentException.class, () -> immutable.getModel().setValueAt("Bia", 0, 0));
        });
    }

    static final class ClientesGrid extends MapGridView {
        ClientesGrid() {
            super("id", "nome");
            setColumnStyle("nome", GridCellStyle.empty().withForeground(Color.BLUE));
        }
        String nomeSelecionado() {
            GridRow<Map<String, Object>> row = getSelectedGridRow();
            return row == null ? null : row.getCell("nome", String.class);
        }
    }

    static final class ProdutosGrid extends GridView<Row> {
        ProdutosGrid() { super(Row.class); }
    }

    @Test void mapAndTypedGridsCanBeSubclassed() throws Exception {
        edt(() -> {
            ClientesGrid grid = new ClientesGrid();
            grid.setDataSource(List.of(record("id", 1, "nome", "Ana"), record("id", 2, "nome", "Bruno")));
            assertNull(grid.nomeSelecionado());
            grid.setRowSelectionInterval(1, 1);
            assertEquals("Bruno", grid.nomeSelecionado());
            grid.clearSelection();
            assertEquals(Color.BLUE, grid.prepareRenderer(grid.getCellRenderer(0, 1), 0, 1).getForeground());
            MapGridView inferred = new MapGridView();
            inferred.setDataSource(List.of(record("a", 1, "b", 2)));
            assertEquals(2, inferred.getColumnCount());
            GridView<Map<String, Object>> fromFactory = GridView.ofMaps("x");
            assertInstanceOf(MapGridView.class, fromFactory);
            ProdutosGrid typed = new ProdutosGrid();
            typed.setDataSource(List.of(new Row(1, "Ana")));
            assertEquals("Ana", typed.getRow(0).getCell("name"));
        });
    }

    @Test void nullNestedObjectWithoutConstructorNeedsFactory() throws Exception {
        edt(() -> {
            NoConstructorRow row = new NoConstructorRow();
            GridView<NoConstructorRow> grid = new GridView<>(NoConstructorRow.class);
            grid.setDataSource(List.of(row));
            grid.setAllowEdit(true);
            grid.setRowFormMode(GridRowFormMode.INLINE);
            grid.setRowFormFactory((item, owner) -> new GridRowForm(new JPanel(),
                    () -> Map.of("object.name", "Criado")));
            assertTrue(grid.openRowForm(0));
            button(grid, "Salvar").doClick();
            assertNull(row.object);
            AtomicReference<GridObjectContext<NoConstructorRow>> factoryContext = new AtomicReference<>();
            grid.setObjectFactory("object", context -> {
                factoryContext.set(context);
                return new NoConstructor("Rascunho");
            });
            button(grid, "Salvar").doClick();
            assertEquals("Criado", row.object.name);
            assertSame(row, factoryContext.get().row());
            assertSame(row, factoryContext.get().parent());
            assertEquals("object", factoryContext.get().path());
            assertSame(NoConstructor.class, factoryContext.get().type());
        });
    }
}
