package dtm.stools.component.panels.editor.word.controller;

import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.editing.WordCompare;
import dtm.stools.component.panels.editor.word.editing.WordRevisions;
import dtm.stools.component.panels.editor.word.model.*;
import java.time.Instant;
import java.util.*;

public final class WordReviewController {
    public record CommentThread(WordComment comment, List<WordComment> replies, String anchor, int start, int end) {}
    private final WordSession session;

    public WordReviewController(WordSession session) { this.session = session; }
    private void requireEditable() { if (session.isReadOnly()) throw new IllegalStateException("Document is read-only"); }

    public String addComment(String text) {
        requireEditable();
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Escreva o comentário");
        WordSelection s = session.getSelection();
        if (s.isEmpty()) throw new IllegalStateException("Selecione o trecho a comentar");
        String id = WordIds.next();
        WordComment comment = new WordComment(id,session.getAuthor(),null,Instant.now(),text.strip(),null,false);
        WordDocument result = session.getDocument();
        for (int[] r : session.selectedRanges(result)) result = result.format(r[0],r[1],st -> st.withComment(id));
        WordDocument next = result.withParts(result.parts().withComment(comment));
        session.execute("Novo comentário",d -> next);
        return id;
    }
    public String reply(String parentId, String text) {
        requireEditable();
        if (text == null || text.isBlank()) throw new IllegalArgumentException("Escreva a resposta");
        WordDocument d = session.getDocument();
        if (d.parts().comments().stream().noneMatch(c -> c.id().equals(parentId))) throw new IllegalArgumentException("Comentário inexistente");
        String id = WordIds.next();
        WordComment reply = new WordComment(id,session.getAuthor(),null,Instant.now(),text.strip(),parentId,false);
        session.execute("Responder comentário",doc -> doc.withParts(doc.parts().withComment(reply)));
        return id;
    }
    public void resolve(String id, boolean resolved) {
        requireEditable();
        session.execute(resolved ? "Resolver comentário" : "Reabrir comentário",d -> {
            WordParts parts = d.parts();
            for (WordComment c : d.parts().comments()) if (c.id().equals(id) || id.equals(c.parentId())) parts = parts.withComment(c.withResolved(resolved));
            return d.withParts(parts);
        });
    }
    public void editComment(String id, String text) {
        requireEditable();
        session.execute("Editar comentário",d -> d.withParts(d.parts().withComment(d.parts().comments().stream().filter(c -> c.id().equals(id)).findFirst().orElseThrow().withText(text))));
    }
    public void deleteComment(String id) {
        requireEditable();
        session.execute("Excluir comentário",d -> d.format(0,d.length(),s -> s.comments().contains(id) ? s.withoutComment(id) : s).withParts(d.parts().withoutComment(id)));
    }
    public List<CommentThread> threads() {
        WordDocument d = session.getDocument();
        Map<String,int[]> ranges = new HashMap<>();
        for (int k = 0; k < d.paragraphs().size(); k++) {
            int offset = d.paragraphStart(k);
            for (WordInline inline : d.paragraphs().get(k).runs()) {
                for (String id : inline.style().comments()) ranges.merge(id,new int[]{offset,offset+inline.length()},(a,b) -> new int[]{Math.min(a[0],b[0]),Math.max(a[1],b[1])});
                offset += inline.length();
            }
        }
        List<CommentThread> result = new ArrayList<>();
        for (WordComment c : d.parts().comments()) {
            if (c.isReply()) continue;
            int[] r = ranges.getOrDefault(c.id(),new int[]{0,0});
            String anchor = d.text().substring(r[0],r[1]).replace('\n',' ').replace("￼","");
            if (anchor.length() > 80) anchor = anchor.substring(0,80) + "…";
            List<WordComment> replies = d.parts().comments().stream().filter(x -> c.id().equals(x.parentId())).toList();
            result.add(new CommentThread(c,replies,anchor,r[0],r[1]));
        }
        return result;
    }
    public void select(CommentThread thread) { session.setSelection(thread.start(),thread.end()); }

    public void setTrackChanges(boolean value) { session.setTrackChanges(value); }
    public List<WordRevisions.Change> changes() { return WordRevisions.changes(session.getDocument()); }
    public void acceptAtSelection() { requireEditable(); WordSelection s = session.getSelection(); session.execute("Aceitar alteração",d -> WordRevisions.accept(d,s.start(),s.end())); }
    public void rejectAtSelection() { requireEditable(); WordSelection s = session.getSelection(); session.execute("Rejeitar alteração",d -> WordRevisions.reject(d,s.start(),s.end())); }
    public void acceptAll() { requireEditable(); session.execute("Aceitar todas",WordRevisions::acceptAll,new WordSelection(0,0)); }
    public void rejectAll() { requireEditable(); session.execute("Rejeitar todas",WordRevisions::rejectAll,new WordSelection(0,0)); }
    public Optional<WordRevisions.Change> nextChange() {
        int caret = session.getSelection().end();
        List<WordRevisions.Change> all = changes();
        Optional<WordRevisions.Change> next = all.stream().filter(c -> c.start() >= caret).findFirst().or(() -> all.stream().findFirst());
        next.ifPresent(c -> { if (session.getDocument().sameContainer(c.start(),c.end())) session.setSelection(c.start(),c.end()); });
        return next;
    }
    public WordDocument compare(WordDocument original, WordDocument revised) { return WordCompare.compare(original,revised,session.getAuthor()); }
}
