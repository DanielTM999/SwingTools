package dtm.stools.examples;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.events.EventGridView;
import dtm.stools.component.grids.GridCellStyle;
import dtm.stools.component.grids.GridStyle;
import dtm.stools.component.grids.GridView;
import dtm.stools.component.grids.GridRowAction;
import dtm.stools.component.grids.GridRowForm;
import dtm.stools.component.grids.GridRowFormMode;
import dtm.stools.component.grids.annotations.GridColumn;
import dtm.stools.component.grids.event.EventGrid;
import dtm.stools.component.popup.ModernComponentDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GridViewExample {
    private static final Color ACCENT = new Color(24, 95, 145);
    private static final Color SOFT_BLUE = new Color(234, 244, 251);
    private static final Color SOFT_RED = new Color(255, 239, 235);

    private GridViewExample() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            showWindow();
        });
    }

    private static void showWindow() {
        List<Product> products = sampleProducts();
        List<Vendor> vendors = List.of(
                new Vendor("Bahia Distribuição", new VendorAddress("Salvador")),
                new Vendor("Sul Atacado", new VendorAddress("Curitiba")),
                new Vendor("Norte Comercial", new VendorAddress("Manaus")));
        for (int index = 0; index < products.size(); index++)
            products.get(index).vendor = index == 1 ? null : vendors.get((index / 6) % vendors.size());
        GridView<Product> grid = new GridView<>(Product.class);
        grid.setGridStyle(GridStyle.standard().withRowHeight(34)
                .withHeaderBackground(ACCENT).withHeaderForeground(Color.WHITE)
                .withStripeBackground(new Color(247, 250, 253)));
        grid.setColumnStyle("price", GridCellStyle.empty().withAlignment(SwingConstants.RIGHT));
        grid.setColumnStyle("stock", GridCellStyle.empty().withAlignment(SwingConstants.CENTER));
        grid.setColumnStyle("active", GridCellStyle.empty().withAlignment(SwingConstants.CENTER));
        grid.setRowStyle(products.getFirst(), GridCellStyle.empty().withBackground(SOFT_BLUE));
        grid.setCellStyle(products.getFirst(), "name", GridCellStyle.empty()
                .withForeground(ACCENT).withFont(grid.getFont().deriveFont(Font.BOLD)));
        grid.setStyleResolver((product, field, value) ->
                "stock".equals(field) && value instanceof Number number && number.intValue() <= 5
                        ? GridCellStyle.empty().withBackground(SOFT_RED)
                                .withForeground(new Color(150, 48, 36))
                        : null);
        grid.setAllowEdit(true);
        grid.setCellEditingEnabled(false);
        grid.setColumnCellEditingEnabled("price", false);
        grid.setRowCellEditingEnabled(products.getFirst(), false);
        grid.setCellEditingEnabled(products.get(1), "stock", false);
        grid.setObjectChoices("vendor", context -> vendors);
        grid.setObjectFactory("vendor", context -> new Vendor());
        grid.setPageSizeOptions(List.of(8, 12, 24));
        grid.setPageSize(8);
        grid.setPaginationEnabled(true);
        grid.setDataSource(products);
        grid.setRowFormMode(GridRowFormMode.DIALOG);

        JLabel count = new JLabel();
        Runnable updateCount = () -> count.setText(grid.getFilteredItems() + " de " + grid.getTotalItems() + " produtos");
        grid.getModel().addTableModelListener(event -> updateCount.run());
        updateCount.run();

        JLabel activity = new JLabel("Selecione uma linha ou edite uma célula para ver os eventos.");
        grid.addEventListener(EventGridView.SELECTION_ROW, event -> {
            EventGrid selection = (EventGrid) event.getValue();
            activity.setText(selection.getSelectedRowCount() + " linha(s) selecionada(s)");
        });
        grid.addEventListener(EventGridView.CELL_EDIT, event -> {
            EventGrid edit = (EventGrid) event.getValue();
            activity.setText("Edição: " + edit.getOldValue() + " → " + edit.getNewValue());
        });
        grid.addEventListener(EventGridView.ROW_EDIT_SAVED, event -> activity.setText("Formulário salvo com sucesso."));
        grid.setRowFormValidator((product, values) -> {
            if (values.containsKey("name") && String.valueOf(values.get("name")).isBlank())
                throw new IllegalArgumentException("Informe o nome do produto.");
            if (values.get("price") instanceof Number price && price.doubleValue() < 0)
                throw new IllegalArgumentException("O preço deve ser positivo.");
        });

        JTextField search = new JTextField(16);
        search.putClientProperty("JTextField.placeholderText", "Buscar produto");
        search.getDocument().addDocumentListener(new DocumentListener() {
            private void update() { grid.setColumnTextFilter("name", search.getText()); }
            @Override public void insertUpdate(DocumentEvent event) { update(); }
            @Override public void removeUpdate(DocumentEvent event) { update(); }
            @Override public void changedUpdate(DocumentEvent event) { update(); }
        });

        JComboBox<String> category = new JComboBox<>(new String[]{
                "Todas as categorias", "Tecnologia", "Escritório", "Casa", "Áudio"
        });
        category.addActionListener(event -> {
            String selected = (String) category.getSelectedItem();
            grid.setColumnFilter("category", "Todas as categorias".equals(selected)
                    ? null : value -> selected.equals(value));
        });

        JCheckBox compact = new JCheckBox("Compacto");
        compact.addActionListener(event -> grid.setGridStyle(grid.getGridStyle()
                .withRowHeight(compact.isSelected() ? 24 : 34)));
        JCheckBox editable = new JCheckBox("Permitir edição", true);
        editable.addActionListener(event -> grid.setAllowEdit(editable.isSelected()));
        JCheckBox directEditing = new JCheckBox("Editar células");
        directEditing.addActionListener(event -> grid.setCellEditingEnabled(directEditing.isSelected()));
        JComboBox<String> formMode = new JComboBox<>(new String[]{"Janela", "Abaixo da linha", "Desligado"});
        formMode.addActionListener(event -> grid.setRowFormMode(switch (formMode.getSelectedIndex()) {
            case 1 -> GridRowFormMode.INLINE;
            case 2 -> GridRowFormMode.OFF;
            default -> GridRowFormMode.DIALOG;
        }));
        JCheckBox customForm = new JCheckBox("Formulário próprio");
        customForm.addActionListener(event -> grid.setRowFormFactory(customForm.isSelected()
                ? (product, owner) -> customProductForm(product) : null));
        JCheckBox extraAction = new JCheckBox("Ação extra");
        extraAction.addActionListener(event -> grid.setRowActions(extraAction.isSelected()
                ? List.of(new GridRowAction<>("details", "Detalhes", product ->
                        JOptionPane.showMessageDialog(grid, product.name + " • " + product.internalCode,
                                "Produto", JOptionPane.INFORMATION_MESSAGE)))
                : List.of()));
        JComboBox<String> nestedDepth = new JComboBox<>(new String[]{"Sem limite", "Só produto", "Fornecedor", "Endereço"});
        nestedDepth.addActionListener(event -> grid.setNestedEditDepth(nestedDepth.getSelectedIndex() - 1));
        JButton clear = new JButton("Limpar filtros");
        clear.addActionListener(event -> {
            search.setText("");
            category.setSelectedIndex(0);
            grid.clearFilters();
            grid.clearSort();
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 4));
        filters.add(new JLabel("Produto:"));
        filters.add(search);
        filters.add(new JLabel("Categoria:"));
        filters.add(category);
        filters.add(clear);
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 4));
        options.add(compact);
        options.add(editable);
        options.add(directEditing);
        options.add(new JLabel("Formulário:"));
        options.add(formMode);
        options.add(customForm);
        options.add(extraAction);
        JPanel nestedOptions = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 4));
        nestedOptions.add(new JLabel("Profundidade de objetos:"));
        nestedOptions.add(nestedDepth);
        nestedOptions.add(new JLabel("O segundo produto começa sem fornecedor; use Criar ou selecione outro."));
        JPanel controls = new JPanel(new GridLayout(3, 1));
        controls.add(filters);
        controls.add(options);
        controls.add(nestedOptions);

        JLabel help = new JLabel("Ações abre o formulário; edição direta começa desligada. Ao ativá-la, preço e primeira linha continuam bloqueados.");
        JPanel top = new JPanel(new BorderLayout());
        top.add(controls, BorderLayout.NORTH);
        top.add(help, BorderLayout.SOUTH);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.add(grid.getPaginationPanel(), BorderLayout.NORTH);
        JPanel status = new JPanel(new BorderLayout());
        status.add(activity, BorderLayout.CENTER);
        status.add(count, BorderLayout.LINE_END);
        footer.add(status, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(grid), BorderLayout.CENTER);
        content.add(footer, BorderLayout.SOUTH);

        JFrame frame = new JFrame("SwingTools • GridView");
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Tipado (@GridColumn)", content);
        tabs.addTab("Chave / valor (Map)", mapGridPanel());
        frame.setContentPane(tabs);
        frame.setSize(1240, 730);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private static JComponent mapGridPanel() {
        GridView<Map<String, Object>> grid = GridView.ofMaps();
        List<Map<String, Object>> rows = new ArrayList<>();
        String[] names = {"Ana", "Bruno", "Carla", "Diego", "Elisa", "Fábio"};
        String[] cities = {"Salvador", "Recife", "Curitiba", "Manaus", "Natal", "Belém"};
        for (int index = 0; index < names.length; index++) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", index + 1);
            row.put("nome", names[index]);
            row.put("cidade", cities[index]);
            row.put("ativo", index % 3 != 0);
            rows.add(row);
        }
        grid.setDataSource(rows);
        grid.setAllowEdit(true);
        grid.setRowFormMode(GridRowFormMode.DIALOG);
        grid.setColumnStyle("id", GridCellStyle.empty().withAlignment(SwingConstants.CENTER));

        JLabel selected = new JLabel("Selecione uma linha.");
        grid.addEventListener(EventGridView.SELECTION_ROW, event -> {
            var row = grid.getSelectedGridRow();
            selected.setText(row == null ? "Selecione uma linha."
                    : "getCell(\"nome\") = " + row.getCell("nome") + "   •   getCell(0) = " + row.getCell(0)
                    + "   •   toMap() = " + row.toMap());
        });
        grid.addEventListener(EventGridView.CELL_EDIT, event -> {
            EventGrid edit = (EventGrid) event.getValue();
            selected.setText("Edição em \"" + edit.getFieldPath() + "\": " + edit.getOldValue() + " → " + edit.getNewValue());
        });

        JTextField search = new JTextField(16);
        search.putClientProperty("JTextField.placeholderText", "Filtrar por nome");
        search.addActionListener(event -> grid.setColumnTextFilter("nome", search.getText()));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 4));
        top.add(new JLabel("Colunas inferidas das chaves do Map. Nome (Enter):"));
        top.add(search);

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(grid), BorderLayout.CENTER);
        panel.add(selected, BorderLayout.SOUTH);
        return panel;
    }

    private static GridRowForm customProductForm(Product product) {
        ModernComponentDialog.FormPanel form = new ModernComponentDialog.FormPanel();
        JTextField name = new JTextField(product.name, 24);
        JTextField price = new JTextField(Double.toString(product.price), 12);
        JCheckBox active = new JCheckBox("Disponível", product.active);
        form.field("name", "Nome", name);
        form.field("price", "Preço (R$)", price);
        form.field("active", "Situação", active);
        return new GridRowForm(form, () -> Map.of(
                "name", name.getText(), "price", price.getText(), "active", active.isSelected()));
    }

    private static List<Product> sampleProducts() {
        String[][] groups = {
                {"Tecnologia", "Notebook", "Monitor", "Teclado", "Mouse", "Webcam", "Tablet"},
                {"Escritório", "Caderno", "Caneta", "Agenda", "Pasta", "Calculadora", "Organizador"},
                {"Casa", "Luminária", "Garrafa", "Relógio", "Suporte", "Cafeteira", "Ventilador"},
                {"Áudio", "Headset", "Microfone", "Caixa de som", "Fone Bluetooth", "Soundbar", "Gravador"}
        };
        List<Product> products = new ArrayList<>();
        for (String[] group : groups) {
            for (int index = 1; index < group.length; index++) {
                int id = products.size() + 1;
                products.add(new Product(id, group[index], group[0], 39.90 + id * 47.50,
                        (id * 7 + 3) % 18, id % 7 != 0));
            }
        }
        return products;
    }

    public static final class Product {
        @GridColumn(name = "Código", order = 1, width = 70, editable = false)
        public int id;
        @GridColumn(name = "Produto", order = 2, width = 245)
        public String name;
        @GridColumn(name = "Categoria", order = 3, width = 170, editable = false)
        public String category;
        @GridColumn(name = "Preço (R$)", order = 4, width = 120)
        public double price;
        @GridColumn(name = "Estoque", order = 5, width = 90)
        public int stock;
        @GridColumn(name = "Ativo", order = 6, width = 75)
        public boolean active;
        @GridColumn(name = "Fornecedor", order = 7, width = 170)
        public Vendor vendor;
        @GridColumn(visible = false)
        public String internalCode;

        public Product(int id, String name, String category, double price, int stock, boolean active) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.stock = stock;
            this.active = active;
            this.internalCode = "SKU-" + id;
        }
    }

    public static final class Vendor {
        @GridColumn(name = "Nome", order = 1)
        public String name;
        @GridColumn(name = "Endereço", order = 2)
        public VendorAddress address;

        public Vendor() {}
        public Vendor(String name, VendorAddress address) { this.name = name; this.address = address; }
        @Override public String toString() { return name == null || name.isBlank() ? "Novo fornecedor" : name; }
    }

    public static final class VendorAddress {
        @GridColumn(name = "Cidade")
        public String city;

        public VendorAddress() {}
        public VendorAddress(String city) { this.city = city; }
        @Override public String toString() { return city == null || city.isBlank() ? "Novo endereço" : city; }
    }
}
