package dtm.stools.component.inputfields.textarea;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class TextAreaSearch {

    private TextAreaSearch() {
    }

    static Pattern compile(String query, boolean caseSensitive, boolean wholeWord, boolean regex) {
        if (query == null || query.isEmpty()) return null;
        try {
            String expression = regex ? query : Pattern.quote(query);
            if (wholeWord) expression = "\\b(?:" + expression + ")\\b";
            int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            return Pattern.compile(expression, flags);
        } catch (PatternSyntaxException ex) {
            return null;
        }
    }

    static List<TextAreaSearchMatch> findAll(String text, Pattern pattern) {
        List<TextAreaSearchMatch> matches = new ArrayList<>();
        if (text == null || pattern == null) return matches;
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            if (matcher.start() == matcher.end()) {
                if (matcher.end() >= text.length()) break;
                matcher.region(matcher.end() + 1, text.length());
                continue;
            }
            matches.add(new TextAreaSearchMatch(matcher.start(), matcher.end()));
        }
        return matches;
    }

    static int indexAtOrAfter(List<TextAreaSearchMatch> matches, int offset) {
        if (matches.isEmpty()) return -1;
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).start() >= offset) return i;
        }
        return 0;
    }

    static int indexBefore(List<TextAreaSearchMatch> matches, int offset) {
        if (matches.isEmpty()) return -1;
        for (int i = matches.size() - 1; i >= 0; i--) {
            if (matches.get(i).end() <= offset) return i;
        }
        return matches.size() - 1;
    }

    static String replacementFor(Pattern pattern, String text, TextAreaSearchMatch match, String replacement, boolean regex) {
        String literal = replacement == null ? "" : replacement;
        if (!regex || pattern == null) return literal;
        try {
            Matcher matcher = pattern.matcher(text);
            if (!matcher.find(match.start()) || matcher.start() != match.start()) return literal;
            StringBuilder builder = new StringBuilder();
            matcher.appendReplacement(builder, literal);
            return builder.substring(match.start());
        } catch (IllegalArgumentException | IndexOutOfBoundsException ex) {
            return literal;
        }
    }
}
