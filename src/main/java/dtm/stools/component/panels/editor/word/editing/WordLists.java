package dtm.stools.component.panels.editor.word.editing;

import dtm.stools.component.panels.editor.word.model.*;
import java.util.*;

public final class WordLists {
    private WordLists() {}

    public static WordDocument toggle(WordDocument document, int start, int end, boolean numbered) {
        int first = document.paragraphIndex(start), last = document.paragraphIndex(end > start ? end-1 : end);
        WordNumbering numbering = document.parts().numbering();
        boolean allSame = true; String existing = null;
        for (int i = first; i <= last; i++) {
            WordListRef ref = document.paragraphs().get(i).style().list();
            boolean matches = ref != null && numbering.get(ref.listId()).map(d -> d.bullet() != numbered).orElse(false);
            if (!matches) allSame = false; else if (existing == null) existing = ref.listId();
        }
        if (allSame) return clear(document,start,end);
        String id = existing;
        if (id == null) {
            WordParagraph previous = first > 0 ? document.paragraphs().get(first-1) : null;
            if (previous != null && previous.style().list() != null && numbering.get(previous.style().list().listId()).map(d -> d.bullet() != numbered).orElse(false)) id = previous.style().list().listId();
        }
        if (id == null) {
            id = numbering.nextId();
            numbering = numbering.with(numbered ? WordListDefinition.numbered(id) : WordListDefinition.bullets(id));
        }
        WordListDefinition definition = numbering.get(id).orElseThrow();
        String listId = id;
        WordDocument next = document.withParts(document.parts().withNumbering(numbering));
        return next.formatParagraphs(start,end,s -> {
            int level = s.list() == null ? 0 : s.list().level();
            WordListLevel l = definition.level(level);
            return s.withList(new WordListRef(listId,level)).withIndents(l.indent(),s.rightIndent(),-l.hanging());
        });
    }
    public static WordDocument clear(WordDocument document, int start, int end) {
        return document.formatParagraphs(start,end,s -> s.list() == null ? s : s.withList(null).withIndents(0,s.rightIndent(),0));
    }
    public static WordDocument indent(WordDocument document, int start, int end, int delta) {
        WordNumbering numbering = document.parts().numbering();
        return document.formatParagraphs(start,end,s -> {
            if (s.list() == null) return s.withIndents(Math.max(0,s.leftIndent() + delta*36),s.rightIndent(),s.firstLineIndent());
            int level = Math.max(0,Math.min(8,s.list().level()+delta));
            WordListLevel l = numbering.get(s.list().listId()).map(d -> d.level(level)).orElse(WordListDefinition.bullets("x").level(level));
            return s.withList(s.list().withLevel(level)).withIndents(l.indent(),s.rightIndent(),-l.hanging());
        });
    }
    public static WordDocument restart(WordDocument document, int offset, int startAt) {
        int index = document.paragraphIndex(offset);
        WordParagraph paragraph = document.paragraphs().get(index);
        WordListRef ref = paragraph.style().list();
        if (ref == null) throw new IllegalArgumentException("O parágrafo não pertence a uma lista");
        WordNumbering numbering = document.parts().numbering();
        WordListDefinition definition = numbering.get(ref.listId()).orElseThrow();
        String id = numbering.nextId();
        numbering = numbering.with(definition.restartedAs(id,Math.max(0,startAt)));
        Map<UUID,WordParagraph> changed = new HashMap<>();
        for (int i = index; i < document.paragraphs().size(); i++) {
            WordParagraph p = document.paragraphs().get(i);
            if (p.style().list() != null && p.style().list().listId().equals(ref.listId())) changed.put(p.id(),p.withStyle(p.style().withList(new WordListRef(id,p.style().list().level()))));
        }
        return document.withParts(document.parts().withNumbering(numbering)).replaceParagraphs(changed);
    }
    public static WordDocument continuePrevious(WordDocument document, int offset) {
        int index = document.paragraphIndex(offset);
        WordListRef ref = document.paragraphs().get(index).style().list();
        if (ref == null) throw new IllegalArgumentException("O parágrafo não pertence a uma lista");
        WordNumbering numbering = document.parts().numbering();
        boolean bullet = numbering.get(ref.listId()).map(WordListDefinition::bullet).orElse(true);
        String previous = null;
        for (int i = index-1; i >= 0; i--) {
            WordListRef r = document.paragraphs().get(i).style().list();
            if (r != null && !r.listId().equals(ref.listId()) && numbering.get(r.listId()).map(d -> d.bullet() == bullet).orElse(false)) { previous = r.listId(); break; }
        }
        if (previous == null) return document;
        String target = previous;
        Map<UUID,WordParagraph> changed = new HashMap<>();
        for (int i = index; i < document.paragraphs().size(); i++) {
            WordParagraph p = document.paragraphs().get(i);
            if (p.style().list() != null && p.style().list().listId().equals(ref.listId())) changed.put(p.id(),p.withStyle(p.style().withList(new WordListRef(target,p.style().list().level()))));
        }
        return document.replaceParagraphs(changed);
    }
}
