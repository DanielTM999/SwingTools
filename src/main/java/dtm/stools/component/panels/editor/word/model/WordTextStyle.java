package dtm.stools.component.panels.editor.word.model;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record WordTextStyle(String family, float size, boolean bold, boolean italic, boolean underline, boolean strike, int color, Integer highlight,
                            VerticalAlign verticalAlign, String link, WordRevision revision, List<String> comments, List<String> extras) {
    public enum VerticalAlign { BASELINE, SUPERSCRIPT, SUBSCRIPT }
    public static final WordTextStyle DEFAULT = new WordTextStyle("Arial", 11, false, false, false, false, 0x111111, null);
    public WordTextStyle {
        Objects.requireNonNull(family, "family");
        if (family.isBlank() || !Float.isFinite(size) || size < 1 || size > 1638) throw new IllegalArgumentException("Invalid font");
        color &= 0xffffff;
        if (highlight != null) highlight &= 0xffffff;
        if (verticalAlign == null) verticalAlign = VerticalAlign.BASELINE;
        if (link != null && link.isBlank()) link = null;
        comments = comments == null ? List.of() : comments.stream().distinct().sorted().toList();
        extras = extras == null ? List.of() : List.copyOf(extras);
    }
    public WordTextStyle(String family, float size, boolean bold, boolean italic, boolean underline, boolean strike, int color, Integer highlight) {
        this(family,size,bold,italic,underline,strike,color,highlight,VerticalAlign.BASELINE,null,null,List.of(),List.of());
    }
    public Font font() { return new Font(family, (bold ? Font.BOLD : 0) | (italic ? Font.ITALIC : 0), 1).deriveFont(size); }
    public Color foreground() { return new Color(color); }
    public WordTextStyle withBold(boolean value) { return new WordTextStyle(family,size,value,italic,underline,strike,color,highlight,verticalAlign,link,revision,comments,extras); }
    public WordTextStyle withItalic(boolean value) { return new WordTextStyle(family,size,bold,value,underline,strike,color,highlight,verticalAlign,link,revision,comments,extras); }
    public WordTextStyle withUnderline(boolean value) { return new WordTextStyle(family,size,bold,italic,value,strike,color,highlight,verticalAlign,link,revision,comments,extras); }
    public WordTextStyle withStrike(boolean value) { return new WordTextStyle(family,size,bold,italic,underline,value,color,highlight,verticalAlign,link,revision,comments,extras); }
    public WordTextStyle withFamily(String value) { return new WordTextStyle(value,size,bold,italic,underline,strike,color,highlight,verticalAlign,link,revision,comments,extras); }
    public WordTextStyle withSize(float value) { return new WordTextStyle(family,value,bold,italic,underline,strike,color,highlight,verticalAlign,link,revision,comments,extras); }
    public WordTextStyle withColor(int value) { return new WordTextStyle(family,size,bold,italic,underline,strike,value,highlight,verticalAlign,link,revision,comments,extras); }
    public WordTextStyle withHighlight(Integer value) { return new WordTextStyle(family,size,bold,italic,underline,strike,color,value,verticalAlign,link,revision,comments,extras); }
    public WordTextStyle withVerticalAlign(VerticalAlign value) { return new WordTextStyle(family,size,bold,italic,underline,strike,color,highlight,value,link,revision,comments,extras); }
    public WordTextStyle withLink(String value) { return new WordTextStyle(family,size,bold,italic,underline,strike,color,highlight,verticalAlign,value,revision,comments,extras); }
    public WordTextStyle withRevision(WordRevision value) { return new WordTextStyle(family,size,bold,italic,underline,strike,color,highlight,verticalAlign,link,value,comments,extras); }
    public WordTextStyle withComments(List<String> value) { return new WordTextStyle(family,size,bold,italic,underline,strike,color,highlight,verticalAlign,link,revision,value,extras); }
    public WordTextStyle withExtras(List<String> value) { return new WordTextStyle(family,size,bold,italic,underline,strike,color,highlight,verticalAlign,link,revision,comments,value); }
    public WordTextStyle withComment(String id) { List<String> next = new ArrayList<>(comments); next.add(id); return withComments(next); }
    public WordTextStyle withoutComment(String id) { return withComments(comments.stream().filter(c -> !c.equals(id)).toList()); }
}
