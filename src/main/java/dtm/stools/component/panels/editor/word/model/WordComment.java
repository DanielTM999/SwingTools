package dtm.stools.component.panels.editor.word.model;

import java.time.Instant;
import java.util.Objects;

public record WordComment(String id, String author, String initials, Instant date, String text, String parentId, boolean resolved) {
    public WordComment {
        Objects.requireNonNull(id); Objects.requireNonNull(author); Objects.requireNonNull(date); Objects.requireNonNull(text);
        if (id.isBlank()) throw new IllegalArgumentException("Invalid comment id");
        initials = initials == null || initials.isBlank() ? initialsOf(author) : initials;
        if (parentId != null && parentId.isBlank()) parentId = null;
    }
    public static String initialsOf(String author) {
        StringBuilder b = new StringBuilder();
        for (String part : author.trim().split("\\s+")) if (!part.isEmpty() && b.length() < 3) b.appendCodePoint(Character.toUpperCase(part.codePointAt(0)));
        return b.isEmpty() ? "?" : b.toString();
    }
    public WordComment withText(String value) { return new WordComment(id,author,initials,date,value,parentId,resolved); }
    public WordComment withResolved(boolean value) { return new WordComment(id,author,initials,date,text,parentId,value); }
    public boolean isReply() { return parentId != null; }
}
