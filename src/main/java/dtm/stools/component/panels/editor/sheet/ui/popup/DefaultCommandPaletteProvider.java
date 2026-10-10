package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.command.CommandEntry;
import dtm.stools.component.command.CommandPalette;
import dtm.stools.component.panels.editor.sheet.provider.*;

/** Default adapter; applications can continue to replace this provider. */
public final class DefaultCommandPaletteProvider implements SheetCommandPaletteProvider {
    @Override public String id() { return "sheet.popup.palette.default"; }
    @Override public SheetPopupHandle open(SheetCommandPaletteContext context) {
        CommandPalette palette = new CommandPalette(
            () -> context.commands().stream().map(e -> new CommandEntry(e.id(), e.name(), e.group(), "", e.shortcut(), e.enabled())).toList(),
            context::execute);
        palette.setLocale(context.locale());
        palette.open(context.owner());
        return new SheetPopupHandle() {
            public boolean isOpen() { return palette.isOpen(); }
            public void toFront() { palette.toFront(); }
            public void close() { palette.close(); }
        };
    }
    static javax.swing.KeyStroke none() { return null; }
}
