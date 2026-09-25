package dtm.stools.component.panels.editor.sheet.api;

import dtm.stools.component.panels.editor.sheet.command.History;
import dtm.stools.component.panels.editor.sheet.command.SheetChange;
import dtm.stools.component.panels.editor.sheet.command.SheetCommand;
import dtm.stools.component.panels.editor.sheet.command.SheetEdit;
import dtm.stools.component.panels.editor.sheet.command.SheetTransaction;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.SheetWorkbook;
import dtm.stools.component.panels.editor.sheet.model.SheetWorksheet;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class SheetSession {
    private final List<Consumer<SheetSessionEvent>> listeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<SheetChange>> changeListeners = new CopyOnWriteArrayList<>();
    private final AtomicLong ids = new AtomicLong();
    private final History history;
    private SheetWorkbook workbook = SheetWorkbook.create();
    private int activeSheet;
    private SheetSelection selection = SheetSelection.home();
    private long revision, savedId;
    private boolean readOnly, forcedDirty;
    private String author = Optional.ofNullable(System.getProperty("user.name")).filter(s -> !s.isBlank()).orElse("Autor");
    private Consumer<Throwable> errorHandler = e -> System.getLogger(SheetSession.class.getName()).log(System.Logger.Level.WARNING, "Sheet listener failed", e);
    private SheetTransaction open;

    public SheetSession() { this(200); }
    public SheetSession(int historyLimit) { history = new History(historyLimit); }

    public SheetWorkbook getWorkbook() { return workbook; }
    public int getActiveSheetIndex() { return Math.min(activeSheet, workbook.sheetCount() - 1); }
    public SheetWorksheet getActiveSheet() { return workbook.sheet(getActiveSheetIndex()); }
    public SheetSelection getSelection() { return selection; }
    public long getRevision() { return revision; }
    public boolean isReadOnly() { return readOnly; }
    public boolean isDirty() { return forcedDirty || history.currentId() != savedId; }
    public boolean canUndo() { return !readOnly && history.canUndo(); }
    public boolean canRedo() { return !readOnly && history.canRedo(); }
    public String undoLabel() { return history.undoLabel(); }
    public String redoLabel() { return history.redoLabel(); }
    public History history() { return history; }
    public String getAuthor() { return author; }
    public void setAuthor(String value) { if (value == null || value.isBlank()) throw new IllegalArgumentException("Author is required"); author = value.strip(); }
    public void setErrorHandler(Consumer<Throwable> handler) { errorHandler = Objects.requireNonNull(handler); }
    public void setHistoryLimit(int value) { history.setLimit(value); }
    public void setReadOnly(boolean value) { readOnly = value; fire(SheetSessionEvent.Kind.STATE, "Somente leitura", Map.of(), false); }

    public ProviderRegistration addListener(Consumer<SheetSessionEvent> listener) {
        listeners.add(Objects.requireNonNull(listener));
        return () -> listeners.remove(listener);
    }

    public ProviderRegistration addChangeListener(Consumer<SheetChange> listener) {
        changeListeners.add(Objects.requireNonNull(listener));
        return () -> changeListeners.remove(listener);
    }

    public void load(SheetWorkbook value) {
        workbook = Objects.requireNonNull(value);
        if (workbook.sheetCount() == 0) workbook.addSheet(0, new SheetWorksheet("Planilha1"));
        activeSheet = workbook.activeSheetIndex();
        selection = SheetSelection.home();
        history.clear();
        savedId = 0; forcedDirty = false;
        revision++;
        fire(SheetSessionEvent.Kind.LOAD, "Abrir", Map.of(), true);
    }

    public void markSaved() { savedId = history.currentId(); forcedDirty = false; fire(SheetSessionEvent.Kind.STATE, "Salvo", Map.of(), false); }
    public void markDirty() { forcedDirty = true; fire(SheetSessionEvent.Kind.STATE, "Alterado", Map.of(), false); }

    public void setActiveSheet(int index) {
        if (index < 0 || index >= workbook.sheetCount()) throw new IndexOutOfBoundsException("Sheet " + index);
        if (index == activeSheet) return;
        activeSheet = index;
        selection = SheetSelection.home();
        revision++;
        fire(SheetSessionEvent.Kind.SHEET, "Planilha", Map.of(), false);
    }

    public void setActiveSheet(int index, SheetSelection value) {
        activeSheet = index;
        selection = Objects.requireNonNull(value);
        revision++;
        fire(SheetSessionEvent.Kind.SHEET, "Planilha", Map.of(), false);
    }

    public void setSelection(SheetSelection value) {
        selection = Objects.requireNonNull(value);
        fire(SheetSessionEvent.Kind.SELECTION, "Seleção", Map.of(), false);
    }

    public void select(CellAddress a) { setSelection(SheetSelection.of(a)); }
    public void select(CellRange r) { setSelection(SheetSelection.of(r)); }

    public boolean isInTransaction() { return open != null; }

    public SheetTransaction currentTransaction() { return open; }

    public boolean execute(SheetCommand command) { return execute(command.label(), command); }

    public boolean execute(String label, SheetCommand command) {
        requireEditable();
        if (open != null) { command.apply(open); return true; }
        SheetTransaction tx = new SheetTransaction(workbook, getActiveSheetIndex(), selection);
        int sheetBefore = getActiveSheetIndex();
        SheetSelection selectionBefore = selection;
        open = tx;
        try {
            command.apply(tx);
        } catch (RuntimeException | Error failure) {
            open = null;
            tx.rollback();
            throw failure;
        }
        open = null;
        boolean moved = tx.activeSheet() != sheetBefore || !Objects.equals(tx.selection(), selectionBefore);
        activeSheet = Math.max(0, Math.min(tx.activeSheet(), workbook.sheetCount() - 1));
        selection = tx.selection() == null ? SheetSelection.home() : tx.selection();
        if (tx.isEmpty()) {
            if (moved) fire(SheetSessionEvent.Kind.SELECTION, label, Map.of(), false);
            return false;
        }
        SheetChange change = new SheetChange(ids.incrementAndGet(), label, tx.edits(), sheetBefore, selectionBefore, activeSheet, selection, tx.touched(), tx.structural() || tx.edits().stream().anyMatch(SheetEdit::structural));
        history.push(change);
        revision++;
        notifyChange(change);
        fire(SheetSessionEvent.Kind.CONTENT, label, change.touched(), change.structural());
        return true;
    }

    public void undo() {
        requireEditable();
        SheetChange change = history.popUndo();
        if (change == null) return;
        List<SheetEdit> edits = change.edits();
        for (int i = edits.size() - 1; i >= 0; i--) edits.get(i).undo(workbook);
        activeSheet = Math.min(change.sheetBefore(), workbook.sheetCount() - 1);
        selection = change.selectionBefore();
        revision++;
        notifyChange(change);
        fire(SheetSessionEvent.Kind.CONTENT, "Desfazer " + change.label(), change.touched(), change.structural());
    }

    public void redo() {
        requireEditable();
        SheetChange change = history.popRedo();
        if (change == null) return;
        for (SheetEdit e : change.edits()) e.redo(workbook);
        activeSheet = Math.min(change.sheetAfter(), workbook.sheetCount() - 1);
        selection = change.selectionAfter();
        revision++;
        notifyChange(change);
        fire(SheetSessionEvent.Kind.CONTENT, "Refazer " + change.label(), change.touched(), change.structural());
    }

    public void applyRemote(String label, SheetCommand command) {
        boolean ro = readOnly;
        readOnly = false;
        try { execute(label, command); } finally { readOnly = ro; }
    }

    public void valuesChanged(Map<String, List<CellRange>> touched) {
        fire(SheetSessionEvent.Kind.VALUES, "Recalcular", touched, false);
    }

    private void notifyChange(SheetChange change) {
        for (Consumer<SheetChange> l : changeListeners) {
            try { l.accept(change); } catch (RuntimeException e) { errorHandler.accept(e); }
        }
    }

    public void requireEditable() { if (readOnly) throw new IllegalStateException("A pasta de trabalho está somente leitura."); }

    protected void fire(SheetSessionEvent.Kind kind, String label, Map<String, List<CellRange>> touched, boolean structural) {
        SheetSessionEvent event = new SheetSessionEvent(kind, revision, label, touched, structural);
        for (Consumer<SheetSessionEvent> l : listeners) {
            try { l.accept(event); } catch (RuntimeException e) { errorHandler.accept(e); }
        }
    }
}
