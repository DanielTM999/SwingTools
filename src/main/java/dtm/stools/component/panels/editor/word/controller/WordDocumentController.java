package dtm.stools.component.panels.editor.word.controller;

import dtm.stools.component.panels.editor.word.api.*;
import dtm.stools.component.panels.editor.word.editing.*;
import dtm.stools.component.panels.editor.word.model.*;
import java.util.*;

public final class WordDocumentController {
    private final WordSession session;
    private final Map<String,WordDocument> buildingBlocks = new LinkedHashMap<>();

    public WordDocumentController(WordSession session) { this.session = session; }
    private void requireEditable() { if (session.isReadOnly()) throw new IllegalStateException("Document is read-only"); }
    private int[] range() { WordSelection s = session.getSelection(); return new int[]{s.start(),s.end()}; }

    public void applyStyle(String styleId) {
        requireEditable();
        WordDocument result = session.getDocument();
        for (int[] r : session.selectedRanges(result)) result = WordStyles.apply(result,r[0],r[1],styleId);
        WordDocument next = result;
        session.execute("Estilo",d -> next);
    }
    public void updateStyleFromSelection(String styleId) {
        requireEditable();
        WordDocument d = session.getDocument();
        WordNamedStyle current = d.styles().get(styleId).orElseThrow(() -> new IllegalArgumentException("Estilo inexistente"));
        WordNamedStyle updated = WordStyles.fromParagraph(current.id(),current.name(),d.paragraphAt(session.getSelection().start()),current.basedOn());
        session.execute("Atualizar estilo",doc -> WordStyles.update(doc,updated));
    }
    public void updateStyle(WordNamedStyle style) { requireEditable(); session.execute("Modificar estilo",d -> WordStyles.update(d,style)); }
    public String createStyle(String name) {
        requireEditable();
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Informe o nome do estilo");
        WordDocument d = session.getDocument();
        String id = name.replaceAll("[^\\p{L}\\p{N}]",""); if (id.isEmpty()) id = "Estilo";
        String unique = id; int n = 2; while (d.styles().get(unique).isPresent()) unique = id + n++;
        WordNamedStyle style = WordStyles.fromParagraph(unique,name.strip(),d.paragraphAt(session.getSelection().start()),WordStyleSheet.NORMAL);
        String finalId = unique;
        session.execute("Criar estilo",doc -> WordStyles.apply(doc.withParts(doc.parts().withStyles(doc.styles().with(style))),range()[0],range()[1],finalId));
        return unique;
    }

    public void toggleList(boolean numbered) { requireEditable(); int[] r = range(); session.execute(numbered ? "Numeração" : "Marcadores",d -> WordLists.toggle(d,r[0],r[1],numbered)); }
    public void indent(int delta) { requireEditable(); int[] r = range(); session.execute(delta > 0 ? "Aumentar recuo" : "Diminuir recuo",d -> WordLists.indent(d,r[0],r[1],delta)); }
    public void restartNumbering(int start) { requireEditable(); int caret = session.getSelection().start(); session.execute("Reiniciar numeração",d -> WordLists.restart(d,caret,start)); }
    public void continueNumbering() { requireEditable(); int caret = session.getSelection().start(); session.execute("Continuar numeração",d -> WordLists.continuePrevious(d,caret)); }

    public WordPageSettings sectionSettings(int offset) {
        WordDocument d = session.getDocument();
        for (int i = d.paragraphIndex(offset); i < d.paragraphs().size(); i++) {
            int[] path = d.pathOf(i);
            if (path.length == 1 && d.paragraphs().get(i).sectionBreak() != null) return d.paragraphs().get(i).sectionBreak();
        }
        return d.pageSettings();
    }
    public void setSectionSettings(WordPageSettings settings) {
        requireEditable(); Objects.requireNonNull(settings);
        WordDocument d = session.getDocument(); int caret = session.getSelection().start();
        for (int i = d.paragraphIndex(caret); i < d.paragraphs().size(); i++) {
            WordParagraph p = d.paragraphs().get(i);
            if (d.pathOf(i).length == 1 && p.sectionBreak() != null) { session.execute("Configurar página",doc -> doc.replaceParagraphs(Map.of(p.id(),p.withSectionBreak(settings)))); return; }
        }
        session.execute("Configurar página",doc -> doc.withPageSettings(settings));
    }
    public void insertSectionBreak() {
        requireEditable();
        WordDocument d = session.getDocument(); int caret = session.getSelection().start();
        if (d.depth(caret) > 0) throw new IllegalStateException("Quebras de seção não podem ficar dentro de tabelas");
        WordPageSettings current = sectionSettings(caret);
        WordDocument split = d.replaceContent(caret,caret,"\n",session.getInsertionStyle());
        int index = split.paragraphIndex(caret);
        WordParagraph before = split.paragraphs().get(index);
        WordDocument next = split.replaceParagraphs(Map.of(before.id(),before.withSectionBreak(current)));
        session.execute("Quebra de seção",doc -> next,new WordSelection(caret+1,caret+1));
    }
    public void insertPageBreak() { requireEditable(); session.insertObject(WordBreak.of(WordBreak.Kind.PAGE)); }
    public void insertColumnBreak() { requireEditable(); session.insertObject(WordBreak.of(WordBreak.Kind.COLUMN)); }

    public void setHeaderFooter(WordHeaders.Kind kind, List<? extends WordBlock> blocks) {
        requireEditable();
        session.execute("Cabeçalho e rodapé",d -> d.withParts(d.parts().withHeaders(d.parts().headers().with(kind,blocks))));
    }
    public void setHeaderFooterText(WordHeaders.Kind kind, String text, WordParagraphStyle.Alignment alignment, boolean pageNumber) {
        WordDocument d = session.getDocument();
        String styleId = kind.name().endsWith("HEADER") ? "Header" : "Footer";
        WordTextStyle style = d.styles().resolveText(styleId).withSize(9).withColor(0x595959);
        List<WordBlock> blocks = new ArrayList<>();
        String[] lines = WordDocument.normalize(text == null ? "" : text).split("\n",-1);
        for (int i = 0; i < lines.length; i++) {
            List<WordInline> runs = new ArrayList<>();
            if (!lines[i].isEmpty()) runs.add(new WordRun(lines[i],style));
            if (pageNumber && i == lines.length-1) { runs.add(new WordRun((lines[i].isEmpty() ? "" : "  •  ") + "Página ",style)); runs.add(new WordObjectRun(WordField.of(WordField.Kind.PAGE,""),style)); runs.add(new WordRun(" de ",style)); runs.add(new WordObjectRun(WordField.of(WordField.Kind.NUM_PAGES,""),style)); }
            blocks.add(new WordParagraph(UUID.randomUUID(),runs,d.styles().resolveParagraph(styleId).withAlignment(alignment)));
        }
        boolean empty = (text == null || text.isBlank()) && !pageNumber;
        setHeaderFooter(kind,empty ? List.of() : blocks);
    }
    public void setHeaderOptions(boolean differentFirst, boolean differentOddEven) {
        requireEditable();
        session.execute("Opções de cabeçalho",d -> d.withParts(d.parts().withHeaders(d.parts().headers().withOptions(differentFirst,differentOddEven))));
    }

    public void insertFootnote(String text, WordNote.Kind kind) {
        requireEditable();
        int caret = session.getSelection().end();
        WordReferences.Result result = WordReferences.footnote(session.getDocument(),caret,text,kind);
        session.execute(kind == WordNote.Kind.FOOTNOTE ? "Nota de rodapé" : "Nota de fim",d -> result.document(),new WordSelection(result.caret(),result.caret()));
    }
    public void updateNote(String noteId, String text) { requireEditable(); session.execute("Editar nota",d -> WordReferences.updateNote(d,noteId,text)); }
    public void insertCaption(String label, String text) {
        requireEditable();
        WordReferences.Result result = WordReferences.caption(session.getDocument(),session.getSelection().end(),label,text);
        session.execute("Inserir legenda",d -> result.document(),new WordSelection(result.caret(),result.caret()));
    }
    public void addBookmark(String name) { requireEditable(); int caret = session.getSelection().start(); session.execute("Indicador",d -> WordReferences.addBookmark(d,caret,name)); }
    public void removeBookmark(String name) { requireEditable(); session.execute("Remover indicador",d -> WordReferences.removeBookmark(d,name)); }
    public List<String> bookmarks() { return WordReferences.bookmarks(session.getDocument()); }
    public void insertCrossReference(String bookmark, boolean page) {
        requireEditable();
        if (!bookmarks().contains(bookmark)) throw new IllegalArgumentException("Indicador inexistente: " + bookmark);
        session.insertObject(WordField.of(page ? WordField.Kind.PAGE_REF : WordField.Kind.REF,bookmark));
    }
    public void insertTableOfContents() { requireEditable(); session.insertBlock(WordTableOfContents.create()); }
    public void insertField(WordField.Kind kind, String argument) { requireEditable(); session.insertObject(WordField.of(kind,argument)); }
    public Optional<Integer> navigateTo(String target, Map<String,Integer> bookmarkPages) {
        WordDocument d = session.getDocument();
        if (target.startsWith("#p:")) {
            try { int i = d.paragraphIndexOf(UUID.fromString(target.substring(3))); if (i >= 0) { session.setSelection(d.paragraphStart(i),d.paragraphStart(i)); return Optional.of(i); } } catch (IllegalArgumentException ignored) {}
            return Optional.empty();
        }
        String name = target.startsWith("#") ? target.substring(1) : target;
        for (int i = 0; i < d.paragraphs().size(); i++) if (d.paragraphs().get(i).bookmarks().contains(name)) { session.setSelection(d.paragraphStart(i),d.paragraphStart(i)); return Optional.of(i); }
        return Optional.empty();
    }

    public void fillTemplate(Map<String,String> values) {
        requireEditable(); Map<String,String> snapshot = Map.copyOf(values);
        session.execute("Preencher modelo",d -> WordTemplates.fill(d,snapshot),new WordSelection(0,0));
    }
    public Map<String,WordDocument> buildingBlocks() { return Collections.unmodifiableMap(buildingBlocks); }
    public void saveBuildingBlock(String name) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Informe o nome do bloco");
        WordSelection s = session.getSelection();
        if (s.isEmpty()) throw new IllegalStateException("Selecione o conteúdo a reutilizar");
        buildingBlocks.put(name.strip(),session.getDocument().fragment(s.start(),s.end()));
    }
    public void addBuildingBlock(String name, WordDocument fragment) { buildingBlocks.put(Objects.requireNonNull(name),Objects.requireNonNull(fragment)); }
    public void removeBuildingBlock(String name) { buildingBlocks.remove(name); }
    public void insertBuildingBlock(String name) {
        requireEditable();
        WordDocument fragment = buildingBlocks.get(name);
        if (fragment == null) throw new IllegalArgumentException("Bloco inexistente: " + name);
        session.replaceSelection(fragment);
    }
}
