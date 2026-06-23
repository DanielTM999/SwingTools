package dtm.stools.component.panels.editor.code.autocomplete;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SnippetExpansion {

    private SnippetExpansion() {}

    public record Stop(int order, int start, int length) {}

    public record Result(String text, List<Stop> stops, int finalCaret) {}

    public static Result expand(String snippet) {
        if (snippet == null || snippet.isEmpty()) {
            return new Result("", List.of(), 0);
        }
        StringBuilder out = new StringBuilder(snippet.length());
        List<Stop> stops = new ArrayList<>();
        int finalCaret = -1;
        int i = 0;
        int n = snippet.length();
        while (i < n) {
            char c = snippet.charAt(i);
            if (c == '\\' && i + 1 < n) {
                char next = snippet.charAt(i + 1);
                if (next == '$' || next == '\\' || next == '}') {
                    out.append(next);
                    i += 2;
                    continue;
                }
            }
            if (c == '$' && i + 1 < n) {
                char next = snippet.charAt(i + 1);
                if (next == '{') {
                    int end = snippet.indexOf('}', i + 2);
                    if (end > i + 2) {
                        String body = snippet.substring(i + 2, end);
                        int colon = body.indexOf(':');
                        String idxStr = colon >= 0 ? body.substring(0, colon) : body;
                        String def = colon >= 0 ? body.substring(colon + 1) : "";
                        if (isInteger(idxStr)) {
                            int idx = Integer.parseInt(idxStr);
                            int startInOut = out.length();
                            out.append(def);
                            int len = def.length();
                            if (idx == 0) {
                                if (finalCaret < 0) finalCaret = startInOut;
                            } else {
                                stops.add(new Stop(idx, startInOut, len));
                            }
                            i = end + 1;
                            continue;
                        }
                    }
                } else if (Character.isDigit(next)) {
                    int j = i + 1;
                    while (j < n && Character.isDigit(snippet.charAt(j))) j++;
                    int idx = Integer.parseInt(snippet.substring(i + 1, j));
                    int startInOut = out.length();
                    if (idx == 0) {
                        if (finalCaret < 0) finalCaret = startInOut;
                    } else {
                        stops.add(new Stop(idx, startInOut, 0));
                    }
                    i = j;
                    continue;
                }
            }
            out.append(c);
            i++;
        }
        stops.sort(Comparator.comparingInt(Stop::order).thenComparingInt(Stop::start));
        if (finalCaret < 0) finalCaret = out.length();
        return new Result(out.toString(), stops, finalCaret);
    }

    private static boolean isInteger(String s) {
        if (s == null || s.isEmpty()) return false;
        for (int k = 0; k < s.length(); k++) {
            if (!Character.isDigit(s.charAt(k))) return false;
        }
        return true;
    }
}
