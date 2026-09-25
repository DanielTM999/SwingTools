package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetPackage;
import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetXml;
import dtm.stools.component.panels.editor.sheet.io.ooxml.XmlBuilder;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellStyle;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.HorizontalAlignment;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetFill;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.model.UnderlineStyle;
import org.w3c.dom.Element;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OdsCodec {
    private static final String OFFICE = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
    private static final String TABLE = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    private static final String TEXT = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    private static final String STYLE = "urn:oasis:names:tc:opendocument:xmlns:style:1.0";
    private static final String FO = "urn:oasis:names:tc:opendocument:xmlns:xsl-fo-compatible:1.0";
    private static final String MIME = "application/vnd.oasis.opendocument.spreadsheet";

    public SheetImportResult read(byte[] bytes) throws IOException {
        SheetPackage pkg = SheetPackage.read(bytes, SheetPackage.Limits.DEFAULT);
        byte[] content = pkg.partOrNull("content.xml");
        if (content == null) throw new IOException("O arquivo não é uma planilha OpenDocument.");
        Element root = SheetXml.parse(content).getDocumentElement();
        Map<String, CellStyle> cellStyles = new HashMap<>();
        Map<String, Integer> columnWidths = new HashMap<>(), rowHeights = new HashMap<>();
        Element auto = SheetXml.child(root, "automatic-styles");
        for (Element s : SheetXml.children(auto, "style")) {
            String name = SheetXml.attr(s, "name"), family = SheetXml.attr(s, "family");
            switch (family) {
                case "table-column" -> { String w = SheetXml.attr(SheetXml.child(s, "table-column-properties"), "column-width"); if (!w.isEmpty()) columnWidths.put(name, length(w)); }
                case "table-row" -> { String h = SheetXml.attr(SheetXml.child(s, "table-row-properties"), "row-height"); if (!h.isEmpty()) rowHeights.put(name, length(h)); }
                case "table-cell" -> cellStyles.put(name, cellStyle(s));
                default -> { }
            }
        }
        SheetWorkbook wb = new SheetWorkbook();
        List<String> diagnostics = new ArrayList<>();
        Element spreadsheet = SheetXml.path(root, "body", "spreadsheet");
        for (Element table : SheetXml.children(spreadsheet, "table")) {
            SheetWorksheet ws = new SheetWorksheet(wb.uniqueSheetName(SheetXml.attr(table, "name", "Planilha" + (wb.sheetCount() + 1))));
            wb.addSheet(wb.sheetCount(), ws);
            List<CellRange> merges = new ArrayList<>();
            int col = 0;
            for (Element c : columns(table)) {
                int repeat = Math.min(16384, SheetXml.intAttr(c, "number-columns-repeated", 1));
                Integer w = columnWidths.get(SheetXml.attr(c, "style-name"));
                boolean hidden = "collapse".equals(SheetXml.attr(c, "visibility"));
                if (repeat < 1000) for (int k = 0; k < repeat; k++) { if (w != null) ws.columns().setSize(col + k, w); if (hidden) ws.columns().setHidden(col + k, true); }
                col += repeat;
            }
            int row = 0;
            for (Element r : rows(table)) {
                int repeat = SheetXml.intAttr(r, "number-rows-repeated", 1);
                Integer h = rowHeights.get(SheetXml.attr(r, "style-name"));
                boolean hasContent = false;
                for (Element c : SheetXml.children(r)) if (c.getLocalName().equals("table-cell") && (c.hasChildNodes() || !SheetXml.attr(c, "value-type").isEmpty())) hasContent = true;
                if (!hasContent && repeat > 1) { row += repeat; if (row >= CellAddress.MAX_ROWS) break; continue; }
                for (int rr = 0; rr < repeat && row < CellAddress.MAX_ROWS; rr++, row++) {
                    if (h != null) ws.rows().setSize(row, h);
                    if ("collapse".equals(SheetXml.attr(r, "visibility"))) ws.rows().setHidden(row, true);
                    int c = 0;
                    for (Element cell : SheetXml.children(r)) {
                        String n = cell.getLocalName();
                        if (!n.equals("table-cell") && !n.equals("covered-table-cell")) continue;
                        int crep = SheetXml.intAttr(cell, "number-columns-repeated", 1);
                        if (n.equals("table-cell")) {
                            CellValue v = value(cell, wb.properties().date1904());
                            String f = SheetXml.attr(cell, "formula");
                            CellStyle st = cellStyles.get(SheetXml.attr(cell, "style-name"));
                            int style = st == null ? 0 : wb.styles().intern(st);
                            String type = SheetXml.attr(cell, "value-type");
                            if (type.equals("date") && style == 0) style = wb.styles().intern(CellStyle.DEFAULT.withNumberFormat("dd/mm/yyyy"));
                            if (type.equals("percentage") && style == 0) style = wb.styles().intern(CellStyle.DEFAULT.withNumberFormat("0.00%"));
                            if (type.equals("time") && style == 0) style = wb.styles().intern(CellStyle.DEFAULT.withNumberFormat("hh:mm:ss"));
                            if (type.equals("currency") && style == 0) style = wb.styles().intern(CellStyle.DEFAULT.withNumberFormat("\"R$\" #,##0.00"));
                            String formula = f.isEmpty() ? null : OdsFormulaTranslator.toCanonical(f);
                            if (crep > 256 && v.isEmpty() && formula == null) { c += crep; continue; }
                            for (int k = 0; k < crep && c + k < CellAddress.MAX_COLUMNS; k++) {
                                if (!v.isEmpty() || formula != null || style != 0) ws.put(row, c + k, new SheetCell(v, formula, style));
                            }
                            int cs = SheetXml.intAttr(cell, "number-columns-spanned", 1), rs = SheetXml.intAttr(cell, "number-rows-spanned", 1);
                            if (cs > 1 || rs > 1) merges.add(new CellRange(row, c, row + rs - 1, c + cs - 1));
                        }
                        c += crep;
                    }
                }
            }
            ws.setProperties(ws.properties().withMerges(merges));
        }
        if (wb.sheetCount() == 0) wb.addSheet(0, new SheetWorksheet("Planilha1"));
        return new SheetImportResult(wb, diagnostics, true, bytes, "ods");
    }

    private static List<Element> columns(Element table) {
        List<Element> out = new ArrayList<>();
        for (Element e : SheetXml.children(table)) {
            if (e.getLocalName().equals("table-column")) out.add(e);
            else if (e.getLocalName().equals("table-columns") || e.getLocalName().equals("table-header-columns") || e.getLocalName().equals("table-column-group")) out.addAll(columns(e));
        }
        return out;
    }

    private static List<Element> rows(Element table) {
        List<Element> out = new ArrayList<>();
        for (Element e : SheetXml.children(table)) {
            if (e.getLocalName().equals("table-row")) out.add(e);
            else if (e.getLocalName().equals("table-rows") || e.getLocalName().equals("table-header-rows") || e.getLocalName().equals("table-row-group")) out.addAll(rows(e));
        }
        return out;
    }

    private static CellValue value(Element cell, boolean date1904) {
        String type = SheetXml.attr(cell, "value-type");
        try {
            return switch (type) {
                case "float", "percentage", "currency" -> CellValue.of(Double.parseDouble(SheetXml.attr(cell, "value")));
                case "boolean" -> CellValue.of(Boolean.parseBoolean(SheetXml.attr(cell, "boolean-value")));
                case "date" -> {
                    String d = SheetXml.attr(cell, "date-value");
                    yield CellValue.of(d.length() <= 10 ? DateSerial.toSerial(LocalDate.parse(d), date1904) : DateSerial.toSerial(LocalDateTime.parse(d), date1904));
                }
                case "time" -> CellValue.of(Duration.parse(SheetXml.attr(cell, "time-value")).toMillis() / 86_400_000.0);
                case "string" -> CellValue.of(text(cell));
                default -> {
                    String t = text(cell);
                    yield t.isEmpty() ? CellValue.EMPTY : CellValue.of(t);
                }
            };
        } catch (RuntimeException e) {
            return CellValue.of(text(cell));
        }
    }

    private static String text(Element cell) {
        StringBuilder b = new StringBuilder();
        for (Element p : SheetXml.children(cell, "p")) {
            if (b.length() > 0) b.append('\n');
            appendText(p, b);
        }
        return b.toString();
    }

    private static void appendText(org.w3c.dom.Node n, StringBuilder b) {
        for (org.w3c.dom.Node c = n.getFirstChild(); c != null; c = c.getNextSibling()) {
            if (c.getNodeType() == org.w3c.dom.Node.TEXT_NODE) b.append(c.getNodeValue());
            else if (c instanceof Element e) {
                switch (e.getLocalName()) {
                    case "s" -> b.append(" ".repeat(Math.max(1, SheetXml.intAttr(e, "c", 1))));
                    case "tab" -> b.append('\t');
                    case "line-break" -> b.append('\n');
                    default -> appendText(e, b);
                }
            }
        }
    }

    private static CellStyle cellStyle(Element s) {
        Element text = SheetXml.child(s, "text-properties"), cell = SheetXml.child(s, "table-cell-properties"), para = SheetXml.child(s, "paragraph-properties");
        CellStyle.CellStyleBuilder b = CellStyle.DEFAULT.toBuilder();
        if (text != null) {
            if ("bold".equals(SheetXml.attr(text, "font-weight"))) b.bold(true);
            if ("italic".equals(SheetXml.attr(text, "font-style"))) b.italic(true);
            String u = SheetXml.attr(text, "text-underline-style");
            if (!u.isEmpty() && !u.equals("none")) b.underline(UnderlineStyle.SINGLE);
            Integer color = hex(SheetXml.attr(text, "color"));
            if (color != null) b.fontColor(color);
            String size = SheetXml.attr(text, "font-size");
            if (size.endsWith("pt")) try { b.fontSize(Double.parseDouble(size.substring(0, size.length() - 2))); } catch (NumberFormatException ignored) { }
            String font = SheetXml.attr(text, "font-name");
            if (!font.isEmpty()) b.fontFamily(font);
        }
        if (cell != null) {
            Integer bg = hex(SheetXml.attr(cell, "background-color"));
            if (bg != null) b.fill(SheetFill.solid(bg));
            if ("wrap".equals(SheetXml.attr(cell, "wrap-option"))) b.wrap(true);
        }
        if (para != null) {
            String align = SheetXml.attr(para, "text-align");
            b.horizontal(switch (align) { case "center" -> HorizontalAlignment.CENTER; case "end", "right" -> HorizontalAlignment.RIGHT; case "start", "left" -> HorizontalAlignment.LEFT; case "justify" -> HorizontalAlignment.JUSTIFY; default -> HorizontalAlignment.GENERAL; });
        }
        return b.build();
    }

    private static Integer hex(String s) {
        if (s == null || !s.startsWith("#") || s.length() != 7) return null;
        try { return 0xFF000000 | Integer.parseInt(s.substring(1), 16); } catch (NumberFormatException e) { return null; }
    }

    private static int length(String v) {
        try {
            double n = Double.parseDouble(v.replaceAll("[a-z]+$", ""));
            if (v.endsWith("cm")) return (int) Math.round(n / 2.54 * 96);
            if (v.endsWith("mm")) return (int) Math.round(n / 25.4 * 96);
            if (v.endsWith("in")) return (int) Math.round(n * 96);
            if (v.endsWith("pt")) return (int) Math.round(n / 0.75);
            return (int) Math.round(n);
        } catch (NumberFormatException e) { return 64; }
    }

    public void write(SheetWorkbook wb, CalcEngine engine, OutputStream out) throws IOException {
        Map<String, byte[]> parts = new LinkedHashMap<>();
        parts.put("mimetype", MIME.getBytes(StandardCharsets.US_ASCII));
        Map<Integer, String> styleNames = new LinkedHashMap<>();
        XmlBuilder x = new XmlBuilder();
        x.open("office:document-content", "xmlns:office", OFFICE, "xmlns:table", TABLE, "xmlns:text", TEXT, "xmlns:style", STYLE, "xmlns:fo", FO, "office:version", "1.3");
        x.open("office:automatic-styles");
        for (int s = 0; s < wb.sheetCount(); s++) {
            SheetWorksheet ws = wb.sheet(s);
            CellRange used = engine != null ? engine.usedRange(s) : ws.usedRange();
            int lastCol = used == null ? 0 : used.lastColumn();
            for (int c = 0; c <= lastCol; c++) {
                x.open("style:style", "style:name", "co" + s + "_" + c, "style:family", "table-column").empty("style:table-column-properties", "style:column-width", cm(ws.columns().rawSize(c))).close();
            }
        }
        List<CellStyle> styles = wb.styles().all();
        for (int id = 1; id < styles.size(); id++) {
            CellStyle st = styles.get(id);
            String name = "ce" + id;
            styleNames.put(id, name);
            x.open("style:style", "style:name", name, "style:family", "table-cell");
            x.empty("style:table-cell-properties", "fo:background-color", st.fill().primaryColor() == null ? null : "#" + XlsxColors.hex(st.fill().primaryColor()).substring(2), "fo:wrap-option", st.wrap() ? "wrap" : null);
            String align = switch (st.horizontal()) { case CENTER -> "center"; case RIGHT -> "end"; case LEFT -> "start"; case JUSTIFY -> "justify"; default -> null; };
            if (align != null) x.empty("style:paragraph-properties", "fo:text-align", align);
            x.empty("style:text-properties", "fo:font-weight", st.bold() ? "bold" : null, "fo:font-style", st.italic() ? "italic" : null, "style:text-underline-style", st.underline() != UnderlineStyle.NONE ? "solid" : null,
                    "fo:color", st.fontColor() == null ? null : "#" + XlsxColors.hex(st.fontColor()).substring(2), "fo:font-size", XlsxStyles.trim(st.fontSize()) + "pt", "style:font-name", st.fontFamily());
            x.close();
        }
        x.close();
        x.open("office:body").open("office:spreadsheet");
        for (int s = 0; s < wb.sheetCount(); s++) {
            SheetWorksheet ws = wb.sheet(s);
            x.open("table:table", "table:name", ws.name());
            CellRange used = engine != null ? engine.usedRange(s) : ws.usedRange();
            for (CellRange m : ws.properties().merges()) used = used == null ? m : used.union(m);
            int lastCol = used == null ? 0 : used.lastColumn(), lastRow = used == null ? 0 : used.lastRow();
            for (int c = 0; c <= lastCol; c++) x.empty("table:table-column", "table:style-name", "co" + s + "_" + c, "table:visibility", ws.columns().isHidden(c) ? "collapse" : null);
            Map<Long, CellRange> merges = new HashMap<>();
            java.util.Set<Long> covered = new java.util.HashSet<>();
            for (CellRange m : ws.properties().merges()) {
                merges.put(m.first().key(), m);
                for (CellAddress a : m) if (!a.equals(m.first())) covered.add(a.key());
            }
            for (int r = 0; r <= lastRow; r++) {
                x.open("table:table-row", "table:visibility", ws.rows().isHidden(r) ? "collapse" : null);
                for (int c = 0; c <= lastCol; c++) {
                    long key = CellAddress.key(r, c);
                    if (covered.contains(key)) { x.empty("table:covered-table-cell"); continue; }
                    SheetCell cell = ws.cell(r, c);
                    CellValue v = engine != null ? engine.valueAt(s, r, c) : cell.value();
                    CellRange merge = merges.get(key);
                    List<String> attrs = new ArrayList<>();
                    if (cell.style() > 0) { attrs.add("table:style-name"); attrs.add(styleNames.get(cell.style())); }
                    if (cell.hasFormula()) { attrs.add("table:formula"); attrs.add(OdsFormulaTranslator.toOdf(cell.formula())); }
                    if (merge != null) { attrs.add("table:number-columns-spanned"); attrs.add(String.valueOf(merge.columnCount())); attrs.add("table:number-rows-spanned"); attrs.add(String.valueOf(merge.rowCount())); }
                    String display = null;
                    switch (v) {
                        case NumberValue n -> {
                            boolean date = new dtm.stools.component.panels.editor.sheet.format.NumberFormatter().isDateFormat(wb.style(cell.style()).numberFormat());
                            if (date && n.value() >= 1) { attrs.add("office:value-type"); attrs.add("date"); attrs.add("office:date-value"); attrs.add(DateSerial.toDateTime(n.value(), wb.properties().date1904()).toString()); }
                            else { attrs.add("office:value-type"); attrs.add("float"); attrs.add("office:value"); attrs.add(XlsxWriter.number(n.value())); }
                            display = n.display();
                        }
                        case BoolValue b -> { attrs.add("office:value-type"); attrs.add("boolean"); attrs.add("office:boolean-value"); attrs.add(String.valueOf(b.value())); display = b.value() ? "TRUE" : "FALSE"; }
                        case TextValue t -> { attrs.add("office:value-type"); attrs.add("string"); display = t.value(); }
                        case ErrorValue e -> { attrs.add("office:value-type"); attrs.add("string"); display = e.error().text(); }
                        default -> { }
                    }
                    x.open("table:table-cell", attrs.toArray(String[]::new));
                    if (display != null) for (String line : display.split("\n", -1)) x.element("text:p", line);
                    x.close();
                }
                x.close();
            }
            x.close();
        }
        x.close().close().close();
        parts.put("content.xml", x.bytes());
        XmlBuilder st = new XmlBuilder();
        st.open("office:document-styles", "xmlns:office", OFFICE, "xmlns:style", STYLE, "office:version", "1.3").close();
        parts.put("styles.xml", st.bytes());
        XmlBuilder meta = new XmlBuilder();
        meta.open("office:document-meta", "xmlns:office", OFFICE, "xmlns:meta", "urn:oasis:names:tc:opendocument:xmlns:meta:1.0", "office:version", "1.3").open("office:meta").element("meta:generator", "SwingTools SheetEditor").close().close();
        parts.put("meta.xml", meta.bytes());
        XmlBuilder manifest = new XmlBuilder();
        manifest.open("manifest:manifest", "xmlns:manifest", "urn:oasis:names:tc:opendocument:xmlns:manifest:1.0", "manifest:version", "1.3");
        manifest.empty("manifest:file-entry", "manifest:full-path", "/", "manifest:version", "1.3", "manifest:media-type", MIME);
        for (String p : new String[]{"content.xml", "styles.xml", "meta.xml"}) manifest.empty("manifest:file-entry", "manifest:full-path", p, "manifest:media-type", "text/xml");
        manifest.close();
        parts.put("META-INF/manifest.xml", manifest.bytes());
        new SheetPackage(parts).write(out);
    }

    private static String cm(int px) { return String.format(java.util.Locale.ROOT, "%.3fcm", px / 96.0 * 2.54); }
}
