package dtm.stools.component.panels.editor.word.model;

public record WordStyleProperties(String family, Float size, Boolean bold, Boolean italic, Boolean underline, Integer color,
                                  WordParagraphStyle.Alignment alignment, Float before, Float after, Float lineSpacing,
                                  Float leftIndent, Float firstLineIndent, Integer headingLevel, Boolean keepWithNext) {
    public static final WordStyleProperties NONE = new WordStyleProperties(null,null,null,null,null,null,null,null,null,null,null,null,null,null);
    public WordStyleProperties {
        if (family != null && family.isBlank()) family = null;
        if (size != null && (!Float.isFinite(size) || size < 1 || size > 1638)) throw new IllegalArgumentException("Invalid style size");
        if (color != null) color &= 0xffffff;
        if (headingLevel != null && (headingLevel < 0 || headingLevel > 9)) throw new IllegalArgumentException("Invalid outline level");
    }
    public static WordStyleProperties text(String family, Float size, Boolean bold, Boolean italic, Integer color) {
        return new WordStyleProperties(family,size,bold,italic,null,color,null,null,null,null,null,null,null,null);
    }
    public WordStyleProperties withParagraph(WordParagraphStyle.Alignment a, Float b, Float af, Float line, Integer heading, Boolean keep) {
        return new WordStyleProperties(family,size,bold,italic,underline,color,a,b,af,line,leftIndent,firstLineIndent,heading,keep);
    }
    public WordStyleProperties withIndents(Float left, Float first) {
        return new WordStyleProperties(family,size,bold,italic,underline,color,alignment,before,after,lineSpacing,left,first,headingLevel,keepWithNext);
    }
    public WordStyleProperties withText(String f, Float s, Boolean b, Boolean i, Boolean u, Integer c) {
        return new WordStyleProperties(f,s,b,i,u,c,alignment,before,after,lineSpacing,leftIndent,firstLineIndent,headingLevel,keepWithNext);
    }
    public WordTextStyle apply(WordTextStyle style) {
        WordTextStyle s = style;
        if (family != null) s = s.withFamily(family);
        if (size != null) s = s.withSize(size);
        if (bold != null) s = s.withBold(bold);
        if (italic != null) s = s.withItalic(italic);
        if (underline != null) s = s.withUnderline(underline);
        if (color != null) s = s.withColor(color);
        return s;
    }
    public WordParagraphStyle apply(WordParagraphStyle style) {
        WordParagraphStyle s = style;
        if (alignment != null) s = s.withAlignment(alignment);
        if (before != null || after != null || lineSpacing != null)
            s = s.withSpacing(before == null ? s.before() : before,after == null ? s.after() : after,lineSpacing == null ? s.lineSpacing() : lineSpacing);
        if (leftIndent != null || firstLineIndent != null)
            s = s.withIndents(leftIndent == null ? s.leftIndent() : leftIndent,s.rightIndent(),firstLineIndent == null ? s.firstLineIndent() : firstLineIndent);
        if (headingLevel != null) s = s.withHeadingLevel(headingLevel);
        if (keepWithNext != null) s = s.withKeepWithNext(keepWithNext);
        return s;
    }
}
