package dtm.stools.component.panels.editor.word.math;

import java.util.Objects;

public record WordMathDelimiter(String open, String close, WordMath content) implements WordMath {
    public WordMathDelimiter { Objects.requireNonNull(open); Objects.requireNonNull(close); Objects.requireNonNull(content); }
}
