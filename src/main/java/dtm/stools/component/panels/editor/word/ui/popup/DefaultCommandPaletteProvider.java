package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.command.CommandEntry;
import dtm.stools.component.command.CommandPalette;
import dtm.stools.component.panels.editor.word.provider.*;

/** Default adapter; applications can continue to replace this provider. */
public final class DefaultCommandPaletteProvider implements WordCommandPaletteProvider {
    @Override
    public String id() { return "word.popup.palette.default"; }
    @Override
    public WordPopupHandle show(WordCommandPaletteContext context) {
        CommandPalette palette = new CommandPalette(
            () -> context.commands().stream().map(e -> new CommandEntry(e.id(), e.name(), e.group(), "", "", e.enabled())).toList(),
            context::execute).onClosed(context::closed);
        palette.setLocale(context.locale()); palette.setName("word.palette.dialog");
        palette.open(context.owner());
        return new WordPopupHandle() {
            public boolean isOpen() { return palette.isOpen(); }
            public void toFront() { palette.toFront(); }
            public void close() { palette.close(); }
        };
    }

}
