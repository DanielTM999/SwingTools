package dtm.stools.component.panels.editor.sheet.print;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.format.FormattedValue;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.PrintSettings;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetImage;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.VerticalAlignment;
import dtm.stools.component.panels.editor.sheet.render.ChartData;
import dtm.stools.component.panels.editor.sheet.render.SheetPalette;
import dtm.stools.component.panels.editor.sheet.render.SheetRenderer;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;

public final class SheetPageRenderer {
    private final SheetWorkbook workbook;
    private final CalcEngine engine;
    private final NumberFormatter formatter;
    private final SheetRenderer renderer;
    private final Function<SheetChart, ChartData> charts;
    private final String fileName;

    public SheetPageRenderer(SheetWorkbook workbook, CalcEngine engine, NumberFormatter formatter, SheetRenderer renderer, Function<SheetChart, ChartData> charts, String fileName) {
        this.workbook = workbook;
        this.engine = engine;
        this.formatter = formatter;
        this.renderer = renderer == null ? new SheetRenderer() : renderer;
        this.charts = charts;
        this.fileName = fileName == null ? "" : fileName;
    }

    public void paint(Graphics2D g, SheetPageLayout layout, SheetPage page, int totalPages) {
        SheetWorksheet ws = workbook.sheet(page.sheet());
        PrintSettings ps = layout.settings();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, (int) Math.ceil(layout.pageWidth()), (int) Math.ceil(layout.pageHeight()));
        paintHeaderFooter(g, layout, page, totalPages, ws.name());
        double left = ps.marginLeft() * 96, top = ps.marginTop() * 96;
        double contentW = widthOf(ws, page), contentH = heightOf(ws, page);
        double availW = layout.pageWidth() - (ps.marginLeft() + ps.marginRight()) * 96, availH = layout.pageHeight() - (ps.marginTop() + ps.marginBottom()) * 96;
        if (ps.centerHorizontally()) left += Math.max(0, (availW - contentW * layout.scale()) / 2);
        if (ps.centerVertically()) top += Math.max(0, (availH - contentH * layout.scale()) / 2);
        Graphics2D c = (Graphics2D) g.create();
        try {
            c.translate(left, top);
            c.scale(layout.scale(), layout.scale());
            int x0 = 0, y0 = 0;
            int titleW = page.repeatColumns() == null ? 0 : (int) SheetPagination.span(ws.columns(), page.repeatColumns().firstColumn(), page.repeatColumns().lastColumn());
            int titleH = page.repeatRows() == null ? 0 : (int) SheetPagination.span(ws.rows(), page.repeatRows().firstRow(), page.repeatRows().lastRow());
            if (page.repeatRows() != null && page.repeatColumns() != null)
                paintBlock(c, ws, page.sheet(), new CellRange(page.repeatRows().firstRow(), page.repeatColumns().firstColumn(), page.repeatRows().lastRow(), page.repeatColumns().lastColumn()), x0, y0, ps);
            if (page.repeatRows() != null) paintBlock(c, ws, page.sheet(), page.repeatRows(), x0 + titleW, y0, ps);
            if (page.repeatColumns() != null) paintBlock(c, ws, page.sheet(), page.repeatColumns(), x0, y0 + titleH, ps);
            paintBlock(c, ws, page.sheet(), page.cells(), x0 + titleW, y0 + titleH, ps);
            paintObjects(c, ws, page.cells(), x0 + titleW, y0 + titleH);
        } finally {
            c.dispose();
        }
    }

    private static double widthOf(SheetWorksheet ws, SheetPage p) {
        double w = SheetPagination.span(ws.columns(), p.cells().firstColumn(), p.cells().lastColumn());
        if (p.repeatColumns() != null) w += SheetPagination.span(ws.columns(), p.repeatColumns().firstColumn(), p.repeatColumns().lastColumn());
        return w;
    }

    private static double heightOf(SheetWorksheet ws, SheetPage p) {
        double h = SheetPagination.span(ws.rows(), p.cells().firstRow(), p.cells().lastRow());
        if (p.repeatRows() != null) h += SheetPagination.span(ws.rows(), p.repeatRows().firstRow(), p.repeatRows().lastRow());
        return h;
    }

    private void paintBlock(Graphics2D g, SheetWorksheet ws, int sheet, CellRange r, int ox, int oy, PrintSettings ps) {
        long baseX = ws.columns().position(r.firstColumn()), baseY = ws.rows().position(r.firstRow());
        java.util.function.IntUnaryOperator cx = col -> ox + (int) (ws.columns().position(col) - baseX);
        java.util.function.IntUnaryOperator cy = row -> oy + (int) (ws.rows().position(row) - baseY);
        Shape clip = g.getClip();
        int bw = (int) (ws.columns().position(r.lastColumn() + 1) - baseX), bh = (int) (ws.rows().position(r.lastRow() + 1) - baseY);
        g.clipRect(ox, oy, bw + 1, bh + 1);
        if (ps.gridlines()) {
            g.setColor(new Color(0xD0D0D0));
            g.setStroke(new BasicStroke(0.5f));
            for (int row = r.firstRow(); row <= r.lastRow() + 1; row++) g.drawLine(ox, cy.applyAsInt(row), ox + bw, cy.applyAsInt(row));
            for (int col = r.firstColumn(); col <= r.lastColumn() + 1; col++) g.drawLine(cx.applyAsInt(col), oy, cx.applyAsInt(col), oy + bh);
        }
        List<CellRange> merges = ws.properties().merges();
        for (int row = r.firstRow(); row <= r.lastRow(); row++) {
            if (ws.rows().size(row) == 0) continue;
            for (int col = r.firstColumn(); col <= r.lastColumn(); col++) {
                if (ws.columns().size(col) == 0) continue;
                CellRange merge = null;
                for (CellRange m : merges) if (m.contains(row, col)) { merge = m; break; }
                if (merge != null && (merge.firstRow() != row && row != r.firstRow() || merge.firstColumn() != col && col != r.firstColumn())) continue;
                CellRange area = merge == null ? CellRange.of(row, col) : merge;
                Rectangle rect = new Rectangle(cx.applyAsInt(area.firstColumn()), cy.applyAsInt(area.firstRow()),
                        cx.applyAsInt(area.lastColumn() + 1) - cx.applyAsInt(area.firstColumn()), cy.applyAsInt(area.lastRow() + 1) - cy.applyAsInt(area.firstRow()));
                SheetCell cell = ws.cell(area.first());
                CellStyle style = workbook.style(cell.style() != 0 ? cell.style() : columnOrRowStyle(ws, row, col));
                if (style.fill().visible()) renderer.paintFill(g, style.fill(), rect);
                CellValue v = engine == null ? cell.value() : engine.valueAt(sheet, area.first());
                if (!v.isEmpty()) paintText(g, style, v, rect, r, ws, area);
                if (style.hasBorders()) renderer.paintBorders(g, style, rect);
            }
        }
        g.setClip(clip);
    }

    private static int columnOrRowStyle(SheetWorksheet ws, int row, int col) {
        int s = ws.rows().style(row);
        return s != 0 ? s : ws.columns().style(col);
    }

    private void paintText(Graphics2D g, CellStyle style, CellValue v, Rectangle rect, CellRange block, SheetWorksheet ws, CellRange area) {
        if (v instanceof dtm.stools.component.panels.editor.sheet.model.ArrayValue a) v = a.get(0, 0);
        FormattedValue f = formatter.format(v, style.numberFormat());
        String text = f.text();
        if (text.isEmpty()) return;
        Font font = renderer.font(style, 1);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        Color color = f.color() != null ? SheetPalette.color(f.color()) : style.fontColor() != null ? SheetPalette.color(style.fontColor()) : Color.BLACK;
        g.setColor(color);
        HorizontalAlignment h = style.horizontal();
        if (h == HorizontalAlignment.GENERAL) h = v instanceof NumberValue ? HorizontalAlignment.RIGHT : v instanceof BoolValue || v instanceof ErrorValue ? HorizontalAlignment.CENTER : HorizontalAlignment.LEFT;
        int pad = 3 + style.indent() * 9;
        List<String> lines = style.wrap() ? SheetRenderer.wrap(text, fm, Math.max(4, rect.width - 6)) : List.of(text.replace('\n', ' '));
        int lineH = fm.getHeight();
        int total = lineH * lines.size();
        int y;
        VerticalAlignment va = style.vertical();
        if (va == VerticalAlignment.TOP) y = rect.y + fm.getAscent() + 1;
        else if (va == VerticalAlignment.CENTER) y = rect.y + (rect.height - total) / 2 + fm.getAscent();
        else y = rect.y + rect.height - total + fm.getAscent() - fm.getDescent() + 1;
        Shape clip = g.getClip();
        Rectangle textClip = new Rectangle(rect);
        if (!style.wrap() && !(v instanceof NumberValue) && h == HorizontalAlignment.LEFT) {
            int col = area.lastColumn() + 1;
            int extra = 0;
            while (col <= block.lastColumn() && ws.cell(area.firstRow(), col).value().isEmpty() && !ws.cell(area.firstRow(), col).hasFormula() && extra < 4000) { extra += ws.columns().size(col); col++; }
            textClip.width += extra;
        }
        g.clip(textClip);
        for (String line : lines) {
            int w = fm.stringWidth(line);
            int x = switch (h) {
                case RIGHT -> rect.x + rect.width - w - pad;
                case CENTER, CENTER_ACROSS -> rect.x + (rect.width - w) / 2;
                default -> rect.x + pad;
            };
            if (v instanceof NumberValue && w > rect.width - 4 && !style.wrap()) { line = "#".repeat(Math.max(1, (rect.width - 4) / Math.max(1, fm.charWidth('#')))); x = rect.x + 2; }
            g.drawString(line, x, y);
            y += lineH;
        }
        g.setClip(clip);
    }

    private void paintObjects(Graphics2D g, SheetWorksheet ws, CellRange cells, int ox, int oy) {
        long baseX = ws.columns().position(cells.firstColumn()), baseY = ws.rows().position(cells.firstRow());
        Shape clip = g.getClip();
        g.clipRect(ox, oy, (int) (ws.columns().position(cells.lastColumn() + 1) - baseX), (int) (ws.rows().position(cells.lastRow() + 1) - baseY));
        for (SheetObject o : ws.properties().objects()) {
            ObjectAnchor a = o.anchor();
            Rectangle r = new Rectangle(ox + (int) (ws.columns().position(a.column()) - baseX) + a.offsetX(), oy + (int) (ws.rows().position(a.row()) - baseY) + a.offsetY(), a.width(), a.height());
            switch (o) {
                case SheetChart chart -> { if (charts != null) renderer.charts().paint(g, chart, charts.apply(chart), r, false); }
                case SheetImage img -> {
                    try {
                        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(img.data()));
                        if (bi != null) g.drawImage(bi, r.x, r.y, r.width, r.height, null);
                    } catch (Exception ignored) { }
                }
                default -> { }
            }
        }
        g.setClip(clip);
    }

    private void paintHeaderFooter(Graphics2D g, SheetPageLayout layout, SheetPage page, int total, String sheetName) {
        PrintSettings ps = layout.settings();
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        g.setColor(Color.BLACK);
        FontMetrics fm = g.getFontMetrics();
        double left = ps.marginLeft() * 96, right = layout.pageWidth() - ps.marginRight() * 96;
        int headerY = (int) (ps.marginHeader() * 96 + fm.getAscent());
        int footerY = (int) (layout.pageHeight() - ps.marginFooter() * 96 - fm.getDescent());
        String[][] parts = {{ps.headerLeft(), ps.headerCenter(), ps.headerRight()}, {ps.footerLeft(), ps.footerCenter(), ps.footerRight()}};
        for (int k = 0; k < 2; k++) {
            int y = k == 0 ? headerY : footerY;
            for (int i = 0; i < 3; i++) {
                String t = expand(parts[k][i], page.number(), total, sheetName);
                if (t.isEmpty()) continue;
                int w = fm.stringWidth(t);
                int x = i == 0 ? (int) left : i == 1 ? (int) ((left + right - w) / 2) : (int) (right - w);
                g.drawString(t, x, y);
            }
        }
    }

    private String expand(String code, int page, int total, String sheetName) {
        if (code == null || code.isEmpty()) return "";
        return code.replace("&P", String.valueOf(page)).replace("&N", String.valueOf(total)).replace("&D", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .replace("&T", LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))).replace("&A", sheetName).replace("&F", fileName).replace("&&", "&");
    }
}
