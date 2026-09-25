package dtm.stools.component.panels.editor.sheet.provider;

public interface SheetSearchPopupProvider extends SheetProvider {
    SheetPopupHandle open(SheetSearchContext context, boolean replace);
}
