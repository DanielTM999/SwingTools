package dtm.stools.component.panels.editor.word.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class WordResources {
    public static final WordResources EMPTY = new WordResources(Map.of());
    private final Map<String,WordResource> resources;

    private WordResources(Map<String,WordResource> resources) { this.resources = Collections.unmodifiableMap(new LinkedHashMap<>(resources)); }

    public static WordResources of(Collection<WordResource> values) {
        Map<String,WordResource> map = new LinkedHashMap<>();
        for (WordResource r : values) map.put(r.id(),r);
        return new WordResources(map);
    }
    public WordResources with(WordResource resource) {
        Objects.requireNonNull(resource);
        if (resource.equals(resources.get(resource.id()))) return this;
        Map<String,WordResource> copy = new LinkedHashMap<>(resources); copy.put(resource.id(),resource); return new WordResources(copy);
    }
    public WordResources retain(Set<String> ids) {
        Map<String,WordResource> copy = new LinkedHashMap<>(resources); copy.keySet().retainAll(ids);
        return copy.size() == resources.size() ? this : new WordResources(copy);
    }
    public WordResources merge(WordResources other) {
        WordResources result = this;
        for (WordResource r : other.values()) result = result.with(r);
        return result;
    }
    public Optional<WordResource> get(String id) { return Optional.ofNullable(resources.get(id)); }
    public Collection<WordResource> values() { return resources.values(); }
    public Set<String> ids() { return resources.keySet(); }
    public int size() { return resources.size(); }
    @Override public boolean equals(Object o) { return o instanceof WordResources r && r.resources.keySet().equals(resources.keySet()); }
    @Override public int hashCode() { return resources.keySet().hashCode(); }
    @Override public String toString() { return "WordResources" + resources.keySet(); }
}
