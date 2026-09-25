package dtm.stools.component.panels.editor.sheet.api;

@FunctionalInterface
public interface SheetSelectionListener {
    void selectionChanged(int sheet, SheetSelection selection);
}
