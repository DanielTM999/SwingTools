package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.model.*;
import org.w3c.dom.Element;
import java.io.IOException;
import java.util.*;

final class DocxDrawingXml {
    static final float EMU = 12700f;
    private static final Map<String,Integer> SCHEME = Map.ofEntries(Map.entry("accent1",0x4472C4),Map.entry("accent2",0xED7D31),Map.entry("accent3",0xA5A5A5),
            Map.entry("accent4",0xFFC000),Map.entry("accent5",0x5B9BD5),Map.entry("accent6",0x70AD47),Map.entry("dk1",0x000000),Map.entry("lt1",0xFFFFFF),
            Map.entry("tx1",0x000000),Map.entry("bg1",0xFFFFFF),Map.entry("dk2",0x44546A),Map.entry("lt2",0xE7E6E6),Map.entry("tx2",0x44546A),Map.entry("bg2",0xE7E6E6));
    private DocxDrawingXml() {}

    static WordInlineObject read(Element drawing, DocxReadContext context) throws IOException {
        Element container = OoxmlXml.child(drawing,"inline");
        boolean floating = false;
        if (container == null) { container = OoxmlXml.child(drawing,"anchor"); floating = true; }
        if (container == null) return null;
        Element extent = OoxmlXml.child(container,"extent");
        float width = emu(extent,"cx",72), height = emu(extent,"cy",72);
        Element docPr = OoxmlXml.child(container,"docPr");
        String alt = docPr == null ? "" : docPr.getAttribute("descr");
        String name = docPr == null ? "" : docPr.getAttribute("name");
        WordPlacement placement = floating ? placement(container,width,context) : WordPlacement.INLINE;
        if (placement == null) return null;
        Element data = OoxmlXml.path(container,"graphic","graphicData");
        if (data == null) return null;
        String uri = data.getAttribute("uri");
        try {
            if (uri.endsWith("/picture")) {
                Element pic = OoxmlXml.child(data,"pic");
                Element blip = OoxmlXml.path(pic,"blipFill","blip");
                String embed = OoxmlXml.attr(blip,DocxNames.R,"embed");
                if (embed.isEmpty()) return null;
                String resource = context.resource(embed);
                if (name.startsWith("swingtools:")) {
                    WordInlineObject custom = custom(name.substring("swingtools:".length()),resource,width,height,alt,placement,context);
                    if (custom != null) return custom;
                }
                Element rect = OoxmlXml.path(pic,"blipFill","srcRect");
                WordCrop crop = WordCrop.NONE;
                if (rect != null) crop = new WordCrop(fraction(rect,"l"),fraction(rect,"t"),fraction(rect,"r"),fraction(rect,"b"));
                float rotation = rotation(OoxmlXml.path(pic,"spPr","xfrm"));
                Element locks = OoxmlXml.path(container,"cNvGraphicFramePr","graphicFrameLocks");
                return new WordImage(WordIds.next(),resource,Math.max(1,width),Math.max(1,height),crop,rotation,alt,placement,locks == null || !"0".equals(locks.getAttribute("noChangeAspect")));
            }
            if (uri.endsWith("/chart")) {
                Element chart = OoxmlXml.child(data,"chart");
                String id = OoxmlXml.attr(chart,DocxNames.R,"id");
                if (id.isEmpty()) return null;
                return DocxChartXml.read(context.partXml(id),WordIds.next(),width,height,alt,placement);
            }
            if (uri.equals(DocxNames.WPS)) {
                WordShape shape = shape(OoxmlXml.child(data,"wsp"),width,height,0,0);
                return shape == null ? null : shape.withPlacement(placement).withAltText(alt);
            }
            if (uri.equals(DocxNames.WPG)) {
                WordShape group = group(OoxmlXml.child(data,"wgp"),width,height);
                return group == null ? null : group.withPlacement(placement).withAltText(alt);
            }
        } catch (IllegalArgumentException e) { return null; }
        return null;
    }
    private static WordInlineObject custom(String id, String preview, float width, float height, String alt, WordPlacement placement, DocxReadContext context) {
        Map<String,String> data = context.state.customData.get(id);
        if (data == null) return null;
        Map<String,String> copy = new TreeMap<>(data);
        String type = copy.remove("$type");
        if (type == null) return null;
        try {
            if (type.equals("swingtools.diagram")) {
                WordDiagram d = WordDiagram.parse(WordDiagramLayout.valueOf(copy.getOrDefault("layout","BASIC_LIST")),copy.getOrDefault("outline",""));
                return d.withColor(Integer.parseInt(copy.getOrDefault("color","4472C4"),16)).resize(Math.max(48,width),Math.max(36,height)).withAltText(alt).withPlacement(placement).withId(id);
            }
            return new WordCustomObject(id,type,copy,Math.max(4,width),Math.max(4,height),alt,placement,preview);
        } catch (IllegalArgumentException e) { return null; }
    }
    private static WordPlacement placement(Element anchor, float width, DocxReadContext context) {
        WordPlacement.Wrap wrap = WordPlacement.Wrap.SQUARE;
        if (OoxmlXml.child(anchor,"wrapTopAndBottom") != null) wrap = WordPlacement.Wrap.TOP_AND_BOTTOM;
        else if (OoxmlXml.child(anchor,"wrapTight") != null || OoxmlXml.child(anchor,"wrapThrough") != null) wrap = WordPlacement.Wrap.TIGHT;
        else if (OoxmlXml.child(anchor,"wrapNone") != null) wrap = "1".equals(anchor.getAttribute("behindDoc")) || "true".equals(anchor.getAttribute("behindDoc")) ? WordPlacement.Wrap.BEHIND_TEXT : WordPlacement.Wrap.IN_FRONT_OF_TEXT;
        Element h = OoxmlXml.child(anchor,"positionH"), v = OoxmlXml.child(anchor,"positionV");
        String hFrom = h == null ? "margin" : h.getAttribute("relativeFrom"), vFrom = v == null ? "paragraph" : v.getAttribute("relativeFrom");
        WordPlacement.Anchor ha = hFrom.equals("page") ? WordPlacement.Anchor.PAGE : WordPlacement.Anchor.MARGIN;
        WordPlacement.Anchor va = switch (vFrom) { case "page" -> WordPlacement.Anchor.PAGE; case "margin", "topMargin" -> WordPlacement.Anchor.MARGIN; default -> WordPlacement.Anchor.PARAGRAPH; };
        float x = offset(h), y = offset(v);
        Element alignH = OoxmlXml.child(h,"align"), alignV = OoxmlXml.child(v,"align");
        if (alignH != null) {
            float area = ha == WordPlacement.Anchor.PAGE ? context.state.settings.width() : context.state.settings.contentWidth();
            x = switch (alignH.getTextContent().trim()) { case "center" -> (area-width)/2; case "right", "outside" -> area-width; default -> 0; };
        }
        if (alignV != null) {
            float area = va == WordPlacement.Anchor.PAGE ? context.state.settings.height() : va == WordPlacement.Anchor.MARGIN ? context.state.settings.contentHeight() : 0;
            y = switch (alignV.getTextContent().trim()) { case "center" -> Math.max(0,area/2); case "bottom", "outside" -> Math.max(0,area-36); default -> 0; };
        }
        int z = 0;
        try { z = (int)Math.min(100000,Long.parseLong(anchor.getAttribute("relativeHeight"))/1024); } catch (NumberFormatException ignored) {}
        try { return new WordPlacement(true,ha,x,va,y,wrap,z); } catch (IllegalArgumentException e) { return null; }
    }
    private static float offset(Element position) {
        Element off = OoxmlXml.child(position,"posOffset");
        if (off == null) return 0;
        try { return Long.parseLong(off.getTextContent().trim())/EMU; } catch (NumberFormatException e) { return 0; }
    }
    private static float emu(Element e, String name, float fallback) {
        if (e == null) return fallback;
        try { return Math.max(0,Math.min(14400,Long.parseLong(e.getAttribute(name))/EMU)); } catch (NumberFormatException ex) { return fallback; }
    }
    private static float fraction(Element e, String name) {
        try { String v = e.getAttribute(name); return v.isEmpty() ? 0 : Math.max(0,Math.min(0.45f,Integer.parseInt(v)/100000f)); } catch (NumberFormatException ex) { return 0; }
    }
    private static float rotation(Element xfrm) {
        if (xfrm == null || xfrm.getAttribute("rot").isEmpty()) return 0;
        try { return Long.parseLong(xfrm.getAttribute("rot"))/60000f; } catch (NumberFormatException e) { return 0; }
    }
    private static Integer color(Element holder) {
        if (holder == null) return null;
        Element rgb = OoxmlXml.child(holder,"srgbClr");
        if (rgb != null && rgb.getAttribute("val").matches("[0-9a-fA-F]{6}")) return Integer.parseInt(rgb.getAttribute("val"),16);
        Element scheme = OoxmlXml.child(holder,"schemeClr");
        return scheme == null ? null : SCHEME.get(scheme.getAttribute("val"));
    }
    static WordShape shape(Element wsp, float width, float height, float x, float y) {
        if (wsp == null) return null;
        Element spPr = OoxmlXml.child(wsp,"spPr");
        Element geometry = OoxmlXml.child(spPr,"prstGeom");
        if (geometry == null) return null;
        WordShapeType type = WordShapeType.fromPreset(geometry.getAttribute("prst"));
        if (type == null) return null;
        Element nv = OoxmlXml.child(wsp,"cNvSpPr");
        if (nv != null && ("1".equals(nv.getAttribute("txBox")) || "true".equals(nv.getAttribute("txBox"))) && type == WordShapeType.RECTANGLE) type = WordShapeType.TEXT_BOX;
        Element style = OoxmlXml.child(wsp,"style");
        Integer fill = OoxmlXml.child(spPr,"noFill") != null ? null : color(OoxmlXml.child(spPr,"solidFill"));
        if (fill == null && OoxmlXml.child(spPr,"noFill") == null && OoxmlXml.child(spPr,"solidFill") == null && style != null) fill = color(OoxmlXml.child(style,"fillRef"));
        Element ln = OoxmlXml.child(spPr,"ln");
        Integer stroke = ln != null && OoxmlXml.child(ln,"noFill") != null ? null : color(OoxmlXml.child(ln,"solidFill"));
        if (stroke == null && (ln == null || OoxmlXml.child(ln,"noFill") == null) && style != null) stroke = color(OoxmlXml.child(style,"lnRef"));
        float strokeWidth = 0.75f;
        if (ln != null && !ln.getAttribute("w").isEmpty()) try { strokeWidth = Math.min(50,Long.parseLong(ln.getAttribute("w"))/EMU); } catch (NumberFormatException ignored) {}
        boolean arrow = ln != null && OoxmlXml.child(ln,"tailEnd") != null && !"none".equals(OoxmlXml.child(ln,"tailEnd").getAttribute("type"));
        Element content = OoxmlXml.path(wsp,"txbx","txbxContent");
        String text = ""; float fontSize = 11; int textColor = type == WordShapeType.TEXT_BOX ? 0x111111 : 0xFFFFFF;
        if (content != null) {
            for (Element e : OoxmlXml.children(content)) if (!e.getLocalName().equals("p")) return null;
            List<String> lines = new ArrayList<>();
            for (Element p : OoxmlXml.children(content,"p")) {
                for (Element r : OoxmlXml.children(p)) if (!Set.of("pPr","r","proofErr","bookmarkStart","bookmarkEnd").contains(r.getLocalName())) return null;
                lines.add(OoxmlXml.text(p));
            }
            text = String.join("\n",lines);
            Element sz = OoxmlXml.descendant(content,"sz");
            if (sz != null) try { fontSize = Math.max(1,Float.parseFloat(OoxmlXml.attr(sz,"val"))/2); } catch (NumberFormatException ignored) {}
            Element c = OoxmlXml.descendant(content,"color");
            if (c != null && OoxmlXml.attr(c,"val").matches("[0-9a-fA-F]{6}")) textColor = Integer.parseInt(OoxmlXml.attr(c,"val"),16);
        }
        Element xfrm = OoxmlXml.child(spPr,"xfrm");
        return new WordShape(WordIds.next(),type,x,y,Math.max(1,width),Math.max(type.isLinear() ? 0 : 1,height),fill,stroke,strokeWidth,text,fontSize,textColor,rotation(xfrm),arrow,List.of(),"",WordPlacement.INLINE);
    }
    private static WordShape group(Element wgp, float width, float height) {
        if (wgp == null) return null;
        Element xfrm = OoxmlXml.path(wgp,"grpSpPr","xfrm");
        float chX = 0, chY = 0, chW = width, chH = height;
        Element chOff = OoxmlXml.child(xfrm,"chOff"), chExt = OoxmlXml.child(xfrm,"chExt");
        if (chOff != null) { chX = emu(chOff,"x",0); chY = emu(chOff,"y",0); }
        if (chExt != null) { chW = Math.max(1,emu(chExt,"cx",width)); chH = Math.max(1,emu(chExt,"cy",height)); }
        float sx = width/chW, sy = height/chH;
        List<WordShape> children = new ArrayList<>();
        for (Element child : OoxmlXml.children(wgp)) {
            if (child.getLocalName().equals("cNvGrpSpPr") || child.getLocalName().equals("grpSpPr") || child.getLocalName().equals("cNvPr")) continue;
            if (!child.getLocalName().equals("wsp")) return null;
            Element cx = OoxmlXml.path(child,"spPr","xfrm");
            Element off = OoxmlXml.child(cx,"off"), ext = OoxmlXml.child(cx,"ext");
            float x = (emu(off,"x",0)-chX)*sx, y = (emu(off,"y",0)-chY)*sy, w = emu(ext,"cx",10)*sx, h = emu(ext,"cy",10)*sy;
            WordShape shape = shape(child,w,h,x,y);
            if (shape == null) return null;
            children.add(shape);
        }
        if (children.isEmpty()) return null;
        return new WordShape(WordIds.next(),WordShapeType.GROUP,0,0,Math.max(1,width),Math.max(1,height),null,null,0,"",11,0,rotation(xfrm),false,children,"",WordPlacement.INLINE);
    }

    static long emu(float points) { return Math.round(points*EMU); }
    static String wrap(WordInlineObject object, int docPrId, String name, String uri, String graphic) {
        WordPlacement p = object.placement();
        long cx = emu(Math.max(1,object.width())), cy = emu(Math.max(1,object.height()));
        String docPr = "<wp:docPr id=\"" + docPrId + "\" name=\"" + OoxmlXml.escape(name) + "\"" + (object.altText().isBlank() ? "" : " descr=\"" + OoxmlXml.escape(object.altText()) + "\"") + "/>";
        String frame = "<wp:cNvGraphicFramePr>" + (object instanceof WordImage ? "<a:graphicFrameLocks xmlns:a=\"" + DocxNames.A + "\" noChangeAspect=\"1\"/>" : "") + "</wp:cNvGraphicFramePr>";
        String body = "<a:graphic xmlns:a=\"" + DocxNames.A + "\"><a:graphicData uri=\"" + uri + "\">" + graphic + "</a:graphicData></a:graphic>";
        if (!p.floating()) return "<w:drawing><wp:inline distT=\"0\" distB=\"0\" distL=\"0\" distR=\"0\"><wp:extent cx=\"" + cx + "\" cy=\"" + cy + "\"/><wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>" + docPr + frame + body + "</wp:inline></w:drawing>";
        boolean behind = p.wrap() == WordPlacement.Wrap.BEHIND_TEXT;
        String wrap = switch (p.wrap()) {
            case TOP_AND_BOTTOM -> "<wp:wrapTopAndBottom/>";
            case TIGHT -> "<wp:wrapTight wrapText=\"bothSides\"><wp:wrapPolygon edited=\"0\"><wp:start x=\"0\" y=\"0\"/><wp:lineTo x=\"0\" y=\"21600\"/><wp:lineTo x=\"21600\" y=\"21600\"/><wp:lineTo x=\"21600\" y=\"0\"/><wp:lineTo x=\"0\" y=\"0\"/></wp:wrapPolygon></wp:wrapTight>";
            case BEHIND_TEXT, IN_FRONT_OF_TEXT -> "<wp:wrapNone/>";
            default -> "<wp:wrapSquare wrapText=\"bothSides\"/>";
        };
        String hFrom = p.horizontalFrom() == WordPlacement.Anchor.PAGE ? "page" : "margin";
        String vFrom = switch (p.verticalFrom()) { case PAGE -> "page"; case MARGIN -> "margin"; default -> "paragraph"; };
        return "<w:drawing><wp:anchor distT=\"0\" distB=\"0\" distL=\"114300\" distR=\"114300\" simplePos=\"0\" relativeHeight=\"" + (251658240L + Math.max(0,p.zOrder())*1024L) + "\" behindDoc=\"" + (behind ? 1 : 0)
                + "\" locked=\"0\" layoutInCell=\"1\" allowOverlap=\"1\"><wp:simplePos x=\"0\" y=\"0\"/><wp:positionH relativeFrom=\"" + hFrom + "\"><wp:posOffset>" + emu(p.x()) + "</wp:posOffset></wp:positionH><wp:positionV relativeFrom=\""
                + vFrom + "\"><wp:posOffset>" + emu(p.y()) + "</wp:posOffset></wp:positionV><wp:extent cx=\"" + cx + "\" cy=\"" + cy + "\"/><wp:effectExtent l=\"0\" t=\"0\" r=\"0\" b=\"0\"/>" + wrap + docPr + frame + body + "</wp:anchor></w:drawing>";
    }
    static String picture(String relationshipId, float width, float height, WordCrop crop, float rotation, String name) {
        StringBuilder b = new StringBuilder("<pic:pic xmlns:pic=\"").append(DocxNames.PIC).append("\"><pic:nvPicPr><pic:cNvPr id=\"0\" name=\"").append(OoxmlXml.escape(name)).append("\"/><pic:cNvPicPr/></pic:nvPicPr><pic:blipFill><a:blip r:embed=\"").append(relationshipId).append("\"/>");
        if (crop != null && !crop.isEmpty()) b.append("<a:srcRect l=\"").append(Math.round(crop.left()*100000)).append("\" t=\"").append(Math.round(crop.top()*100000)).append("\" r=\"").append(Math.round(crop.right()*100000)).append("\" b=\"").append(Math.round(crop.bottom()*100000)).append("\"/>");
        b.append("<a:stretch><a:fillRect/></a:stretch></pic:blipFill><pic:spPr><a:xfrm").append(rotation == 0 ? "" : " rot=\"" + Math.round(rotation*60000) + "\"").append("><a:off x=\"0\" y=\"0\"/><a:ext cx=\"").append(emu(width)).append("\" cy=\"").append(emu(height)).append("\"/></a:xfrm><a:prstGeom prst=\"rect\"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>");
        return b.toString();
    }
    static String shape(WordShape shape, boolean child) {
        StringBuilder b = new StringBuilder("<wps:wsp>");
        if (shape.shapeType().isLinear() && shape.shapeType() != WordShapeType.LINE) b.append("<wps:cNvCnPr/>");
        else b.append("<wps:cNvSpPr").append(shape.shapeType() == WordShapeType.TEXT_BOX ? " txBox=\"1\"" : "").append("/>");
        b.append("<wps:spPr><a:xfrm").append(shape.rotation() == 0 ? "" : " rot=\"" + Math.round(shape.rotation()*60000) + "\"").append("><a:off x=\"").append(child ? emu(shape.x()) : 0).append("\" y=\"").append(child ? emu(shape.y()) : 0)
                .append("\"/><a:ext cx=\"").append(emu(shape.width())).append("\" cy=\"").append(emu(shape.height())).append("\"/></a:xfrm><a:prstGeom prst=\"").append(shape.shapeType().preset()).append("\"><a:avLst/></a:prstGeom>");
        if (shape.fill() == null || shape.shapeType().isLinear()) b.append("<a:noFill/>"); else b.append("<a:solidFill><a:srgbClr val=\"").append(hex(shape.fill())).append("\"/></a:solidFill>");
        b.append("<a:ln w=\"").append(emu(shape.strokeWidth())).append("\">");
        if (shape.stroke() == null || shape.strokeWidth() <= 0) b.append("<a:noFill/>"); else b.append("<a:solidFill><a:srgbClr val=\"").append(hex(shape.stroke())).append("\"/></a:solidFill>");
        if (shape.arrowEnd()) b.append("<a:tailEnd type=\"triangle\"/>");
        b.append("</a:ln></wps:spPr>");
        boolean box = shape.shapeType() == WordShapeType.TEXT_BOX;
        if (!shape.text().isBlank() && !shape.shapeType().isLinear()) {
            b.append("<wps:txbx><w:txbxContent>");
            for (String line : shape.text().split("\n",-1)) {
                b.append("<w:p><w:pPr><w:spacing w:after=\"0\"/><w:jc w:val=\"").append(box ? "left" : "center").append("\"/></w:pPr><w:r><w:rPr><w:color w:val=\"").append(hex(shape.textColor()))
                        .append("\"/><w:sz w:val=\"").append(Math.round(shape.fontSize()*2)).append("\"/></w:rPr><w:t xml:space=\"preserve\">").append(OoxmlXml.escape(line)).append("</w:t></w:r></w:p>");
            }
            b.append("</w:txbxContent></wps:txbx>");
        }
        b.append("<wps:bodyPr rot=\"0\" vert=\"horz\" wrap=\"square\" lIns=\"91440\" tIns=\"45720\" rIns=\"91440\" bIns=\"45720\" anchor=\"").append(box ? "t" : "ctr").append("\" anchorCtr=\"0\"><a:noAutofit/></wps:bodyPr></wps:wsp>");
        return b.toString();
    }
    static String group(WordShape group) {
        StringBuilder b = new StringBuilder("<wpg:wgp><wpg:cNvGrpSpPr/><wpg:grpSpPr><a:xfrm><a:off x=\"0\" y=\"0\"/><a:ext cx=\"").append(emu(group.width())).append("\" cy=\"").append(emu(group.height()))
                .append("\"/><a:chOff x=\"0\" y=\"0\"/><a:chExt cx=\"").append(emu(group.width())).append("\" cy=\"").append(emu(group.height())).append("\"/></a:xfrm></wpg:grpSpPr>");
        for (WordShape child : group.children()) {
            if (child.shapeType() == WordShapeType.GROUP) return null;
            b.append(shape(child,true));
        }
        return b.append("</wpg:wgp>").toString();
    }
    private static String hex(int rgb) { return String.format(Locale.ROOT,"%06X",rgb & 0xffffff); }
}
