package dtm.stools.component.panels.editor.word.math;

import java.util.Objects;

public record WordMathAccent(String accent, WordMath base) implements WordMath {
    public WordMathAccent { Objects.requireNonNull(accent); Objects.requireNonNull(base); }
}
