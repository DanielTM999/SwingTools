package dtm.stools.component.panels.editor.word.editing;

import dtm.stools.component.panels.editor.word.model.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;

public final class WordRevisions {
    public record Result(WordDocument document, int start, int end) {}
    public record Change(int start, int end, WordRevision revision, String text) {}
    private WordRevisions() {}

    public static WordRevision revision(WordRevision.Type type, String author) {
        return new WordRevision(type,author,Instant.now().truncatedTo(ChronoUnit.MINUTES));
    }
    public static Result markDeleted(WordDocument document, int start, int end, String author) {
        document.checkRange(start,end);
        if (start == end) return new Result(document,start,end);
        if (document.hasStructure(start,end) && hasBlocks(document,start,end)) {
            WordDocument next = document.delete(start,end);
            return new Result(next,start,start);
        }
        WordRevision deletion = revision(WordRevision.Type.DELETE,author);
        Map<UUID,WordParagraph> changed = new HashMap<>(); int removed = 0;
        for (int k = document.paragraphIndex(start); k <= document.paragraphIndex(end); k++) {
            WordParagraph p = document.paragraphs().get(k); int base = document.paragraphStart(k);
            int from = Math.max(0,start-base), to = Math.min(p.length(),end-base);
            if (from >= to) continue;
            List<WordInline> runs = new ArrayList<>(p.slice(0,from));
            for (WordInline run : p.slice(from,to)) {
                WordRevision r = run.style().revision();
                if (r != null && r.type() == WordRevision.Type.INSERT && r.author().equals(author)) { removed += run.length(); continue; }
                runs.add(r != null && r.type() == WordRevision.Type.DELETE ? run : run.withStyle(run.style().withRevision(deletion)));
            }
            runs.addAll(p.slice(to,p.length()));
            changed.put(p.id(),p.withRuns(runs));
        }
        return new Result(document.replaceParagraphs(changed),start,end-removed);
    }
    private static boolean hasBlocks(WordDocument document, int start, int end) {
        if (!document.sameContainer(start,end)) return true;
        int first = document.paragraphIndex(start), last = document.paragraphIndex(end);
        int[] a = document.pathOf(first), b = document.pathOf(last);
        if (b[b.length-1] - a[a.length-1] != last - first) return true;
        return false;
    }
    public static Result replace(WordDocument document, int start, int end, String text, WordTextStyle style, String author) {
        Result deleted = markDeleted(document,start,end,author);
        if (text.isEmpty()) return deleted;
        WordTextStyle inserted = style.withRevision(revision(WordRevision.Type.INSERT,author));
        String normalized = WordDocument.normalize(text);
        WordDocument next = deleted.document().replaceContent(deleted.end(),deleted.end(),normalized,inserted);
        return new Result(next,deleted.end(),deleted.end()+normalized.length());
    }
    public static WordDocument markInserted(WordDocument document, int start, int end, String author) {
        WordRevision insert = revision(WordRevision.Type.INSERT,author);
        return document.format(start,end,s -> s.withRevision(insert));
    }
    public static List<Change> changes(WordDocument document) {
        List<Change> result = new ArrayList<>();
        for (int k = 0; k < document.paragraphs().size(); k++) {
            int offset = document.paragraphStart(k);
            for (WordInline run : document.paragraphs().get(k).runs()) {
                WordRevision r = run.style().revision();
                if (r != null) {
                    Change last = result.isEmpty() ? null : result.getLast();
                    if (last != null && last.end() == offset && last.revision().equals(r)) result.set(result.size()-1,new Change(last.start(),offset+run.length(),r,last.text()+run.text()));
                    else result.add(new Change(offset,offset+run.length(),r,run.text()));
                }
                offset += run.length();
            }
        }
        return result;
    }
    public static WordDocument acceptAll(WordDocument document) { return resolve(document,0,document.length(),true); }
    public static WordDocument rejectAll(WordDocument document) { return resolve(document,0,document.length(),false); }
    public static WordDocument accept(WordDocument document, int start, int end) { return resolve(document,start,end,true); }
    public static WordDocument reject(WordDocument document, int start, int end) { return resolve(document,start,end,false); }
    private static WordDocument resolve(WordDocument document, int start, int end, boolean accept) {
        Function<WordInline,WordInline> decide = run -> {
            WordRevision r = run.style().revision();
            if (r == null) return run;
            boolean keep = (r.type() == WordRevision.Type.INSERT) == accept;
            return keep ? run.withStyle(run.style().withRevision(null)) : null;
        };
        Map<UUID,WordParagraph> changed = new HashMap<>();
        int first = document.paragraphIndex(start), last = document.paragraphIndex(end);
        for (int k = first; k <= last; k++) {
            WordParagraph p = document.paragraphs().get(k); int base = document.paragraphStart(k);
            int from = Math.max(0,start-base), to = Math.min(p.length(),Math.max(from,end-base));
            if (start == end) { from = 0; to = p.length(); }
            List<WordInline> runs = new ArrayList<>(p.slice(0,from)); boolean touched = false;
            for (WordInline run : p.slice(from,to)) {
                WordInline next = decide.apply(run);
                if (next != run) touched = true;
                if (next != null) runs.add(next);
            }
            runs.addAll(p.slice(to,p.length()));
            if (touched) changed.put(p.id(),p.withRuns(runs));
        }
        return document.replaceParagraphs(changed);
    }
}
