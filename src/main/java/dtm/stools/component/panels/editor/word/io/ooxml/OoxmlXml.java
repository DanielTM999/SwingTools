package dtm.stools.component.panels.editor.word.io.ooxml;

import java.io.*;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class OoxmlXml {
    private OoxmlXml() {}
    public static Document parse(byte[] data) throws IOException {
        try {
            DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance(); factory.setNamespaceAware(true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities",false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,""); factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
            factory.setXIncludeAware(false); factory.setExpandEntityReferences(false);
            var builder=factory.newDocumentBuilder();
            builder.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException e) throws SAXException { throw e; }
                public void error(SAXParseException e) throws SAXException { throw e; }
                public void fatalError(SAXParseException e) throws SAXException { throw e; }
            });
            return builder.parse(new ByteArrayInputStream(data));
        } catch(Exception e) { throw new IOException("Invalid or unsafe XML",e); }
    }
    public static List<Element> children(Element parent) {
        List<Element> result=new ArrayList<>();
        for(Node node=parent.getFirstChild();node!=null;node=node.getNextSibling()) if(node instanceof Element element) result.add(element);
        return result;
    }
    public static List<Element> children(Element parent,String localName) {
        List<Element> result=new ArrayList<>();
        for(Element e:children(parent)) if(localName.equals(e.getLocalName())) result.add(e);
        return result;
    }
    public static Element child(Element parent,String localName) {
        if(parent==null) return null;
        for(Node node=parent.getFirstChild();node!=null;node=node.getNextSibling()) if(node instanceof Element e&&localName.equals(e.getLocalName())) return e;
        return null;
    }
    public static Element path(Element parent,String... names) {
        Element current=parent;
        for(String name:names){current=child(current,name);if(current==null)return null;}
        return current;
    }
    public static Element descendant(Element parent,String localName) {
        if(parent==null) return null;
        NodeList list=parent.getElementsByTagNameNS("*",localName);
        return list.getLength()==0?null:(Element)list.item(0);
    }
    public static List<Element> descendants(Element parent,String localName) {
        List<Element> result=new ArrayList<>();
        if(parent==null) return result;
        NodeList list=parent.getElementsByTagNameNS("*",localName);
        for(int i=0;i<list.getLength();i++) result.add((Element)list.item(i));
        return result;
    }
    public static String attr(Element element,String name) {
        if(element==null) return "";
        String value=element.getAttributeNS(element.getNamespaceURI(),name);
        return value.isEmpty()?element.getAttribute(name):value;
    }
    public static String attr(Element element,String namespace,String name) {
        if(element==null) return "";
        String value=element.getAttributeNS(namespace,name);
        return value.isEmpty()?element.getAttribute(name):value;
    }
    public static String anyAttr(Element element,String localName) {
        if(element==null) return "";
        NamedNodeMap attributes=element.getAttributes();
        for(int i=0;i<attributes.getLength();i++){Node a=attributes.item(i);if(localName.equals(a.getLocalName())||localName.equals(a.getNodeName()))return a.getNodeValue();}
        return "";
    }
    public static String serialize(Node node) {
        DOMImplementationLS ls=(DOMImplementationLS)(node instanceof Document d?d:node.getOwnerDocument()).getImplementation().getFeature("LS","3.0");
        LSSerializer serializer=ls.createLSSerializer();
        serializer.getDomConfig().setParameter("xml-declaration",false);
        if(serializer.getDomConfig().canSetParameter("namespaces",true)) serializer.getDomConfig().setParameter("namespaces",true);
        StringWriter writer=new StringWriter();LSOutput output=ls.createLSOutput();output.setCharacterStream(writer);
        serializer.write(node,output);
        return writer.toString();
    }
    public static byte[] bytes(Document document) {
        return ("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"+serialize(document.getDocumentElement())).getBytes(StandardCharsets.UTF_8);
    }
    public static String text(Element element) {
        if(element==null) return "";
        StringBuilder b=new StringBuilder();
        for(Element t:descendants(element,"t")) b.append(t.getTextContent());
        return b.toString();
    }
    public static String escape(String value) {
        StringBuilder b=new StringBuilder(value.length()+16);
        for(int i=0;i<value.length();i++){char c=value.charAt(i);switch(c){case '&'->b.append("&amp;");case '<'->b.append("&lt;");case '>'->b.append("&gt;");case '"'->b.append("&quot;");case '\''->b.append("&apos;");default->b.append(c);}}
        return b.toString();
    }
    public static void validateText(String text) throws IOException {
        for(int i=0;i<text.length();) { int cp=text.codePointAt(i); i+=Character.charCount(cp);
            if(!(cp==9 || cp==10 || cp==13 || cp>=32&&cp<=0xD7FF || cp>=0xE000&&cp<=0xFFFD || cp>=0x10000&&cp<=0x10FFFF)) throw new IOException("Text contains a character not allowed in XML"); }
    }
}
