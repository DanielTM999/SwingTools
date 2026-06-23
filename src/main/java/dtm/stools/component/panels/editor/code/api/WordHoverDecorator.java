package dtm.stools.component.panels.editor.code.api;

import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@FunctionalInterface
public interface WordHoverDecorator {

    WordHoverStyle decorate(WordClickEvent candidate);

    static WordHoverDecorator forMap(Map<String, WordHoverStyle> map) {
        return candidate -> {
            if (map == null) {
                return null;
            }
            if (!map.containsKey(candidate.word())) {
                return null;
            }
            WordHoverStyle s = map.get(candidate.word());
            return s != null ? s : WordHoverStyle.DEFAULT;
        };
    }

    static WordHoverDecorator forWords(Set<String> words, WordHoverStyle style) {
        WordHoverStyle effective = style != null ? style : WordHoverStyle.DEFAULT;
        return candidate -> {
            if (words == null) {
                return null;
            }
            return words.contains(candidate.word()) ? effective : null;
        };
    }

    static WordHoverDecorator forWords(Set<String> words) {
        return forWords(words, null);
    }

    static WordHoverDecorator forPredicate(Predicate<String> filter, WordHoverStyle style) {
        WordHoverStyle effective = style != null ? style : WordHoverStyle.DEFAULT;
        return candidate -> {
            if (filter == null) {
                return null;
            }
            return filter.test(candidate.word()) ? effective : null;
        };
    }

    static WordHoverDecorator forPredicate(Predicate<String> filter) {
        return forPredicate(filter, null);
    }

    default WordHoverDecorator orElse(WordHoverDecorator fallback) {
        return candidate -> {
            WordHoverStyle s = decorate(candidate);
            if (s != null) {
                return s;
            }
            return fallback == null ? null : fallback.decorate(candidate);
        };
    }
}
