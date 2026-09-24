package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.model.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class DocxStylesXml {
    static final Map<String,String> BUILT_IN = Map.ofEntries(
            Map.entry("Normal","Normal"),Map.entry("Title","Title"),Map.entry("Subtitle","Subtitle"),Map.entry("Heading1","heading 1"),Map.entry("Heading2","heading 2"),
            Map.entry("Heading3","heading 3"),Map.entry("Heading4","heading 4"),Map.entry("Quote","Quote"),Map.entry("ListParagraph","List Paragraph"),Map.entry("Caption","caption"),
            Map.entry("TOCHeading","TOC Heading"),Map.entry("TOC1","toc 1"),Map.entry("TOC2","toc 2"),Map.entry("TOC3","toc 3"),Map.entry("FootnoteText","footnote text"),
            Map.entry("Header","header"),Map.entry("Footer","footer"));
    private static final Map<String,String> DISPLAY = Map.ofEntries(
            Map.entry("normal","Normal"),Map.entry("title","Título"),Map.entry("subtitle","Subtítulo"),Map.entry("heading 1","Título 1"),Map.entry("heading 2","Título 2"),
            Map.entry("heading 3","Título 3"),Map.entry("heading 4","Título 4"),Map.entry("quote","Citação"),Map.entry("list paragraph","Parágrafo da lista"),Map.entry("caption","Legenda"),
            Map.entry("toc heading","Título do sumário"),Map.entry("toc 1","Sumário 1"),Map.entry("toc 2","Sumário 2"),Map.entry("toc 3","Sumário 3"),Map.entry("footnote text","Texto de nota de rodapé"),
            Map.entry("header","Cabeçalho"),Map.entry("footer","Rodapé"));
    private DocxStylesXml() {}

    static WordStyleSheet readStyles(Element root) throws IOException {
        WordTextStyle text = WordTextStyle.DEFAULT.withFamily("Calibri").withSize(10).withColor(0);
        WordParagraphStyle paragraph = WordParagraphStyle.DEFAULT.withSpacing(0,0,1);
        Element defaults = OoxmlXml.child(root,"docDefaults");
        if (defaults != null) {
            text = properties(OoxmlXml.path(defaults,"rPrDefault","rPr"),null).apply(text);
            paragraph = properties(null,OoxmlXml.path(defaults,"pPrDefault","pPr")).apply(paragraph);
        }
        List<WordNamedStyle> styles = new ArrayList<>();
        for (Element s : OoxmlXml.children(root,"style")) {
            String type = OoxmlXml.attr(s,"type");
            if (!type.equals("paragraph") && !type.equals("character")) continue;
            String id = OoxmlXml.attr(s,"styleId");
            if (id.isBlank()) continue;
            String name = OoxmlXml.attr(OoxmlXml.child(s,"name"),"val");
            String display = DISPLAY.getOrDefault(name.toLowerCase(Locale.ROOT),name.isBlank() ? id : name);
            WordStyleProperties props = properties(OoxmlXml.child(s,"rPr"),OoxmlXml.child(s,"pPr"));
            try {
                styles.add(new WordNamedStyle(id,display,type.equals("paragraph") ? WordNamedStyle.Type.PARAGRAPH : WordNamedStyle.Type.CHARACTER,
                        emptyToNull(OoxmlXml.attr(OoxmlXml.child(s,"basedOn"),"val")),emptyToNull(OoxmlXml.attr(OoxmlXml.child(s,"next"),"val")),props,""));
            } catch (IllegalArgumentException ignored) {}
        }
        WordStyleSheet sheet = new WordStyleSheet(styles,text,paragraph);
        for (WordNamedStyle s : styles) { try { sheet.chain(s.id()); } catch (IllegalArgumentException e) { throw new IOException("Circular style inheritance",e); } }
        return sheet;
    }
    private static String emptyToNull(String v) { return v == null || v.isBlank() ? null : v; }
    static WordStyleProperties properties(Element rPr, Element pPr) throws IOException {
        String family = null; Float size = null; Boolean bold = null, italic = null, underline = null, keep = null; Integer color = null, heading = null;
        WordParagraphStyle.Alignment alignment = null; Float before = null, after = null, line = null, left = null, first = null;
        if (rPr != null) for (Element e : OoxmlXml.children(rPr)) {
            String v = OoxmlXml.attr(e,"val");
            switch (e.getLocalName()) {
                case "rFonts" -> { String f = OoxmlXml.attr(e,"ascii"); if (f.isEmpty()) f = OoxmlXml.attr(e,"hAnsi"); if (!f.isEmpty()) family = f; else if (!OoxmlXml.attr(e,"asciiTheme").isEmpty()) family = OoxmlXml.attr(e,"asciiTheme").startsWith("major") ? "Calibri Light" : "Calibri"; }
                case "sz" -> size = number(v)/2;
                case "b" -> bold = on(v);
                case "i" -> italic = on(v);
                case "u" -> underline = !v.equals("none");
                case "color" -> { if (v.matches("[0-9a-fA-F]{6}")) color = Integer.parseInt(v,16); }
                default -> { }
            }
        }
        if (pPr != null) for (Element e : OoxmlXml.children(pPr)) {
            switch (e.getLocalName()) {
                case "jc" -> alignment = alignment(OoxmlXml.attr(e,"val"));
                case "spacing" -> {
                    if (!OoxmlXml.attr(e,"before").isEmpty()) before = number(OoxmlXml.attr(e,"before"))/20;
                    if (!OoxmlXml.attr(e,"after").isEmpty()) after = number(OoxmlXml.attr(e,"after"))/20;
                    if (!OoxmlXml.attr(e,"line").isEmpty() && Set.of("","auto").contains(OoxmlXml.attr(e,"lineRule"))) line = Math.max(0.5f,Math.min(10,number(OoxmlXml.attr(e,"line"))/240));
                }
                case "ind" -> {
                    String l = OoxmlXml.attr(e,"left"); if (l.isEmpty()) l = OoxmlXml.attr(e,"start");
                    if (!l.isEmpty()) left = number(l)/20;
                    if (!OoxmlXml.attr(e,"firstLine").isEmpty()) first = number(OoxmlXml.attr(e,"firstLine"))/20;
                    if (!OoxmlXml.attr(e,"hanging").isEmpty()) first = -number(OoxmlXml.attr(e,"hanging"))/20;
                }
                case "outlineLvl" -> { int lvl = (int)number(OoxmlXml.attr(e,"val")); heading = lvl >= 0 && lvl < 9 ? lvl+1 : 0; }
                case "keepNext" -> keep = on(OoxmlXml.attr(e,"val"));
                default -> { }
            }
        }
        try { return new WordStyleProperties(family,size,bold,italic,underline,color,alignment,before,after,line,left,first,heading,keep); }
        catch (IllegalArgumentException e) { throw new IOException("Invalid style properties",e); }
    }
    static WordParagraphStyle.Alignment alignment(String v) {
        return switch (v) { case "center" -> WordParagraphStyle.Alignment.CENTER; case "right", "end" -> WordParagraphStyle.Alignment.RIGHT; case "both", "distribute" -> WordParagraphStyle.Alignment.JUSTIFY; default -> WordParagraphStyle.Alignment.LEFT; };
    }
    static boolean on(String value) { return !Set.of("0","false","off").contains(value); }
    static float number(String value) throws IOException {
        try { float n = Float.parseFloat(value); if (!Float.isFinite(n)) throw new NumberFormatException(); return n; }
        catch (NumberFormatException e) { throw new IOException("Invalid numeric DOCX property",e); }
    }

    static String styleXml(WordNamedStyle style, WordStyleSheet sheet) {
        StringBuilder b = new StringBuilder("<w:style w:type=\"").append(style.type() == WordNamedStyle.Type.PARAGRAPH ? "paragraph" : "character").append('"');
        if (style.id().equals(WordStyleSheet.NORMAL)) b.append(" w:default=\"1\"");
        b.append(" w:styleId=\"").append(OoxmlXml.escape(style.id())).append("\"><w:name w:val=\"").append(OoxmlXml.escape(BUILT_IN.getOrDefault(style.id(),style.name()))).append("\"/>");
        if (style.basedOn() != null) b.append("<w:basedOn w:val=\"").append(OoxmlXml.escape(style.basedOn())).append("\"/>");
        String next = style.next() != null ? style.next() : style.type() == WordNamedStyle.Type.PARAGRAPH && !style.id().equals(WordStyleSheet.NORMAL) && (style.id().startsWith("Heading") || style.id().equals("Title") || style.id().equals("Subtitle")) ? WordStyleSheet.NORMAL : null;
        if (next != null) b.append("<w:next w:val=\"").append(OoxmlXml.escape(next)).append("\"/>");
        b.append("<w:qFormat/>");
        b.append(pPr(style.properties())).append(rPr(style.properties()));
        return b.append("</w:style>").toString();
    }
    static String pPr(WordStyleProperties p) {
        StringBuilder b = new StringBuilder();
        if (Boolean.TRUE.equals(p.keepWithNext())) b.append("<w:keepNext/>");
        if (p.before() != null || p.after() != null || p.lineSpacing() != null) {
            b.append("<w:spacing");
            if (p.before() != null) b.append(" w:before=\"").append(twips(p.before())).append('"');
            if (p.after() != null) b.append(" w:after=\"").append(twips(p.after())).append('"');
            if (p.lineSpacing() != null) b.append(" w:line=\"").append(Math.round(p.lineSpacing()*240)).append("\" w:lineRule=\"auto\"");
            b.append("/>");
        }
        if (p.leftIndent() != null || p.firstLineIndent() != null) {
            b.append("<w:ind");
            if (p.leftIndent() != null) b.append(" w:left=\"").append(twips(p.leftIndent())).append('"');
            if (p.firstLineIndent() != null) b.append(p.firstLineIndent() < 0 ? " w:hanging=\"" : " w:firstLine=\"").append(twips(Math.abs(p.firstLineIndent()))).append('"');
            b.append("/>");
        }
        if (p.alignment() != null) b.append("<w:jc w:val=\"").append(jc(p.alignment())).append("\"/>");
        if (p.headingLevel() != null && p.headingLevel() > 0) b.append("<w:outlineLvl w:val=\"").append(p.headingLevel()-1).append("\"/>");
        return b.isEmpty() ? "" : "<w:pPr>" + b + "</w:pPr>";
    }
    static String rPr(WordStyleProperties p) {
        StringBuilder b = new StringBuilder();
        if (p.family() != null) b.append("<w:rFonts w:ascii=\"").append(OoxmlXml.escape(p.family())).append("\" w:hAnsi=\"").append(OoxmlXml.escape(p.family())).append("\" w:cs=\"").append(OoxmlXml.escape(p.family())).append("\"/>");
        if (p.bold() != null) b.append(p.bold() ? "<w:b/>" : "<w:b w:val=\"0\"/>");
        if (p.italic() != null) b.append(p.italic() ? "<w:i/>" : "<w:i w:val=\"0\"/>");
        if (p.color() != null) b.append("<w:color w:val=\"").append(String.format(Locale.ROOT,"%06X",p.color())).append("\"/>");
        if (p.size() != null) b.append("<w:sz w:val=\"").append(Math.round(p.size()*2)).append("\"/><w:szCs w:val=\"").append(Math.round(p.size()*2)).append("\"/>");
        if (p.underline() != null) b.append("<w:u w:val=\"").append(p.underline() ? "single" : "none").append("\"/>");
        return b.isEmpty() ? "" : "<w:rPr>" + b + "</w:rPr>";
    }
    static String jc(WordParagraphStyle.Alignment a) { return switch (a) { case LEFT -> "left"; case CENTER -> "center"; case RIGHT -> "right"; case JUSTIFY -> "both"; }; }
    static String twips(float n) { return Integer.toString(Math.round(n*20)); }

    static byte[] writeStyles(WordStyleSheet sheet, byte[] original, WordStyleSheet originalSheet, String ns) throws IOException {
        WordStyleProperties text = new WordStyleProperties(sheet.defaultText().family(),sheet.defaultText().size(),null,null,null,null,null,null,null,null,null,null,null,null);
        WordParagraphStyle dp = sheet.defaultParagraph();
        WordStyleProperties paragraph = new WordStyleProperties(null,null,null,null,null,null,null,dp.before(),dp.after(),dp.lineSpacing(),null,null,null,null);
        if (original != null) {
            Document document = OoxmlXml.parse(original); Element root = document.getDocumentElement();
            Map<String,Element> existing = new HashMap<>();
            for (Element s : OoxmlXml.children(root,"style")) existing.put(OoxmlXml.attr(s,"styleId"),s);
            for (WordNamedStyle style : sheet.styles()) {
                Optional<WordNamedStyle> before = originalSheet == null ? Optional.empty() : originalSheet.get(style.id());
                if (before.isPresent() && before.get().equals(style) && existing.containsKey(style.id())) continue;
                Element fresh = (Element)document.importNode(fragment(styleXml(style,sheet),ns),true);
                Element old = existing.get(style.id());
                if (old == null) root.appendChild(fresh);
                else {
                    for (Element c : OoxmlXml.children(old)) if (Set.of("name","basedOn","next","pPr","rPr").contains(c.getLocalName())) old.removeChild(c);
                    Node anchor = old.getFirstChild();
                    for (Element c : OoxmlXml.children(fresh)) {
                        if (c.getLocalName().equals("qFormat") && OoxmlXml.child(old,"qFormat") != null) continue;
                        Node imported = c.cloneNode(true);
                        if (Set.of("name","basedOn","next").contains(c.getLocalName())) old.insertBefore(imported,anchor);
                        else if (c.getLocalName().equals("pPr") || c.getLocalName().equals("rPr")) { Element tbl = firstOf(old,"tblPr","trPr","tcPr","tblStylePr"); old.insertBefore(imported,tbl); }
                    }
                }
            }
            return OoxmlXml.bytes(document);
        }
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<w:styles xmlns:w=\"").append(ns).append("\">");
        b.append("<w:docDefaults><w:rPrDefault>").append(rPr(text).isEmpty() ? "<w:rPr/>" : rPr(text)).append("</w:rPrDefault><w:pPrDefault>").append(pPr(paragraph).isEmpty() ? "<w:pPr/>" : pPr(paragraph)).append("</w:pPrDefault></w:docDefaults>");
        for (WordNamedStyle style : sheet.styles()) b.append(styleXml(style,sheet));
        if (sheet.get("TableGrid").isEmpty()) b.append("<w:style w:type=\"table\" w:styleId=\"TableGrid\"><w:name w:val=\"Table Grid\"/><w:tblPr><w:tblBorders><w:top w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/><w:left w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/><w:bottom w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/><w:right w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/><w:insideH w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/><w:insideV w:val=\"single\" w:sz=\"4\" w:space=\"0\" w:color=\"auto\"/></w:tblBorders></w:tblPr></w:style>");
        if (sheet.get("Hyperlink").isEmpty()) b.append("<w:style w:type=\"character\" w:styleId=\"Hyperlink\"><w:name w:val=\"Hyperlink\"/><w:rPr><w:color w:val=\"0563C1\"/><w:u w:val=\"single\"/></w:rPr></w:style>");
        if (sheet.get("FootnoteReference").isEmpty()) b.append("<w:style w:type=\"character\" w:styleId=\"FootnoteReference\"><w:name w:val=\"footnote reference\"/><w:rPr><w:vertAlign w:val=\"superscript\"/></w:rPr></w:style>");
        if (sheet.get("EndnoteReference").isEmpty()) b.append("<w:style w:type=\"character\" w:styleId=\"EndnoteReference\"><w:name w:val=\"endnote reference\"/><w:rPr><w:vertAlign w:val=\"superscript\"/></w:rPr></w:style>");
        return b.append("</w:styles>").toString().getBytes(StandardCharsets.UTF_8);
    }
    private static Element firstOf(Element parent, String... names) {
        for (Element c : OoxmlXml.children(parent)) for (String n : names) if (n.equals(c.getLocalName())) return c;
        return null;
    }
    static Element fragment(String xml, String ns) throws IOException {
        return OoxmlXml.parse(("<w:root xmlns:w=\"" + ns + "\" xmlns:w14=\"" + DocxNames.W14 + "\">" + xml + "</w:root>").getBytes(StandardCharsets.UTF_8)).getDocumentElement().getFirstChild() instanceof Element e ? e : null;
    }

    static WordNumbering readNumbering(Element root) throws IOException {
        Map<String,List<WordListLevel>> abstracts = new HashMap<>();
        for (Element a : OoxmlXml.children(root,"abstractNum")) {
            WordListLevel[] levels = new WordListLevel[9];
            for (Element lvl : OoxmlXml.children(a,"lvl")) {
                int index; try { index = Integer.parseInt(OoxmlXml.attr(lvl,"ilvl")); } catch (NumberFormatException e) { continue; }
                if (index < 0 || index > 8) continue;
                levels[index] = level(lvl,index);
            }
            List<WordListLevel> list = new ArrayList<>();
            for (int i = 0; i < 9; i++) list.add(levels[i] != null ? levels[i] : new WordListLevel(WordListLevel.Format.DECIMAL,"%" + (i+1) + ".",1,36+18*i,18));
            abstracts.put(OoxmlXml.attr(a,"abstractNumId"),list);
        }
        List<WordListDefinition> result = new ArrayList<>();
        for (Element n : OoxmlXml.children(root,"num")) {
            String id = OoxmlXml.attr(n,"numId"), abstractId = OoxmlXml.attr(OoxmlXml.child(n,"abstractNumId"),"val");
            List<WordListLevel> levels = abstracts.get(abstractId);
            if (id.isBlank() || levels == null) continue;
            List<WordListLevel> copy = new ArrayList<>(levels); boolean restart = false;
            for (Element o : OoxmlXml.children(n,"lvlOverride")) {
                int index; try { index = Integer.parseInt(OoxmlXml.attr(o,"ilvl")); } catch (NumberFormatException e) { continue; }
                if (index < 0 || index > 8) continue;
                Element start = OoxmlXml.child(o,"startOverride");
                if (start != null) { try { copy.set(index,copy.get(index).withStart(Integer.parseInt(OoxmlXml.attr(start,"val")))); restart = true; } catch (IllegalArgumentException ignored) {} }
                Element lvl = OoxmlXml.child(o,"lvl");
                if (lvl != null) copy.set(index,level(lvl,index));
            }
            result.add(new WordListDefinition(id,abstractId,copy,restart));
        }
        return WordNumbering.of(result);
    }
    private static WordListLevel level(Element lvl, int index) throws IOException {
        String format = OoxmlXml.attr(OoxmlXml.child(lvl,"numFmt"),"val");
        WordListLevel.Format f = switch (format) {
            case "bullet" -> WordListLevel.Format.BULLET; case "lowerLetter" -> WordListLevel.Format.LOWER_LETTER; case "upperLetter" -> WordListLevel.Format.UPPER_LETTER;
            case "lowerRoman" -> WordListLevel.Format.LOWER_ROMAN; case "upperRoman" -> WordListLevel.Format.UPPER_ROMAN; case "none" -> WordListLevel.Format.NONE; default -> WordListLevel.Format.DECIMAL;
        };
        String text = OoxmlXml.attr(OoxmlXml.child(lvl,"lvlText"),"val");
        if (f == WordListLevel.Format.BULLET) text = bullet(text);
        int start = 1; String s = OoxmlXml.attr(OoxmlXml.child(lvl,"start"),"val");
        try { if (!s.isEmpty()) start = Math.max(0,Math.min(32767,Integer.parseInt(s))); } catch (NumberFormatException ignored) {}
        float indent = 36+18*index, hanging = 18;
        Element ind = OoxmlXml.path(lvl,"pPr","ind");
        if (ind != null) {
            String l = OoxmlXml.attr(ind,"left"); if (l.isEmpty()) l = OoxmlXml.attr(ind,"start");
            if (!l.isEmpty()) indent = Math.max(0,number(l)/20);
            if (!OoxmlXml.attr(ind,"hanging").isEmpty()) hanging = Math.max(0,number(OoxmlXml.attr(ind,"hanging"))/20);
        }
        return new WordListLevel(f,text,start,indent,hanging);
    }
    private static String bullet(String text) {
        if (text.isEmpty()) return "•";
        char c = text.charAt(0);
        return switch (c) { case '', '·' -> "•"; case 'o' -> "◦"; case '', '' -> "▪"; case '' -> "➢"; case '' -> "✓"; default -> text; };
    }
    static byte[] writeNumbering(WordNumbering numbering, byte[] original, String ns) throws IOException {
        Document document; Element root; Set<String> existingNums = new HashSet<>(); int maxAbstract = 0;
        if (original != null) {
            document = OoxmlXml.parse(original); root = document.getDocumentElement();
            for (Element n : OoxmlXml.children(root,"num")) existingNums.add(OoxmlXml.attr(n,"numId"));
            for (Element a : OoxmlXml.children(root,"abstractNum")) { try { maxAbstract = Math.max(maxAbstract,Integer.parseInt(OoxmlXml.attr(a,"abstractNumId"))); } catch (NumberFormatException ignored) {} }
        } else {
            document = OoxmlXml.parse(("<w:numbering xmlns:w=\"" + ns + "\"/>").getBytes(StandardCharsets.UTF_8)); root = document.getDocumentElement();
        }
        Map<String,String> abstractIds = new HashMap<>();
        Element firstNum = OoxmlXml.child(root,"num");
        for (WordListDefinition d : numbering.lists()) {
            if (existingNums.contains(d.id())) continue;
            String abstractId = abstractIds.get(d.abstractId());
            if (abstractId == null) {
                boolean numeric = d.abstractId().matches("\\d+") && original != null && hasAbstract(root,d.abstractId());
                abstractId = numeric ? d.abstractId() : Integer.toString(++maxAbstract);
                abstractIds.put(d.abstractId(),abstractId);
                if (!numeric) {
                    StringBuilder b = new StringBuilder("<w:abstractNum w:abstractNumId=\"").append(abstractId).append("\"><w:multiLevelType w:val=\"hybridMultilevel\"/>");
                    for (int i = 0; i < d.levels().size(); i++) b.append(levelXml(d.levels().get(i),i));
                    b.append("</w:abstractNum>");
                    root.insertBefore(document.importNode(fragment(b.toString(),ns),true),firstNum);
                }
            }
            StringBuilder b = new StringBuilder("<w:num w:numId=\"").append(OoxmlXml.escape(d.id())).append("\"><w:abstractNumId w:val=\"").append(abstractId).append("\"/>");
            if (d.restart()) b.append("<w:lvlOverride w:ilvl=\"0\"><w:startOverride w:val=\"").append(d.levels().getFirst().start()).append("\"/></w:lvlOverride>");
            b.append("</w:num>");
            root.appendChild(document.importNode(fragment(b.toString(),ns),true));
        }
        return OoxmlXml.bytes(document);
    }
    private static boolean hasAbstract(Element root, String id) {
        for (Element a : OoxmlXml.children(root,"abstractNum")) if (id.equals(OoxmlXml.attr(a,"abstractNumId"))) return true;
        return false;
    }
    private static String levelXml(WordListLevel level, int index) {
        String format = switch (level.format()) {
            case BULLET -> "bullet"; case DECIMAL -> "decimal"; case LOWER_LETTER -> "lowerLetter"; case UPPER_LETTER -> "upperLetter";
            case LOWER_ROMAN -> "lowerRoman"; case UPPER_ROMAN -> "upperRoman"; case NONE -> "none";
        };
        return "<w:lvl w:ilvl=\"" + index + "\"><w:start w:val=\"" + level.start() + "\"/><w:numFmt w:val=\"" + format + "\"/><w:lvlText w:val=\"" + OoxmlXml.escape(level.text())
                + "\"/><w:lvlJc w:val=\"left\"/><w:pPr><w:ind w:left=\"" + twips(level.indent()) + "\" w:hanging=\"" + twips(level.hanging()) + "\"/></w:pPr>"
                + (level.format() == WordListLevel.Format.BULLET ? "<w:rPr><w:rFonts w:ascii=\"Arial\" w:hAnsi=\"Arial\"/></w:rPr>" : "") + "</w:lvl>";
    }

    static byte[] writeSettings(byte[] original, boolean evenAndOdd, String ns) throws IOException {
        if (original == null) {
            return ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<w:settings xmlns:w=\"" + ns + "\"><w:zoom w:percent=\"100\"/><w:defaultTabStop w:val=\"720\"/>"
                    + (evenAndOdd ? "<w:evenAndOddHeaders/>" : "") + "<w:characterSpacingControl w:val=\"doNotCompress\"/><w:compat><w:compatSetting w:name=\"compatibilityMode\" w:uri=\"http://schemas.microsoft.com/office/word\" w:val=\"15\"/></w:compat></w:settings>").getBytes(StandardCharsets.UTF_8);
        }
        Document document = OoxmlXml.parse(original); Element root = document.getDocumentElement();
        Element current = OoxmlXml.child(root,"evenAndOddHeaders");
        if (evenAndOdd == (current != null && on(OoxmlXml.attr(current,"val")))) return original;
        if (current != null) root.removeChild(current);
        if (evenAndOdd) {
            Element anchor = null;
            for (String name : List.of("defaultTableStyle","clickAndTypeStyle","summaryLength","showEnvelope","doNotHyphenateCaps","hyphenationZone","consecutiveHyphenLimit","autoHyphenation","defaultTabStop","zoom"))
                if ((anchor = OoxmlXml.child(root,name)) != null) break;
            Element element = document.createElementNS(root.getNamespaceURI(),root.getPrefix() == null ? "evenAndOddHeaders" : root.getPrefix() + ":evenAndOddHeaders");
            root.insertBefore(element,anchor == null ? root.getFirstChild() : anchor.getNextSibling());
        }
        return OoxmlXml.bytes(document);
    }
}
