package dtm.stools.component.panels.editor.word.provider;

public interface WordConfirmationProvider extends WordProvider {
    boolean confirm(WordConfirmationRequest request);
}
