package dtm.stools.component.panels.editor.code.search;

import dtm.stools.component.panels.editor.code.prototype.TextBuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SearchEngine {

    public List<SearchMatch> findAll(TextBuffer buffer, String query, SearchOptions opts) {
        List<SearchMatch> matches = new ArrayList<>();
        if (buffer == null || query == null || query.isEmpty()) return matches;
        if (opts == null) opts = new SearchOptions();

        String text = buffer.getText();
        Pattern pattern = buildPattern(query, opts);
        if (pattern == null) return matches;

        Matcher m = pattern.matcher(text);
        while (m.find()) {
            if (m.start() == m.end()) {
                if (m.end() >= text.length()) break;
                m.region(m.end() + 1, text.length());
                continue;
            }
            matches.add(new SearchMatch(m.start(), m.end()));
        }
        return matches;
    }

    public SearchMatch findNext(List<SearchMatch> all, int fromOffset, boolean wrap) {
        if (all == null || all.isEmpty()) return null;
        for (SearchMatch m : all) {
            if (m.startOffset() >= fromOffset) return m;
        }
        return wrap ? all.get(0) : null;
    }

    public SearchMatch findPrev(List<SearchMatch> all, int fromOffset, boolean wrap) {
        if (all == null || all.isEmpty()) return null;
        SearchMatch last = null;
        for (SearchMatch m : all) {
            if (m.endOffset() <= fromOffset) last = m;
            else break;
        }
        if (last != null) return last;
        return wrap ? all.get(all.size() - 1) : null;
    }

    protected Pattern buildPattern(String query, SearchOptions opts) {
        try {
            String regex = opts.isRegex() ? query : Pattern.quote(query);
            if (opts.isWholeWord()) {
                regex = "\\b(?:" + regex + ")\\b";
            }
            int flags = 0;
            if (!opts.isCaseSensitive()) flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
            return Pattern.compile(regex, flags);
        } catch (PatternSyntaxException ex) {
            return null;
        }
    }
}
