package dtm.stools.component.panels.editor.word.math;

import java.util.List;

public record WordMathRow(List<WordMath> items) implements WordMath {
    public WordMathRow { items = List.copyOf(items); }
    public static WordMath of(List<WordMath> items) { return items.size() == 1 ? items.getFirst() : new WordMathRow(items); }
}
