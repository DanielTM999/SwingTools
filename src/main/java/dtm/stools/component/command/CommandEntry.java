package dtm.stools.component.command;

import java.util.Objects;

/** shortcut is display text; the palette does not install command shortcuts. */
public record CommandEntry(String id, String name, String group, String description, String shortcut, boolean enabled) {
    public CommandEntry {
        Objects.requireNonNull(id); Objects.requireNonNull(name);
        if (id.isBlank()) throw new IllegalArgumentException("empty command id");
        group = Objects.requireNonNullElse(group, "");
        description = Objects.requireNonNullElse(description, "");
        shortcut = Objects.requireNonNullElse(shortcut, "");
    }
}
