package dtm.stools.component.panels.editor.word.math;

import java.util.Objects;

public record WordMathRadical(WordMath degree, WordMath body) implements WordMath {
    public WordMathRadical { Objects.requireNonNull(body); }
}
