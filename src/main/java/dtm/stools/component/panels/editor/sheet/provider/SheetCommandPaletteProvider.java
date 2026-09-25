package dtm.stools.component.panels.editor.sheet.provider;

public interface SheetCommandPaletteProvider extends SheetProvider {
    SheetPopupHandle open(SheetCommandPaletteContext context);
}
