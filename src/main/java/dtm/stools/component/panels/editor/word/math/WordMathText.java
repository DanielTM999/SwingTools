package dtm.stools.component.panels.editor.word.math;

import java.util.Objects;

public record WordMathText(String text, boolean plain) implements WordMath {
    public WordMathText { Objects.requireNonNull(text); }
    public WordMathText(String text) { this(text,false); }
}
