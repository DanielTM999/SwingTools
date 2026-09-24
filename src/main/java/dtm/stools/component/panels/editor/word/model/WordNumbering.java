package dtm.stools.component.panels.editor.word.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class WordNumbering {
    public static final WordNumbering EMPTY = new WordNumbering(Map.of());
    private final Map<String,WordListDefinition> lists;

    private WordNumbering(Map<String,WordListDefinition> lists) { this.lists = Collections.unmodifiableMap(new LinkedHashMap<>(lists)); }

    public static WordNumbering of(Collection<WordListDefinition> values) {
        Map<String,WordListDefinition> map = new LinkedHashMap<>();
        for (WordListDefinition d : values) map.put(d.id(),d);
        return new WordNumbering(map);
    }
    public Optional<WordListDefinition> get(String id) { return Optional.ofNullable(lists.get(id)); }
    public Collection<WordListDefinition> lists() { return lists.values(); }
    public WordNumbering with(WordListDefinition definition) {
        Map<String,WordListDefinition> copy = new LinkedHashMap<>(lists); copy.put(definition.id(),definition); return new WordNumbering(copy);
    }
    public String nextId() {
        int n = 1;
        while (lists.containsKey(Integer.toString(n))) n++;
        return Integer.toString(n);
    }
    @Override public boolean equals(Object o) { return o instanceof WordNumbering n && n.lists.equals(lists); }
    @Override public int hashCode() { return lists.hashCode(); }
}
