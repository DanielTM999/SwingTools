package dtm.stools.component.panels.editor.sheet.io;

import dtm.stools.component.panels.editor.sheet.formula.FormulaPrinter;
import dtm.stools.component.panels.editor.sheet.io.ooxml.SheetXml;
import dtm.stools.component.panels.editor.sheet.io.ooxml.XmlBuilder;
import dtm.stools.component.panels.editor.sheet.model.ChartSeries;
import dtm.stools.component.panels.editor.sheet.model.ChartType;
import dtm.stools.component.panels.editor.sheet.model.LegendPosition;
import dtm.stools.component.panels.editor.sheet.model.ObjectAnchor;
import dtm.stools.component.panels.editor.sheet.model.SheetChart;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

final class XlsxCharts {
    private XlsxCharts() {}

    static byte[] write(SheetChart chart, String sheet) {
        XmlBuilder x = new XmlBuilder();
        x.open("c:chartSpace", "xmlns:c", "http://schemas.openxmlformats.org/drawingml/2006/chart", "xmlns:a", "http://schemas.openxmlformats.org/drawingml/2006/main",
                "xmlns:r", XlsxWriter.R);
        x.empty("c:roundedCorners", "val", "0");
        x.open("c:chart");
        if (!chart.title().isEmpty()) {
            x.open("c:title").open("c:tx").open("c:rich").empty("a:bodyPr").open("a:p").open("a:r").element("a:t", chart.title()).close().close().close().close();
            x.empty("c:overlay", "val", "0").close();
            x.empty("c:autoTitleDeleted", "val", "0");
        } else x.empty("c:autoTitleDeleted", "val", "1");
        x.open("c:plotArea").empty("c:layout");
        ChartType type = chart.type();
        List<ChartSeries> primary = new ArrayList<>(), secondary = new ArrayList<>();
        for (ChartSeries s : chart.series()) (s.type() != null && s.type() != type && type == ChartType.COMBO ? secondary : primary).add(s);
        ChartType base = type == ChartType.COMBO ? ChartType.COLUMN : type;
        group(x, base, primary, sheet, chart.dataLabels(), 0);
        for (ChartSeries s : secondary) group(x, s.type(), List.of(s), sheet, chart.dataLabels(), primary.size() + secondary.indexOf(s));
        if (!base.circular()) {
            if (base.xy()) {
                valAx(x, 100, 200, "b", chart.xAxisTitle(), false, null, null);
                valAx(x, 200, 100, "l", chart.yAxisTitle(), chart.gridlines(), chart.minimum(), chart.maximum());
            } else {
                boolean horizontal = base.horizontal();
                x.open("c:catAx").empty("c:axId", "val", "100").open("c:scaling").empty("c:orientation", "val", "minMax").close().empty("c:delete", "val", "0")
                        .empty("c:axPos", "val", horizontal ? "l" : "b");
                axisTitle(x, chart.xAxisTitle());
                x.empty("c:numFmt", "formatCode", "General", "sourceLinked", "1").empty("c:tickLblPos", "val", "nextTo").empty("c:crossAx", "val", "200").empty("c:crosses", "val", "autoZero")
                        .empty("c:auto", "val", "1").empty("c:lblAlgn", "val", "ctr").empty("c:lblOffset", "val", "100").close();
                valAx(x, 200, 100, horizontal ? "b" : "l", chart.yAxisTitle(), chart.gridlines(), chart.minimum(), chart.maximum());
            }
        }
        x.close();
        if (chart.legend() != LegendPosition.NONE) {
            String pos = switch (chart.legend()) { case RIGHT -> "r"; case TOP -> "t"; case LEFT -> "l"; default -> "b"; };
            x.open("c:legend").empty("c:legendPos", "val", pos).empty("c:overlay", "val", "0").close();
        }
        x.empty("c:plotVisOnly", "val", "1").empty("c:dispBlanksAs", "val", "gap");
        x.close();
        x.close();
        return x.bytes();
    }

    private static void axisTitle(XmlBuilder x, String title) {
        if (title == null || title.isEmpty()) return;
        x.open("c:title").open("c:tx").open("c:rich").empty("a:bodyPr").open("a:p").open("a:r").element("a:t", title).close().close().close().close().empty("c:overlay", "val", "0").close();
    }

    private static void valAx(XmlBuilder x, int id, int cross, String pos, String title, boolean gridlines, Double min, Double max) {
        x.open("c:valAx").empty("c:axId", "val", String.valueOf(id)).open("c:scaling").empty("c:orientation", "val", "minMax");
        if (max != null) x.empty("c:max", "val", String.valueOf(max));
        if (min != null) x.empty("c:min", "val", String.valueOf(min));
        x.close().empty("c:delete", "val", "0").empty("c:axPos", "val", pos);
        if (gridlines) x.empty("c:majorGridlines");
        axisTitle(x, title);
        x.empty("c:numFmt", "formatCode", "General", "sourceLinked", "1").empty("c:tickLblPos", "val", "nextTo").empty("c:crossAx", "val", String.valueOf(cross))
                .empty("c:crosses", "val", "autoZero").empty("c:crossBetween", "val", "between").close();
    }

    private static void group(XmlBuilder x, ChartType type, List<ChartSeries> series, String sheet, boolean labels, int offset) {
        String tag = switch (type) {
            case BAR, STACKED_BAR, COLUMN, STACKED_COLUMN, PERCENT_COLUMN, HISTOGRAM, PARETO, WATERFALL, FUNNEL -> "c:barChart";
            case LINE, LINE_MARKERS, STACKED_LINE, STOCK -> "c:lineChart";
            case AREA, STACKED_AREA -> "c:areaChart";
            case PIE -> "c:pieChart";
            case DOUGHNUT -> "c:doughnutChart";
            case SCATTER, SCATTER_LINES -> "c:scatterChart";
            case BUBBLE -> "c:bubbleChart";
            case RADAR, FILLED_RADAR -> "c:radarChart";
            default -> "c:barChart";
        };
        x.open(tag);
        if (tag.equals("c:barChart")) {
            x.empty("c:barDir", "val", type.horizontal() ? "bar" : "col");
            x.empty("c:grouping", "val", type == ChartType.PERCENT_COLUMN ? "percentStacked" : type.stacked() ? "stacked" : "clustered");
            x.empty("c:varyColors", "val", "0");
        } else if (tag.equals("c:lineChart") || tag.equals("c:areaChart")) {
            x.empty("c:grouping", "val", type.stacked() ? "stacked" : "standard");
            x.empty("c:varyColors", "val", "0");
        } else if (tag.equals("c:scatterChart")) {
            x.empty("c:scatterStyle", "val", type == ChartType.SCATTER_LINES ? "lineMarker" : "lineMarker");
            x.empty("c:varyColors", "val", "0");
        } else if (tag.equals("c:radarChart")) {
            x.empty("c:radarStyle", "val", type == ChartType.FILLED_RADAR ? "filled" : "marker");
            x.empty("c:varyColors", "val", "0");
        } else x.empty("c:varyColors", "val", "1");
        int idx = offset;
        for (ChartSeries s : series) {
            x.open("c:ser").empty("c:idx", "val", String.valueOf(idx)).empty("c:order", "val", String.valueOf(idx));
            idx++;
            if (s.nameRef() != null) x.open("c:tx").open("c:strRef").element("c:f", ref(s.nameRef(), sheet)).close().close();
            else if (s.name() != null) x.open("c:tx").element("c:v", s.name()).close();
            if (s.color() != null) {
                x.open("c:spPr");
                if (tag.equals("c:lineChart") || tag.equals("c:scatterChart")) x.open("a:ln", "w", "28575").open("a:solidFill").empty("a:srgbClr", "val", XlsxColors.hex(s.color()).substring(2)).close().close();
                else x.open("a:solidFill").empty("a:srgbClr", "val", XlsxColors.hex(s.color()).substring(2)).close();
                x.close();
            }
            if (tag.equals("c:lineChart")) x.open("c:marker").empty("c:symbol", "val", type == ChartType.LINE_MARKERS ? "circle" : "none").close();
            if (tag.equals("c:scatterChart") && type == ChartType.SCATTER) x.open("c:spPr").open("a:ln", "w", "19050").empty("a:noFill").close().close();
            if (labels) x.open("c:dLbls").empty("c:showLegendKey", "val", "0").empty("c:showVal", "val", "1").empty("c:showCatName", "val", "0").empty("c:showSerName", "val", "0").empty("c:showPercent", "val", "0").empty("c:showBubbleSize", "val", "0").close();
            if (type.xy()) {
                if (s.categoriesRef() != null) x.open("c:xVal").open("c:numRef").element("c:f", ref(s.categoriesRef(), sheet)).close().close();
                if (s.valuesRef() != null) x.open("c:yVal").open("c:numRef").element("c:f", ref(s.valuesRef(), sheet)).close().close();
                if (type == ChartType.BUBBLE && s.sizesRef() != null) x.open("c:bubbleSize").open("c:numRef").element("c:f", ref(s.sizesRef(), sheet)).close().close();
                if (tag.equals("c:scatterChart")) x.empty("c:smooth", "val", "0");
            } else {
                if (s.categoriesRef() != null) x.open("c:cat").open("c:strRef").element("c:f", ref(s.categoriesRef(), sheet)).close().close();
                if (s.valuesRef() != null) x.open("c:val").open("c:numRef").element("c:f", ref(s.valuesRef(), sheet)).close().close();
                if (tag.equals("c:lineChart")) x.empty("c:smooth", "val", "0");
            }
            x.close();
        }
        if (tag.equals("c:barChart")) { x.empty("c:gapWidth", "val", "150"); if (type.stacked()) x.empty("c:overlap", "val", "100"); }
        if (tag.equals("c:doughnutChart")) { x.empty("c:firstSliceAng", "val", "0"); x.empty("c:holeSize", "val", "50"); }
        if (tag.equals("c:pieChart")) x.empty("c:firstSliceAng", "val", "0");
        if (!tag.equals("c:pieChart") && !tag.equals("c:doughnutChart")) { x.empty("c:axId", "val", "100"); x.empty("c:axId", "val", "200"); }
        x.close();
    }

    private static String ref(String ref, String sheet) {
        String r = ref.startsWith("=") ? ref.substring(1) : ref;
        return r.contains("!") ? r : FormulaPrinter.sheet(sheet) + "!" + r;
    }

    static SheetChart read(Element space, ObjectAnchor anchor) {
        Element chart = SheetXml.child(space, "chart");
        Element plot = SheetXml.child(chart, "plotArea");
        String title = "";
        Element t = SheetXml.child(chart, "title");
        if (t != null) { StringBuilder b = new StringBuilder(); for (Element e : SheetXml.descendants(t, "t")) b.append(e.getTextContent()); title = b.toString(); }
        List<ChartSeries> series = new ArrayList<>();
        ChartType type = null;
        for (Element group : SheetXml.children(plot)) {
            String name = group.getLocalName();
            if (!name.endsWith("Chart")) continue;
            ChartType groupType = switch (name) {
                case "barChart", "bar3DChart" -> {
                    boolean bar = "bar".equals(SheetXml.attr(SheetXml.child(group, "barDir"), "val"));
                    String grouping = SheetXml.attr(SheetXml.child(group, "grouping"), "val");
                    yield bar ? (grouping.contains("tacked") ? ChartType.STACKED_BAR : ChartType.BAR) : grouping.equals("percentStacked") ? ChartType.PERCENT_COLUMN : grouping.equals("stacked") ? ChartType.STACKED_COLUMN : ChartType.COLUMN;
                }
                case "lineChart", "line3DChart" -> "stacked".equals(SheetXml.attr(SheetXml.child(group, "grouping"), "val")) ? ChartType.STACKED_LINE : ChartType.LINE_MARKERS;
                case "areaChart", "area3DChart" -> "stacked".equals(SheetXml.attr(SheetXml.child(group, "grouping"), "val")) ? ChartType.STACKED_AREA : ChartType.AREA;
                case "pieChart", "pie3DChart", "ofPieChart" -> ChartType.PIE;
                case "doughnutChart" -> ChartType.DOUGHNUT;
                case "scatterChart" -> ChartType.SCATTER;
                case "bubbleChart" -> ChartType.BUBBLE;
                case "radarChart" -> "filled".equals(SheetXml.attr(SheetXml.child(group, "radarStyle"), "val")) ? ChartType.FILLED_RADAR : ChartType.RADAR;
                case "stockChart" -> ChartType.STOCK;
                default -> ChartType.COLUMN;
            };
            if (type == null) type = groupType; else if (type != groupType) type = ChartType.COMBO;
            for (Element ser : SheetXml.children(group, "ser")) {
                String nameRef = f(SheetXml.path(ser, "tx", "strRef"));
                String sname = nameRef == null ? SheetXml.text(SheetXml.path(ser, "tx", "v")) : null;
                String cat = f(SheetXml.child(ser, "cat")), val = f(SheetXml.child(ser, "val"));
                if (cat == null) cat = f(SheetXml.child(ser, "xVal"));
                if (val == null) val = f(SheetXml.child(ser, "yVal"));
                String sizes = f(SheetXml.child(ser, "bubbleSize"));
                Integer color = null;
                Element clr = SheetXml.descendant(SheetXml.child(ser, "spPr"), "srgbClr");
                if (clr != null) try { color = 0xFF000000 | Integer.parseInt(SheetXml.attr(clr, "val"), 16); } catch (NumberFormatException ignored) { }
                series.add(new ChartSeries(sname, nameRef, cat, val, sizes, color, groupType, false));
            }
        }
        Element legend = SheetXml.child(chart, "legend");
        LegendPosition lp = legend == null ? LegendPosition.NONE : switch (SheetXml.attr(SheetXml.child(legend, "legendPos"), "val")) { case "r" -> LegendPosition.RIGHT; case "t" -> LegendPosition.TOP; case "l" -> LegendPosition.LEFT; default -> LegendPosition.BOTTOM; };
        boolean grid = !SheetXml.descendants(plot, "majorGridlines").isEmpty();
        return SheetChart.builder().type(type == null ? ChartType.COLUMN : type).anchor(anchor).title(title).series(series).legend(lp).gridlines(grid)
                .dataLabels(!SheetXml.descendants(plot, "showVal").isEmpty() && SheetXml.descendants(plot, "showVal").stream().anyMatch(e -> SheetXml.boolAttr(e, "val", false))).build();
    }

    private static String f(Element parent) {
        Element e = SheetXml.descendant(parent, "f");
        return e == null ? null : e.getTextContent();
    }
}
