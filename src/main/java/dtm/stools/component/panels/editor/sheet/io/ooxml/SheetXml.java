package dtm.stools.component.panels.editor.sheet.io.ooxml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class SheetXml {
    private SheetXml() {}

    public static Document parse(byte[] data) throws IOException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(new ErrorHandler() {
                @Override public void warning(SAXParseException e) { }
                @Override public void error(SAXParseException e) throws SAXParseException { throw e; }
                @Override public void fatalError(SAXParseException e) throws SAXParseException { throw e; }
            });
            return b.parse(new ByteArrayInputStream(data));
        } catch (Exception e) {
            throw new IOException("XML inválido ou inseguro.", e);
        }
    }

    public static List<Element> children(Element parent) {
        List<Element> out = new ArrayList<>();
        if (parent == null) return out;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element e) out.add(e);
        return out;
    }

    public static List<Element> children(Element parent, String localName) {
        List<Element> out = new ArrayList<>();
        for (Element e : children(parent)) if (localName.equals(e.getLocalName())) out.add(e);
        return out;
    }

    public static Element child(Element parent, String localName) {
        if (parent == null) return null;
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) if (n instanceof Element e && localName.equals(e.getLocalName())) return e;
        return null;
    }

    public static Element path(Element parent, String... names) {
        Element current = parent;
        for (String n : names) { current = child(current, n); if (current == null) return null; }
        return current;
    }

    public static Element descendant(Element parent, String localName) {
        if (parent == null) return null;
        NodeList list = parent.getElementsByTagNameNS("*", localName);
        return list.getLength() == 0 ? null : (Element) list.item(0);
    }

    public static List<Element> descendants(Element parent, String localName) {
        List<Element> out = new ArrayList<>();
        if (parent == null) return out;
        NodeList list = parent.getElementsByTagNameNS("*", localName);
        for (int i = 0; i < list.getLength(); i++) out.add((Element) list.item(i));
        return out;
    }

    public static String attr(Element e, String name) {
        if (e == null) return "";
        if (e.hasAttribute(name)) return e.getAttribute(name);
        var attrs = e.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node a = attrs.item(i);
            if (name.equals(a.getLocalName())) return a.getNodeValue();
        }
        return "";
    }

    public static String attr(Element e, String name, String fallback) { String v = attr(e, name); return v.isEmpty() ? fallback : v; }

    public static int intAttr(Element e, String name, int fallback) {
        String v = attr(e, name);
        if (v.isEmpty()) return fallback;
        try { return (int) Double.parseDouble(v); } catch (NumberFormatException x) { return fallback; }
    }

    public static double doubleAttr(Element e, String name, double fallback) {
        String v = attr(e, name);
        if (v.isEmpty()) return fallback;
        try { return Double.parseDouble(v); } catch (NumberFormatException x) { return fallback; }
    }

    public static boolean boolAttr(Element e, String name, boolean fallback) {
        String v = attr(e, name);
        if (v.isEmpty()) return fallback;
        return v.equals("1") || v.equalsIgnoreCase("true");
    }

    public static String text(Element e) { return e == null ? "" : e.getTextContent(); }

    public static String escape(String value) {
        StringBuilder b = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            i += Character.charCount(cp);
            switch (cp) {
                case '&' -> b.append("&amp;");
                case '<' -> b.append("&lt;");
                case '>' -> b.append("&gt;");
                case '"' -> b.append("&quot;");
                case '\'' -> b.append("&apos;");
                default -> {
                    if (cp == 9 || cp == 10 || cp == 13 || cp >= 32 && cp <= 0xD7FF || cp >= 0xE000 && cp <= 0xFFFD || cp >= 0x10000) b.appendCodePoint(cp);
                    else b.append(String.format("_x%04X_", cp));
                }
            }
        }
        return b.toString();
    }

    public static String unescapeOoxml(String s) {
        if (s.indexOf("_x") < 0) return s;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i + 6 < s.length() && s.charAt(i) == '_' && s.charAt(i + 1) == 'x' && s.charAt(i + 6) == '_') {
                String hex = s.substring(i + 2, i + 6);
                if (hex.matches("[0-9A-Fa-f]{4}")) { b.append((char) Integer.parseInt(hex, 16)); i += 6; continue; }
            }
            b.append(s.charAt(i));
        }
        return b.toString();
    }
}
