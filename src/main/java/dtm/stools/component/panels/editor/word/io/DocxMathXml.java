package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.math.*;
import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import org.w3c.dom.Element;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class DocxMathXml {
    private static final Map<String,String> ACCENT_CHARS = Map.of("̂","hat","̅","bar","⃗","vec","̇","dot","̃","tilde");
    private DocxMathXml() {}

    static WordMath read(Element oMath) {
        try { return row(oMath); } catch (UnsupportedOperationException e) { return null; }
    }
    private static WordMath row(Element parent) {
        List<WordMath> items = new ArrayList<>();
        for (Element e : OoxmlXml.children(parent)) {
            String name = e.getLocalName();
            if (name.endsWith("Pr") || name.equals("ctrlPr") || name.equals("bookmarkStart") || name.equals("bookmarkEnd") || name.equals("proofErr")) continue;
            items.add(node(e));
        }
        if (items.isEmpty()) return new WordMathText("");
        return WordMathRow.of(items);
    }
    private static WordMath node(Element e) {
        if (!DocxNames.M.equals(e.getNamespaceURI())) {
            if ("r".equals(e.getLocalName())) return new WordMathText(OoxmlXml.text(e),true);
            throw new UnsupportedOperationException(e.getLocalName());
        }
        return switch (e.getLocalName()) {
            case "r" -> {
                StringBuilder b = new StringBuilder(); for (Element t : OoxmlXml.children(e,"t")) b.append(t.getTextContent());
                Element sty = OoxmlXml.path(e,"rPr","sty"); boolean plain = sty != null && "p".equals(OoxmlXml.attr(sty,DocxNames.M,"val")) || OoxmlXml.path(e,"rPr","nor") != null;
                yield new WordMathText(b.toString(),plain);
            }
            case "f" -> new WordMathFraction(part(e,"num"),part(e,"den"));
            case "sSup" -> new WordMathScript(part(e,"e"),null,part(e,"sup"));
            case "sSub" -> new WordMathScript(part(e,"e"),part(e,"sub"),null);
            case "sSubSup" -> new WordMathScript(part(e,"e"),part(e,"sub"),part(e,"sup"));
            case "rad" -> {
                Element hide = OoxmlXml.path(e,"radPr","degHide");
                boolean hidden = hide != null && !"0".equals(OoxmlXml.attr(hide,DocxNames.M,"val")) && !"off".equals(OoxmlXml.attr(hide,DocxNames.M,"val"));
                WordMath degree = hidden || OoxmlXml.child(e,"deg") == null || OoxmlXml.children(OoxmlXml.child(e,"deg")).stream().allMatch(c -> c.getLocalName().endsWith("Pr")) ? null : part(e,"deg");
                yield new WordMathRadical(degree,part(e,"e"));
            }
            case "d" -> {
                Element pr = OoxmlXml.child(e,"dPr");
                String open = value(pr,"begChr","("), close = value(pr,"endChr",")"), separator = value(pr,"sepChr","|");
                List<WordMath> items = new ArrayList<>();
                for (Element item : OoxmlXml.children(e,"e")) { if (!items.isEmpty()) items.add(new WordMathText(separator,true)); items.add(row(item)); }
                yield new WordMathDelimiter(open,close,items.isEmpty() ? new WordMathText("") : WordMathRow.of(items));
            }
            case "nary" -> {
                Element pr = OoxmlXml.child(e,"naryPr");
                boolean subHide = flag(pr,"subHide"), supHide = flag(pr,"supHide");
                yield new WordMathNary(value(pr,"chr","∫"),subHide ? null : part(e,"sub"),supHide ? null : part(e,"sup"),part(e,"e"));
            }
            case "func" -> {
                String name = OoxmlXml.text(OoxmlXml.child(e,"fName"));
                if (name.isBlank()) throw new UnsupportedOperationException("func");
                yield new WordMathFunction(name,part(e,"e"));
            }
            case "acc" -> {
                String chr = value(OoxmlXml.child(e,"accPr"),"chr","̂");
                String accent = ACCENT_CHARS.get(chr);
                if (accent == null) throw new UnsupportedOperationException("acc");
                yield new WordMathAccent(accent,part(e,"e"));
            }
            case "bar" -> new WordMathAccent("bar",part(e,"e"));
            default -> throw new UnsupportedOperationException(e.getLocalName());
        };
    }
    private static WordMath part(Element parent, String name) {
        Element e = OoxmlXml.child(parent,name);
        return e == null ? new WordMathText("") : row(e);
    }
    private static String value(Element pr, String name, String fallback) {
        Element e = OoxmlXml.child(pr,name);
        if (e == null) return fallback;
        String v = OoxmlXml.attr(e,DocxNames.M,"val");
        return e.hasAttributeNS(DocxNames.M,"val") || e.hasAttribute("m:val") ? v : fallback;
    }
    private static boolean flag(Element pr, String name) {
        Element e = OoxmlXml.child(pr,name);
        if (e == null) return false;
        String v = OoxmlXml.attr(e,DocxNames.M,"val");
        return v.isEmpty() || v.equals("1") || v.equals("on") || v.equals("true");
    }

    static String write(WordMath math) {
        StringBuilder b = new StringBuilder();
        append(b,math);
        return b.toString();
    }
    private static void append(StringBuilder b, WordMath math) {
        switch (math) {
            case WordMathRow row -> { for (WordMath item : row.items()) append(b,item); }
            case WordMathText text -> {
                if (text.text().isEmpty()) return;
                b.append("<m:r>");
                if (text.plain() && text.text().chars().anyMatch(Character::isLetter)) b.append("<m:rPr><m:sty m:val=\"p\"/></m:rPr>");
                b.append("<m:t xml:space=\"preserve\">").append(OoxmlXml.escape(text.text())).append("</m:t></m:r>");
            }
            case WordMathFraction f -> { b.append("<m:f><m:num>"); append(b,f.numerator()); b.append("</m:num><m:den>"); append(b,f.denominator()); b.append("</m:den></m:f>"); }
            case WordMathScript s -> {
                String tag = s.subscript() != null && s.superscript() != null ? "sSubSup" : s.superscript() != null ? "sSup" : "sSub";
                b.append("<m:").append(tag).append("><m:e>"); append(b,s.base()); b.append("</m:e>");
                if (s.subscript() != null) { b.append("<m:sub>"); append(b,s.subscript()); b.append("</m:sub>"); }
                if (s.superscript() != null) { b.append("<m:sup>"); append(b,s.superscript()); b.append("</m:sup>"); }
                b.append("</m:").append(tag).append('>');
            }
            case WordMathRadical r -> {
                b.append("<m:rad>");
                if (r.degree() == null) b.append("<m:radPr><m:degHide m:val=\"1\"/></m:radPr><m:deg/>");
                else { b.append("<m:deg>"); append(b,r.degree()); b.append("</m:deg>"); }
                b.append("<m:e>"); append(b,r.body()); b.append("</m:e></m:rad>");
            }
            case WordMathDelimiter d -> {
                b.append("<m:d><m:dPr><m:begChr m:val=\"").append(OoxmlXml.escape(d.open())).append("\"/><m:endChr m:val=\"").append(OoxmlXml.escape(d.close())).append("\"/></m:dPr><m:e>");
                append(b,d.content()); b.append("</m:e></m:d>");
            }
            case WordMathNary n -> {
                boolean integral = "∫∬∭∮".contains(n.operator());
                b.append("<m:nary><m:naryPr><m:chr m:val=\"").append(OoxmlXml.escape(n.operator())).append("\"/><m:limLoc m:val=\"").append(integral ? "subSup" : "undOvr").append("\"/>");
                if (n.lower() == null) b.append("<m:subHide m:val=\"1\"/>");
                if (n.upper() == null) b.append("<m:supHide m:val=\"1\"/>");
                b.append("</m:naryPr><m:sub>"); if (n.lower() != null) append(b,n.lower()); b.append("</m:sub><m:sup>"); if (n.upper() != null) append(b,n.upper());
                b.append("</m:sup><m:e>"); append(b,n.body()); b.append("</m:e></m:nary>");
            }
            case WordMathFunction f -> {
                b.append("<m:func><m:fName><m:r><m:rPr><m:sty m:val=\"p\"/></m:rPr><m:t>").append(OoxmlXml.escape(f.name())).append("</m:t></m:r></m:fName><m:e>");
                append(b,f.argument()); b.append("</m:e></m:func>");
            }
            case WordMathAccent a -> {
                if (a.accent().equals("bar")) { b.append("<m:bar><m:barPr><m:pos m:val=\"top\"/></m:barPr><m:e>"); append(b,a.base()); b.append("</m:e></m:bar>"); return; }
                String chr = ACCENT_CHARS.entrySet().stream().filter(en -> en.getValue().equals(a.accent())).map(Map.Entry::getKey).findFirst().orElse("̂");
                b.append("<m:acc><m:accPr><m:chr m:val=\"").append(chr).append("\"/></m:accPr><m:e>"); append(b,a.base()); b.append("</m:e></m:acc>");
            }
            default -> { }
        }
    }
}
