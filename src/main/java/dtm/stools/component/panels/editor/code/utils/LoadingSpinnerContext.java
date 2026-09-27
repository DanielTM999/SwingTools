package dtm.stools.component.panels.editor.code.utils;

import java.awt.Color;

public record LoadingSpinnerContext(Usage usage, int size, Color color) {

    public enum Usage {
        AUTOCOMPLETE,
        RENAME
    }

    public LoadingSpinnerContext {
        usage = usage == null ? Usage.AUTOCOMPLETE : usage;
        size = Math.max(8, size);
    }
}
