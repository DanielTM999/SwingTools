package dtm.stools.component.panels.editor.code.rename;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.i18n.I18n;

import javax.swing.JOptionPane;

public class SimpleDialogRenamePresenter implements RenamePresenter {

    @Override
    public void present(RenameSession session) {
        if (session == null || session.isFinished()) return;
        String current = session.currentName();
        String prompt = current.isEmpty()
                ? text("rename.prompt.empty", "Rename to:")
                : text("rename.prompt.current", "Rename '{current}' to:").replace("{current}", current);
        String value = current;
        while (true) {
            String input = (String) JOptionPane.showInputDialog(session.owner(), prompt, session.displayTitle(), JOptionPane.QUESTION_MESSAGE, null, null, value);
            if (input == null) {
                session.cancel();
                return;
            }
            String error = session.validate(input.trim());
            if (error == null) {
                session.commit(input.trim());
                return;
            }
            JOptionPane.showMessageDialog(session.owner(), error, text("rename.title", "Rename"), JOptionPane.WARNING_MESSAGE);
            value = input;
        }
    }

    protected String text(String key, String defaultValue) {
        return I18n.getText(CodeEditorTextArea.class, key, defaultValue);
    }
}
