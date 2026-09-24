package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.api.WordCellSelection;
import dtm.stools.component.panels.editor.word.editing.WordTableEditing;
import dtm.stools.component.panels.editor.word.model.*;
import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public final class WordTablePropertiesPanel extends WordPropertiesPanel<WordTable> {
    private final WordTable table;
    private final WordCellSelection cells;
    private final JComboBox<WordTable.Alignment> alignment = new JComboBox<>(WordTable.Alignment.values());
    private final JComboBox<WordBorder.Style> borderStyle = new JComboBox<>(WordBorder.Style.values());
    private final JSpinner borderWidth, padding, columnWidth, rowHeight;
    private final ColorButton borderColor, fill;
    private final JComboBox<WordTableCell.VerticalAlign> verticalAlign = new JComboBox<>(WordTableCell.VerticalAlign.values());
    private final JCheckBox header = new JCheckBox("Repetir como linha de cabeçalho em cada página"), cantSplit = new JCheckBox("Não dividir a linha entre páginas");
    private final int row, column;

    public WordTablePropertiesPanel(WordTable table, WordCellSelection cells, int row, int column) {
        this.table = table; this.cells = cells; this.row = row; this.column = column;
        row(null,new JLabel(table.rows().size() + " linhas × " + table.gridColumns() + " colunas"));
        alignment.setSelectedItem(table.alignment()); row("Alinhamento da tabela",alignment);
        borderStyle.setSelectedItem(table.border().style()); row("Borda",borderStyle);
        borderWidth = row("Espessura da borda (pt)",number(table.border().width(),0,12,0.25));
        borderColor = new ColorButton(table.border().color(),false); row("Cor da borda",borderColor);
        padding = row("Margem interna das células (pt)",number(table.cellPadding(),0,72,0.5));
        columnWidth = row("Largura da coluna " + (column+1) + " (pt)",number(table.columnWidths().get(column),12,1440,1));
        WordTableRow r = table.rows().get(row);
        rowHeight = row("Altura mínima da linha " + (row+1) + " (pt)",number(r.height(),0,1440,1));
        header.setSelected(r.header()); header.setOpaque(false); row(null,header);
        cantSplit.setSelected(r.cantSplit()); cantSplit.setOpaque(false); row(null,cantSplit);
        WordTableCell cell = table.cell(row,column);
        fill = new ColorButton(cell == null ? null : cell.fill(),true); row(cells == null ? "Preenchimento da célula" : "Preenchimento das células",fill.withClear());
        verticalAlign.setSelectedItem(cell == null ? WordTableCell.VerticalAlign.TOP : cell.verticalAlign()); row("Alinhamento vertical",verticalAlign);
    }
    @Override public String title() { return "Propriedades da tabela"; }
    @Override public WordTable result() {
        WordTable t = table.withAlignment((WordTable.Alignment)alignment.getSelectedItem())
                .withBorder(new WordBorder((WordBorder.Style)borderStyle.getSelectedItem(),value(borderWidth),borderColor.color()))
                .withCellPadding(value(padding));
        List<Float> widths = new ArrayList<>(t.columnWidths()); widths.set(column,value(columnWidth)); t = t.withColumnWidths(widths);
        List<WordTableRow> rows = new ArrayList<>(t.rows());
        int firstRow = cells == null ? row : cells.firstRow(), lastRow = cells == null ? row : cells.lastRow();
        for (int r = firstRow; r <= lastRow; r++) rows.set(r,rows.get(r).withHeight(value(rowHeight)).withHeader(header.isSelected()).withCantSplit(cantSplit.isSelected()));
        t = t.withRows(rows);
        int fc = cells == null ? column : cells.firstColumn(), lc = cells == null ? column : cells.lastColumn();
        Integer color = fill.color(); WordTableCell.VerticalAlign va = (WordTableCell.VerticalAlign)verticalAlign.getSelectedItem();
        return WordTableEditing.updateCells(t,firstRow,fc,lastRow,lc,c -> c.withFill(color).withVerticalAlign(va));
    }
}
