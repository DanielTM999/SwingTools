package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.PageOrientation;
import dtm.stools.component.panels.editor.sheet.model.PaperSize;
import dtm.stools.component.panels.editor.sheet.model.PrintSettings;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ButtonGroup;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Function;

public class PageSetupPanel extends javax.swing.JPanel {
    private final PrintSettings base;
    private final JComboBox<String> orientation = SheetForm.combo("Retrato", "Paisagem");
    private final JComboBox<PaperSize> paper = new JComboBox<>(PaperSize.values());
    private final JRadioButton adjust = new JRadioButton("Ajustar para:"), fit = new JRadioButton("Ajustar para páginas:");
    private final JSpinner scale, fitW, fitH;
    private final JSpinner top, bottom, left, right, header, footer;
    private final JCheckBox centerH, centerV, gridlines, headings, overThenDown;
    private final JTextField hl, hc, hr, fl, fc, fr, area, rows, cols;
    private final Function<String, CellRange> parser;

    public PageSetupPanel(PrintSettings settings, Function<String, CellRange> parser) {
        super(new BorderLayout());
        base = settings;
        this.parser = parser;
        JTabbedPane tabs = new JTabbedPane();
        SheetForm page = new SheetForm();
        page.add("Orientação:", orientation);
        orientation.setSelectedIndex(settings.orientation() == PageOrientation.LANDSCAPE ? 1 : 0);
        page.add("Tamanho do papel:", paper);
        paper.setSelectedItem(settings.paper());
        ButtonGroup g = new ButtonGroup();
        g.add(adjust);
        g.add(fit);
        scale = SheetForm.integer(settings.scale(), 10, 400);
        fitW = SheetForm.integer(Math.max(1, settings.fitWidth()), 0, 100);
        fitH = SheetForm.integer(settings.fitHeight(), 0, 100);
        (settings.fitWidth() > 0 || settings.fitHeight() > 0 ? fit : adjust).setSelected(true);
        page.full(adjust);
        page.add("% do tamanho normal:", scale);
        page.full(fit);
        page.add("Páginas de largura:", fitW);
        page.add("Páginas de altura (0 = automático):", fitH);
        tabs.addTab("Página", page);
        SheetForm margins = new SheetForm();
        top = SheetForm.number(settings.marginTop() * 2.54, 0, 20, 0.1);
        bottom = SheetForm.number(settings.marginBottom() * 2.54, 0, 20, 0.1);
        left = SheetForm.number(settings.marginLeft() * 2.54, 0, 20, 0.1);
        right = SheetForm.number(settings.marginRight() * 2.54, 0, 20, 0.1);
        header = SheetForm.number(settings.marginHeader() * 2.54, 0, 20, 0.1);
        footer = SheetForm.number(settings.marginFooter() * 2.54, 0, 20, 0.1);
        margins.add("Superior (cm):", top);
        margins.add("Inferior (cm):", bottom);
        margins.add("Esquerda (cm):", left);
        margins.add("Direita (cm):", right);
        margins.add("Cabeçalho (cm):", header);
        margins.add("Rodapé (cm):", footer);
        centerH = margins.full(SheetForm.check("Centralizar horizontalmente", settings.centerHorizontally()));
        centerV = margins.full(SheetForm.check("Centralizar verticalmente", settings.centerVertically()));
        tabs.addTab("Margens", margins);
        SheetForm hf = new SheetForm();
        hf.section("Códigos: &P página, &N total de páginas, &D data, &T hora, &A planilha, &F arquivo");
        hl = hf.add("Cabeçalho esquerdo:", SheetForm.text(settings.headerLeft(), 24));
        hc = hf.add("Cabeçalho central:", SheetForm.text(settings.headerCenter(), 24));
        hr = hf.add("Cabeçalho direito:", SheetForm.text(settings.headerRight(), 24));
        fl = hf.add("Rodapé esquerdo:", SheetForm.text(settings.footerLeft(), 24));
        fc = hf.add("Rodapé central:", SheetForm.text(settings.footerCenter(), 24));
        fr = hf.add("Rodapé direito:", SheetForm.text(settings.footerRight(), 24));
        tabs.addTab("Cabeçalho/Rodapé", hf);
        SheetForm sheet = new SheetForm();
        area = sheet.add("Área de impressão:", SheetForm.text(settings.printArea() == null ? "" : settings.printArea().toA1(), 18));
        rows = sheet.add("Linhas a repetir na parte superior:", SheetForm.text(settings.repeatRowFirst() == null ? "" : "$" + (settings.repeatRowFirst() + 1) + ":$" + ((settings.repeatRowLast() == null ? settings.repeatRowFirst() : settings.repeatRowLast()) + 1), 12));
        cols = sheet.add("Colunas a repetir à esquerda:", SheetForm.text(settings.repeatColumnFirst() == null ? "" : "$" + dtm.stools.component.panels.editor.sheet.model.CellAddress.columnName(settings.repeatColumnFirst()) + ":$"
                + dtm.stools.component.panels.editor.sheet.model.CellAddress.columnName(settings.repeatColumnLast() == null ? settings.repeatColumnFirst() : settings.repeatColumnLast()), 12));
        gridlines = sheet.full(SheetForm.check("Linhas de grade", settings.gridlines()));
        headings = sheet.full(SheetForm.check("Títulos de linha e coluna", settings.headings()));
        overThenDown = sheet.full(SheetForm.check("Ordem: da esquerda para a direita, depois para baixo", settings.overThenDown()));
        tabs.addTab("Planilha", sheet);
        add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(520, 380));
    }

    public PrintSettings result() {
        CellRange printArea = area.getText().isBlank() ? null : parser.apply(area.getText().strip());
        CellRange repeatRows = rows.getText().isBlank() ? null : parser.apply(rows.getText().strip());
        CellRange repeatCols = cols.getText().isBlank() ? null : parser.apply(cols.getText().strip());
        return base.toBuilder()
                .orientation(orientation.getSelectedIndex() == 1 ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT)
                .paper((PaperSize) paper.getSelectedItem())
                .scale(adjust.isSelected() ? SheetForm.integer(scale) : 100)
                .fitWidth(fit.isSelected() ? SheetForm.integer(fitW) : 0).fitHeight(fit.isSelected() ? SheetForm.integer(fitH) : 0)
                .marginTop(SheetForm.number(top) / 2.54).marginBottom(SheetForm.number(bottom) / 2.54).marginLeft(SheetForm.number(left) / 2.54).marginRight(SheetForm.number(right) / 2.54)
                .marginHeader(SheetForm.number(header) / 2.54).marginFooter(SheetForm.number(footer) / 2.54)
                .centerHorizontally(centerH.isSelected()).centerVertically(centerV.isSelected())
                .headerLeft(hl.getText()).headerCenter(hc.getText()).headerRight(hr.getText()).footerLeft(fl.getText()).footerCenter(fc.getText()).footerRight(fr.getText())
                .printArea(printArea)
                .repeatRowFirst(repeatRows == null ? null : repeatRows.firstRow()).repeatRowLast(repeatRows == null ? null : repeatRows.lastRow())
                .repeatColumnFirst(repeatCols == null ? null : repeatCols.firstColumn()).repeatColumnLast(repeatCols == null ? null : repeatCols.lastColumn())
                .gridlines(gridlines.isSelected()).headings(headings.isSelected()).overThenDown(overThenDown.isSelected())
                .build();
    }
}
