package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class WordChartEditorPanel extends WordPropertiesPanel<WordChart> {
    private final WordChart chart;
    private final WordDocument document;
    private final WordObjectRegistry registry;
    private final DefaultTableModel data = new DefaultTableModel();
    private final JTable grid = new JTable(data);
    private final JComboBox<WordChartType> type = new JComboBox<>(WordChartType.values());
    private final JTextField title = new JTextField(), categoryAxis = new JTextField(), valueAxis = new JTextField();
    private final JCheckBox legend = new JCheckBox("Legenda"), labels = new JCheckBox("Rótulos de dados");
    private final JComboBox<WordChart.LegendPosition> legendPosition = new JComboBox<>(WordChart.LegendPosition.values());
    private final List<Integer> colors = new ArrayList<>();
    private final JLabel error = new JLabel(" ");
    private final Supplier<WordPlacement> placement;
    private final JSpinner width, height;
    private final JComponent preview;
    private WordChart last;

    public WordChartEditorPanel(WordChart chart, WordDocument document, WordObjectRegistry registry) {
        this.chart = chart; this.document = document; this.registry = registry; this.last = chart;
        type.setSelectedItem(chart.chartType()); row("Tipo",type);
        title.setText(chart.title()); row("Título",title);
        JPanel legendRow = new JPanel(new FlowLayout(FlowLayout.LEADING,0,0)); legendRow.setOpaque(false);
        legend.setSelected(chart.legend()); legend.setOpaque(false); legendPosition.setSelectedItem(chart.legendPosition());
        legendPosition.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list,Object value,int index,boolean selected,boolean focus) {
                String label=value instanceof WordChart.LegendPosition position ? switch(position) {
                    case RIGHT -> "À direita";case BOTTOM -> "Abaixo";case TOP -> "Acima";case LEFT -> "À esquerda";
                } : "";
                return super.getListCellRendererComponent(list,label,index,selected,focus);
            }
        });
        labels.setSelected(chart.dataLabels()); labels.setOpaque(false);
        legendRow.add(legend); legendRow.add(legendPosition); legendRow.add(Box.createHorizontalStrut(12)); legendRow.add(labels);
        row("Exibição",legendRow);
        categoryAxis.setText(chart.categoryAxisTitle()); row("Título do eixo de categorias",categoryAxis);
        valueAxis.setText(chart.valueAxisTitle()); row("Título do eixo de valores",valueAxis);
        width = row("Largura (pt)",number(chart.width(),36,1440,1)); height = row("Altura (pt)",number(chart.height(),36,1440,1));
        placement = placement(chart.placement());
        data.addColumn("Categoria");
        for (WordChartSeries s : chart.series()) { data.addColumn(s.name()); colors.add(s.color()); }
        Object[] names = new Object[chart.series().size()+1]; names[0] = "(nome da série)";
        for (int i = 0; i < chart.series().size(); i++) names[i+1] = chart.series().get(i).name();
        data.addRow(names);
        for (int c = 0; c < chart.categories().size(); c++) {
            Object[] row = new Object[chart.series().size()+1]; row[0] = chart.categories().get(c);
            for (int s = 0; s < chart.series().size(); s++) { double v = chart.series().get(s).value(c); row[s+1] = Double.isNaN(v) ? "" : format(v); }
            data.addRow(row);
        }
        grid.setRowHeight(22); grid.getTableHeader().setReorderingAllowed(false); grid.setCellSelectionEnabled(true);
        grid.putClientProperty("terminateEditOnFocusLost",true);
        JScrollPane scroll = new JScrollPane(grid); scroll.setPreferredSize(new Dimension(420,170));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEADING,4,0)); tools.setOpaque(false);
        tools.add(button("+ Categoria",() -> { Object[] r = new Object[data.getColumnCount()]; r[0] = "Categoria " + data.getRowCount(); for (int i = 1; i < r.length; i++) r[i] = ""; data.addRow(r); }));
        tools.add(button("− Categoria",() -> { int r = grid.getSelectedRow(); if (data.getRowCount() > 2) data.removeRow(r > 0 ? r : data.getRowCount()-1); }));
        tools.add(button("+ Série",() -> { String n = "Série " + data.getColumnCount(); Object[] column = new Object[data.getRowCount()]; column[0] = n; for (int i = 1; i < column.length; i++) column[i] = ""; data.addColumn(n,column); colors.add(null); }));
        tools.add(button("− Série",() -> removeSeries()));
        tools.add(button("Cor da série",() -> {
            int s = Math.max(1,grid.getSelectedColumn()) - 1;
            if (s < 0 || s >= colors.size()) return;
            Color current = colors.get(s) == null ? dtm.stools.component.panels.editor.word.render.WordPaintSupport.palette(s) : new Color(colors.get(s));
            WordColors.show(this,"Cor da série",current,chosen -> { colors.set(s,chosen.getRGB() & 0xffffff); refresh(); },null);
        }));
        wide(new JLabel("Dados (primeira linha: nomes das séries; primeira coluna: categorias)"),0);
        wide(tools,0);
        wide(scroll,1);
        preview = new JComponent() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D)g.create();
                try {
                    WordChart c = last; float s = Math.min(getWidth()/c.width(),getHeight()/c.height());
                    float w = c.width()*s, h = c.height()*s;
                    registry.paint(g2,c.resize(Math.max(36,w),Math.max(36,h)),new Rectangle2D.Float((getWidth()-w)/2,(getHeight()-h)/2,w,h),document);
                } finally { g2.dispose(); }
            }
        };
        preview.setPreferredSize(new Dimension(420,200));
        wide(preview,1);
        error.setForeground(new Color(0xB91C1C)); wide(error,0);
        data.addTableModelListener(e -> refresh());
        type.addActionListener(e -> refresh()); legend.addActionListener(e -> refresh()); labels.addActionListener(e -> refresh()); legendPosition.addActionListener(e -> refresh());
        for (JTextField f : new JTextField[]{title,categoryAxis,valueAxis}) f.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
    }
    private JButton button(String text, Runnable action) { JButton b = new JButton(text); b.addActionListener(e -> { if (grid.isEditing()) grid.getCellEditor().stopCellEditing(); action.run(); refresh(); }); return b; }
    private void removeSeries() {
        if (data.getColumnCount() <= 2) return;
        int column = grid.getSelectedColumn(); if (column < 1) column = data.getColumnCount()-1;
        List<Object[]> rows = new ArrayList<>(); List<String> headers = new ArrayList<>();
        for (int c = 0; c < data.getColumnCount(); c++) if (c != column) headers.add(data.getColumnName(c));
        for (int r = 0; r < data.getRowCount(); r++) { List<Object> row = new ArrayList<>(); for (int c = 0; c < data.getColumnCount(); c++) if (c != column) row.add(data.getValueAt(r,c)); rows.add(row.toArray()); }
        colors.remove(column-1);
        data.setColumnCount(0); data.setRowCount(0);
        for (String h : headers) data.addColumn(h);
        for (Object[] r : rows) data.addRow(r);
    }
    private void refresh() {
        try { last = build(); error.setText(" "); } catch (IllegalArgumentException e) { error.setText(e.getMessage()); }
        preview.repaint();
    }
    private WordChart build() {
        List<String> categories = new ArrayList<>(); List<WordChartSeries> series = new ArrayList<>();
        int seriesCount = data.getColumnCount()-1;
        for (int r = 1; r < data.getRowCount(); r++) categories.add(text(data.getValueAt(r,0)));
        for (int s = 0; s < seriesCount; s++) {
            List<Double> values = new ArrayList<>();
            for (int r = 1; r < data.getRowCount(); r++) values.add(parse(text(data.getValueAt(r,s+1)),r,s));
            String name = text(data.getValueAt(0,s+1)); if (name.isBlank()) name = "Série " + (s+1);
            series.add(new WordChartSeries(name,values,s < colors.size() ? colors.get(s) : null));
        }
        if (categories.isEmpty() || series.isEmpty()) throw new IllegalArgumentException("Informe ao menos uma categoria e uma série");
        return chart.withChartType((WordChartType)type.getSelectedItem()).withTitle(title.getText()).withData(categories,series)
                .withLegend(legend.isSelected(),(WordChart.LegendPosition)legendPosition.getSelectedItem()).withDataLabels(labels.isSelected())
                .withAxisTitles(categoryAxis.getText(),valueAxis.getText()).resize(value(width),value(height));
    }
    private static String text(Object o) { return o == null ? "" : o.toString().strip(); }
    static double parse(String value, int row, int series) {
        if (value.isEmpty()) return Double.NaN;
        String v = value.replace(" ","");
        if (v.contains(",")) v = v.replace(".","").replace(',','.');
        try { double d = Double.parseDouble(v); if (Double.isInfinite(d)) throw new NumberFormatException(); return d; }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Valor inválido na linha " + row + ", série " + (series+1) + ": " + value); }
    }
    private static String format(double v) { return v == Math.rint(v) && Math.abs(v) < 1e12 ? Long.toString((long)v) : Double.toString(v).replace('.',','); }
    @Override public String title() { return "Dados e formatação do gráfico"; }
    @Override public WordChart result() {
        if (grid.isEditing()) grid.getCellEditor().stopCellEditing();
        return build().withPlacement(placement.get());
    }
}
