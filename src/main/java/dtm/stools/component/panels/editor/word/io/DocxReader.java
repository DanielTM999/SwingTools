package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import dtm.stools.component.panels.editor.word.math.WordMath;
import dtm.stools.component.panels.editor.word.model.*;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class DocxReader {
    static final String CUSTOM_NS = "urn:swingtools:word";
    private static final Pattern TOC_LEVELS = Pattern.compile("\\\\o\\s+\"(\\d)-(\\d)\"");
    private static final Map<String,Integer> HIGHLIGHT = Map.ofEntries(Map.entry("yellow",0xFFFF00),Map.entry("green",0x00FF00),Map.entry("cyan",0x00FFFF),Map.entry("magenta",0xFF00FF),
            Map.entry("blue",0x0000FF),Map.entry("red",0xFF0000),Map.entry("darkBlue",0x000080),Map.entry("darkCyan",0x008080),Map.entry("darkGreen",0x008000),Map.entry("darkMagenta",0x800080),
            Map.entry("darkRed",0x800000),Map.entry("darkYellow",0x808000),Map.entry("darkGray",0x808080),Map.entry("lightGray",0xC0C0C0),Map.entry("black",0x000000),Map.entry("white",0xFFFFFF));
    private static final Set<String> RPR_KNOWN = Set.of("rStyle","rFonts","b","i","u","strike","color","sz","szCs","highlight","shd","vertAlign");
    private static final Set<String> PPR_KNOWN = Set.of("pStyle","keepNext","pageBreakBefore","numPr","tabs","spacing","ind","jc","outlineLvl","shd","sectPr");
    private final OpcPackage.Limits limits;
    private DocxReadState state;
    private OpcPackage source;

    DocxReader(OpcPackage.Limits limits) { this.limits = limits; }

    WordImportResult read(InputStream input) throws IOException {
        byte[] original = OpcPackage.readBounded(input,limits.compressedBytes());
        source = OpcPackage.read(original,limits);
        state = new DocxReadState();
        for (String name : source.names()) if (name.startsWith("_xmlsignatures/")) state.blocking.add("Assinatura digital: a edição invalidaria a assinatura");
        String main = null;
        for (Element relationship : OoxmlXml.children(OoxmlXml.parse(source.part("_rels/.rels")).getDocumentElement())) {
            if (relationship.getAttribute("Type").endsWith("/officeDocument")) {
                if (main != null || "External".equals(relationship.getAttribute("TargetMode"))) throw new IOException("Invalid office document relationship");
                main = relationship.getAttribute("Target"); if (main.startsWith("/")) main = main.substring(1);
                try { OpcPackage.validateName(main); } catch (IllegalArgumentException e) { throw new IOException("Invalid main part",e); }
            }
        }
        if (main == null) throw new IOException("Missing office document relationship");
        boolean documentContentType = false;
        for (Element declaration : OoxmlXml.children(OoxmlXml.parse(source.part("[Content_Types].xml")).getDocumentElement()))
            if (("/" + main).equals(declaration.getAttribute("PartName"))) documentContentType = DocxNames.CT_MAIN.equals(declaration.getAttribute("ContentType"));
        if (!documentContentType) throw new IOException("Only non-macro DOCX documents are supported");
        Element root = OoxmlXml.parse(source.part(main)).getDocumentElement(); String ns = root.getNamespaceURI();
        if (!"document".equals(root.getLocalName()) || !DocxNames.isWord(ns)) throw new IOException("Not a Word document");
        Element body = OoxmlXml.child(root,"body"); if (body == null) throw new IOException("Missing document body");
        state.namespace = ns;
        Map<String,String> namespaces = new LinkedHashMap<>();
        NamedNodeMap attributes = root.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) { Node a = attributes.item(i); if ("http://www.w3.org/2000/xmlns/".equals(a.getNamespaceURI()) && a.getLocalName() != null && !a.getLocalName().equals("xmlns")) namespaces.put(a.getLocalName(),a.getNodeValue()); }
        String ignorable = root.getAttributeNS(DocxNames.MC,"Ignorable");
        DocxReadContext context = new DocxReadContext(source,main,state);
        WordStyleSheet parsedStyles = null;
        Optional<DocxRelationships.Relationship> styles = context.relationships.byType("styles");
        if (styles.isPresent() && source.contains(styles.get().target())) {
            parsedStyles = DocxStylesXml.readStyles(OoxmlXml.parse(source.part(styles.get().target())).getDocumentElement());
            state.styles = parsedStyles.merge(WordStyleSheet.defaults());
        }
        Optional<DocxRelationships.Relationship> numbering = context.relationships.byType("numbering");
        if (numbering.isPresent() && source.contains(numbering.get().target())) state.numbering = DocxStylesXml.readNumbering(OoxmlXml.parse(source.part(numbering.get().target())).getDocumentElement());
        boolean evenAndOdd = false;
        Optional<DocxRelationships.Relationship> settings = context.relationships.byType("settings");
        if (settings.isPresent() && source.contains(settings.get().target())) {
            Element s = OoxmlXml.parse(source.part(settings.get().target())).getDocumentElement();
            Element protection = OoxmlXml.child(s,"documentProtection");
            if (protection != null && DocxStylesXml.on(OoxmlXml.attr(protection,"enforcement")) && !OoxmlXml.attr(protection,"enforcement").isEmpty() && !"none".equals(OoxmlXml.attr(protection,"edit")))
                state.blocking.add("Documento protegido contra edição (" + OoxmlXml.attr(protection,"edit") + ")");
            Element eo = OoxmlXml.child(s,"evenAndOddHeaders");
            evenAndOdd = eo != null && DocxStylesXml.on(OoxmlXml.attr(eo,"val"));
        }
        for (DocxRelationships.Relationship r : context.relationships.all()) if (r.kind().equals("customXml") && !r.external() && source.contains(r.target())) readCustomXml(r.target());
        Element finalSection = null;
        for (Element e : OoxmlXml.children(body)) if (e.getLocalName().equals("sectPr")) finalSection = e;
        WordPageSettings page = finalSection == null ? WordPageSettings.A4 : section(finalSection);
        state.settings = page;
        List<WordBlock> blocks = blocks(body,context);
        if (blocks.isEmpty() || !(blocks.getLast() instanceof WordParagraph)) blocks.add(WordParagraph.of(""));
        WordHeaders headers = WordHeaders.EMPTY; Map<WordHeaders.Kind,String> headerRefs = new EnumMap<>(WordHeaders.Kind.class);
        if (finalSection != null) {
            for (Element ref : OoxmlXml.children(finalSection)) {
                boolean header = ref.getLocalName().equals("headerReference");
                if (!header && !ref.getLocalName().equals("footerReference")) continue;
                String type = OoxmlXml.attr(ref,"type"), id = OoxmlXml.attr(ref,DocxNames.R,"id");
                WordHeaders.Kind kind = switch (type) { case "first" -> header ? WordHeaders.Kind.FIRST_HEADER : WordHeaders.Kind.FIRST_FOOTER; case "even" -> header ? WordHeaders.Kind.EVEN_HEADER : WordHeaders.Kind.EVEN_FOOTER; default -> header ? WordHeaders.Kind.HEADER : WordHeaders.Kind.FOOTER; };
                Optional<DocxRelationships.Relationship> r = context.relationship(id);
                if (r.isEmpty() || r.get().external() || !source.contains(r.get().target())) continue;
                DocxReadContext partContext = new DocxReadContext(source,r.get().target(),state);
                List<WordBlock> content = blocks(OoxmlXml.parse(source.part(r.get().target())).getDocumentElement(),partContext);
                headers = headers.with(kind,content); headerRefs.put(kind,id);
            }
            headers = headers.withOptions(OoxmlXml.child(finalSection,"titlePg") != null && DocxStylesXml.on(OoxmlXml.attr(OoxmlXml.child(finalSection,"titlePg"),"val")),evenAndOdd);
        }
        Map<String,WordNote> notes = new LinkedHashMap<>();
        readNotes(context,"footnotes","footnote",WordNote.Kind.FOOTNOTE,"f",notes);
        readNotes(context,"endnotes","endnote",WordNote.Kind.ENDNOTE,"e",notes);
        List<WordComment> comments = readComments(context);
        WordParts parts = new WordParts(state.styles,state.numbering,state.resources,headers,notes,comments,Map.of());
        WordDocument document;
        try { document = new WordDocument(blocks,page,parts); } catch (IllegalArgumentException e) { throw new IOException("Unsupported document values",e); }
        return new WordImportResult(document,new ArrayList<>(state.diagnostics),new ArrayList<>(state.blocking),original,source,main,ns,namespaces,ignorable,state.originals,headerRefs);
    }

    private void readCustomXml(String part) throws IOException {
        Element root = OoxmlXml.parse(source.part(part)).getDocumentElement();
        if (!CUSTOM_NS.equals(root.getNamespaceURI()) || !"objects".equals(root.getLocalName())) return;
        for (Element object : OoxmlXml.children(root,"object")) {
            Map<String,String> data = new LinkedHashMap<>();
            data.put("$type",object.getAttribute("type"));
            for (Element entry : OoxmlXml.children(object,"entry")) data.put(entry.getAttribute("key"),entry.getTextContent());
            state.customData.put(object.getAttribute("id"),data);
        }
    }

    private List<WordBlock> blocks(Element container, DocxReadContext context) throws IOException {
        List<WordBlock> result = new ArrayList<>();
        for (Element e : OoxmlXml.children(container)) {
            if (!DocxNames.isWord(e.getNamespaceURI())) { result.add(opaqueBlock(e,context)); continue; }
            switch (e.getLocalName()) {
                case "p" -> result.add(paragraph(e,context));
                case "tbl" -> result.add(table(e,context));
                case "sectPr", "bookmarkStart", "bookmarkEnd", "proofErr", "permStart", "permEnd", "tblPr", "tcPr", "trPr" -> { }
                case "commentRangeStart" -> state.activeComments.add(OoxmlXml.attr(e,"id"));
                case "commentRangeEnd" -> state.activeComments.remove(OoxmlXml.attr(e,"id"));
                case "sdt" -> {
                    Element gallery = OoxmlXml.path(e,"sdtPr","docPartObj","docPartGallery");
                    if (gallery != null && OoxmlXml.attr(gallery,"val").equals("Table of Contents")) result.add(toc(e));
                    else result.add(opaqueBlock(e,context));
                }
                default -> result.add(opaqueBlock(e,context));
            }
        }
        return result;
    }
    private WordBlock opaqueBlock(Element e, DocxReadContext context) {
        String label = e.getLocalName().equals("sdt") ? "controle de conteúdo" : e.getNodeName();
        context.note("Conteúdo preservado sem editor: " + label);
        return new WordOpaqueBlock(UUID.randomUUID(),label,OoxmlXml.serialize(e),OoxmlXml.text(e));
    }
    private WordTableOfContents toc(Element sdt) {
        int max = 3; String title = "Sumário";
        for (Element instr : OoxmlXml.descendants(sdt,"instrText")) { Matcher m = TOC_LEVELS.matcher(instr.getTextContent()); if (m.find()) max = Math.max(1,Math.min(9,Integer.parseInt(m.group(2)))); }
        Element content = OoxmlXml.child(sdt,"sdtContent");
        Element first = OoxmlXml.child(content,"p");
        if (first != null) {
            Element style = OoxmlXml.path(first,"pPr","pStyle");
            if (style != null && OoxmlXml.attr(style,"val").toLowerCase(Locale.ROOT).contains("tocheading")) title = OoxmlXml.text(first);
            else if (OoxmlXml.descendant(first,"fldChar") == null && !OoxmlXml.text(first).isBlank()) title = OoxmlXml.text(first);
            else title = "";
        }
        return new WordTableOfContents(UUID.randomUUID(),title,max);
    }

    WordPageSettings section(Element sectPr) {
        WordPageSettings a = WordPageSettings.A4;
        float width = a.width(), height = a.height(), top = a.top(), right = a.right(), bottom = a.bottom(), left = a.left(), header = 35.4f, footer = 35.4f, spacing = 36;
        int columns = 1, start = 0;
        try {
            Element size = OoxmlXml.child(sectPr,"pgSz");
            if (size != null) { width = twips(size,"w",width); height = twips(size,"h",height); }
            Element margin = OoxmlXml.child(sectPr,"pgMar");
            if (margin != null) {
                top = Math.abs(twips(margin,"top",top)); right = twips(margin,"right",right); bottom = Math.abs(twips(margin,"bottom",bottom)); left = twips(margin,"left",left);
                header = twips(margin,"header",header); footer = twips(margin,"footer",footer);
                if (twips(margin,"gutter",0) != 0) state.diagnostics.add("Margem de encadernação ignorada na composição");
            }
            Element cols = OoxmlXml.child(sectPr,"cols");
            if (cols != null) { String num = OoxmlXml.attr(cols,"num"); if (!num.isEmpty()) columns = Math.max(1,Math.min(10,Integer.parseInt(num))); spacing = twips(cols,"space",spacing); }
            Element numbering = OoxmlXml.child(sectPr,"pgNumType");
            if (numbering != null && !OoxmlXml.attr(numbering,"start").isEmpty()) start = Math.max(0,Integer.parseInt(OoxmlXml.attr(numbering,"start")));
            Element type = OoxmlXml.child(sectPr,"type");
            if (type != null && !"nextPage".equals(OoxmlXml.attr(type,"val"))) state.diagnostics.add("Quebra de seção \"" + OoxmlXml.attr(type,"val") + "\" composta como próxima página");
            return new WordPageSettings(width,height,top,right,bottom,left,columns,spacing,header,footer,start);
        } catch (IOException | IllegalArgumentException e) {
            state.diagnostics.add("Configuração de página inválida substituída por A4");
            return WordPageSettings.A4;
        }
    }
    private static float twips(Element e, String name, float fallback) throws IOException {
        String v = OoxmlXml.attr(e,name); return v.isEmpty() ? fallback : DocxStylesXml.number(v)/20;
    }

    private final class InlineState {
        final DocxReadContext context; final WordTextStyle base; final List<WordInline> out = new ArrayList<>(); final List<String> bookmarks = new ArrayList<>();
        String link; WordRevision revision;
        int fieldDepth; boolean fieldResult; StringBuilder instruction; int fieldStart; StringBuilder captured; boolean complex;
        InlineState(DocxReadContext context, WordTextStyle base) { this.context = context; this.base = base; }
        WordTextStyle decorate(WordTextStyle style) {
            WordTextStyle s = style;
            if (link != null) s = s.withLink(link);
            if (revision != null) s = s.withRevision(revision);
            if (!state.activeComments.isEmpty()) s = s.withComments(new ArrayList<>(state.activeComments));
            return s;
        }
    }

    WordParagraph paragraph(Element p, DocxReadContext context) throws IOException {
        Element pPr = OoxmlXml.child(p,"pPr");
        String styleId = pPr == null ? null : emptyToNull(OoxmlXml.attr(OoxmlXml.child(pPr,"pStyle"),"val"));
        WordParagraphStyle style = paragraphStyle(pPr,styleId);
        WordPageSettings sectionBreak = pPr != null && OoxmlXml.child(pPr,"sectPr") != null ? section(OoxmlXml.child(pPr,"sectPr")) : null;
        InlineState s = new InlineState(context,state.styles.resolveText(styleId));
        inlines(p,s);
        if (s.fieldDepth > 0) state.diagnostics.add("Campo que atravessa parágrafos preservado como texto");
        try { return new WordParagraph(UUID.randomUUID(),s.out,style,s.bookmarks,sectionBreak); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid paragraph",e); }
    }
    private static String emptyToNull(String v) { return v == null || v.isBlank() ? null : v; }
    private WordParagraphStyle paragraphStyle(Element pPr, String styleId) throws IOException {
        WordParagraphStyle style = state.styles.resolveParagraph(styleId);
        if (styleId == null) style = style.withStyleId(null);
        if (pPr == null) return style;
        List<String> extras = new ArrayList<>(); List<WordTabStop> tabs = new ArrayList<>(style.tabs());
        float before = style.before(), after = style.after(), line = style.lineSpacing(), left = style.leftIndent(), right = style.rightIndent(), first = style.firstLineIndent();
        WordParagraphStyle.Alignment alignment = style.alignment(); boolean keep = style.keepWithNext(), pageBreak = style.pageBreakBefore(); int heading = style.headingLevel();
        WordListRef list = null; Integer shading = style.shading();
        for (Element e : OoxmlXml.children(pPr)) {
            if (!DocxNames.isWord(e.getNamespaceURI()) || !PPR_KNOWN.contains(e.getLocalName())) { extras.add(OoxmlXml.serialize(e)); continue; }
            switch (e.getLocalName()) {
                case "keepNext" -> keep = DocxStylesXml.on(OoxmlXml.attr(e,"val"));
                case "pageBreakBefore" -> pageBreak = DocxStylesXml.on(OoxmlXml.attr(e,"val"));
                case "numPr" -> {
                    String id = OoxmlXml.attr(OoxmlXml.child(e,"numId"),"val"), level = OoxmlXml.attr(OoxmlXml.child(e,"ilvl"),"val");
                    if (!id.isEmpty() && !id.equals("0")) {
                        int lvl = 0; try { lvl = level.isEmpty() ? 0 : Math.max(0,Math.min(8,Integer.parseInt(level))); } catch (NumberFormatException ignored) {}
                        if (state.numbering.get(id).isPresent()) list = new WordListRef(id,lvl); else state.diagnostics.add("Lista sem definição de numeração: " + id);
                    }
                }
                case "tabs" -> {
                    for (Element tab : OoxmlXml.children(e,"tab")) {
                        float pos = DocxStylesXml.number(OoxmlXml.attr(tab,"pos"))/20; String val = OoxmlXml.attr(tab,"val");
                        tabs.removeIf(t -> Math.abs(t.position()-pos) < 0.5f);
                        if (val.equals("clear") || pos < 0) continue;
                        WordTabStop.Alignment a = switch (val) { case "center" -> WordTabStop.Alignment.CENTER; case "right", "end" -> WordTabStop.Alignment.RIGHT; case "decimal" -> WordTabStop.Alignment.DECIMAL; default -> WordTabStop.Alignment.LEFT; };
                        WordTabStop.Leader leader = switch (OoxmlXml.attr(tab,"leader")) { case "dot", "middleDot" -> WordTabStop.Leader.DOT; case "hyphen" -> WordTabStop.Leader.HYPHEN; case "underscore", "heavy" -> WordTabStop.Leader.UNDERSCORE; default -> WordTabStop.Leader.NONE; };
                        tabs.add(new WordTabStop(Math.min(14400,pos),a,leader));
                    }
                }
                case "spacing" -> {
                    if (!OoxmlXml.attr(e,"before").isEmpty()) before = Math.max(0,DocxStylesXml.number(OoxmlXml.attr(e,"before"))/20);
                    if (!OoxmlXml.attr(e,"after").isEmpty()) after = Math.max(0,DocxStylesXml.number(OoxmlXml.attr(e,"after"))/20);
                    if (!OoxmlXml.attr(e,"line").isEmpty()) {
                        String rule = OoxmlXml.attr(e,"lineRule");
                        if (Set.of("","auto").contains(rule)) line = Math.max(0.5f,Math.min(10,DocxStylesXml.number(OoxmlXml.attr(e,"line"))/240));
                        else { line = Math.max(0.5f,Math.min(10,DocxStylesXml.number(OoxmlXml.attr(e,"line"))/20/13.8f)); state.diagnostics.add("Espaçamento exato/mínimo de linhas aproximado"); extras.add(OoxmlXml.serialize(e)); }
                    }
                }
                case "ind" -> {
                    String l = OoxmlXml.attr(e,"left"); if (l.isEmpty()) l = OoxmlXml.attr(e,"start");
                    String r = OoxmlXml.attr(e,"right"); if (r.isEmpty()) r = OoxmlXml.attr(e,"end");
                    if (!l.isEmpty()) left = DocxStylesXml.number(l)/20;
                    if (!r.isEmpty()) right = DocxStylesXml.number(r)/20;
                    if (!OoxmlXml.attr(e,"firstLine").isEmpty()) first = DocxStylesXml.number(OoxmlXml.attr(e,"firstLine"))/20;
                    if (!OoxmlXml.attr(e,"hanging").isEmpty()) first = -DocxStylesXml.number(OoxmlXml.attr(e,"hanging"))/20;
                }
                case "jc" -> alignment = DocxStylesXml.alignment(OoxmlXml.attr(e,"val"));
                case "outlineLvl" -> { int lvl = (int)DocxStylesXml.number(OoxmlXml.attr(e,"val")); heading = lvl >= 0 && lvl < 9 ? lvl+1 : 0; }
                case "shd" -> { String fill = OoxmlXml.attr(e,"fill"); shading = fill.matches("[0-9a-fA-F]{6}") ? Integer.valueOf(Integer.parseInt(fill,16)) : null; }
                default -> { }
            }
        }
        try {
            return new WordParagraphStyle(alignment,before,after,line,left,right,first,pageBreak,heading,styleId,list,tabs,keep,shading,extras);
        } catch (IllegalArgumentException e) { throw new IOException("Invalid paragraph properties",e); }
    }

    private void inlines(Element parent, InlineState s) throws IOException {
        for (Element e : OoxmlXml.children(parent)) {
            String ns = e.getNamespaceURI(), name = e.getLocalName();
            if (DocxNames.M.equals(ns) && (name.equals("oMath") || name.equals("oMathPara"))) { math(e,s); continue; }
            if (!DocxNames.isWord(ns)) { if (!name.equals("AlternateContent")) opaqueInline(e,s,WordOpaqueObject.Level.PARAGRAPH,"elemento " + e.getNodeName()); else opaqueInline(e,s,WordOpaqueObject.Level.PARAGRAPH,"conteúdo alternativo"); continue; }
            switch (name) {
                case "pPr", "proofErr", "permStart", "permEnd", "bookmarkEnd" -> { }
                case "r" -> run(e,s);
                case "hyperlink" -> {
                    String previous = s.link;
                    String id = OoxmlXml.attr(e,DocxNames.R,"id"), anchor = OoxmlXml.attr(e,"anchor");
                    if (!id.isEmpty()) s.link = s.context.relationship(id).filter(DocxRelationships.Relationship::external).map(DocxRelationships.Relationship::target).orElse(previous);
                    else if (!anchor.isEmpty()) s.link = "#" + anchor;
                    inlines(e,s); s.link = previous;
                }
                case "ins", "moveTo", "del", "moveFrom" -> {
                    WordRevision previous = s.revision;
                    s.revision = new WordRevision(name.equals("ins") || name.equals("moveTo") ? WordRevision.Type.INSERT : WordRevision.Type.DELETE,
                            OoxmlXml.attr(e,"author").isBlank() ? "Autor" : OoxmlXml.attr(e,"author"),date(OoxmlXml.attr(e,"date")));
                    inlines(e,s); s.revision = previous;
                }
                case "bookmarkStart" -> { String b = OoxmlXml.attr(e,"name"); if (!b.isBlank() && !b.equals("_GoBack")) s.bookmarks.add(b); }
                case "commentRangeStart" -> state.activeComments.add(OoxmlXml.attr(e,"id"));
                case "commentRangeEnd" -> state.activeComments.remove(OoxmlXml.attr(e,"id"));
                case "fldSimple" -> {
                    WordField field = field(OoxmlXml.attr(e,"instr"),OoxmlXml.text(e));
                    if (field != null) s.out.add(new WordObjectRun(field,s.decorate(runStyle(OoxmlXml.path(e,"r","rPr"),s.base))));
                    else inlines(e,s);
                }
                case "smartTag", "customXml" -> { if (name.equals("customXml")) opaqueInline(e,s,WordOpaqueObject.Level.PARAGRAPH,"XML personalizado"); else inlines(e,s); }
                case "sdt" -> sdt(e,s);
                default -> opaqueInline(e,s,WordOpaqueObject.Level.PARAGRAPH,e.getNodeName());
            }
        }
    }
    private static Instant date(String value) {
        if (value == null || value.isBlank()) return Instant.EPOCH;
        try { return Instant.parse(value.endsWith("Z") || value.contains("+") ? value : value + "Z"); } catch (DateTimeParseException e) { return Instant.EPOCH; }
    }
    private void opaqueInline(Element e, InlineState s, WordOpaqueObject.Level level, String label) {
        s.context.note("Conteúdo preservado sem editor: " + label);
        String preview = OoxmlXml.text(e);
        float height = 14, width = Math.max(24,Math.min(260,preview.length()*5f+40));
        s.out.add(new WordObjectRun(new WordOpaqueObject(WordIds.next(),label,OoxmlXml.serialize(e),level,width,height,preview),s.decorate(s.base)));
    }
    private void math(Element e, InlineState s) {
        Element target = e;
        boolean display = e.getLocalName().equals("oMathPara");
        if (display) { List<Element> maths = OoxmlXml.children(e,"oMath"); target = maths.size() == 1 ? maths.getFirst() : null; }
        WordMath math = target == null ? null : DocxMathXml.read(target);
        if (math == null) { opaqueInline(e,s,WordOpaqueObject.Level.PARAGRAPH,"equação"); return; }
        WordEquation equation = new WordEquation(WordIds.next(),math,display,11);
        state.originals.put(equation.id(),new WordImportResult.OriginalObject(equation,OoxmlXml.serialize(e),s.context.part));
        s.out.add(new WordObjectRun(equation,s.decorate(s.base)));
    }
    private void sdt(Element e, InlineState s) throws IOException {
        Element pr = OoxmlXml.child(e,"sdtPr"), content = OoxmlXml.child(e,"sdtContent");
        if (pr == null || content == null) { opaqueInline(e,s,WordOpaqueObject.Level.PARAGRAPH,"controle de conteúdo"); return; }
        for (Element c : OoxmlXml.children(content)) if (!c.getLocalName().equals("r") || OoxmlXml.descendant(c,"drawing") != null) { opaqueInline(e,s,WordOpaqueObject.Level.PARAGRAPH,"controle de conteúdo"); return; }
        String name = OoxmlXml.attr(OoxmlXml.child(pr,"alias"),"val"); if (name.isEmpty()) name = OoxmlXml.attr(OoxmlXml.child(pr,"tag"),"val");
        String text = OoxmlXml.text(content); boolean placeholder = OoxmlXml.child(pr,"showingPlcHdr") != null;
        WordTextStyle style = s.decorate(runStyle(OoxmlXml.path(content,"r","rPr"),s.base));
        WordFormField field;
        try {
            if (OoxmlXml.child(pr,"checkbox") != null) {
                Element checked = OoxmlXml.path(pr,"checkbox","checked");
                field = WordFormField.checkbox(name,checked != null && DocxStylesXml.on(OoxmlXml.attr(checked,DocxNames.W14,"val")));
            } else if (OoxmlXml.child(pr,"dropDownList") != null || OoxmlXml.child(pr,"comboBox") != null) {
                Element list = OoxmlXml.child(pr,"dropDownList") != null ? OoxmlXml.child(pr,"dropDownList") : OoxmlXml.child(pr,"comboBox");
                List<String> options = new ArrayList<>();
                for (Element item : OoxmlXml.children(list,"listItem")) { String d = OoxmlXml.attr(item,"displayText"); options.add(d.isEmpty() ? OoxmlXml.attr(item,"value") : d); }
                field = WordFormField.dropdown(name,options);
                if (!placeholder && options.contains(text)) field = field.withValue(text);
            } else if (OoxmlXml.child(pr,"date") != null) {
                field = WordFormField.date(name); if (!placeholder) field = field.withValue(text);
            } else if (OoxmlXml.child(pr,"text") != null || OoxmlXml.children(content,"r").size() <= 1) {
                field = WordFormField.text(name); if (!placeholder) field = field.withValue(text);
            } else { opaqueInline(e,s,WordOpaqueObject.Level.PARAGRAPH,"controle de conteúdo"); return; }
        } catch (IllegalArgumentException ex) { opaqueInline(e,s,WordOpaqueObject.Level.PARAGRAPH,"controle de conteúdo"); return; }
        s.out.add(new WordObjectRun(field,style));
    }
    private WordField field(String instruction, String cached) {
        String instr = instruction.trim(); String[] parts = instr.split("\\s+");
        if (parts.length == 0) return null;
        String kind = parts[0].toUpperCase(Locale.ROOT);
        try {
            return switch (kind) {
                case "PAGE" -> new WordField(WordIds.next(),WordField.Kind.PAGE,"",cached);
                case "NUMPAGES", "SECTIONPAGES" -> new WordField(WordIds.next(),WordField.Kind.NUM_PAGES,"",cached);
                case "DATE", "TIME", "CREATEDATE" -> { Matcher m = Pattern.compile("\\\\@\\s+\"([^\"]*)\"").matcher(instr); yield new WordField(WordIds.next(),WordField.Kind.DATE,m.find() ? m.group(1) : "",cached); }
                case "REF" -> parts.length > 1 ? new WordField(WordIds.next(),WordField.Kind.REF,parts[1],cached) : null;
                case "PAGEREF" -> parts.length > 1 ? new WordField(WordIds.next(),WordField.Kind.PAGE_REF,parts[1],cached) : null;
                case "SEQ" -> parts.length > 1 ? new WordField(WordIds.next(),WordField.Kind.SEQ,parts[1],cached) : null;
                default -> null;
            };
        } catch (IllegalArgumentException e) { return null; }
    }
    private void run(Element r, InlineState s) throws IOException {
        WordTextStyle style = s.decorate(runStyle(OoxmlXml.child(r,"rPr"),s.base));
        StringBuilder text = new StringBuilder();
        for (Element e : OoxmlXml.children(r)) {
            String name = e.getLocalName();
            if (name.equals("rPr")) continue;
            if (name.equals("fldChar")) { flush(text,style,s); fieldChar(OoxmlXml.attr(e,"fldCharType"),r,s); continue; }
            if (name.equals("instrText")) { if (s.fieldDepth > 0 && !s.fieldResult && s.instruction != null) s.instruction.append(e.getTextContent()); continue; }
            if (s.fieldDepth > 0 && !s.fieldResult) continue;
            switch (name) {
                case "t", "delText" -> text.append(e.getTextContent().replace('\n',' ').replace('\r',' ').replace(WordObjectRun.TEXT,""));
                case "tab", "ptab" -> text.append('\t');
                case "noBreakHyphen" -> text.append('‑');
                case "softHyphen" -> text.append('­');
                case "sym" -> { try { text.appendCodePoint(Integer.parseInt(OoxmlXml.attr(e,"char"),16)); } catch (NumberFormatException ignored) {} }
                case "br", "cr" -> {
                    flush(text,style,s);
                    String type = OoxmlXml.attr(e,"type");
                    s.out.add(new WordObjectRun(WordBreak.of(type.equals("page") ? WordBreak.Kind.PAGE : type.equals("column") ? WordBreak.Kind.COLUMN : WordBreak.Kind.LINE),style));
                }
                case "drawing" -> { flush(text,style,s); drawing(e,e,s,style); }
                case "AlternateContent" -> {
                    flush(text,style,s);
                    Element drawing = null;
                    for (Element choice : OoxmlXml.children(e,"Choice")) { drawing = OoxmlXml.child(choice,"drawing"); if (drawing != null) break; }
                    if (drawing != null) drawing(drawing,e,s,style); else opaqueInline(e,s,WordOpaqueObject.Level.RUN,"conteúdo alternativo");
                }
                case "footnoteReference", "endnoteReference" -> {
                    flush(text,style,s);
                    String prefix = name.startsWith("foot") ? "f" : "e";
                    s.out.add(new WordObjectRun(new WordNoteReference(WordIds.next(),prefix + OoxmlXml.attr(e,"id")),style.withVerticalAlign(WordTextStyle.VerticalAlign.BASELINE)));
                }
                case "commentReference", "lastRenderedPageBreak", "footnoteRef", "endnoteRef", "annotationRef", "separator", "continuationSeparator" -> { }
                default -> { flush(text,style,s); opaqueInline(e,s,WordOpaqueObject.Level.RUN,e.getNodeName()); }
            }
        }
        flush(text,style,s);
        if (s.captured != null) s.captured.append(OoxmlXml.serialize(r));
    }
    private void drawing(Element drawing, Element original, InlineState s, WordTextStyle style) throws IOException {
        WordInlineObject object = DocxDrawingXml.read(drawing,s.context);
        if (object == null) { opaqueInline(original,s,WordOpaqueObject.Level.RUN,OoxmlXml.descendant(drawing,"graphicData") == null ? "desenho" : graphicLabel(drawing)); return; }
        state.originals.put(object.id(),new WordImportResult.OriginalObject(object,OoxmlXml.serialize(original),s.context.part));
        s.out.add(new WordObjectRun(object,style));
    }
    private static String graphicLabel(Element drawing) {
        String uri = OoxmlXml.descendant(drawing,"graphicData").getAttribute("uri");
        if (uri.contains("diagram")) return "SmartArt";
        if (uri.contains("chart")) return "gráfico";
        if (uri.contains("wordprocessingCanvas")) return "tela de desenho";
        if (uri.contains("Group")) return "grupo de formas";
        if (uri.contains("Shape")) return "forma";
        return "desenho";
    }
    private void flush(StringBuilder text, WordTextStyle style, InlineState s) {
        if (text.isEmpty()) return;
        s.out.add(new WordRun(text.toString(),style)); text.setLength(0);
    }
    private void fieldChar(String type, Element run, InlineState s) {
        switch (type) {
            case "begin" -> {
                s.fieldDepth++;
                if (s.fieldDepth == 1) { s.fieldResult = false; s.instruction = new StringBuilder(); s.fieldStart = s.out.size(); s.captured = new StringBuilder(); s.complex = false; }
                else s.complex = true;
            }
            case "separate" -> { if (s.fieldDepth == 1) s.fieldResult = true; }
            case "end" -> {
                if (s.fieldDepth == 0) return;
                s.fieldDepth--;
                if (s.fieldDepth > 0) return;
                String instr = s.instruction == null ? "" : s.instruction.toString();
                String captured = s.captured == null ? "" : s.captured + OoxmlXml.serialize(run);
                s.captured = null;
                List<WordInline> result = new ArrayList<>(s.out.subList(s.fieldStart,s.out.size()));
                StringBuilder cached = new StringBuilder(); for (WordInline i : result) cached.append(i instanceof WordRun r ? r.text() : "");
                String kind = instr.trim().isEmpty() ? "" : instr.trim().split("\\s+")[0].toUpperCase(Locale.ROOT);
                WordField field = s.complex ? null : field(instr,cached.toString());
                if (field != null) {
                    WordTextStyle style = result.isEmpty() ? s.decorate(s.base) : result.getFirst().style();
                    s.out.subList(s.fieldStart,s.out.size()).clear();
                    s.out.add(new WordObjectRun(field,style));
                } else if (kind.equals("HYPERLINK")) {
                    Matcher m = Pattern.compile("HYPERLINK\\s+(?:\\\\l\\s+)?\"([^\"]+)\"").matcher(instr.trim());
                    if (m.find()) { String target = instr.contains("\\l") ? "#" + m.group(1) : m.group(1); for (int i = s.fieldStart; i < s.out.size(); i++) s.out.set(i,s.out.get(i).withStyle(s.out.get(i).style().withLink(target))); }
                } else if (!kind.equals("TOC") && !kind.isEmpty()) {
                    s.out.subList(s.fieldStart,s.out.size()).clear();
                    s.context.note("Campo preservado sem editor: " + kind);
                    s.out.add(new WordObjectRun(new WordOpaqueObject(WordIds.next(),"campo " + kind,captured,WordOpaqueObject.Level.PARAGRAPH,Math.max(24,Math.min(260,cached.length()*5f+30)),14,cached.toString()),s.decorate(s.base)));
                }
                s.instruction = null; s.fieldResult = false;
            }
            default -> { }
        }
    }
    WordTextStyle runStyle(Element rPr, WordTextStyle base) throws IOException {
        WordTextStyle style = base;
        if (rPr == null) return style;
        Element rStyle = OoxmlXml.child(rPr,"rStyle");
        if (rStyle != null) {
            String id = OoxmlXml.attr(rStyle,"val");
            for (WordNamedStyle named : state.styles.chain(id)) style = named.properties().apply(style);
            if (id.equalsIgnoreCase("Hyperlink")) style = style.withColor(0x0563C1).withUnderline(true);
        }
        List<String> extras = new ArrayList<>();
        for (Element e : OoxmlXml.children(rPr)) {
            if (!DocxNames.isWord(e.getNamespaceURI()) || !RPR_KNOWN.contains(e.getLocalName())) { extras.add(OoxmlXml.serialize(e)); continue; }
            String v = OoxmlXml.attr(e,"val");
            switch (e.getLocalName()) {
                case "b" -> style = style.withBold(DocxStylesXml.on(v));
                case "i" -> style = style.withItalic(DocxStylesXml.on(v));
                case "u" -> style = style.withUnderline(!v.equals("none"));
                case "strike" -> style = style.withStrike(DocxStylesXml.on(v));
                case "sz" -> style = style.withSize(Math.max(1,Math.min(1638,DocxStylesXml.number(v)/2)));
                case "rFonts" -> {
                    String font = OoxmlXml.attr(e,"ascii"); if (font.isEmpty()) font = OoxmlXml.attr(e,"hAnsi");
                    if (!font.isEmpty()) style = style.withFamily(font);
                    else if (!OoxmlXml.attr(e,"asciiTheme").isEmpty()) { style = style.withFamily(OoxmlXml.attr(e,"asciiTheme").startsWith("major") ? "Calibri Light" : "Calibri"); extras.add(OoxmlXml.serialize(e)); }
                }
                case "color" -> { if (v.matches("[0-9a-fA-F]{6}")) style = style.withColor(Integer.parseInt(v,16)); }
                case "highlight" -> { Integer c = HIGHLIGHT.get(v); style = style.withHighlight(c); }
                case "shd" -> { String fill = OoxmlXml.attr(e,"fill"); if (fill.matches("[0-9a-fA-F]{6}")) style = style.withHighlight(Integer.parseInt(fill,16)); }
                case "vertAlign" -> style = style.withVerticalAlign(v.equals("superscript") ? WordTextStyle.VerticalAlign.SUPERSCRIPT : v.equals("subscript") ? WordTextStyle.VerticalAlign.SUBSCRIPT : WordTextStyle.VerticalAlign.BASELINE);
                default -> { }
            }
        }
        return extras.isEmpty() ? style : style.withExtras(extras);
    }

    private WordBlock table(Element tbl, DocxReadContext context) throws IOException {
        try {
            Element tblPr = OoxmlXml.child(tbl,"tblPr");
            List<String> extras = new ArrayList<>(); String styleId = null; WordTable.Alignment alignment = WordTable.Alignment.LEFT; WordBorder border = null; float padding = 5.4f;
            if (tblPr != null) for (Element e : OoxmlXml.children(tblPr)) {
                switch (e.getLocalName()) {
                    case "tblStyle" -> styleId = OoxmlXml.attr(e,"val");
                    case "jc" -> alignment = switch (OoxmlXml.attr(e,"val")) { case "center" -> WordTable.Alignment.CENTER; case "right", "end" -> WordTable.Alignment.RIGHT; default -> WordTable.Alignment.LEFT; };
                    case "tblBorders" -> border = border(e);
                    case "tblCellMar" -> { Element l = OoxmlXml.child(e,"left"); if (l == null) l = OoxmlXml.child(e,"start"); if (l != null) padding = Math.max(0,Math.min(72,DocxStylesXml.number(OoxmlXml.attr(l,"w"))/20)); }
                    case "tblW", "tblLayout", "tblLook" -> { }
                    default -> extras.add(OoxmlXml.serialize(e));
                }
            }
            if (border == null) border = styleId != null ? WordBorder.DEFAULT : WordBorder.NONE;
            List<Float> widths = new ArrayList<>();
            Element grid = OoxmlXml.child(tbl,"tblGrid");
            if (grid != null) for (Element col : OoxmlXml.children(grid,"gridCol")) widths.add(Math.max(4,Math.min(14400,DocxStylesXml.number(OoxmlXml.attr(col,"w").isEmpty() ? "0" : OoxmlXml.attr(col,"w"))/20)));
            List<WordTableRow> rows = new ArrayList<>();
            for (Element tr : OoxmlXml.children(tbl)) {
                if (tr.getLocalName().equals("tblPr") || tr.getLocalName().equals("tblGrid") || tr.getLocalName().equals("bookmarkStart") || tr.getLocalName().equals("bookmarkEnd")) continue;
                if (!tr.getLocalName().equals("tr")) return opaqueBlock(tbl,context);
                boolean header = false, cantSplit = false; float height = 0; List<String> rowExtras = new ArrayList<>();
                Element trPr = OoxmlXml.child(tr,"trPr");
                if (trPr != null) for (Element e : OoxmlXml.children(trPr)) {
                    switch (e.getLocalName()) {
                        case "tblHeader" -> header = DocxStylesXml.on(OoxmlXml.attr(e,"val"));
                        case "cantSplit" -> cantSplit = DocxStylesXml.on(OoxmlXml.attr(e,"val"));
                        case "trHeight" -> height = Math.max(0,Math.min(14400,DocxStylesXml.number(OoxmlXml.attr(e,"val").isEmpty() ? "0" : OoxmlXml.attr(e,"val"))/20));
                        default -> rowExtras.add(OoxmlXml.serialize(e));
                    }
                }
                List<WordTableCell> cells = new ArrayList<>();
                for (Element tc : OoxmlXml.children(tr)) {
                    if (tc.getLocalName().equals("trPr") || tc.getLocalName().equals("tblPrEx") || tc.getLocalName().startsWith("bookmark")) continue;
                    if (!tc.getLocalName().equals("tc")) return opaqueBlock(tbl,context);
                    int span = 1; WordTableCell.Merge merge = WordTableCell.Merge.NONE; Integer fill = null; WordTableCell.VerticalAlign valign = WordTableCell.VerticalAlign.TOP; WordBorder cellBorder = null;
                    List<String> cellExtras = new ArrayList<>();
                    Element tcPr = OoxmlXml.child(tc,"tcPr");
                    if (tcPr != null) for (Element e : OoxmlXml.children(tcPr)) {
                        switch (e.getLocalName()) {
                            case "gridSpan" -> span = Math.max(1,Math.min(63,(int)DocxStylesXml.number(OoxmlXml.attr(e,"val"))));
                            case "vMerge" -> merge = "restart".equals(OoxmlXml.attr(e,"val")) ? WordTableCell.Merge.RESTART : WordTableCell.Merge.CONTINUE;
                            case "shd" -> { String f = OoxmlXml.attr(e,"fill"); if (f.matches("[0-9a-fA-F]{6}")) fill = Integer.parseInt(f,16); }
                            case "vAlign" -> valign = switch (OoxmlXml.attr(e,"val")) { case "center" -> WordTableCell.VerticalAlign.CENTER; case "bottom" -> WordTableCell.VerticalAlign.BOTTOM; default -> WordTableCell.VerticalAlign.TOP; };
                            case "tcBorders" -> cellBorder = border(e);
                            case "tcW" -> { }
                            default -> cellExtras.add(OoxmlXml.serialize(e));
                        }
                    }
                    List<WordBlock> content = blocks(tc,context);
                    cells.add(new WordTableCell(UUID.randomUUID(),content,span,merge,fill,valign,cellBorder,cellExtras));
                }
                if (cells.isEmpty()) cells.add(WordTableCell.of(""));
                rows.add(new WordTableRow(UUID.randomUUID(),cells,height,header,cantSplit,rowExtras));
            }
            if (rows.isEmpty()) return opaqueBlock(tbl,context);
            int columns = 0; for (WordTableRow r : rows) columns = Math.max(columns,r.gridColumns());
            float fallback = state.settings.contentWidth()/Math.max(1,columns);
            while (widths.size() < columns) widths.add(Math.max(12,fallback));
            for (int i = 0; i < widths.size(); i++) if (widths.get(i) < 4.01f) widths.set(i,Math.max(12,fallback));
            for (int r = 0; r < rows.size(); r++) {
                List<WordTableCell> cells = new ArrayList<>(rows.get(r).cells());
                for (int c = 0; c < cells.size(); c++) if (cells.get(c).verticalMerge() == WordTableCell.Merge.CONTINUE && (r == 0 || rows.get(r-1).cellAt(rows.get(r).columnOf(c)) < 0)) cells.set(c,cells.get(c).withVerticalMerge(WordTableCell.Merge.RESTART));
                rows.set(r,rows.get(r).withCells(cells));
            }
            return new WordTable(UUID.randomUUID(),rows,widths,border,alignment,padding,styleId,extras);
        } catch (IllegalArgumentException e) {
            return opaqueBlock(tbl,context);
        }
    }
    private static WordBorder border(Element borders) throws IOException {
        for (String side : List.of("insideH","insideV","top","left","start","bottom","right","end")) {
            Element e = OoxmlXml.child(borders,side);
            if (e == null) continue;
            String val = OoxmlXml.attr(e,"val");
            if (val.equals("nil") || val.equals("none")) continue;
            WordBorder.Style style = switch (val) { case "double" -> WordBorder.Style.DOUBLE; case "dashed", "dashSmallGap", "dotDash" -> WordBorder.Style.DASHED; case "dotted" -> WordBorder.Style.DOTTED; default -> WordBorder.Style.SINGLE; };
            float width = OoxmlXml.attr(e,"sz").isEmpty() ? 0.5f : Math.max(0.25f,Math.min(12,DocxStylesXml.number(OoxmlXml.attr(e,"sz"))/8));
            String color = OoxmlXml.attr(e,"color");
            return new WordBorder(style,width,color.matches("[0-9a-fA-F]{6}") ? Integer.parseInt(color,16) : 0);
        }
        return WordBorder.NONE;
    }

    private void readNotes(DocxReadContext context, String type, String element, WordNote.Kind kind, String prefix, Map<String,WordNote> notes) throws IOException {
        Optional<DocxRelationships.Relationship> r = context.relationships.byType(type);
        if (r.isEmpty() || r.get().external() || !source.contains(r.get().target())) return;
        DocxReadContext partContext = new DocxReadContext(source,r.get().target(),state);
        for (Element note : OoxmlXml.children(OoxmlXml.parse(source.part(r.get().target())).getDocumentElement(),element)) {
            String t = OoxmlXml.attr(note,"type");
            if (t.equals("separator") || t.equals("continuationSeparator") || t.equals("continuationNotice")) continue;
            List<WordParagraph> paragraphs = new ArrayList<>();
            for (WordBlock b : blocks(note,partContext)) if (b instanceof WordParagraph p) paragraphs.add(p); else state.diagnostics.add("Conteúdo não textual em nota simplificado");
            notes.put(prefix + OoxmlXml.attr(note,"id"),new WordNote(prefix + OoxmlXml.attr(note,"id"),kind,paragraphs));
        }
    }
    private List<WordComment> readComments(DocxReadContext context) throws IOException {
        Optional<DocxRelationships.Relationship> r = context.relationships.byType("comments");
        List<WordComment> result = new ArrayList<>();
        if (r.isEmpty() || r.get().external() || !source.contains(r.get().target())) return result;
        Map<String,String> byPara = new HashMap<>(); Map<String,String> parents = new HashMap<>(); Set<String> done = new HashSet<>();
        Optional<DocxRelationships.Relationship> extended = context.relationships.all().stream().filter(x -> x.type().equals(DocxNames.REL_COMMENTS_EXTENDED)).findFirst();
        if (extended.isPresent() && source.contains(extended.get().target())) {
            for (Element ex : OoxmlXml.children(OoxmlXml.parse(source.part(extended.get().target())).getDocumentElement(),"commentEx")) {
                String para = ex.getAttributeNS(DocxNames.W15,"paraId"), parent = ex.getAttributeNS(DocxNames.W15,"paraIdParent");
                if (!parent.isEmpty()) parents.put(para,parent);
                if ("1".equals(ex.getAttributeNS(DocxNames.W15,"done"))) done.add(para);
            }
        }
        List<Element> elements = OoxmlXml.children(OoxmlXml.parse(source.part(r.get().target())).getDocumentElement(),"comment");
        Map<String,String> lastPara = new HashMap<>();
        for (Element c : elements) {
            List<Element> paragraphs = OoxmlXml.children(c,"p");
            String para = paragraphs.isEmpty() ? "" : paragraphs.getLast().getAttributeNS(DocxNames.W14,"paraId");
            String id = OoxmlXml.attr(c,"id");
            if (!para.isEmpty()) { byPara.put(para,id); lastPara.put(id,para); }
        }
        for (Element c : elements) {
            String id = OoxmlXml.attr(c,"id");
            List<String> lines = new ArrayList<>(); for (Element p : OoxmlXml.children(c,"p")) lines.add(OoxmlXml.text(p));
            String para = lastPara.get(id);
            String parent = para == null ? null : byPara.get(parents.get(para));
            try {
                result.add(new WordComment(id,OoxmlXml.attr(c,"author").isBlank() ? "Autor" : OoxmlXml.attr(c,"author"),OoxmlXml.attr(c,"initials"),date(OoxmlXml.attr(c,"date")),
                        String.join("\n",lines),parent,para != null && done.contains(para)));
            } catch (IllegalArgumentException ignored) {}
        }
        return result;
    }
}
