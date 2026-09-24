package dtm.stools.component.panels.editor.word.model;

import java.util.List;
import java.util.Objects;

public record WordFormField(String id, Kind kind, String name, String value, List<String> options, boolean checked, String placeholder) implements WordInlineObject {
    public static final String TYPE = "form";
    public enum Kind { TEXT, CHECKBOX, DROPDOWN, DATE }
    public WordFormField {
        WordInlineObject.requireId(id); Objects.requireNonNull(kind);
        name = name == null ? "" : name;
        value = value == null ? "" : value;
        options = options == null ? List.of() : List.copyOf(options);
        placeholder = placeholder == null || placeholder.isBlank() ? "Clique para inserir" : placeholder;
        if (kind == Kind.DROPDOWN && !value.isEmpty() && !options.contains(value)) throw new IllegalArgumentException("Value is not an option");
    }
    public static WordFormField text(String name) { return new WordFormField(WordIds.next(),Kind.TEXT,name,"",List.of(),false,null); }
    public static WordFormField checkbox(String name, boolean checked) { return new WordFormField(WordIds.next(),Kind.CHECKBOX,name,"",List.of(),checked,null); }
    public static WordFormField dropdown(String name, List<String> options) { return new WordFormField(WordIds.next(),Kind.DROPDOWN,name,"",options,false,"Escolha um item"); }
    public static WordFormField date(String name) { return new WordFormField(WordIds.next(),Kind.DATE,name,"",List.of(),false,"Escolha uma data"); }
    @Override public String type() { return TYPE; }
    @Override public float width() { return 0; }
    @Override public float height() { return 0; }
    @Override public boolean resizable() { return false; }
    @Override public boolean textual() { return true; }
    @Override public WordFormField withId(String v) { return new WordFormField(v,kind,name,value,options,checked,placeholder); }
    public WordFormField withValue(String v) { return new WordFormField(id,kind,name,v,options,checked,placeholder); }
    public WordFormField withChecked(boolean v) { return new WordFormField(id,kind,name,value,options,v,placeholder); }
    public String display() {
        if (kind == Kind.CHECKBOX) return checked ? "☒" : "☐";
        return value.isEmpty() ? placeholder : value;
    }
    @Override public String plainText() { return kind == Kind.CHECKBOX ? display() : value; }
}
