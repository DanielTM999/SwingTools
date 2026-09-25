package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.command.SheetTransaction;
import dtm.stools.component.panels.editor.sheet.model.BorderStyle;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.ConditionalFormat;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRule;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.SheetBorder;
import dtm.stools.component.panels.editor.sheet.model.SheetFill;
import dtm.stools.component.panels.editor.sheet.model.SheetTheme;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.UnderlineStyle;
import dtm.stools.component.panels.editor.sheet.model.VerticalAlignment;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public final class FormatController {
    public enum Border { BOTTOM, TOP, LEFT, RIGHT, NONE, ALL, OUTSIDE, THICK_OUTSIDE, DOUBLE_BOTTOM, THICK_BOTTOM, TOP_BOTTOM, TOP_THICK_BOTTOM, TOP_DOUBLE_BOTTOM, INSIDE }

    private static final long MAX_CELLS = 4_000_000;
    private final SheetEditor editor;
    private Map<CellAddress, Integer> painter;
    private CellRange painterSource;
    private boolean painterSticky, painterListener;

    public FormatController(SheetEditor editor) { this.editor = editor; }

    public CellStyle activeStyle() {
        CellAddress a = editor.getSelection().active();
        return editor.getWorkbook().style(editor.activeSheet().cell(a).style());
    }

    public boolean applyStyle(int sheet, List<CellRange> ranges, UnaryOperator<CellStyle> change, String label) {
        for (CellRange r : ranges) if (!editor.review().canFormat(sheet, r)) { editor.review().warnProtected(); return false; }
        boolean grows = change.apply(CellStyle.DEFAULT).fontSize() > CellStyle.DEFAULT.fontSize() || change.apply(CellStyle.DEFAULT).wrap();
        return editor.edit(label, tx -> {
            applyStyle(tx, sheet, ranges, change);
            if (grows && sheet == editor.activeSheetIndex()) adjustHeights(tx, sheet, ranges);
        });
    }

    private void adjustHeights(SheetTransaction tx, int sheet, List<CellRange> ranges) {
        List<int[]> heights = new ArrayList<>();
        for (CellRange r0 : ranges) {
            CellRange r = editor.clipboard().bounded(editor.activeSheet(), r0);
            if (r.rowCount() > 2000) continue;
            for (int row = r.firstRow(); row <= r.lastRow(); row++) {
                if (editor.activeSheet().rows().hasCustomSize(row)) continue;
                int h = editor.structure().measureRow(row);
                if (h > editor.activeSheet().rows().size(row)) heights.add(new int[]{row, h});
            }
        }
        if (!heights.isEmpty()) tx.updateAxis(sheet, true, axis -> { for (int[] h : heights) axis.setSize(h[0], h[1]); });
    }

    public void applyStyle(SheetTransaction tx, int sheet, List<CellRange> ranges, UnaryOperator<CellStyle> change) {
        SheetWorksheet ws = tx.sheet(sheet);
        for (CellRange r : ranges) {
            if (r.isWholeColumn() || r.isWholeRow()) {
                boolean rows = r.isWholeRow() && !r.isWholeColumn();
                int from = rows ? r.firstRow() : r.firstColumn(), to = rows ? r.lastRow() : r.lastColumn();
                if (!(r.isWholeRow() && r.isWholeColumn())) tx.updateAxis(sheet, rows, axis -> { for (int i = from; i <= to; i++) axis.setStyle(i, editor.getWorkbook().styles().derive(axis.style(i), change)); });
                List<CellAddress> existing = ws.cells().addresses(r);
                for (CellAddress a : existing) tx.setStyle(sheet, a.row(), a.column(), change);
                continue;
            }
            if (r.cellCount() > MAX_CELLS) throw new IllegalArgumentException("A seleção é grande demais para formatar.");
            for (int row = r.firstRow(); row <= r.lastRow(); row++)
                for (int col = r.firstColumn(); col <= r.lastColumn(); col++) tx.setStyle(sheet, row, col, change);
        }
    }

    public boolean apply(String label, UnaryOperator<CellStyle> change) {
        return applyStyle(editor.activeSheetIndex(), editor.getSelection().ranges(), change, label);
    }

    public void toggle(String label, Predicate<CellStyle> current, Function<Boolean, UnaryOperator<CellStyle>> set) {
        boolean next = !current.test(activeStyle());
        apply(label, set.apply(next));
    }

    public void bold() { toggle("Negrito", CellStyle::bold, v -> s -> s.withBold(v)); }
    public void italic() { toggle("Itálico", CellStyle::italic, v -> s -> s.withItalic(v)); }
    public void underline() { toggle("Sublinhado", s -> s.underline() != UnderlineStyle.NONE, v -> s -> s.withUnderline(v ? UnderlineStyle.SINGLE : UnderlineStyle.NONE)); }
    public void doubleUnderline() { toggle("Sublinhado duplo", s -> s.underline() == UnderlineStyle.DOUBLE, v -> s -> s.withUnderline(v ? UnderlineStyle.DOUBLE : UnderlineStyle.NONE)); }
    public void strike() { toggle("Tachado", CellStyle::strikethrough, v -> s -> s.withStrikethrough(v)); }
    public void wrap() { toggle("Quebrar texto", CellStyle::wrap, v -> s -> s.withWrap(v)); }

    public void applyFontFamily(String family) { if (family != null && !family.isBlank()) apply("Fonte", s -> s.withFontFamily(family)); }
    public void applyFontSize(double size) { if (size >= 1 && size <= 409) apply("Tamanho da fonte", s -> s.withFontSize(size)); }

    public void growFont(boolean grow) {
        double[] sizes = {8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24, 26, 28, 36, 48, 72};
        double cur = activeStyle().fontSize();
        double next = cur;
        if (grow) { for (double s : sizes) if (s > cur) { next = s; break; } if (next == cur) next = cur + 10; }
        else { for (int i = sizes.length - 1; i >= 0; i--) if (sizes[i] < cur) { next = sizes[i]; break; } if (next == cur) next = Math.max(1, cur - 1); }
        applyFontSize(next);
    }

    public void applyNumberFormat(String code) { if (code != null) apply("Formato de número", s -> s.withNumberFormat(code)); }
    public void applyFill(Integer argb) { apply("Cor de preenchimento", s -> s.withFill(argb == null ? SheetFill.NONE : SheetFill.solid(argb))); }
    public void applyFontColor(Integer argb) { apply("Cor da fonte", s -> s.withFontColor(argb)); }
    public void horizontal(HorizontalAlignment h) { HorizontalAlignment cur = activeStyle().horizontal(); apply("Alinhamento", s -> s.withHorizontal(cur == h ? HorizontalAlignment.GENERAL : h)); }
    public void vertical(VerticalAlignment v) { apply("Alinhamento", s -> s.withVertical(v)); }
    public void indent(int delta) { apply("Recuo", s -> s.withIndent(Math.max(0, Math.min(250, s.indent() + delta))).withHorizontal(s.horizontal() == HorizontalAlignment.GENERAL || s.horizontal() == HorizontalAlignment.CENTER ? HorizontalAlignment.LEFT : s.horizontal())); }
    public void rotation(int degrees) { int cur = activeStyle().rotation(); apply("Orientação", s -> s.withRotation(cur == degrees ? 0 : degrees)); }

    public String currencyFormat() {
        String lang = editor.getConfig().locale().getLanguage();
        if (lang.equals("pt")) return "\"R$\" #,##0.00;[Red]-\"R$\" #,##0.00";
        if (lang.equals("en")) return "\"$\"#,##0.00;[Red]-\"$\"#,##0.00";
        return "#,##0.00 \"€\";[Red]-#,##0.00 \"€\"";
    }

    public String accountingFormat() {
        String sym = editor.getConfig().locale().getLanguage().equals("pt") ? "R$" : "$";
        return "_-\"" + sym + "\" * #,##0.00_-;-\"" + sym + "\" * #,##0.00_-;_-\"" + sym + "\" * \"-\"??_-;_-@_-";
    }

    public void currency() { applyNumberFormat(currencyFormat()); }
    public void accounting() { applyNumberFormat(accountingFormat()); }
    public void percent() { applyNumberFormat("0%"); }
    public void comma() { applyNumberFormat("#,##0.00"); }

    public void decimals(int delta) {
        String code = activeStyle().numberFormat();
        if (code == null || code.equals("General")) {
            double v = editor.getEngine().valueAt(editor.activeSheetIndex(), editor.getSelection().active()) instanceof dtm.stools.component.panels.editor.sheet.model.NumberValue n ? n.value() : 0;
            String g = dtm.stools.component.panels.editor.sheet.model.NumberValue.general(v);
            int places = g.contains(".") && !g.contains("E") ? g.length() - g.indexOf('.') - 1 : 0;
            code = places == 0 ? "0" : "0." + "0".repeat(places);
        }
        applyNumberFormat(adjustDecimals(code, delta));
    }

    public static String adjustDecimals(String code, int delta) {
        String[] sections = code.split(";", -1);
        for (int i = 0; i < sections.length; i++) sections[i] = adjustSection(sections[i], delta);
        return String.join(";", sections);
    }

    private static String adjustSection(String s, int delta) {
        boolean quoted = false;
        int lastZero = -1, dot = -1;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') quoted = !quoted;
            if (quoted) continue;
            if (c == '\\' || c == '_' || c == '*') { i++; continue; }
            if (c == '[') { int end = s.indexOf(']', i); if (end > 0) { i = end; continue; } }
            if (c == '.' && dot < 0) dot = i;
            if (c == '0' || c == '#' || c == '?') lastZero = i;
        }
        if (lastZero < 0) return s;
        if (delta > 0) {
            if (dot < 0) return s.substring(0, lastZero + 1) + "." + "0".repeat(delta) + s.substring(lastZero + 1);
            return s.substring(0, lastZero + 1) + "0".repeat(delta) + s.substring(lastZero + 1);
        }
        if (dot < 0 || lastZero < dot) return s;
        int remove = Math.min(-delta, lastZero - dot);
        String before = s.substring(0, lastZero + 1 - remove), after = s.substring(lastZero + 1);
        if (before.endsWith(".")) before = before.substring(0, before.length() - 1);
        return before + after;
    }

    public void border(Border kind) {
        SheetSelection sel = editor.getSelection();
        int sheet = editor.activeSheetIndex();
        SheetBorder thin = SheetBorder.of(BorderStyle.THIN), thick = SheetBorder.of(BorderStyle.THICK), dbl = SheetBorder.of(BorderStyle.DOUBLE);
        for (CellRange r : sel.ranges()) if (!editor.review().canFormat(sheet, r)) { editor.review().warnProtected(); return; }
        editor.edit("Bordas", tx -> {
            for (CellRange r0 : sel.ranges()) {
                CellRange r = editor.clipboard().bounded(editor.activeSheet(), r0);
                for (int row = r.firstRow(); row <= r.lastRow(); row++)
                    for (int col = r.firstColumn(); col <= r.lastColumn(); col++) {
                        boolean top = row == r.firstRow(), bottom = row == r.lastRow(), left = col == r.firstColumn(), right = col == r.lastColumn();
                        UnaryOperator<CellStyle> change = switch (kind) {
                            case NONE -> s -> s.withAllBorders(SheetBorder.NONE);
                            case ALL -> s -> s.withAllBorders(thin);
                            case INSIDE -> s -> s.withTop(top ? s.top() : thin).withBottom(bottom ? s.bottom() : thin).withLeft(left ? s.left() : thin).withRight(right ? s.right() : thin);
                            case OUTSIDE -> edges(top, bottom, left, right, thin);
                            case THICK_OUTSIDE -> edges(top, bottom, left, right, thick);
                            case TOP -> top ? s -> s.withTop(thin) : null;
                            case BOTTOM -> bottom ? s -> s.withBottom(thin) : null;
                            case LEFT -> left ? s -> s.withLeft(thin) : null;
                            case RIGHT -> right ? s -> s.withRight(thin) : null;
                            case DOUBLE_BOTTOM -> bottom ? s -> s.withBottom(dbl) : null;
                            case THICK_BOTTOM -> bottom ? s -> s.withBottom(thick) : null;
                            case TOP_BOTTOM -> top || bottom ? s -> (top ? s.withTop(thin) : s).withBottom(bottom ? thin : s.bottom()) : null;
                            case TOP_THICK_BOTTOM -> top || bottom ? s -> (top ? s.withTop(thin) : s).withBottom(bottom ? thick : s.bottom()) : null;
                            case TOP_DOUBLE_BOTTOM -> top || bottom ? s -> (top ? s.withTop(thin) : s).withBottom(bottom ? dbl : s.bottom()) : null;
                        };
                        if (change != null) tx.setStyle(sheet, row, col, change);
                    }
            }
        });
    }

    private static UnaryOperator<CellStyle> edges(boolean top, boolean bottom, boolean left, boolean right, SheetBorder b) {
        if (!top && !bottom && !left && !right) return null;
        return s -> {
            CellStyle n = s;
            if (top) n = n.withTop(b);
            if (bottom) n = n.withBottom(b);
            if (left) n = n.withLeft(b);
            if (right) n = n.withRight(b);
            return n;
        };
    }

    public boolean isPainterArmed() { return painter != null; }

    public void armPainter(boolean sticky) {
        if (painter != null) { painter = null; editor.getCanvas().setCursor(java.awt.Cursor.getDefaultCursor()); return; }
        CellRange src = editor.clipboard().bounded(editor.activeSheet(), editor.getSelection().range());
        if (src.cellCount() > 100_000) return;
        Map<CellAddress, Integer> styles = new LinkedHashMap<>();
        for (CellAddress a : src) styles.put(a, editor.activeSheet().cell(a).style());
        painter = styles;
        painterSource = src;
        painterSticky = sticky;
        if (!painterListener) {
            painterListener = true;
            editor.getCanvas().addMouseListener(new MouseAdapter() {
                @Override public void mouseReleased(MouseEvent e) { if (painter != null) java.awt.EventQueue.invokeLater(FormatController.this::paintSelection); }
            });
        }
    }

    private void paintSelection() {
        if (painter == null) return;
        Map<CellAddress, Integer> styles = painter;
        CellRange src = painterSource;
        if (!painterSticky) painter = null;
        CellRange dest = editor.clipboard().bounded(editor.activeSheet(), editor.getSelection().range());
        int sheet = editor.activeSheetIndex();
        if (!editor.review().canFormat(sheet, dest)) { editor.review().warnProtected(); return; }
        editor.edit("Pincel de Formatação", tx -> {
            for (int r = dest.firstRow(); r <= dest.lastRow(); r++)
                for (int c = dest.firstColumn(); c <= dest.lastColumn(); c++) {
                    int sr = src.firstRow() + Math.floorMod(r - dest.firstRow(), src.rowCount()), sc = src.firstColumn() + Math.floorMod(c - dest.firstColumn(), src.columnCount());
                    int style = styles.getOrDefault(new CellAddress(sr, sc), 0);
                    tx.updateCell(sheet, r, c, cell -> cell.withStyle(style));
                }
        });
    }

    public static final List<String> CELL_STYLES = List.of("Normal", "Bom", "Incorreto", "Neutro", "Cálculo", "Célula de Verificação", "Texto Explicativo", "Entrada", "Célula Vinculada",
            "Anotação", "Saída", "Texto de Aviso", "Título", "Título 1", "Título 2", "Título 3", "Título 4", "Total", "Ênfase1", "Ênfase2", "Ênfase3", "Ênfase4", "Ênfase5", "Ênfase6",
            "Porcentagem", "Moeda", "Vírgula");

    public void cellStyle(String name) {
        SheetTheme theme = editor.getWorkbook().properties().theme();
        UnaryOperator<CellStyle> change = switch (name) {
            case "Normal" -> s -> CellStyle.DEFAULT.withNumberFormat(s.numberFormat());
            case "Bom" -> s -> s.withFill(SheetFill.solid(0xFFC6EFCE)).withFontColor(0xFF006100);
            case "Incorreto" -> s -> s.withFill(SheetFill.solid(0xFFFFC7CE)).withFontColor(0xFF9C0006);
            case "Neutro" -> s -> s.withFill(SheetFill.solid(0xFFFFEB9C)).withFontColor(0xFF9C5700);
            case "Cálculo" -> s -> s.withFill(SheetFill.solid(0xFFF2F2F2)).withFontColor(0xFFFA7D00).withBold(true).withAllBorders(SheetBorder.of(BorderStyle.THIN, 0xFF7F7F7F));
            case "Célula de Verificação" -> s -> s.withFill(SheetFill.solid(0xFFA5A5A5)).withFontColor(0xFFFFFFFF).withBold(true).withAllBorders(SheetBorder.of(BorderStyle.DOUBLE, 0xFF3F3F3F));
            case "Texto Explicativo" -> s -> s.withItalic(true).withFontColor(0xFF7F7F7F);
            case "Entrada" -> s -> s.withFill(SheetFill.solid(0xFFFFCC99)).withFontColor(0xFF3F3F76).withAllBorders(SheetBorder.of(BorderStyle.THIN, 0xFF7F7F7F));
            case "Célula Vinculada" -> s -> s.withFontColor(0xFFFA7D00).withBottom(SheetBorder.of(BorderStyle.DOUBLE, 0xFFFF8001));
            case "Anotação" -> s -> s.withFill(SheetFill.solid(0xFFFFFFCC)).withAllBorders(SheetBorder.of(BorderStyle.THIN, 0xFFB2B2B2));
            case "Saída" -> s -> s.withFill(SheetFill.solid(0xFFF2F2F2)).withFontColor(0xFF3F3F3F).withBold(true).withAllBorders(SheetBorder.of(BorderStyle.THIN, 0xFF3F3F3F));
            case "Texto de Aviso" -> s -> s.withFontColor(0xFFFF0000);
            case "Título" -> s -> s.withFontFamily(theme.majorFont()).withFontSize(18).withFontColor(theme.color(3) == 0xFFE8E8E8 ? 0xFF0E2841 : theme.color(2));
            case "Título 1" -> s -> s.withBold(true).withFontSize(15).withFontColor(0xFF0E2841).withBottom(SheetBorder.of(BorderStyle.THICK, theme.accent(0)));
            case "Título 2" -> s -> s.withBold(true).withFontSize(13).withFontColor(0xFF0E2841).withBottom(SheetBorder.of(BorderStyle.THICK, SheetTheme.tint(theme.accent(0), 0.5)));
            case "Título 3" -> s -> s.withBold(true).withFontColor(0xFF0E2841).withBottom(SheetBorder.of(BorderStyle.MEDIUM, SheetTheme.tint(theme.accent(0), 0.4)));
            case "Título 4" -> s -> s.withBold(true).withFontColor(0xFF0E2841);
            case "Total" -> s -> s.withBold(true).withTop(SheetBorder.of(BorderStyle.THIN, theme.accent(0))).withBottom(SheetBorder.of(BorderStyle.DOUBLE, theme.accent(0)));
            case "Porcentagem" -> s -> s.withNumberFormat("0%");
            case "Moeda" -> s -> s.withNumberFormat(currencyFormat());
            case "Vírgula" -> s -> s.withNumberFormat("#,##0.00");
            default -> {
                if (name.startsWith("Ênfase")) {
                    int n = Integer.parseInt(name.substring(6)) - 1;
                    int color = theme.accent(n);
                    yield s -> s.withFill(SheetFill.solid(color)).withFontColor(0xFFFFFFFF);
                }
                yield s -> s;
            }
        };
        apply("Estilo de célula", change);
    }

    public void addConditionalRule(ConditionalRule rule) {
        int sheet = editor.activeSheetIndex();
        List<CellRange> ranges = new ArrayList<>();
        for (CellRange r : editor.getSelection().ranges()) ranges.add(editor.clipboard().bounded(editor.activeSheet(), r));
        editor.edit("Formatação Condicional", tx -> tx.updateProperties(sheet, p -> {
            List<ConditionalFormat> list = new ArrayList<>(p.conditionalFormats());
            int priority = list.stream().flatMap(f -> f.rules().stream()).mapToInt(ConditionalRule::priority).max().orElse(0) + 1;
            list.add(new ConditionalFormat(ranges, List.of(rule.withPriority(priority))));
            return p.withConditionalFormats(list);
        }));
    }

    public void clearConditionalRules(boolean wholeSheet) {
        int sheet = editor.activeSheetIndex();
        List<CellRange> sel = editor.getSelection().ranges();
        editor.edit("Limpar regras", tx -> tx.updateProperties(sheet, p -> {
            if (wholeSheet) return p.withConditionalFormats(List.of());
            List<ConditionalFormat> list = new ArrayList<>();
            for (ConditionalFormat f : p.conditionalFormats()) {
                List<CellRange> keep = new ArrayList<>();
                for (CellRange r : f.ranges()) if (sel.stream().noneMatch(s -> s.contains(r))) keep.add(r);
                if (!keep.isEmpty()) list.add(new ConditionalFormat(keep, f.rules()));
            }
            return p.withConditionalFormats(list);
        }));
    }

    public void setTheme(SheetTheme theme) {
        editor.edit("Tema", tx -> tx.updateWorkbook(p -> p.withTheme(theme)));
        editor.renderer().charts().setTheme(theme);
    }
}
