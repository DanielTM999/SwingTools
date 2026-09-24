package dtm.stools.component.panels.editor.word.math;

import java.util.Objects;

public record WordMathFunction(String name, WordMath argument) implements WordMath {
    public WordMathFunction { Objects.requireNonNull(name); Objects.requireNonNull(argument); }
}
