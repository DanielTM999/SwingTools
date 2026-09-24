package dtm.stools.component.panels.editor.word.provider;

public interface WordCollaborationProvider extends WordProvider {
    void localChange(WordCollaborationEvent event);
}
