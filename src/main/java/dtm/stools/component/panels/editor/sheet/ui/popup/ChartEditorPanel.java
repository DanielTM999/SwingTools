package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.model.ChartSeries;
import dtm.stools.component.panels.editor.sheet.model.ChartType;
import dtm.stools.component.panels.editor.sheet.model.LegendPosition;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

public class ChartEditorPanel extends JPanel {
    private final SheetChart chart;
    private final JComboBox<ChartType> type = new JComboBox<>(ChartType.values());
    private final JTextField title = SheetForm.text("", 24), xTitle = SheetForm.text("", 16), yTitle = SheetForm.text("", 16);
    private final JComboBox<LegendPosition> legend = new JComboBox<>(LegendPosition.values());
    private final JCheckBox labels = SheetForm.check("Rótulos de dados", false), grid = SheetForm.check("Linhas de grade", true);
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Nome", "Categorias (X)", "Valores (Y)"}, 0);

    public ChartEditorPanel(SheetChart chart) {
        super(new BorderLayout(0, 8));
        this.chart = chart;
        SheetForm f = new SheetForm();
        f.add("Tipo:", type);
        f.add("Título:", title);
        f.add("Título do eixo X:", xTitle);
        f.add("Título do eixo Y:", yTitle);
        f.add("Legenda:", legend);
        f.full(labels);
        f.full(grid);
        add(f, BorderLayout.NORTH);
        type.setSelectedItem(chart.type());
        title.setText(chart.title());
        xTitle.setText(chart.xAxisTitle());
        yTitle.setText(chart.yAxisTitle());
        legend.setSelectedItem(chart.legend());
        labels.setSelected(chart.dataLabels());
        grid.setSelected(chart.gridlines());
        for (ChartSeries s : chart.series()) model.addRow(new Object[]{s.name(), s.categoriesRef(), s.valuesRef()});
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(560, 150));
        add(scroll, BorderLayout.CENTER);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("Adicionar Série"), remove = new JButton("Remover Série");
        add.addActionListener(e -> model.addRow(new Object[]{"Série " + (model.getRowCount() + 1), "", ""}));
        remove.addActionListener(e -> { int i = table.getSelectedRow(); if (i >= 0) model.removeRow(i); });
        tools.add(add);
        tools.add(remove);
        add(tools, BorderLayout.SOUTH);
    }

    private static String cell(Object o) { return o == null || o.toString().isBlank() ? null : o.toString().strip().replaceFirst("^=", ""); }

    public SheetChart result() {
        List<ChartSeries> series = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String values = cell(model.getValueAt(i, 2));
            if (values == null) continue;
            ChartSeries old = i < chart.series().size() ? chart.series().get(i) : null;
            series.add(new ChartSeries(cell(model.getValueAt(i, 0)), old == null ? null : old.nameRef(), cell(model.getValueAt(i, 1)), values, old == null ? null : old.sizesRef(),
                    old == null ? null : old.color(), old == null ? null : old.type(), old != null && old.secondaryAxis()));
        }
        if (series.isEmpty()) throw new IllegalArgumentException("O gráfico precisa de ao menos uma série.");
        return chart.toBuilder().type((ChartType) type.getSelectedItem()).title(title.getText()).xAxisTitle(xTitle.getText()).yAxisTitle(yTitle.getText())
                .legend((LegendPosition) legend.getSelectedItem()).dataLabels(labels.isSelected()).gridlines(grid.isSelected()).series(series).build();
    }
}
