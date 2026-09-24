package dtm.stools.component.panels.editor.word.provider;

import java.util.Objects;

public record WordCommandEntry(String id, String name, String group, boolean enabled) {
    public WordCommandEntry { Objects.requireNonNull(id); Objects.requireNonNull(name); group = group == null ? "" : group; }
}
