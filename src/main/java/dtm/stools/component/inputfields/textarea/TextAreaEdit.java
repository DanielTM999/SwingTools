package dtm.stools.component.inputfields.textarea;

import javax.swing.text.BadLocationException;

@FunctionalInterface
interface TextAreaEdit {
    void run() throws BadLocationException;
}
