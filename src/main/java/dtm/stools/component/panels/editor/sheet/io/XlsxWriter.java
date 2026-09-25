package dtm.stools.component.panels.editor.sheet.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import dtm.stools.component.panels.editor.sheet.calc.CalcEngine;
import dtm.stools.component.panels.editor.sheet.calc.FormulaCell;
import dtm.stools.component.panels.editor.sheet.formula.FormulaPrinter;
import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetPackage;
import dtm.stools.component.panels.editor.sheet.io.ooxml.XmlBuilder;
import dtm.stools.component.panels.editor.sheet.model.ArrayValue;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ConditionalFormat;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRule;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.DefinedName;
import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;
import dtm.stools.component.panels.editor.sheet.model.EmptyValue;
import dtm.stools.component.panels.editor.sheet.model.ErrorValue;
import dtm.stools.component.panels.editor.sheet.model.FilterCondition;
import dtm.stools.component.panels.editor.sheet.model.FilterCriteria;
import dtm.stools.component.panels.editor.sheet.model.Hyperlink;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.PageOrientation;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.PrintSettings;
import dtm.stools.component.panels.editor.sheet.model.Scenario;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import dtm.stools.component.panels.editor.sheet.model.SheetImage;
import dtm.stools.component.panels.editor.sheet.model.SheetNote;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetProtection;
import dtm.stools.component.panels.editor.sheet.model.SheetShape;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetVisibility;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.Sparkline;
import dtm.stools.component.panels.editor.sheet.model.TableColumn;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.model.Threshold;
import dtm.stools.component.panels.editor.sheet.model.TotalsFunction;
import dtm.stools.component.panels.editor.sheet.store.AxisIndex;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

public final class XlsxWriter {
    static final String NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    static final String R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    static final String REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships/";
    static final int EMU = 9525;

    private final SheetWorkbook workbook;
    private final CalcEngine engine;
    private final Map<String, byte[]> parts = new LinkedHashMap<>();
    private final List<String[]> overrides = new ArrayList<>();
    private final Map<String, Integer> strings = new LinkedHashMap<>();
    private final XlsxStyles.WriteResult styles = new XlsxStyles.WriteResult();
    private final List<DifferentialStyle> dxfs = new ArrayList<>();
    private final Map<DifferentialStyle, Integer> dxfIds = new HashMap<>();
    private int tableCounter, drawingCounter, chartCounter, imageCounter, commentCounter, stringRefs;
    private boolean dynamicArrays;
    private final List<String> mediaExtensions = new ArrayList<>();

    public XlsxWriter(SheetWorkbook workbook, CalcEngine engine) { this.workbook = workbook; this.engine = engine; }

    public void write(OutputStream out) throws IOException { build().write(out); }

    public SheetPackage build() throws IOException {
        byte[] stylesXml;
        List<byte[]> sheets = new ArrayList<>();
        XlsxStyles.write(workbook.styles(), styles, List.of());
        for (int i = 0; i < workbook.sheetCount(); i++) sheets.add(sheet(i));
        stylesXml = XlsxStyles.write(workbook.styles(), styles, dxfs);
        for (int i = 0; i < sheets.size(); i++) parts.put("xl/worksheets/sheet" + (i + 1) + ".xml", sheets.get(i));
        parts.put("xl/styles.xml", stylesXml);
        parts.put("xl/sharedStrings.xml", sharedStrings());
        parts.put("xl/workbook.xml", workbookXml());
        parts.put("xl/_rels/workbook.xml.rels", workbookRels());
        if (dynamicArrays) parts.put("xl/metadata.xml", metadata());
        parts.put("docProps/core.xml", core());
        parts.put("docProps/app.xml", app());
        parts.put("_rels/.rels", rootRels());
        for (Map.Entry<String, byte[]> e : workbook.properties().preserved().entrySet()) if (!parts.containsKey(e.getKey())) parts.put(e.getKey(), e.getValue());
        Map<String, byte[]> ordered = new LinkedHashMap<>();
        ordered.put("[Content_Types].xml", contentTypes());
        ordered.putAll(parts);
        return new SheetPackage(ordered);
    }

    private int string(String s) {
        stringRefs++;
        return strings.computeIfAbsent(s, k -> strings.size());
    }

    private CellValue cachedValue(int sheet, int row, int col, SheetCell cell) {
        if (engine != null && engine.workbook() == workbook) return engine.valueAt(sheet, row, col);
        return cell.value();
    }

    private byte[] sheet(int index) {
        SheetWorksheet ws = workbook.sheet(index);
        SheetProperties p = ws.properties();
        List<String[]> rels = new ArrayList<>();
        XmlBuilder x = new XmlBuilder();
        x.open("worksheet", "xmlns", NS, "xmlns:r", R, "xmlns:mc", "http://schemas.openxmlformats.org/markup-compatibility/2006",
                "xmlns:x14ac", "http://schemas.microsoft.com/office/spreadsheetml/2009/9/ac", "mc:Ignorable", "x14ac");
        boolean fit = p.print().fitWidth() > 0 || p.print().fitHeight() > 0;
        x.open("sheetPr");
        if (p.tabColor() != null) x.empty("tabColor", "rgb", XlsxColors.hex(p.tabColor()));
        x.empty("outlinePr", "summaryBelow", "1", "summaryRight", "1");
        if (fit) x.empty("pageSetUpPr", "fitToPage", "1");
        x.close();
        CellRange used = ws.usedRange();
        Map<Long, CellValue> spilled = new HashMap<>();
        Map<Long, CellRange> anchors = new HashMap<>();
        if (engine != null && engine.workbook() == workbook) {
            for (CellRange spill : engine.spillRanges(index)) {
                anchors.put(spill.first().key(), spill);
                used = used == null ? spill : used.union(spill);
                for (CellAddress a : spill) if (!a.equals(spill.first())) spilled.put(a.key(), engine.valueAt(index, a));
            }
        }
        x.empty("dimension", "ref", used == null ? "A1" : used.toA1());
        x.open("sheetViews").open("sheetView", "workbookViewId", "0", "tabSelected", index == workbook.activeSheetIndex() ? "1" : null,
                "showGridLines", p.showGridlines() ? null : "0", "showRowColHeaders", p.showHeaders() ? null : "0", "showFormulas", p.showFormulas() ? "1" : null,
                "showZeros", p.showZeros() ? null : "0", "rightToLeft", p.rightToLeft() ? "1" : null, "zoomScale", p.zoom() == 1 ? null : String.valueOf((int) Math.round(p.zoom() * 100)),
                "view", switch (p.viewMode()) { case PAGE_LAYOUT -> "pageLayout"; case PAGE_BREAK_PREVIEW -> "pageBreakPreview"; default -> null; });
        if (p.freeze().active()) {
            CellAddress tl = new CellAddress(p.freeze().rows(), p.freeze().columns());
            String pane = p.freeze().rows() > 0 && p.freeze().columns() > 0 ? "bottomRight" : p.freeze().rows() > 0 ? "bottomLeft" : "topRight";
            x.empty("pane", "xSplit", p.freeze().columns() > 0 ? String.valueOf(p.freeze().columns()) : null, "ySplit", p.freeze().rows() > 0 ? String.valueOf(p.freeze().rows()) : null,
                    "topLeftCell", tl.toA1(), "activePane", pane, "state", "frozen");
        }
        x.close().close();
        AxisIndex rows = ws.rows(), cols = ws.columns();
        int maxOutlineRow = rows.outlineLevels().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int maxOutlineCol = cols.outlineLevels().values().stream().mapToInt(Integer::intValue).max().orElse(0);
        x.empty("sheetFormatPr", "defaultRowHeight", pts(rows.defaultSize()), "baseColWidth", "10", "defaultColWidth", chars(cols.defaultSize()),
                "outlineLevelRow", maxOutlineRow > 0 ? String.valueOf(maxOutlineRow) : null, "outlineLevelCol", maxOutlineCol > 0 ? String.valueOf(maxOutlineCol) : null);
        TreeSet<Integer> colIndexes = new TreeSet<>(cols.customSizes().keySet());
        colIndexes.addAll(cols.hiddenIndexes());
        colIndexes.addAll(cols.outlineLevels().keySet());
        colIndexes.addAll(cols.styles().keySet());
        if (!colIndexes.isEmpty()) {
            x.open("cols");
            for (int c : colIndexes) {
                x.empty("col", "min", String.valueOf(c + 1), "max", String.valueOf(c + 1), "width", chars(cols.rawSize(c)), "customWidth", cols.hasCustomSize(c) ? "1" : null,
                        "hidden", cols.isHidden(c) ? "1" : null, "outlineLevel", cols.outlineLevel(c) > 0 ? String.valueOf(cols.outlineLevel(c)) : null,
                        "style", cols.style(c) > 0 ? String.valueOf(styles.xf(cols.style(c))) : null);
            }
            x.close();
        }
        x.open("sheetData");
        TreeSet<Integer> rowSet = new TreeSet<>();
        ws.cells().forEach((r, c, cell) -> rowSet.add(r));
        for (long k : spilled.keySet()) rowSet.add(CellAddress.keyRow(k));
        rowSet.addAll(rows.customSizes().keySet());
        rowSet.addAll(rows.hiddenIndexes());
        rowSet.addAll(rows.outlineLevels().keySet());
        rowSet.addAll(rows.styles().keySet());
        Map<Integer, List<int[]>> byRow = new HashMap<>();
        ws.cells().forEach((r, c, cell) -> byRow.computeIfAbsent(r, k -> new ArrayList<>()).add(new int[]{c}));
        for (long k : spilled.keySet()) byRow.computeIfAbsent(CellAddress.keyRow(k), key -> new ArrayList<>()).add(new int[]{CellAddress.keyColumn(k)});
        for (int r : rowSet) {
            List<int[]> cells = byRow.getOrDefault(r, List.of());
            cells.sort((a, b) -> Integer.compare(a[0], b[0]));
            x.open("row", "r", String.valueOf(r + 1), "ht", rows.hasCustomSize(r) ? pts(rows.rawSize(r)) : null, "customHeight", rows.hasCustomSize(r) ? "1" : null,
                    "hidden", rows.isHidden(r) ? "1" : null, "outlineLevel", rows.outlineLevel(r) > 0 ? String.valueOf(rows.outlineLevel(r)) : null,
                    "s", rows.style(r) > 0 ? String.valueOf(styles.xf(rows.style(r))) : null, "customFormat", rows.style(r) > 0 ? "1" : null);
            int lastCol = -1;
            for (int[] cc : cells) {
                int c = cc[0];
                if (c == lastCol) continue;
                lastCol = c;
                SheetCell cell = ws.cells().get(r, c);
                long key = CellAddress.key(r, c);
                if (cell == null) {
                    CellValue v = spilled.get(key);
                    if (v != null) writeCell(x, r, c, v, null, 0, null);
                    continue;
                }
                CellValue value = cell.hasFormula() ? cachedValue(index, r, c, cell) : cell.value();
                writeCell(x, r, c, value, cell.hasFormula() ? cell.formula() : null, cell.style(), anchors.get(key));
            }
            x.close();
        }
        x.close();
        if (p.protection().enabled()) {
            SheetProtection sp = p.protection();
            x.empty("sheetProtection", "password", sp.passwordHash(), "sheet", "1", "objects", sp.objects() ? null : "1", "scenarios", sp.scenarios() ? null : "1",
                    "formatCells", sp.formatCells() ? "0" : null, "formatColumns", sp.formatColumns() ? "0" : null, "formatRows", sp.formatRows() ? "0" : null,
                    "insertColumns", sp.insertColumns() ? "0" : null, "insertRows", sp.insertRows() ? "0" : null, "insertHyperlinks", sp.insertHyperlinks() ? "0" : null,
                    "deleteColumns", sp.deleteColumns() ? "0" : null, "deleteRows", sp.deleteRows() ? "0" : null, "selectLockedCells", sp.selectLocked() ? null : "1",
                    "sort", sp.sort() ? "0" : null, "autoFilter", sp.autoFilter() ? "0" : null, "pivotTables", sp.pivotTables() ? "0" : null, "selectUnlockedCells", sp.selectUnlocked() ? null : "1");
        }
        if (!p.protectedRanges().isEmpty()) {
            x.open("protectedRanges");
            for (var pr : p.protectedRanges()) x.empty("protectedRange", "name", pr.name(), "sqref", sqref(pr.ranges()), "password", pr.passwordHash());
            x.close();
        }
        if (!p.scenarios().isEmpty()) {
            x.open("scenarios");
            for (Scenario s : p.scenarios()) {
                x.open("scenario", "name", s.name(), "count", String.valueOf(s.values().size()), "comment", s.comment().isEmpty() ? null : s.comment());
                for (var e : s.values().entrySet()) x.empty("inputCells", "r", e.getKey().toA1(), "val", e.getValue().display());
                x.close();
            }
            x.close();
        }
        if (p.autoFilter() != null) writeAutoFilter(x, p.autoFilter().range(), p.autoFilter().criteria());
        if (!p.merges().isEmpty()) {
            x.open("mergeCells", "count", String.valueOf(p.merges().size()));
            for (CellRange m : p.merges()) x.empty("mergeCell", "ref", m.toA1());
            x.close();
        }
        int priority = 1;
        for (ConditionalFormat f : p.conditionalFormats()) {
            x.open("conditionalFormatting", "sqref", sqref(f.ranges()));
            for (ConditionalRule rule : f.rules()) writeRule(x, rule, f.ranges().getFirst(), priority++);
            x.close();
        }
        if (!p.validations().isEmpty()) {
            x.open("dataValidations", "count", String.valueOf(p.validations().size()));
            for (DataValidation v : p.validations()) writeValidation(x, v);
            x.close();
        }
        if (!p.links().isEmpty()) {
            x.open("hyperlinks");
            for (Map.Entry<CellAddress, Hyperlink> e : p.links().entrySet()) {
                Hyperlink h = e.getValue();
                if (h.internal()) x.empty("hyperlink", "ref", e.getKey().toA1(), "location", h.target().substring(1), "tooltip", h.tooltip().isEmpty() ? null : h.tooltip(), "display", null);
                else {
                    String id = "rId" + (rels.size() + 1);
                    rels.add(new String[]{id, REL + "hyperlink", h.target(), "External"});
                    x.empty("hyperlink", "ref", e.getKey().toA1(), "r:id", id, "tooltip", h.tooltip().isEmpty() ? null : h.tooltip());
                }
            }
            x.close();
        }
        PrintSettings ps = p.print();
        x.empty("printOptions", "gridLines", ps.gridlines() ? "1" : null, "headings", ps.headings() ? "1" : null, "horizontalCentered", ps.centerHorizontally() ? "1" : null, "verticalCentered", ps.centerVertically() ? "1" : null);
        x.empty("pageMargins", "left", String.valueOf(ps.marginLeft()), "right", String.valueOf(ps.marginRight()), "top", String.valueOf(ps.marginTop()), "bottom", String.valueOf(ps.marginBottom()),
                "header", String.valueOf(ps.marginHeader()), "footer", String.valueOf(ps.marginFooter()));
        x.empty("pageSetup", "paperSize", String.valueOf(ps.paper().code()), "scale", ps.scale() == 100 ? null : String.valueOf(ps.scale()), "fitToWidth", fit ? String.valueOf(ps.fitWidth()) : null,
                "fitToHeight", fit ? String.valueOf(ps.fitHeight()) : null, "orientation", ps.orientation() == PageOrientation.LANDSCAPE ? "landscape" : "portrait", "pageOrder", ps.overThenDown() ? "overThenDown" : null);
        String header = hf(ps.headerLeft(), ps.headerCenter(), ps.headerRight()), footer = hf(ps.footerLeft(), ps.footerCenter(), ps.footerRight());
        if (!header.isEmpty() || !footer.isEmpty()) {
            x.open("headerFooter");
            if (!header.isEmpty()) x.element("oddHeader", header);
            if (!footer.isEmpty()) x.element("oddFooter", footer);
            x.close();
        }
        if (!ps.rowBreaks().isEmpty()) { x.open("rowBreaks", "count", String.valueOf(ps.rowBreaks().size()), "manualBreakCount", String.valueOf(ps.rowBreaks().size())); for (int b : ps.rowBreaks()) x.empty("brk", "id", String.valueOf(b), "max", "16383", "man", "1"); x.close(); }
        if (!ps.columnBreaks().isEmpty()) { x.open("colBreaks", "count", String.valueOf(ps.columnBreaks().size()), "manualBreakCount", String.valueOf(ps.columnBreaks().size())); for (int b : ps.columnBreaks()) x.empty("brk", "id", String.valueOf(b), "max", "1048575", "man", "1"); x.close(); }
        List<SheetObject> drawn = p.objects().stream().filter(o -> o instanceof SheetChart || o instanceof SheetImage || o instanceof SheetShape).toList();
        if (!drawn.isEmpty()) {
            int drawing = ++drawingCounter;
            String id = "rId" + (rels.size() + 1);
            rels.add(new String[]{id, REL + "drawing", "../drawings/drawing" + drawing + ".xml", null});
            writeDrawing(drawing, drawn, ws);
            x.empty("drawing", "r:id", id);
        }
        if (!p.notes().isEmpty()) {
            int comments = ++commentCounter;
            String vmlId = "rId" + (rels.size() + 1);
            rels.add(new String[]{vmlId, REL + "vmlDrawing", "../drawings/vmlDrawing" + comments + ".vml", null});
            rels.add(new String[]{"rId" + (rels.size() + 1), REL + "comments", "../comments" + comments + ".xml", null});
            writeComments(comments, p.notes());
            x.empty("legacyDrawing", "r:id", vmlId);
        }
        if (!p.tables().isEmpty()) {
            x.open("tableParts", "count", String.valueOf(p.tables().size()));
            for (SheetTable t : p.tables()) {
                int n = ++tableCounter;
                String id = "rId" + (rels.size() + 1);
                rels.add(new String[]{id, REL + "table", "../tables/table" + n + ".xml", null});
                parts.put("xl/tables/table" + n + ".xml", table(t, n));
                overrides.add(new String[]{"/xl/tables/table" + n + ".xml", "application/vnd.openxmlformats-officedocument.spreadsheetml.table+xml"});
                x.empty("tablePart", "r:id", id);
            }
            x.close();
        }
        if (!p.sparklines().isEmpty()) writeSparklines(x, p.sparklines());
        x.close();
        if (!rels.isEmpty()) parts.put("xl/worksheets/_rels/sheet" + (index + 1) + ".xml.rels", relationships(rels));
        return x.bytes();
    }

    private void writeCell(XmlBuilder x, int r, int c, CellValue value, String formula, int style, CellRange spill) {
        String ref = new CellAddress(r, c).toA1();
        String s = style > 0 ? String.valueOf(styles.xf(style)) : null;
        if (value instanceof ArrayValue a) value = a.get(0, 0);
        String type = null, v = null;
        switch (value) {
            case NumberValue n -> v = number(n.value());
            case BoolValue b -> { type = "b"; v = b.value() ? "1" : "0"; }
            case ErrorValue e -> { type = "e"; v = e.error().text(); }
            case TextValue t -> {
                if (formula != null) { type = "str"; v = t.value(); }
                else { type = "s"; v = String.valueOf(string(t.value())); }
            }
            case EmptyValue e -> { }
            default -> { v = null; }
        }
        boolean dynamic = spill != null && formula != null;
        if (dynamic) dynamicArrays = true;
        if (formula == null && v == null && s == null) return;
        x.open("c", "r", ref, "s", s, "t", type, "cm", dynamic ? "1" : null);
        if (formula != null) {
            String f = XlsxFormulas.toFile(formula);
            if (dynamic) x.element("f", f, "t", "array", "ref", spill.toA1()); else x.element("f", f);
        }
        if (v != null) x.element("v", v);
        x.close();
    }

    static String number(double v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
        return String.valueOf(v).replace("E", "E");
    }

    private static String pts(int px) { double pt = px * 0.75; return XlsxStyles.trim(Math.round(pt * 100) / 100.0); }
    private static String chars(int px) { double w = Math.max(0, (px - 5) / 7.0); return XlsxStyles.trim(Math.round(w * 256) / 256.0); }

    private static String sqref(List<CellRange> ranges) {
        StringBuilder b = new StringBuilder();
        for (CellRange r : ranges) { if (b.length() > 0) b.append(' '); b.append(r.toA1()); }
        return b.toString();
    }

    private static String hf(String l, String c, String r) {
        StringBuilder b = new StringBuilder();
        if (!l.isEmpty()) b.append("&L").append(l);
        if (!c.isEmpty()) b.append("&C").append(c);
        if (!r.isEmpty()) b.append("&R").append(r);
        return b.toString();
    }

    private void writeAutoFilter(XmlBuilder x, CellRange range, Map<Integer, FilterCriteria> criteria) {
        x.open("autoFilter", "ref", range.toA1());
        for (Map.Entry<Integer, FilterCriteria> e : criteria.entrySet()) {
            FilterCriteria c = e.getValue();
            x.open("filterColumn", "colId", String.valueOf(e.getKey() - range.firstColumn()));
            if (c.values() != null) {
                x.open("filters", "blank", c.includeBlanks() ? "1" : null);
                for (String v : c.values()) x.empty("filter", "val", v);
                x.close();
            } else if (!c.conditions().isEmpty()) {
                x.open("customFilters", "and", c.and() && c.conditions().size() > 1 ? "1" : null);
                for (FilterCondition f : c.conditions()) {
                    String op, val = f.value();
                    switch (f.operator()) {
                        case BEGINS_WITH -> { op = null; val = val + "*"; }
                        case ENDS_WITH -> { op = null; val = "*" + val; }
                        case CONTAINS -> { op = null; val = "*" + val + "*"; }
                        case NOT_CONTAINS -> { op = "notEqual"; val = "*" + val + "*"; }
                        default -> op = f.operator().xml().equals("equal") ? null : f.operator().xml();
                    }
                    x.empty("customFilter", "operator", op, "val", val);
                }
                x.close();
            } else if (c.top() != null) x.empty("top10", "top", c.bottom() ? "0" : null, "percent", c.topPercent() ? "1" : null, "val", String.valueOf(c.top()));
            else if (c.dynamic() != null) x.empty("dynamicFilter", "type", c.dynamic());
            x.close();
        }
        x.close();
    }

    private int dxf(DifferentialStyle d) {
        return dxfIds.computeIfAbsent(d, k -> { dxfs.add(k); return dxfs.size() - 1; });
    }

    private void writeRule(XmlBuilder x, ConditionalRule rule, CellRange anchor, int priority) {
        String cell = anchor.first().toA1();
        String prio = String.valueOf(priority);
        switch (rule.type()) {
            case COLOR_SCALE -> {
                x.open("cfRule", "type", "colorScale", "priority", prio).open("colorScale");
                for (Threshold t : rule.thresholds()) cfvo(x, t);
                for (int c : rule.colors()) x.empty("color", "rgb", XlsxColors.hex(c));
                x.close().close();
            }
            case DATA_BAR -> {
                x.open("cfRule", "type", "dataBar", "priority", prio).open("dataBar", "showValue", rule.showValue() ? null : "0");
                for (Threshold t : rule.thresholds()) cfvo(x, t);
                x.empty("color", "rgb", XlsxColors.hex(rule.colors().isEmpty() ? 0xFF638EC6 : rule.colors().getFirst()));
                x.close().close();
            }
            case ICON_SET -> {
                x.open("cfRule", "type", "iconSet", "priority", prio).open("iconSet", "iconSet", rule.iconSet().xml(), "reverse", rule.reverseIcons() ? "1" : null, "showValue", rule.showValue() ? null : "0");
                for (Threshold t : rule.thresholds()) cfvo(x, t);
                x.close().close();
            }
            default -> {
                String dxf = String.valueOf(dxf(rule.style()));
                String type = rule.type().xml(), text = rule.text().replace("\"", "\"\"");
                List<String> formulas = new ArrayList<>();
                String op = null;
                switch (rule.type()) {
                    case CELL_VALUE -> { op = rule.operator().xml(); formulas.add(rule.formula1()); if (rule.operator().twoOperands()) formulas.add(rule.formula2()); }
                    case EXPRESSION -> formulas.add(rule.formula1());
                    case CONTAINS_TEXT -> { op = "containsText"; formulas.add("NOT(ISERROR(SEARCH(\"" + text + "\"," + cell + ")))"); }
                    case NOT_CONTAINS_TEXT -> { op = "notContains"; formulas.add("ISERROR(SEARCH(\"" + text + "\"," + cell + "))"); }
                    case BEGINS_WITH -> { op = "beginsWith"; formulas.add("LEFT(" + cell + ",LEN(\"" + text + "\"))=\"" + text + "\""); }
                    case ENDS_WITH -> { op = "endsWith"; formulas.add("RIGHT(" + cell + ",LEN(\"" + text + "\"))=\"" + text + "\""); }
                    case BLANKS -> formulas.add("LEN(TRIM(" + cell + "))=0");
                    case NO_BLANKS -> formulas.add("LEN(TRIM(" + cell + "))>0");
                    case ERRORS -> formulas.add("ISERROR(" + cell + ")");
                    case NO_ERRORS -> formulas.add("NOT(ISERROR(" + cell + "))");
                    case TIME_PERIOD -> formulas.add("FLOOR(" + cell + ",1)=TODAY()");
                    default -> { }
                }
                x.open("cfRule", "type", type, "dxfId", dxf, "priority", prio, "stopIfTrue", rule.stopIfTrue() ? "1" : null, "operator", op,
                        "text", rule.text().isEmpty() ? null : rule.text(), "timePeriod", rule.type() == dtm.stools.component.panels.editor.sheet.model.ConditionalRuleType.TIME_PERIOD ? rule.timePeriod().xml() : null,
                        "rank", rule.type() == dtm.stools.component.panels.editor.sheet.model.ConditionalRuleType.TOP_BOTTOM ? String.valueOf(rule.rank()) : null,
                        "percent", rule.percent() ? "1" : null, "bottom", rule.bottom() ? "1" : null,
                        "aboveAverage", rule.type() == dtm.stools.component.panels.editor.sheet.model.ConditionalRuleType.ABOVE_AVERAGE && rule.below() ? "0" : null,
                        "equalAverage", rule.equalAverage() ? "1" : null, "stdDev", rule.stdDev() > 0 ? String.valueOf(rule.stdDev()) : null);
                for (String f : formulas) if (f != null) x.element("formula", XlsxFormulas.toFile(f.startsWith("=") ? f.substring(1) : f));
                x.close();
            }
        }
    }

    private static void cfvo(XmlBuilder x, Threshold t) {
        x.empty("cfvo", "type", t.type().xml(), "val", t.value().isEmpty() ? null : t.value(), "gte", t.greaterOrEqual() ? null : "0");
    }

    private static void writeValidation(XmlBuilder x, DataValidation v) {
        boolean checkbox = v.type() == dtm.stools.component.panels.editor.sheet.model.ValidationType.CHECKBOX;
        x.open("dataValidation", "type", checkbox ? "list" : v.type() == dtm.stools.component.panels.editor.sheet.model.ValidationType.ANY ? null : v.type().xml(),
                "errorStyle", v.errorStyle() == dtm.stools.component.panels.editor.sheet.model.ErrorAlertStyle.STOP ? null : v.errorStyle().xml(),
                "operator", v.operator() == dtm.stools.component.panels.editor.sheet.model.ComparisonOperator.BETWEEN ? null : v.operator().xml(),
                "allowBlank", v.allowBlank() ? "1" : null, "showDropDown", v.type() == dtm.stools.component.panels.editor.sheet.model.ValidationType.LIST && !v.showDropdown() ? "1" : null,
                "showInputMessage", v.showInput() ? "1" : null, "showErrorMessage", v.showError() ? "1" : null,
                "errorTitle", v.errorTitle().isEmpty() ? null : v.errorTitle(), "error", v.errorMessage().isEmpty() ? null : v.errorMessage(),
                "promptTitle", checkbox ? "sheetEditor:checkbox" : v.inputTitle().isEmpty() ? null : v.inputTitle(), "prompt", v.inputMessage().isEmpty() ? null : v.inputMessage(),
                "sqref", sqref(v.ranges()));
        if (checkbox) x.element("formula1", "\"TRUE,FALSE\"");
        else {
            if (v.formula1() != null) x.element("formula1", formulaOrList(v.formula1()));
            if (v.formula2() != null && v.operator().twoOperands()) x.element("formula2", formulaOrList(v.formula2()));
        }
        x.close();
    }

    private static String formulaOrList(String f) {
        if (f.startsWith("\"")) return f;
        try { return XlsxFormulas.toFile(f.startsWith("=") ? f.substring(1) : f); } catch (RuntimeException e) { return f; }
    }

    private byte[] table(SheetTable t, int id) {
        XmlBuilder x = new XmlBuilder();
        x.open("table", "xmlns", NS, "id", String.valueOf(id), "name", t.name(), "displayName", t.name(), "ref", t.range().toA1(),
                "headerRowCount", t.headerRow() ? null : "0", "totalsRowCount", t.totalsRow() ? "1" : null, "totalsRowShown", t.totalsRow() ? null : "0");
        if (t.filterButton() && t.headerRow()) {
            CellRange af = t.totalsRow() ? new CellRange(t.range().firstRow(), t.range().firstColumn(), t.range().lastRow() - 1, t.range().lastColumn()) : t.range();
            x.empty("autoFilter", "ref", af.toA1());
        }
        x.open("tableColumns", "count", String.valueOf(t.columns().size()));
        int k = 1;
        for (TableColumn c : t.columns()) {
            x.open("tableColumn", "id", String.valueOf(k++), "name", c.name(), "totalsRowFunction", c.totals() == TotalsFunction.NONE ? null : c.totals().xml(), "totalsRowLabel", c.totalsLabel());
            if (c.calculatedFormula() != null) x.element("calculatedColumnFormula", XlsxFormulas.toFile(c.calculatedFormula()));
            x.close();
        }
        x.close();
        x.empty("tableStyleInfo", "name", t.style(), "showFirstColumn", t.firstColumn() ? "1" : "0", "showLastColumn", t.lastColumn() ? "1" : "0",
                "showRowStripes", t.bandedRows() ? "1" : "0", "showColumnStripes", t.bandedColumns() ? "1" : "0");
        x.close();
        return x.bytes();
    }

    private void writeComments(int n, Map<CellAddress, SheetNote> notes) {
        List<String> authors = new ArrayList<>();
        for (SheetNote note : notes.values()) if (!authors.contains(note.author())) authors.add(note.author());
        XmlBuilder x = new XmlBuilder();
        x.open("comments", "xmlns", NS).open("authors");
        for (String a : authors) x.element("author", a);
        x.close().open("commentList");
        for (Map.Entry<CellAddress, SheetNote> e : notes.entrySet()) {
            x.open("comment", "ref", e.getKey().toA1(), "authorId", String.valueOf(authors.indexOf(e.getValue().author()))).open("text").open("r").element("t", e.getValue().text(), "xml:space", "preserve").close().close().close();
        }
        x.close().close();
        parts.put("xl/comments" + n + ".xml", x.bytes());
        overrides.add(new String[]{"/xl/comments" + n + ".xml", "application/vnd.openxmlformats-officedocument.spreadsheetml.comments+xml"});
        StringBuilder vml = new StringBuilder("<xml xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\">");
        vml.append("<o:shapelayout v:ext=\"edit\"><o:idmap v:ext=\"edit\" data=\"").append(n).append("\"/></o:shapelayout>");
        vml.append("<v:shapetype id=\"_x0000_t202\" coordsize=\"21600,21600\" o:spt=\"202\" path=\"m,l,21600r21600,l21600,xe\"><v:stroke joinstyle=\"miter\"/><v:path gradientshapeok=\"t\" o:connecttype=\"rect\"/></v:shapetype>");
        int shape = n * 1024 + 1;
        for (Map.Entry<CellAddress, SheetNote> e : notes.entrySet()) {
            CellAddress a = e.getKey();
            vml.append("<v:shape id=\"_x0000_s").append(shape++).append("\" type=\"#_x0000_t202\" style=\"position:absolute;margin-left:59.25pt;margin-top:1.5pt;width:108pt;height:59.25pt;z-index:1;visibility:")
                    .append(e.getValue().visible() ? "visible" : "hidden").append("\" fillcolor=\"#ffffe1\" o:insetmode=\"auto\"><v:fill color2=\"#ffffe1\"/><v:shadow on=\"t\" color=\"black\" obscured=\"t\"/><v:path o:connecttype=\"none\"/>")
                    .append("<v:textbox style=\"mso-direction-alt:auto\"><div style=\"text-align:left\"></div></v:textbox><x:ClientData ObjectType=\"Note\"><x:MoveWithCells/><x:SizeWithCells/>")
                    .append("<x:Anchor>").append(a.column() + 1).append(", 15, ").append(a.row()).append(", 2, ").append(a.column() + 3).append(", 15, ").append(a.row() + 4).append(", 16</x:Anchor>")
                    .append("<x:AutoFill>False</x:AutoFill><x:Row>").append(a.row()).append("</x:Row><x:Column>").append(a.column()).append("</x:Column>")
                    .append(e.getValue().visible() ? "<x:Visible/>" : "").append("</x:ClientData></v:shape>");
        }
        vml.append("</xml>");
        parts.put("xl/drawings/vmlDrawing" + n + ".vml", vml.toString().getBytes(StandardCharsets.UTF_8));
    }

    private void writeSparklines(XmlBuilder x, List<Sparkline> sparklines) {
        x.open("extLst").open("ext", "uri", "{05C60535-1F16-4fd2-B633-F4F36F0B64E0}", "xmlns:x14", "http://schemas.microsoft.com/office/spreadsheetml/2009/9/main");
        x.open("x14:sparklineGroups", "xmlns:xm", "http://schemas.microsoft.com/office/excel/2006/main");
        for (Sparkline s : sparklines) {
            String type = switch (s.type()) { case COLUMN -> "column"; case WIN_LOSS -> "stacked"; default -> null; };
            x.open("x14:sparklineGroup", "type", type, "markers", s.markers() ? "1" : null, "high", s.highPoint() ? "1" : null, "low", s.lowPoint() ? "1" : null, "negative", s.negativePoints() ? "1" : null, "displayEmptyCellsAs", "gap");
            x.empty("x14:colorSeries", "rgb", XlsxColors.hex(s.color()));
            x.empty("x14:colorNegative", "rgb", "FFD00000");
            x.open("x14:sparklines").open("x14:sparkline");
            String ref = s.dataRef();
            x.element("xm:f", ref.contains("!") ? ref : FormulaPrinter.sheet(workbook.sheet(0).name()) + "!" + ref);
            x.element("xm:sqref", s.location().toA1());
            x.close().close().close();
        }
        x.close().close().close();
    }

    private void writeDrawing(int n, List<SheetObject> objects, SheetWorksheet ws) {
        List<String[]> rels = new ArrayList<>();
        XmlBuilder x = new XmlBuilder();
        x.open("xdr:wsDr", "xmlns:xdr", "http://schemas.openxmlformats.org/drawingml/2006/spreadsheetDrawing", "xmlns:a", "http://schemas.openxmlformats.org/drawingml/2006/main");
        int shapeId = 2;
        for (SheetObject o : objects) {
            var a = o.anchor();
            x.open("xdr:oneCellAnchor");
            x.open("xdr:from").element("xdr:col", String.valueOf(a.column())).element("xdr:colOff", String.valueOf((long) a.offsetX() * EMU))
                    .element("xdr:row", String.valueOf(a.row())).element("xdr:rowOff", String.valueOf((long) a.offsetY() * EMU)).close();
            x.empty("xdr:ext", "cx", String.valueOf((long) a.width() * EMU), "cy", String.valueOf((long) a.height() * EMU));
            switch (o) {
                case SheetChart chart -> {
                    int c = ++chartCounter;
                    String id = "rId" + (rels.size() + 1);
                    rels.add(new String[]{id, REL + "chart", "../charts/chart" + c + ".xml", null});
                    parts.put("xl/charts/chart" + c + ".xml", XlsxCharts.write(chart, ws.name()));
                    overrides.add(new String[]{"/xl/charts/chart" + c + ".xml", "application/vnd.openxmlformats-officedocument.drawingml.chart+xml"});
                    x.open("xdr:graphicFrame", "macro", "").open("xdr:nvGraphicFramePr").empty("xdr:cNvPr", "id", String.valueOf(shapeId++), "name", "Gráfico " + c).empty("xdr:cNvGraphicFramePr").close();
                    x.open("xdr:xfrm").empty("a:off", "x", "0", "y", "0").empty("a:ext", "cx", "0", "cy", "0").close();
                    x.open("a:graphic").open("a:graphicData", "uri", "http://schemas.openxmlformats.org/drawingml/2006/chart")
                            .empty("c:chart", "xmlns:c", "http://schemas.openxmlformats.org/drawingml/2006/chart", "xmlns:r", R, "r:id", id).close().close();
                    x.close();
                }
                case SheetImage image -> {
                    int k = ++imageCounter;
                    String ext = image.format().toLowerCase(java.util.Locale.ROOT).replace("jpeg", "jpg");
                    if (!mediaExtensions.contains(ext)) mediaExtensions.add(ext);
                    parts.put("xl/media/image" + k + "." + ext, image.data());
                    String id = "rId" + (rels.size() + 1);
                    rels.add(new String[]{id, REL + "image", "../media/image" + k + "." + ext, null});
                    x.open("xdr:pic").open("xdr:nvPicPr").empty("xdr:cNvPr", "id", String.valueOf(shapeId++), "name", "Imagem " + k, "descr", image.altText().isEmpty() ? null : image.altText())
                            .open("xdr:cNvPicPr").empty("a:picLocks", "noChangeAspect", "1").close().close();
                    x.open("xdr:blipFill").empty("a:blip", "xmlns:r", R, "r:embed", id).open("a:stretch").empty("a:fillRect").close().close();
                    x.open("xdr:spPr").open("a:xfrm").empty("a:off", "x", "0", "y", "0").empty("a:ext", "cx", String.valueOf((long) a.width() * EMU), "cy", String.valueOf((long) a.height() * EMU)).close()
                            .open("a:prstGeom", "prst", "rect").empty("a:avLst").close().close();
                    x.close();
                }
                case SheetShape shape -> {
                    String prst = switch (shape.type()) { case ELLIPSE -> "ellipse"; case ROUNDED_RECTANGLE -> "roundRect"; case TRIANGLE -> "triangle"; case ARROW_RIGHT -> "rightArrow"; case LINE -> "line"; default -> "rect"; };
                    x.open("xdr:sp", "macro", "", "textlink", "").open("xdr:nvSpPr").empty("xdr:cNvPr", "id", String.valueOf(shapeId++), "name", "Forma " + shapeId).empty("xdr:cNvSpPr", "txBox", shape.type() == dtm.stools.component.panels.editor.sheet.model.ShapeType.TEXT_BOX ? "1" : null).close();
                    x.open("xdr:spPr").open("a:xfrm").empty("a:off", "x", "0", "y", "0").empty("a:ext", "cx", String.valueOf((long) a.width() * EMU), "cy", String.valueOf((long) a.height() * EMU)).close()
                            .open("a:prstGeom", "prst", prst).empty("a:avLst").close();
                    if (shape.fill() != null) x.open("a:solidFill").empty("a:srgbClr", "val", XlsxColors.hex(shape.fill()).substring(2)).close();
                    if (shape.line() != null) x.open("a:ln", "w", String.valueOf((long) (shape.lineWidth() * 12700))).open("a:solidFill").empty("a:srgbClr", "val", XlsxColors.hex(shape.line()).substring(2)).close().close();
                    x.close();
                    if (!shape.text().isEmpty()) x.open("xdr:txBody").empty("a:bodyPr").open("a:p").open("a:r").element("a:t", shape.text()).close().close().close();
                    x.close();
                }
                default -> { }
            }
            x.empty("xdr:clientData");
            x.close();
        }
        x.close();
        parts.put("xl/drawings/drawing" + n + ".xml", x.bytes());
        overrides.add(new String[]{"/xl/drawings/drawing" + n + ".xml", "application/vnd.openxmlformats-officedocument.drawing+xml"});
        if (!rels.isEmpty()) parts.put("xl/drawings/_rels/drawing" + n + ".xml.rels", relationships(rels));
    }

    private byte[] sharedStrings() {
        XmlBuilder x = new XmlBuilder();
        x.open("sst", "xmlns", NS, "count", String.valueOf(stringRefs), "uniqueCount", String.valueOf(strings.size()));
        for (String s : strings.keySet()) {
            boolean preserve = !s.equals(s.strip()) || s.contains("\n");
            x.open("si").element("t", s, "xml:space", preserve ? "preserve" : null).close();
        }
        x.close();
        return x.bytes();
    }

    private byte[] workbookXml() {
        XmlBuilder x = new XmlBuilder();
        var props = workbook.properties();
        x.open("workbook", "xmlns", NS, "xmlns:r", R);
        x.empty("fileVersion", "appName", "xl", "lastEdited", "7", "lowestEdited", "7", "rupBuild", "27425");
        x.empty("workbookPr", "date1904", props.date1904() ? "1" : null, "defaultThemeVersion", "202300");
        if (props.protectStructure()) x.empty("workbookProtection", "lockStructure", "1", "workbookPassword", props.protectionHash());
        x.open("bookViews").empty("workbookView", "xWindow", "0", "yWindow", "0", "windowWidth", "28800", "windowHeight", "15000", "activeTab", String.valueOf(workbook.activeSheetIndex())).close();
        x.open("sheets");
        for (int i = 0; i < workbook.sheetCount(); i++) {
            SheetWorksheet s = workbook.sheet(i);
            x.empty("sheet", "name", s.name(), "sheetId", String.valueOf(i + 1), "state", s.properties().visibility() == SheetVisibility.VISIBLE ? null : s.properties().visibility().xml(), "r:id", "rId" + (i + 1));
        }
        x.close();
        List<String[]> names = new ArrayList<>();
        for (DefinedName n : props.names()) names.add(new String[]{n.name(), n.sheetScope() == null ? null : String.valueOf(n.sheetScope()), XlsxFormulas.toFile(n.formula()), n.hidden() ? "1" : null, n.comment().isEmpty() ? null : n.comment()});
        ObjectMapper json = new ObjectMapper();
        for (int i = 0; i < workbook.sheetCount(); i++) {
            SheetWorksheet s = workbook.sheet(i);
            String sheetRef = FormulaPrinter.sheet(s.name()) + "!";
            PrintSettings ps = s.properties().print();
            if (ps.printArea() != null) names.add(new String[]{"_xlnm.Print_Area", String.valueOf(i), sheetRef + abs(ps.printArea()), null, null});
            List<String> titles = new ArrayList<>();
            if (ps.repeatColumnFirst() != null) titles.add(sheetRef + "$" + CellAddress.columnName(ps.repeatColumnFirst()) + ":$" + CellAddress.columnName(ps.repeatColumnLast() == null ? ps.repeatColumnFirst() : ps.repeatColumnLast()));
            if (ps.repeatRowFirst() != null) titles.add(sheetRef + "$" + (ps.repeatRowFirst() + 1) + ":$" + ((ps.repeatRowLast() == null ? ps.repeatRowFirst() : ps.repeatRowLast()) + 1));
            if (!titles.isEmpty()) names.add(new String[]{"_xlnm.Print_Titles", String.valueOf(i), String.join(",", titles), null, null});
            if (s.properties().autoFilter() != null) names.add(new String[]{"_xlnm._FilterDatabase", String.valueOf(i), sheetRef + abs(s.properties().autoFilter().range()), "1", null});
            int k = 1;
            for (PivotTable p : s.properties().pivots()) {
                try {
                    names.add(new String[]{"_SheetEditorPivot" + (i + 1) + "_" + (k++), String.valueOf(i), sheetRef + p.target().toAbsolute(), "1", json.writeValueAsString(PivotJson.from(p))});
                } catch (Exception ignored) { }
            }
            k = 1;
            for (var view : s.properties().filterViews()) {
                try {
                    names.add(new String[]{"_SheetEditorFilterView" + (i + 1) + "_" + (k++), String.valueOf(i), sheetRef + abs(view.filter().range()), "1", json.writeValueAsString(Map.of("name", view.name(), "criteria", FilterJson.from(view.filter().criteria())))});
                } catch (Exception ignored) { }
            }
        }
        if (!names.isEmpty()) {
            x.open("definedNames");
            for (String[] n : names) x.element("definedName", n[2], "name", n[0], "localSheetId", n[1], "hidden", n[3], "comment", n[4]);
            x.close();
        }
        x.empty("calcPr", "calcId", "191029", "fullCalcOnLoad", "1");
        x.close();
        return x.bytes();
    }

    private static String abs(CellRange r) {
        if (r.isWholeColumn()) return "$" + CellAddress.columnName(r.firstColumn()) + ":$" + CellAddress.columnName(r.lastColumn());
        if (r.isWholeRow()) return "$" + (r.firstRow() + 1) + ":$" + (r.lastRow() + 1);
        return r.first().toAbsolute() + (r.isSingleCell() ? "" : ":" + r.last().toAbsolute());
    }

    private byte[] workbookRels() {
        List<String[]> rels = new ArrayList<>();
        for (int i = 0; i < workbook.sheetCount(); i++) rels.add(new String[]{"rId" + (i + 1), REL + "worksheet", "worksheets/sheet" + (i + 1) + ".xml", null});
        int n = workbook.sheetCount();
        rels.add(new String[]{"rId" + (++n), REL + "styles", "styles.xml", null});
        rels.add(new String[]{"rId" + (++n), REL + "sharedStrings", "sharedStrings.xml", null});
        if (dynamicArrays) rels.add(new String[]{"rId" + (++n), REL + "sheetMetadata", "metadata.xml", null});
        return relationships(rels);
    }

    private static byte[] relationships(List<String[]> rels) {
        XmlBuilder x = new XmlBuilder();
        x.open("Relationships", "xmlns", "http://schemas.openxmlformats.org/package/2006/relationships");
        for (String[] r : rels) x.empty("Relationship", "Id", r[0], "Type", r[1], "Target", r[2], "TargetMode", r[3]);
        x.close();
        return x.bytes();
    }

    private byte[] rootRels() {
        return relationships(List.of(
                new String[]{"rId1", REL + "officeDocument", "xl/workbook.xml", null},
                new String[]{"rId2", "http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties", "docProps/core.xml", null},
                new String[]{"rId3", REL + "extended-properties", "docProps/app.xml", null}));
    }

    private byte[] core() {
        XmlBuilder x = new XmlBuilder();
        x.open("cp:coreProperties", "xmlns:cp", "http://schemas.openxmlformats.org/package/2006/metadata/core-properties", "xmlns:dc", "http://purl.org/dc/elements/1.1/",
                "xmlns:dcterms", "http://purl.org/dc/terms/", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        if (!workbook.properties().title().isEmpty()) x.element("dc:title", workbook.properties().title());
        x.element("dc:creator", workbook.properties().author().isEmpty() ? System.getProperty("user.name", "") : workbook.properties().author());
        String now = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        x.element("dcterms:created", now, "xsi:type", "dcterms:W3CDTF");
        x.element("dcterms:modified", now, "xsi:type", "dcterms:W3CDTF");
        x.close();
        return x.bytes();
    }

    private byte[] app() {
        XmlBuilder x = new XmlBuilder();
        x.open("Properties", "xmlns", "http://schemas.openxmlformats.org/officeDocument/2006/extended-properties").element("Application", "Microsoft Excel").element("DocSecurity", "0").close();
        return x.bytes();
    }

    private static byte[] metadata() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<metadata xmlns=\"" + NS + "\" xmlns:xda=\"http://schemas.microsoft.com/office/spreadsheetml/2017/dynamicarray\">"
                + "<metadataTypes count=\"1\"><metadataType name=\"XLDAPR\" minSupportedVersion=\"120000\" copy=\"1\" pasteAll=\"1\" pasteValues=\"1\" merge=\"1\" splitFirst=\"1\" rowColShift=\"1\" clearFormats=\"1\" clearComments=\"1\" assign=\"1\" coerce=\"1\" cellMeta=\"1\"/></metadataTypes>"
                + "<futureMetadata name=\"XLDAPR\" count=\"1\"><bk><extLst><ext uri=\"{bdbb8cdc-fa1e-496e-a857-3c3f30c029c3}\"><xda:dynamicArrayProperties fDynamic=\"1\" fCollapsed=\"0\"/></ext></extLst></bk></futureMetadata>"
                + "<cellMetadata count=\"1\"><bk><rc t=\"1\" v=\"0\"/></bk></cellMetadata></metadata>";
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    private byte[] contentTypes() {
        XmlBuilder x = new XmlBuilder();
        x.open("Types", "xmlns", "http://schemas.openxmlformats.org/package/2006/content-types");
        x.empty("Default", "Extension", "rels", "ContentType", "application/vnd.openxmlformats-package.relationships+xml");
        x.empty("Default", "Extension", "xml", "ContentType", "application/xml");
        x.empty("Default", "Extension", "vml", "ContentType", "application/vnd.openxmlformats-officedocument.vmlDrawing");
        for (String ext : mediaExtensions) x.empty("Default", "Extension", ext, "ContentType", switch (ext) { case "jpg" -> "image/jpeg"; case "gif" -> "image/gif"; case "bmp" -> "image/bmp"; default -> "image/" + ext; });
        x.empty("Override", "PartName", "/xl/workbook.xml", "ContentType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml");
        for (int i = 0; i < workbook.sheetCount(); i++) x.empty("Override", "PartName", "/xl/worksheets/sheet" + (i + 1) + ".xml", "ContentType", "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml");
        x.empty("Override", "PartName", "/xl/styles.xml", "ContentType", "application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml");
        x.empty("Override", "PartName", "/xl/sharedStrings.xml", "ContentType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml");
        if (dynamicArrays) x.empty("Override", "PartName", "/xl/metadata.xml", "ContentType", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheetMetadata+xml");
        for (String[] o : overrides) x.empty("Override", "PartName", o[0], "ContentType", o[1]);
        x.empty("Override", "PartName", "/docProps/core.xml", "ContentType", "application/vnd.openxmlformats-package.core-properties+xml");
        x.empty("Override", "PartName", "/docProps/app.xml", "ContentType", "application/vnd.openxmlformats-officedocument.extended-properties+xml");
        x.close();
        return x.bytes();
    }

    static Optional<FormulaCell> none() { return Optional.empty(); }
}
