package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.autocomplete.CompletionContext;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Font;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CodeEditorCompletionBoundaryTest {

    @Test
    void typingDotWhileCompletionIsOpenKeepsTheReceiverOnAccept() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TestEditor editor = new TestEditor();
            TestPopup previous = new TestPopup(editor, 0);
            editor.autoCompletePopup = previous;
            editor.insertText(3, ".");
            editor.setCaretPosition(0, 4);

            editor.refreshAutoCompleteIfVisible();

            assertFalse(previous.isVisible());
            assertEquals(4, editor.autoCompletePopup.getTriggerOffset());
            assertEquals("", editor.prefixAtRestart);
            editor.applyAutoCompleteSelection();
            assertEquals("obj.equals()", editor.getBuffer().getText());
        });
    }

    private static final class TestEditor extends CodeEditorTextArea {
        private String prefixAtRestart;

        private TestEditor() {
            super("obj");
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            autoCompleteProvider = context -> List.of(new AutoCompleteItem("equals()"));
            autoCompleteTypingTrigger = value -> value == '.';
            setCaretPosition(0, 3);
        }

        @Override
        protected void triggerAutoComplete(CompletionContext.TriggerKind kind) {
            prefixAtRestart = computeWordPrefix(caretOffset());
            autoCompletePopup = new TestPopup(this, caretOffset() - prefixAtRestart.length());
        }
    }

    private static final class TestPopup extends AutoCompletePopup {
        private final int offset;
        private boolean visible = true;

        private TestPopup(CodeEditorTextArea owner, int offset) {
            super(owner);
            this.offset = offset;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public void hide() {
            visible = false;
        }

        @Override
        public int getTriggerOffset() {
            return offset;
        }

        @Override
        public AutoCompleteItem getSelectedItem() {
            return new AutoCompleteItem("equals()");
        }
    }
}
