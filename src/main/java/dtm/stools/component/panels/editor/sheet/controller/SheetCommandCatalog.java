package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.CalcMode;
import dtm.stools.component.panels.editor.sheet.command.SheetOperations;
import dtm.stools.component.panels.editor.sheet.data.AutoFill;
import dtm.stools.component.panels.editor.sheet.function.FunctionCategory;
import dtm.stools.component.panels.editor.sheet.model.ChartType;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.LegendPosition;
import dtm.stools.component.panels.editor.sheet.model.PageOrientation;
import dtm.stools.component.panels.editor.sheet.model.PaperSize;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetTheme;
import dtm.stools.component.panels.editor.sheet.model.SheetViewMode;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.ShapeType;
import dtm.stools.component.panels.editor.sheet.model.SparklineType;
import dtm.stools.component.panels.editor.sheet.model.UnderlineStyle;
import dtm.stools.component.panels.editor.sheet.model.VerticalAlignment;
import dtm.stools.component.panels.editor.sheet.ui.TableStyles;
import dtm.stools.component.panels.editor.sheet.ui.popup.GoToSpecialPanel;

import javax.swing.JColorChooser;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

import static dtm.stools.component.panels.editor.sheet.controller.SheetCommandRegistry.alt;
import static dtm.stools.component.panels.editor.sheet.controller.SheetCommandRegistry.ctrl;
import static dtm.stools.component.panels.editor.sheet.controller.SheetCommandRegistry.ctrlShift;
import static dtm.stools.component.panels.editor.sheet.controller.SheetCommandRegistry.key;
import static dtm.stools.component.panels.editor.sheet.controller.SheetCommandRegistry.plain;
import static dtm.stools.component.panels.editor.sheet.controller.SheetCommandRegistry.shift;

public final class SheetCommandCatalog {
    private static final String FILE = "Arquivo", HOME = "Página Inicial", INSERT = "Inserir", LAYOUT = "Layout da Página", FORMULAS = "Fórmulas", DATA = "Dados",
            REVIEW = "Revisão", VIEW = "Exibir", TABLE = "Tabela", CHART = "Gráfico", PIVOT = "Tabela Dinâmica", NAV = "Navegação", SHEET = "Planilhas";

    private SheetCommandCatalog() {}

    public static void registerAll(SheetEditor e, SheetCommandRegistry r) {
        file(e, r);
        clipboard(e, r);
        font(e, r);
        alignment(e, r);
        number(e, r);
        styles(e, r);
        cells(e, r);
        editing(e, r);
        insert(e, r);
        layout(e, r);
        formulas(e, r);
        data(e, r);
        review(e, r);
        view(e, r);
        table(e, r);
        chart(e, r);
        pivot(e, r);
        sheets(e, r);
        navigation(e, r);
        bindings(e, r);
        r.refresh();
    }

    private static void file(SheetEditor e, SheetCommandRegistry r) {
        r.add("sheet.file.new", "Novo", "new", FILE, false, () -> e.files().newWorkbook());
        r.add("sheet.file.open", "Abrir", "open", FILE, false, () -> e.files().openDialog());
        r.add("sheet.file.save", "Salvar", "save", FILE, false, () -> e.files().save());
        r.add("sheet.file.saveAs", "Salvar Como", "save", FILE, false, () -> e.files().saveAs());
        r.add("sheet.file.print", "Imprimir", "print", FILE, false, () -> e.files().print());
        r.add("sheet.file.recover", "Recuperar", "refresh", FILE, false, () -> e.files().recover());
        r.add("sheet.file.exportPdf", "Exportar PDF", "pdf", FILE, false, () -> e.files().exportDialog(SheetEditor.ExportFormat.PDF));
        r.add("sheet.file.exportCsv", "Exportar CSV", "data", FILE, false, () -> e.files().exportDialog(SheetEditor.ExportFormat.CSV));
        r.add("sheet.file.exportOds", "Exportar ODS", "sheet", FILE, false, () -> e.files().exportDialog(SheetEditor.ExportFormat.ODS));
        r.add("sheet.file.exportHtml", "Exportar HTML", "link", FILE, false, () -> e.files().exportDialog(SheetEditor.ExportFormat.HTML));
        r.add("sheet.file.importCsv", "Importar Texto/CSV", "data", FILE, true, () -> e.files().importCsvDialog());
        r.add("sheet.data.importText", "De Texto/CSV", "data", DATA, true, () -> e.files().importCsvDialog());
        r.add("sheet.file.properties", "Propriedades", "names", FILE, true, () -> e.files().properties());
        r.add("sheet.commandPalette", "Paleta de Comandos", "palette", VIEW, false, () -> e.popups().openPalette());
    }

    private static void clipboard(SheetEditor e, SheetCommandRegistry r) {
        r.add("sheet.cut", "Recortar", "cut", HOME, true, () -> e.clipboard().cut());
        r.add("sheet.copy", "Copiar", "copy", HOME, false, () -> e.clipboard().copy());
        r.add("sheet.paste", "Colar", "paste", HOME, true, () -> e.clipboard().paste());
        r.add("sheet.paste.values", "Colar Valores", "paste", HOME, true, () -> e.clipboard().paste(PasteOptions.of(PasteOptions.What.VALUES)));
        r.add("sheet.paste.formulas", "Colar Fórmulas", "paste", HOME, true, () -> e.clipboard().paste(PasteOptions.of(PasteOptions.What.FORMULAS)));
        r.add("sheet.paste.formats", "Colar Formatação", "format-painter", HOME, true, () -> e.clipboard().paste(PasteOptions.of(PasteOptions.What.FORMATS)));
        r.add("sheet.paste.transpose", "Transpor", "paste", HOME, true, () -> e.clipboard().paste(PasteOptions.ALL.withTranspose(true)));
        r.add("sheet.paste.link", "Colar Vínculo", "link", HOME, true, () -> e.clipboard().paste(new PasteOptions(PasteOptions.What.ALL, PasteOptions.Operation.NONE, false, false, true)));
        r.add("sheet.pasteSpecial", "Colar Especial…", "paste", HOME, true, () -> SheetCommandSupport.pasteSpecial(e));
        r.menu("sheet.paste.menu", "Colar", "paste", HOME, "sheet.paste", "sheet.paste.values", "sheet.paste.formulas", "sheet.paste.formats", "sheet.paste.transpose", "sheet.paste.link", "-", "sheet.pasteSpecial");
        r.toggle("sheet.formatPainter", "Pincel de Formatação", "format-painter", HOME, true, () -> e.format().isPainterArmed(), () -> e.format().armPainter(false));
        r.add("sheet.undo", "Desfazer", "undo", HOME, true, e::undo).when(() -> e.getSession().canUndo());
        r.add("sheet.redo", "Refazer", "redo", HOME, true, e::redo).when(() -> e.getSession().canRedo());
    }

    private static void font(SheetEditor e, SheetCommandRegistry r) {
        FormatController f = e.format();
        r.toggle("sheet.font.bold", "Negrito", "bold", HOME, true, () -> f.activeStyle().bold(), f::bold);
        r.toggle("sheet.font.italic", "Itálico", "italic", HOME, true, () -> f.activeStyle().italic(), f::italic);
        r.toggle("sheet.font.underline", "Sublinhado", "underline", HOME, true, () -> f.activeStyle().underline() != UnderlineStyle.NONE, f::underline);
        r.add("sheet.font.doubleUnderline", "Sublinhado Duplo", "underline", HOME, true, f::doubleUnderline);
        r.toggle("sheet.font.strike", "Tachado", "strike", HOME, true, () -> f.activeStyle().strikethrough(), f::strike);
        r.add("sheet.font.grow", "Aumentar Fonte", "font-color", HOME, true, () -> f.growFont(true)).iconOnly();
        r.add("sheet.font.shrink", "Diminuir Fonte", "font-color", HOME, true, () -> f.growFont(false)).iconOnly();
        r.add("sheet.fill.color", "Cor de Preenchimento", "fill", HOME, true, () -> pickColor(e, true));
        r.add("sheet.font.color", "Cor da Fonte", "font-color", HOME, true, () -> pickColor(e, false));
        String[][] borders = {{"bottom", "Borda Inferior", "BOTTOM"}, {"top", "Borda Superior", "TOP"}, {"left", "Borda Esquerda", "LEFT"}, {"right", "Borda Direita", "RIGHT"},
                {"none", "Sem Borda", "NONE"}, {"all", "Todas as Bordas", "ALL"}, {"outside", "Bordas Externas", "OUTSIDE"}, {"thickOutside", "Borda Externa Espessa", "THICK_OUTSIDE"},
                {"doubleBottom", "Borda Inferior Dupla", "DOUBLE_BOTTOM"}, {"thickBottom", "Borda Inferior Espessa", "THICK_BOTTOM"}, {"topBottom", "Borda Superior e Inferior", "TOP_BOTTOM"},
                {"topThickBottom", "Borda Superior e Inferior Espessa", "TOP_THICK_BOTTOM"}, {"topDoubleBottom", "Borda Superior e Inferior Dupla", "TOP_DOUBLE_BOTTOM"}, {"inside", "Bordas Internas", "INSIDE"}};
        List<String> ids = new ArrayList<>();
        for (String[] b : borders) {
            FormatController.Border kind = FormatController.Border.valueOf(b[2]);
            r.add("sheet.border." + b[0], b[1], "borders", HOME, true, () -> f.border(kind));
            ids.add("sheet.border." + b[0]);
        }
        ids.add("-");
        ids.add("sheet.format.borders");
        r.add("sheet.format.borders", "Mais Bordas…", "borders", HOME, true, () -> SheetCommandSupport.formatCells(e, 3));
        r.menu("sheet.border.menu", "Bordas", "borders", HOME, ids.toArray(String[]::new)).iconOnly();
    }

    private static void alignment(SheetEditor e, SheetCommandRegistry r) {
        FormatController f = e.format();
        r.toggle("sheet.align.top", "Alinhar em Cima", "align-top", HOME, true, () -> f.activeStyle().vertical() == VerticalAlignment.TOP, () -> f.vertical(VerticalAlignment.TOP));
        r.toggle("sheet.align.middle", "Alinhar no Meio", "align-middle", HOME, true, () -> f.activeStyle().vertical() == VerticalAlignment.CENTER, () -> f.vertical(VerticalAlignment.CENTER));
        r.toggle("sheet.align.bottom", "Alinhar Embaixo", "align-bottom", HOME, true, () -> f.activeStyle().vertical() == VerticalAlignment.BOTTOM, () -> f.vertical(VerticalAlignment.BOTTOM));
        r.toggle("sheet.align.left", "Alinhar à Esquerda", "align-left", HOME, true, () -> f.activeStyle().horizontal() == HorizontalAlignment.LEFT, () -> f.horizontal(HorizontalAlignment.LEFT));
        r.toggle("sheet.align.center", "Centralizar", "align-center", HOME, true, () -> f.activeStyle().horizontal() == HorizontalAlignment.CENTER, () -> f.horizontal(HorizontalAlignment.CENTER));
        r.toggle("sheet.align.right", "Alinhar à Direita", "align-right", HOME, true, () -> f.activeStyle().horizontal() == HorizontalAlignment.RIGHT, () -> f.horizontal(HorizontalAlignment.RIGHT));
        r.toggle("sheet.align.wrap", "Quebrar Texto Automaticamente", "wrap", HOME, true, () -> f.activeStyle().wrap(), f::wrap);
        r.add("sheet.align.indent", "Aumentar Recuo", "indent", HOME, true, () -> f.indent(1));
        r.add("sheet.align.outdent", "Diminuir Recuo", "outdent", HOME, true, () -> f.indent(-1));
        r.add("sheet.orientation.ccw", "Girar Texto para Cima", "orientation", HOME, true, () -> f.rotation(45));
        r.add("sheet.orientation.cw", "Girar Texto para Baixo", "orientation", HOME, true, () -> f.rotation(135));
        r.add("sheet.orientation.vertical", "Texto Vertical", "orientation", HOME, true, () -> f.rotation(255));
        r.add("sheet.orientation.up", "Girar Texto 90° para Cima", "orientation", HOME, true, () -> f.rotation(90));
        r.add("sheet.orientation.down", "Girar Texto 90° para Baixo", "orientation", HOME, true, () -> f.rotation(180));
        r.add("sheet.format.alignment", "Formatar Alinhamento de Célula…", "format-cells", HOME, true, () -> SheetCommandSupport.formatCells(e, 1));
        r.menu("sheet.orientation.menu", "Orientação", "orientation", HOME, "sheet.orientation.ccw", "sheet.orientation.cw", "sheet.orientation.vertical", "sheet.orientation.up", "sheet.orientation.down", "-", "sheet.format.alignment").iconOnly();
        r.add("sheet.merge.center", "Mesclar e Centralizar", "merge", HOME, true, () -> e.structure().toggleMergeCenter());
        r.add("sheet.merge.across", "Mesclar Através", "merge", HOME, true, () -> e.structure().merge(false, true));
        r.add("sheet.merge.cells", "Mesclar Células", "merge", HOME, true, () -> e.structure().merge(false, false));
        r.add("sheet.merge.unmerge", "Desfazer Mesclagem", "merge", HOME, true, () -> e.structure().unmerge());
        r.menu("sheet.merge.menu", "Mesclar e Centralizar", "merge", HOME, "sheet.merge.center", "sheet.merge.across", "sheet.merge.cells", "sheet.merge.unmerge");
    }

    private static void number(SheetEditor e, SheetCommandRegistry r) {
        FormatController f = e.format();
        r.add("sheet.number.currency", "Formato de Número de Contabilização", "currency", HOME, true, f::accounting).iconOnly();
        r.add("sheet.number.currencySimple", "Moeda", "currency", HOME, true, f::currency);
        r.add("sheet.number.percent", "Estilo de Porcentagem", "percent", HOME, true, f::percent).iconOnly();
        r.add("sheet.number.comma", "Separador de Milhares", "comma", HOME, true, f::comma).iconOnly();
        r.add("sheet.number.increaseDecimals", "Aumentar Casas Decimais", "dec-inc", HOME, true, () -> f.decimals(1)).iconOnly();
        r.add("sheet.number.decreaseDecimals", "Diminuir Casas Decimais", "dec-dec", HOME, true, () -> f.decimals(-1)).iconOnly();
        r.add("sheet.number.date", "Data Abreviada", "number-format", HOME, true, () -> f.applyNumberFormat("dd/mm/yyyy"));
        r.add("sheet.number.general", "Geral", "number-format", HOME, true, () -> f.applyNumberFormat("General"));
        r.add("sheet.format.cells", "Formatar Células…", "format-cells", HOME, true, () -> SheetCommandSupport.formatCells(e, 0));
        r.add("sheet.format.font", "Fonte…", "format-cells", HOME, true, () -> SheetCommandSupport.formatCells(e, 2));
    }

    private static void styles(SheetEditor e, SheetCommandRegistry r) {
        ConditionalController cf = new ConditionalController(e);
        String[][] highlight = {{"greater", "É Maior do que…"}, {"less", "É Menor do que…"}, {"between", "Está Entre…"}, {"equal", "É Igual a…"}, {"text", "Texto que Contém…"},
                {"date", "Uma Data Ocorrendo…"}, {"duplicates", "Valores Duplicados"}, {"unique", "Valores Exclusivos"}};
        String[][] topBottom = {{"top10", "10 Primeiros Itens"}, {"top10pct", "10% Primeiros"}, {"bottom10", "10 Últimos Itens"}, {"bottom10pct", "10% Últimos"}, {"above", "Acima da Média"}, {"below", "Abaixo da Média"}};
        String[][] bars = {{"bar.blue", "Barra de Dados Azul"}, {"bar.green", "Barra de Dados Verde"}, {"bar.red", "Barra de Dados Vermelha"}, {"bar.orange", "Barra de Dados Laranja"}};
        String[][] scales = {{"scale.gyr", "Escala Verde - Amarelo - Vermelho"}, {"scale.ryg", "Escala Vermelho - Amarelo - Verde"}, {"scale.wr", "Escala Branco - Vermelho"}, {"scale.wg", "Escala Branco - Verde"}};
        String[][] icons = {{"icons.arrows", "3 Setas"}, {"icons.lights", "3 Sinais de Trânsito"}, {"icons.flags", "3 Sinalizadores"}, {"icons.stars", "3 Estrelas"}, {"icons.rating", "5 Classificações"}};
        List<String> menu = new ArrayList<>();
        menu.add(group(r, cf, "sheet.cf.highlight.menu", "Realçar Regras das Células", highlight));
        menu.add(group(r, cf, "sheet.cf.topBottom.menu", "Regras de Primeiros/Últimos", topBottom));
        menu.add(group(r, cf, "sheet.cf.bars.menu", "Barras de Dados", bars));
        menu.add(group(r, cf, "sheet.cf.scales.menu", "Escalas de Cor", scales));
        menu.add(group(r, cf, "sheet.cf.icons.menu", "Conjuntos de Ícones", icons));
        r.add("sheet.cf.new", "Nova Regra…", "cond-format", HOME, true, cf::newRule);
        r.add("sheet.cf.clear.selection", "Limpar Regras das Células Selecionadas", "clear", HOME, true, () -> e.format().clearConditionalRules(false));
        r.add("sheet.cf.clear.sheet", "Limpar Regras da Planilha Inteira", "clear", HOME, true, () -> e.format().clearConditionalRules(true));
        r.add("sheet.cf.manage", "Gerenciar Regras…", "cond-format", HOME, true, cf::manage);
        menu.addAll(List.of("-", "sheet.cf.new", "sheet.cf.clear.selection", "sheet.cf.clear.sheet", "sheet.cf.manage"));
        r.menu("sheet.cf.menu", "Formatação Condicional", "cond-format", HOME, menu.toArray(String[]::new));
        List<String> styleIds = new ArrayList<>();
        for (int i = 0; i < FormatController.CELL_STYLES.size(); i++) {
            String name = FormatController.CELL_STYLES.get(i);
            r.add("sheet.cellStyle." + i, name, "cell-styles", HOME, true, () -> e.format().cellStyle(name));
            styleIds.add("sheet.cellStyle." + i);
        }
        r.menu("sheet.cellStyles", "Estilos de Célula", "cell-styles", HOME, styleIds.toArray(String[]::new));
        List<String> light = new ArrayList<>(), medium = new ArrayList<>(), dark = new ArrayList<>();
        for (String name : TableStyles.names()) {
            String id = "sheet.table.style." + name;
            String label = name.replace("TableStyleLight", "Claro ").replace("TableStyleMedium", "Médio ").replace("TableStyleDark", "Escuro ");
            r.add(id, label, "table", HOME, true, () -> e.data().createTable(name));
            (name.contains("Light") ? light : name.contains("Dark") ? dark : medium).add(id);
        }
        r.menu("sheet.table.style.light", "Claro", "table", HOME, light.toArray(String[]::new));
        r.menu("sheet.table.style.medium", "Médio", "table", HOME, medium.toArray(String[]::new));
        r.menu("sheet.table.style.dark", "Escuro", "table", HOME, dark.toArray(String[]::new));
        r.menu("sheet.table.format", "Formatar como Tabela", "table", HOME, "sheet.table.style.TableStyleMedium2", "sheet.table.style.TableStyleMedium9", "sheet.table.style.TableStyleLight9", "-",
                "sheet.table.style.light", "sheet.table.style.medium", "sheet.table.style.dark", "-", "sheet.table.create");
        r.add("sheet.table.create", "Criar Tabela…", "table", INSERT, true, () -> e.data().createTable(null));
        r.menu("sheet.table.style.menu", "Estilos de Tabela", "cell-styles", TABLE, "sheet.table.style.light", "sheet.table.style.medium", "sheet.table.style.dark");
    }

    private static void pickColor(SheetEditor e, boolean fill) {
        java.awt.Rectangle r = e.popups().activeCellRect();
        dtm.stools.component.panels.editor.sheet.ui.ColorPalettePopup.show(e.getCanvas(), r.x, r.y + r.height, e.getWorkbook().properties().theme(), fill ? "Sem Preenchimento" : "Automático",
                c -> { if (fill) e.applyFill(c); else e.applyFontColor(c); });
    }

    private static String group(SheetCommandRegistry r, ConditionalController cf, String id, String name, String[][] items) {
        List<String> ids = new ArrayList<>();
        for (String[] it : items) {
            String kind = it[0];
            r.add("sheet.cf." + kind, it[1], "cond-format", HOME, true, () -> cf.quick(kind));
            ids.add("sheet.cf." + kind);
        }
        r.menu(id, name, "cond-format", HOME, ids.toArray(String[]::new));
        return id;
    }

    private static void cells(SheetEditor e, SheetCommandRegistry r) {
        StructureController s = e.structure();
        r.add("sheet.insert.cellsDialog", "Inserir Células…", "insert-cells", HOME, true, s::insertCellsDialog);
        r.add("sheet.insert.rows", "Inserir Linhas na Planilha", "insert-cells", HOME, true, s::insertRows);
        r.add("sheet.insert.columns", "Inserir Colunas na Planilha", "insert-cells", HOME, true, s::insertColumns);
        r.add("sheet.delete.cellsDialog", "Excluir Células…", "delete-cells", HOME, true, s::deleteCellsDialog);
        r.add("sheet.delete.rows", "Excluir Linhas da Planilha", "delete-cells", HOME, true, s::deleteRows);
        r.add("sheet.delete.columns", "Excluir Colunas da Planilha", "delete-cells", HOME, true, s::deleteColumns);
        r.menu("sheet.insert.menu", "Inserir", "insert-cells", HOME, "sheet.insert.cellsDialog", "sheet.insert.rows", "sheet.insert.columns", "sheet.sheet.insert");
        r.menu("sheet.delete.menu", "Excluir", "delete-cells", HOME, "sheet.delete.cellsDialog", "sheet.delete.rows", "sheet.delete.columns", "sheet.sheet.delete");
        r.add("sheet.row.height", "Altura da Linha…", "format-cells", HOME, true, () -> s.promptSize(true));
        r.add("sheet.row.autofit", "AutoAjuste da Altura da Linha", "format-cells", HOME, true, s::autoFitSelectionRows);
        r.add("sheet.column.width", "Largura da Coluna…", "format-cells", HOME, true, () -> s.promptSize(false));
        r.add("sheet.column.autofit", "AutoAjuste da Largura da Coluna", "format-cells", HOME, true, s::autoFitSelectionColumns);
        r.add("sheet.column.defaultWidth", "Largura Padrão…", "format-cells", HOME, true, s::defaultWidth);
        r.add("sheet.row.hide", "Ocultar Linhas", "hide", HOME, true, () -> s.setHidden(true, true));
        r.add("sheet.row.unhide", "Reexibir Linhas", "hide", HOME, true, () -> s.setHidden(true, false));
        r.add("sheet.column.hide", "Ocultar Colunas", "hide", HOME, true, () -> s.setHidden(false, true));
        r.add("sheet.column.unhide", "Reexibir Colunas", "hide", HOME, true, () -> s.setHidden(false, false));
        r.menu("sheet.visibility.menu", "Ocultar e Reexibir", "hide", HOME, "sheet.row.hide", "sheet.column.hide", "sheet.sheet.hide", "-", "sheet.row.unhide", "sheet.column.unhide", "sheet.sheet.unhide");
        r.menu("sheet.format.menu", "Formatar", "format-cells", HOME, "sheet.row.height", "sheet.row.autofit", "sheet.column.width", "sheet.column.autofit", "sheet.column.defaultWidth", "-",
                "sheet.visibility.menu", "-", "sheet.sheet.rename", "sheet.sheet.duplicate", "sheet.sheet.tabColor.menu", "-", "sheet.protect.sheet", "sheet.format.cells");
    }

    private static void editing(SheetEditor e, SheetCommandRegistry r) {
        String[][] sums = {{"SUM", "Soma"}, {"AVERAGE", "Média"}, {"COUNT", "Contar Números"}, {"MAX", "Máx"}, {"MIN", "Mín"}};
        List<String> ids = new ArrayList<>();
        for (String[] s : sums) {
            String fn = s[0];
            r.add("sheet.autosum." + fn.toLowerCase(), s[1], "autosum", HOME, true, () -> SheetCommandSupport.autosum(e, fn));
            ids.add("sheet.autosum." + fn.toLowerCase());
        }
        ids.add("-");
        ids.add("sheet.insertFunction");
        r.menu("sheet.autosum.menu", "AutoSoma", "autosum", HOME, ids.toArray(String[]::new));
        DataController d = e.data();
        r.add("sheet.fill.down", "Para Baixo", "fill-down", HOME, true, () -> d.fillDirection(AutoFill.Direction.DOWN));
        r.add("sheet.fill.right", "Para a Direita", "fill-down", HOME, true, () -> d.fillDirection(AutoFill.Direction.RIGHT));
        r.add("sheet.fill.up", "Para Cima", "fill-down", HOME, true, () -> d.fillDirection(AutoFill.Direction.UP));
        r.add("sheet.fill.left", "Para a Esquerda", "fill-down", HOME, true, () -> d.fillDirection(AutoFill.Direction.LEFT));
        r.add("sheet.fill.series", "Série…", "fill-down", HOME, true, d::seriesDialog);
        r.add("sheet.fill.flash", "Preenchimento Relâmpago", "flash-fill", DATA, true, d::flashFill);
        r.menu("sheet.fill.menu", "Preencher", "fill-down", HOME, "sheet.fill.down", "sheet.fill.right", "sheet.fill.up", "sheet.fill.left", "-", "sheet.fill.series", "sheet.fill.flash");
        StructureController s = e.structure();
        r.add("sheet.clear.all", "Limpar Tudo", "clear", HOME, true, () -> s.clear(SheetOperations.ClearMode.ALL));
        r.add("sheet.clear.formats", "Limpar Formatos", "clear", HOME, true, () -> s.clear(SheetOperations.ClearMode.FORMATS));
        r.add("sheet.clear.contents", "Limpar Conteúdo", "clear", HOME, true, () -> s.clear(SheetOperations.ClearMode.CONTENTS));
        r.add("sheet.clear.notes", "Limpar Comentários e Anotações", "clear", HOME, true, () -> e.review().deleteNotes());
        r.add("sheet.clear.links", "Remover Hiperlinks", "clear", HOME, true, () -> e.review().removeLinks());
        r.menu("sheet.clear.menu", "Limpar", "clear", HOME, "sheet.clear.all", "sheet.clear.formats", "sheet.clear.contents", "sheet.clear.notes", "sheet.clear.links");
        r.add("sheet.sort.asc", "Classificar de A a Z", "sort-asc", DATA, true, () -> d.sort(false));
        r.add("sheet.sort.desc", "Classificar de Z a A", "sort-desc", DATA, true, () -> d.sort(true));
        r.add("sheet.sort.custom", "Classificação Personalizada…", "sort", DATA, true, d::customSort);
        r.toggle("sheet.filter.toggle", "Filtro", "filter", DATA, true, () -> e.activeSheet().properties().autoFilter() != null, d::toggleFilter);
        r.add("sheet.filter.clear", "Limpar", "clear-filter", DATA, true, d::clearFilters);
        r.add("sheet.filter.reapply", "Reaplicar", "refresh", DATA, true, d::reapplyFilter);
        r.add("sheet.filter.bySelected", "Filtrar pelo Valor da Célula Selecionada", "filter", DATA, true, d::filterBySelectedValue);
        r.menu("sheet.sortFilter.menu", "Classificar e Filtrar", "sort-filter", HOME, "sheet.sort.asc", "sheet.sort.desc", "sheet.sort.custom", "-", "sheet.filter.toggle", "sheet.filter.clear", "sheet.filter.reapply");
        r.menu("sheet.context.filter.menu", "Filtrar", "filter", HOME, "sheet.filter.bySelected", "sheet.filter.clear", "sheet.filter.reapply");
        r.menu("sheet.context.sort.menu", "Classificar", "sort", HOME, "sheet.sort.asc", "sheet.sort.desc", "sheet.sort.custom");
        r.add("sheet.find", "Localizar…", "find", HOME, false, () -> e.popups().openSearch(false));
        r.add("sheet.replace", "Substituir…", "replace", HOME, true, () -> e.popups().openSearch(true));
        r.add("sheet.goTo", "Ir para…", "find", HOME, false, () -> SheetCommandSupport.goTo(e));
        r.add("sheet.goToSpecial", "Ir para Especial…", "find", HOME, false, () -> SheetCommandSupport.goToSpecial(e));
        r.add("sheet.select.formulas", "Fórmulas", "function", HOME, false, () -> SheetCommandSupport.selectSpecial(e, GoToSpecialPanel.Kind.FORMULAS));
        r.add("sheet.select.notes", "Comentários e Anotações", "comment", HOME, false, () -> SheetCommandSupport.selectSpecial(e, GoToSpecialPanel.Kind.NOTES));
        r.add("sheet.select.constants", "Constantes", "number-format", HOME, false, () -> SheetCommandSupport.selectSpecial(e, GoToSpecialPanel.Kind.CONSTANTS));
        r.add("sheet.select.validation", "Validação de Dados", "validation", HOME, false, () -> SheetCommandSupport.selectSpecial(e, GoToSpecialPanel.Kind.VALIDATION));
        r.add("sheet.select.objects", "Selecionar Objetos", "shapes", HOME, false, () -> SheetCommandSupport.selectSpecial(e, GoToSpecialPanel.Kind.OBJECTS));
        r.menu("sheet.find.menu", "Localizar e Selecionar", "find", HOME, "sheet.find", "sheet.replace", "sheet.goTo", "sheet.goToSpecial", "-", "sheet.select.formulas", "sheet.select.notes",
                "sheet.select.constants", "sheet.select.validation", "sheet.select.objects");
    }

    private static void insert(SheetEditor e, SheetCommandRegistry r) {
        ObjectController o = e.objects();
        r.add("sheet.pivot.insert", "Tabela Dinâmica", "pivot", INSERT, true, o::insertPivot);
        r.add("sheet.insert.image", "Imagens", "image", INSERT, true, o::insertImageFromFile);
        String[][] shapes = {{"RECTANGLE", "Retângulo"}, {"ROUNDED_RECTANGLE", "Retângulo Arredondado"}, {"ELLIPSE", "Elipse"}, {"TRIANGLE", "Triângulo"}, {"ARROW_RIGHT", "Seta para a Direita"}, {"LINE", "Linha"}};
        List<String> ids = new ArrayList<>();
        for (String[] s : shapes) {
            ShapeType t = ShapeType.valueOf(s[0]);
            r.add("sheet.shape." + s[0].toLowerCase(), s[1], "shapes", INSERT, true, () -> o.insertShape(t));
            ids.add("sheet.shape." + s[0].toLowerCase());
        }
        r.menu("sheet.insert.shape.menu", "Formas", "shapes", INSERT, ids.toArray(String[]::new));
        r.add("sheet.insert.textbox", "Caixa de Texto", "textbox", INSERT, true, () -> o.insertShape(ShapeType.TEXT_BOX));
        r.add("sheet.object.editText", "Editar Texto", "textbox", INSERT, true, o::editShapeText);
        r.add("sheet.chart.recommended", "Gráficos Recomendados", "chart", INSERT, true, o::recommendedChart);
        chartType(r, o, "sheet.chart.column", "Colunas", "chart-column", ChartType.COLUMN);
        chartType(r, o, "sheet.chart.bar", "Barras", "chart-bar", ChartType.BAR);
        chartType(r, o, "sheet.chart.line", "Linhas", "chart-line", ChartType.LINE);
        chartType(r, o, "sheet.chart.pie", "Pizza", "chart-pie", ChartType.PIE);
        chartType(r, o, "sheet.chart.area", "Área", "chart-area", ChartType.AREA);
        chartType(r, o, "sheet.chart.scatter", "Dispersão", "chart-scatter", ChartType.SCATTER);
        chartType(r, o, "sheet.chart.combo", "Combinação", "chart-combo", ChartType.COMBO);
        List<String> more = new ArrayList<>();
        for (ChartType t : ChartType.values()) {
            String id = "sheet.chart.insert." + t.name().toLowerCase();
            r.add(id, t.label(), "chart-other", INSERT, true, () -> o.insertChart(t));
            more.add(id);
        }
        r.menu("sheet.chart.more.menu", "Mais Gráficos", "chart-other", INSERT, more.toArray(String[]::new));
        r.add("sheet.sparkline.line", "Minigráfico de Linha", "sparkline", INSERT, true, () -> o.insertSparkline(SparklineType.LINE));
        r.add("sheet.sparkline.column", "Minigráfico de Coluna", "sparkline", INSERT, true, () -> o.insertSparkline(SparklineType.COLUMN));
        r.add("sheet.sparkline.winloss", "Minigráfico de Ganhos/Perdas", "sparkline", INSERT, true, () -> o.insertSparkline(SparklineType.WIN_LOSS));
        r.add("sheet.sparkline.clear", "Limpar Minigráficos", "clear", INSERT, true, o::clearSparklines);
        r.add("sheet.slicer.insert", "Segmentação de Dados", "slicer", INSERT, true, o::insertSlicer);
        r.add("sheet.link.insert", "Link", "link", INSERT, true, () -> e.review().insertLink());
        r.add("sheet.insert.checkbox", "Caixa de Seleção", "checkbox", INSERT, true, () -> e.data().insertCheckbox());
        r.add("sheet.insert.dropdown", "Lista Suspensa", "dropdown", INSERT, true, () -> e.data().insertDropdown());
    }

    private static void chartType(SheetCommandRegistry r, ObjectController o, String id, String name, String icon, ChartType t) {
        r.add(id, name, icon, INSERT, true, () -> o.insertChart(t));
    }

    private static void layout(SheetEditor e, SheetCommandRegistry r) {
        FileController f = e.files();
        r.add("sheet.page.margins.normal", "Normal", "margins", LAYOUT, true, () -> f.margins("normal"));
        r.add("sheet.page.margins.wide", "Largas", "margins", LAYOUT, true, () -> f.margins("wide"));
        r.add("sheet.page.margins.narrow", "Estreitas", "margins", LAYOUT, true, () -> f.margins("narrow"));
        r.add("sheet.page.setup", "Configurar Página…", "margins", LAYOUT, true, f::pageSetup);
        r.menu("sheet.page.margins.menu", "Margens", "margins", LAYOUT, "sheet.page.margins.normal", "sheet.page.margins.wide", "sheet.page.margins.narrow", "-", "sheet.page.setup");
        r.add("sheet.page.portrait", "Retrato", "page-orientation", LAYOUT, true, () -> f.orientation(PageOrientation.PORTRAIT));
        r.add("sheet.page.landscape", "Paisagem", "page-orientation", LAYOUT, true, () -> f.orientation(PageOrientation.LANDSCAPE));
        r.menu("sheet.page.orientation.menu", "Orientação", "page-orientation", LAYOUT, "sheet.page.portrait", "sheet.page.landscape");
        List<String> sizes = new ArrayList<>();
        for (PaperSize p : PaperSize.values()) {
            String id = "sheet.page.size." + p.name().toLowerCase();
            r.add(id, switch (p) { case LETTER -> "Carta"; case LEGAL -> "Ofício"; case EXECUTIVE -> "Executivo"; default -> p.name(); }, "page-size", LAYOUT, true, () -> f.paper(p));
            sizes.add(id);
        }
        r.menu("sheet.page.size.menu", "Tamanho", "page-size", LAYOUT, sizes.toArray(String[]::new));
        r.add("sheet.page.printArea.set", "Definir Área de Impressão", "print-area", LAYOUT, true, f::setPrintArea);
        r.add("sheet.page.printArea.clear", "Limpar Área de Impressão", "print-area", LAYOUT, true, f::clearPrintArea);
        r.menu("sheet.page.printArea.menu", "Área de Impressão", "print-area", LAYOUT, "sheet.page.printArea.set", "sheet.page.printArea.clear");
        r.add("sheet.page.breaks.insert", "Inserir Quebra de Página", "page-break", LAYOUT, true, f::insertBreak);
        r.add("sheet.page.breaks.remove", "Remover Quebra de Página", "page-break", LAYOUT, true, f::removeBreak);
        r.add("sheet.page.breaks.reset", "Redefinir Todas as Quebras de Página", "page-break", LAYOUT, true, f::resetBreaks);
        r.menu("sheet.page.breaks.menu", "Quebras", "page-break", LAYOUT, "sheet.page.breaks.insert", "sheet.page.breaks.remove", "sheet.page.breaks.reset");
        r.add("sheet.page.titles", "Imprimir Títulos", "print-titles", LAYOUT, true, f::printTitles);
        r.toggle("sheet.page.gridlinesPrint", "Imprimir Linhas de Grade", "gridlines", LAYOUT, true, () -> e.activeSheet().properties().print().gridlines(), () -> f.updatePrint("Linhas de grade", p -> p.withGridlines(!p.gridlines())));
        r.toggle("sheet.page.headingsPrint", "Imprimir Títulos de Linha/Coluna", "headers", LAYOUT, true, () -> e.activeSheet().properties().print().headings(), () -> f.updatePrint("Títulos", p -> p.withHeadings(!p.headings())));
        List<String> themes = new ArrayList<>();
        for (SheetTheme t : SheetCommandSupport.THEMES) {
            String id = "sheet.page.theme." + t.name().toLowerCase().replace(' ', '_');
            r.toggle(id, t.name(), "cell-styles", LAYOUT, true, () -> e.getWorkbook().properties().theme().name().equals(t.name()), () -> e.format().setTheme(t));
            themes.add(id);
        }
        r.menu("sheet.page.theme.menu", "Temas", "cell-styles", LAYOUT, themes.toArray(String[]::new));
    }

    private static void formulas(SheetEditor e, SheetCommandRegistry r) {
        r.add("sheet.insertFunction", "Inserir Função", "function", FORMULAS, true, () -> SheetCommandSupport.insertFunction(e, null)).whileEditing();
        Object[][] cats = {{"financial", "Financeira", FunctionCategory.FINANCIAL}, {"logical", "Lógica", FunctionCategory.LOGICAL}, {"text", "Texto", FunctionCategory.TEXT},
                {"date", "Data e Hora", FunctionCategory.DATE_TIME}, {"lookup", "Pesquisa e Referência", FunctionCategory.LOOKUP}, {"math", "Matemática e Trigonometria", FunctionCategory.MATH},
                {"more", "Mais Funções", FunctionCategory.STATISTICAL}};
        for (Object[] c : cats) {
            FunctionCategory cat = (FunctionCategory) c[2];
            r.add("sheet.fn." + c[0], (String) c[1], "function", FORMULAS, true, () -> SheetCommandSupport.insertFunction(e, cat)).whileEditing();
        }
        ReviewController rv = e.review();
        r.add("sheet.names.manager", "Gerenciador de Nomes", "names", FORMULAS, true, rv::nameManager);
        r.add("sheet.names.define", "Definir Nome", "names", FORMULAS, true, rv::defineNameDialog);
        r.add("sheet.names.use", "Usar em Fórmula", "names", FORMULAS, true, rv::useNameMenu).whileEditing();
        r.add("sheet.names.fromSelection", "Criar a partir da Seleção", "names", FORMULAS, true, rv::createNamesFromSelection);
        r.add("sheet.audit.precedents", "Rastrear Precedentes", "trace-precedents", FORMULAS, false, rv::tracePrecedents);
        r.add("sheet.audit.dependents", "Rastrear Dependentes", "trace-dependents", FORMULAS, false, rv::traceDependents);
        r.add("sheet.audit.removeArrows", "Remover Setas", "remove-arrows", FORMULAS, false, rv::removeArrows);
        r.toggle("sheet.view.formulas", "Mostrar Fórmulas", "show-formulas", FORMULAS, false, () -> e.activeSheet().properties().showFormulas(), rv::toggleShowFormulas);
        r.add("sheet.audit.errorCheck", "Verificação de Erros", "error-check", FORMULAS, false, rv::errorCheck);
        r.toggle("sheet.audit.errorIndicators", "Indicadores de Erro", "error-check", FORMULAS, false, rv::errorIndicators, rv::toggleErrorIndicators);
        r.add("sheet.audit.evaluate", "Avaliar Fórmula", "evaluate", FORMULAS, false, rv::evaluateFormula);
        r.add("sheet.audit.watch", "Janela de Inspeção", "watch", FORMULAS, false, rv::watchWindow);
        r.toggle("sheet.calc.auto", "Automático", "calc-options", FORMULAS, false, () -> e.getConfig().calcMode() == CalcMode.AUTOMATIC, () -> e.setConfig(e.getConfig().withCalcMode(CalcMode.AUTOMATIC)));
        r.toggle("sheet.calc.autoExceptTables", "Automático, exceto Tabelas de Dados", "calc-options", FORMULAS, false, () -> e.getConfig().calcMode() == CalcMode.AUTOMATIC_EXCEPT_TABLES,
                () -> e.setConfig(e.getConfig().withCalcMode(CalcMode.AUTOMATIC_EXCEPT_TABLES)));
        r.toggle("sheet.calc.manual", "Manual", "calc-options", FORMULAS, false, () -> e.getConfig().calcMode() == CalcMode.MANUAL, () -> e.setConfig(e.getConfig().withCalcMode(CalcMode.MANUAL)));
        r.toggle("sheet.calc.iterative", "Habilitar Cálculo Iterativo", "calc-options", FORMULAS, false, () -> e.getConfig().iteration().enabled(),
                () -> e.setConfig(e.getConfig().withIteration(e.getConfig().iteration().withEnabled(!e.getConfig().iteration().enabled()))));
        r.menu("sheet.calc.menu", "Opções de Cálculo", "calc-options", FORMULAS, "sheet.calc.auto", "sheet.calc.autoExceptTables", "sheet.calc.manual", "-", "sheet.calc.iterative");
        r.add("sheet.calc.now", "Calcular Agora", "calc-now", FORMULAS, false, e::recalculate);
        r.add("sheet.calc.sheet", "Calcular Planilha", "calc-now", FORMULAS, false, e::recalculate);
        r.add("sheet.calc.full", "Recalcular Tudo", "calc-now", FORMULAS, false, e::recalculateAll);
    }

    private static void data(SheetEditor e, SheetCommandRegistry r) {
        DataController d = e.data();
        r.add("sheet.data.refreshAll", "Atualizar Tudo", "refresh", DATA, true, d::refreshAll);
        r.add("sheet.filterView.create", "Criar Visão de Filtro", "filter", DATA, true, d::createFilterView);
        r.add("sheet.filterView.manage", "Visões de Filtro…", "filter", DATA, true, d::manageFilterViews);
        r.add("sheet.data.textToColumns", "Texto para Colunas", "text-to-columns", DATA, true, d::textToColumns);
        r.add("sheet.data.removeDuplicates", "Remover Duplicatas", "remove-duplicates", DATA, true, () -> d.removeDuplicates(null));
        r.add("sheet.data.validation", "Validação de Dados…", "validation", DATA, true, d::validationDialog);
        r.add("sheet.data.validation.circle", "Circular Dados Inválidos", "validation", DATA, false, () -> e.review().circleInvalid());
        r.add("sheet.data.validation.clearCircles", "Limpar Círculos de Validação", "validation", DATA, false, () -> e.review().removeArrows());
        r.add("sheet.data.validation.clear", "Limpar Validação", "validation", DATA, true, d::clearValidation);
        r.add("sheet.data.validation.list.pick", "Escolher na Lista Suspensa…", "dropdown", DATA, true, d::pickFromList);
        r.menu("sheet.data.validation.menu", "Validação de Dados", "validation", DATA, "sheet.data.validation", "sheet.data.validation.circle", "sheet.data.validation.clearCircles", "sheet.data.validation.clear");
        r.add("sheet.data.consolidate", "Consolidar", "consolidate", DATA, true, d::consolidate);
        r.add("sheet.data.scenarios", "Gerenciador de Cenários…", "what-if", DATA, true, d::scenarios);
        r.add("sheet.data.goalSeek", "Atingir Meta…", "what-if", DATA, true, d::goalSeek);
        r.add("sheet.data.dataTable", "Tabela de Dados…", "what-if", DATA, true, d::dataTable);
        r.menu("sheet.data.whatIf.menu", "Teste de Hipóteses", "what-if", DATA, "sheet.data.scenarios", "sheet.data.goalSeek", "sheet.data.dataTable");
        r.add("sheet.data.forecast", "Planilha de Previsão", "forecast", DATA, true, d::forecast);
        r.add("sheet.data.group", "Agrupar", "group", DATA, true, () -> e.structure().group(true, 1));
        r.add("sheet.data.ungroup", "Desagrupar", "ungroup", DATA, true, () -> e.structure().group(true, -1));
        r.add("sheet.data.subtotal", "Subtotal", "subtotal", DATA, true, d::subtotal);
        r.add("sheet.data.showDetail", "Mostrar Detalhe", "expand", DATA, true, () -> e.structure().setCollapsed(false));
        r.add("sheet.data.hideDetail", "Ocultar Detalhe", "collapse", DATA, true, () -> e.structure().setCollapsed(true));
    }

    private static void review(SheetEditor e, SheetCommandRegistry r) {
        ReviewController rv = e.review();
        r.add("sheet.review.spelling", "Verificar Ortografia", "spelling", REVIEW, false, rv::spelling);
        r.add("sheet.review.statistics", "Estatísticas da Pasta de Trabalho", "names", REVIEW, false, rv::statistics);
        r.add("sheet.comment.new", "Novo Comentário", "comment", REVIEW, true, rv::newComment);
        r.add("sheet.comment.resolve", "Resolver Conversa", "check", REVIEW, true, rv::resolveComment);
        r.add("sheet.comment.delete", "Excluir", "delete-cells", REVIEW, true, rv::deleteNotes);
        r.add("sheet.comment.previous", "Anterior", "outdent", REVIEW, false, () -> rv.nextAnnotation(true, -1));
        r.add("sheet.comment.next", "Próximo", "indent", REVIEW, false, () -> rv.nextAnnotation(true, 1));
        r.add("sheet.comment.showAll", "Mostrar Comentários", "comment", REVIEW, false, rv::showAllComments);
        r.add("sheet.note.new", "Nova Anotação", "note", REVIEW, true, rv::editNote);
        r.toggle("sheet.note.showAll", "Mostrar Todas as Anotações", "note", REVIEW, true, rv::allNotesVisible, rv::toggleAllNotes);
        r.toggle("sheet.protect.sheet", "Proteger Planilha", "protect", REVIEW, false, rv::isSheetProtected, rv::protectSheet);
        r.toggle("sheet.protect.workbook", "Proteger Pasta de Trabalho", "protect", REVIEW, false, rv::isWorkbookProtected, rv::protectWorkbook);
        r.add("sheet.protect.ranges", "Permitir Edição de Intervalos", "protect", REVIEW, true, rv::allowEditRanges);
        r.add("sheet.ai.assistant", "Assistente", "ai", REVIEW, false, rv::aiAssistant);
    }

    private static void view(SheetEditor e, SheetCommandRegistry r) {
        r.toggle("sheet.view.normal", "Normal", "normal-view", VIEW, false, () -> e.activeSheet().properties().viewMode() == SheetViewMode.NORMAL, () -> e.setViewMode(SheetViewMode.NORMAL));
        r.toggle("sheet.view.pageBreak", "Visualização da Quebra de Página", "page-break", VIEW, false, () -> e.activeSheet().properties().viewMode() == SheetViewMode.PAGE_BREAK_PREVIEW, () -> e.setViewMode(SheetViewMode.PAGE_BREAK_PREVIEW));
        r.toggle("sheet.view.pageLayout", "Layout da Página", "page-layout", VIEW, false, () -> e.activeSheet().properties().viewMode() == SheetViewMode.PAGE_LAYOUT, () -> e.setViewMode(SheetViewMode.PAGE_LAYOUT));
        r.toggle("sheet.view.gridlines", "Linhas de Grade", "gridlines", VIEW, false, () -> e.activeSheet().properties().showGridlines(), () -> viewProperty(e, p -> p.withShowGridlines(!p.showGridlines())));
        r.toggle("sheet.view.headings", "Títulos", "headers", VIEW, false, () -> e.activeSheet().properties().showHeaders(), () -> viewProperty(e, p -> p.withShowHeaders(!p.showHeaders())));
        r.toggle("sheet.view.zeros", "Mostrar Zeros", "number-format", VIEW, false, () -> e.activeSheet().properties().showZeros(), () -> viewProperty(e, p -> p.withShowZeros(!p.showZeros())));
        r.toggle("sheet.view.formulaBar", "Barra de Fórmulas", "function", VIEW, false, () -> e.getConfig().formulaBarVisible(), () -> e.setConfig(e.getConfig().withFormulaBarVisible(!e.getConfig().formulaBarVisible())));
        r.toggle("sheet.view.ribbon", "Mostrar Faixa de Opções", "more", VIEW, false, () -> e.getConfig().ribbonVisible(), () -> e.setConfig(e.getConfig().withRibbonVisible(!e.getConfig().ribbonVisible())));
        r.add("sheet.view.zoomDialog", "Zoom", "zoom", VIEW, false, () -> SheetCommandSupport.zoomDialog(e));
        r.add("sheet.view.zoom100", "100%", "zoom-100", VIEW, false, () -> e.setZoom(1));
        r.add("sheet.view.zoomSelection", "Zoom na Seleção", "zoom-selection", VIEW, false, () -> SheetCommandSupport.zoomToSelection(e));
        r.add("sheet.view.zoomIn", "Ampliar", "zoom", VIEW, false, () -> e.setZoom(e.effectiveZoom() + 0.1));
        r.add("sheet.view.zoomOut", "Reduzir", "zoom", VIEW, false, () -> e.setZoom(e.effectiveZoom() - 0.1));
        r.add("sheet.freeze.panes", "Congelar Painéis", "freeze", VIEW, false, () -> e.structure().freezeAtSelection());
        r.add("sheet.freeze.topRow", "Congelar Linha Superior", "freeze", VIEW, false, () -> e.structure().freeze(1, 0));
        r.add("sheet.freeze.firstColumn", "Congelar Primeira Coluna", "freeze", VIEW, false, () -> e.structure().freeze(0, 1));
        r.add("sheet.freeze.none", "Descongelar Painéis", "freeze", VIEW, false, () -> e.structure().freeze(0, 0));
        r.menu("sheet.freeze.menu", "Congelar Painéis", "freeze", VIEW, "sheet.freeze.panes", "sheet.freeze.topRow", "sheet.freeze.firstColumn", "sheet.freeze.none");
    }

    private static void viewProperty(SheetEditor e, UnaryOperator<dtm.stools.component.panels.editor.sheet.model.SheetProperties> change) {
        SheetWorksheet ws = e.activeSheet();
        ws.setProperties(change.apply(ws.properties()));
        e.getCanvas().invalidateGeometry();
        e.getSession().markDirty();
        e.refreshAll();
    }

    private static void table(SheetEditor e, SheetCommandRegistry r) {
        DataController d = e.data();
        r.add("sheet.table.rename", "Nome da Tabela", "table", TABLE, true, d::renameTable);
        r.add("sheet.table.resize", "Redimensionar Tabela", "table", TABLE, true, d::resizeTable);
        r.add("sheet.table.removeDuplicates", "Remover Duplicatas", "remove-duplicates", TABLE, true, () -> { SheetTable t = d.activeTable(); if (t != null) d.removeDuplicates(t.dataRange().union(t.range().resize(1, t.range().columnCount()))); });
        r.add("sheet.table.convertToRange", "Converter em Intervalo", "table", TABLE, true, d::convertToRange);
        tableOption(r, d, "sheet.table.headerRow", "Linha de Cabeçalho", SheetTable::headerRow, t -> t.withHeaderRow(!t.headerRow()));
        r.toggle("sheet.table.totalRow", "Linha de Totais", "subtotal", TABLE, true, () -> d.activeTable() != null && d.activeTable().totalsRow(), d::toggleTotals);
        tableOption(r, d, "sheet.table.bandedRows", "Linhas em Tiras", SheetTable::bandedRows, t -> t.withBandedRows(!t.bandedRows()));
        tableOption(r, d, "sheet.table.firstColumn", "Primeira Coluna", SheetTable::firstColumn, t -> t.withFirstColumn(!t.firstColumn()));
        tableOption(r, d, "sheet.table.lastColumn", "Última Coluna", SheetTable::lastColumn, t -> t.withLastColumn(!t.lastColumn()));
        tableOption(r, d, "sheet.table.bandedColumns", "Colunas em Tiras", SheetTable::bandedColumns, t -> t.withBandedColumns(!t.bandedColumns()));
        tableOption(r, d, "sheet.table.filterButton", "Botão de Filtro", SheetTable::filterButton, t -> t.withFilterButton(!t.filterButton()));
    }

    private static void tableOption(SheetCommandRegistry r, DataController d, String id, String name, java.util.function.Predicate<SheetTable> state, UnaryOperator<SheetTable> change) {
        r.toggle(id, name, "table", TABLE, true, () -> d.activeTable() != null && state.test(d.activeTable()), () -> d.updateActiveTable(name, change));
    }

    private static void chart(SheetEditor e, SheetCommandRegistry r) {
        ObjectController o = e.objects();
        List<String> types = new ArrayList<>();
        for (ChartType t : ChartType.values()) {
            String id = "sheet.chart.type." + t.name().toLowerCase();
            r.toggle(id, t.label(), "chart-other", CHART, true, () -> o.selectedObject() instanceof SheetChart c && c.type() == t, () -> o.updateChart("Alterar tipo de gráfico", c -> c.withType(t)));
            types.add(id);
        }
        r.menu("sheet.chart.changeType.menu", "Alterar Tipo de Gráfico", "chart-combo", CHART, types.toArray(String[]::new));
        r.add("sheet.chart.editData", "Selecionar Dados", "data", CHART, true, o::editChart);
        r.add("sheet.chart.title", "Título do Gráfico", "textbox", CHART, true, o::chartTitle);
        List<String> legends = new ArrayList<>();
        for (LegendPosition p : LegendPosition.values()) {
            String id = "sheet.chart.legend." + p.name().toLowerCase();
            String label = switch (p) { case NONE -> "Nenhuma"; case RIGHT -> "Direita"; case BOTTOM -> "Inferior"; case TOP -> "Superior"; case LEFT -> "Esquerda"; };
            r.toggle(id, label, "chart", CHART, true, () -> o.selectedObject() instanceof SheetChart c && c.legend() == p, () -> o.updateChart("Legenda", c -> c.withLegend(p)));
            legends.add(id);
        }
        r.menu("sheet.chart.legend.menu", "Legenda", "chart", CHART, legends.toArray(String[]::new));
        r.toggle("sheet.chart.dataLabels", "Rótulos de Dados", "chart", CHART, true, () -> o.selectedObject() instanceof SheetChart c && c.dataLabels(), () -> o.updateChart("Rótulos de dados", c -> c.withDataLabels(!c.dataLabels())));
        r.toggle("sheet.chart.gridlines", "Linhas de Grade", "gridlines", CHART, true, () -> o.selectedObject() instanceof SheetChart c && c.gridlines(), () -> o.updateChart("Linhas de grade", c -> c.withGridlines(!c.gridlines())));
        r.add("sheet.object.bringFront", "Trazer para a Frente", "shapes", CHART, true, () -> o.order(true));
        r.add("sheet.object.sendBack", "Enviar para Trás", "shapes", CHART, true, () -> o.order(false));
        r.add("sheet.object.delete", "Excluir Objeto", "delete-cells", CHART, true, o::deleteSelected);
        r.add("sheet.object.altText", "Texto Alternativo", "textbox", CHART, true, o::altText);
    }

    private static void pivot(SheetEditor e, SheetCommandRegistry r) {
        ObjectController o = e.objects();
        r.add("sheet.pivot.refresh", "Atualizar", "refresh", PIVOT, true, o::refreshPivot);
        r.add("sheet.pivot.fields", "Lista de Campos", "pivot", PIVOT, true, o::editPivot);
        r.add("sheet.pivot.options", "Opções", "pivot", PIVOT, true, o::editPivot);
        r.add("sheet.pivot.delete", "Excluir Tabela Dinâmica", "delete-cells", PIVOT, true, o::deletePivot);
    }

    private static void sheets(SheetEditor e, SheetCommandRegistry r) {
        StructureController s = e.structure();
        r.add("sheet.sheet.insert", "Inserir Planilha", "sheet-add", SHEET, true, s::addSheet);
        r.add("sheet.sheet.delete", "Excluir Planilha", "delete-cells", SHEET, true, s::deleteSheet);
        r.add("sheet.sheet.rename", "Renomear Planilha", "sheet", SHEET, true, s::promptRename);
        r.add("sheet.sheet.duplicate", "Mover ou Copiar (Duplicar)", "copy", SHEET, true, s::duplicateSheet);
        r.add("sheet.sheet.hide", "Ocultar Planilha", "hide", SHEET, true, s::hideSheet);
        r.add("sheet.sheet.unhide", "Reexibir Planilha…", "hide", SHEET, true, s::unhideSheet);
        String[][] colors = {{"none", "Sem Cor", null}, {"red", "Vermelho", "FFC00000"}, {"orange", "Laranja", "FFFFC000"}, {"yellow", "Amarelo", "FFFFFF00"}, {"green", "Verde", "FF00B050"},
                {"blue", "Azul", "FF0070C0"}, {"purple", "Roxo", "FF7030A0"}, {"gray", "Cinza", "FF808080"}};
        List<String> ids = new ArrayList<>();
        for (String[] c : colors) {
            Integer argb = c[2] == null ? null : (int) Long.parseLong(c[2], 16);
            r.add("sheet.sheet.tabColor." + c[0], c[1], "fill", SHEET, true, () -> s.tabColor(argb));
            ids.add("sheet.sheet.tabColor." + c[0]);
        }
        r.add("sheet.sheet.tabColor.custom", "Mais Cores…", "fill", SHEET, true, () -> {
            Color chosen = JColorChooser.showDialog(e, "Cor da Guia", Color.WHITE);
            if (chosen != null) s.tabColor(chosen.getRGB() | 0xFF000000);
        });
        ids.add("-");
        ids.add("sheet.sheet.tabColor.custom");
        r.menu("sheet.sheet.tabColor.menu", "Cor da Guia", "fill", SHEET, ids.toArray(String[]::new));
        r.add("sheet.sheet.next", "Próxima Planilha", null, NAV, false, () -> e.navigation().nextSheet(1)).putValue("sheet.hidden", Boolean.TRUE);
        r.add("sheet.sheet.previous", "Planilha Anterior", null, NAV, false, () -> e.navigation().nextSheet(-1)).putValue("sheet.hidden", Boolean.TRUE);
    }

    private static void nav(SheetCommandRegistry r, String id, String name, Runnable run) {
        r.add(id, name, null, NAV, false, run).putValue("sheet.hidden", Boolean.TRUE);
    }

    private static void navigation(SheetEditor e, SheetCommandRegistry r) {
        NavigationController n = e.navigation();
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        String[] names = {"up", "down", "left", "right"};
        for (int i = 0; i < 4; i++) {
            int dr = dirs[i][0], dc = dirs[i][1];
            nav(r, "sheet.nav." + names[i], "Mover", () -> n.move(dr, dc, false));
            nav(r, "sheet.nav.extend." + names[i], "Estender", () -> n.move(dr, dc, true));
            nav(r, "sheet.nav.jump." + names[i], "Saltar", () -> n.jump(dr, dc, false));
            nav(r, "sheet.nav.jumpExtend." + names[i], "Saltar e estender", () -> n.jump(dr, dc, true));
        }
        nav(r, "sheet.nav.home", "Início da linha", () -> n.home(false));
        nav(r, "sheet.nav.extend.home", "Estender até o início", () -> n.home(true));
        nav(r, "sheet.nav.start", "Início da planilha", () -> n.documentStart(false));
        nav(r, "sheet.nav.extend.start", "Estender até o início da planilha", () -> n.documentStart(true));
        nav(r, "sheet.nav.end", "Última célula", () -> n.documentEnd(false));
        nav(r, "sheet.nav.extend.end", "Estender até a última célula", () -> n.documentEnd(true));
        nav(r, "sheet.nav.pageUp", "Página acima", () -> n.page(-1, false, false));
        nav(r, "sheet.nav.pageDown", "Página abaixo", () -> n.page(1, false, false));
        nav(r, "sheet.nav.extend.pageUp", "Estender página acima", () -> n.page(-1, false, true));
        nav(r, "sheet.nav.extend.pageDown", "Estender página abaixo", () -> n.page(1, false, true));
        nav(r, "sheet.nav.pageLeft", "Página à esquerda", () -> n.page(-1, true, false));
        nav(r, "sheet.nav.pageRight", "Página à direita", () -> n.page(1, true, false));
        nav(r, "sheet.nav.enter", "Próxima célula", () -> n.moveWithinSelection(e.getConfig().enterMovesDown() ? 1 : 0, e.getConfig().enterMovesDown() ? 0 : 1));
        nav(r, "sheet.nav.shiftEnter", "Célula anterior", () -> n.moveWithinSelection(e.getConfig().enterMovesDown() ? -1 : 0, e.getConfig().enterMovesDown() ? 0 : -1));
        nav(r, "sheet.nav.tab", "Próxima coluna", () -> n.moveWithinSelection(0, 1));
        nav(r, "sheet.nav.shiftTab", "Coluna anterior", () -> n.moveWithinSelection(0, -1));
        r.add("sheet.select.all", "Selecionar Tudo", null, NAV, false, n::selectAll);
        r.add("sheet.select.columns", "Selecionar Coluna Inteira", null, NAV, false, n::selectColumns);
        r.add("sheet.select.rows", "Selecionar Linha Inteira", null, NAV, false, n::selectRows);
        r.add("sheet.edit", "Editar Célula", null, NAV, true, () -> e.startEditing(null, false));
        r.add("sheet.delete", "Excluir Conteúdo", null, NAV, true, () -> SheetCommandSupport.delete(e));
        r.add("sheet.backspace", "Limpar e Editar", null, NAV, true, () -> SheetCommandSupport.clearAndEdit(e)).putValue("sheet.hidden", Boolean.TRUE);
        r.add("sheet.escape", "Cancelar", null, NAV, false, () -> SheetCommandSupport.escape(e)).putValue("sheet.hidden", Boolean.TRUE);
        r.add("sheet.insert.date", "Inserir Data Atual", "calc-now", NAV, true, () -> SheetCommandSupport.insertNow(e, false));
        r.add("sheet.insert.time", "Inserir Hora Atual", "calc-now", NAV, true, () -> SheetCommandSupport.insertNow(e, true));
        r.add("sheet.formulaBar.expand", "Expandir Barra de Fórmulas", "expand", VIEW, false, () -> e.getFormulaBar().toggleExpanded());
        r.add("sheet.chart.quick", "Gráfico Rápido", "chart", INSERT, true, () -> e.objects().insertChart(ChartType.COLUMN));
        r.add("sheet.number.dateQuick", "Formato de Data", "number-format", HOME, true, () -> e.format().applyNumberFormat("dd/mm/yyyy"));
        r.add("sheet.number.timeQuick", "Formato de Hora", "number-format", HOME, true, () -> e.format().applyNumberFormat("hh:mm"));
    }

    private static void bindings(SheetEditor e, SheetCommandRegistry r) {
        int[] arrows = {KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT};
        String[] names = {"up", "down", "left", "right"};
        for (int i = 0; i < 4; i++) {
            r.bind("sheet.nav." + names[i], plain(arrows[i]));
            r.bind("sheet.nav.extend." + names[i], shift(arrows[i]));
            r.bind("sheet.nav.jump." + names[i], ctrl(arrows[i]));
            r.bind("sheet.nav.jumpExtend." + names[i], ctrlShift(arrows[i]));
        }
        r.bind("sheet.nav.home", plain(KeyEvent.VK_HOME));
        r.bind("sheet.nav.extend.home", shift(KeyEvent.VK_HOME));
        r.bind("sheet.nav.start", ctrl(KeyEvent.VK_HOME));
        r.bind("sheet.nav.extend.start", ctrlShift(KeyEvent.VK_HOME));
        r.bind("sheet.nav.end", ctrl(KeyEvent.VK_END));
        r.bind("sheet.nav.extend.end", ctrlShift(KeyEvent.VK_END));
        r.bind("sheet.nav.pageUp", plain(KeyEvent.VK_PAGE_UP));
        r.bind("sheet.nav.pageDown", plain(KeyEvent.VK_PAGE_DOWN));
        r.bind("sheet.nav.extend.pageUp", shift(KeyEvent.VK_PAGE_UP));
        r.bind("sheet.nav.extend.pageDown", shift(KeyEvent.VK_PAGE_DOWN));
        r.bind("sheet.nav.pageLeft", alt(KeyEvent.VK_PAGE_UP));
        r.bind("sheet.nav.pageRight", alt(KeyEvent.VK_PAGE_DOWN));
        r.bind("sheet.nav.enter", plain(KeyEvent.VK_ENTER));
        r.bind("sheet.nav.shiftEnter", shift(KeyEvent.VK_ENTER));
        r.bind("sheet.nav.tab", plain(KeyEvent.VK_TAB));
        r.bind("sheet.nav.shiftTab", shift(KeyEvent.VK_TAB));
        r.bind("sheet.sheet.next", ctrl(KeyEvent.VK_PAGE_DOWN));
        r.bind("sheet.sheet.previous", ctrl(KeyEvent.VK_PAGE_UP));
        r.bind("sheet.select.all", ctrl(KeyEvent.VK_A));
        r.bind("sheet.select.columns", ctrl(KeyEvent.VK_SPACE));
        r.bind("sheet.select.rows", shift(KeyEvent.VK_SPACE));
        r.bind("sheet.edit", plain(KeyEvent.VK_F2));
        r.bind("sheet.delete", plain(KeyEvent.VK_DELETE));
        r.bind("sheet.backspace", plain(KeyEvent.VK_BACK_SPACE));
        r.bind("sheet.escape", plain(KeyEvent.VK_ESCAPE));
        r.bind("sheet.copy", ctrl(KeyEvent.VK_C), ctrl(KeyEvent.VK_INSERT));
        r.bind("sheet.cut", ctrl(KeyEvent.VK_X), shift(KeyEvent.VK_DELETE));
        r.bind("sheet.paste", ctrl(KeyEvent.VK_V), shift(KeyEvent.VK_INSERT));
        r.bind("sheet.pasteSpecial", key(KeyEvent.VK_V, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK));
        r.bind("sheet.paste.values", ctrlShift(KeyEvent.VK_V));
        r.bind("sheet.undo", ctrl(KeyEvent.VK_Z));
        r.bind("sheet.redo", ctrl(KeyEvent.VK_Y));
        r.bind("sheet.font.bold", ctrl(KeyEvent.VK_B), ctrl(KeyEvent.VK_N), ctrl(KeyEvent.VK_2));
        r.bind("sheet.font.italic", ctrl(KeyEvent.VK_I), ctrl(KeyEvent.VK_3));
        r.bind("sheet.font.underline", ctrl(KeyEvent.VK_U), ctrl(KeyEvent.VK_4));
        r.bind("sheet.font.strike", ctrl(KeyEvent.VK_5));
        r.bind("sheet.format.cells", ctrl(KeyEvent.VK_1));
        r.bind("sheet.format.font", ctrlShift(KeyEvent.VK_F));
        r.bind("sheet.find", ctrl(KeyEvent.VK_F), shift(KeyEvent.VK_F5));
        r.bind("sheet.replace", ctrl(KeyEvent.VK_H));
        r.bind("sheet.goTo", ctrl(KeyEvent.VK_G), plain(KeyEvent.VK_F5));
        r.bind("sheet.insert.date", ctrl(KeyEvent.VK_SEMICOLON));
        r.bind("sheet.insert.time", ctrlShift(KeyEvent.VK_SEMICOLON));
        r.bind("sheet.fill.down", ctrl(KeyEvent.VK_D));
        r.bind("sheet.fill.right", ctrl(KeyEvent.VK_R));
        r.bind("sheet.fill.flash", ctrl(KeyEvent.VK_E));
        r.bind("sheet.table.create", ctrl(KeyEvent.VK_T));
        r.bind("sheet.link.insert", ctrl(KeyEvent.VK_K));
        r.bind("sheet.filter.toggle", ctrlShift(KeyEvent.VK_L));
        r.bind("sheet.view.formulas", ctrl(KeyEvent.VK_BACK_QUOTE), ctrl(KeyEvent.VK_DEAD_GRAVE));
        r.bind("sheet.calc.now", plain(KeyEvent.VK_F9));
        r.bind("sheet.calc.sheet", shift(KeyEvent.VK_F9));
        r.bind("sheet.calc.full", key(KeyEvent.VK_F9, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK));
        r.bind("sheet.insertFunction", shift(KeyEvent.VK_F3));
        r.bind("sheet.names.manager", ctrl(KeyEvent.VK_F3));
        r.bind("sheet.commandPalette", ctrlShift(KeyEvent.VK_P));
        r.bind("sheet.file.save", ctrl(KeyEvent.VK_S));
        r.bind("sheet.file.saveAs", plain(KeyEvent.VK_F12));
        r.bind("sheet.file.open", ctrl(KeyEvent.VK_O));
        r.bind("sheet.file.new", key(KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK));
        r.bind("sheet.file.print", ctrl(KeyEvent.VK_P));
        r.bind("sheet.insert.cellsDialog", ctrl(KeyEvent.VK_ADD), ctrlShift(KeyEvent.VK_EQUALS), ctrlShift(KeyEvent.VK_PLUS));
        r.bind("sheet.delete.cellsDialog", ctrl(KeyEvent.VK_SUBTRACT), ctrl(KeyEvent.VK_MINUS));
        r.bind("sheet.row.hide", ctrl(KeyEvent.VK_9));
        r.bind("sheet.row.unhide", ctrlShift(KeyEvent.VK_9));
        r.bind("sheet.column.hide", ctrl(KeyEvent.VK_0));
        r.bind("sheet.column.unhide", ctrlShift(KeyEvent.VK_0));
        r.bind("sheet.autosum.sum", alt(KeyEvent.VK_EQUALS));
        r.bind("sheet.note.new", shift(KeyEvent.VK_F2));
        r.bind("sheet.comment.new", ctrlShift(KeyEvent.VK_F2));
        r.bind("sheet.number.currencySimple", ctrlShift(KeyEvent.VK_4));
        r.bind("sheet.number.percent", ctrlShift(KeyEvent.VK_5));
        r.bind("sheet.number.dateQuick", ctrlShift(KeyEvent.VK_3));
        r.bind("sheet.number.timeQuick", ctrlShift(KeyEvent.VK_2));
        r.bind("sheet.number.comma", ctrlShift(KeyEvent.VK_1));
        r.bind("sheet.number.general", ctrlShift(KeyEvent.VK_BACK_QUOTE));
        r.bind("sheet.border.outside", ctrlShift(KeyEvent.VK_7));
        r.bind("sheet.border.none", ctrlShift(KeyEvent.VK_MINUS));
        r.bind("sheet.formulaBar.expand", ctrlShift(KeyEvent.VK_U));
        r.bind("sheet.chart.quick", alt(KeyEvent.VK_F1), plain(KeyEvent.VK_F11));
        r.bind("sheet.data.validation.list.pick", alt(KeyEvent.VK_DOWN));
        r.bind("sheet.audit.precedents", ctrl(KeyEvent.VK_OPEN_BRACKET));
        r.bind("sheet.audit.dependents", ctrl(KeyEvent.VK_CLOSE_BRACKET));
        r.bind("sheet.review.spelling", plain(KeyEvent.VK_F7));
        r.bind("sheet.sheet.insert", shift(KeyEvent.VK_F11));
        r.bind("sheet.view.ribbon", ctrl(KeyEvent.VK_F1));
        r.bind("sheet.view.zoomIn", key(KeyEvent.VK_EQUALS, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK));
        r.bind("sheet.view.zoomOut", key(KeyEvent.VK_MINUS, java.awt.event.InputEvent.CTRL_DOWN_MASK | java.awt.event.InputEvent.ALT_DOWN_MASK));
    }
}
