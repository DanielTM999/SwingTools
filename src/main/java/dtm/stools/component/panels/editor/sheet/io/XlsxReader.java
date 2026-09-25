package dtm.stools.component.panels.editor.sheet.io;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dtm.stools.component.panels.editor.sheet.format.DateSerial;
import dtm.stools.component.panels.editor.sheet.formula.ReferenceAdjuster;
import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetPackage;
import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetXml;
import dtm.stools.component.panels.editor.sheet.model.AutoFilter;
import dtm.stools.component.panels.editor.sheet.model.BoolValue;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellError;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ComparisonOperator;
import dtm.stools.component.panels.editor.sheet.model.ConditionalFormat;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRule;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRuleType;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.DefinedName;
import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;
import dtm.stools.component.panels.editor.sheet.model.ErrorAlertStyle;
import dtm.stools.component.panels.editor.sheet.model.FilterCondition;
import dtm.stools.component.panels.editor.sheet.model.FilterCriteria;
import dtm.stools.component.panels.editor.sheet.model.FilterOperator;
import dtm.stools.component.panels.editor.sheet.model.FilterView;
import dtm.stools.component.panels.editor.sheet.model.FreezePane;
import dtm.stools.component.panels.editor.sheet.model.Hyperlink;
import dtm.stools.component.panels.editor.sheet.model.IconSetType;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.PageOrientation;
import dtm.stools.component.panels.editor.sheet.model.PaperSize;
import dtm.stools.component.panels.editor.sheet.model.PivotTable;
import dtm.stools.component.panels.editor.sheet.model.PrintSettings;
import dtm.stools.component.panels.editor.sheet.model.ProtectedRange;
import dtm.stools.component.panels.editor.sheet.model.Scenario;
import dtm.stools.component.panels.editor.sheet.model.ShapeType;
import dtm.stools.component.panels.editor.sheet.model.SheetCell;
import dtm.stools.component.panels.editor.sheet.model.SheetComment;
import dtm.stools.component.panels.editor.sheet.model.SheetImage;
import dtm.stools.component.panels.editor.sheet.model.SheetNote;
import dtm.stools.component.panels.editor.sheet.model.SheetObject;
import dtm.stools.component.panels.editor.sheet.model.SheetProperties;
import dtm.stools.component.panels.editor.sheet.model.SheetProtection;
import dtm.stools.component.panels.editor.sheet.model.SheetShape;
import dtm.stools.component.panels.editor.sheet.model.SheetStylePool;
import dtm.stools.component.panels.editor.sheet.model.SheetTable;
import dtm.stools.component.panels.editor.sheet.model.SheetTheme;
import dtm.stools.component.panels.editor.sheet.model.SheetThread;
import dtm.stools.component.panels.editor.sheet.model.SheetViewMode;
import dtm.stools.component.panels.editor.sheet.model.SheetVisibility;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;
import dtm.stools.component.panels.editor.sheet.model.Sparkline;
import dtm.stools.component.panels.editor.sheet.model.SparklineType;
import dtm.stools.component.panels.editor.sheet.model.TableColumn;
import dtm.stools.component.panels.editor.sheet.model.TextValue;
import dtm.stools.component.panels.editor.sheet.model.Threshold;
import dtm.stools.component.panels.editor.sheet.model.ThresholdType;
import dtm.stools.component.panels.editor.sheet.model.TimePeriod;
import dtm.stools.component.panels.editor.sheet.model.TotalsFunction;
import dtm.stools.component.panels.editor.sheet.model.ValidationType;
import dtm.stools.component.panels.editor.sheet.model.WorkbookProperties;
import org.w3c.dom.Element;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class XlsxReader {
    private final SheetPackage pkg;
    private final List<String> diagnostics = new ArrayList<>();
    private final SheetStylePool pool = new SheetStylePool();
    private final List<String> sharedStrings = new ArrayList<>();
    private XlsxStyles.ReadResult styles;
    private SheetTheme theme = SheetTheme.OFFICE;
    private boolean date1904;

    public XlsxReader(SheetPackage pkg) { this.pkg = pkg; }

    public static SheetImportResult read(byte[] bytes, SheetPackage.Limits limits) throws IOException {
        SheetPackage pkg = SheetPackage.read(bytes, limits);
        if (!pkg.contains("[Content_Types].xml")) throw new IOException("O arquivo não é uma pasta de trabalho do Excel.");
        XlsxReader r = new XlsxReader(pkg);
        SheetWorkbook wb = r.read();
        return new SheetImportResult(wb, r.diagnostics, true, bytes, "xlsx");
    }

    private Map<String, String[]> rels(String part) throws IOException {
        Map<String, String[]> map = new LinkedHashMap<>();
        byte[] data = pkg.partOrNull(SheetPackage.relsPath(part));
        if (data == null) return map;
        for (Element r : SheetXml.children(SheetXml.parse(data).getDocumentElement(), "Relationship")) {
            String target = SheetXml.attr(r, "Target");
            boolean external = "External".equals(SheetXml.attr(r, "TargetMode"));
            map.put(SheetXml.attr(r, "Id"), new String[]{SheetXml.attr(r, "Type"), external ? target : SheetPackage.resolve(part, target), external ? "external" : ""});
        }
        return map;
    }

    private String relTarget(Map<String, String[]> rels, String typeSuffix) {
        for (String[] r : rels.values()) if (r[0].endsWith(typeSuffix)) return r[1];
        return null;
    }

    public SheetWorkbook read() throws IOException {
        Map<String, String[]> root = rels("");
        String workbookPart = relTarget(root, "/officeDocument");
        if (workbookPart == null) workbookPart = "xl/workbook.xml";
        if (!pkg.contains(workbookPart)) throw new IOException("Pasta de trabalho sem a parte principal.");
        Element wbx = SheetXml.parse(pkg.part(workbookPart)).getDocumentElement();
        Map<String, String[]> wbRels = rels(workbookPart);
        String themePart = relTarget(wbRels, "/theme");
        if (themePart != null && pkg.contains(themePart)) theme = readTheme(pkg.part(themePart));
        String stylesPart = relTarget(wbRels, "/styles");
        styles = XlsxStyles.read(stylesPart == null ? null : pkg.partOrNull(stylesPart), pool, theme);
        String ssPart = relTarget(wbRels, "/sharedStrings");
        if (ssPart != null && pkg.contains(ssPart)) readSharedStrings(pkg.part(ssPart));
        date1904 = SheetXml.boolAttr(SheetXml.child(wbx, "workbookPr"), "date1904", false);
        if (pkg.names().stream().anyMatch(n -> n.startsWith("xl/pivotTables/"))) diagnostics.add("Tabelas dinâmicas do Excel foram importadas como valores.");
        if (pkg.contains("xl/vbaProject.bin")) diagnostics.add("Macros VBA não são executadas nem preservadas.");
        if (pkg.names().stream().anyMatch(n -> n.startsWith("xl/externalLinks/"))) diagnostics.add("Vínculos com pastas de trabalho externas não são atualizados.");
        List<SheetWorksheet> sheets = new ArrayList<>();
        List<Element> sheetElements = SheetXml.children(SheetXml.child(wbx, "sheets"), "sheet");
        Map<Integer, List<Element>> localNames = new HashMap<>();
        List<DefinedName> names = new ArrayList<>();
        for (Element dn : SheetXml.children(SheetXml.child(wbx, "definedNames"), "definedName")) {
            String local = SheetXml.attr(dn, "localSheetId");
            if (!local.isEmpty()) localNames.computeIfAbsent(Integer.parseInt(local), k -> new ArrayList<>()).add(dn);
            else addName(names, dn, null);
        }
        for (int i = 0; i < sheetElements.size(); i++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Cancelado");
            Element se = sheetElements.get(i);
            String rid = SheetXml.attr(se, "id");
            String[] rel = wbRels.get(rid);
            String name = SheetXml.attr(se, "name");
            SheetWorksheet ws = new SheetWorksheet(name);
            ws.setProperties(ws.properties().withVisibility(SheetVisibility.fromXml(SheetXml.attr(se, "state", "visible"))));
            if (rel == null || !rel[0].endsWith("/worksheet")) {
                if (rel != null && rel[0].endsWith("/chartsheet")) diagnostics.add("A planilha de gráfico \"" + name + "\" foi importada vazia.");
                sheets.add(ws);
                continue;
            }
            readWorksheet(rel[1], ws);
            sheets.add(ws);
        }
        SheetWorkbook wb = new SheetWorkbook(sheets, pool, WorkbookProperties.DEFAULT);
        for (Map.Entry<Integer, List<Element>> e : localNames.entrySet()) {
            if (e.getKey() >= sheets.size()) continue;
            SheetWorksheet ws = sheets.get(e.getKey());
            for (Element dn : e.getValue()) localName(ws, e.getKey(), dn, names);
        }
        Element protection = SheetXml.child(wbx, "workbookProtection");
        int active = SheetXml.intAttr(SheetXml.child(SheetXml.child(wbx, "bookViews"), "workbookView"), "activeTab", 0);
        Map<String, byte[]> preserved = new LinkedHashMap<>();
        for (String n : pkg.names()) if (n.startsWith("customXml/") || n.equals("docProps/custom.xml")) preserved.put(n, pkg.part(n));
        wb.setProperties(WorkbookProperties.DEFAULT.withNames(names).withDate1904(date1904).withTheme(theme).withActiveSheet(Math.max(0, Math.min(active, sheets.size() - 1)))
                .withProtectStructure(protection != null && SheetXml.boolAttr(protection, "lockStructure", false))
                .withProtectionHash(protection == null ? null : emptyToNull(SheetXml.attr(protection, "workbookPassword"))).withDiagnostics(diagnostics)
                .withTitle(title()).withPreserved(preserved));
        if (sheets.isEmpty()) wb.addSheet(0, new SheetWorksheet("Planilha1"));
        return wb;
    }

    private String title() {
        try {
            byte[] core = pkg.partOrNull("docProps/core.xml");
            if (core == null) return "";
            Element t = SheetXml.descendant(SheetXml.parse(core).getDocumentElement(), "title");
            return t == null ? "" : t.getTextContent();
        } catch (IOException e) { return ""; }
    }

    private static String emptyToNull(String s) { return s == null || s.isEmpty() ? null : s; }

    private void addName(List<DefinedName> names, Element dn, Integer scope) {
        String name = SheetXml.attr(dn, "name");
        if (name.startsWith("_xlnm.") || name.startsWith("_SheetEditor")) return;
        try {
            String formula = XlsxFormulas.fromFile(dn.getTextContent());
            if (DefinedName.isValidName(name)) names.add(new DefinedName(name, formula, scope, SheetXml.boolAttr(dn, "hidden", false), SheetXml.attr(dn, "comment")));
        } catch (RuntimeException e) {
            diagnostics.add("Nome definido ignorado: " + name);
        }
    }

    private void localName(SheetWorksheet ws, int sheet, Element dn, List<DefinedName> names) {
        String name = SheetXml.attr(dn, "name");
        String text = dn.getTextContent();
        String local = text.contains("!") ? text.substring(text.lastIndexOf('!') + 1) : text;
        try {
            switch (name) {
                case "_xlnm.Print_Area" -> ws.setProperties(ws.properties().withPrint(ws.properties().print().withPrintArea(CellRange.parse(local.split(",")[0]))));
                case "_xlnm.Print_Titles" -> {
                    PrintSettings ps = ws.properties().print();
                    for (String part : text.split(",")) {
                        String ref = part.contains("!") ? part.substring(part.lastIndexOf('!') + 1) : part;
                        CellRange r = CellRange.parse(ref);
                        if (r.isWholeRow()) ps = ps.withRepeatRowFirst(r.firstRow()).withRepeatRowLast(r.lastRow());
                        else if (r.isWholeColumn()) ps = ps.withRepeatColumnFirst(r.firstColumn()).withRepeatColumnLast(r.lastColumn());
                    }
                    ws.setProperties(ws.properties().withPrint(ps));
                }
                case "_xlnm._FilterDatabase" -> { }
                default -> {
                    ObjectMapper json = new ObjectMapper();
                    if (name.startsWith("_SheetEditorPivot")) {
                        PivotTable p = json.readValue(SheetXml.attr(dn, "comment"), PivotJson.class).toModel();
                        ws.setProperties(ws.properties().addPivot(p));
                    } else if (name.startsWith("_SheetEditorFilterView")) {
                        JsonNode node = json.readTree(SheetXml.attr(dn, "comment"));
                        Map<Integer, FilterCriteria> criteria = new HashMap<>();
                        node.get("criteria").fields().forEachRemaining(e -> {
                            try { criteria.put(Integer.parseInt(e.getKey()), json.treeToValue(e.getValue(), FilterJson.class).toModel()); } catch (Exception ignored) { }
                        });
                        ws.setProperties(ws.properties().withFilterViews(SheetProperties.add(ws.properties().filterViews(), new FilterView(null, node.get("name").asText(), new AutoFilter(CellRange.parse(local), criteria, null)))));
                    } else addName(names, dn, sheet);
                }
            }
        } catch (Exception e) {
            diagnostics.add("Nome local ignorado: " + name);
        }
    }

    private SheetTheme readTheme(byte[] data) {
        try {
            Element scheme = SheetXml.descendant(SheetXml.parse(data).getDocumentElement(), "clrScheme");
            if (scheme == null) return SheetTheme.OFFICE;
            String[] order = {"dk1", "lt1", "dk2", "lt2", "accent1", "accent2", "accent3", "accent4", "accent5", "accent6", "hlink", "folHlink"};
            List<Integer> colors = new ArrayList<>();
            for (String k : order) {
                Element c = SheetXml.child(scheme, k);
                Element srgb = SheetXml.child(c, "srgbClr"), sys = SheetXml.child(c, "sysClr");
                String hex = srgb != null ? SheetXml.attr(srgb, "val") : sys != null ? SheetXml.attr(sys, "lastClr") : "";
                colors.add(hex.isEmpty() ? SheetTheme.OFFICE.color(colors.size()) : 0xFF000000 | Integer.parseInt(hex, 16));
            }
            Element fonts = SheetXml.descendant(SheetXml.parse(data).getDocumentElement(), "fontScheme");
            String major = SheetXml.attr(SheetXml.descendant(SheetXml.child(fonts, "majorFont"), "latin"), "typeface");
            String minor = SheetXml.attr(SheetXml.descendant(SheetXml.child(fonts, "minorFont"), "latin"), "typeface");
            return new SheetTheme("Importado", colors, major.isEmpty() ? null : major, minor.isEmpty() ? null : minor);
        } catch (Exception e) {
            return SheetTheme.OFFICE;
        }
    }

    private void readSharedStrings(byte[] data) throws IOException {
        try {
            XMLStreamReader r = stax(data);
            StringBuilder b = null;
            boolean inT = false, inRph = false;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String n = r.getLocalName();
                    if (n.equals("si")) b = new StringBuilder();
                    else if (n.equals("t") && !inRph) inT = true;
                    else if (n.equals("rPh")) inRph = true;
                } else if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                    if (inT && b != null) b.append(r.getText());
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    String n = r.getLocalName();
                    if (n.equals("t")) inT = false;
                    else if (n.equals("rPh")) inRph = false;
                    else if (n.equals("si") && b != null) { sharedStrings.add(SheetXml.unescapeOoxml(b.toString())); b = null; }
                }
            }
        } catch (XMLStreamException e) { throw new IOException("Strings compartilhadas inválidas.", e); }
    }

    static XMLStreamReader stax(byte[] data) throws XMLStreamException {
        XMLInputFactory f = XMLInputFactory.newFactory();
        f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
        return f.createXMLStreamReader(new ByteArrayInputStream(data));
    }

    private static String a(XMLStreamReader r, String name) {
        for (int i = 0; i < r.getAttributeCount(); i++) if (name.equals(r.getAttributeLocalName(i))) return r.getAttributeValue(i);
        return "";
    }

    private static String a(XMLStreamReader r, String name, String fallback) { String v = a(r, name); return v.isEmpty() ? fallback : v; }
    private static boolean ab(XMLStreamReader r, String name, boolean fallback) { String v = a(r, name); return v.isEmpty() ? fallback : v.equals("1") || v.equalsIgnoreCase("true"); }
    private static double ad(XMLStreamReader r, String name, double fallback) { String v = a(r, name); try { return v.isEmpty() ? fallback : Double.parseDouble(v); } catch (NumberFormatException e) { return fallback; } }
    private static int ai(XMLStreamReader r, String name, int fallback) { return (int) ad(r, name, fallback); }

    static int px(double width) { return (int) Math.round(width * 7 + 5); }
    static int rowPx(double points) { return (int) Math.round(points / 0.75); }

    private void readWorksheet(String part, SheetWorksheet ws) throws IOException {
        byte[] data = pkg.part(part);
        Map<String, String[]> rels = rels(part);
        SheetProperties p = ws.properties();
        List<CellRange> merges = new ArrayList<>();
        Map<CellAddress, Hyperlink> links = new HashMap<>();
        List<ConditionalFormat> formats = new ArrayList<>();
        List<DataValidation> validations = new ArrayList<>();
        List<ProtectedRange> protectedRanges = new ArrayList<>();
        List<Scenario> scenarios = new ArrayList<>();
        List<Sparkline> sparklines = new ArrayList<>();
        Map<String, String[]> sharedFormulas = new HashMap<>();
        List<String> tableRels = new ArrayList<>();
        String drawingRel = null, commentsRel = null, threadedRel = null;
        PrintSettings print = PrintSettings.DEFAULT;
        AutoFilter filter = null;
        try {
            XMLStreamReader r = stax(data);
            Deque stack = new Deque();
            int row = -1, col = -1, style = 0, nextRow = 0, nextCol = 0;
            String type = "", formulaType = "", formulaRef = "", formulaSi = "", cm = "";
            StringBuilder text = new StringBuilder();
            String value = null, formula = null;
            boolean inIs = false, inT = false;
            List<CellRange> cfRanges = null;
            ConditionalRule.ConditionalRuleBuilder rule = null;
            List<ConditionalRule> rules = null;
            List<Threshold> thresholds = null;
            List<Integer> colors = null;
            List<String> cfFormulas = null;
            DataValidation.DataValidationBuilder validation = null;
            String validationF1 = null, validationF2 = null;
            Map<Integer, FilterCriteria> criteria = null;
            CellRange filterRange = null;
            int filterCol = -1;
            List<String> filterValues = null;
            boolean filterBlank = false;
            List<FilterCondition> customs = null;
            boolean customAnd = false;
            String scenarioName = null, scenarioComment = null;
            Map<CellAddress, CellValue> scenarioValues = null;
            String sparkType = null, sparkF = null;
            int sparkColor = 0xFF376092;
            boolean sparkMarkers = false, sparkHigh = false, sparkLow = false, sparkNeg = false;
            List<Integer> rowBreaks = new ArrayList<>(), colBreaks = new ArrayList<>();
            String brkKind = null;
            while (r.hasNext()) {
                int ev = r.next();
                if (ev == XMLStreamConstants.START_ELEMENT) {
                    String n = r.getLocalName();
                    stack.push(n);
                    switch (n) {
                        case "tabColor" -> { Integer c = color(r); if (c != null) p = p.withTabColor(c); }
                        case "sheetView" -> {
                            p = p.withShowGridlines(ab(r, "showGridLines", true)).withShowHeaders(ab(r, "showRowColHeaders", true)).withShowFormulas(ab(r, "showFormulas", false))
                                    .withShowZeros(ab(r, "showZeros", true)).withRightToLeft(ab(r, "rightToLeft", false)).withZoom(ad(r, "zoomScale", 100) / 100.0)
                                    .withViewMode(switch (a(r, "view")) { case "pageLayout" -> SheetViewMode.PAGE_LAYOUT; case "pageBreakPreview" -> SheetViewMode.PAGE_BREAK_PREVIEW; default -> SheetViewMode.NORMAL; });
                        }
                        case "pane" -> {
                            String state = a(r, "state");
                            if (state.equals("frozen") || state.equals("frozenSplit")) p = p.withFreeze(new FreezePane(ai(r, "ySplit", 0), ai(r, "xSplit", 0)));
                        }
                        case "sheetFormatPr" -> {
                            double h = ad(r, "defaultRowHeight", 15);
                            ws.rows().setDefaultSize(rowPx(h));
                            String dcw = a(r, "defaultColWidth");
                            if (!dcw.isEmpty()) ws.columns().setDefaultSize(px(Double.parseDouble(dcw)));
                            else { String base = a(r, "baseColWidth"); if (!base.isEmpty()) ws.columns().setDefaultSize(px(Double.parseDouble(base) + 0.71)); }
                        }
                        case "col" -> {
                            int min = ai(r, "min", 1) - 1, max = Math.min(16383, ai(r, "max", 1) - 1);
                            String w = a(r, "width");
                            boolean hidden = ab(r, "hidden", false);
                            int level = ai(r, "outlineLevel", 0);
                            int s = styles.styleFor(ai(r, "style", 0));
                            if (max - min > 16000 && w.isEmpty() && !hidden && level == 0 && s == 0) continue;
                            for (int c = min; c <= max; c++) {
                                if (!w.isEmpty() && (ab(r, "customWidth", false) || Math.abs(px(Double.parseDouble(w)) - ws.columns().defaultSize()) > 1)) ws.columns().setSize(c, Math.max(1, px(Double.parseDouble(w))));
                                if (hidden) ws.columns().setHidden(c, true);
                                if (level > 0) ws.columns().setOutlineLevel(c, level);
                                if (s > 0) ws.columns().setStyle(c, s);
                            }
                        }
                        case "row" -> {
                            row = a(r, "r").isEmpty() ? nextRow : ai(r, "r", 1) - 1;
                            nextRow = row + 1;
                            nextCol = 0;
                            if (ab(r, "customHeight", false) && !a(r, "ht").isEmpty()) ws.rows().setSize(row, Math.max(1, rowPx(ad(r, "ht", 15))));
                            if (ab(r, "hidden", false)) ws.rows().setHidden(row, true);
                            int level = ai(r, "outlineLevel", 0);
                            if (level > 0) ws.rows().setOutlineLevel(row, level);
                            if (ab(r, "customFormat", false)) ws.rows().setStyle(row, styles.styleFor(ai(r, "s", 0)));
                        }
                        case "c" -> {
                            String ref = a(r, "r");
                            if (ref.isEmpty()) col = nextCol;
                            else { CellAddress addr = CellAddress.parse(ref); row = addr.row(); col = addr.column(); }
                            nextCol = col + 1;
                            style = styles.styleFor(ai(r, "s", 0));
                            type = a(r, "t");
                            cm = a(r, "cm");
                            value = null; formula = null; formulaType = ""; formulaRef = ""; formulaSi = "";
                        }
                        case "f" -> { formulaType = a(r, "t"); formulaRef = a(r, "ref"); formulaSi = a(r, "si"); text.setLength(0); }
                        case "v" -> text.setLength(0);
                        case "is" -> { inIs = true; text.setLength(0); }
                        case "t" -> { if (inIs && !stack.contains("rPh")) inT = true; }
                        case "mergeCell" -> { try { merges.add(CellRange.parse(a(r, "ref"))); } catch (RuntimeException ignored) { } }
                        case "hyperlink" -> {
                            try {
                                CellRange range = CellRange.parse(a(r, "ref"));
                                String rid = a(r, "id");
                                String target = !rid.isEmpty() && rels.containsKey(rid) ? rels.get(rid)[1] : "#" + a(r, "location");
                                if (!rid.isEmpty() && !a(r, "location").isEmpty()) target += "#" + a(r, "location");
                                for (CellAddress addr : range) { links.put(addr, new Hyperlink(target, a(r, "tooltip"))); if (range.cellCount() > 100) break; }
                            } catch (RuntimeException ignored) { }
                        }
                        case "conditionalFormatting" -> { cfRanges = sqref(a(r, "sqref")); rules = new ArrayList<>(); }
                        case "cfRule" -> {
                            if (rules == null) break;
                            ConditionalRuleType t = ConditionalRuleType.fromXml(a(r, "type"));
                            String dxf = a(r, "dxfId");
                            DifferentialStyle ds = dxf.isEmpty() || Integer.parseInt(dxf) >= styles.dxfs.size() ? DifferentialStyle.EMPTY : styles.dxfs.get(Integer.parseInt(dxf));
                            String op = a(r, "operator");
                            rule = ConditionalRule.builder().type(t).style(ds).priority(ai(r, "priority", 1)).stopIfTrue(ab(r, "stopIfTrue", false))
                                    .operator(t == ConditionalRuleType.CELL_VALUE ? ComparisonOperator.fromXml(op) : ComparisonOperator.GREATER).text(a(r, "text"))
                                    .rank(ai(r, "rank", 10)).percent(ab(r, "percent", false)).bottom(ab(r, "bottom", false)).below(!ab(r, "aboveAverage", true))
                                    .equalAverage(ab(r, "equalAverage", false)).stdDev(ai(r, "stdDev", 0)).timePeriod(TimePeriod.fromXml(a(r, "timePeriod"))).showValue(true);
                            thresholds = new ArrayList<>(); colors = new ArrayList<>(); cfFormulas = new ArrayList<>();
                        }
                        case "dataBar" -> { if (rule != null) rule.showValue(ab(r, "showValue", true)).gradient(ab(r, "gradient", true)); }
                        case "iconSet" -> { if (rule != null) rule.iconSet(IconSetType.fromXml(a(r, "iconSet", "3TrafficLights1"))).reverseIcons(ab(r, "reverse", false)).showValue(ab(r, "showValue", true)); }
                        case "cfvo" -> { if (thresholds != null) thresholds.add(new Threshold(ThresholdType.fromXml(a(r, "type")), a(r, "val"), ab(r, "gte", true))); }
                        case "color" -> {
                            if (colors != null && (stack.contains("colorScale") || stack.contains("dataBar"))) { Integer c = color(r); if (c != null) colors.add(c); }
                            if (stack.contains("sparklineGroup") && stack.contains("colorSeries")) { Integer c = color(r); if (c != null) sparkColor = c; }
                        }
                        case "formula", "formula1", "formula2", "sqref" -> text.setLength(0);
                        case "dataValidation" -> {
                            String t = a(r, "type");
                            boolean checkbox = "sheetEditor:checkbox".equals(a(r, "promptTitle"));
                            validation = DataValidation.builder().ranges(sqref(a(r, "sqref"))).type(checkbox ? ValidationType.CHECKBOX : ValidationType.fromXml(t.isEmpty() ? "none" : t))
                                    .operator(ComparisonOperator.fromXml(a(r, "operator", "between"))).allowBlank(ab(r, "allowBlank", false)).showDropdown(!ab(r, "showDropDown", false))
                                    .errorStyle(ErrorAlertStyle.fromXml(a(r, "errorStyle", "stop"))).showInput(ab(r, "showInputMessage", false)).showError(ab(r, "showErrorMessage", false))
                                    .inputTitle(checkbox ? "" : a(r, "promptTitle")).inputMessage(a(r, "prompt")).errorTitle(a(r, "errorTitle")).errorMessage(a(r, "error"));
                            validationF1 = null; validationF2 = null;
                        }
                        case "autoFilter" -> {
                            if (stack.size() <= 2) { try { filterRange = CellRange.parse(a(r, "ref")); criteria = new HashMap<>(); } catch (RuntimeException ignored) { } }
                        }
                        case "filterColumn" -> { filterCol = ai(r, "colId", 0); filterValues = null; customs = null; }
                        case "filters" -> { filterValues = new ArrayList<>(); filterBlank = ab(r, "blank", false); }
                        case "filter" -> { if (filterValues != null) filterValues.add(a(r, "val")); }
                        case "customFilters" -> { customs = new ArrayList<>(); customAnd = ab(r, "and", false); }
                        case "customFilter" -> { if (customs != null) customs.add(customFilter(a(r, "operator"), a(r, "val"))); }
                        case "top10" -> { if (criteria != null && filterRange != null) criteria.put(filterRange.firstColumn() + filterCol, FilterCriteria.top((int) ad(r, "val", 10), ab(r, "percent", false), !ab(r, "top", true))); }
                        case "dynamicFilter" -> { if (criteria != null && filterRange != null) criteria.put(filterRange.firstColumn() + filterCol, FilterCriteria.dynamic(a(r, "type"))); }
                        case "sheetProtection" -> {
                            if (ab(r, "sheet", false)) p = p.withProtection(SheetProtection.NONE.toBuilder().enabled(true).passwordHash(emptyToNull(a(r, "password")))
                                    .formatCells(!ab(r, "formatCells", true)).formatColumns(!ab(r, "formatColumns", true)).formatRows(!ab(r, "formatRows", true))
                                    .insertColumns(!ab(r, "insertColumns", true)).insertRows(!ab(r, "insertRows", true)).insertHyperlinks(!ab(r, "insertHyperlinks", true))
                                    .deleteColumns(!ab(r, "deleteColumns", true)).deleteRows(!ab(r, "deleteRows", true)).sort(!ab(r, "sort", true)).autoFilter(!ab(r, "autoFilter", true))
                                    .pivotTables(!ab(r, "pivotTables", true)).objects(!ab(r, "objects", false)).scenarios(!ab(r, "scenarios", false))
                                    .selectLocked(!ab(r, "selectLockedCells", false)).selectUnlocked(!ab(r, "selectUnlockedCells", false)).build());
                        }
                        case "protectedRange" -> protectedRanges.add(new ProtectedRange(a(r, "name"), sqref(a(r, "sqref")), List.of(), emptyToNull(a(r, "password")), ""));
                        case "scenario" -> { scenarioName = a(r, "name"); scenarioComment = a(r, "comment"); scenarioValues = new LinkedHashMap<>(); }
                        case "inputCells" -> { if (scenarioValues != null) try { scenarioValues.put(CellAddress.parse(a(r, "r")), parseLiteral(a(r, "val"))); } catch (RuntimeException ignored) { } }
                        case "printOptions" -> print = print.withGridlines(ab(r, "gridLines", false)).withHeadings(ab(r, "headings", false)).withCenterHorizontally(ab(r, "horizontalCentered", false)).withCenterVertically(ab(r, "verticalCentered", false));
                        case "pageMargins" -> print = print.withMarginLeft(ad(r, "left", .7)).withMarginRight(ad(r, "right", .7)).withMarginTop(ad(r, "top", .75)).withMarginBottom(ad(r, "bottom", .75)).withMarginHeader(ad(r, "header", .3)).withMarginFooter(ad(r, "footer", .3));
                        case "pageSetup" -> {
                            print = print.withPaper(PaperSize.fromCode(ai(r, "paperSize", 9))).withScale(ai(r, "scale", 100)).withOrientation("landscape".equals(a(r, "orientation")) ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT)
                                    .withOverThenDown("overThenDown".equals(a(r, "pageOrder")));
                            if (!a(r, "fitToWidth").isEmpty() || !a(r, "fitToHeight").isEmpty()) print = print.withFitWidth(ai(r, "fitToWidth", 1)).withFitHeight(ai(r, "fitToHeight", 1));
                        }
                        case "fitToPage" -> { }
                        case "oddHeader", "oddFooter" -> text.setLength(0);
                        case "rowBreaks" -> brkKind = "row";
                        case "colBreaks" -> brkKind = "col";
                        case "brk" -> { if ("row".equals(brkKind)) rowBreaks.add(ai(r, "id", 0)); else if ("col".equals(brkKind)) colBreaks.add(ai(r, "id", 0)); }
                        case "drawing" -> drawingRel = a(r, "id");
                        case "legacyDrawing" -> { }
                        case "tablePart" -> tableRels.add(a(r, "id"));
                        case "sparklineGroup" -> { sparkType = a(r, "type"); sparkMarkers = ab(r, "markers", false); sparkHigh = ab(r, "high", false); sparkLow = ab(r, "low", false); sparkNeg = ab(r, "negative", false); }
                        case "colorSeries" -> { Integer c = color(r); if (c != null) sparkColor = c; }
                        default -> { }
                    }
                } else if (ev == XMLStreamConstants.CHARACTERS || ev == XMLStreamConstants.CDATA) {
                    String top = stack.peek();
                    if (top == null) continue;
                    if (top.equals("t") && inT || top.equals("v") || top.equals("f") || top.equals("formula") || top.equals("formula1") || top.equals("formula2")
                            || top.equals("oddHeader") || top.equals("oddFooter") || top.equals("sqref")) text.append(r.getText());
                } else if (ev == XMLStreamConstants.END_ELEMENT) {
                    String n = stack.pop();
                    switch (n) {
                        case "v" -> value = text.toString();
                        case "t" -> inT = false;
                        case "is" -> { inIs = false; value = text.toString(); }
                        case "f" -> {
                            String f = text.toString();
                            if (formulaType.equals("shared")) {
                                if (!f.isEmpty()) { formula = XlsxFormulas.fromFile(f); sharedFormulas.put(formulaSi, new String[]{formula, String.valueOf(row), String.valueOf(col)}); }
                                else {
                                    String[] master = sharedFormulas.get(formulaSi);
                                    if (master != null) {
                                        try { formula = ReferenceAdjuster.shift(master[0], row - Integer.parseInt(master[1]), col - Integer.parseInt(master[2])); } catch (RuntimeException e) { formula = master[0]; }
                                    }
                                }
                            } else if (!f.isEmpty()) formula = XlsxFormulas.fromFile(f);
                        }
                        case "c" -> {
                            CellValue v = cellValue(type, value);
                            if (formula != null && formula.toUpperCase(java.util.Locale.ROOT).startsWith("__XLUDF.DUMMYFUNCTION")) formula = null;
                            if (formula != null || !v.isEmpty() || style != 0) ws.put(row, col, new SheetCell(v, formula, style));
                        }
                        case "cfRule" -> {
                            if (rule != null && rules != null) {
                                rule.thresholds(thresholds).colors(colors);
                                if (!cfFormulas.isEmpty()) {
                                    ConditionalRuleType t = rule.build().type();
                                    if (t == ConditionalRuleType.CELL_VALUE || t == ConditionalRuleType.EXPRESSION) {
                                        rule.formula1(cfFormulas.getFirst());
                                        if (cfFormulas.size() > 1) rule.formula2(cfFormulas.get(1));
                                    }
                                }
                                rules.add(rule.build());
                                rule = null;
                            }
                        }
                        case "formula" -> { if (cfFormulas != null && stack.contains("cfRule")) cfFormulas.add(XlsxFormulas.fromFile(text.toString())); }
                        case "conditionalFormatting" -> { if (cfRanges != null && rules != null && !rules.isEmpty() && !cfRanges.isEmpty()) formats.add(new ConditionalFormat(cfRanges, rules)); cfRanges = null; rules = null; }
                        case "formula1" -> validationF1 = validationFormula(text.toString());
                        case "formula2" -> validationF2 = validationFormula(text.toString());
                        case "sqref" -> { if (validation != null && stack.contains("dataValidation")) validation.ranges(sqref(text.toString())); }
                        case "dataValidation" -> {
                            if (validation != null) {
                                DataValidation v = validation.formula1(validationF1).formula2(validationF2).build();
                                if (!v.ranges().isEmpty()) validations.add(v);
                                validation = null;
                            }
                        }
                        case "filters" -> { if (criteria != null && filterRange != null && filterValues != null) criteria.put(filterRange.firstColumn() + filterCol, FilterCriteria.values(Set.copyOf(filterValues), filterBlank)); }
                        case "customFilters" -> { if (criteria != null && filterRange != null && customs != null && !customs.isEmpty()) criteria.put(filterRange.firstColumn() + filterCol, FilterCriteria.conditions(customs, customAnd)); }
                        case "autoFilter" -> { if (filterRange != null && criteria != null && stack.size() <= 1) filter = new AutoFilter(filterRange, criteria, null); }
                        case "scenario" -> { if (scenarioName != null) scenarios.add(new Scenario(scenarioName, scenarioValues, scenarioComment)); scenarioName = null; }
                        case "oddHeader" -> { String[] hf = splitHeaderFooter(text.toString()); print = print.withHeaderLeft(hf[0]).withHeaderCenter(hf[1]).withHeaderRight(hf[2]); }
                        case "oddFooter" -> { String[] hf = splitHeaderFooter(text.toString()); print = print.withFooterLeft(hf[0]).withFooterCenter(hf[1]).withFooterRight(hf[2]); }
                        default -> { }
                    }
                    if (n.equals("f") && stack.contains("sparkline")) sparkF = text.toString();
                    if (n.equals("sqref") && stack.contains("sparkline") && sparkF != null) {
                        try {
                            String loc = text.toString().strip();
                            SparklineType st = "column".equals(sparkType) ? SparklineType.COLUMN : "stacked".equals(sparkType) ? SparklineType.WIN_LOSS : SparklineType.LINE;
                            sparklines.add(new Sparkline(CellAddress.parse(loc), sparkF.contains("!") ? sparkF.substring(sparkF.indexOf('!') + 1) : sparkF, st, sparkColor, sparkMarkers, sparkHigh, sparkLow, sparkNeg));
                        } catch (RuntimeException ignored) { }
                        sparkF = null;
                    }
                }
            }
            print = print.withRowBreaks(rowBreaks).withColumnBreaks(colBreaks);
        } catch (XMLStreamException e) {
            throw new IOException("Planilha inválida: " + ws.name(), e);
        }
        for (Map.Entry<String, String[]> e : rels.entrySet()) {
            if (e.getValue()[0].endsWith("/comments")) commentsRel = e.getValue()[1];
            if (e.getValue()[0].endsWith("/threadedComment")) threadedRel = e.getValue()[1];
        }
        List<SheetTable> tables = new ArrayList<>();
        for (String rid : tableRels) {
            String[] rel = rels.get(rid);
            if (rel != null && pkg.contains(rel[1])) try { tables.add(table(pkg.part(rel[1]))); } catch (RuntimeException ex) { diagnostics.add("Tabela ignorada em " + ws.name()); }
        }
        List<SheetObject> objects = new ArrayList<>();
        if (drawingRel != null && rels.containsKey(drawingRel)) objects.addAll(drawing(rels.get(drawingRel)[1], ws));
        Map<CellAddress, SheetNote> notes = commentsRel == null || !pkg.contains(commentsRel) ? Map.of() : comments(pkg.part(commentsRel));
        Map<CellAddress, SheetThread> threads = threadedRel == null || !pkg.contains(threadedRel) ? Map.of() : threads(pkg.part(threadedRel));
        if (!threads.isEmpty()) {
            Map<CellAddress, SheetNote> filtered = new HashMap<>(notes);
            filtered.keySet().removeAll(threads.keySet());
            notes = filtered;
        }
        ws.setProperties(p.withMerges(merges).withLinks(links).withConditionalFormats(formats).withValidations(validations).withProtectedRanges(protectedRanges)
                .withScenarios(scenarios).withSparklines(sparklines).withPrint(print).withAutoFilter(filter).withTables(tables).withObjects(objects).withNotes(notes).withThreads(threads));
    }

    private static final class Deque {
        private final ArrayList<String> items = new ArrayList<>();
        void push(String s) { items.add(s); }
        String pop() { return items.isEmpty() ? null : items.removeLast(); }
        String peek() { return items.isEmpty() ? null : items.getLast(); }
        boolean contains(String s) { return items.contains(s); }
        int size() { return items.size(); }
    }

    private Integer color(XMLStreamReader r) {
        String rgb = a(r, "rgb"), th = a(r, "theme"), ix = a(r, "indexed");
        Integer base = null;
        if (!rgb.isEmpty()) try { base = (int) Long.parseLong(rgb.length() == 6 ? "FF" + rgb : rgb, 16); } catch (NumberFormatException ignored) { }
        if (!th.isEmpty()) { int idx = Integer.parseInt(th); base = theme.color(switch (idx) { case 0 -> 1; case 1 -> 0; case 2 -> 3; case 3 -> 2; default -> idx; }); }
        if (!ix.isEmpty()) { int idx = Integer.parseInt(ix); base = idx >= 0 && idx < XlsxColors.INDEXED.length ? XlsxColors.INDEXED[idx] : null; }
        if (base == null) return null;
        double tint = ad(r, "tint", 0);
        return tint == 0 ? base : SheetTheme.tint(base, tint);
    }

    private CellValue cellValue(String type, String value) {
        if (value == null) return CellValue.EMPTY;
        try {
            return switch (type) {
                case "s" -> { int idx = Integer.parseInt(value.strip()); yield idx >= 0 && idx < sharedStrings.size() ? new TextValue(sharedStrings.get(idx)) : CellValue.EMPTY; }
                case "b" -> CellValue.of(value.strip().equals("1") || value.strip().equalsIgnoreCase("true"));
                case "e" -> CellValue.error(CellError.parse(value).orElse(CellError.VALUE));
                case "str", "inlineStr" -> new TextValue(SheetXml.unescapeOoxml(value));
                case "d" -> CellValue.of(DateSerial.toSerial(LocalDateTime.parse(value.length() == 10 ? value + "T00:00:00" : value.replace("Z", "")), date1904));
                default -> value.isEmpty() ? CellValue.EMPTY : CellValue.of(Double.parseDouble(value));
            };
        } catch (RuntimeException e) {
            return new TextValue(value);
        }
    }

    private static CellValue parseLiteral(String v) {
        try { return CellValue.of(Double.parseDouble(v)); } catch (NumberFormatException e) { return new TextValue(v); }
    }

    private static List<CellRange> sqref(String sqref) {
        List<CellRange> list = new ArrayList<>();
        for (String s : sqref.strip().split("\\s+")) { if (s.isEmpty()) continue; try { list.add(CellRange.parse(s)); } catch (RuntimeException ignored) { } }
        return list;
    }

    private static String validationFormula(String text) {
        String t = text.strip();
        if (t.startsWith("\"")) return t;
        return XlsxFormulas.fromFile(t);
    }

    private static FilterCondition customFilter(String operator, String value) {
        if (operator.isEmpty() || operator.equals("equal")) {
            if (value.startsWith("*") && value.endsWith("*") && value.length() > 1) return new FilterCondition(FilterOperator.CONTAINS, value.substring(1, value.length() - 1));
            if (value.endsWith("*")) return new FilterCondition(FilterOperator.BEGINS_WITH, value.substring(0, value.length() - 1));
            if (value.startsWith("*")) return new FilterCondition(FilterOperator.ENDS_WITH, value.substring(1));
            return new FilterCondition(FilterOperator.EQUAL, value);
        }
        if (operator.equals("notEqual") && value.startsWith("*") && value.endsWith("*")) return new FilterCondition(FilterOperator.NOT_CONTAINS, value.substring(1, value.length() - 1));
        return new FilterCondition(FilterOperator.fromXml(operator), value);
    }

    static String[] splitHeaderFooter(String s) {
        String[] out = {"", "", ""};
        int section = 1;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' && i + 1 < s.length() && "LCR".indexOf(s.charAt(i + 1)) >= 0) {
                out[section] += b.toString();
                b.setLength(0);
                section = "LCR".indexOf(s.charAt(i + 1));
                i++;
                continue;
            }
            b.append(c);
        }
        out[section] += b.toString();
        return out;
    }

    private SheetTable table(byte[] data) throws IOException {
        Element t = SheetXml.parse(data).getDocumentElement();
        CellRange range = CellRange.parse(SheetXml.attr(t, "ref"));
        List<TableColumn> cols = new ArrayList<>();
        for (Element c : SheetXml.children(SheetXml.child(t, "tableColumns"), "tableColumn")) {
            Element calc = SheetXml.child(c, "calculatedColumnFormula");
            cols.add(new TableColumn(SheetXml.unescapeOoxml(SheetXml.attr(c, "name")), TotalsFunction.fromXml(SheetXml.attr(c, "totalsRowFunction", "none")),
                    emptyToNull(SheetXml.attr(c, "totalsRowLabel")), calc == null ? null : XlsxFormulas.fromFile(calc.getTextContent())));
        }
        while (cols.size() < range.columnCount()) cols.add(TableColumn.of("Coluna" + (cols.size() + 1)));
        while (cols.size() > range.columnCount()) cols.removeLast();
        Element info = SheetXml.child(t, "tableStyleInfo");
        String name = SheetXml.attr(t, "displayName", SheetXml.attr(t, "name"));
        return new SheetTable(SheetXml.intAttr(t, "id", 1), name, range, SheetXml.intAttr(t, "headerRowCount", 1) > 0, SheetXml.intAttr(t, "totalsRowCount", 0) > 0,
                SheetXml.attr(info, "name", "TableStyleMedium2"), cols, SheetXml.boolAttr(info, "showRowStripes", true), SheetXml.boolAttr(info, "showColumnStripes", false),
                SheetXml.boolAttr(info, "showFirstColumn", false), SheetXml.boolAttr(info, "showLastColumn", false), SheetXml.child(t, "autoFilter") != null);
    }

    private Map<CellAddress, SheetNote> comments(byte[] data) throws IOException {
        Element root = SheetXml.parse(data).getDocumentElement();
        List<String> authors = new ArrayList<>();
        for (Element a : SheetXml.children(SheetXml.child(root, "authors"), "author")) authors.add(a.getTextContent());
        Map<CellAddress, SheetNote> notes = new HashMap<>();
        for (Element c : SheetXml.children(SheetXml.child(root, "commentList"), "comment")) {
            try {
                int author = SheetXml.intAttr(c, "authorId", 0);
                StringBuilder b = new StringBuilder();
                for (Element t : SheetXml.descendants(SheetXml.child(c, "text"), "t")) b.append(t.getTextContent());
                notes.put(CellAddress.parse(SheetXml.attr(c, "ref")), new SheetNote(author < authors.size() ? authors.get(author) : "", SheetXml.unescapeOoxml(b.toString()), false));
            } catch (RuntimeException ignored) { }
        }
        return notes;
    }

    private Map<CellAddress, SheetThread> threads(byte[] data) throws IOException {
        Element root = SheetXml.parse(data).getDocumentElement();
        Map<CellAddress, List<SheetComment>> map = new LinkedHashMap<>();
        Map<CellAddress, Boolean> resolved = new HashMap<>();
        for (Element c : SheetXml.children(root, "threadedComment")) {
            try {
                CellAddress a = CellAddress.parse(SheetXml.attr(c, "ref"));
                java.time.Instant when = SheetXml.attr(c, "dT").isEmpty() ? java.time.Instant.now() : LocalDateTime.parse(SheetXml.attr(c, "dT").replace("Z", "").substring(0, 19)).toInstant(java.time.ZoneOffset.UTC);
                map.computeIfAbsent(a, k -> new ArrayList<>()).add(new SheetComment(SheetXml.attr(c, "id"), SheetXml.attr(c, "personId"), SheetXml.text(SheetXml.child(c, "text")), when));
                if (SheetXml.boolAttr(c, "done", false)) resolved.put(a, true);
            } catch (RuntimeException ignored) { }
        }
        Map<CellAddress, SheetThread> out = new HashMap<>();
        map.forEach((a, list) -> out.put(a, new SheetThread(list, resolved.getOrDefault(a, false))));
        return out;
    }

    private List<SheetObject> drawing(String part, SheetWorksheet ws) throws IOException {
        List<SheetObject> out = new ArrayList<>();
        if (!pkg.contains(part)) return out;
        Map<String, String[]> rels = rels(part);
        Element root = SheetXml.parse(pkg.part(part)).getDocumentElement();
        for (Element anchor : SheetXml.children(root)) {
            String kind = anchor.getLocalName();
            if (!kind.endsWith("Anchor")) continue;
            ObjectAnchor oa = anchor(anchor, ws);
            Element frame = SheetXml.child(anchor, "graphicFrame"), pic = SheetXml.child(anchor, "pic"), sp = SheetXml.child(anchor, "sp");
            try {
                if (frame != null) {
                    Element chartRef = SheetXml.descendant(frame, "chart");
                    String rid = SheetXml.attr(chartRef, "id");
                    String[] rel = rels.get(rid);
                    if (rel != null && pkg.contains(rel[1])) out.add(XlsxCharts.read(SheetXml.parse(pkg.part(rel[1])).getDocumentElement(), oa));
                } else if (pic != null) {
                    Element blip = SheetXml.descendant(pic, "blip");
                    String rid = SheetXml.attr(blip, "embed");
                    String[] rel = rels.get(rid);
                    if (rel != null && pkg.contains(rel[1])) {
                        String ext = rel[1].substring(rel[1].lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
                        out.add(new SheetImage(null, pkg.part(rel[1]), ext, oa, SheetXml.attr(SheetXml.descendant(pic, "cNvPr"), "descr")));
                    }
                } else if (sp != null) {
                    String prst = SheetXml.attr(SheetXml.descendant(sp, "prstGeom"), "prst");
                    ShapeType type = switch (prst) { case "ellipse" -> ShapeType.ELLIPSE; case "roundRect" -> ShapeType.ROUNDED_RECTANGLE; case "triangle" -> ShapeType.TRIANGLE; case "rightArrow" -> ShapeType.ARROW_RIGHT; case "line" -> ShapeType.LINE; default -> SheetXml.boolAttr(SheetXml.descendant(sp, "cNvSpPr"), "txBox", false) ? ShapeType.TEXT_BOX : ShapeType.RECTANGLE; };
                    StringBuilder b = new StringBuilder();
                    for (Element t : SheetXml.descendants(SheetXml.child(sp, "txBody"), "t")) b.append(t.getTextContent());
                    Integer fill = srgb(SheetXml.path(sp, "spPr", "solidFill")), line = srgb(SheetXml.path(sp, "spPr", "ln", "solidFill"));
                    out.add(new SheetShape(null, type, oa, b.toString(), fill, line, 1));
                }
            } catch (RuntimeException ex) {
                diagnostics.add("Objeto de desenho ignorado em " + ws.name());
            }
        }
        return out;
    }

    private static Integer srgb(Element fill) {
        Element c = SheetXml.child(fill, "srgbClr");
        if (c == null) return null;
        try { return 0xFF000000 | Integer.parseInt(SheetXml.attr(c, "val"), 16); } catch (NumberFormatException e) { return null; }
    }

    private static ObjectAnchor anchor(Element anchor, SheetWorksheet ws) {
        Element from = SheetXml.child(anchor, "from"), to = SheetXml.child(anchor, "to"), ext = SheetXml.child(anchor, "ext");
        int col = from == null ? 0 : Integer.parseInt(SheetXml.text(SheetXml.child(from, "col")).strip());
        int row = from == null ? 0 : Integer.parseInt(SheetXml.text(SheetXml.child(from, "row")).strip());
        int colOff = from == null ? 0 : (int) (Long.parseLong(SheetXml.text(SheetXml.child(from, "colOff")).strip()) / XlsxWriter.EMU);
        int rowOff = from == null ? 0 : (int) (Long.parseLong(SheetXml.text(SheetXml.child(from, "rowOff")).strip()) / XlsxWriter.EMU);
        int width = 480, height = 288;
        if (ext != null) { width = (int) (SheetXml.doubleAttr(ext, "cx", width * XlsxWriter.EMU) / XlsxWriter.EMU); height = (int) (SheetXml.doubleAttr(ext, "cy", height * XlsxWriter.EMU) / XlsxWriter.EMU); }
        else if (to != null) {
            int tc = Integer.parseInt(SheetXml.text(SheetXml.child(to, "col")).strip()), tr = Integer.parseInt(SheetXml.text(SheetXml.child(to, "row")).strip());
            int tco = (int) (Long.parseLong(SheetXml.text(SheetXml.child(to, "colOff")).strip()) / XlsxWriter.EMU), tro = (int) (Long.parseLong(SheetXml.text(SheetXml.child(to, "rowOff")).strip()) / XlsxWriter.EMU);
            width = (int) (ws.columns().position(tc) + tco - ws.columns().position(col) - colOff);
            height = (int) (ws.rows().position(tr) + tro - ws.rows().position(row) - rowOff);
        }
        return new ObjectAnchor(row, col, colOff, rowOff, Math.max(8, width), Math.max(8, height));
    }
}
