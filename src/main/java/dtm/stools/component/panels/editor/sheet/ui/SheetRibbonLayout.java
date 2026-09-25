package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.SheetEditor;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.util.List;

import static dtm.stools.component.panels.editor.sheet.ui.RibbonItem.component;
import static dtm.stools.component.panels.editor.sheet.ui.RibbonItem.large;
import static dtm.stools.component.panels.editor.sheet.ui.RibbonItem.small;

public final class SheetRibbonLayout {
    private SheetRibbonLayout() {}

    private static RibbonGroup group(String id, String title, int priority, String icon, RibbonItem... items) { return new RibbonGroup(id, title, priority, icon, List.of(items)); }

    public static List<RibbonTab> defaults(SheetEditor editor, SheetRibbon ribbon) {
        RibbonTab file = new RibbonTab("file", "Arquivo", List.of(
                group("file", "Arquivo", 100, "save", large("sheet.file.new"), large("sheet.file.open"), large("sheet.file.save"), small("sheet.file.saveAs"), small("sheet.file.print"), small("sheet.file.recover")),
                group("export", "Exportar", 80, "pdf", large("sheet.file.exportPdf"), small("sheet.file.exportCsv"), small("sheet.file.exportOds"), small("sheet.file.exportHtml"), small("sheet.file.importCsv")),
                group("props", "Informações", 50, "names", small("sheet.file.properties"), small("sheet.commandPalette"))), false);
        RibbonTab home = new RibbonTab("home", "Página Inicial", List.of(
                group("clipboard", "Área de Transferência", 20, "paste", large("sheet.paste.menu"), small("sheet.cut"), small("sheet.copy"), small("sheet.formatPainter")),
                group("font", "Fonte", 100, "bold",
                        component(() -> fontRow(ribbon)),
                        component(() -> ribbon.styleButtons("sheet.font.bold", "sheet.font.italic", "sheet.font.underline", "sheet.font.strike", "|", "sheet.font.grow", "sheet.font.shrink")),
                        component(() -> colorRow(ribbon))),
                group("alignment", "Alinhamento", 90, "align-center",
                        component(() -> ribbon.styleButtons("sheet.align.top", "sheet.align.middle", "sheet.align.bottom", "|", "sheet.orientation.menu", "sheet.align.wrap")),
                        component(() -> ribbon.styleButtons("sheet.align.left", "sheet.align.center", "sheet.align.right", "|", "sheet.align.outdent", "sheet.align.indent")),
                        component(() -> ribbon.styleButtons("sheet.merge.menu"))),
                group("number", "Número", 80, "number-format",
                        component(() -> ribbon.formatBox()),
                        component(() -> ribbon.styleButtons("sheet.number.currency", "sheet.number.percent", "sheet.number.comma", "|", "sheet.number.increaseDecimals", "sheet.number.decreaseDecimals"))),
                group("styles", "Estilos", 50, "cond-format", large("sheet.cf.menu"), large("sheet.table.format"), large("sheet.cellStyles")),
                group("cells", "Células", 40, "insert-cells", large("sheet.insert.menu"), large("sheet.delete.menu"), large("sheet.format.menu")),
                group("editing", "Edição", 60, "autosum", small("sheet.autosum.menu"), small("sheet.fill.menu"), small("sheet.clear.menu"), large("sheet.sortFilter.menu"), large("sheet.find.menu"))), false);
        RibbonTab insert = new RibbonTab("insert", "Inserir", List.of(
                group("tables", "Tabelas", 70, "table", large("sheet.pivot.insert"), large("sheet.table.format")),
                group("illustrations", "Ilustrações", 40, "image", large("sheet.insert.image"), small("sheet.insert.shape.menu"), small("sheet.insert.textbox")),
                group("charts", "Gráficos", 100, "chart-column", large("sheet.chart.recommended"), small("sheet.chart.column"), small("sheet.chart.bar"), small("sheet.chart.line"), small("sheet.chart.pie"),
                        small("sheet.chart.area"), small("sheet.chart.scatter"), small("sheet.chart.combo"), small("sheet.chart.more.menu")),
                group("sparklines", "Minigráficos", 30, "sparkline", small("sheet.sparkline.line"), small("sheet.sparkline.column"), small("sheet.sparkline.winloss")),
                group("filters", "Filtros", 20, "slicer", large("sheet.slicer.insert")),
                group("links", "Links", 50, "link", large("sheet.link.insert")),
                group("comments", "Comentários", 45, "comment", large("sheet.comment.new"), small("sheet.note.new")),
                group("controls", "Controles", 35, "checkbox", large("sheet.insert.checkbox"), large("sheet.insert.dropdown"))), false);
        RibbonTab layout = new RibbonTab("layout", "Layout da Página", List.of(
                group("setup", "Configurar Página", 100, "margins", large("sheet.page.margins.menu"), large("sheet.page.orientation.menu"), large("sheet.page.size.menu"),
                        large("sheet.page.printArea.menu"), large("sheet.page.breaks.menu"), large("sheet.page.titles"), small("sheet.page.setup")),
                group("sheetOptions", "Opções de Planilha", 60, "gridlines", small("sheet.view.gridlines"), small("sheet.page.gridlinesPrint"), small("sheet.view.headings"), small("sheet.page.headingsPrint")),
                group("themes", "Temas", 40, "cell-styles", large("sheet.page.theme.menu"))), false);
        RibbonTab formulas = new RibbonTab("formulas", "Fórmulas", List.of(
                group("library", "Biblioteca de Funções", 100, "function", large("sheet.insertFunction"), large("sheet.autosum.menu"), large("sheet.fn.financial"), large("sheet.fn.logical"), large("sheet.fn.text"),
                        large("sheet.fn.date"), large("sheet.fn.lookup"), large("sheet.fn.math"), large("sheet.fn.more")),
                group("names", "Nomes Definidos", 60, "names", large("sheet.names.manager"), small("sheet.names.define"), small("sheet.names.use"), small("sheet.names.fromSelection")),
                group("auditing", "Auditoria de Fórmulas", 50, "trace-precedents", small("sheet.audit.precedents"), small("sheet.audit.dependents"), small("sheet.audit.removeArrows"),
                        small("sheet.view.formulas"), small("sheet.audit.errorCheck"), small("sheet.audit.evaluate"), large("sheet.audit.watch")),
                group("calculation", "Cálculo", 70, "calc-now", large("sheet.calc.menu"), small("sheet.calc.now"), small("sheet.calc.sheet"))), false);
        RibbonTab data = new RibbonTab("data", "Dados", List.of(
                group("get", "Obter e Transformar Dados", 40, "data", large("sheet.data.importText"), large("sheet.data.refreshAll")),
                group("sortFilter", "Classificar e Filtrar", 100, "sort-filter", small("sheet.sort.asc"), small("sheet.sort.desc"), large("sheet.sort.custom"), large("sheet.filter.toggle"),
                        small("sheet.filter.clear"), small("sheet.filter.reapply"), small("sheet.filterView.create")),
                group("tools", "Ferramentas de Dados", 90, "text-to-columns", large("sheet.data.textToColumns"), small("sheet.fill.flash"), small("sheet.data.removeDuplicates"),
                        small("sheet.data.validation.menu"), small("sheet.data.consolidate")),
                group("forecast", "Previsão", 50, "what-if", large("sheet.data.whatIf.menu"), large("sheet.data.forecast")),
                group("outline", "Estrutura de Tópicos", 60, "group", large("sheet.data.group"), large("sheet.data.ungroup"), large("sheet.data.subtotal"), small("sheet.data.showDetail"), small("sheet.data.hideDetail"))), false);
        RibbonTab review = new RibbonTab("review", "Revisão", List.of(
                group("proofing", "Revisão de Texto", 40, "spelling", large("sheet.review.spelling"), small("sheet.review.statistics")),
                group("comments", "Comentários", 90, "comment", large("sheet.comment.new"), small("sheet.comment.delete"), small("sheet.comment.previous"), small("sheet.comment.next"), large("sheet.comment.showAll")),
                group("notes", "Anotações", 70, "note", large("sheet.note.new"), small("sheet.note.showAll")),
                group("protect", "Proteger", 80, "protect", large("sheet.protect.sheet"), large("sheet.protect.workbook"), small("sheet.protect.ranges")),
                group("ai", "Assistente", 30, "ai", large("sheet.ai.assistant"))), false);
        RibbonTab view = new RibbonTab("view", "Exibir", List.of(
                group("modes", "Modos de Exibição de Pasta de Trabalho", 60, "normal-view", large("sheet.view.normal"), large("sheet.view.pageBreak"), large("sheet.view.pageLayout")),
                group("show", "Mostrar", 50, "gridlines", small("sheet.view.gridlines"), small("sheet.view.formulaBar"), small("sheet.view.headings")),
                group("zoom", "Zoom", 70, "zoom", large("sheet.view.zoomDialog"), large("sheet.view.zoom100"), large("sheet.view.zoomSelection")),
                group("window", "Janela", 100, "freeze", large("sheet.freeze.menu"), small("sheet.view.ribbon"), small("sheet.commandPalette"))), false);
        RibbonTab table = new RibbonTab("table", "Design da Tabela", List.of(
                group("properties", "Propriedades", 60, "table", small("sheet.table.rename"), small("sheet.table.resize")),
                group("tools", "Ferramentas", 50, "pivot", small("sheet.table.removeDuplicates"), small("sheet.table.convertToRange"), small("sheet.slicer.insert")),
                group("options", "Opções de Estilo de Tabela", 100, "table", small("sheet.table.headerRow"), small("sheet.table.totalRow"), small("sheet.table.bandedRows"),
                        small("sheet.table.firstColumn"), small("sheet.table.lastColumn"), small("sheet.table.bandedColumns"), small("sheet.table.filterButton")),
                group("styles", "Estilos de Tabela", 70, "cell-styles", large("sheet.table.style.menu"))), true);
        RibbonTab chart = new RibbonTab("chart", "Design do Gráfico", List.of(
                group("type", "Tipo", 100, "chart-combo", large("sheet.chart.changeType.menu"), large("sheet.chart.editData")),
                group("elements", "Elementos", 80, "chart", small("sheet.chart.title"), small("sheet.chart.legend.menu"), small("sheet.chart.dataLabels"), small("sheet.chart.gridlines")),
                group("arrange", "Organizar", 40, "shapes", small("sheet.object.bringFront"), small("sheet.object.sendBack"), small("sheet.object.delete"))), true);
        RibbonTab pivot = new RibbonTab("pivot", "Tabela Dinâmica", List.of(
                group("data", "Dados", 100, "refresh", large("sheet.pivot.refresh"), large("sheet.pivot.fields")),
                group("actions", "Ações", 50, "pivot", small("sheet.pivot.options"), small("sheet.pivot.delete"), small("sheet.slicer.insert"))), true);
        return List.of(file, home, insert, layout, formulas, data, review, view, table, chart, pivot);
    }

    private static JComponent fontRow(SheetRibbon ribbon) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        p.setOpaque(false);
        p.add(ribbon.fontBox());
        p.add(ribbon.sizeBox());
        return p;
    }

    private static JComponent colorRow(SheetRibbon ribbon) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        p.setOpaque(false);
        p.add(ribbon.styleButtons("sheet.border.menu"));
        p.add(ribbon.colorButton("sheet.fill.color", "fill", "Cor de Preenchimento", true));
        p.add(ribbon.colorButton("sheet.font.color", "font-color", "Cor da Fonte", false));
        return p;
    }
}
