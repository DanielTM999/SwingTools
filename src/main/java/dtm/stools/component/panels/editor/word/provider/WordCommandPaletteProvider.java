package dtm.stools.component.panels.editor.word.provider;

public interface WordCommandPaletteProvider extends WordProvider {
    WordPopupHandle show(WordCommandPaletteContext context);
}
