package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.format.FormattedValue;
import dtm.stools.component.panels.editor.sheet.format.NumberFormatter;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetBorder;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.UnderlineStyle;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SheetHtmlExporter {
    public String html(SheetWorkbook wb, CalcEngine engine, NumberFormatter formatter, int sheet, CellRange range, boolean fragment) {
        StringBuilder b = new StringBuilder();
        if (!fragment) b.append("<!DOCTYPE html><html><head><meta charset=\"utf-8\"><title>").append(escape(wb.properties().title().isEmpty() ? "Planilha" : wb.properties().title()))
                .append("</title><style>body{font-family:Calibri,Arial,sans-serif}table{border-collapse:collapse;margin:8px 0 24px}td{padding:1px 4px;vertical-align:bottom;white-space:nowrap}h2{font-size:14px}</style></head><body>");
        int from = sheet < 0 ? 0 : sheet, to = sheet < 0 ? wb.sheetCount() - 1 : sheet;
        for (int s = from; s <= to; s++) {
            SheetWorksheet ws = wb.sheet(s);
            if (!fragment && sheet < 0) b.append("<h2>").append(escape(ws.name())).append("</h2>");
            CellRange area = range != null ? range : engine != null ? engine.usedRange(s) : ws.usedRange();
            if (area == null) continue;
            if (!fragment || range == null) area = new CellRange(Math.min(area.firstRow(), range == null ? 0 : area.firstRow()), range == null ? 0 : area.firstColumn(), area.lastRow(), area.lastColumn());
            Map<Long, CellRange> merges = new HashMap<>();
            Set<Long> covered = new HashSet<>();
            for (CellRange m : ws.properties().merges()) { merges.put(m.first().key(), m); for (CellAddress a : m) if (!a.equals(m.first())) covered.add(a.key()); }
            b.append("<table>");
            for (int r = area.firstRow(); r <= area.lastRow(); r++) {
                if (ws.rows().isHidden(r)) continue;
                b.append("<tr style=\"height:").append(ws.rows().size(r)).append("px\">");
                for (int c = area.firstColumn(); c <= area.lastColumn(); c++) {
                    if (ws.columns().isHidden(c)) continue;
                    long key = CellAddress.key(r, c);
                    if (covered.contains(key)) continue;
                    CellStyle st = wb.style(ws.cell(r, c).style());
                    CellValue v = engine != null ? engine.valueAt(s, r, c) : ws.cell(r, c).value();
                    FormattedValue f = formatter.format(v, st.numberFormat());
                    CellRange m = merges.get(key);
                    b.append("<td");
                    if (m != null) b.append(" colspan=\"").append(m.columnCount()).append("\" rowspan=\"").append(m.rowCount()).append("\"");
                    b.append(" style=\"").append(css(st, v, f, ws.columns().size(c))).append("\">").append(escape(f.text()).replace("\n", "<br>")).append("</td>");
                }
                b.append("</tr>");
            }
            b.append("</table>");
        }
        if (!fragment) b.append("</body></html>");
        return b.toString();
    }

    public void write(SheetWorkbook wb, CalcEngine engine, NumberFormatter formatter, OutputStream out) throws IOException {
        out.write(html(wb, engine, formatter, -1, null, false).getBytes(StandardCharsets.UTF_8));
    }

    static String css(CellStyle s, CellValue v, FormattedValue f, int width) {
        StringBuilder b = new StringBuilder("min-width:").append(width).append("px;");
        b.append("font-family:'").append(s.fontFamily().replace("'", "")).append("';font-size:").append(XlsxStyles.trim(s.fontSize())).append("pt;");
        if (s.bold()) b.append("font-weight:bold;");
        if (s.italic()) b.append("font-style:italic;");
        if (s.underline() != UnderlineStyle.NONE || s.strikethrough()) b.append("text-decoration:").append(s.underline() != UnderlineStyle.NONE ? "underline " : "").append(s.strikethrough() ? "line-through" : "").append(';');
        Integer color = f.color() != null ? f.color() : s.fontColor();
        if (color != null) b.append("color:#").append(XlsxColors.hex(color).substring(2)).append(';');
        if (s.fill().primaryColor() != null) b.append("background:#").append(XlsxColors.hex(s.fill().primaryColor()).substring(2)).append(';');
        HorizontalAlignment h = s.horizontal();
        String align = switch (h) { case CENTER, CENTER_ACROSS -> "center"; case RIGHT -> "right"; case LEFT -> "left"; case JUSTIFY -> "justify"; default -> v instanceof NumberValue ? "right" : "left"; };
        b.append("text-align:").append(align).append(';');
        if (s.wrap()) b.append("white-space:normal;");
        border(b, "top", s.top()); border(b, "right", s.right()); border(b, "bottom", s.bottom()); border(b, "left", s.left());
        return b.toString();
    }

    private static void border(StringBuilder b, String side, SheetBorder border) {
        if (!border.visible()) return;
        String style = switch (border.style()) { case DOTTED, HAIR -> "dotted"; case DASHED, MEDIUM_DASHED, DASH_DOT, DASH_DOT_DOT, MEDIUM_DASH_DOT, MEDIUM_DASH_DOT_DOT, SLANT_DASH_DOT -> "dashed"; case DOUBLE -> "double"; default -> "solid"; };
        b.append("border-").append(side).append(':').append(Math.max(1, (int) Math.ceil(border.style().width()))).append("px ").append(style).append(" #")
                .append(border.color() == null ? "000000" : XlsxColors.hex(border.color()).substring(2)).append(';');
    }

    static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
}
