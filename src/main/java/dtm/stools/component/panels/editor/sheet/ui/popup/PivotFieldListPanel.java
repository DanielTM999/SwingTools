package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.model.PivotAggregation;
import dtm.stools.component.panels.editor.sheet.model.PivotField;
import dtm.stools.component.panels.editor.sheet.model.PivotShowAs;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.PivotValueField;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class PivotFieldListPanel extends JPanel {
    private final PivotTable base;
    private final JList<String> fields;
    private final DefaultListModel<String> filters = new DefaultListModel<>(), columns = new DefaultListModel<>(), rows = new DefaultListModel<>(), values = new DefaultListModel<>();
    private final List<PivotAggregation> aggregations = new ArrayList<>();
    private final List<PivotShowAs> showAs = new ArrayList<>();
    private final JCheckBox rowTotals, columnTotals, compact;

    public PivotFieldListPanel(PivotTable pivot, List<String> available) {
        super(new BorderLayout(8, 8));
        base = pivot;
        fields = new JList<>(available.toArray(String[]::new));
        JPanel left = new JPanel(new BorderLayout(0, 4));
        left.add(new JLabel("Escolha os campos para adicionar ao relatório:"), BorderLayout.NORTH);
        JScrollPane fs = new JScrollPane(fields);
        fs.setPreferredSize(new Dimension(200, 300));
        left.add(fs, BorderLayout.CENTER);
        JPanel add = new JPanel(new GridLayout(4, 1, 2, 2));
        add.add(button("→ Filtros", filters));
        add.add(button("→ Colunas", columns));
        add.add(button("→ Linhas", rows));
        add.add(button("→ Valores", values));
        left.add(add, BorderLayout.SOUTH);
        add(left, BorderLayout.WEST);
        JPanel areas = new JPanel(new GridLayout(2, 2, 6, 6));
        areas.add(area("Filtros", filters));
        areas.add(area("Colunas", columns));
        areas.add(area("Linhas", rows));
        areas.add(valuesArea());
        add(areas, BorderLayout.CENTER);
        JPanel opts = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        rowTotals = new JCheckBox("Totais gerais de linhas", pivot.rowGrandTotals());
        columnTotals = new JCheckBox("Totais gerais de colunas", pivot.columnGrandTotals());
        compact = new JCheckBox("Layout compacto", pivot.compact());
        opts.add(rowTotals);
        opts.add(columnTotals);
        opts.add(compact);
        add(opts, BorderLayout.SOUTH);
        pivot.filters().forEach(f -> filters.addElement(f.name()));
        pivot.columns().forEach(f -> columns.addElement(f.name()));
        pivot.rows().forEach(f -> rows.addElement(f.name()));
        for (PivotValueField v : pivot.values()) { values.addElement(v.field()); aggregations.add(v.aggregation()); showAs.add(v.showAs()); }
        setPreferredSize(new Dimension(760, 420));
    }

    private JButton button(String label, DefaultListModel<String> target) {
        JButton b = new JButton(label);
        b.addActionListener(e -> {
            for (String f : fields.getSelectedValuesList()) {
                if (target == values) { target.addElement(f); aggregations.add(PivotAggregation.SUM); showAs.add(PivotShowAs.NORMAL); }
                else { removeEverywhere(f); target.addElement(f); }
            }
        });
        return b;
    }

    private void removeEverywhere(String f) { filters.removeElement(f); columns.removeElement(f); rows.removeElement(f); }

    private JPanel area(String title, DefaultListModel<String> model) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setBorder(BorderFactory.createTitledBorder(title));
        JList<String> list = new JList<>(model);
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        JButton up = new JButton("▲"), down = new JButton("▼"), remove = new JButton("Remover");
        up.addActionListener(e -> move(list, model, -1));
        down.addActionListener(e -> move(list, model, 1));
        remove.addActionListener(e -> { int i = list.getSelectedIndex(); if (i >= 0) { model.remove(i); if (model == values) { aggregations.remove(i); showAs.remove(i); } } });
        tools.add(up);
        tools.add(down);
        tools.add(remove);
        p.add(tools, BorderLayout.SOUTH);
        return p;
    }

    private JPanel valuesArea() {
        JPanel p = area("Valores", values);
        JList<?> list = (JList<?>) ((JScrollPane) p.getComponent(0)).getViewport().getView();
        JComboBox<PivotAggregation> agg = new JComboBox<>(PivotAggregation.values());
        JComboBox<PivotShowAs> show = new JComboBox<>(PivotShowAs.values());
        agg.addActionListener(e -> { int i = list.getSelectedIndex(); if (i >= 0 && i < aggregations.size()) aggregations.set(i, (PivotAggregation) agg.getSelectedItem()); });
        show.addActionListener(e -> { int i = list.getSelectedIndex(); if (i >= 0 && i < showAs.size()) showAs.set(i, (PivotShowAs) show.getSelectedItem()); });
        list.addListSelectionListener(e -> { int i = list.getSelectedIndex(); if (i >= 0 && i < aggregations.size()) { agg.setSelectedItem(aggregations.get(i)); show.setSelectedItem(showAs.get(i)); } });
        JPanel north = new JPanel(new GridLayout(1, 2, 4, 0));
        north.add(agg);
        north.add(show);
        p.add(north, BorderLayout.NORTH);
        return p;
    }

    private void move(JList<String> list, DefaultListModel<String> model, int delta) {
        int i = list.getSelectedIndex(), j = i + delta;
        if (i < 0 || j < 0 || j >= model.size()) return;
        String v = model.remove(i);
        model.add(j, v);
        if (model == values) { aggregations.add(j, aggregations.remove(i)); showAs.add(j, showAs.remove(i)); }
        list.setSelectedIndex(j);
    }

    private static List<PivotField> fields(DefaultListModel<String> model, List<PivotField> old) {
        List<PivotField> list = new ArrayList<>();
        for (int i = 0; i < model.size(); i++) {
            String n = model.get(i);
            list.add(old.stream().filter(f -> f.name().equals(n)).findFirst().orElse(PivotField.of(n)));
        }
        return list;
    }

    public PivotTable result() {
        List<PivotValueField> v = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) v.add(new PivotValueField(values.get(i), aggregations.get(i), showAs.get(i), null, null));
        if (v.isEmpty() && rows.isEmpty() && columns.isEmpty()) throw new IllegalArgumentException("Adicione ao menos um campo ao relatório.");
        return base.toBuilder().filters(fields(filters, base.filters())).columns(fields(columns, base.columns())).rows(fields(rows, base.rows())).values(v)
                .rowGrandTotals(rowTotals.isSelected()).columnGrandTotals(columnTotals.isSelected()).compact(compact.isSelected()).build();
    }
}
