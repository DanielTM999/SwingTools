package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordBorder(Style style, float width, int color) {
    public enum Style { NONE, SINGLE, DOUBLE, DASHED, DOTTED }
    public static final WordBorder NONE = new WordBorder(Style.NONE,0,0);
    public static final WordBorder DEFAULT = new WordBorder(Style.SINGLE,0.5f,0x7f7f7f);
    public WordBorder {
        Objects.requireNonNull(style);
        if (!Float.isFinite(width) || width < 0 || width > 12) throw new IllegalArgumentException("Invalid border width");
        color &= 0xffffff;
    }
    public boolean visible() { return style != Style.NONE && width > 0; }
}
