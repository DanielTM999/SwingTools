package dtm.stools.component.panels.editor.word.controller;

import dtm.stools.component.panels.editor.word.api.WordSelection;
import dtm.stools.component.panels.editor.word.api.WordSession;
import dtm.stools.component.panels.editor.word.model.WordDocument;
import dtm.stools.component.panels.editor.word.provider.WordSearchContext;
import dtm.stools.component.panels.editor.word.provider.WordSearchOptions;
import java.awt.Component;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class WordSearchController {
    private final WordSession session;

    public WordSearchController(WordSession session) { this.session = Objects.requireNonNull(session); }

    public Pattern pattern(String query, WordSearchOptions options) {
        if (query == null || query.isEmpty()) throw new IllegalArgumentException("Informe o texto a localizar");
        String core = options.regex() ? query : Pattern.quote(query);
        if (options.wholeWord()) core = "(?<![\\p{L}\\p{N}_])(?:" + core + ")(?![\\p{L}\\p{N}_])";
        try { return Pattern.compile(core,options.matchCase() ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE); }
        catch (PatternSyntaxException e) { throw new IllegalArgumentException("Expressão regular inválida: " + e.getDescription(),e); }
    }
    public List<WordSelection> matches(String query, WordSearchOptions options) {
        if (query == null || query.isEmpty()) return List.of();
        WordDocument document = session.getDocument();
        Matcher m = pattern(query,options).matcher(document.text());
        List<WordSelection> result = new ArrayList<>();
        int guard = 0;
        while (m.find() && guard++ < 100_000) {
            if (m.end() <= m.start()) continue;
            if (document.isBoundary(m.start()) && document.isBoundary(m.end()) && document.sameContainer(m.start(),m.end())) result.add(new WordSelection(m.start(),m.end()));
        }
        return result;
    }
    public Optional<WordSelection> find(String query, WordSearchOptions options, int from, boolean forward) {
        List<WordSelection> all = matches(query,options);
        if (all.isEmpty()) return Optional.empty();
        if (forward) { for (WordSelection s : all) if (s.start() >= from) return Optional.of(s); return Optional.of(all.getFirst()); }
        for (int i = all.size()-1; i >= 0; i--) if (all.get(i).end() <= from) return Optional.of(all.get(i));
        return Optional.of(all.getLast());
    }
    public int currentIndex(String query, WordSearchOptions options) {
        WordSelection current = session.getSelection();
        List<WordSelection> all = matches(query,options);
        for (int i = 0; i < all.size(); i++) if (all.get(i).start() == current.start() && all.get(i).end() == current.end()) return i;
        return -1;
    }
    public boolean replaceCurrent(String query, String replacement, WordSearchOptions options) {
        requireEditable();
        WordSelection current = session.getSelection();
        Optional<WordSelection> match = matches(query,options).stream().filter(s -> s.start() == current.start() && s.end() == current.end()).findFirst();
        if (match.isEmpty()) return false;
        String text = expand(query,replacement,options,match.get());
        int start = match.get().start(), end = match.get().end();
        session.execute("Substituir",d -> d.replace(start,end,text,d.styleAt(start)),new WordSelection(start+WordDocument.normalize(text).length(),start+WordDocument.normalize(text).length()));
        return true;
    }
    public int replaceAll(String query, String replacement, WordSearchOptions options) {
        requireEditable();
        List<WordSelection> all = matches(query,options);
        WordDocument document = session.getDocument();
        List<WordSelection> safe = all.stream().filter(s -> !document.hasStructure(s.start(),s.end())).toList();
        if (safe.isEmpty()) return 0;
        List<String> texts = new ArrayList<>(); for (WordSelection s : safe) texts.add(expand(query,replacement,options,s));
        session.execute("Substituir tudo",d -> {
            WordDocument next = d;
            for (int i = safe.size()-1; i >= 0; i--) { WordSelection s = safe.get(i); next = next.replace(s.start(),s.end(),texts.get(i),next.styleAt(s.start())); }
            return next;
        },new WordSelection(0,0));
        return safe.size();
    }
    private String expand(String query, String replacement, WordSearchOptions options, WordSelection match) {
        if (!options.regex()) return replacement;
        String text = session.getDocument().text();
        Matcher m = pattern(query,options).matcher(text);
        if (m.find(match.start()) && m.start() == match.start()) {
            StringBuilder b = new StringBuilder();
            try { m.appendReplacement(b,replacement); } catch (IllegalArgumentException | IndexOutOfBoundsException e) { throw new IllegalArgumentException("Substituição inválida: " + e.getMessage(),e); }
            return b.substring(match.start());
        }
        return replacement;
    }
    private void requireEditable() { if (session.isReadOnly()) throw new IllegalStateException("Document is read-only"); }

    public WordSearchContext context(Component owner, Runnable onClose) {
        String initial = "";
        WordSelection s = session.getSelection();
        if (!s.isEmpty() && s.end() - s.start() < 200) initial = session.getDocument().text().substring(s.start(),s.end()).replace('\n',' ').replace("￼","");
        String query = initial;
        return new WordSearchContext() {
            public Component owner() { return owner; }
            public String initialQuery() { return query; }
            public boolean readOnly() { return session.isReadOnly(); }
            public Optional<WordSelection> currentSelection() { return Optional.of(session.getSelection()); }
            public void validate(String q, WordSearchOptions o) { pattern(q,o); }
            public Optional<WordSelection> findNext(String q, WordSearchOptions o) { return select(find(q,o,session.getSelection().isEmpty() ? session.getSelection().caret() : session.getSelection().start()+1,true)); }
            public Optional<WordSelection> findPrevious(String q, WordSearchOptions o) { return select(find(q,o,session.getSelection().start(),false)); }
            public int count(String q, WordSearchOptions o) { return q == null || q.isEmpty() ? 0 : matches(q,o).size(); }
            public int currentIndex(String q, WordSearchOptions o) { return WordSearchController.this.currentIndex(q,o); }
            public boolean replace(String q, String r, WordSearchOptions o) {
                boolean replaced = replaceCurrent(q,r,o);
                findNext(q,o);
                return replaced;
            }
            public int replaceAll(String q, String r, WordSearchOptions o) { return WordSearchController.this.replaceAll(q,r,o); }
            public void closed() { onClose.run(); }
            private Optional<WordSelection> select(Optional<WordSelection> found) { found.ifPresent(f -> session.setSelection(f.start(),f.end())); return found; }
        };
    }
}
