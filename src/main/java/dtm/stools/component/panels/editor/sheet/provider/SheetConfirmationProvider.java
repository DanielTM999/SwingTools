package dtm.stools.component.panels.editor.sheet.provider;

public interface SheetConfirmationProvider extends SheetProvider {
    int confirm(SheetConfirmationRequest request);
}
