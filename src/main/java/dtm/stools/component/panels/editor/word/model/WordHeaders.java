package dtm.stools.component.panels.editor.word.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record WordHeaders(Map<Kind,List<WordBlock>> parts, boolean differentFirst, boolean differentOddEven) {
    public enum Kind { HEADER, FIRST_HEADER, EVEN_HEADER, FOOTER, FIRST_FOOTER, EVEN_FOOTER }
    public static final WordHeaders EMPTY = new WordHeaders(Map.of(),false,false);
    public WordHeaders {
        EnumMap<Kind,List<WordBlock>> copy = new EnumMap<>(Kind.class);
        Objects.requireNonNull(parts).forEach((k,v) -> { if (v != null && !v.isEmpty()) copy.put(Objects.requireNonNull(k),List.copyOf(v)); });
        parts = java.util.Collections.unmodifiableMap(copy);
    }
    public List<WordBlock> get(Kind kind) { return parts.getOrDefault(kind,List.of()); }
    public WordHeaders with(Kind kind, List<? extends WordBlock> blocks) {
        EnumMap<Kind,List<WordBlock>> copy = new EnumMap<>(Kind.class); copy.putAll(parts);
        if (blocks == null || blocks.isEmpty()) copy.remove(kind); else copy.put(kind,List.copyOf(blocks));
        return new WordHeaders(copy,differentFirst,differentOddEven);
    }
    public WordHeaders withOptions(boolean first, boolean oddEven) { return new WordHeaders(parts,first,oddEven); }
    public List<WordBlock> header(int pageNumber, boolean firstOfSection) { return resolve(Kind.HEADER,Kind.FIRST_HEADER,Kind.EVEN_HEADER,pageNumber,firstOfSection); }
    public List<WordBlock> footer(int pageNumber, boolean firstOfSection) { return resolve(Kind.FOOTER,Kind.FIRST_FOOTER,Kind.EVEN_FOOTER,pageNumber,firstOfSection); }
    private List<WordBlock> resolve(Kind normal, Kind first, Kind even, int pageNumber, boolean firstOfSection) {
        if (differentFirst && firstOfSection) return get(first);
        if (differentOddEven && pageNumber % 2 == 0) return get(even);
        return get(normal);
    }
    public boolean isEmpty() { return parts.isEmpty(); }
}
