package dtm.stools.component.panels.editor.word.model;

import java.util.*;

public record WordSectionProperties(BreakType breakType, WordHeaders headers, Set<WordHeaders.Kind> linkedHeaders,
                                    Map<WordHeaders.Kind,String> originalReferences, List<String> extras) {
    public enum BreakType { NEXT_PAGE, CONTINUOUS, EVEN_PAGE, ODD_PAGE, NEXT_COLUMN }
    public static final WordSectionProperties DEFAULT = new WordSectionProperties(BreakType.NEXT_PAGE,WordHeaders.EMPTY,
            EnumSet.allOf(WordHeaders.Kind.class),Map.of(),List.of());
    public WordSectionProperties {
        Objects.requireNonNull(breakType); Objects.requireNonNull(headers);
        linkedHeaders = Set.copyOf(linkedHeaders);
        originalReferences = Map.copyOf(originalReferences);
        extras = List.copyOf(extras);
    }
    public WordSectionProperties withBreakType(BreakType value) {
        return new WordSectionProperties(value,headers,linkedHeaders,originalReferences,extras);
    }
    public WordSectionProperties withHeaders(WordHeaders value) {
        return new WordSectionProperties(breakType,value,linkedHeaders,originalReferences,extras);
    }
    public WordSectionProperties withLinked(WordHeaders.Kind kind, boolean linked) {
        Set<WordHeaders.Kind> next = new HashSet<>(linkedHeaders);
        if (linked) next.add(kind); else next.remove(kind);
        return new WordSectionProperties(breakType,headers,next,originalReferences,extras);
    }
    public WordHeaders resolveHeaders(WordHeaders previous, boolean oddEven) {
        WordHeaders resolved = headers;
        for (WordHeaders.Kind kind : linkedHeaders) resolved = resolved.with(kind,previous.get(kind));
        return resolved.withOptions(headers.differentFirst(),oddEven);
    }
}
