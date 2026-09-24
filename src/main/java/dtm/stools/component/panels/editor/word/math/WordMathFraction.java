package dtm.stools.component.panels.editor.word.math;

import java.util.Objects;

public record WordMathFraction(WordMath numerator, WordMath denominator) implements WordMath {
    public WordMathFraction { Objects.requireNonNull(numerator); Objects.requireNonNull(denominator); }
}
