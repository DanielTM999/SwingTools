package dtm.stools.component.panels.editor.word.io;

import dtm.stools.component.panels.editor.word.io.ooxml.OoxmlXml;
import dtm.stools.component.panels.editor.word.io.ooxml.OpcPackage;
import org.w3c.dom.Element;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class DocxRelationships {
    record Relationship(String id, String type, String target, boolean external) {
        String kind() { return DocxNames.relType(type); }
    }
    private final String part;
    private final Map<String,Relationship> relationships = new LinkedHashMap<>();
    private int counter;

    DocxRelationships(String part) { this.part = part; }

    static String relsName(String part) {
        int slash = part.lastIndexOf('/');
        return (slash < 0 ? "" : part.substring(0,slash+1)) + "_rels/" + part.substring(slash+1) + ".rels";
    }
    static DocxRelationships read(OpcPackage source, String part) throws IOException {
        DocxRelationships result = new DocxRelationships(part);
        String name = relsName(part);
        if (!source.contains(name)) return result;
        for (Element e : OoxmlXml.children(OoxmlXml.parse(source.part(name)).getDocumentElement())) {
            if (!"Relationship".equals(e.getLocalName())) continue;
            boolean external = "External".equals(e.getAttribute("TargetMode"));
            String target = e.getAttribute("Target");
            String resolved = external ? target : resolve(part,target);
            result.relationships.put(e.getAttribute("Id"),new Relationship(e.getAttribute("Id"),e.getAttribute("Type"),resolved,external));
        }
        return result;
    }
    static String resolve(String part, String target) throws IOException {
        if (target.startsWith("/")) { String t = target.substring(1); validate(t); return t; }
        Deque<String> segments = new ArrayDeque<>();
        int slash = part.lastIndexOf('/');
        if (slash > 0) for (String s : part.substring(0,slash).split("/")) segments.addLast(s);
        for (String s : target.split("/")) {
            if (s.isEmpty() || s.equals(".")) continue;
            if (s.equals("..")) { if (segments.isEmpty()) throw new IOException("Relationship escapes the package"); segments.removeLast(); }
            else segments.addLast(s);
        }
        String resolved = String.join("/",segments);
        validate(resolved);
        return resolved;
    }
    private static void validate(String name) throws IOException {
        try { OpcPackage.validateName(name); } catch (IllegalArgumentException e) { throw new IOException("Invalid relationship target",e); }
    }
    Optional<Relationship> get(String id) { return Optional.ofNullable(relationships.get(id)); }
    Collection<Relationship> all() { return relationships.values(); }
    Optional<Relationship> byType(String typeSuffix) { return relationships.values().stream().filter(r -> r.kind().equals(typeSuffix)).findFirst(); }
    String add(String type, String target, boolean external) {
        for (Relationship r : relationships.values()) if (r.type().equals(type) && r.target().equals(target) && r.external() == external) return r.id();
        String id;
        do { id = "rIdSt" + (++counter); } while (relationships.containsKey(id));
        relationships.put(id,new Relationship(id,type,target,external));
        return id;
    }
    DocxRelationships copy(String newPart) {
        DocxRelationships copy = new DocxRelationships(newPart);
        copy.relationships.putAll(relationships);
        return copy;
    }
    boolean isEmpty() { return relationships.isEmpty(); }
    byte[] bytes() {
        StringBuilder b = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n<Relationships xmlns=\"").append(DocxNames.PKG_RELS).append("\">");
        for (Relationship r : relationships.values()) {
            b.append("<Relationship Id=\"").append(OoxmlXml.escape(r.id())).append("\" Type=\"").append(OoxmlXml.escape(r.type())).append("\" Target=\"")
             .append(OoxmlXml.escape(r.external() ? r.target() : relative(part,r.target()))).append('"');
            if (r.external()) b.append(" TargetMode=\"External\"");
            b.append("/>");
        }
        return b.append("</Relationships>").toString().getBytes(StandardCharsets.UTF_8);
    }
    static String relative(String from, String target) {
        String[] base = from.contains("/") ? from.substring(0,from.lastIndexOf('/')).split("/") : new String[0];
        String[] to = target.split("/");
        int common = 0;
        while (common < base.length && common < to.length-1 && base[common].equals(to[common])) common++;
        StringBuilder b = new StringBuilder();
        for (int i = common; i < base.length; i++) b.append("../");
        for (int i = common; i < to.length; i++) { if (i > common) b.append('/'); b.append(to[i]); }
        return b.toString();
    }
}
