package dtm.stools.component.panels.editor.word.math;

import java.util.Objects;

public record WordMathScript(WordMath base, WordMath subscript, WordMath superscript) implements WordMath {
    public WordMathScript {
        Objects.requireNonNull(base);
        if (subscript == null && superscript == null) throw new IllegalArgumentException("A script needs a subscript or superscript");
    }
}
