package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.render.WordPaintSupport;
import org.w3c.dom.Element;
import java.util.*;

final class DocxChartXml {
    private static final Set<String> TYPES = Set.of("barChart","lineChart","areaChart","pieChart","doughnutChart","scatterChart","radarChart");
    private DocxChartXml() {}

    static WordChart read(Element chartSpace, String id, float width, float height, String altText, WordPlacement placement) {
        Element chart = OoxmlXml.child(chartSpace,"chart"), plot = OoxmlXml.child(chart,"plotArea");
        if (plot == null) return null;
        List<Element> kinds = new ArrayList<>();
        for (Element e : OoxmlXml.children(plot)) if (e.getLocalName().endsWith("Chart")) kinds.add(e);
        if (kinds.size() != 1 || !TYPES.contains(kinds.getFirst().getLocalName())) return null;
        Element kind = kinds.getFirst();
        WordChartType type = switch (kind.getLocalName()) {
            case "barChart" -> {
                boolean bar = "bar".equals(val(OoxmlXml.child(kind,"barDir")));
                String grouping = val(OoxmlXml.child(kind,"grouping"));
                if (grouping.equals("percentStacked")) yield bar ? null : WordChartType.COLUMN_PERCENT;
                boolean stacked = grouping.equals("stacked");
                yield bar ? stacked ? WordChartType.BAR_STACKED : WordChartType.BAR_CLUSTERED : stacked ? WordChartType.COLUMN_STACKED : WordChartType.COLUMN_CLUSTERED;
            }
            case "lineChart" -> {
                String grouping = val(OoxmlXml.child(kind,"grouping"));
                if (grouping.equals("stacked") || grouping.equals("percentStacked")) yield null;
                boolean markers = false;
                for (Element s : OoxmlXml.children(kind,"ser")) { Element symbol = OoxmlXml.path(s,"marker","symbol"); if (symbol == null || !"none".equals(val(symbol))) markers = true; }
                yield markers ? WordChartType.LINE_MARKERS : WordChartType.LINE;
            }
            case "areaChart" -> switch (val(OoxmlXml.child(kind,"grouping"))) { case "stacked" -> WordChartType.AREA_STACKED; case "percentStacked" -> null; default -> WordChartType.AREA; };
            case "pieChart" -> WordChartType.PIE;
            case "doughnutChart" -> WordChartType.DOUGHNUT;
            case "scatterChart" -> WordChartType.SCATTER;
            default -> WordChartType.RADAR;
        };
        if (type == null) return null;
        List<String> categories = new ArrayList<>(); List<WordChartSeries> series = new ArrayList<>(); boolean labels = false;
        for (Element s : OoxmlXml.children(kind,"ser")) {
            Element cat = OoxmlXml.child(s,type == WordChartType.SCATTER ? "xVal" : "cat"), values = OoxmlXml.child(s,type == WordChartType.SCATTER ? "yVal" : "val");
            List<String> cats = points(cat); if (categories.isEmpty()) categories.addAll(cats);
            List<String> raw = points(values); List<Double> numbers = new ArrayList<>();
            for (String v : raw) { try { numbers.add(v.isEmpty() ? Double.NaN : Double.parseDouble(v)); } catch (NumberFormatException e) { numbers.add(Double.NaN); } }
            String name = OoxmlXml.text(OoxmlXml.child(s,"tx"));
            if (name.isEmpty()) { Element v = OoxmlXml.descendant(OoxmlXml.child(s,"tx"),"v"); name = v == null ? "Série " + (series.size()+1) : v.getTextContent(); }
            Integer color = color(OoxmlXml.child(s,"spPr"));
            Element show = OoxmlXml.path(s,"dLbls","showVal");
            if (show != null && !"0".equals(val(show))) labels = true;
            series.add(new WordChartSeries(name,numbers,color));
        }
        if (series.isEmpty()) return null;
        while (categories.size() < series.getFirst().values().size()) categories.add(Integer.toString(categories.size()+1));
        String title = "";
        Element titleElement = OoxmlXml.child(chart,"title");
        if (titleElement != null) title = OoxmlXml.text(titleElement);
        if (titleElement != null && title.isEmpty() && series.size() == 1) title = series.getFirst().name();
        Element legend = OoxmlXml.child(chart,"legend");
        WordChart.LegendPosition position = switch (val(OoxmlXml.child(legend,"legendPos"))) { case "b" -> WordChart.LegendPosition.BOTTOM; case "t" -> WordChart.LegendPosition.TOP; case "l" -> WordChart.LegendPosition.LEFT; default -> WordChart.LegendPosition.RIGHT; };
        String catTitle = "", valTitle = "";
        for (Element axis : OoxmlXml.children(plot)) {
            String t = OoxmlXml.text(OoxmlXml.child(axis,"title"));
            if (axis.getLocalName().equals("catAx") || axis.getLocalName().equals("dateAx")) catTitle = t;
            if (axis.getLocalName().equals("valAx")) { String pos = val(OoxmlXml.child(axis,"axPos")); if (type == WordChartType.SCATTER && pos.equals("b")) catTitle = t; else valTitle = t; }
        }
        try {
            return new WordChart(id,type,title,categories,series,legend != null,position,labels,catTitle,valTitle,Math.max(36,width),Math.max(36,height),altText,placement);
        } catch (IllegalArgumentException e) { return null; }
    }
    private static List<String> points(Element holder) {
        List<String> result = new ArrayList<>();
        if (holder == null) return result;
        Element cache = null;
        for (String name : new String[]{"strCache","numCache","strLit","numLit"}) { cache = OoxmlXml.descendant(holder,name); if (cache != null) break; }
        if (cache == null) return result;
        int count = 0; Element pc = OoxmlXml.child(cache,"ptCount");
        try { count = pc == null ? 0 : Integer.parseInt(val(pc)); } catch (NumberFormatException ignored) {}
        count = Math.min(count,4000);
        Map<Integer,String> values = new TreeMap<>();
        for (Element pt : OoxmlXml.children(cache,"pt")) {
            try { values.put(Integer.parseInt(pt.getAttribute("idx")),OoxmlXml.text(pt).isEmpty() ? Objects.requireNonNullElse(OoxmlXml.child(pt,"v"),pt).getTextContent() : OoxmlXml.text(pt)); } catch (NumberFormatException ignored) {}
        }
        int size = Math.max(count,values.isEmpty() ? 0 : ((TreeMap<Integer,String>)values).lastKey()+1);
        for (int i = 0; i < Math.min(size,4000); i++) result.add(values.getOrDefault(i,""));
        return result;
    }
    private static Integer color(Element spPr) {
        if (spPr == null) return null;
        Element fill = OoxmlXml.child(spPr,"solidFill");
        if (fill == null) fill = OoxmlXml.path(spPr,"ln","solidFill");
        Element rgb = OoxmlXml.child(fill,"srgbClr");
        if (rgb == null || !rgb.getAttribute("val").matches("[0-9a-fA-F]{6}")) return null;
        return Integer.parseInt(rgb.getAttribute("val"),16);
    }
    private static String val(Element e) { return e == null ? "" : e.getAttribute("val"); }

    static String write(WordChart chart, String workbookRelId) {
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        b.append("<c:chartSpace xmlns:c=\"").append(DocxNames.C).append("\" xmlns:a=\"").append(DocxNames.A).append("\" xmlns:r=\"").append(DocxNames.R).append("\">");
        b.append("<c:date1904 val=\"0\"/><c:roundedCorners val=\"0\"/><c:chart>");
        if (!chart.title().isBlank()) b.append("<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/><a:p><a:pPr><a:defRPr sz=\"1400\" b=\"1\"/></a:pPr><a:r><a:rPr lang=\"pt-BR\" sz=\"1400\" b=\"1\"/><a:t>")
                .append(OoxmlXml.escape(chart.title())).append("</a:t></a:r></a:p></c:rich></c:tx><c:overlay val=\"0\"/></c:title><c:autoTitleDeleted val=\"0\"/>");
        else b.append("<c:autoTitleDeleted val=\"1\"/>");
        b.append("<c:plotArea><c:layout/>");
        WordChartType type = chart.chartType();
        String tag = switch (type) {
            case COLUMN_CLUSTERED, COLUMN_STACKED, COLUMN_PERCENT, BAR_CLUSTERED, BAR_STACKED -> "barChart";
            case LINE, LINE_MARKERS -> "lineChart";
            case AREA, AREA_STACKED -> "areaChart";
            case PIE -> "pieChart";
            case DOUGHNUT -> "doughnutChart";
            case SCATTER -> "scatterChart";
            case RADAR -> "radarChart";
        };
        b.append("<c:").append(tag).append('>');
        switch (type) {
            case COLUMN_CLUSTERED, COLUMN_STACKED, COLUMN_PERCENT, BAR_CLUSTERED, BAR_STACKED ->
                    b.append("<c:barDir val=\"").append(type.isBar() ? "bar" : "col").append("\"/><c:grouping val=\"").append(type.isPercent() ? "percentStacked" : type.isStacked() ? "stacked" : "clustered").append("\"/><c:varyColors val=\"0\"/>");
            case LINE, LINE_MARKERS -> b.append("<c:grouping val=\"standard\"/><c:varyColors val=\"0\"/>");
            case AREA, AREA_STACKED -> b.append("<c:grouping val=\"").append(type.isStacked() ? "stacked" : "standard").append("\"/><c:varyColors val=\"0\"/>");
            case PIE, DOUGHNUT -> b.append("<c:varyColors val=\"1\"/>");
            case SCATTER -> b.append("<c:scatterStyle val=\"lineMarker\"/><c:varyColors val=\"0\"/>");
            case RADAR -> b.append("<c:radarStyle val=\"marker\"/><c:varyColors val=\"0\"/>");
        }
        int categories = chart.categories().size();
        String lastRow = Integer.toString(categories+1);
        for (int s = 0; s < chart.series().size(); s++) {
            if (type.isCircular() && s > 0) break;
            WordChartSeries series = chart.series().get(s);
            String column = XlsxWriter.column(s+1), color = hex(series.color() == null ? WordPaintSupport.PALETTE[s % WordPaintSupport.PALETTE.length] : series.color());
            b.append("<c:ser><c:idx val=\"").append(s).append("\"/><c:order val=\"").append(s).append("\"/>");
            b.append("<c:tx><c:strRef><c:f>Sheet1!$").append(column).append("$1</c:f><c:strCache><c:ptCount val=\"1\"/><c:pt idx=\"0\"><c:v>").append(OoxmlXml.escape(series.name())).append("</c:v></c:pt></c:strCache></c:strRef></c:tx>");
            if (type.isLine() || type == WordChartType.RADAR) b.append("<c:spPr><a:ln w=\"28575\" cap=\"rnd\"><a:solidFill><a:srgbClr val=\"").append(color).append("\"/></a:solidFill><a:round/></a:ln></c:spPr>");
            else if (type == WordChartType.SCATTER) b.append("<c:spPr><a:ln w=\"25400\"><a:noFill/></a:ln></c:spPr>");
            else if (!type.isCircular()) b.append("<c:spPr><a:solidFill><a:srgbClr val=\"").append(color).append("\"/></a:solidFill></c:spPr>");
            if (type.isColumnOrBar()) b.append("<c:invertIfNegative val=\"0\"/>");
            if (type.isLine() || type == WordChartType.SCATTER || type == WordChartType.RADAR) {
                boolean markers = type != WordChartType.LINE;
                b.append("<c:marker><c:symbol val=\"").append(markers ? "circle" : "none").append("\"/>");
                if (markers) b.append("<c:size val=\"6\"/><c:spPr><a:solidFill><a:srgbClr val=\"").append(color).append("\"/></a:solidFill></c:spPr>");
                b.append("</c:marker>");
            }
            if (type.isCircular()) for (int c = 0; c < categories; c++)
                b.append("<c:dPt><c:idx val=\"").append(c).append("\"/><c:bubble3D val=\"0\"/><c:spPr><a:solidFill><a:srgbClr val=\"").append(hex(WordPaintSupport.PALETTE[c % WordPaintSupport.PALETTE.length])).append("\"/></a:solidFill><a:ln><a:solidFill><a:srgbClr val=\"FFFFFF\"/></a:solidFill></a:ln></c:spPr></c:dPt>");
            if (chart.dataLabels()) b.append("<c:dLbls><c:showLegendKey val=\"0\"/><c:showVal val=\"").append(type.isCircular() ? 0 : 1).append("\"/><c:showCatName val=\"0\"/><c:showSerName val=\"0\"/><c:showPercent val=\"").append(type.isCircular() ? 1 : 0).append("\"/><c:showBubbleSize val=\"0\"/></c:dLbls>");
            String catTag = type == WordChartType.SCATTER ? "xVal" : "cat", valTag = type == WordChartType.SCATTER ? "yVal" : "val";
            b.append("<c:").append(catTag).append("><c:strRef><c:f>Sheet1!$A$2:$A$").append(lastRow).append("</c:f><c:strCache><c:ptCount val=\"").append(categories).append("\"/>");
            for (int c = 0; c < categories; c++) b.append("<c:pt idx=\"").append(c).append("\"><c:v>").append(OoxmlXml.escape(chart.categories().get(c))).append("</c:v></c:pt>");
            b.append("</c:strCache></c:strRef></c:").append(catTag).append('>');
            b.append("<c:").append(valTag).append("><c:numRef><c:f>Sheet1!$").append(column).append("$2:$").append(column).append('$').append(lastRow).append("</c:f><c:numCache><c:formatCode>General</c:formatCode><c:ptCount val=\"").append(categories).append("\"/>");
            for (int c = 0; c < categories; c++) { double v = series.value(c); if (!Double.isNaN(v)) b.append("<c:pt idx=\"").append(c).append("\"><c:v>").append(XlsxWriter.number(v)).append("</c:v></c:pt>"); }
            b.append("</c:numCache></c:numRef></c:").append(valTag).append('>');
            if (type.isLine() || type == WordChartType.SCATTER) b.append("<c:smooth val=\"0\"/>");
            b.append("</c:ser>");
        }
        if (type.isColumnOrBar()) b.append("<c:gapWidth val=\"150\"/>").append(type.isStacked() ? "<c:overlap val=\"100\"/>" : "");
        if (type.isLine()) b.append("<c:marker val=\"1\"/>");
        if (type == WordChartType.PIE || type == WordChartType.DOUGHNUT) b.append("<c:firstSliceAng val=\"0\"/>");
        if (type == WordChartType.DOUGHNUT) b.append("<c:holeSize val=\"50\"/>");
        if (!type.isCircular()) b.append("<c:axId val=\"500000001\"/><c:axId val=\"500000002\"/>");
        b.append("</c:").append(tag).append('>');
        if (!type.isCircular()) {
            boolean bar = type.isBar();
            if (type == WordChartType.SCATTER) {
                axis(b,"valAx","500000001","500000002","b",chart.categoryAxisTitle(),false);
                axis(b,"valAx","500000002","500000001","l",chart.valueAxisTitle(),true);
            } else {
                axis(b,"catAx","500000001","500000002",bar ? "l" : "b",chart.categoryAxisTitle(),false);
                axis(b,"valAx","500000002","500000001",bar ? "b" : "l",chart.valueAxisTitle(),true);
            }
        }
        b.append("<c:spPr><a:noFill/><a:ln><a:noFill/></a:ln></c:spPr></c:plotArea>");
        if (chart.legend()) b.append("<c:legend><c:legendPos val=\"").append(switch (chart.legendPosition()) { case BOTTOM -> "b"; case TOP -> "t"; case LEFT -> "l"; default -> "r"; }).append("\"/><c:overlay val=\"0\"/></c:legend>");
        b.append("<c:plotVisOnly val=\"1\"/><c:dispBlanksAs val=\"gap\"/></c:chart>");
        b.append("<c:spPr><a:solidFill><a:srgbClr val=\"FFFFFF\"/></a:solidFill><a:ln w=\"9525\"><a:solidFill><a:srgbClr val=\"D9D9D9\"/></a:solidFill></a:ln></c:spPr>");
        if (workbookRelId != null) b.append("<c:externalData r:id=\"").append(workbookRelId).append("\"><c:autoUpdate val=\"0\"/></c:externalData>");
        b.append("</c:chartSpace>");
        return b.toString();
    }
    private static void axis(StringBuilder b, String tag, String id, String cross, String position, String title, boolean grid) {
        b.append("<c:").append(tag).append("><c:axId val=\"").append(id).append("\"/><c:scaling><c:orientation val=\"minMax\"/></c:scaling><c:delete val=\"0\"/><c:axPos val=\"").append(position).append("\"/>");
        if (grid) b.append("<c:majorGridlines><c:spPr><a:ln w=\"9525\"><a:solidFill><a:srgbClr val=\"D9D9D9\"/></a:solidFill></a:ln></c:spPr></c:majorGridlines>");
        if (!title.isBlank()) b.append("<c:title><c:tx><c:rich><a:bodyPr/><a:lstStyle/><a:p><a:r><a:t>").append(OoxmlXml.escape(title)).append("</a:t></a:r></a:p></c:rich></c:tx><c:overlay val=\"0\"/></c:title>");
        b.append("<c:numFmt formatCode=\"General\" sourceLinked=\"1\"/><c:majorTickMark val=\"none\"/><c:minorTickMark val=\"none\"/><c:tickLblPos val=\"nextTo\"/><c:crossAx val=\"").append(cross).append("\"/><c:crosses val=\"autoZero\"/>");
        if (tag.equals("catAx")) b.append("<c:auto val=\"1\"/><c:lblAlgn val=\"ctr\"/><c:lblOffset val=\"100\"/><c:noMultiLvlLbl val=\"0\"/>");
        else b.append("<c:crossBetween val=\"").append(position.equals("b") && tag.equals("valAx") && cross.equals("500000002") ? "midCat" : "between").append("\"/>");
        b.append("</c:").append(tag).append('>');
    }
    private static String hex(int rgb) { return String.format(Locale.ROOT,"%06X",rgb & 0xffffff); }
}
