package dtm.stools.component.panels.editor.code;

public final class MarkupTagCloser {

    private MarkupTagCloser() {
    }

    public static String closingTagFor(CharSequence before, CharSequence after) {
        if (before == null || before.isEmpty() || before.charAt(before.length() - 1) == '/') {
            return null;
        }
        String preceding = before.toString();
        if (preceding.lastIndexOf("<!--") > preceding.lastIndexOf("-->")
                || preceding.lastIndexOf("<![CDATA[") > preceding.lastIndexOf("]]>")) {
            return null;
        }
        int open = openingBracket(preceding);
        if (open < 0) {
            return null;
        }
        String name = tagName(preceding, open + 1);
        if (name == null) {
            return null;
        }
        String closing = "</" + name + ">";
        if (after != null && after.toString().startsWith(closing)) {
            return null;
        }
        return closing;
    }

    private static int openingBracket(String text) {
        char quote = 0;
        for (int i = text.length() - 1; i >= 0; i--) {
            char ch = text.charAt(i);
            if (quote != 0) {
                if (ch == quote) {
                    quote = 0;
                }
            } else if (ch == '"' || ch == '\'') {
                quote = ch;
            } else if (ch == '>') {
                return -1;
            } else if (ch == '<') {
                return i;
            }
        }
        return -1;
    }

    private static String tagName(String text, int start) {
        if (start >= text.length() || !isNameStart(text.charAt(start))) {
            return null;
        }
        int end = start + 1;
        while (end < text.length() && isNamePart(text.charAt(end))) {
            end++;
        }
        if (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
            return null;
        }
        return text.substring(start, end);
    }

    private static boolean isNameStart(char ch) {
        return Character.isLetter(ch) || ch == '_' || ch == ':';
    }

    private static boolean isNamePart(char ch) {
        return isNameStart(ch) || Character.isDigit(ch) || ch == '.' || ch == '-';
    }
}
