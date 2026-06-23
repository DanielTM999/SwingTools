package dtm.stools.component.panels.editor.code.prototype.folding;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public sealed interface FoldRule {

    record Pair(char open, char close) implements FoldRule {}

    record StringPair(String open, String close) implements FoldRule {
        public StringPair {
            Objects.requireNonNull(open, "open");
            Objects.requireNonNull(close, "close");
            if (open.isEmpty()) throw new IllegalArgumentException("open must not be empty");
            if (close.isEmpty()) throw new IllegalArgumentException("close must not be empty");
        }
    }

    record Section(String markerPrefix) implements FoldRule {}

    record Custom(RegionProvider provider) implements FoldRule {
        public Custom {
            Objects.requireNonNull(provider, "provider");
        }
    }

    record Context(int lineCount, LineReader lineReader) {
        public Context {
            if (lineCount < 0) throw new IllegalArgumentException("lineCount must be >= 0");
            Objects.requireNonNull(lineReader, "lineReader");
        }

        public String lineAt(int line) {
            if (line < 0 || line >= lineCount) {
                throw new IndexOutOfBoundsException("line " + line + " out of bounds");
            }
            String text = lineReader.lineAt(line);
            return text == null ? "" : text;
        }
    }

    @FunctionalInterface
    interface LineReader {
        String lineAt(int line);
    }

    @FunctionalInterface
    interface RegionProvider {
        List<FoldRegion> compute(Context context);
    }

    static FoldRule pair(char open, char close) {
        return new Pair(open, close);
    }

    static FoldRule pair(String open, String close) {
        return new StringPair(open, close);
    }

    static FoldRule section(String markerPrefix) {
        return new Section(markerPrefix);
    }

    static FoldRule custom(RegionProvider provider) {
        return new Custom(provider);
    }

    static FoldRule xmlTags() {
        return custom(context -> computeTagRegions(context, false));
    }

    static FoldRule htmlTags() {
        return custom(context -> computeTagRegions(context, true));
    }

    static FoldRule indentation() {
        return indentation(4);
    }

    static FoldRule indentation(int tabSize) {
        if (tabSize <= 0) throw new IllegalArgumentException("tabSize must be > 0");
        return custom(context -> computeIndentationRegions(context, tabSize));
    }

    private static List<FoldRegion> computeTagRegions(Context context, boolean htmlMode) {
        List<FoldRegion> regions = new ArrayList<>();
        Deque<String> stackNames = new ArrayDeque<>();
        Deque<Integer> stackLines = new ArrayDeque<>();
        boolean inComment = false;
        StringBuilder pendingTagBody = null;
        int pendingTagStartLine = -1;

        for (int line = 0; line < context.lineCount(); line++) {
            String text = context.lineAt(line);
            int pos = 0;

            while (pos < text.length()) {
                if (pendingTagBody != null) {
                    pendingTagBody.append('\n');
                    int currentLineOffset = pendingTagBody.length();
                    pendingTagBody.append(text.substring(pos));
                    int gt = findTagEnd(pendingTagBody.toString(), 0);
                    if (gt < 0) break;

                    processTagBody(pendingTagBody.substring(0, gt).trim(), pendingTagStartLine, line,
                            htmlMode, stackNames, stackLines, regions);
                    pos += gt - currentLineOffset + 1;
                    pendingTagBody = null;
                    pendingTagStartLine = -1;
                    continue;
                }

                if (inComment) {
                    int commentEnd = text.indexOf("-->", pos);
                    if (commentEnd < 0) break;
                    inComment = false;
                    pos = commentEnd + 3;
                    continue;
                }

                int lt = text.indexOf('<', pos);
                if (lt < 0) break;
                if (startsWith(text, lt, "<!--")) {
                    int commentEnd = text.indexOf("-->", lt + 4);
                    if (commentEnd < 0) {
                        inComment = true;
                        break;
                    }
                    pos = commentEnd + 3;
                    continue;
                }

                int gt = findTagEnd(text, lt + 1);
                if (gt < 0) {
                    pendingTagBody = new StringBuilder(text.substring(lt + 1));
                    pendingTagStartLine = line;
                    break;
                }

                String body = text.substring(lt + 1, gt).trim();
                pos = gt + 1;
                processTagBody(body, line, line, htmlMode, stackNames, stackLines, regions);
            }
        }
        return regions;
    }

    private static void processTagBody(String body, int startLine, int endLine, boolean htmlMode,
                                       Deque<String> stackNames, Deque<Integer> stackLines,
                                       List<FoldRegion> regions) {
        if (body.isEmpty() || body.startsWith("!") || body.startsWith("?")) return;

        if (body.startsWith("/")) {
            String name = normalizeTagName(readTagName(body, 1), htmlMode);
            closeXmlTag(name, endLine, stackNames, stackLines, regions);
        } else {
            String name = normalizeTagName(readTagName(body, 0), htmlMode);
            if (!name.isEmpty() && !body.endsWith("/") && !isHtmlVoidTag(name, htmlMode)) {
                stackNames.push(name);
                stackLines.push(startLine);
            }
        }
    }

    private static List<FoldRegion> computeIndentationRegions(Context context, int tabSize) {
        List<FoldRegion> regions = new ArrayList<>();
        Deque<Integer> stackStartLines = new ArrayDeque<>();
        Deque<Integer> stackBaseIndents = new ArrayDeque<>();
        int previousContentLine = -1;
        int previousContentIndent = 0;

        for (int line = 0; line < context.lineCount(); line++) {
            String text = context.lineAt(line);
            if (text.trim().isEmpty()) continue;

            int indent = indentationWidth(text, tabSize);
            while (!stackBaseIndents.isEmpty() && indent <= stackBaseIndents.peek()) {
                int start = stackStartLines.pop();
                stackBaseIndents.pop();
                if (previousContentLine > start) regions.add(new FoldRegion(start, previousContentLine, false));
            }

            if (previousContentLine >= 0 && indent > previousContentIndent) {
                stackStartLines.push(previousContentLine);
                stackBaseIndents.push(previousContentIndent);
            }

            previousContentLine = line;
            previousContentIndent = indent;
        }

        while (!stackStartLines.isEmpty()) {
            int start = stackStartLines.pop();
            stackBaseIndents.pop();
            if (previousContentLine > start) regions.add(new FoldRegion(start, previousContentLine, false));
        }
        return regions;
    }

    private static void closeXmlTag(String name, int line, Deque<String> stackNames, Deque<Integer> stackLines, List<FoldRegion> regions) {
        if (name.isEmpty()) return;
        while (!stackNames.isEmpty()) {
            String openName = stackNames.pop();
            int openLine = stackLines.pop();
            if (openName.equals(name)) {
                if (line > openLine) regions.add(new FoldRegion(openLine, line, false));
                return;
            }
        }
    }

    private static int findTagEnd(String text, int start) {
        char quote = 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
            } else if (c == '"' || c == '\'') {
                quote = c;
            } else if (c == '>') {
                return i;
            }
        }
        return -1;
    }

    private static String readTagName(String body, int start) {
        int i = start;
        while (i < body.length() && Character.isWhitespace(body.charAt(i))) i++;
        int nameStart = i;
        while (i < body.length()) {
            char c = body.charAt(i);
            if (Character.isWhitespace(c) || c == '/' || c == '>') break;
            i++;
        }
        return body.substring(nameStart, i);
    }

    private static String normalizeTagName(String name, boolean htmlMode) {
        return htmlMode ? name.toLowerCase(Locale.ROOT) : name;
    }

    private static boolean isHtmlVoidTag(String name, boolean htmlMode) {
        return htmlMode && HTML_VOID_TAGS.contains(name);
    }

    private static int indentationWidth(String text, int tabSize) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                width++;
            } else if (c == '\t') {
                width += tabSize - (width % tabSize);
            } else {
                break;
            }
        }
        return width;
    }

    private static boolean startsWith(String text, int offset, String prefix) {
        return offset >= 0 && offset + prefix.length() <= text.length()
                && text.regionMatches(offset, prefix, 0, prefix.length());
    }

    Set<String> HTML_VOID_TAGS = Set.of(
            "area", "base", "br", "col", "embed", "hr", "img", "input",
            "link", "meta", "param", "source", "track", "wbr"
    );
}
