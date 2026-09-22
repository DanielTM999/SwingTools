package dtm.stools.component.panels.editor.code.rename;

public class InlineRenamePresenter implements RenamePresenter {

    @Override
    public void present(RenameSession session) {
        if (session == null || session.isFinished()) return;
        session.startInline();
    }
}
