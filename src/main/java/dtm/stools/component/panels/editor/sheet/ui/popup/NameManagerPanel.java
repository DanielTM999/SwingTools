package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.model.DefinedName;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NameManagerPanel extends JPanel {
    private final List<DefinedName> names;
    private final List<String> sheets;
    private final Function<String, String> toDisplay, toCanonical;
    private final String defaultRef;
    private final AbstractTableModel model = new AbstractTableModel() {
        @Override public int getRowCount() { return names.size(); }
        @Override public int getColumnCount() { return 4; }
        @Override public String getColumnName(int c) { return new String[]{"Nome", "Refere-se a", "Escopo", "Comentário"}[c]; }
        @Override public Object getValueAt(int r, int c) {
            DefinedName n = names.get(r);
            return switch (c) {
                case 0 -> n.name();
                case 1 -> toDisplay.apply(n.formula());
                case 2 -> n.sheetScope() == null ? "Pasta de trabalho" : n.sheetScope() < sheets.size() ? sheets.get(n.sheetScope()) : "?";
                default -> n.comment() == null ? "" : n.comment();
            };
        }
    };

    public NameManagerPanel(List<DefinedName> current, List<String> sheets, String defaultRef, Function<String, String> toDisplay, Function<String, String> toCanonical) {
        super(new BorderLayout(0, 8));
        this.names = new ArrayList<>(current.stream().filter(n -> !n.hidden()).toList());
        this.sheets = List.copyOf(sheets);
        this.toDisplay = toDisplay;
        this.toCanonical = toCanonical;
        this.defaultRef = defaultRef;
        hiddenNames = current.stream().filter(DefinedName::hidden).toList();
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(620, 260));
        add(scroll, BorderLayout.CENTER);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("Novo…"), edit = new JButton("Editar…"), delete = new JButton("Excluir");
        add.addActionListener(e -> form(null).ifPresent(n -> { names.add(n); model.fireTableDataChanged(); }));
        edit.addActionListener(e -> { int i = table.getSelectedRow(); if (i >= 0) form(names.get(i)).ifPresent(n -> { names.set(i, n); model.fireTableDataChanged(); }); });
        delete.addActionListener(e -> { int i = table.getSelectedRow(); if (i >= 0) { names.remove(i); model.fireTableDataChanged(); } });
        tools.add(add);
        tools.add(edit);
        tools.add(delete);
        add(tools, BorderLayout.NORTH);
    }

    private final List<DefinedName> hiddenNames;

    private java.util.Optional<DefinedName> form(DefinedName current) {
        SheetForm f = new SheetForm();
        JTextField name = SheetForm.text(current == null ? "" : current.name(), 20);
        List<String> scopes = new ArrayList<>();
        scopes.add("Pasta de trabalho");
        scopes.addAll(sheets);
        JComboBox<String> scope = new JComboBox<>(scopes.toArray(String[]::new));
        if (current != null && current.sheetScope() != null) scope.setSelectedIndex(current.sheetScope() + 1);
        JTextField comment = SheetForm.text(current == null ? "" : current.comment(), 24);
        JTextField ref = SheetForm.text(current == null ? defaultRef : toDisplay.apply(current.formula()), 24);
        f.add("Nome:", name);
        f.add("Escopo:", scope);
        f.add("Comentário:", comment);
        f.add("Refere-se a:", ref);
        while (true) {
            int answer = JOptionPane.showConfirmDialog(this, f, current == null ? "Novo Nome" : "Editar Nome", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) return java.util.Optional.empty();
            try {
                String n = name.getText().strip();
                if (!n.matches("[\\p{L}_\\\\][\\p{L}\\p{N}_.]*") || n.replace("$", "").matches("[A-Za-z]{1,3}\\d+")) throw new IllegalArgumentException("O nome inserido não é válido.");
                Integer sc = scope.getSelectedIndex() == 0 ? null : scope.getSelectedIndex() - 1;
                for (DefinedName other : names) if (other != current && other.name().equalsIgnoreCase(n) && java.util.Objects.equals(other.sheetScope(), sc)) throw new IllegalArgumentException("Esse nome já existe.");
                return java.util.Optional.of(new DefinedName(n, toCanonical.apply(ref.getText().strip()), sc, false, comment.getText().strip()));
            } catch (RuntimeException failure) {
                JOptionPane.showMessageDialog(this, failure.getMessage(), "Nome", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    public List<DefinedName> result() {
        List<DefinedName> all = new ArrayList<>(names);
        all.addAll(hiddenNames);
        return all;
    }
}
