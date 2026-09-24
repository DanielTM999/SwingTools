package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import dtm.stools.component.panels.editor.word.layout.WordLayout;
import dtm.stools.component.panels.editor.word.layout.WordLayoutEngine;
import dtm.stools.component.panels.editor.word.model.*;
import dtm.stools.component.panels.editor.word.render.WordObjectRegistry;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

final class DocxWriter {
    private static final List<String> PPR_ORDER = List.of("pStyle","keepNext","keepLines","pageBreakBefore","framePr","widowControl","numPr","suppressLineNumbers","pBdr","shd","tabs",
            "suppressAutoHyphens","kinsoku","wordWrap","overflowPunct","topLinePunct","autoSpaceDE","autoSpaceDN","bidi","adjustRightInd","snapToGrid","spacing","ind","contextualSpacing",
            "mirrorIndents","suppressOverlap","jc","textDirection","textAlignment","textboxTightWrap","outlineLvl","divId","cnfStyle","rPr","sectPr","pPrChange");
    private static final List<String> RPR_ORDER = List.of("rStyle","rFonts","b","bCs","i","iCs","caps","smallCaps","strike","dstrike","outline","shadow","emboss","imprint","noProof",
            "snapToGrid","vanish","webHidden","color","spacing","w","kern","position","sz","szCs","highlight","u","effect","bdr","shd","fitText","vertAlign","rtl","cs","em","lang",
            "eastAsianLayout","specVanish","oMath","rPrChange");
    private static final List<String> TBLPR_ORDER = List.of("tblStyle","tblpPr","tblOverlap","bidiVisual","tblStyleRowBandSize","tblStyleColBandSize","tblW","jc","tblCellSpacing","tblInd",
            "tblBorders","shd","tblLayout","tblCellMar","tblLook","tblCaption","tblDescription","tblPrChange");
    private static final List<String> TRPR_ORDER = List.of("cnfStyle","divId","gridBefore","gridAfter","wBefore","wAfter","cantSplit","trHeight","tblHeader","tblCellSpacing","jc","hidden","ins","del","trPrChange");
    private static final List<String> TCPR_ORDER = List.of("cnfStyle","tcW","gridSpan","hMerge","vMerge","tcBorders","shd","noWrap","tcMar","textDirection","tcFitText","vAlign","hideMark","headers","cellIns","cellDel","cellMerge","tcPrChange");
    private static final Map<Integer,String> HIGHLIGHT = Map.ofEntries(Map.entry(0xFFFF00,"yellow"),Map.entry(0x00FF00,"green"),Map.entry(0x00FFFF,"cyan"),Map.entry(0xFF00FF,"magenta"),
            Map.entry(0x0000FF,"blue"),Map.entry(0xFF0000,"red"),Map.entry(0x000080,"darkBlue"),Map.entry(0x008080,"darkCyan"),Map.entry(0x008000,"darkGreen"),Map.entry(0x800080,"darkMagenta"),
            Map.entry(0x800000,"darkRed"),Map.entry(0x808000,"darkYellow"),Map.entry(0x808080,"darkGray"),Map.entry(0xC0C0C0,"lightGray"),Map.entry(0x000000,"black"));

    private final class Part {
        final String name; final DocxRelationships rels;
        Part(String name, DocxRelationships rels) { this.name = name; this.rels = rels; }
        String media(String resourceId) throws IOException {
            WordResource resource = document.resources().get(resourceId).orElseThrow(() -> new IOException("Missing image resource " + resourceId));
            String target = mediaNames.computeIfAbsent(resourceId,id -> "word/media/st" + id + "." + resource.extension());
            parts.put(target,resource.data());
            return rels.add(DocxNames.REL_IMAGE,target,false);
        }
    }

    private final WordDocument document;
    private final WordImportResult origin;
    private final WordObjectRegistry registry;
    private final String ns;
    private final Map<String,byte[]> parts = new LinkedHashMap<>();
    private final Map<String,String> overrides = new LinkedHashMap<>();
    private final Map<String,String> mediaNames = new HashMap<>();
    private final Map<String,Map<String,String>> customData = new LinkedHashMap<>();
    private final Map<String,Integer> noteNumbers = new HashMap<>();
    private final Map<String,Integer> commentNumbers = new HashMap<>();
    private final Map<UUID,String> headingBookmarks = new HashMap<>();
    private final Map<UUID,Integer> headingPages = new HashMap<>();
    private final Map<String,String> bookmarkTexts = new HashMap<>();
    private final Map<String,Integer> sequences = new HashMap<>();
    private final Set<String> openComments = new LinkedHashSet<>();
    private int docPr = 1, markId = 1, chartCounter, partCounter;
    private String headerXmlRefs = "";

    DocxWriter(WordDocument document, WordImportResult origin, WordObjectRegistry registry) {
        this.document = document; this.origin = origin; this.registry = registry == null ? WordObjectRegistry.defaults() : registry;
        this.ns = origin == null ? DocxNames.W : origin.namespace();
    }

    void write(OutputStream output) throws IOException {
        String main = origin == null ? "word/document.xml" : origin.mainPart();
        if (origin != null) for (String name : origin.source().names()) parts.put(name,origin.source().part(name));
        DocxRelationships rels = origin == null ? new DocxRelationships(main) : DocxRelationships.read(origin.source(),main);
        Part mainPart = new Part(main,rels);
        prepare();
        String styles = rels.byType("styles").map(DocxRelationships.Relationship::target).orElse("word/styles.xml");
        byte[] originalStyles = origin != null && parts.containsKey(styles) && rels.byType("styles").isPresent() ? parts.get(styles) : null;
        parts.put(styles,DocxStylesXml.writeStyles(document.styles(),originalStyles,origin == null ? null : origin.document().styles(),ns));
        rels.add(DocxNames.REL_STYLES,styles,false); overrides.put(styles,DocxNames.CT_STYLES);
        boolean usesLists = document.paragraphs().stream().anyMatch(p -> p.style().list() != null) || !document.parts().numbering().lists().isEmpty();
        if (usesLists) {
            String numbering = rels.byType("numbering").map(DocxRelationships.Relationship::target).orElse("word/numbering.xml");
            byte[] original = origin != null && rels.byType("numbering").isPresent() ? parts.get(numbering) : null;
            parts.put(numbering,DocxStylesXml.writeNumbering(document.parts().numbering(),original,ns));
            rels.add(DocxNames.REL_NUMBERING,numbering,false); overrides.put(numbering,DocxNames.CT_NUMBERING);
        }
        String settings = rels.byType("settings").map(DocxRelationships.Relationship::target).orElse("word/settings.xml");
        byte[] originalSettings = origin != null && rels.byType("settings").isPresent() ? parts.get(settings) : null;
        parts.put(settings,DocxStylesXml.writeSettings(originalSettings,document.parts().headers().differentOddEven(),ns));
        rels.add(DocxNames.REL_SETTINGS,settings,false); overrides.put(settings,DocxNames.CT_SETTINGS);
        headers(mainPart);
        notes(mainPart,WordNote.Kind.FOOTNOTE);
        notes(mainPart,WordNote.Kind.ENDNOTE);
        StringBuilder body = new StringBuilder();
        blocks(body,document.blocks(),mainPart,true);
        for (String id : new ArrayList<>(openComments)) closeComment(body,id);
        body.append(sectPr(document.pageSettings()));
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<w:document" + namespaces() + "><w:body>" + body + "</w:body></w:document>";
        byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
        OoxmlXml.parse(bytes);
        parts.put(main,bytes); overrides.put(main,DocxNames.CT_MAIN);
        comments(mainPart);
        customXml(mainPart);
        parts.put(DocxRelationships.relsName(main),rels.bytes());
        if (!parts.containsKey("_rels/.rels"))
            parts.put("_rels/.rels",("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Relationships xmlns=\"" + DocxNames.PKG_RELS + "\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"" + main + "\"/></Relationships>").getBytes(StandardCharsets.UTF_8));
        contentTypes();
        Map<String,byte[]> ordered = new LinkedHashMap<>();
        ordered.put("[Content_Types].xml",parts.remove("[Content_Types].xml"));
        ordered.putAll(parts);
        new OpcPackage(ordered).write(output);
    }

    private void prepare() {
        boolean toc = document.blocks().stream().anyMatch(b -> b instanceof WordTableOfContents);
        int index = 0;
        for (WordParagraph p : document.paragraphs()) {
            if (toc && p.style().headingLevel() > 0 && !p.plainText().isBlank()) headingBookmarks.put(p.id(),"_Toc" + (100000000 + index++));
            for (String b : p.bookmarks()) bookmarkTexts.put(b,p.plainText());
        }
        if (toc) {
            try {
                WordLayout layout = new WordLayoutEngine().layout(document);
                Map<Integer,UUID> starts = new HashMap<>();
                for (int i = 0; i < document.paragraphs().size(); i++) if (headingBookmarks.containsKey(document.paragraphs().get(i).id())) starts.put(document.paragraphStart(i),document.paragraphs().get(i).id());
                for (WordLayout.Page page : layout.pages()) for (WordLayout.Line line : page.lines()) {
                    UUID id = line.positional() ? starts.get(line.start()) : null;
                    if (id != null) headingPages.putIfAbsent(id,page.number());
                }
            } catch (RuntimeException ignored) {}
        }
        Set<Integer> used = new HashSet<>();
        for (String id : document.parts().notes().keySet()) {
            Integer n = numeric(id.length() > 1 ? id.substring(1) : id);
            if (n != null && n > 0 && used.add(n * 2 + (id.startsWith("e") ? 1 : 0))) noteNumbers.put(id,n);
        }
        int next = 1;
        for (String id : document.parts().notes().keySet()) if (!noteNumbers.containsKey(id)) { while (noteNumbers.containsValue(next)) next++; noteNumbers.put(id,next++); }
        Set<Integer> commentIds = new HashSet<>();
        for (WordComment c : document.parts().comments()) { Integer n = numeric(c.id()); if (n != null && n >= 0 && commentIds.add(n)) commentNumbers.put(c.id(),n); }
        int nextComment = 0;
        for (WordComment c : document.parts().comments()) if (!commentNumbers.containsKey(c.id())) { while (commentIds.contains(nextComment)) nextComment++; commentIds.add(nextComment); commentNumbers.put(c.id(),nextComment); }
    }
    private static Integer numeric(String value) { try { return Integer.parseInt(value); } catch (NumberFormatException e) { return null; } }

    private String namespaces() {
        Map<String,String> all = new LinkedHashMap<>(DocxNames.ROOT_NAMESPACES);
        if (origin != null) origin.rootNamespaces().forEach((prefix,uri) -> { if (!prefix.equals("w")) all.putIfAbsent(prefix,uri); });
        all.put("w",ns);
        StringBuilder b = new StringBuilder();
        all.forEach((prefix,uri) -> b.append(" xmlns:").append(prefix).append("=\"").append(OoxmlXml.escape(uri)).append('"'));
        Set<String> ignorable = new LinkedHashSet<>(List.of("w14","w15","wp14"));
        if (origin != null) for (String p : origin.ignorable().split("\\s+")) if (!p.isBlank() && all.containsKey(p)) ignorable.add(p);
        b.append(" mc:Ignorable=\"").append(String.join(" ",ignorable)).append('"');
        return b.toString();
    }

    private void blocks(StringBuilder b, List<WordBlock> blocks, Part part, boolean body) throws IOException {
        for (WordBlock block : blocks) {
            switch (block) {
                case WordParagraph p -> paragraph(b,p,part,body);
                case WordTable t -> table(b,t,part,body);
                case WordTableOfContents toc -> toc(b,toc);
                case WordOpaqueBlock o -> b.append(o.xml());
                default -> b.append("<w:p><w:r><w:t xml:space=\"preserve\">").append(OoxmlXml.escape(block.plainText())).append("</w:t></w:r></w:p>");
            }
        }
    }

    private void paragraph(StringBuilder b, WordParagraph p, Part part, boolean body) throws IOException {
        b.append("<w:p>").append(pPr(p));
        List<String> marks = new ArrayList<>(p.bookmarks());
        String toc = headingBookmarks.get(p.id());
        if (toc != null) marks.add(toc);
        List<Integer> ids = new ArrayList<>();
        for (String mark : marks) { int id = markId++; ids.add(id); b.append("<w:bookmarkStart w:id=\"").append(id).append("\" w:name=\"").append(OoxmlXml.escape(mark)).append("\"/>"); }
        WordRevision revision = null; String link = null;
        for (WordInline inline : p.runs()) {
            WordTextStyle style = inline.style();
            if (body) {
                Set<String> wanted = new LinkedHashSet<>(style.comments());
                for (String id : new ArrayList<>(openComments)) if (!wanted.contains(id)) { if (link != null) { b.append("</w:hyperlink>"); link = null; } if (revision != null) { b.append(revision.type() == WordRevision.Type.INSERT ? "</w:ins>" : "</w:del>"); revision = null; } closeComment(b,id); }
                for (String id : wanted) if (!openComments.contains(id) && commentNumbers.containsKey(id)) { openComments.add(id); b.append("<w:commentRangeStart w:id=\"").append(commentNumbers.get(id)).append("\"/>"); }
            }
            String nextLink = linkTarget(style.link());
            if (!Objects.equals(revision,style.revision()) || !Objects.equals(link,nextLink)) {
                if (link != null) { b.append("</w:hyperlink>"); link = null; }
                if (!Objects.equals(revision,style.revision())) {
                    if (revision != null) b.append(revision.type() == WordRevision.Type.INSERT ? "</w:ins>" : "</w:del>");
                    revision = style.revision();
                    if (revision != null) b.append(revision.type() == WordRevision.Type.INSERT ? "<w:ins" : "<w:del").append(" w:id=\"").append(markId++).append("\" w:author=\"").append(OoxmlXml.escape(revision.author()))
                            .append("\" w:date=\"").append(revision.date().truncatedTo(ChronoUnit.SECONDS)).append("\">");
                }
                if (nextLink != null) {
                    if (nextLink.startsWith("#")) b.append("<w:hyperlink w:anchor=\"").append(OoxmlXml.escape(nextLink.substring(1))).append("\">");
                    else b.append("<w:hyperlink r:id=\"").append(part.rels.add(DocxNames.REL_HYPERLINK,nextLink,true)).append("\" w:history=\"1\">");
                    link = nextLink;
                }
            }
            inline(b,inline,part,revision != null && revision.type() == WordRevision.Type.DELETE);
        }
        if (link != null) b.append("</w:hyperlink>");
        if (revision != null) b.append(revision.type() == WordRevision.Type.INSERT ? "</w:ins>" : "</w:del>");
        for (int id : ids) b.append("<w:bookmarkEnd w:id=\"").append(id).append("\"/>");
        b.append("</w:p>");
    }
    private String linkTarget(String link) {
        if (link == null) return null;
        if (link.startsWith("#p:")) {
            try { String bookmark = headingBookmarks.get(UUID.fromString(link.substring(3))); return bookmark == null ? null : "#" + bookmark; } catch (IllegalArgumentException e) { return null; }
        }
        return link;
    }
    private void closeComment(StringBuilder b, String id) {
        openComments.remove(id);
        Integer n = commentNumbers.get(id);
        if (n == null) return;
        b.append("<w:commentRangeEnd w:id=\"").append(n).append("\"/><w:r><w:commentReference w:id=\"").append(n).append("\"/></w:r>");
    }

    private String pPr(WordParagraph p) {
        WordParagraphStyle s = p.style();
        Map<String,String> elements = new LinkedHashMap<>();
        if (s.styleId() != null) elements.put("pStyle","<w:pStyle w:val=\"" + OoxmlXml.escape(s.styleId()) + "\"/>");
        if (s.keepWithNext()) elements.put("keepNext","<w:keepNext/>");
        if (s.pageBreakBefore()) elements.put("pageBreakBefore","<w:pageBreakBefore/>");
        if (s.list() != null) elements.put("numPr","<w:numPr><w:ilvl w:val=\"" + s.list().level() + "\"/><w:numId w:val=\"" + OoxmlXml.escape(s.list().listId()) + "\"/></w:numPr>");
        if (s.shading() != null) elements.put("shd","<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"" + hex(s.shading()) + "\"/>");
        if (!s.tabs().isEmpty()) {
            StringBuilder t = new StringBuilder("<w:tabs>");
            for (WordTabStop tab : s.tabs()) {
                t.append("<w:tab w:val=\"").append(switch (tab.alignment()) { case LEFT -> "left"; case CENTER -> "center"; case RIGHT -> "right"; case DECIMAL -> "decimal"; }).append('"');
                if (tab.leader() != WordTabStop.Leader.NONE) t.append(" w:leader=\"").append(switch (tab.leader()) { case DOT -> "dot"; case HYPHEN -> "hyphen"; default -> "underscore"; }).append('"');
                t.append(" w:pos=\"").append(DocxStylesXml.twips(tab.position())).append("\"/>");
            }
            elements.put("tabs",t.append("</w:tabs>").toString());
        }
        elements.put("spacing","<w:spacing w:before=\"" + DocxStylesXml.twips(s.before()) + "\" w:after=\"" + DocxStylesXml.twips(s.after()) + "\" w:line=\"" + Math.round(s.lineSpacing()*240) + "\" w:lineRule=\"auto\"/>");
        elements.put("ind","<w:ind w:left=\"" + DocxStylesXml.twips(s.leftIndent()) + "\" w:right=\"" + DocxStylesXml.twips(s.rightIndent()) + "\" " + (s.firstLineIndent() < 0 ? "w:hanging=\"" : "w:firstLine=\"") + DocxStylesXml.twips(Math.abs(s.firstLineIndent())) + "\"/>");
        elements.put("jc","<w:jc w:val=\"" + DocxStylesXml.jc(s.alignment()) + "\"/>");
        int styleHeading = s.styleId() == null ? 0 : document.styles().resolveParagraph(s.styleId()).headingLevel();
        if (s.headingLevel() > 0) elements.put("outlineLvl","<w:outlineLvl w:val=\"" + (s.headingLevel()-1) + "\"/>");
        else if (styleHeading > 0) elements.put("outlineLvl","<w:outlineLvl w:val=\"9\"/>");
        if (p.sectionBreak() != null) elements.put("sectPr",sectPr(p.sectionBreak()));
        return "<w:pPr>" + ordered(elements,s.extras(),PPR_ORDER) + "</w:pPr>";
    }
    private static String ordered(Map<String,String> generated, List<String> extras, List<String> order) {
        List<Map.Entry<String,String>> all = new ArrayList<>(generated.entrySet());
        for (String extra : extras) { String name = localName(extra); if (!generated.containsKey(name)) all.add(Map.entry(name,extra)); }
        all.sort(Comparator.comparingInt(e -> { int i = order.indexOf(e.getKey()); return i < 0 ? order.size() : i; }));
        StringBuilder b = new StringBuilder(); for (var e : all) b.append(e.getValue()); return b.toString();
    }
    private static String localName(String xml) {
        int start = xml.indexOf('<') + 1, end = start;
        while (end < xml.length() && " />\t\n\r".indexOf(xml.charAt(end)) < 0) end++;
        String name = xml.substring(start,end); int colon = name.indexOf(':');
        return colon < 0 ? name : name.substring(colon+1);
    }
    private String rPr(WordTextStyle s, boolean deleted) {
        Map<String,String> e = new LinkedHashMap<>();
        String family = OoxmlXml.escape(s.family());
        e.put("rFonts","<w:rFonts w:ascii=\"" + family + "\" w:hAnsi=\"" + family + "\" w:cs=\"" + family + "\"/>");
        e.put("b","<w:b w:val=\"" + (s.bold() ? 1 : 0) + "\"/>");
        e.put("i","<w:i w:val=\"" + (s.italic() ? 1 : 0) + "\"/>");
        e.put("strike","<w:strike w:val=\"" + (s.strike() ? 1 : 0) + "\"/>");
        e.put("color","<w:color w:val=\"" + hex(s.color()) + "\"/>");
        e.put("sz","<w:sz w:val=\"" + Math.round(s.size()*2) + "\"/>");
        e.put("szCs","<w:szCs w:val=\"" + Math.round(s.size()*2) + "\"/>");
        if (s.highlight() != null) {
            String named = HIGHLIGHT.get(s.highlight());
            if (named != null) e.put("highlight","<w:highlight w:val=\"" + named + "\"/>");
            else e.put("shd","<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"" + hex(s.highlight()) + "\"/>");
        }
        e.put("u","<w:u w:val=\"" + (s.underline() ? "single" : "none") + "\"/>");
        if (s.verticalAlign() != WordTextStyle.VerticalAlign.BASELINE) e.put("vertAlign","<w:vertAlign w:val=\"" + (s.verticalAlign() == WordTextStyle.VerticalAlign.SUPERSCRIPT ? "superscript" : "subscript") + "\"/>");
        return "<w:rPr>" + ordered(e,s.extras(),RPR_ORDER) + "</w:rPr>";
    }

    private void inline(StringBuilder b, WordInline inline, Part part, boolean deleted) throws IOException {
        if (inline instanceof WordRun run) {
            b.append("<w:r>").append(rPr(run.style(),deleted));
            String[] pieces = run.text().split("\t",-1);
            for (int i = 0; i < pieces.length; i++) {
                if (i > 0) b.append("<w:tab/>");
                if (pieces[i].isEmpty()) continue;
                OoxmlXml.validateText(pieces[i]);
                String tag = deleted ? "w:delText" : "w:t";
                b.append('<').append(tag).append(" xml:space=\"preserve\">").append(OoxmlXml.escape(pieces[i])).append("</").append(tag).append('>');
            }
            b.append("</w:r>");
            return;
        }
        WordObjectRun run = (WordObjectRun)inline;
        WordInlineObject object = run.object();
        WordImportResult.OriginalObject original = origin == null ? null : origin.originals().get(object.id());
        if (original != null && original.parsed().equals(object) && original.part().equals(part.name)) {
            if (object instanceof WordEquation) b.append(original.xml());
            else b.append("<w:r>").append(rPr(run.style(),deleted)).append(original.xml()).append("</w:r>");
            return;
        }
        String rPr = rPr(run.style(),deleted);
        switch (object) {
            case WordBreak br -> b.append("<w:r>").append(rPr).append(switch (br.kind()) { case LINE -> "<w:br/>"; case PAGE -> "<w:br w:type=\"page\"/>"; case COLUMN -> "<w:br w:type=\"column\"/>"; }).append("</w:r>");
            case WordField f -> {
                OoxmlXml.validateText(f.argument());
                b.append("<w:fldSimple w:instr=\" ").append(OoxmlXml.escape(f.instruction())).append(" \"><w:r>").append(rPr).append("<w:t xml:space=\"preserve\">").append(OoxmlXml.escape(cached(f))).append("</w:t></w:r></w:fldSimple>");
            }
            case WordNoteReference n -> {
                WordNote note = document.parts().notes().get(n.noteId());
                boolean end = note != null && note.kind() == WordNote.Kind.ENDNOTE;
                Integer number = noteNumbers.get(n.noteId());
                if (number == null) return;
                b.append("<w:r><w:rPr><w:rStyle w:val=\"").append(end ? "EndnoteReference" : "FootnoteReference").append("\"/><w:vertAlign w:val=\"superscript\"/></w:rPr><w:").append(end ? "endnoteReference" : "footnoteReference").append(" w:id=\"").append(number).append("\"/></w:r>");
            }
            case WordFormField f -> form(b,f,rPr);
            case WordEquation e -> {
                String math = DocxMathXml.write(e.math());
                b.append(e.display() ? "<m:oMathPara><m:oMath>" + math + "</m:oMath></m:oMathPara>" : "<m:oMath>" + math + "</m:oMath>");
            }
            case WordOpaqueObject o -> { if (o.level() == WordOpaqueObject.Level.PARAGRAPH) b.append(o.xml()); else b.append("<w:r>").append(rPr).append(o.xml()).append("</w:r>"); }
            case WordImage image -> {
                String rel = part.media(image.resourceId());
                String graphic = DocxDrawingXml.picture(rel,image.width(),image.height(),image.crop(),image.rotation(),"Imagem " + docPr);
                b.append("<w:r>").append(rPr).append(DocxDrawingXml.wrap(image,docPr,"Imagem " + docPr++,DocxNames.PIC,graphic)).append("</w:r>");
            }
            case WordChart chart -> {
                String rel = chart(chart,part);
                String graphic = "<c:chart xmlns:c=\"" + DocxNames.C + "\" r:id=\"" + rel + "\"/>";
                b.append("<w:r>").append(rPr).append(DocxDrawingXml.wrap(chart,docPr,"Gráfico " + docPr++,DocxNames.C,graphic)).append("</w:r>");
            }
            case WordShape shape -> {
                String graphic = shape.shapeType() == WordShapeType.GROUP ? DocxDrawingXml.group(shape) : DocxDrawingXml.shape(shape,false);
                if (graphic == null) { preview(b,shape,rPr,part,Map.of("$type","swingtools.shape")); return; }
                String uri = shape.shapeType() == WordShapeType.GROUP ? DocxNames.WPG : DocxNames.WPS;
                b.append("<w:r>").append(rPr).append("<mc:AlternateContent><mc:Choice Requires=\"").append(shape.shapeType() == WordShapeType.GROUP ? "wpg" : "wps").append("\">")
                        .append(DocxDrawingXml.wrap(shape,docPr,"Forma " + docPr++,uri,graphic)).append("</mc:Choice></mc:AlternateContent></w:r>");
            }
            case WordDiagram diagram -> {
                Map<String,String> data = new LinkedHashMap<>();
                data.put("$type","swingtools.diagram"); data.put("layout",diagram.layout().name()); data.put("color",String.format(Locale.ROOT,"%06X",diagram.color())); data.put("outline",diagram.outline());
                preview(b,diagram,rPr,part,data);
            }
            case WordCustomObject custom -> {
                Map<String,String> data = new LinkedHashMap<>(custom.data()); data.put("$type",custom.customType());
                preview(b,custom,rPr,part,data);
            }
            default -> b.append("<w:r>").append(rPr).append("<w:t xml:space=\"preserve\">").append(OoxmlXml.escape(object.plainText())).append("</w:t></w:r>");
        }
    }
    private String cached(WordField f) {
        return switch (f.kind()) {
            case PAGE, NUM_PAGES, PAGE_REF -> f.cachedText().isBlank() ? "1" : f.cachedText();
            case DATE -> { try { yield new SimpleDateFormat(f.argument().isBlank() ? "dd/MM/yyyy" : f.argument(),Locale.forLanguageTag("pt-BR")).format(new Date()); } catch (IllegalArgumentException e) { yield f.cachedText(); } }
            case REF -> bookmarkTexts.getOrDefault(f.argument(),f.cachedText());
            case SEQ -> Integer.toString(sequences.merge(f.argument(),1,Integer::sum));
        };
    }
    private void form(StringBuilder b, WordFormField f, String rPr) throws IOException {
        OoxmlXml.validateText(f.name()); OoxmlXml.validateText(f.value());
        b.append("<w:sdt><w:sdtPr>");
        if (!f.name().isBlank()) b.append("<w:alias w:val=\"").append(OoxmlXml.escape(f.name())).append("\"/><w:tag w:val=\"").append(OoxmlXml.escape(f.name())).append("\"/>");
        boolean placeholder = f.kind() != WordFormField.Kind.CHECKBOX && f.value().isEmpty();
        if (placeholder) b.append("<w:showingPlcHdr/>");
        switch (f.kind()) {
            case TEXT -> b.append("<w:text/>");
            case CHECKBOX -> b.append("<w14:checkbox><w14:checked w14:val=\"").append(f.checked() ? 1 : 0).append("\"/><w14:checkedState w14:val=\"2612\" w14:font=\"MS Gothic\"/><w14:uncheckedState w14:val=\"2610\" w14:font=\"MS Gothic\"/></w14:checkbox>");
            case DROPDOWN -> { b.append("<w:dropDownList>"); for (String o : f.options()) b.append("<w:listItem w:displayText=\"").append(OoxmlXml.escape(o)).append("\" w:value=\"").append(OoxmlXml.escape(o)).append("\"/>"); b.append("</w:dropDownList>"); }
            case DATE -> b.append("<w:date><w:dateFormat w:val=\"dd/MM/yyyy\"/><w:lid w:val=\"pt-BR\"/><w:storeMappedDataAs w:val=\"dateTime\"/><w:calendar w:val=\"gregorian\"/></w:date>");
        }
        b.append("</w:sdtPr><w:sdtContent><w:r>").append(rPr).append("<w:t xml:space=\"preserve\">").append(OoxmlXml.escape(f.kind() == WordFormField.Kind.CHECKBOX ? f.display() : placeholder ? f.placeholder() : f.value())).append("</w:t></w:r></w:sdtContent></w:sdt>");
    }
    private void preview(StringBuilder b, WordInlineObject object, String rPr, Part part, Map<String,String> data) throws IOException {
        int scale = 2;
        int w = Math.max(1,Math.min(4000,Math.round(object.width()*scale))), h = Math.max(1,Math.min(4000,Math.round(object.height()*scale)));
        BufferedImage image = new BufferedImage(w,h,BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { g.scale(scale,scale); registry.paint(g,object.withRotation(0),new Rectangle2D.Float(0,0,object.width(),object.height()),document); } finally { g.dispose(); }
        ByteArrayOutputStream png = new ByteArrayOutputStream(); ImageIO.write(image,"png",png);
        WordResource resource = WordResource.of(png.toByteArray(),"image/png");
        String target = mediaNames.computeIfAbsent(resource.id(),id -> "word/media/st" + id + ".png");
        parts.put(target,resource.data());
        String rel = part.rels.add(DocxNames.REL_IMAGE,target,false);
        customData.put(object.id(),data);
        String graphic = DocxDrawingXml.picture(rel,object.width(),object.height(),WordCrop.NONE,object.rotation(),"swingtools:" + object.id());
        b.append("<w:r>").append(rPr).append(DocxDrawingXml.wrap(object,docPr++,"swingtools:" + object.id(),DocxNames.PIC,graphic)).append("</w:r>");
    }
    private String chart(WordChart chart, Part part) throws IOException {
        int n = ++chartCounter;
        String name = "word/charts/stchart" + n + ".xml", workbook = "word/embeddings/stchart" + n + ".xlsx";
        while (parts.containsKey(name)) { n = ++chartCounter; name = "word/charts/stchart" + n + ".xml"; workbook = "word/embeddings/stchart" + n + ".xlsx"; }
        DocxRelationships chartRels = new DocxRelationships(name);
        String rel = chartRels.add(DocxNames.REL_PACKAGE,workbook,false);
        parts.put(workbook,XlsxWriter.workbook(chart));
        parts.put(name,DocxChartXml.write(chart,rel).getBytes(StandardCharsets.UTF_8));
        parts.put(DocxRelationships.relsName(name),chartRels.bytes());
        overrides.put(name,DocxNames.CT_CHART);
        return part.rels.add(DocxNames.REL_CHART,name,false);
    }

    private void table(StringBuilder b, WordTable t, Part part, boolean body) throws IOException {
        Map<String,String> props = new LinkedHashMap<>();
        if (t.styleId() != null) props.put("tblStyle","<w:tblStyle w:val=\"" + OoxmlXml.escape(t.styleId()) + "\"/>");
        props.put("tblW","<w:tblW w:w=\"" + DocxStylesXml.twips(t.width()) + "\" w:type=\"dxa\"/>");
        if (t.alignment() != WordTable.Alignment.LEFT) props.put("jc","<w:jc w:val=\"" + (t.alignment() == WordTable.Alignment.CENTER ? "center" : "right") + "\"/>");
        String border = border(t.border());
        props.put("tblBorders","<w:tblBorders>" + side("top",border) + side("left",border) + side("bottom",border) + side("right",border) + side("insideH",border) + side("insideV",border) + "</w:tblBorders>");
        props.put("tblLayout","<w:tblLayout w:type=\"fixed\"/>");
        props.put("tblCellMar","<w:tblCellMar><w:left w:w=\"" + DocxStylesXml.twips(t.cellPadding()) + "\" w:type=\"dxa\"/><w:right w:w=\"" + DocxStylesXml.twips(t.cellPadding()) + "\" w:type=\"dxa\"/></w:tblCellMar>");
        props.put("tblLook","<w:tblLook w:val=\"04A0\" w:firstRow=\"1\" w:lastRow=\"0\" w:firstColumn=\"1\" w:lastColumn=\"0\" w:noHBand=\"0\" w:noVBand=\"1\"/>");
        b.append("<w:tbl><w:tblPr>").append(ordered(props,t.extras(),TBLPR_ORDER)).append("</w:tblPr><w:tblGrid>");
        for (float w : t.columnWidths()) b.append("<w:gridCol w:w=\"").append(DocxStylesXml.twips(w)).append("\"/>");
        b.append("</w:tblGrid>");
        for (WordTableRow row : t.rows()) {
            Map<String,String> rp = new LinkedHashMap<>();
            if (row.cantSplit()) rp.put("cantSplit","<w:cantSplit/>");
            if (row.height() > 0) rp.put("trHeight","<w:trHeight w:val=\"" + DocxStylesXml.twips(row.height()) + "\"/>");
            if (row.header()) rp.put("tblHeader","<w:tblHeader/>");
            b.append("<w:tr>");
            String trPr = ordered(rp,row.extras(),TRPR_ORDER);
            if (!trPr.isEmpty()) b.append("<w:trPr>").append(trPr).append("</w:trPr>");
            for (int c = 0; c < row.cells().size(); c++) {
                WordTableCell cell = row.cells().get(c);
                Map<String,String> cp = new LinkedHashMap<>();
                cp.put("tcW","<w:tcW w:w=\"" + DocxStylesXml.twips(t.spanWidth(row.columnOf(c),cell.gridSpan())) + "\" w:type=\"dxa\"/>");
                if (cell.gridSpan() > 1) cp.put("gridSpan","<w:gridSpan w:val=\"" + cell.gridSpan() + "\"/>");
                if (cell.verticalMerge() == WordTableCell.Merge.RESTART) cp.put("vMerge","<w:vMerge w:val=\"restart\"/>");
                if (cell.verticalMerge() == WordTableCell.Merge.CONTINUE) cp.put("vMerge","<w:vMerge/>");
                if (cell.border() != null) { String cb = border(cell.border()); cp.put("tcBorders","<w:tcBorders>" + side("top",cb) + side("left",cb) + side("bottom",cb) + side("right",cb) + "</w:tcBorders>"); }
                if (cell.fill() != null) cp.put("shd","<w:shd w:val=\"clear\" w:color=\"auto\" w:fill=\"" + hex(cell.fill()) + "\"/>");
                if (cell.verticalAlign() != WordTableCell.VerticalAlign.TOP) cp.put("vAlign","<w:vAlign w:val=\"" + (cell.verticalAlign() == WordTableCell.VerticalAlign.CENTER ? "center" : "bottom") + "\"/>");
                b.append("<w:tc><w:tcPr>").append(ordered(cp,cell.extras(),TCPR_ORDER)).append("</w:tcPr>");
                blocks(b,cell.blocks(),part,body);
                b.append("</w:tc>");
            }
            b.append("</w:tr>");
        }
        b.append("</w:tbl>");
    }
    private static String border(WordBorder border) {
        if (!border.visible()) return "w:val=\"nil\"";
        String val = switch (border.style()) { case DOUBLE -> "double"; case DASHED -> "dashed"; case DOTTED -> "dotted"; default -> "single"; };
        return "w:val=\"" + val + "\" w:sz=\"" + Math.max(2,Math.round(border.width()*8)) + "\" w:space=\"0\" w:color=\"" + hex(border.color()) + "\"";
    }
    private static String side(String name, String attributes) { return "<w:" + name + " " + attributes + "/>"; }

    private void toc(StringBuilder b, WordTableOfContents toc) {
        b.append("<w:sdt><w:sdtPr><w:docPartObj><w:docPartGallery w:val=\"Table of Contents\"/><w:docPartUnique/></w:docPartObj></w:sdtPr><w:sdtContent>");
        if (!toc.title().isBlank()) b.append("<w:p><w:pPr><w:pStyle w:val=\"TOCHeading\"/></w:pPr><w:r><w:t xml:space=\"preserve\">").append(OoxmlXml.escape(toc.title())).append("</w:t></w:r></w:p>");
        List<WordParagraph> headings = document.paragraphs().stream().filter(p -> headingBookmarks.containsKey(p.id()) && p.style().headingLevel() <= toc.maxLevel()).toList();
        String begin = "<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r><w:r><w:instrText xml:space=\"preserve\"> TOC \\o \"1-" + toc.maxLevel() + "\" \\h \\z \\u </w:instrText></w:r><w:r><w:fldChar w:fldCharType=\"separate\"/></w:r>";
        if (headings.isEmpty()) { b.append("<w:p>").append(begin).append("<w:r><w:t>Nenhuma entrada de sumário encontrada.</w:t></w:r><w:r><w:fldChar w:fldCharType=\"end\"/></w:r></w:p>"); }
        for (int i = 0; i < headings.size(); i++) {
            WordParagraph h = headings.get(i); String mark = headingBookmarks.get(h.id());
            int level = Math.min(9,Math.max(1,h.style().headingLevel()));
            b.append("<w:p><w:pPr><w:pStyle w:val=\"TOC").append(level).append("\"/><w:tabs><w:tab w:val=\"right\" w:leader=\"dot\" w:pos=\"").append(DocxStylesXml.twips(document.pageSettings().contentWidth()-2)).append("\"/></w:tabs></w:pPr>");
            if (i == 0) b.append(begin);
            b.append("<w:hyperlink w:anchor=\"").append(mark).append("\" w:history=\"1\"><w:r><w:t xml:space=\"preserve\">").append(OoxmlXml.escape(h.plainText().replace(' ',' '))).append("</w:t></w:r><w:r><w:tab/></w:r>")
                    .append("<w:r><w:fldChar w:fldCharType=\"begin\"/></w:r><w:r><w:instrText xml:space=\"preserve\"> PAGEREF ").append(mark).append(" \\h </w:instrText></w:r><w:r><w:fldChar w:fldCharType=\"separate\"/></w:r><w:r><w:t>")
                    .append(headingPages.getOrDefault(h.id(),1)).append("</w:t></w:r><w:r><w:fldChar w:fldCharType=\"end\"/></w:r></w:hyperlink>");
            if (i == headings.size()-1) b.append("<w:r><w:fldChar w:fldCharType=\"end\"/></w:r>");
            b.append("</w:p>");
        }
        b.append("</w:sdtContent></w:sdt>");
    }

    private String sectPr(WordPageSettings s) {
        StringBuilder b = new StringBuilder("<w:sectPr>").append(headerXmlRefs);
        b.append("<w:pgSz w:w=\"").append(DocxStylesXml.twips(s.width())).append("\" w:h=\"").append(DocxStylesXml.twips(s.height())).append('"').append(s.landscape() ? " w:orient=\"landscape\"" : "").append("/>");
        b.append("<w:pgMar w:top=\"").append(DocxStylesXml.twips(s.top())).append("\" w:right=\"").append(DocxStylesXml.twips(s.right())).append("\" w:bottom=\"").append(DocxStylesXml.twips(s.bottom()))
                .append("\" w:left=\"").append(DocxStylesXml.twips(s.left())).append("\" w:header=\"").append(DocxStylesXml.twips(s.headerDistance())).append("\" w:footer=\"").append(DocxStylesXml.twips(s.footerDistance())).append("\" w:gutter=\"0\"/>");
        if (s.pageNumberStart() > 0) b.append("<w:pgNumType w:start=\"").append(s.pageNumberStart()).append("\"/>");
        b.append("<w:cols w:space=\"").append(DocxStylesXml.twips(s.columnSpacing())).append('"').append(s.columns() > 1 ? " w:num=\"" + s.columns() + "\"" : "").append("/>");
        if (document.parts().headers().differentFirst()) b.append("<w:titlePg/>");
        return b.append("</w:sectPr>").toString();
    }

    private void headers(Part main) throws IOException {
        WordHeaders headers = document.parts().headers();
        StringBuilder refs = new StringBuilder(), footers = new StringBuilder();
        boolean unchanged = origin != null && headers.equals(origin.document().parts().headers());
        for (WordHeaders.Kind kind : WordHeaders.Kind.values()) {
            boolean header = kind.name().endsWith("HEADER");
            String type = kind.name().startsWith("FIRST") ? "first" : kind.name().startsWith("EVEN") ? "even" : "default";
            String id;
            if (unchanged) { id = origin.headerReferences().get(kind); if (id == null) continue; }
            else {
                List<WordBlock> blocks = headers.get(kind);
                if (blocks.isEmpty()) continue;
                String name;
                do { name = "word/st" + (header ? "header" : "footer") + (++partCounter) + ".xml"; } while (parts.containsKey(name));
                Part part = new Part(name,new DocxRelationships(name));
                StringBuilder b = new StringBuilder();
                List<WordBlock> content = new ArrayList<>(blocks);
                if (!(content.getLast() instanceof WordParagraph)) content.add(WordParagraph.of(""));
                blocks(b,content,part,false);
                String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<w:" + (header ? "hdr" : "ftr") + namespaces() + ">" + b + "</w:" + (header ? "hdr" : "ftr") + ">";
                byte[] bytes = xml.getBytes(StandardCharsets.UTF_8); OoxmlXml.parse(bytes);
                parts.put(name,bytes);
                if (!part.rels.isEmpty()) parts.put(DocxRelationships.relsName(name),part.rels.bytes());
                overrides.put(name,header ? DocxNames.CT_HEADER : DocxNames.CT_FOOTER);
                id = main.rels.add(header ? DocxNames.REL_HEADER : DocxNames.REL_FOOTER,name,false);
            }
            (header ? refs : footers).append("<w:").append(header ? "headerReference" : "footerReference").append(" w:type=\"").append(type).append("\" r:id=\"").append(id).append("\"/>");
        }
        headerXmlRefs = refs.toString() + footers;
    }

    private void notes(Part main, WordNote.Kind kind) throws IOException {
        boolean foot = kind == WordNote.Kind.FOOTNOTE;
        List<WordNote> notes = document.parts().notes().values().stream().filter(n -> n.kind() == kind).toList();
        String type = foot ? "footnotes" : "endnotes";
        Optional<DocxRelationships.Relationship> existing = main.rels.byType(type);
        if (notes.isEmpty() && existing.isEmpty()) return;
        boolean unchanged = origin != null && existing.isPresent() && notes.equals(origin.document().parts().notes().values().stream().filter(n -> n.kind() == kind).toList());
        if (unchanged) return;
        String name = existing.map(DocxRelationships.Relationship::target).orElse(foot ? "word/footnotes.xml" : "word/endnotes.xml");
        Part part = new Part(name,origin != null && existing.isPresent() ? DocxRelationships.read(origin.source(),name) : new DocxRelationships(name));
        String element = foot ? "footnote" : "endnote";
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<w:").append(type).append(namespaces()).append('>');
        b.append("<w:").append(element).append(" w:type=\"separator\" w:id=\"-1\"><w:p><w:pPr><w:spacing w:after=\"0\" w:line=\"240\" w:lineRule=\"auto\"/></w:pPr><w:r><w:separator/></w:r></w:p></w:").append(element).append('>');
        b.append("<w:").append(element).append(" w:type=\"continuationSeparator\" w:id=\"0\"><w:p><w:pPr><w:spacing w:after=\"0\" w:line=\"240\" w:lineRule=\"auto\"/></w:pPr><w:r><w:continuationSeparator/></w:r></w:p></w:").append(element).append('>');
        for (WordNote note : notes) {
            b.append("<w:").append(element).append(" w:id=\"").append(noteNumbers.get(note.id())).append("\">");
            boolean first = true;
            for (WordParagraph p : note.paragraphs()) {
                if (first) {
                    StringBuilder para = new StringBuilder(); paragraph(para,p,part,false);
                    String ref = "<w:r><w:rPr><w:rStyle w:val=\"" + (foot ? "FootnoteReference" : "EndnoteReference") + "\"/><w:vertAlign w:val=\"superscript\"/></w:rPr><w:" + (foot ? "footnoteRef" : "endnoteRef") + "/></w:r><w:r><w:t xml:space=\"preserve\"> </w:t></w:r>";
                    int at = para.indexOf("</w:pPr>") + "</w:pPr>".length();
                    para.insert(at,ref); b.append(para); first = false;
                } else paragraph(b,p,part,false);
            }
            b.append("</w:").append(element).append('>');
        }
        b.append("</w:").append(type).append('>');
        byte[] bytes = b.toString().getBytes(StandardCharsets.UTF_8); OoxmlXml.parse(bytes);
        parts.put(name,bytes);
        if (!part.rels.isEmpty()) parts.put(DocxRelationships.relsName(name),part.rels.bytes());
        overrides.put(name,foot ? DocxNames.CT_FOOTNOTES : DocxNames.CT_ENDNOTES);
        main.rels.add(foot ? DocxNames.REL_FOOTNOTES : DocxNames.REL_ENDNOTES,name,false);
    }

    private void comments(Part main) throws IOException {
        List<WordComment> comments = document.parts().comments();
        Optional<DocxRelationships.Relationship> existing = main.rels.byType("comments");
        if (comments.isEmpty() && existing.isEmpty()) return;
        if (origin != null && existing.isPresent() && comments.equals(origin.document().parts().comments())) return;
        String name = existing.map(DocxRelationships.Relationship::target).orElse("word/comments.xml");
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<w:comments").append(namespaces()).append('>');
        StringBuilder ex = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<w15:commentsEx xmlns:w15=\"").append(DocxNames.W15).append("\" xmlns:mc=\"").append(DocxNames.MC).append("\" mc:Ignorable=\"w15\">");
        Map<String,String> paraIds = new HashMap<>();
        for (WordComment c : comments) paraIds.put(c.id(),String.format(Locale.ROOT,"%08X",(c.id().hashCode() & 0x3fffffff) | 0x10000000));
        for (WordComment c : comments) {
            OoxmlXml.validateText(c.text()); OoxmlXml.validateText(c.author());
            b.append("<w:comment w:id=\"").append(commentNumbers.get(c.id())).append("\" w:author=\"").append(OoxmlXml.escape(c.author())).append("\" w:date=\"").append(c.date().truncatedTo(ChronoUnit.SECONDS))
                    .append("\" w:initials=\"").append(OoxmlXml.escape(c.initials())).append("\">");
            String[] lines = c.text().split("\n",-1);
            for (int i = 0; i < lines.length; i++) {
                b.append("<w:p").append(i == lines.length-1 ? " w14:paraId=\"" + paraIds.get(c.id()) + "\" w14:textId=\"77777777\"" : "").append('>');
                if (i == 0) b.append("<w:r><w:annotationRef/></w:r>");
                b.append("<w:r><w:t xml:space=\"preserve\">").append(OoxmlXml.escape(lines[i])).append("</w:t></w:r></w:p>");
            }
            b.append("</w:comment>");
            ex.append("<w15:commentEx w15:paraId=\"").append(paraIds.get(c.id())).append('"');
            if (c.parentId() != null && paraIds.containsKey(c.parentId())) ex.append(" w15:paraIdParent=\"").append(paraIds.get(c.parentId())).append('"');
            ex.append(" w15:done=\"").append(c.resolved() ? 1 : 0).append("\"/>");
        }
        b.append("</w:comments>"); ex.append("</w15:commentsEx>");
        byte[] bytes = b.toString().getBytes(StandardCharsets.UTF_8); OoxmlXml.parse(bytes);
        parts.put(name,bytes); overrides.put(name,DocxNames.CT_COMMENTS);
        main.rels.add(DocxNames.REL_COMMENTS,name,false);
        String exName = main.rels.all().stream().filter(r -> r.type().equals(DocxNames.REL_COMMENTS_EXTENDED)).map(DocxRelationships.Relationship::target).findFirst().orElse("word/commentsExtended.xml");
        parts.put(exName,ex.toString().getBytes(StandardCharsets.UTF_8)); overrides.put(exName,DocxNames.CT_COMMENTS_EXTENDED);
        main.rels.add(DocxNames.REL_COMMENTS_EXTENDED,exName,false);
    }

    private void customXml(Part main) {
        String item = "customXml/stitem1.xml", props = "customXml/stitemProps1.xml";
        if (customData.isEmpty()) { if (parts.containsKey(item)) parts.put(item,("<?xml version=\"1.0\" encoding=\"UTF-8\"?><objects xmlns=\"" + DocxReader.CUSTOM_NS + "\"/>").getBytes(StandardCharsets.UTF_8)); return; }
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><objects xmlns=\"").append(DocxReader.CUSTOM_NS).append("\">");
        customData.forEach((id,data) -> {
            b.append("<object id=\"").append(OoxmlXml.escape(id)).append("\" type=\"").append(OoxmlXml.escape(data.get("$type"))).append("\">");
            data.forEach((k,v) -> { if (!k.equals("$type")) b.append("<entry key=\"").append(OoxmlXml.escape(k)).append("\">").append(OoxmlXml.escape(v)).append("</entry>"); });
            b.append("</object>");
        });
        b.append("</objects>");
        parts.put(item,b.toString().getBytes(StandardCharsets.UTF_8));
        parts.put(props,("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><ds:datastoreItem ds:itemID=\"{6C4A3E1B-5E86-4F7C-9E07-5A0C2D3F7A11}\" xmlns:ds=\"http://schemas.openxmlformats.org/officeDocument/2006/customXml\"><ds:schemaRefs/></ds:datastoreItem>").getBytes(StandardCharsets.UTF_8));
        DocxRelationships itemRels = new DocxRelationships(item);
        itemRels.add(DocxNames.REL_CUSTOM_XML_PROPS,props,false);
        parts.put(DocxRelationships.relsName(item),itemRels.bytes());
        overrides.put(props,DocxNames.CT_CUSTOM_PROPS);
        main.rels.add(DocxNames.REL_CUSTOM_XML,item,false);
    }

    private void contentTypes() throws IOException {
        Document types; Element root;
        if (parts.containsKey("[Content_Types].xml")) { types = OoxmlXml.parse(parts.get("[Content_Types].xml")); root = types.getDocumentElement(); }
        else { types = OoxmlXml.parse(("<Types xmlns=\"" + DocxNames.CONTENT_TYPES + "\"/>").getBytes(StandardCharsets.UTF_8)); root = types.getDocumentElement(); }
        Set<String> defaults = new HashSet<>(), explicit = new HashSet<>();
        for (Element e : OoxmlXml.children(root)) { if (e.getLocalName().equals("Default")) defaults.add(e.getAttribute("Extension").toLowerCase(Locale.ROOT)); else explicit.add(e.getAttribute("PartName")); }
        Map<String,String> wanted = new LinkedHashMap<>();
        wanted.put("rels","application/vnd.openxmlformats-package.relationships+xml"); wanted.put("xml","application/xml");
        wanted.put("png","image/png"); wanted.put("jpeg","image/jpeg"); wanted.put("jpg","image/jpeg"); wanted.put("gif","image/gif"); wanted.put("bmp","image/bmp");
        wanted.put("tiff","image/tiff"); wanted.put("emf","image/x-emf"); wanted.put("wmf","image/x-wmf"); wanted.put("svg","image/svg+xml"); wanted.put("xlsx",DocxNames.CT_XLSX); wanted.put("bin","application/octet-stream");
        Element firstOverride = OoxmlXml.child(root,"Override");
        for (var e : wanted.entrySet()) if (!defaults.contains(e.getKey())) {
            Element d = types.createElementNS(DocxNames.CONTENT_TYPES,"Default"); d.setAttribute("Extension",e.getKey()); d.setAttribute("ContentType",e.getValue());
            root.insertBefore(d,firstOverride);
        }
        for (var e : overrides.entrySet()) {
            String partName = "/" + e.getKey();
            for (Element o : OoxmlXml.children(root,"Override")) if (o.getAttribute("PartName").equals(partName)) root.removeChild(o);
            Element o = types.createElementNS(DocxNames.CONTENT_TYPES,"Override"); o.setAttribute("PartName",partName); o.setAttribute("ContentType",e.getValue()); root.appendChild(o);
        }
        parts.put("[Content_Types].xml",OoxmlXml.bytes(types));
    }
    private static String hex(int rgb) { return String.format(Locale.ROOT,"%06X",rgb & 0xffffff); }
}
