package dtm.stools.component.panels.editor.sheet.io.ooxml;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

public final class XmlBuilder {
    private final StringBuilder b = new StringBuilder(4096);
    private final Deque<String> open = new ArrayDeque<>();
    private boolean tagOpen;

    public XmlBuilder() { b.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"); }

    public XmlBuilder open(String name, String... attributes) {
        closeTag();
        b.append('<').append(name);
        attrs(attributes);
        tagOpen = true;
        open.push(name);
        return this;
    }

    public XmlBuilder empty(String name, String... attributes) {
        closeTag();
        b.append('<').append(name);
        attrs(attributes);
        b.append("/>");
        return this;
    }

    public XmlBuilder element(String name, String text, String... attributes) {
        closeTag();
        b.append('<').append(name);
        attrs(attributes);
        b.append('>').append(SheetXml.escape(text)).append("</").append(name).append('>');
        return this;
    }

    public XmlBuilder text(String text) { closeTag(); b.append(SheetXml.escape(text)); return this; }
    public XmlBuilder raw(String xml) { closeTag(); b.append(xml); return this; }

    public XmlBuilder close() {
        String name = open.pop();
        if (tagOpen) { b.append("/>"); tagOpen = false; return this; }
        b.append("</").append(name).append('>');
        return this;
    }

    private void attrs(String[] attributes) {
        for (int i = 0; i + 1 < attributes.length; i += 2) {
            if (attributes[i + 1] == null) continue;
            b.append(' ').append(attributes[i]).append("=\"").append(SheetXml.escape(attributes[i + 1])).append('"');
        }
    }

    private void closeTag() { if (tagOpen) { b.append('>'); tagOpen = false; } }

    public byte[] bytes() {
        while (!open.isEmpty()) close();
        return b.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override public String toString() { return b.toString(); }
}
