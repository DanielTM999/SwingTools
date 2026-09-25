package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.calc.FormulaCell;
import dtm.stools.component.panels.editor.sheet.data.ConditionalResult;
import dtm.stools.component.panels.editor.sheet.format.FormattedValue;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.model.AutoFilter;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetImage;
import dtm.stools.component.panels.editor.sheet.model.SheetNote;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetShape;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetViewMode;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.Slicer;
import dtm.stools.component.panels.editor.sheet.model.Sparkline;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.model.ValidationType;
import dtm.stools.component.panels.editor.sheet.provider.SheetCellRendererProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetConditionalRuleProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetContextMenuContext;
import dtm.stools.component.panels.editor.sheet.render.CellPaintContext;
import dtm.stools.component.panels.editor.sheet.render.SheetPalette;
import dtm.stools.component.panels.editor.sheet.render.SheetRenderer;
import dtm.stools.component.panels.editor.sheet.render.SparklinePainter;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ToolTipManager;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class SheetCanvas extends JComponent implements Accessible {
    public record Highlight(int sheet, CellRange range, Color color) {}

    private enum Drag { NONE, SELECT, COLUMN_SELECT, ROW_SELECT, RESIZE_COLUMN, RESIZE_ROW, FILL, MOVE, OBJECT_MOVE, OBJECT_RESIZE, POINT }

    private final SheetEditor editor;
    private long scrollX, scrollY;
    private SheetGeometry geometry;
    private Drag drag = Drag.NONE;
    private CellAddress dragAnchor, dragFocus;
    private int resizeIndex = -1, resizeOrigin, resizeStart;
    private CellRange fillPreview, movePreview;
    private boolean dragCopy;
    private String selectedObject;
    private Rectangle objectDragStart;
    private Point pressPoint;
    private int resizeHandle = -1;
    private List<Highlight> highlights = List.of();
    private float marqueePhase;
    private final Timer marquee;
    private final Timer autoScroll;
    private Point lastMouse;
    private final Map<String, Image> imageCache = new HashMap<>();
    private List<Integer> pageRowBreaks = List.of(), pageColumnBreaks = List.of();
    private boolean dark;

    public SheetCanvas(SheetEditor editor) {
        this.editor = editor;
        setFocusable(true);
        setOpaque(true);
        setFocusTraversalKeysEnabled(false);
        setRequestFocusEnabled(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        marquee = new Timer(120, e -> { marqueePhase = (marqueePhase + 1) % 8; if (editor.copySource() != null) repaintMarquee(); });
        marquee.start();
        autoScroll = new Timer(50, e -> autoScrollStep());
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { press(e); }
            @Override public void mouseDragged(MouseEvent e) { dragged(e); }
            @Override public void mouseReleased(MouseEvent e) { release(e); }
            @Override public void mouseMoved(MouseEvent e) { moved(e); }
            @Override public void mouseClicked(MouseEvent e) { clicked(e); }
            @Override public void mouseWheelMoved(MouseWheelEvent e) { wheel(e); }
            @Override public void mouseExited(MouseEvent e) { setCursor(Cursor.getDefaultCursor()); }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(mouse);
    }

    public SheetEditor editor() { return editor; }

    public void bind(String id, KeyStroke key, Action action) {
        getInputMap(WHEN_FOCUSED).put(key, id);
        getActionMap().put(id, action);
    }

    public void unbind(KeyStroke key) { getInputMap(WHEN_FOCUSED).remove(key); }

    public void setHighlights(List<Highlight> value) { highlights = List.copyOf(value); repaint(); }
    public List<Highlight> highlights() { return highlights; }
    public String selectedObject() { return selectedObject; }
    public void setSelectedObject(String id) { selectedObject = id; repaint(); }
    public void setDark(boolean value) { dark = value; repaint(); }
    public boolean isDark() { return dark; }
    public void setPageBreaks(List<Integer> rows, List<Integer> columns) { pageRowBreaks = List.copyOf(rows); pageColumnBreaks = List.copyOf(columns); repaint(); }
    public void clearImageCache() { imageCache.clear(); }

    public long scrollX() { return scrollX; }
    public long scrollY() { return scrollY; }

    public void setScroll(long x, long y) {
        long nx = Math.max(0, x), ny = Math.max(0, y);
        if (nx == scrollX && ny == scrollY) return;
        scrollX = nx;
        scrollY = ny;
        geometry = null;
        editor.canvasScrolled();
        repaint();
    }

    public void resetScroll() { scrollX = 0; scrollY = 0; geometry = null; }

    public SheetGeometry geometry() {
        SheetWorksheet ws = editor.getSession().getActiveSheet();
        double zoom = editor.effectiveZoom();
        boolean headers = editor.getConfig().headersVisible() && ws.properties().showHeaders();
        int headerH = headers ? (int) Math.round(22 * Math.max(0.6, zoom)) : 0;
        int headerW = 0;
        if (headers) {
            Font f = headerFont();
            FontMetrics fm = getFontMetrics(f);
            int lastRow = Math.max(999, rowEstimate(ws, zoom));
            headerW = Math.max((int) Math.round(34 * Math.max(0.6, zoom)), fm.stringWidth(String.valueOf(lastRow + 1)) + (int) Math.round(14 * Math.max(0.6, zoom)));
        }
        SheetGeometry g = geometry;
        if (g == null || g.sheet() != ws || g.zoom() != zoom || g.width() != getWidth() || g.height() != getHeight() || g.headerHeight() != headerH || g.headerWidth() != headerW
                || g.frozenRows() != ws.properties().freeze().rows() || g.frozenColumns() != ws.properties().freeze().columns()) {
            g = new SheetGeometry(ws, zoom, headerW, headerH, scrollX, scrollY, getWidth(), getHeight());
            geometry = g;
        }
        return g;
    }

    public void invalidateGeometry() { geometry = null; }

    private int rowEstimate(SheetWorksheet ws, double zoom) {
        long pos = ws.rows().position(ws.properties().freeze().rows()) + scrollY + (long) (getHeight() / Math.max(0.1, zoom));
        return ws.rows().indexAt(pos);
    }

    Font headerFont() { return new Font(Font.SANS_SERIF, Font.PLAIN, (int) Math.max(8, Math.round(12 * Math.max(0.6, editor.effectiveZoom())))); }

    public void scrollToCell(int row, int column) {
        SheetGeometry g = geometry();
        long nx = g.scrollXFor(column, true), ny = g.scrollYFor(row);
        setScroll(nx, ny);
    }

    public void scrollToTop(int row, int column) {
        SheetWorksheet ws = editor.getSession().getActiveSheet();
        int fr = ws.properties().freeze().rows(), fc = ws.properties().freeze().columns();
        setScroll(column < fc ? scrollX : ws.columns().position(column) - ws.columns().position(fc), row < fr ? scrollY : ws.rows().position(row) - ws.rows().position(fr));
    }

    public void pageScroll(int direction, boolean horizontal) {
        SheetGeometry g = geometry();
        if (horizontal) setScroll(scrollX + direction * (long) ((getWidth() - g.bodyX()) / g.zoom()), scrollY);
        else setScroll(scrollX, scrollY + direction * (long) ((getHeight() - g.bodyY()) / g.zoom()));
    }

    @Override public Dimension getPreferredSize() { return new Dimension(900, 520); }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            SheetGeometry geo = geometry();
            Color background = dark ? new Color(0x1F1F1F) : Color.WHITE;
            g.setColor(background);
            g.fillRect(0, 0, getWidth(), getHeight());
            int fr = geo.frozenRows(), fc = geo.frozenColumns();
            int bodyX = geo.bodyX(), bodyY = geo.bodyY();
            paintQuadrant(g, geo, geo.firstScrollRow(), geo.lastVisibleRow(), geo.firstScrollColumn(), geo.lastVisibleColumn(), new Rectangle(bodyX, bodyY, getWidth() - bodyX, getHeight() - bodyY));
            if (fr > 0) paintQuadrant(g, geo, 0, fr - 1, geo.firstScrollColumn(), geo.lastVisibleColumn(), new Rectangle(bodyX, geo.headerHeight(), getWidth() - bodyX, geo.frozenHeight()));
            if (fc > 0) paintQuadrant(g, geo, geo.firstScrollRow(), geo.lastVisibleRow(), 0, fc - 1, new Rectangle(geo.headerWidth(), bodyY, geo.frozenWidth(), getHeight() - bodyY));
            if (fr > 0 && fc > 0) paintQuadrant(g, geo, 0, fr - 1, 0, fc - 1, new Rectangle(geo.headerWidth(), geo.headerHeight(), geo.frozenWidth(), geo.frozenHeight()));
            paintObjects(g, geo);
            paintHeaders(g, geo);
            if (fr > 0 || fc > 0) {
                g.setColor(dark ? new Color(0x707070) : new Color(0x9E9E9E));
                if (fr > 0) g.drawLine(geo.headerWidth(), bodyY - 1, getWidth(), bodyY - 1);
                if (fc > 0) g.drawLine(bodyX - 1, geo.headerHeight(), bodyX - 1, getHeight());
            }
        } finally {
            g.dispose();
        }
    }

    private void paintQuadrant(Graphics2D g0, SheetGeometry geo, int r1, int r2, int c1, int c2, Rectangle clip) {
        if (clip.width <= 0 || clip.height <= 0 || r2 < r1 || c2 < c1) return;
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.clip(clip);
            int sheet = editor.getSession().getActiveSheetIndex();
            SheetWorksheet ws = geo.sheet();
            SheetProperties props = ws.properties();
            CalcEngine engine = editor.getEngine();
            SheetRenderer renderer = editor.renderer();
            boolean showFormulas = props.showFormulas();
            boolean grid = editor.getConfig().gridlinesVisible() && props.showGridlines();
            List<Integer> rows = new ArrayList<>(), cols = new ArrayList<>();
            for (int r = r1; r <= r2 && r < CellAddress.MAX_ROWS; r++) if (geo.rowHeight(r) > 0) rows.add(r);
            for (int c = c1; c <= c2 && c < CellAddress.MAX_COLUMNS; c++) if (geo.columnWidth(c) > 0) cols.add(c);
            if (grid) {
                g.setColor(dark ? new Color(0x3A3A3A) : new Color(0xE1E1E1));
                int left = Math.max(clip.x, geo.columnX(c1)), right = Math.min(clip.x + clip.width, geo.columnX(c2 + 1));
                int top = Math.max(clip.y, geo.rowY(r1)), bottom = Math.min(clip.y + clip.height, geo.rowY(r2 + 1));
                if (props.viewMode() == SheetViewMode.PAGE_BREAK_PREVIEW) { right = clip.x + clip.width; bottom = clip.y + clip.height; }
                for (int r : rows) { int y = geo.rowY(r + 1) - 1; g.drawLine(left, y, right, y); }
                for (int c : cols) { int x = geo.columnX(c + 1) - 1; g.drawLine(x, top, x, bottom); }
            }
            Map<Long, CellRange> mergeAnchors = new HashMap<>();
            Set<Long> merged = new HashSet<>();
            List<CellRange> merges = new ArrayList<>();
            CellRange view = new CellRange(Math.max(0, r1), Math.max(0, c1), Math.max(r1, r2), Math.max(c1, c2));
            for (CellRange m : props.merges()) {
                if (!m.intersects(view)) continue;
                merges.add(m);
                mergeAnchors.put(m.first().key(), m);
                for (int r = Math.max(m.firstRow(), r1); r <= Math.min(m.lastRow(), r2); r++)
                    for (int c = Math.max(m.firstColumn(), c1); c <= Math.min(m.lastColumn(), c2); c++) merged.add(CellAddress.key(r, c));
            }
            List<SheetCellRendererProvider> custom = editor.cellRenderers();
            List<CellPaintContext> texts = new ArrayList<>();
            Map<Long, Boolean> occupied = new HashMap<>();
            int overflowLeft = Math.max(0, c1 - 12);
            List<Integer> textCols = new ArrayList<>();
            for (int c = overflowLeft; c < c1; c++) if (geo.columnWidth(c) > 0 || c >= geo.frozenColumns()) textCols.add(c);
            textCols.addAll(cols);
            for (CellRange m : merges) {
                Rectangle rect = geo.rangeRect(m);
                SheetCell cell = ws.cell(m.first());
                CellPaintContext ctx = context(sheet, m.first(), rect, rect, ws, cell, engine, showFormulas);
                g.setColor(dark ? new Color(0x1F1F1F) : Color.WHITE);
                g.fillRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
                renderer.paintBackground(g, ctx);
                texts.add(ctx);
            }
            for (int r : rows) {
                for (int c : textCols) {
                    boolean inView = c >= c1;
                    long key = CellAddress.key(r, c);
                    if (merged.contains(key)) continue;
                    SheetCell cell = ws.cells().get(r, c);
                    CellValue value = engine.valueAt(sheet, r, c);
                    int rowStyle = ws.rows().style(r), colStyle = ws.columns().style(c);
                    if (cell == null && value.isEmpty() && rowStyle == 0 && colStyle == 0 && props.conditionalFormats().isEmpty() && !inTable(props, r, c)) continue;
                    if (cell == null) cell = new SheetCell(CellValue.EMPTY, null, rowStyle != 0 ? rowStyle : colStyle);
                    Rectangle rect = geo.cellRect(r, c);
                    CellPaintContext ctx = context(sheet, new CellAddress(r, c), rect, rect, ws, cell, engine, showFormulas);
                    if (!value.isEmpty() || cell.hasFormula()) occupied.put(key, true);
                    if (inView) {
                        boolean handled = false;
                        for (SheetCellRendererProvider p : custom) if (p.supports(ctx)) { handled = p.paint(g, ctx); if (handled) break; }
                        if (handled) continue;
                        renderer.paintBackground(g, ctx);
                    }
                    if (ctx.text() != null && !ctx.text().isEmpty() || ctx.checkbox() || ctx.value() instanceof dtm.stools.component.panels.editor.sheet.calc.SparklineValue || ctx.conditional() != null && ctx.conditional().iconSet() != null) texts.add(ctx);
                }
            }
            for (Sparkline s : props.sparklines()) {
                if (!view.contains(s.location()) || merged.contains(s.location().key())) continue;
                double[] data = editor.sparklineData(s);
                SparklinePainter.paint(g, data, s.type(), geo.cellRect(s.location().row(), s.location().column()), SheetPalette.color(s.color()), s.negativePoints(), s.markers(), s.highPoint() || s.lowPoint(), geo.zoom());
            }
            Graphics2D tg = (Graphics2D) g.create();
            for (CellPaintContext ctx : texts) {
                CellPaintContext withBounds = ctx.bounds() == ctx.textBounds() && !ctx.style().wrap() && ctx.style().rotation() == 0 && !mergeAnchors.containsKey(ctx.address().key()) ? overflow(geo, ctx, occupied, merged, tg) : ctx;
                renderer.paintContent(tg, withBounds);
            }
            tg.dispose();
            for (CellRange m : merges) renderer.paintBorders(g, editor.getSession().getWorkbook().style(ws.cell(m.first()).style()), geo.rangeRect(m));
            for (int r : rows) {
                for (int c : cols) {
                    SheetCell cell = ws.cells().get(r, c);
                    if (cell == null || cell.style() == 0 || merged.contains(CellAddress.key(r, c))) continue;
                    CellStyle st = editor.getSession().getWorkbook().style(cell.style());
                    if (!st.hasBorders()) continue;
                    renderer.paintBorders(g, st, geo.cellRect(r, c));
                }
            }
            for (CellPaintContext ctx : texts) renderer.paintIndicators(g, ctx);
            for (Map.Entry<CellAddress, SheetNote> e : props.notes().entrySet()) if (view.contains(e.getKey()) && !textContains(texts, e.getKey())) renderer.paintIndicators(g, marker(geo, e.getKey(), false));
            for (CellAddress a : props.threads().keySet()) if (view.contains(a) && !textContains(texts, a)) renderer.paintIndicators(g, marker(geo, a, true));
            paintFilterButtons(g, geo, props, view);
            paintSpillBorder(g, geo, sheet);
            paintHighlights(g, geo, sheet);
            paintSelection(g, geo);
            paintMarquee(g, geo, sheet);
            if (props.viewMode() == SheetViewMode.PAGE_BREAK_PREVIEW || !pageRowBreaks.isEmpty() || !pageColumnBreaks.isEmpty()) paintPageBreaks(g, geo, r1, r2, c1, c2);
            if (fillPreview != null) {
                g.setColor(dark ? new Color(0xBBBBBB) : new Color(0x666666));
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0));
                Rectangle rr = geo.rangeRect(fillPreview);
                g.drawRect(rr.x, rr.y, rr.width - 1, rr.height - 1);
            }
            if (movePreview != null) {
                g.setColor(new Color(0x107C41));
                g.setStroke(new BasicStroke(2f));
                Rectangle rr = geo.rangeRect(movePreview);
                g.drawRect(rr.x, rr.y, rr.width - 1, rr.height - 1);
            }
        } finally {
            g.dispose();
        }
    }

    private static boolean inTable(SheetProperties props, int row, int column) {
        for (SheetTable t : props.tables()) if (t.range().contains(row, column)) return true;
        return false;
    }

    private static boolean textContains(List<CellPaintContext> texts, CellAddress a) {
        for (CellPaintContext c : texts) if (c.address().equals(a)) return true;
        return false;
    }

    private CellPaintContext marker(SheetGeometry geo, CellAddress a, boolean thread) {
        Rectangle r = geo.cellRect(a.row(), a.column());
        return new CellPaintContext(0, a, r, r, CellStyle.DEFAULT, CellValue.EMPTY, "", null, null, geo.zoom(), false, false, !thread, thread, false, false, false, dark);
    }

    private CellPaintContext context(int sheet, CellAddress a, Rectangle rect, Rectangle textRect, SheetWorksheet ws, SheetCell cell, CalcEngine engine, boolean showFormulas) {
        CellValue value = engine.valueAt(sheet, a.row(), a.column());
        CellStyle style = editor.getSession().getWorkbook().style(cell.style());
        for (SheetTable t : ws.properties().tables()) if (t.range().contains(a)) { style = TableStyles.apply(style, cell.style() == 0, t, a.row(), a.column(), editor.getSession().getWorkbook().properties().theme()); break; }
        ConditionalResult cf = editor.conditionalEvaluator().evaluate(sheet, a.row(), a.column());
        for (SheetConditionalRuleProvider p : editor.conditionalProviders()) {
            Optional<DifferentialStyle> extra = p.style(editor, sheet, a.row(), a.column(), value);
            if (extra.isPresent()) cf = new ConditionalResult(extra.get(), cf.scaleColor(), cf.barFraction(), cf.barColor(), cf.barGradient(), cf.iconSet(), cf.iconIndex(), cf.hideValue());
        }
        if (cf.style() != null) style = cf.style().apply(style);
        String text;
        Integer color = null;
        boolean fillChar = false;
        if (showFormulas && cell.hasFormula()) text = "=" + Formulas.toDisplay(cell.formula(), editor.formulaLocale());
        else {
            FormattedValue f = editor.formatter().format(value, style.numberFormat());
            text = f.text();
            color = f.color();
            fillChar = f.fill() != null;
        }
        boolean checkbox = false;
        Optional<DataValidation> v = editor.validationEvaluator().find(sheet, a.row(), a.column());
        if (v.isPresent() && v.get().type() == ValidationType.CHECKBOX) checkbox = true;
        boolean error = value instanceof ErrorValue && cell.hasFormula() && editor.showErrorIndicators();
        SheetProperties props = ws.properties();
        boolean note = props.notes().containsKey(a), thread = props.threads().containsKey(a), link = props.links().containsKey(a) || cell.hasFormula() && cell.formula().startsWith("HYPERLINK(");
        return new CellPaintContext(sheet, a, rect, textRect, style, value, text, color, cf.isEmpty() ? null : cf, geometry().zoom(), showFormulas, error, note, thread, link, checkbox, fillChar, dark);
    }

    private CellPaintContext overflow(SheetGeometry geo, CellPaintContext ctx, Map<Long, Boolean> occupied, Set<Long> merged, Graphics2D g) {
        if (!(ctx.value() instanceof TextValue) && !(ctx.showFormulas())) return ctx;
        SheetRenderer renderer = editor.renderer();
        int needed = renderer.measureWidth(g, ctx.style(), ctx.text(), geo.zoom());
        Rectangle r = ctx.bounds();
        if (needed <= r.width) return ctx;
        HorizontalAlignment h = ctx.style().horizontal();
        int row = ctx.address().row(), col = ctx.address().column();
        int left = r.x, right = r.x + r.width;
        boolean toRight = h == HorizontalAlignment.GENERAL || h == HorizontalAlignment.LEFT || h == HorizontalAlignment.CENTER || h == HorizontalAlignment.CENTER_ACROSS;
        boolean toLeft = h == HorizontalAlignment.RIGHT || h == HorizontalAlignment.CENTER || h == HorizontalAlignment.CENTER_ACROSS;
        int extra = needed - r.width;
        int c = col;
        while (toRight && right - r.x < needed + (toLeft ? extra / 2 : 0) && c + 1 < CellAddress.MAX_COLUMNS && right < getWidth() + 400) {
            c++;
            long key = CellAddress.key(row, c);
            if (occupied.containsKey(key) || merged.contains(key) || hasContent(ctx.sheet(), row, c)) break;
            right += geo.columnWidth(c);
        }
        c = col;
        while (toLeft && r.x + r.width - left < needed && c - 1 >= 0 && left > -400) {
            c--;
            long key = CellAddress.key(row, c);
            if (occupied.containsKey(key) || merged.contains(key) || hasContent(ctx.sheet(), row, c)) break;
            left -= geo.columnWidth(c);
        }
        Rectangle tb = new Rectangle(left, r.y, right - left, r.height);
        return new CellPaintContext(ctx.sheet(), ctx.address(), ctx.style().horizontal() == HorizontalAlignment.GENERAL || h == HorizontalAlignment.LEFT ? new Rectangle(r.x, r.y, right - r.x, r.height) : r, tb,
                ctx.style(), ctx.value(), ctx.text(), ctx.textColor(), ctx.conditional(), ctx.zoom(), ctx.showFormulas(), ctx.errorIndicator(), ctx.noteIndicator(),
                ctx.threadIndicator(), ctx.hyperlink(), ctx.checkbox(), ctx.fillCharacter(), ctx.dark());
    }

    private boolean hasContent(int sheet, int row, int column) { return !editor.getEngine().valueAt(sheet, row, column).isEmpty(); }

    private void paintFilterButtons(Graphics2D g, SheetGeometry geo, SheetProperties props, CellRange view) {
        AutoFilter af = props.autoFilter();
        if (af != null) {
            CellRange r = af.range();
            if (r.firstRow() >= view.firstRow() && r.firstRow() <= view.lastRow())
                for (int c = Math.max(r.firstColumn(), view.firstColumn()); c <= Math.min(r.lastColumn(), view.lastColumn()); c++)
                    editor.renderer().paintDropdownButton(g, filterButton(geo, r.firstRow(), c), af.criteria().containsKey(c), dark);
        }
        for (SheetTable t : props.tables()) {
            if (!t.headerRow() || !t.filterButton()) continue;
            CellRange r = t.range();
            if (r.firstRow() < view.firstRow() || r.firstRow() > view.lastRow()) continue;
            for (int c = Math.max(r.firstColumn(), view.firstColumn()); c <= Math.min(r.lastColumn(), view.lastColumn()); c++)
                editor.renderer().paintDropdownButton(g, filterButton(geo, r.firstRow(), c), editor.isTableColumnFiltered(t, c), dark);
        }
    }

    Rectangle filterButton(SheetGeometry geo, int row, int column) {
        Rectangle cell = geo.cellRect(row, column);
        int size = Math.max(10, Math.min(cell.height - 2, (int) Math.round(16 * geo.zoom())));
        return new Rectangle(cell.x + cell.width - size - 2, cell.y + (cell.height - size) / 2, size, size);
    }

    private void paintSpillBorder(Graphics2D g, SheetGeometry geo, int sheet) {
        CellAddress active = editor.getSession().getSelection().active();
        Optional<FormulaCell> anchor = editor.getEngine().spillAnchor(sheet, active.row(), active.column());
        if (anchor.isEmpty() || anchor.get().spillRange() == null) return;
        Rectangle r = geo.rangeRect(anchor.get().spillRange());
        g.setColor(new Color(0x4472C4));
        g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2f, 2f}, 0));
        g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
        g.setStroke(new BasicStroke(1f));
    }

    private void paintHighlights(Graphics2D g, SheetGeometry geo, int sheet) {
        for (Highlight h : highlights) {
            if (h.sheet() != sheet) continue;
            Rectangle r = geo.rangeRect(h.range());
            g.setColor(new Color(h.color().getRed(), h.color().getGreen(), h.color().getBlue(), 28));
            g.fillRect(r.x, r.y, r.width, r.height);
            g.setColor(h.color());
            g.setStroke(new BasicStroke(1.5f));
            g.drawRect(r.x, r.y, r.width - 1, r.height - 1);
            int s = 5;
            for (int[] p : new int[][]{{r.x, r.y}, {r.x + r.width, r.y}, {r.x, r.y + r.height}, {r.x + r.width, r.y + r.height}}) g.fillRect(p[0] - s / 2, p[1] - s / 2, s, s);
        }
        g.setStroke(new BasicStroke(1f));
    }

    private void paintSelection(Graphics2D g, SheetGeometry geo) {
        if (selectedObject != null) return;
        SheetSelection sel = editor.getSession().getSelection();
        Color accent = new Color(0x107C41);
        Composite old = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, dark ? 0.22f : 0.14f));
        g.setColor(dark ? new Color(0x6FCF97) : new Color(0x0E5C2F));
        for (CellRange range : sel.ranges()) {
            CellRange r = expandMerges(range);
            if (r.isSingleCell() && sel.ranges().size() == 1) continue;
            Rectangle rect = geo.rangeRect(r);
            Shape clip = g.getClip();
            if (sel.ranges().size() == 1 || r.contains(sel.active())) {
                Rectangle active = geo.rangeRect(mergeAt(sel.active()));
                java.awt.geom.Area area = new java.awt.geom.Area(rect);
                area.subtract(new java.awt.geom.Area(active));
                g.fill(area);
            } else g.fill(rect);
            g.setClip(clip);
        }
        g.setComposite(old);
        CellRange last = expandMerges(sel.range());
        Rectangle rect = geo.rangeRect(last);
        g.setColor(accent);
        g.setStroke(new BasicStroke(2f));
        if (sel.ranges().size() == 1) g.drawRect(rect.x, rect.y, rect.width - 1, rect.height - 1);
        else {
            Rectangle active = geo.rangeRect(mergeAt(sel.active()));
            g.drawRect(active.x, active.y, active.width - 1, active.height - 1);
        }
        g.setStroke(new BasicStroke(1f));
        if (sel.ranges().size() == 1 && !editor.isReadOnlyView()) {
            Rectangle h = fillHandle(rect);
            g.setColor(dark ? new Color(0x1F1F1F) : Color.WHITE);
            g.fillRect(h.x - 1, h.y - 1, h.width + 2, h.height + 2);
            g.setColor(accent);
            g.fillRect(h.x, h.y, h.width, h.height);
        }
        Optional<DataValidation> v = editor.validationEvaluator().find(editor.getSession().getActiveSheetIndex(), sel.active().row(), sel.active().column());
        if (v.isPresent() && v.get().type() == ValidationType.LIST && v.get().showDropdown() && !editor.isEditing()) {
            Rectangle act = geo.rangeRect(mergeAt(sel.active()));
            editor.renderer().paintDropdownButton(g, validationButton(act), false, dark);
        }
    }

    Rectangle validationButton(Rectangle cell) {
        int size = Math.max(14, Math.min(cell.height, 18));
        return new Rectangle(cell.x + cell.width + 1, cell.y + cell.height - size, size, size);
    }

    private Rectangle fillHandle(Rectangle rect) {
        int s = (int) Math.max(5, Math.round(6 * Math.min(1.5, geometry().zoom())));
        return new Rectangle(rect.x + rect.width - s / 2 - 1, rect.y + rect.height - s / 2 - 1, s, s);
    }

    private void paintMarquee(Graphics2D g, SheetGeometry geo, int sheet) {
        CellRange copy = editor.copySource();
        if (copy == null || editor.copySheet() != sheet) return;
        Rectangle r = geo.rangeRect(copy);
        g.setColor(new Color(0x107C41));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, marqueePhase));
        g.drawRect(r.x + 1, r.y + 1, r.width - 3, r.height - 3);
        g.setStroke(new BasicStroke(1f));
    }

    private void repaintMarquee() {
        CellRange copy = editor.copySource();
        if (copy == null) return;
        Rectangle r = geometry().rangeRect(copy);
        repaint(r.x - 2, r.y - 2, r.width + 4, r.height + 4);
    }

    private void paintPageBreaks(Graphics2D g, SheetGeometry geo, int r1, int r2, int c1, int c2) {
        g.setColor(new Color(0x2F5597));
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 4f}, 0));
        for (int b : pageRowBreaks) if (b >= r1 && b <= r2 + 1) { int y = geo.rowY(b); g.drawLine(geo.headerWidth(), y, getWidth(), y); }
        for (int b : pageColumnBreaks) if (b >= c1 && b <= c2 + 1) { int x = geo.columnX(b); g.drawLine(x, geo.headerHeight(), x, getHeight()); }
        g.setStroke(new BasicStroke(1f));
    }

    private void paintObjects(Graphics2D g0, SheetGeometry geo) {
        Graphics2D g = (Graphics2D) g0.create();
        try {
            g.clip(new Rectangle(geo.headerWidth(), geo.headerHeight(), getWidth(), getHeight()));
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (SheetObject o : geo.sheet().properties().objects()) {
                Rectangle r = geo.objectRect(o.anchor());
                if (r.x > getWidth() || r.y > getHeight() || r.x + r.width < 0 || r.y + r.height < 0) continue;
                switch (o) {
                    case SheetChart chart -> editor.renderer().charts().paint(g, chart, editor.chartData(chart), r, dark);
                    case SheetImage image -> {
                        Image img = imageCache.computeIfAbsent(image.id(), k -> { try { return javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(image.data())); } catch (Exception e) { return null; } });
                        if (img != null) g.drawImage(img, r.x, r.y, r.width, r.height, null);
                        else { g.setColor(Color.LIGHT_GRAY); g.fill(r); }
                    }
                    case SheetShape shape -> paintShape(g, shape, r);
                    case Slicer slicer -> paintSlicer(g, slicer, r);
                    default -> { }
                }
                if (o.id().equals(selectedObject)) {
                    g.setColor(new Color(0x107C41));
                    g.setStroke(new BasicStroke(1f));
                    g.drawRect(r.x, r.y, r.width, r.height);
                    for (Rectangle h : handles(r)) { g.setColor(Color.WHITE); g.fill(h); g.setColor(new Color(0x107C41)); g.draw(h); }
                }
            }
        } finally {
            g.dispose();
        }
    }

    private void paintShape(Graphics2D g, SheetShape s, Rectangle r) {
        Shape shape = switch (s.type()) {
            case ELLIPSE -> new Ellipse2D.Double(r.x, r.y, r.width, r.height);
            case ROUNDED_RECTANGLE -> new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, Math.min(r.width, r.height) / 3.0, Math.min(r.width, r.height) / 3.0);
            case TRIANGLE -> { Path2D p = new Path2D.Double(); p.moveTo(r.x + r.width / 2.0, r.y); p.lineTo(r.x + r.width, r.y + r.height); p.lineTo(r.x, r.y + r.height); p.closePath(); yield p; }
            case ARROW_RIGHT -> { Path2D p = new Path2D.Double(); double h = r.height; p.moveTo(r.x, r.y + h * .3); p.lineTo(r.x + r.width * .65, r.y + h * .3); p.lineTo(r.x + r.width * .65, r.y); p.lineTo(r.x + r.width, r.y + h / 2); p.lineTo(r.x + r.width * .65, r.y + h); p.lineTo(r.x + r.width * .65, r.y + h * .7); p.lineTo(r.x, r.y + h * .7); p.closePath(); yield p; }
            case LINE -> new java.awt.geom.Line2D.Double(r.x, r.y, r.x + r.width, r.y + r.height);
            default -> new Rectangle(r);
        };
        if (s.type() != dtm.stools.component.panels.editor.sheet.model.ShapeType.LINE) {
            g.setColor(s.fill() == null ? (s.type() == dtm.stools.component.panels.editor.sheet.model.ShapeType.TEXT_BOX ? (dark ? new Color(0x2B2B2B) : Color.WHITE) : new Color(0x4472C4)) : SheetPalette.color(s.fill()));
            g.fill(shape);
        }
        g.setColor(s.line() == null ? new Color(0x2F528F) : SheetPalette.color(s.line()));
        g.setStroke(new BasicStroke(Math.max(1f, s.lineWidth())));
        g.draw(shape);
        g.setStroke(new BasicStroke(1f));
        if (!s.text().isEmpty()) {
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, (int) Math.max(9, 14 * geometry().zoom())));
            FontMetrics fm = g.getFontMetrics();
            Color fill = s.fill() == null ? new Color(0x4472C4) : SheetPalette.color(s.fill());
            g.setColor(s.type() == dtm.stools.component.panels.editor.sheet.model.ShapeType.TEXT_BOX && s.fill() == null ? (dark ? Color.WHITE : Color.BLACK) : SheetPalette.contrast(fill));
            List<String> lines = SheetRenderer.wrap(s.text(), fm, r.width - 8);
            int y = r.y + (r.height - lines.size() * fm.getHeight()) / 2 + fm.getAscent();
            for (String line : lines) { g.drawString(line, r.x + (r.width - fm.stringWidth(line)) / 2, y); y += fm.getHeight(); }
        }
    }

    private void paintSlicer(Graphics2D g, Slicer s, Rectangle r) {
        g.setColor(dark ? new Color(0x2B2B2B) : Color.WHITE);
        g.fill(r);
        g.setColor(dark ? new Color(0x606060) : new Color(0xBFBFBF));
        g.draw(r);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(dark ? Color.WHITE : Color.BLACK);
        g.drawString(s.field(), r.x + 8, r.y + fm.getAscent() + 6);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        List<String> items = editor.slicerItems(s);
        int y = r.y + fm.getHeight() + 12;
        for (String item : items) {
            if (y + 22 > r.y + r.height) break;
            boolean on = s.selected().isEmpty() || s.selected().contains(item);
            g.setColor(on ? new Color(0xDDEBF7) : (dark ? new Color(0x3A3A3A) : new Color(0xF2F2F2)));
            g.fillRect(r.x + 6, y, r.width - 12, 20);
            g.setColor(dark ? Color.WHITE : Color.BLACK);
            g.drawString(item, r.x + 12, y + 15);
            y += 24;
        }
    }

    private List<Rectangle> handles(Rectangle r) {
        int s = 7;
        List<Rectangle> list = new ArrayList<>();
        int[] xs = {r.x, r.x + r.width / 2, r.x + r.width}, ys = {r.y, r.y + r.height / 2, r.y + r.height};
        for (int iy = 0; iy < 3; iy++) for (int ix = 0; ix < 3; ix++) if (!(ix == 1 && iy == 1)) list.add(new Rectangle(xs[ix] - s / 2, ys[iy] - s / 2, s, s));
        return list;
    }

    private void paintHeaders(Graphics2D g, SheetGeometry geo) {
        if (geo.headerHeight() == 0) return;
        SheetSelection sel = editor.getSession().getSelection();
        Color bg = dark ? new Color(0x2B2B2B) : new Color(0xF5F5F5), line = dark ? new Color(0x444444) : new Color(0xD4D4D4);
        Color selBg = dark ? new Color(0x3F5F4A) : new Color(0xD3E7D9), fullBg = dark ? new Color(0x2E7D4F) : new Color(0x107C41);
        Color fg = dark ? new Color(0xCCCCCC) : new Color(0x444444);
        g.setFont(headerFont());
        FontMetrics fm = g.getFontMetrics();
        g.setColor(bg);
        g.fillRect(0, 0, getWidth(), geo.headerHeight());
        g.fillRect(0, 0, geo.headerWidth(), getHeight());
        int fc = geo.frozenColumns(), fr = geo.frozenRows();
        List<int[]> colRanges = new ArrayList<>();
        if (fc > 0) colRanges.add(new int[]{0, fc - 1});
        colRanges.add(new int[]{geo.firstScrollColumn(), geo.lastVisibleColumn()});
        for (int[] cr : colRanges) {
            for (int c = cr[0]; c <= cr[1]; c++) {
                int w = geo.columnWidth(c);
                if (w <= 0) continue;
                int x = geo.columnX(c);
                if (c >= fc && x + w < geo.bodyX()) continue;
                boolean full = false, partial = false;
                for (CellRange r : sel.ranges()) { if (r.isWholeColumn() && c >= r.firstColumn() && c <= r.lastColumn()) full = true; if (c >= r.firstColumn() && c <= r.lastColumn()) partial = true; }
                Shape clip = g.getClip();
                if (c >= fc) g.clipRect(geo.bodyX(), 0, getWidth(), geo.headerHeight());
                if (full || partial) { g.setColor(full ? fullBg : selBg); g.fillRect(x, 0, w, geo.headerHeight()); }
                g.setColor(line);
                g.drawLine(x + w - 1, 0, x + w - 1, geo.headerHeight());
                String name = CellAddress.columnName(c);
                g.setColor(full ? Color.WHITE : partial ? (dark ? Color.WHITE : new Color(0x0E5C2F)) : fg);
                g.drawString(name, x + (w - fm.stringWidth(name)) / 2, (geo.headerHeight() + fm.getAscent() - fm.getDescent()) / 2);
                if (partial && !full) { g.setColor(new Color(0x107C41)); g.fillRect(x, geo.headerHeight() - 2, w, 2); }
                g.setClip(clip);
            }
        }
        List<int[]> rowRanges = new ArrayList<>();
        if (fr > 0) rowRanges.add(new int[]{0, fr - 1});
        rowRanges.add(new int[]{geo.firstScrollRow(), geo.lastVisibleRow()});
        for (int[] rr : rowRanges) {
            for (int r = rr[0]; r <= rr[1]; r++) {
                int h = geo.rowHeight(r);
                if (h <= 0) continue;
                int y = geo.rowY(r);
                boolean full = false, partial = false;
                for (CellRange x : sel.ranges()) { if (x.isWholeRow() && r >= x.firstRow() && r <= x.lastRow()) full = true; if (r >= x.firstRow() && r <= x.lastRow()) partial = true; }
                Shape clip = g.getClip();
                if (r >= fr) g.clipRect(0, geo.bodyY(), geo.headerWidth(), getHeight());
                if (full || partial) { g.setColor(full ? fullBg : selBg); g.fillRect(0, y, geo.headerWidth(), h); }
                g.setColor(line);
                g.drawLine(0, y + h - 1, geo.headerWidth(), y + h - 1);
                String name = String.valueOf(r + 1);
                g.setColor(full ? Color.WHITE : partial ? (dark ? Color.WHITE : new Color(0x0E5C2F)) : fg);
                if (h >= fm.getAscent() - 2) g.drawString(name, (geo.headerWidth() - fm.stringWidth(name)) / 2, y + (h + fm.getAscent() - fm.getDescent()) / 2);
                if (partial && !full) { g.setColor(new Color(0x107C41)); g.fillRect(geo.headerWidth() - 2, y, 2, h); }
                g.setClip(clip);
            }
        }
        g.setColor(line);
        g.drawLine(0, geo.headerHeight() - 1, getWidth(), geo.headerHeight() - 1);
        g.drawLine(geo.headerWidth() - 1, 0, geo.headerWidth() - 1, getHeight());
        g.setColor(bg);
        g.fillRect(0, 0, geo.headerWidth() - 1, geo.headerHeight() - 1);
        g.setColor(dark ? new Color(0x777777) : new Color(0xB5B5B5));
        int s = Math.min(geo.headerWidth(), geo.headerHeight()) / 2;
        g.fillPolygon(new int[]{geo.headerWidth() - 4, geo.headerWidth() - 4, geo.headerWidth() - 4 - s}, new int[]{geo.headerHeight() - 4 - s, geo.headerHeight() - 4, geo.headerHeight() - 4}, 3);
    }

    public CellRange expandMerges(CellRange r) {
        CellRange out = r;
        List<CellRange> merges = editor.getSession().getActiveSheet().properties().merges();
        if (merges.isEmpty() || r.isWholeColumn() || r.isWholeRow()) return r;
        boolean changed = true;
        while (changed) {
            changed = false;
            for (CellRange m : merges) if (m.intersects(out) && !out.contains(m)) { out = out.union(m); changed = true; }
        }
        return out;
    }

    public CellRange mergeAt(CellAddress a) {
        CellRange m = editor.getSession().getActiveSheet().properties().mergeAt(a.row(), a.column());
        return m == null ? CellRange.of(a) : m;
    }

    private boolean nearColumnBoundary(SheetGeometry geo, int x, int y) {
        if (y >= geo.headerHeight() || x < geo.headerWidth()) return false;
        int c = geo.columnAt(x);
        if (c < 0) return false;
        int right = geo.columnX(c + 1), left = geo.columnX(c);
        if (Math.abs(x - right) <= 3) { resizeIndex = c; return true; }
        if (Math.abs(x - left) <= 3 && c > 0) { int prev = c - 1; while (prev > 0 && geo.columnWidth(prev) == 0 && !geo.sheet().columns().isHidden(prev)) prev--; resizeIndex = prev; return true; }
        return false;
    }

    private boolean nearRowBoundary(SheetGeometry geo, int x, int y) {
        if (x >= geo.headerWidth() || y < geo.headerHeight()) return false;
        int r = geo.rowAt(y);
        if (r < 0) return false;
        int bottom = geo.rowY(r + 1), top = geo.rowY(r);
        if (Math.abs(y - bottom) <= 3) { resizeIndex = r; return true; }
        if (Math.abs(y - top) <= 3 && r > 0) { resizeIndex = r - 1; return true; }
        return false;
    }

    private SheetObject objectAt(Point p) {
        SheetGeometry geo = geometry();
        List<SheetObject> objects = geo.sheet().properties().objects();
        for (int k = objects.size() - 1; k >= 0; k--) if (geo.objectRect(objects.get(k).anchor()).contains(p)) return objects.get(k);
        return null;
    }

    private int handleAt(Rectangle r, Point p) {
        List<Rectangle> hs = handles(r);
        for (int k = 0; k < hs.size(); k++) { Rectangle h = hs.get(k); h.grow(2, 2); if (h.contains(p)) return k; }
        return -1;
    }

    private void press(MouseEvent e) {
        requestFocusInWindow();
        SheetGeometry geo = geometry();
        Point p = e.getPoint();
        pressPoint = p;
        lastMouse = p;
        if (SwingUtilities.isRightMouseButton(e) || e.isPopupTrigger()) { popup(e); return; }
        if (!SwingUtilities.isLeftMouseButton(e)) return;
        if (selectedObject != null) {
            SheetObject o = findObject(selectedObject);
            if (o != null) {
                Rectangle r = geo.objectRect(o.anchor());
                int h = handleAt(r, p);
                if (h >= 0 && !editor.isReadOnlyView()) { drag = Drag.OBJECT_RESIZE; resizeHandle = h; objectDragStart = r; return; }
            }
        }
        SheetObject hit = objectAt(p);
        if (hit != null) {
            if (hit instanceof Slicer s && hit.id().equals(selectedObject)) { editor.slicerClicked(s, p, geo.objectRect(s.anchor())); return; }
            editor.commitEditingIfActive();
            selectedObject = hit.id();
            editor.objectSelected(hit);
            if (!editor.isReadOnlyView()) { drag = Drag.OBJECT_MOVE; objectDragStart = geo.objectRect(hit.anchor()); }
            repaint();
            return;
        }
        if (selectedObject != null) { selectedObject = null; editor.objectSelected(null); repaint(); }
        if (p.x < geo.headerWidth() && p.y < geo.headerHeight()) { editor.selectAll(); return; }
        if (nearColumnBoundary(geo, p.x, p.y) && !editor.isReadOnlyView()) {
            if (e.getClickCount() == 2) { editor.autoFitColumns(columnsForResize(resizeIndex)); drag = Drag.NONE; return; }
            drag = Drag.RESIZE_COLUMN; resizeOrigin = p.x; resizeStart = geo.sheet().columns().size(resizeIndex); return;
        }
        if (nearRowBoundary(geo, p.x, p.y) && !editor.isReadOnlyView()) {
            if (e.getClickCount() == 2) { editor.autoFitRows(rowsForResize(resizeIndex)); drag = Drag.NONE; return; }
            drag = Drag.RESIZE_ROW; resizeOrigin = p.y; resizeStart = geo.sheet().rows().size(resizeIndex); return;
        }
        if (p.y < geo.headerHeight()) {
            int c = geo.columnAt(p.x);
            if (c < 0) return;
            if (editor.isFormulaPointMode()) { editor.insertReference(CellRange.columns(c, c), e.isShiftDown()); drag = Drag.POINT; dragAnchor = new CellAddress(0, c); return; }
            editor.commitEditingIfActive();
            SheetSelection sel = editor.getSession().getSelection();
            if (e.isShiftDown()) editor.select(new SheetSelection(sel.active(), sel.anchor(), replaceLast(sel, CellRange.columns(Math.min(sel.anchor().column(), c), Math.max(sel.anchor().column(), c)))));
            else if (e.isControlDown()) editor.select(sel.addRange(CellRange.columns(c, c)).withActive(new CellAddress(geo.firstScrollRow(), c)));
            else editor.select(new SheetSelection(new CellAddress(Math.max(0, geo.firstScrollRow()), c), new CellAddress(0, c), List.of(CellRange.columns(c, c))));
            drag = Drag.COLUMN_SELECT;
            dragAnchor = new CellAddress(0, e.isShiftDown() ? sel.anchor().column() : c);
            return;
        }
        if (p.x < geo.headerWidth()) {
            int r = geo.rowAt(p.y);
            if (r < 0) return;
            if (editor.isFormulaPointMode()) { editor.insertReference(CellRange.rows(r, r), e.isShiftDown()); drag = Drag.POINT; dragAnchor = new CellAddress(r, 0); return; }
            editor.commitEditingIfActive();
            SheetSelection sel = editor.getSession().getSelection();
            if (e.isShiftDown()) editor.select(new SheetSelection(sel.active(), sel.anchor(), replaceLast(sel, CellRange.rows(Math.min(sel.anchor().row(), r), Math.max(sel.anchor().row(), r)))));
            else if (e.isControlDown()) editor.select(sel.addRange(CellRange.rows(r, r)).withActive(new CellAddress(r, geo.firstScrollColumn())));
            else editor.select(new SheetSelection(new CellAddress(r, Math.max(0, geo.firstScrollColumn())), new CellAddress(r, 0), List.of(CellRange.rows(r, r))));
            drag = Drag.ROW_SELECT;
            dragAnchor = new CellAddress(e.isShiftDown() ? sel.anchor().row() : r, 0);
            return;
        }
        int row = geo.rowAt(p.y), col = geo.columnAt(p.x);
        if (row < 0 || col < 0) return;
        CellAddress cell = new CellAddress(row, col);
        if (editor.isFormulaPointMode()) {
            CellRange target = mergeAt(cell);
            editor.insertReference(target, e.isShiftDown());
            drag = Drag.POINT;
            dragAnchor = cell;
            return;
        }
        SheetSelection sel = editor.getSession().getSelection();
        Rectangle selRect = geo.rangeRect(expandMerges(sel.range()));
        if (!editor.isReadOnlyView() && sel.ranges().size() == 1 && fillHandle(selRect).getBounds().contains(p.x + 1, p.y + 1) || fillHandleHit(selRect, p) && sel.ranges().size() == 1 && !editor.isReadOnlyView()) {
            editor.commitEditingIfActive();
            if (e.getClickCount() == 2) { editor.fillDownToAdjacent(); return; }
            drag = Drag.FILL;
            fillPreview = sel.range();
            return;
        }
        if (!editor.isReadOnlyView() && onBorder(selRect, p) && sel.ranges().size() == 1) {
            editor.commitEditingIfActive();
            drag = Drag.MOVE;
            dragCopy = e.isControlDown();
            dragAnchor = cell;
            movePreview = sel.range();
            return;
        }
        AutoFilter af = geo.sheet().properties().autoFilter();
        if (af != null && row == af.range().firstRow() && col >= af.range().firstColumn() && col <= af.range().lastColumn() && filterButton(geo, row, col).contains(p)) {
            editor.commitEditingIfActive();
            editor.showFilterMenu(null, col, filterButton(geo, row, col));
            return;
        }
        for (SheetTable t : geo.sheet().properties().tables()) {
            if (t.headerRow() && t.filterButton() && row == t.range().firstRow() && col >= t.range().firstColumn() && col <= t.range().lastColumn() && filterButton(geo, row, col).contains(p)) {
                editor.commitEditingIfActive();
                editor.showFilterMenu(t, col, filterButton(geo, row, col));
                return;
            }
        }
        Rectangle active = geo.rangeRect(mergeAt(sel.active()));
        if (validationButton(active).contains(p)) {
            Optional<DataValidation> v = editor.validationEvaluator().find(editor.getSession().getActiveSheetIndex(), sel.active().row(), sel.active().column());
            if (v.isPresent() && v.get().type() == ValidationType.LIST && v.get().showDropdown()) { editor.showValidationList(sel.active(), validationButton(active)); return; }
        }
        editor.commitEditingIfActive();
        if (e.getClickCount() == 1 && editor.isCheckbox(cell) && !editor.isReadOnlyView()) {
            Rectangle cr = geo.cellRect(row, col);
            int size = (int) Math.max(10, Math.min(cr.height - 4, 14 * geo.zoom()));
            if (new Rectangle(cr.x + (cr.width - size) / 2, cr.y + (cr.height - size) / 2, size, size).contains(p)) { editor.select(SheetSelection.of(cell)); editor.toggleCheckbox(cell); return; }
        }
        if (e.getClickCount() == 2) { editor.select(SheetSelection.of(mergeAt(cell).first())); editor.startEditing(null, false); return; }
        if (e.isShiftDown()) editor.select(sel.extendTo(cell).withActive(sel.active()));
        else if (e.isControlDown()) editor.select(sel.addRange(mergeAt(cell)).withActive(cell));
        else editor.select(new SheetSelection(cell, cell, List.of(mergeAt(cell))));
        drag = Drag.SELECT;
        dragAnchor = e.isShiftDown() ? sel.anchor() : cell;
        dragFocus = cell;
    }

    private static boolean fillHandleHit(Rectangle rect, Point p) {
        return Math.abs(p.x - (rect.x + rect.width)) <= 4 && Math.abs(p.y - (rect.y + rect.height)) <= 4;
    }

    private static boolean onBorder(Rectangle r, Point p) {
        int t = 3;
        boolean nearX = Math.abs(p.x - r.x) <= t || Math.abs(p.x - (r.x + r.width)) <= t;
        boolean nearY = Math.abs(p.y - r.y) <= t || Math.abs(p.y - (r.y + r.height)) <= t;
        boolean insideX = p.x >= r.x - t && p.x <= r.x + r.width + t, insideY = p.y >= r.y - t && p.y <= r.y + r.height + t;
        return nearX && insideY || nearY && insideX;
    }

    private static List<CellRange> replaceLast(SheetSelection sel, CellRange r) {
        List<CellRange> list = new ArrayList<>(sel.ranges());
        list.set(list.size() - 1, r);
        return list;
    }

    private List<Integer> columnsForResize(int index) {
        SheetSelection sel = editor.getSession().getSelection();
        for (CellRange r : sel.ranges()) if (r.isWholeColumn() && index >= r.firstColumn() && index <= r.lastColumn()) {
            List<Integer> list = new ArrayList<>();
            for (CellRange x : sel.ranges()) if (x.isWholeColumn()) for (int c = x.firstColumn(); c <= x.lastColumn(); c++) list.add(c);
            return list;
        }
        return List.of(index);
    }

    private List<Integer> rowsForResize(int index) {
        SheetSelection sel = editor.getSession().getSelection();
        for (CellRange r : sel.ranges()) if (r.isWholeRow() && index >= r.firstRow() && index <= r.lastRow()) {
            List<Integer> list = new ArrayList<>();
            for (CellRange x : sel.ranges()) if (x.isWholeRow()) for (int k = x.firstRow(); k <= x.lastRow(); k++) list.add(k);
            return list;
        }
        return List.of(index);
    }

    private SheetObject findObject(String id) {
        for (SheetObject o : geometry().sheet().properties().objects()) if (o.id().equals(id)) return o;
        return null;
    }

    private void dragged(MouseEvent e) {
        lastMouse = e.getPoint();
        SheetGeometry geo = geometry();
        Point p = e.getPoint();
        boolean outside = p.x < geo.bodyX() || p.y < geo.bodyY() || p.x > getWidth() || p.y > getHeight();
        if (drag == Drag.SELECT || drag == Drag.FILL || drag == Drag.MOVE || drag == Drag.COLUMN_SELECT || drag == Drag.ROW_SELECT || drag == Drag.POINT) {
            if (outside && !autoScroll.isRunning()) autoScroll.start();
            if (!outside) autoScroll.stop();
        }
        switch (drag) {
            case SELECT -> {
                CellAddress focus = new CellAddress(Math.max(0, geo.rowAtClamped(p.y)), Math.max(0, geo.columnAtClamped(p.x)));
                if (focus.equals(dragFocus)) return;
                dragFocus = focus;
                SheetSelection sel = editor.getSession().getSelection();
                CellRange r = expandMerges(CellRange.of(dragAnchor, focus));
                editor.select(new SheetSelection(sel.active(), dragAnchor, replaceLast(sel, r)));
            }
            case POINT -> {
                CellAddress focus = new CellAddress(Math.max(0, geo.rowAtClamped(p.y)), Math.max(0, geo.columnAtClamped(p.x)));
                editor.insertReference(expandMerges(CellRange.of(dragAnchor, focus)), false);
            }
            case COLUMN_SELECT -> {
                int c = Math.max(0, geo.columnAtClamped(p.x));
                SheetSelection sel = editor.getSession().getSelection();
                editor.select(new SheetSelection(sel.active(), sel.anchor(), replaceLast(sel, CellRange.columns(Math.min(dragAnchor.column(), c), Math.max(dragAnchor.column(), c)))));
            }
            case ROW_SELECT -> {
                int r = Math.max(0, geo.rowAtClamped(p.y));
                SheetSelection sel = editor.getSession().getSelection();
                editor.select(new SheetSelection(sel.active(), sel.anchor(), replaceLast(sel, CellRange.rows(Math.min(dragAnchor.row(), r), Math.max(dragAnchor.row(), r)))));
            }
            case RESIZE_COLUMN -> { editor.previewColumnWidth(resizeIndex, Math.max(0, resizeStart + (int) Math.round((p.x - resizeOrigin) / geo.zoom()))); invalidateGeometry(); repaint(); }
            case RESIZE_ROW -> { editor.previewRowHeight(resizeIndex, Math.max(0, resizeStart + (int) Math.round((p.y - resizeOrigin) / geo.zoom()))); invalidateGeometry(); repaint(); }
            case FILL -> {
                CellRange src = editor.getSession().getSelection().range();
                int row = Math.max(0, geo.rowAtClamped(p.y)), col = Math.max(0, geo.columnAtClamped(p.x));
                int dRow = row > src.lastRow() ? row - src.lastRow() : row < src.firstRow() ? row - src.firstRow() : 0;
                int dCol = col > src.lastColumn() ? col - src.lastColumn() : col < src.firstColumn() ? col - src.firstColumn() : 0;
                if (Math.abs(dRow) >= Math.abs(dCol)) fillPreview = dRow >= 0 ? new CellRange(src.firstRow(), src.firstColumn(), Math.max(src.lastRow(), row), src.lastColumn()) : new CellRange(row, src.firstColumn(), src.lastRow(), src.lastColumn());
                else fillPreview = dCol >= 0 ? new CellRange(src.firstRow(), src.firstColumn(), src.lastRow(), Math.max(src.lastColumn(), col)) : new CellRange(src.firstRow(), col, src.lastRow(), src.lastColumn());
                repaint();
            }
            case MOVE -> {
                CellRange src = editor.getSession().getSelection().range();
                int row = Math.max(0, geo.rowAtClamped(p.y)), col = Math.max(0, geo.columnAtClamped(p.x));
                int dr = row - dragAnchor.row(), dc = col - dragAnchor.column();
                int nr = Math.max(0, Math.min(CellAddress.MAX_ROWS - src.rowCount(), src.firstRow() + dr)), nc = Math.max(0, Math.min(CellAddress.MAX_COLUMNS - src.columnCount(), src.firstColumn() + dc));
                movePreview = new CellRange(nr, nc, nr + src.rowCount() - 1, nc + src.columnCount() - 1);
                dragCopy = e.isControlDown();
                repaint();
            }
            case OBJECT_MOVE -> {
                SheetObject o = findObject(selectedObject);
                if (o == null) return;
                Rectangle r = new Rectangle(objectDragStart);
                r.translate(p.x - pressPoint.x, p.y - pressPoint.y);
                editor.previewObject(selectedObject, geo.anchorAt(r.x, r.y, r.width, r.height));
            }
            case OBJECT_RESIZE -> {
                Rectangle r = new Rectangle(objectDragStart);
                int dx = p.x - pressPoint.x, dy = p.y - pressPoint.y;
                int h = resizeHandle;
                if (h == 0 || h == 3 || h == 5) { r.x += dx; r.width -= dx; }
                if (h == 2 || h == 4 || h == 7) r.width += dx;
                if (h == 0 || h == 1 || h == 2) { r.y += dy; r.height -= dy; }
                if (h == 5 || h == 6 || h == 7) r.height += dy;
                if (r.width < 16 || r.height < 16) return;
                editor.previewObject(selectedObject, geo.anchorAt(r.x, r.y, r.width, r.height));
            }
            default -> { }
        }
    }

    private void autoScrollStep() {
        if (lastMouse == null || drag == Drag.NONE) { autoScroll.stop(); return; }
        SheetGeometry geo = geometry();
        long dx = 0, dy = 0;
        if (lastMouse.x > getWidth()) dx = (long) (geo.sheet().columns().defaultSize());
        else if (lastMouse.x < geo.bodyX() && geo.scrollX() > 0) dx = -(long) (geo.sheet().columns().defaultSize());
        if (lastMouse.y > getHeight()) dy = (long) (geo.sheet().rows().defaultSize() * 2);
        else if (lastMouse.y < geo.bodyY() && geo.scrollY() > 0) dy = -(long) (geo.sheet().rows().defaultSize() * 2);
        if (dx == 0 && dy == 0) return;
        setScroll(scrollX + dx, scrollY + dy);
        MouseEvent synthetic = new MouseEvent(this, MouseEvent.MOUSE_DRAGGED, System.currentTimeMillis(), 0, lastMouse.x, lastMouse.y, 1, false);
        dragged(synthetic);
    }

    private void release(MouseEvent e) {
        autoScroll.stop();
        if (e.isPopupTrigger()) { popup(e); drag = Drag.NONE; return; }
        switch (drag) {
            case RESIZE_COLUMN -> editor.commitColumnWidth(columnsForResize(resizeIndex), geometry().sheet().columns().rawSize(resizeIndex), resizeStart, resizeIndex);
            case RESIZE_ROW -> editor.commitRowHeight(rowsForResize(resizeIndex), geometry().sheet().rows().rawSize(resizeIndex), resizeStart, resizeIndex);
            case FILL -> {
                CellRange src = editor.getSession().getSelection().range();
                CellRange target = fillPreview;
                fillPreview = null;
                if (target != null && !target.equals(src)) editor.fill(src, target, e.isControlDown());
                repaint();
            }
            case MOVE -> {
                CellRange target = movePreview;
                movePreview = null;
                CellRange src = editor.getSession().getSelection().range();
                if (target != null && !target.equals(src)) editor.moveRange(src, target.first(), dragCopy);
                repaint();
            }
            case OBJECT_MOVE, OBJECT_RESIZE -> editor.commitObjectPreview(selectedObject);
            case POINT -> editor.pointFinished();
            default -> { }
        }
        drag = Drag.NONE;
    }

    private void clicked(MouseEvent e) {
        if (!SwingUtilities.isLeftMouseButton(e) || e.getClickCount() != 1) return;
        SheetGeometry geo = geometry();
        if (e.getX() < geo.headerWidth() || e.getY() < geo.headerHeight()) return;
        int row = geo.rowAt(e.getY()), col = geo.columnAt(e.getX());
        if (row < 0 || col < 0) return;
        CellAddress a = new CellAddress(row, col);
        if (editor.hasHyperlink(a) && (e.isControlDown() || editor.followLinksOnClick())) editor.openHyperlink(a);
    }

    private void moved(MouseEvent e) {
        SheetGeometry geo = geometry();
        Point p = e.getPoint();
        Cursor cursor = Cursor.getDefaultCursor();
        if (nearColumnBoundary(geo, p.x, p.y)) cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
        else if (nearRowBoundary(geo, p.x, p.y)) cursor = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
        else if (selectedObject != null && findObject(selectedObject) != null && handleAt(geo.objectRect(findObject(selectedObject).anchor()), p) >= 0) {
            int h = handleAt(geo.objectRect(findObject(selectedObject).anchor()), p);
            cursor = Cursor.getPredefinedCursor(switch (h) { case 0 -> Cursor.NW_RESIZE_CURSOR; case 1 -> Cursor.N_RESIZE_CURSOR; case 2 -> Cursor.NE_RESIZE_CURSOR; case 3 -> Cursor.W_RESIZE_CURSOR; case 4 -> Cursor.E_RESIZE_CURSOR; case 5 -> Cursor.SW_RESIZE_CURSOR; case 6 -> Cursor.S_RESIZE_CURSOR; default -> Cursor.SE_RESIZE_CURSOR; });
        } else if (objectAt(p) != null) cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        else if (p.x > geo.headerWidth() && p.y > geo.headerHeight()) {
            SheetSelection sel = editor.getSession().getSelection();
            Rectangle rect = geo.rangeRect(expandMerges(sel.range()));
            if (fillHandleHit(rect, p) && sel.ranges().size() == 1) cursor = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
            else if (onBorder(rect, p) && sel.ranges().size() == 1) cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            else {
                int row = geo.rowAt(p.y), col = geo.columnAt(p.x);
                if (row >= 0 && col >= 0 && editor.hasHyperlink(new CellAddress(row, col))) cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
            }
        }
        if (getCursor() != cursor) setCursor(cursor);
    }

    private void wheel(MouseWheelEvent e) {
        if (e.isControlDown()) {
            double z = editor.effectiveZoom() * (e.getWheelRotation() < 0 ? 1.1 : 1 / 1.1);
            editor.setZoom(Math.max(0.1, Math.min(4, Math.round(z * 100) / 100.0)));
            return;
        }
        SheetGeometry geo = geometry();
        double amount = e.getPreciseWheelRotation() * 3;
        if (e.isShiftDown()) setScroll(scrollX + (long) (amount * geo.sheet().columns().defaultSize()), scrollY);
        else setScroll(scrollX, scrollY + (long) (amount * geo.sheet().rows().defaultSize()));
    }

    private void popup(MouseEvent e) {
        SheetGeometry geo = geometry();
        Point p = e.getPoint();
        int sheet = editor.getSession().getActiveSheetIndex();
        SheetObject o = objectAt(p);
        if (o != null) {
            selectedObject = o.id();
            editor.objectSelected(o);
            repaint();
            editor.showContextMenu(new SheetContextMenuContext(SheetContextMenuContext.Target.OBJECT, sheet, null, -1, o), this, p.x, p.y);
            return;
        }
        if (p.y < geo.headerHeight() && p.x >= geo.headerWidth()) {
            int c = geo.columnAt(p.x);
            SheetSelection sel = editor.getSession().getSelection();
            boolean inside = sel.ranges().stream().anyMatch(r -> r.isWholeColumn() && c >= r.firstColumn() && c <= r.lastColumn());
            if (!inside) editor.select(new SheetSelection(new CellAddress(geo.firstScrollRow(), c), new CellAddress(0, c), List.of(CellRange.columns(c, c))));
            editor.showContextMenu(new SheetContextMenuContext(SheetContextMenuContext.Target.COLUMN_HEADER, sheet, null, c, null), this, p.x, p.y);
            return;
        }
        if (p.x < geo.headerWidth() && p.y >= geo.headerHeight()) {
            int r = geo.rowAt(p.y);
            SheetSelection sel = editor.getSession().getSelection();
            boolean inside = sel.ranges().stream().anyMatch(x -> x.isWholeRow() && r >= x.firstRow() && r <= x.lastRow());
            if (!inside) editor.select(new SheetSelection(new CellAddress(r, geo.firstScrollColumn()), new CellAddress(r, 0), List.of(CellRange.rows(r, r))));
            editor.showContextMenu(new SheetContextMenuContext(SheetContextMenuContext.Target.ROW_HEADER, sheet, null, r, null), this, p.x, p.y);
            return;
        }
        int row = geo.rowAt(p.y), col = geo.columnAt(p.x);
        if (row < 0 || col < 0) return;
        CellAddress a = new CellAddress(row, col);
        editor.commitEditingIfActive();
        if (!editor.getSession().getSelection().contains(row, col)) editor.select(SheetSelection.of(mergeAt(a).first()));
        editor.showContextMenu(new SheetContextMenuContext(SheetContextMenuContext.Target.CELL, sheet, a, -1, null), this, p.x, p.y);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        SheetGeometry geo = geometry();
        if (e.getX() < geo.headerWidth() || e.getY() < geo.headerHeight()) return null;
        int row = geo.rowAt(e.getY()), col = geo.columnAt(e.getX());
        if (row < 0 || col < 0) return null;
        return editor.tooltipFor(new CellAddress(row, col));
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);
        if (e.isConsumed()) return;
        if (e.getID() == KeyEvent.KEY_TYPED) {
            char c = e.getKeyChar();
            if (c >= ' ' && c != 127 && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown()) {
                e.consume();
                editor.startEditing(String.valueOf(c), true);
            }
        }
    }

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) accessibleContext = new AccessibleJComponent() {
            @Override public AccessibleRole getAccessibleRole() { return AccessibleRole.TABLE; }
            @Override public String getAccessibleName() { return "Planilha " + editor.getSession().getActiveSheet().name(); }
            @Override public String getAccessibleDescription() {
                CellAddress a = editor.getSession().getSelection().active();
                return a.toA1() + ": " + editor.displayText(a);
            }
        };
        return accessibleContext;
    }

    public void dispose() { marquee.stop(); autoScroll.stop(); imageCache.clear(); }
}
