package dtm.stools.component.panels.editor.word.model;

import dtm.stools.component.panels.editor.word.math.WordMath;
import dtm.stools.component.panels.editor.word.math.WordMathLayout;
import dtm.stools.component.panels.editor.word.math.WordMathParser;
import java.util.Objects;

public record WordEquation(String id, WordMath math, boolean display, float fontSize) implements WordInlineObject {
    public static final String TYPE = "equation";
    public WordEquation {
        WordInlineObject.requireId(id); Objects.requireNonNull(math);
        if (!Float.isFinite(fontSize) || fontSize < 4 || fontSize > 200) throw new IllegalArgumentException("Invalid equation size");
    }
    public static WordEquation parse(String linear, boolean display) { return new WordEquation(WordIds.next(),WordMathParser.parse(linear),display,12); }
    @Override public String type() { return TYPE; }
    @Override public float width() { return WordMathLayout.measure(math,fontSize).width() + 2; }
    @Override public float height() { return WordMathLayout.measure(math,fontSize).height() + 2; }
    public float ascent() { return WordMathLayout.measure(math,fontSize).ascent() + 1; }
    @Override public boolean lockAspectRatio() { return true; }
    @Override public WordEquation withId(String value) { return new WordEquation(value,math,display,fontSize); }
    @Override public WordEquation resize(float w, float h) {
        float current = height();
        return new WordEquation(id,math,display,Math.max(4,Math.min(200,fontSize * (current <= 0 ? 1 : h/current))));
    }
    public WordEquation withMath(WordMath value) { return new WordEquation(id,value,display,fontSize); }
    public WordEquation withDisplay(boolean value) { return new WordEquation(id,math,value,fontSize); }
    public String linear() { return WordMathParser.format(math); }
    @Override public String plainText() { return linear(); }
}
