package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.model.SortKey;
import dtm.stools.component.panels.editor.sheet.model.SortOn;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class SortPanel extends JPanel {
    private final List<Integer> columns;
    private final Function<Boolean, List<String>> names;
    private final JPanel levels = new JPanel();
    private final JCheckBox header;
    private final JCheckBox caseSensitive = new JCheckBox("Diferenciar maiúsculas de minúsculas");
    private final List<Level> rows = new ArrayList<>();

    private final class Level {
        final JComboBox<String> column = new JComboBox<>();
        final JComboBox<String> order = new JComboBox<>(new String[]{"De A a Z / Menor para Maior", "De Z a A / Maior para Menor", "Lista Personalizada"});
        final JTextField custom = new JTextField(16);
        final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));

        Level(int columnIndex, boolean descending) {
            for (String n : names.apply(header.isSelected())) column.addItem(n);
            column.setSelectedIndex(Math.max(0, columnIndex));
            order.setSelectedIndex(descending ? 1 : 0);
            custom.setVisible(false);
            custom.setToolTipText("Itens separados por vírgula");
            order.addActionListener(e -> { custom.setVisible(order.getSelectedIndex() == 2); panel.revalidate(); });
            panel.add(new JLabel(rows.isEmpty() ? "Classificar por" : "E depois por"));
            panel.add(column);
            panel.add(order);
            panel.add(custom);
        }

        SortKey key() {
            int c = columns.get(Math.max(0, column.getSelectedIndex()));
            if (order.getSelectedIndex() == 2) return new SortKey(c, false, SortOn.VALUES, null, Arrays.stream(custom.getText().split("[,;]")).map(String::strip).filter(s -> !s.isEmpty()).toList());
            return SortKey.of(c, order.getSelectedIndex() == 1);
        }
    }

    public SortPanel(List<Integer> columns, Function<Boolean, List<String>> names, boolean hasHeader, int activeColumn) {
        super(new BorderLayout(0, 8));
        this.columns = List.copyOf(columns);
        this.names = names;
        header = new JCheckBox("Meus dados contêm cabeçalhos", hasHeader);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("Adicionar Nível");
        JButton remove = new JButton("Excluir Nível");
        tools.add(add);
        tools.add(remove);
        tools.add(header);
        add(tools, BorderLayout.NORTH);
        levels.setLayout(new BoxLayout(levels, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(levels);
        scroll.setPreferredSize(new Dimension(620, 200));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);
        add(caseSensitive, BorderLayout.SOUTH);
        addLevel(Math.max(0, columns.indexOf(activeColumn)), false);
        add.addActionListener(e -> addLevel(0, false));
        remove.addActionListener(e -> { if (rows.size() > 1) { Level l = rows.removeLast(); levels.remove(l.panel); levels.revalidate(); levels.repaint(); } });
        header.addActionListener(e -> {
            List<String> n = names.apply(header.isSelected());
            for (Level l : rows) { int i = l.column.getSelectedIndex(); l.column.removeAllItems(); for (String s : n) l.column.addItem(s); l.column.setSelectedIndex(Math.max(0, i)); }
        });
    }

    private void addLevel(int column, boolean descending) {
        Level l = new Level(column, descending);
        rows.add(l);
        levels.add(l.panel);
        levels.revalidate();
    }

    public List<SortKey> keys() { return rows.stream().map(Level::key).toList(); }
    public boolean hasHeader() { return header.isSelected(); }
    public boolean caseSensitive() { return caseSensitive.isSelected(); }
}
