package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

public final class WordTextExporter {
    private WordTextExporter() {}
    public static String text(WordDocument document) {
        StringBuilder out = new StringBuilder();
        blocksText(document.blocks(),out);
        return out.toString();
    }
    private static void blocksText(List<WordBlock> blocks, StringBuilder out) {
        for (WordBlock b : blocks) {
            if (!out.isEmpty()) out.append('\n');
            out.append(b.plainText().replace(' ','\n'));
        }
    }
    public static String html(WordDocument document) { return html(document,WordObjectRegistry.defaults()); }
    public static String html(WordDocument document, WordObjectRegistry registry) {
        StringBuilder out = new StringBuilder("<!doctype html><html lang=\"pt-BR\"><head><meta charset=\"utf-8\"><title>Documento</title>"
                + "<style>body{max-width:" + Math.round(document.pageSettings().contentWidth()*96/72) + "px;margin:32px auto;font-family:Arial,sans-serif}table{border-collapse:collapse}td{vertical-align:top;padding:4px}</style></head><body>\n");
        blocks(document,document.blocks(),out,registry);
        return out.append("</body></html>").toString();
    }
    private static void blocks(WordDocument document, List<WordBlock> blocks, StringBuilder out, WordObjectRegistry registry) {
        String openList = null;
        for (WordBlock block : blocks) {
            if (block instanceof WordParagraph p && p.style().list() != null) {
                boolean bullet = document.parts().numbering().get(p.style().list().listId()).map(WordListDefinition::bullet).orElse(true);
                String tag = bullet ? "ul" : "ol";
                if (!tag.equals(openList)) { if (openList != null) out.append("</").append(openList).append(">\n"); out.append('<').append(tag).append('>'); openList = tag; }
                out.append("<li>"); inlines(document,p,out,registry); out.append("</li>\n");
                continue;
            }
            if (openList != null) { out.append("</").append(openList).append(">\n"); openList = null; }
            switch (block) {
                case WordParagraph p -> paragraph(document,p,out,registry);
                case WordTable t -> {
                    out.append("<table style=\"width:").append(Math.round(t.width()*96/72)).append("px\">");
                    for (WordTableRow row : t.rows()) {
                        out.append("<tr>");
                        for (WordTableCell cell : row.cells()) {
                            if (cell.verticalMerge() == WordTableCell.Merge.CONTINUE) continue;
                            String tag = row.header() ? "th" : "td";
                            out.append('<').append(tag);
                            if (cell.gridSpan() > 1) out.append(" colspan=\"").append(cell.gridSpan()).append('"');
                            WordBorder border = cell.border() != null ? cell.border() : t.border();
                            out.append(" style=\"").append(border.visible() ? "border:" + border.width() + "pt solid #" + hex(border.color()) + ";" : "").append(cell.fill() == null ? "" : "background:#" + hex(cell.fill())).append("\">");
                            blocks(document,cell.blocks(),out,registry);
                            out.append("</").append(tag).append('>');
                        }
                        out.append("</tr>");
                    }
                    out.append("</table>\n");
                }
                default -> out.append("<div style=\"border:1px dashed #999;padding:4px;color:#555\">").append(escape(block.plainText())).append("</div>\n");
            }
        }
        if (openList != null) out.append("</").append(openList).append(">\n");
    }
    private static void paragraph(WordDocument document, WordParagraph p, StringBuilder out, WordObjectRegistry registry) {
        var s = p.style(); String tag = s.headingLevel() > 0 ? "h" + Math.min(s.headingLevel(),6) : "p";
        out.append('<').append(tag).append(" style=\"white-space:pre-wrap;text-align:").append(s.alignment().name().toLowerCase(Locale.ROOT))
                .append(";margin:").append(s.before()).append("pt ").append(s.rightIndent()).append("pt ").append(s.after()).append("pt ")
                .append(s.leftIndent()).append("pt;text-indent:").append(s.firstLineIndent()).append("pt;line-height:").append(s.lineSpacing());
        if (s.shading() != null) out.append(";background:#").append(hex(s.shading()));
        if (s.pageBreakBefore()) out.append(";page-break-before:always");
        out.append("\">");
        inlines(document,p,out,registry);
        if (p.runs().isEmpty()) out.append("<br>");
        out.append("</").append(tag).append(">\n");
    }
    private static void inlines(WordDocument document, WordParagraph p, StringBuilder out, WordObjectRegistry registry) {
        for (WordInline inline : p.runs()) {
            var f = inline.style();
            if (f.link() != null) out.append("<a href=\"").append(escape(f.link())).append("\">");
            out.append("<span style=\"font-family:&quot;").append(escape(f.family().replace("\\","\\\\").replace("\"","\\\""))).append("&quot;;font-size:")
                    .append(f.size()).append("pt;color:#").append(String.format(Locale.ROOT,"%06x",f.color()));
            if (f.bold()) out.append(";font-weight:bold");
            if (f.italic()) out.append(";font-style:italic");
            if (f.underline() || f.strike()) out.append(";text-decoration:").append(f.underline() ? "underline " : "").append(f.strike() ? "line-through" : "");
            if (f.highlight() != null) out.append(";background-color:#").append(String.format(Locale.ROOT,"%06x",f.highlight()));
            if (f.verticalAlign() != WordTextStyle.VerticalAlign.BASELINE) out.append(";vertical-align:").append(f.verticalAlign() == WordTextStyle.VerticalAlign.SUPERSCRIPT ? "super" : "sub").append(";font-size:smaller");
            out.append("\">");
            if (inline instanceof WordObjectRun o) object(document,o.object(),out,registry);
            else out.append(escape(inline.text()));
            out.append("</span>");
            if (f.link() != null) out.append("</a>");
        }
    }
    private static void object(WordDocument document, WordInlineObject object, StringBuilder out, WordObjectRegistry registry) {
        if (object instanceof WordBreak b) { out.append(b.kind() == WordBreak.Kind.LINE ? "<br>" : "<hr style=\"page-break-after:always;border:0\">"); return; }
        if (object.textual() || object instanceof WordOpaqueObject) { out.append(escape(object.plainText())); return; }
        byte[] png = null;
        if (object instanceof WordImage image) png = document.resources().get(image.resourceId()).filter(r -> r.contentType().equals("image/png") || r.contentType().equals("image/jpeg") || r.contentType().equals("image/gif")).map(WordResource::data).orElse(null);
        String type = "image/png";
        if (object instanceof WordImage image && png != null) type = document.resources().get(image.resourceId()).map(WordResource::contentType).orElse(type);
        if (png == null) png = raster(document,object,registry);
        if (png == null) { out.append(escape(object.plainText())); return; }
        out.append("<img alt=\"").append(escape(object.altText())).append("\" width=\"").append(Math.round(object.width()*96/72)).append("\" height=\"").append(Math.round(object.height()*96/72))
                .append("\" src=\"data:").append(type).append(";base64,").append(Base64.getEncoder().encodeToString(png)).append("\">");
    }
    private static byte[] raster(WordDocument document, WordInlineObject object, WordObjectRegistry registry) {
        try {
            int w = Math.max(1,Math.min(3000,Math.round(object.width()*2))), h = Math.max(1,Math.min(3000,Math.round(object.height()*2)));
            BufferedImage image = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB); Graphics2D g = image.createGraphics();
            try { g.scale(2,2); registry.paint(g,object,new Rectangle2D.Float(0,0,object.width(),object.height()),document); } finally { g.dispose(); }
            ByteArrayOutputStream out = new ByteArrayOutputStream(); ImageIO.write(image,"png",out); return out.toByteArray();
        } catch (IOException | RuntimeException e) { return null; }
    }
    private static String hex(int rgb) { return String.format(Locale.ROOT,"%06x",rgb & 0xffffff); }
    public static String escape(String value) { return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&#39;"); }
}
