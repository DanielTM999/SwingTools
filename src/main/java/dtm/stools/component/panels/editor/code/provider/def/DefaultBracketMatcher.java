package dtm.stools.component.panels.editor.code.provider.def;


import dtm.stools.component.panels.editor.code.provider.BracketMatcher;

public class DefaultBracketMatcher implements BracketMatcher {

    public static final DefaultBracketMatcher INSTANCE = new DefaultBracketMatcher();

    private DefaultBracketMatcher() { }

    @Override
    public int findMatch(String buffer, int offset) {
        if (buffer == null || buffer.isEmpty()) return -1;
        int len = buffer.length();
        int probe = -1;
        char c = 0;
        if (offset >= 0 && offset < len && isBracket(buffer.charAt(offset))) {
            probe = offset;
            c = buffer.charAt(offset);
        } else if (offset - 1 >= 0 && offset - 1 < len && isBracket(buffer.charAt(offset - 1))) {
            probe = offset - 1;
            c = buffer.charAt(probe);
        }
        if (probe < 0) return -1;
        char open, close;
        int dir;
        switch (c) {
            case '(': open = '('; close = ')'; dir = +1; break;
            case ')': open = '('; close = ')'; dir = -1; break;
            case '[': open = '['; close = ']'; dir = +1; break;
            case ']': open = '['; close = ']'; dir = -1; break;
            case '{': open = '{'; close = '}'; dir = +1; break;
            case '}': open = '{'; close = '}'; dir = -1; break;
            default: return -1;
        }
        return scan(buffer, probe, dir, open, close);
    }

    private static boolean isBracket(char c) {
        return c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}';
    }

    private static int scan(String buf, int start, int dir, char open, char close) {
        int len = buf.length();
        int depth = 0;
        int i = start;
        while (i >= 0 && i < len) {
            char ch = buf.charAt(i);
            // skip string/char literals when scanning forward
            if (dir > 0 && (ch == '"' || ch == '\'')) {
                i = skipStringForward(buf, i, ch);
                if (i < 0) return -1;
                i++;
                continue;
            }
            if (dir > 0 && ch == '/' && i + 1 < len) {
                char n = buf.charAt(i + 1);
                if (n == '/') { while (i < len && buf.charAt(i) != '\n') i++; continue; }
                if (n == '*') {
                    i += 2;
                    while (i + 1 < len && !(buf.charAt(i) == '*' && buf.charAt(i + 1) == '/')) i++;
                    i += 2; continue;
                }
            }
            if (ch == open) depth += (dir > 0 ? 1 : -1);
            else if (ch == close) depth += (dir > 0 ? -1 : 1);
            if (depth == 0 && i != start) return i;
            i += dir;
        }
        return -1;
    }

    private static int skipStringForward(String buf, int start, char quote) {
        int len = buf.length();
        int i = start + 1;
        while (i < len) {
            char ch = buf.charAt(i);
            if (ch == '\\') { i += 2; continue; }
            if (ch == quote) return i;
            if (ch == '\n') return i - 1;
            i++;
        }
        return -1;
    }
}
