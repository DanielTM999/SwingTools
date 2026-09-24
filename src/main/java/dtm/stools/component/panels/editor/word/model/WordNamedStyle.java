package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordNamedStyle(String id, String name, Type type, String basedOn, String next, WordStyleProperties properties, String extraXml) {
    public enum Type { PARAGRAPH, CHARACTER }
    public WordNamedStyle {
        Objects.requireNonNull(id); Objects.requireNonNull(name); Objects.requireNonNull(type); Objects.requireNonNull(properties);
        if (id.isBlank() || id.length() > 253 || name.isBlank()) throw new IllegalArgumentException("Invalid style");
        if (basedOn != null && (basedOn.isBlank() || basedOn.equals(id))) basedOn = null;
        if (next != null && next.isBlank()) next = null;
        extraXml = extraXml == null ? "" : extraXml;
    }
    public WordNamedStyle(String id, String name, String basedOn, WordStyleProperties properties) { this(id,name,Type.PARAGRAPH,basedOn,null,properties,""); }
    public WordNamedStyle withProperties(WordStyleProperties value) { return new WordNamedStyle(id,name,type,basedOn,next,value,extraXml); }
    public WordNamedStyle withName(String value) { return new WordNamedStyle(id,value,type,basedOn,next,properties,extraXml); }
    public WordNamedStyle withBasedOn(String value) { return new WordNamedStyle(id,name,type,value,next,properties,extraXml); }
    public WordNamedStyle withNext(String value) { return new WordNamedStyle(id,name,type,basedOn,value,properties,extraXml); }
}
