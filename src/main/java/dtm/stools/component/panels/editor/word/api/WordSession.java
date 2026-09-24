package dtm.stools.component.panels.editor.word.api;

import dtm.stools.component.panels.editor.word.command.EditorCommand;
import dtm.stools.component.panels.editor.word.editing.WordRevisions;
import dtm.stools.component.panels.editor.word.editing.WordTableEditing;
import dtm.stools.component.panels.editor.word.model.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class WordSession {
    public enum Change { DOCUMENT, SELECTION, STATE }
    public record Event(Change change,long revision,String label) {}
    private record State(WordDocument document,WordContentSelection selection) {}
    private final List<Consumer<Event>> listeners=new CopyOnWriteArrayList<>();
    private final Deque<State> undo=new ArrayDeque<>(),redo=new ArrayDeque<>();
    private WordDocument document=WordDocument.empty(),saved=document;
    private WordContentSelection selection=new WordSelection(0,0);
    private WordTextStyle insertionStyle=WordTextStyle.DEFAULT;
    private long revision;
    private int historyLimit=200;
    private boolean readOnly,trackChanges;
    private String author=Optional.ofNullable(System.getProperty("user.name")).filter(s->!s.isBlank()).orElse("Autor");
    private Consumer<Throwable> errorHandler=error->System.getLogger(WordSession.class.getName()).log(System.Logger.Level.WARNING,"Session listener failed",error);

    public WordDocument getDocument() { return document; }
    public WordSelection getSelection() { return selection.range(); }
    public WordContentSelection getContentSelection() { return selection; }
    public Optional<WordCellSelection> getCellSelection() { return selection instanceof WordCellSelection c ? Optional.of(c) : Optional.empty(); }
    public Optional<WordObjectSelection> getObjectSelection() { return selection instanceof WordObjectSelection o ? Optional.of(o) : Optional.empty(); }
    public Optional<WordInlineObject> getSelectedObject() { return getObjectSelection().map(o->document.objectAt(o.offset())).map(WordObjectRun::object); }
    public long getRevision() { return revision; }
    public boolean isDirty() { return !document.equals(saved); }
    public boolean isReadOnly() { return readOnly; }
    public boolean canUndo() { return !readOnly && !undo.isEmpty(); }
    public boolean canRedo() { return !readOnly && !redo.isEmpty(); }
    public WordTextStyle getInsertionStyle() { return insertionStyle; }
    public boolean isTrackChanges() { return trackChanges; }
    public String getAuthor() { return author; }
    public void setAuthor(String value) { if(value==null||value.isBlank())throw new IllegalArgumentException("Author is required"); author=value.strip(); fire(Change.STATE,"Author"); }
    public void setTrackChanges(boolean value) { trackChanges=value; fire(Change.STATE,"Track changes"); }
    public void setErrorHandler(Consumer<Throwable> handler) { errorHandler=Objects.requireNonNull(handler); }
    public void setReadOnly(boolean value) { readOnly=value; fire(Change.STATE,"Read only"); }
    public void setHistoryLimit(int value) {
        if(value<0) throw new IllegalArgumentException("Negative history limit");
        historyLimit=value; trim();
    }
    public ProviderRegistration addListener(Consumer<Event> listener) {
        listeners.add(Objects.requireNonNull(listener)); return ()->listeners.remove(listener);
    }
    public void setSelection(int anchor,int caret) {
        document.checkRange(Math.min(anchor,caret),Math.max(anchor,caret));
        selection=normalize(document,anchor,caret); updateInsertionStyle(); fire(Change.SELECTION,"Selection");
    }
    public void selectObject(int offset) {
        WordObjectRun run=document.objectAt(offset);
        if(run==null) throw new IllegalArgumentException("No object at "+offset);
        selection=new WordObjectSelection(offset,run.object().id()); updateInsertionStyle(); fire(Change.SELECTION,"Selection");
    }
    public void selectCells(UUID tableId,int anchorRow,int anchorColumn,int focusRow,int focusColumn) {
        selection=cellSelection(document,tableId,anchorRow,anchorColumn,focusRow,focusColumn); updateInsertionStyle(); fire(Change.SELECTION,"Selection");
    }
    public static WordContentSelection normalize(WordDocument d,int anchor,int caret) {
        if(anchor!=caret&&!d.sameContainer(anchor,caret)) {
            int[] pa=d.pathOf(d.paragraphIndex(anchor)),pc=d.pathOf(d.paragraphIndex(caret));
            int ta=(pa.length-1)/3,tc=(pc.length-1)/3,common=0;
            while(common<ta&&common<tc&&pa[3*common]==pc[3*common]&&pa[3*common+1]==pc[3*common+1]&&pa[3*common+2]==pc[3*common+2]) common++;
            if(common<ta&&common<tc&&pa[3*common]==pc[3*common]) {
                WordTableLocation a=d.tableAt(anchor,common).orElseThrow(),c=d.tableAt(caret,common).orElseThrow();
                return cellSelection(d,a.table().id(),a.row(),a.gridColumn(),c.row(),c.gridColumn()+c.cellValue().gridSpan()-1);
            }
            boolean forward=anchor<caret;
            if(ta>common){int[] r=d.tableRange(d.tableAt(anchor,common).orElseThrow().table().id());anchor=forward?r[0]:r[1];}
            if(tc>common){int[] r=d.tableRange(d.tableAt(caret,common).orElseThrow().table().id());caret=forward?r[1]:r[0];}
            if(!d.sameContainer(anchor,caret)) return normalize(d,anchor,caret);
        }
        if(Math.abs(caret-anchor)==1) { WordObjectRun run=d.objectAt(Math.min(anchor,caret)); if(run!=null&&!run.object().textual()) return new WordObjectSelection(Math.min(anchor,caret),run.object().id()); }
        return new WordSelection(anchor,caret);
    }
    private static WordCellSelection cellSelection(WordDocument d,UUID tableId,int anchorRow,int anchorColumn,int focusRow,int focusColumn) {
        WordTable table=d.findTable(tableId).orElseThrow(()->new IllegalArgumentException("Unknown table"));
        int rows=table.rows().size()-1,columns=table.gridColumns()-1;
        anchorRow=clamp(anchorRow,rows);focusRow=clamp(focusRow,rows);anchorColumn=clamp(anchorColumn,columns);focusColumn=clamp(focusColumn,columns);
        int start=Integer.MAX_VALUE,end=0;
        for(var ref:WordTableEditing.cells(table,Math.min(anchorRow,focusRow),Math.min(anchorColumn,focusColumn),Math.max(anchorRow,focusRow),Math.max(anchorColumn,focusColumn))){
            int[] range=d.cellRange(tableId,ref.row(),ref.cell());start=Math.min(start,range[0]);end=Math.max(end,range[1]);
        }
        if(start>end) start=end;
        return new WordCellSelection(tableId,anchorRow,anchorColumn,focusRow,focusColumn,new WordSelection(start,end));
    }
    private static int clamp(int v,int max){return Math.max(0,Math.min(max,v));}
    private void updateInsertionStyle() {
        int caret=selection.range().caret();int index=document.paragraphIndex(caret);WordParagraph p=document.paragraphs().get(index);int local=caret-document.paragraphStart(index);
        WordTextStyle style=p.length()==0?document.styles().resolveText(p.style().styleId()):p.styleAt(Math.max(0,local-1));
        insertionStyle=style.withLink(null).withRevision(null).withComments(List.of());
    }
    public void setInsertionStyle(WordTextStyle value) { insertionStyle=Objects.requireNonNull(value); fire(Change.STATE,"Insertion style"); }
    public void load(WordDocument value) {
        document=Objects.requireNonNull(value); saved=value; selection=new WordSelection(0,0);
        updateInsertionStyle(); undo.clear(); redo.clear(); revision++; fire(Change.DOCUMENT,"Open");
    }
    public void markSaved(WordDocument snapshot) { saved=Objects.requireNonNull(snapshot); fire(Change.STATE,"Saved"); }
    public void execute(EditorCommand<WordDocument> command) {
        execute(command.label(),command::apply,selection.range());
    }
    public void execute(String label,UnaryOperator<WordDocument> operation,WordSelection nextSelection) {
        commit(label,operation,d->normalize(d,nextSelection.anchor(),nextSelection.caret()),nextSelection);
    }
    public void execute(String label,UnaryOperator<WordDocument> operation) {
        commit(label,operation,d->clampSelection(d,selection),null);
    }
    public void executeWithSelection(String label,UnaryOperator<WordDocument> operation,java.util.function.Function<WordDocument,WordContentSelection> nextSelection) {
        commit(label,operation,nextSelection,null);
    }
    private void commit(String label,UnaryOperator<WordDocument> operation,java.util.function.Function<WordDocument,WordContentSelection> selector,WordSelection check) {
        requireEditable(); WordDocument next=Objects.requireNonNull(operation.apply(document));
        if(check!=null) next.checkRange(check.start(),check.end());
        WordContentSelection nextSelection=Objects.requireNonNull(selector.apply(next));
        next.checkRange(nextSelection.range().start(),nextSelection.range().end());
        if(next.equals(document)) return;
        undo.addLast(new State(document,selection)); trim(); redo.clear();
        document=next; selection=nextSelection; updateInsertionStyleKeepingFormat(label); revision++; fire(Change.DOCUMENT,label);
    }
    private void updateInsertionStyleKeepingFormat(String label) { if(!"Typing".equals(label)) updateInsertionStyle(); }
    private static WordContentSelection clampSelection(WordDocument d,WordContentSelection current) {
        WordSelection r=current.range();int a=Math.min(r.anchor(),d.length()),c=Math.min(r.caret(),d.length());
        if(current instanceof WordObjectSelection o&&d.objectAt(o.offset())!=null&&d.objectAt(o.offset()).object().id().equals(o.objectId())) return o;
        if(current instanceof WordCellSelection cells&&d.findTable(cells.tableId()).isPresent()) return cellSelection(d,cells.tableId(),cells.anchorRow(),cells.anchorColumn(),cells.focusRow(),cells.focusColumn());
        if(!d.isBoundary(a))a=d.previousBoundary(a); if(!d.isBoundary(c))c=d.previousBoundary(c);
        return normalize(d,a,c);
    }
    public void replaceSelection(String text) {
        requireEditable();
        String normalized=WordDocument.normalize(text);
        if(selection instanceof WordCellSelection cells) {
            WordDocument cleared=clearCells(document,cells);
            WordTable table=cleared.findTable(cells.tableId()).orElseThrow();int row=cells.firstRow();int cell=table.rows().get(row).cellAt(cells.firstColumn());
            int start=cleared.cellRange(cells.tableId(),row,cell)[0];
            WordDocument next=cleared.replaceContent(start,start,normalized,trackChanges?insertionStyle.withRevision(WordRevisions.revision(WordRevision.Type.INSERT,author)):insertionStyle);
            int caret=start+normalized.length();
            execute("Typing",d->next,new WordSelection(caret,caret));return;
        }
        int start=selection.range().start(),end=selection.range().end();
        if(trackChanges) {
            WordRevisions.Result result=WordRevisions.replace(document,start,end,normalized,insertionStyle,author);
            int caret=normalized.isEmpty()?result.start():result.end();
            execute("Typing",d->result.document(),new WordSelection(caret,caret));return;
        }
        execute("Typing",d->d.replaceContent(start,end,normalized,insertionStyle),new WordSelection(start+normalized.length(),start+normalized.length()));
    }
    public void deleteSelection(boolean forward) {
        requireEditable();
        if(selection instanceof WordCellSelection cells){WordDocument next=clearCells(document,cells);execute("Limpar células",d->next);return;}
        int start=selection.range().start(),end=selection.range().end();
        if(start==end) return;
        if(trackChanges) {
            WordRevisions.Result result=WordRevisions.markDeleted(document,start,end,author);
            int caret=forward?result.end():result.start();
            execute("Excluir",d->result.document(),new WordSelection(caret,caret));return;
        }
        execute("Excluir",d->d.delete(start,end),new WordSelection(start,start));
    }
    public void insertParagraphBreak() {
        requireEditable();
        if(selection instanceof WordCellSelection) { replaceSelection("\n"); return; }
        int start=selection.range().start(),end=selection.range().end();
        int index=document.paragraphIndex(start);WordParagraph p=document.paragraphs().get(index);
        if(start==end&&p.length()==0&&p.style().list()!=null) { execute("Encerrar lista",d->d.formatParagraphs(start,start,s->s.withList(null).withIndents(0,s.rightIndent(),0))); return; }
        boolean atEnd=end==document.paragraphEnd(document.paragraphIndex(end));
        String nextStyle=document.styles().nextStyle(p.style().styleId());
        WordDocument base=trackChanges?WordRevisions.markDeleted(document,start,end,author).document():document;
        int at=trackChanges?WordRevisions.markDeleted(document,start,end,author).end():start;
        WordDocument next=trackChanges?base.replaceContent(at,at,"\n",insertionStyle):base.replaceContent(start,end,"\n",insertionStyle);
        int caret=(trackChanges?at:start)+1;
        if(atEnd&&nextStyle!=null&&!Objects.equals(nextStyle,p.style().styleId())) {
            WordParagraphStyle resolved=next.styles().resolveParagraph(nextStyle).withList(p.style().list()==null?null:p.style().list());
            WordDocument styled=next.formatParagraphs(caret,caret,s->resolved);
            WordTextStyle text=styled.styles().resolveText(nextStyle);
            execute("Novo parágrafo",d->styled,new WordSelection(caret,caret));insertionStyle=text;fire(Change.STATE,"Insertion style");return;
        }
        WordDocument result=next;
        WordTextStyle keep=insertionStyle;
        execute("Novo parágrafo",d->result,new WordSelection(caret,caret));insertionStyle=keep;
    }
    public void replaceSelection(WordDocument fragment) {
        requireEditable();
        if(selection instanceof WordCellSelection cells){
            WordDocument cleared=clearCells(document,cells);WordTable table=cleared.findTable(cells.tableId()).orElseThrow();
            int start=cleared.cellRange(cells.tableId(),cells.firstRow(),table.rows().get(cells.firstRow()).cellAt(cells.firstColumn()))[0];
            WordDocument next=cleared.replace(start,start,fragment);int caret=next.length()-(cleared.length()-start);
            execute("Colar",d->next,new WordSelection(caret,caret));return;
        }
        int start=selection.range().start(),end=selection.range().end();
        WordDocument base=document;int at=start,tail=end;
        if(trackChanges){WordRevisions.Result deleted=WordRevisions.markDeleted(document,start,end,author);base=deleted.document();at=deleted.end();tail=deleted.end();}
        WordDocument next=base.replace(at,tail,fragment);int caret=next.length()-(base.length()-tail);
        if(trackChanges) next=WordRevisions.markInserted(next,at,caret,author);
        WordDocument result=next;
        execute("Colar",d->result,new WordSelection(caret,caret));
    }
    public void insertObject(WordInlineObject object) {
        requireEditable();Objects.requireNonNull(object);
        int start=selection.range().start(),end=selection.range().end();
        WordDocument base=start==end||selection instanceof WordCellSelection?document:document.delete(start,end);
        WordDocument next=base.insertObject(start,object);
        if(trackChanges) next=WordRevisions.markInserted(next,start,start+1,author);
        WordDocument result=next;
        executeWithSelection("Inserir objeto",d->result,d->object.textual()?new WordSelection(start+1,start+1):new WordObjectSelection(start,object.id()));
    }
    public void replaceObject(int offset,WordInlineObject object,String label) {
        requireEditable();
        executeWithSelection(label,d->d.replaceObject(offset,object),d->object.textual()?clampSelection(d,selection):new WordObjectSelection(offset,object.id()));
    }
    public void insertBlock(WordBlock block) {
        requireEditable();
        int start=selection.range().start(),end=selection.range().end();
        WordDocument base=start==end||selection instanceof WordCellSelection?document:document.delete(start,end);
        WordDocument next=base.insertBlocks(start,List.of(block));
        int caret=block instanceof WordTable t?next.tableRange(t.id())[0]:Math.min(next.length(),start);
        execute("Inserir",d->next,new WordSelection(caret,caret));
    }
    public void formatSelection(UnaryOperator<WordTextStyle> operation) {
        requireEditable(); WordTextStyle next=Objects.requireNonNull(operation.apply(insertionStyle));
        WordDocument result=document;
        for(int[] range:selectedRanges(document)) result=result.format(range[0],range[1],operation);
        WordDocument finalResult=result;
        execute("Format",d->finalResult);
        insertionStyle=next; fire(Change.STATE,"Format");
    }
    public void formatParagraphs(UnaryOperator<WordParagraphStyle> operation) {
        requireEditable();
        WordDocument result=document;
        for(int[] range:selectedRanges(document)) result=result.formatParagraphs(range[0],range[1],operation);
        WordDocument finalResult=result;
        execute("Paragraph",d->finalResult);
    }
    public List<int[]> selectedRanges(WordDocument d) {
        if(selection instanceof WordCellSelection cells) {
            WordTable table=d.findTable(cells.tableId()).orElseThrow();List<int[]> ranges=new ArrayList<>();
            for(var ref:WordTableEditing.cells(table,cells.firstRow(),cells.firstColumn(),cells.lastRow(),cells.lastColumn())) ranges.add(d.cellRange(cells.tableId(),ref.row(),ref.cell()));
            return ranges;
        }
        return List.<int[]>of(new int[]{selection.range().start(),selection.range().end()});
    }
    public WordDocument clearCells(WordDocument d,WordCellSelection cells) {
        return d.updateTable(cells.tableId(),t->WordTableEditing.clear(t,cells.firstRow(),cells.firstColumn(),cells.lastRow(),cells.lastColumn()));
    }
    public void undo() { requireEditable(); if(!undo.isEmpty()) { redo.addLast(new State(document,selection)); restore(undo.removeLast(),"Undo"); } }
    public void redo() { requireEditable(); if(!redo.isEmpty()) { undo.addLast(new State(document,selection)); restore(redo.removeLast(),"Redo"); } }
    private void restore(State state,String label) {
        document=state.document; selection=state.selection; updateInsertionStyle(); revision++; fire(Change.DOCUMENT,label);
    }
    private void trim() { while(undo.size()>historyLimit) undo.removeFirst(); while(redo.size()>historyLimit) redo.removeFirst(); }
    private void requireEditable() { if(readOnly) throw new IllegalStateException("Document is read-only"); }
    private void fire(Change change,String label) {
        Event event=new Event(change,revision,label);
        for(Consumer<Event> listener:listeners) {
            try { listener.accept(event); }
            catch(RuntimeException error) { errorHandler.accept(error); }
        }
    }
}
