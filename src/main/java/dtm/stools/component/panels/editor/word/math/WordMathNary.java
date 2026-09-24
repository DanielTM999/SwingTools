package dtm.stools.component.panels.editor.word.math;

import java.util.Objects;

public record WordMathNary(String operator, WordMath lower, WordMath upper, WordMath body) implements WordMath {
    public WordMathNary { Objects.requireNonNull(operator); Objects.requireNonNull(body); }
}
