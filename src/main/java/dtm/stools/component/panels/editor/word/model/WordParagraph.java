package dtm.stools.component.panels.editor.word.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record WordParagraph(UUID id, List<WordInline> runs, WordParagraphStyle style, List<String> bookmarks, WordPageSettings sectionBreak) implements WordBlock {
    public WordParagraph {
        Objects.requireNonNull(id); Objects.requireNonNull(style); Objects.requireNonNull(runs);
        List<WordInline> normalized = new ArrayList<>();
        for (WordInline inline : runs) {
            Objects.requireNonNull(inline);
            if (inline instanceof WordRun run) {
                if (run.text().isEmpty()) continue;
                if (!normalized.isEmpty() && normalized.getLast() instanceof WordRun previous && previous.style().equals(run.style())) {
                    normalized.removeLast();
                    normalized.add(new WordRun(previous.text() + run.text(), run.style()));
                    continue;
                }
            }
            normalized.add(inline);
        }
        runs = List.copyOf(normalized);
        bookmarks = bookmarks == null ? List.of() : bookmarks.stream().filter(b -> b != null && !b.isBlank()).distinct().toList();
    }
    public WordParagraph(UUID id, List<? extends WordInline> runs, WordParagraphStyle style) {
        this(id, new ArrayList<WordInline>(runs), style, List.of(), null);
    }
    public static WordParagraph of(String text) {
        return new WordParagraph(UUID.randomUUID(), List.of(new WordRun(text, WordTextStyle.DEFAULT)), WordParagraphStyle.DEFAULT);
    }
    public static WordParagraph of(String text, WordTextStyle textStyle, WordParagraphStyle paragraphStyle) {
        return new WordParagraph(UUID.randomUUID(), List.of(new WordRun(text, textStyle)), paragraphStyle);
    }
    public String text() { StringBuilder b = new StringBuilder(); runs.forEach(r -> b.append(r.text())); return b.toString(); }
    @Override public String plainText() {
        StringBuilder b = new StringBuilder();
        for (WordInline r : runs) b.append(r instanceof WordObjectRun o ? o.object().plainText() : r.text());
        return b.toString();
    }
    public int length() { int n = 0; for (WordInline r : runs) n += r.length(); return n; }
    public WordParagraph withRuns(List<? extends WordInline> value) { return new WordParagraph(id,new ArrayList<WordInline>(value),style,bookmarks,sectionBreak); }
    public WordParagraph withStyle(WordParagraphStyle value) { return new WordParagraph(id,runs,value,bookmarks,sectionBreak); }
    public WordParagraph withId(UUID value) { return new WordParagraph(value,runs,style,bookmarks,sectionBreak); }
    public WordParagraph withBookmarks(List<String> value) { return new WordParagraph(id,runs,style,value,sectionBreak); }
    public WordParagraph withSectionBreak(WordPageSettings value) { return new WordParagraph(id,runs,style,bookmarks,value); }
    public WordTextStyle styleAt(int offset) {
        int cursor = 0;
        for (WordInline run : runs) { cursor += run.length(); if (offset < cursor) return run.style(); }
        return runs.isEmpty() ? WordTextStyle.DEFAULT : runs.getLast().style();
    }
    public WordObjectRun objectAt(int offset) {
        int cursor = 0;
        for (WordInline run : runs) {
            if (offset >= cursor && offset < cursor + run.length()) return run instanceof WordObjectRun o ? o : null;
            cursor += run.length();
        }
        return null;
    }
    public List<WordInline> slice(int start, int end) {
        if (start < 0 || end < start || end > length()) throw new IndexOutOfBoundsException();
        List<WordInline> result = new ArrayList<>(); int offset = 0;
        for (WordInline run : runs) {
            int from = Math.max(start-offset,0), to = Math.min(end-offset,run.length());
            if (from < to) result.add(run instanceof WordRun text ? new WordRun(text.text().substring(from,to),text.style()) : run);
            offset += run.length();
        }
        return result;
    }
    public boolean hasObjects(int start, int end) {
        for (WordInline run : slice(start,end)) if (run instanceof WordObjectRun) return true;
        return false;
    }
}
