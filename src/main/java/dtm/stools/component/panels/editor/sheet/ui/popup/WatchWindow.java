package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.ProviderRegistration;
import dtm.stools.component.panels.editor.sheet.calc.CalcListener;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class WatchWindow {
    private record Watch(String sheetId, CellAddress cell) {}

    private final SheetEditor editor;
    private final List<Watch> watches = new ArrayList<>();
    private SheetDialogActivity<Void> dialog;
    private ProviderRegistration listener = ProviderRegistration.none();
    private final AbstractTableModel model = new AbstractTableModel() {
        @Override public int getRowCount() { return watches.size(); }
        @Override public int getColumnCount() { return 4; }
        @Override public String getColumnName(int c) { return new String[]{"Planilha", "Célula", "Valor", "Fórmula"}[c]; }
        @Override public Object getValueAt(int r, int c) {
            Watch w = watches.get(r);
            int s = editor.getWorkbook().indexOfId(w.sheetId());
            if (s < 0) return c == 0 ? "(excluída)" : "";
            SheetCell cell = editor.getWorkbook().sheet(s).cell(w.cell());
            return switch (c) {
                case 0 -> editor.getWorkbook().sheet(s).name();
                case 1 -> w.cell().toA1();
                case 2 -> editor.displayText(s, w.cell());
                default -> cell.hasFormula() ? editor.editing().editText(s, w.cell()) : "";
            };
        }
    };

    public WatchWindow(SheetEditor editor) { this.editor = editor; }

    public void addSelection() {
        String id = editor.activeSheet().id();
        for (CellRange r : editor.getSelection().ranges()) {
            CellRange b = editor.clipboard().bounded(editor.activeSheet(), r);
            if (b.cellCount() > 500) continue;
            for (CellAddress a : b) if (watches.stream().noneMatch(w -> w.sheetId().equals(id) && w.cell().equals(a))) watches.add(new Watch(id, a));
        }
        model.fireTableDataChanged();
    }

    public void open() {
        if (dialog != null && dialog.isDisplayable()) { dialog.toFront(); return; }
        dialog = new SheetDialogActivity<>(editor, "Janela de Inspeção", Dialog.ModalityType.MODELESS);
        JTable table = new JTable(model);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new Dimension(560, 200));
        JPanel content = new JPanel(new BorderLayout(0, 6));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton add = new JButton("Adicionar Inspeção"), remove = new JButton("Excluir Inspeção");
        add.addActionListener(e -> addSelection());
        remove.addActionListener(e -> { int i = table.getSelectedRow(); if (i >= 0) { watches.remove(i); model.fireTableDataChanged(); } });
        tools.add(add);
        tools.add(remove);
        content.add(tools, BorderLayout.NORTH);
        content.add(scroll, BorderLayout.CENTER);
        dialog.setBody(content);
        dialog.addAction("Fechar", dialog::dispose, true);
        listener = editor.addCalcListener(new CalcListener() {
            @Override public void valuesChanged(Map<String, List<CellRange>> changed) { model.fireTableDataChanged(); }
        });
        dialog.onClosed(() -> listener.close());
        dialog.open();
    }
}
