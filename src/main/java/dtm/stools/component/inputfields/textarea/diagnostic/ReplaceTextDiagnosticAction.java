package dtm.stools.component.inputfields.textarea.diagnostic;

import dtm.stools.component.inputfields.textarea.SmartTextArea;
import dtm.stools.i18n.I18n;

import javax.swing.UIManager;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

record ReplaceTextDiagnosticAction(String replacement) implements TextDiagnosticAction {

    @Override
    public String getLabel() {
        return replacement.isEmpty() ? I18n.getText(SmartTextArea.class, "diagnostic.remove", "Remover") : replacement;
    }

    @Override
    public void apply(SmartTextArea area, TextDiagnostic diagnostic) {
        Document document = area.getDocument();
        int start = Math.min(diagnostic.start(), document.getLength());
        int end = Math.min(diagnostic.end(), document.getLength());
        area.beginCompoundEdit();
        try {
            document.remove(start, end - start);
            document.insertString(start, replacement, null);
            area.setCaretPosition(start + replacement.length());
        } catch (BadLocationException ex) {
            UIManager.getLookAndFeel().provideErrorFeedback(area);
        } finally {
            area.endCompoundEdit();
        }
    }
}
