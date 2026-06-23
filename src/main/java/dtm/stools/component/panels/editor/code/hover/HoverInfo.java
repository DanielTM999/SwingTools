package dtm.stools.component.panels.editor.code.hover;

import java.util.Objects;

public final class HoverInfo {

    public enum ContentType {
        TEXT,
        HTML,
        MARKDOWN
    }

    private final String content;
    private final ContentType contentType;
    private final int startLine;
    private final int startCol;
    private final int endLine;
    private final int endCol;

    public HoverInfo(String content) {
        this(content, ContentType.TEXT, -1, -1, -1, -1);
    }

    public HoverInfo(String content, boolean html, int startLine, int startCol, int endLine, int endCol) {
        this(content, html ? ContentType.HTML : ContentType.TEXT, startLine, startCol, endLine, endCol);
    }

    public HoverInfo(String content, ContentType contentType, int startLine, int startCol, int endLine, int endCol) {
        this.content = content;
        this.contentType = contentType == null ? ContentType.TEXT : contentType;
        this.startLine = startLine;
        this.startCol = startCol;
        this.endLine = endLine;
        this.endCol = endCol;
    }

    public static HoverInfo html(String html) {
        return new HoverInfo(html, ContentType.HTML, -1, -1, -1, -1);
    }

    public static HoverInfo markdown(String markdown) {
        return new HoverInfo(markdown, ContentType.MARKDOWN, -1, -1, -1, -1);
    }

    public String content() {
        return content;
    }

    public boolean html() {
        return contentType == ContentType.HTML;
    }

    public boolean markdown() {
        return contentType == ContentType.MARKDOWN;
    }

    public ContentType contentType() {
        return contentType;
    }

    public int startLine() {
        return startLine;
    }

    public int startCol() {
        return startCol;
    }

    public int endLine() {
        return endLine;
    }

    public int endCol() {
        return endCol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HoverInfo hoverInfo)) return false;
        return startLine == hoverInfo.startLine
                && startCol == hoverInfo.startCol
                && endLine == hoverInfo.endLine
                && endCol == hoverInfo.endCol
                && Objects.equals(content, hoverInfo.content)
                && contentType == hoverInfo.contentType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(content, contentType, startLine, startCol, endLine, endCol);
    }

    @Override
    public String toString() {
        return "HoverInfo[content=" + content
                + ", contentType=" + contentType
                + ", startLine=" + startLine
                + ", startCol=" + startCol
                + ", endLine=" + endLine
                + ", endCol=" + endCol
                + "]";
    }
}
