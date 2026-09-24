package dtm.stools.component.panels.editor.word.editing;

import dtm.stools.component.panels.editor.word.model.*;
import java.util.*;

public final class WordReferences {
    public record Result(WordDocument document, int caret) {}
    private WordReferences() {}

    public static Result footnote(WordDocument document, int offset, String text, WordNote.Kind kind) {
        String prefix = kind == WordNote.Kind.FOOTNOTE ? "f" : "e";
        int n = 1; while (document.parts().notes().containsKey(prefix + n)) n++;
        String id = prefix + n;
        WordStyleSheet styles = document.styles();
        List<WordParagraph> paragraphs = new ArrayList<>();
        for (String line : WordDocument.normalize(text).split("\n",-1)) paragraphs.add(new WordParagraph(UUID.randomUUID(),List.of(new WordRun(line,styles.resolveText("FootnoteText"))),styles.resolveParagraph("FootnoteText")));
        WordDocument next = document.withParts(document.parts().withNote(new WordNote(id,kind,paragraphs)));
        next = next.insertObject(offset,new WordNoteReference(WordIds.next(),id));
        return new Result(next,offset+1);
    }
    public static WordDocument updateNote(WordDocument document, String id, String text) {
        WordNote note = document.parts().notes().get(id);
        if (note == null) throw new IllegalArgumentException("Nota inexistente");
        WordStyleSheet styles = document.styles();
        List<WordParagraph> paragraphs = new ArrayList<>();
        WordTextStyle style = note.paragraphs().getFirst().runs().isEmpty() ? styles.resolveText("FootnoteText") : note.paragraphs().getFirst().runs().getFirst().style();
        for (String line : WordDocument.normalize(text).split("\n",-1)) paragraphs.add(new WordParagraph(UUID.randomUUID(),List.of(new WordRun(line,style)),note.paragraphs().getFirst().style()));
        return document.withParts(document.parts().withNote(new WordNote(id,note.kind(),paragraphs)));
    }
    public static String uniqueBookmark(WordDocument document, String base) {
        String clean = base.replaceAll("[^\\p{L}\\p{N}_]","_");
        if (clean.isEmpty() || !Character.isLetter(clean.charAt(0))) clean = "M_" + clean;
        if (clean.length() > 36) clean = clean.substring(0,36);
        Set<String> used = new HashSet<>(); for (WordParagraph p : document.paragraphs()) used.addAll(p.bookmarks());
        String candidate = clean; int n = 2;
        while (used.contains(candidate)) candidate = clean + "_" + n++;
        return candidate;
    }
    public static WordDocument addBookmark(WordDocument document, int offset, String name) {
        if (name == null || !name.matches("[\\p{L}][\\p{L}\\p{N}_]{0,39}")) throw new IllegalArgumentException("Nome de indicador inválido (use letras, números e _; comece com letra)");
        for (WordParagraph p : document.paragraphs()) if (p.bookmarks().contains(name)) throw new IllegalArgumentException("Já existe um indicador com esse nome");
        return document.mapParagraphs(offset,offset,p -> { List<String> b = new ArrayList<>(p.bookmarks()); b.add(name); return p.withBookmarks(b); });
    }
    public static WordDocument removeBookmark(WordDocument document, String name) {
        return document.mapAllParagraphs(p -> p.bookmarks().contains(name) ? p.withBookmarks(p.bookmarks().stream().filter(b -> !b.equals(name)).toList()) : p);
    }
    public static List<String> bookmarks(WordDocument document) {
        List<String> result = new ArrayList<>();
        for (WordParagraph p : document.paragraphs()) for (String b : p.bookmarks()) if (!b.startsWith("_")) result.add(b);
        return result;
    }
    public static Result caption(WordDocument document, int offset, String label, String text) {
        int index = document.paragraphIndex(offset);
        int end = document.paragraphEnd(index);
        WordStyleSheet styles = document.styles();
        WordTextStyle style = styles.resolveText("Caption");
        String bookmark = uniqueBookmark(document,"Legenda_" + label);
        WordParagraph caption = new WordParagraph(UUID.randomUUID(),List.of(new WordRun(label + " ",style),new WordObjectRun(WordField.of(WordField.Kind.SEQ,label.replaceAll("\\s+","_")),style),
                new WordRun(text.isBlank() ? "" : ": " + text.replace('\n',' '),style)),styles.resolveParagraph("Caption"),List.of(bookmark),null);
        WordDocument next = document.insertBlocks(end,List.of(caption));
        return new Result(next,Math.min(next.length(),end+1));
    }
    public static Result crossReference(WordDocument document, int offset, String bookmark, boolean page, WordTextStyle style) {
        WordField field = WordField.of(page ? WordField.Kind.PAGE_REF : WordField.Kind.REF,bookmark);
        WordDocument next = document.insertObject(offset,field);
        return new Result(next,offset+1);
    }
    public static String headingBookmark(WordDocument document, int offset) {
        WordParagraph p = document.paragraphAt(offset);
        for (String b : p.bookmarks()) if (!b.startsWith("_")) return b;
        return null;
    }
}
