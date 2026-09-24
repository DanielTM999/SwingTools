package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record WordParts(WordStyleSheet styles, WordNumbering numbering, WordResources resources, WordHeaders headers,
                        Map<String,WordNote> notes, List<WordComment> comments, Map<String,String> metadata) {
    public static final WordParts EMPTY = new WordParts(WordStyleSheet.defaults(),WordNumbering.EMPTY,WordResources.EMPTY,WordHeaders.EMPTY,Map.of(),List.of(),Map.of());
    public WordParts {
        Objects.requireNonNull(styles); Objects.requireNonNull(numbering); Objects.requireNonNull(resources); Objects.requireNonNull(headers);
        notes = Collections.unmodifiableMap(new LinkedHashMap<>(notes));
        comments = List.copyOf(comments);
        metadata = Collections.unmodifiableMap(new LinkedHashMap<>(metadata));
    }
    public WordParts withStyles(WordStyleSheet value) { return new WordParts(value,numbering,resources,headers,notes,comments,metadata); }
    public WordParts withNumbering(WordNumbering value) { return new WordParts(styles,value,resources,headers,notes,comments,metadata); }
    public WordParts withResources(WordResources value) { return new WordParts(styles,numbering,value,headers,notes,comments,metadata); }
    public WordParts withHeaders(WordHeaders value) { return new WordParts(styles,numbering,resources,value,notes,comments,metadata); }
    public WordParts withNotes(Map<String,WordNote> value) { return new WordParts(styles,numbering,resources,headers,value,comments,metadata); }
    public WordParts withComments(List<WordComment> value) { return new WordParts(styles,numbering,resources,headers,notes,value,metadata); }
    public WordParts withMetadata(Map<String,String> value) { return new WordParts(styles,numbering,resources,headers,notes,comments,value); }
    public WordParts withNote(WordNote note) { Map<String,WordNote> copy = new LinkedHashMap<>(notes); copy.put(note.id(),note); return withNotes(copy); }
    public WordParts withComment(WordComment comment) {
        List<WordComment> copy = new ArrayList<>(comments);
        copy.removeIf(c -> c.id().equals(comment.id())); copy.add(comment);
        return withComments(copy);
    }
    public WordParts withoutComment(String id) {
        List<WordComment> copy = new ArrayList<>(comments);
        copy.removeIf(c -> c.id().equals(id) || id.equals(c.parentId()));
        return withComments(copy);
    }
}
