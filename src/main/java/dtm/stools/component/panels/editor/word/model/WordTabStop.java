package dtm.stools.component.panels.editor.word.model;

import java.util.Objects;

public record WordTabStop(float position, Alignment alignment, Leader leader) {
    public enum Alignment { LEFT, CENTER, RIGHT, DECIMAL }
    public enum Leader { NONE, DOT, HYPHEN, UNDERSCORE }
    public WordTabStop {
        Objects.requireNonNull(alignment); Objects.requireNonNull(leader);
        if (!Float.isFinite(position) || position < 0 || position > 14400) throw new IllegalArgumentException("Invalid tab stop");
    }
    public static WordTabStop left(float position) { return new WordTabStop(position,Alignment.LEFT,Leader.NONE); }
}
