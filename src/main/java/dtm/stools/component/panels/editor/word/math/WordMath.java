package dtm.stools.component.panels.editor.word.math;

public interface WordMath {
    default String linear() { return WordMathParser.format(this); }
}
