package dtm.stools.component.panels.editor.word.api;

import java.util.Objects;

public record WordSuggestion(long revision,WordSelection range,String original,String replacement) {
    public WordSuggestion {Objects.requireNonNull(range);Objects.requireNonNull(original);Objects.requireNonNull(replacement);}
}
