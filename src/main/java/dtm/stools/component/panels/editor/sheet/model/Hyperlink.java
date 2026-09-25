package dtm.stools.component.panels.editor.sheet.model;

import java.util.Objects;

public record Hyperlink(String target, String tooltip) {
    public Hyperlink {
        Objects.requireNonNull(target);
        tooltip = Objects.requireNonNullElse(tooltip, "");
    }

    public boolean internal() { return target.startsWith("#"); }
}
