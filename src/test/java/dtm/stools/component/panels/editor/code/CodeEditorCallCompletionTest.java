package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Font;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorCallCompletionTest {

    private static final String SOURCE = "class A {\n  void run() {\n    lista.ad\n  }\n}\n";

    @Test void replacementRangeKeepsReceiverAndRemovesOnlyTargetWord() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var range = new dtm.stools.component.panels.editor.code.api.Range(
                    new dtm.stools.component.panels.editor.code.api.Position(2, 10),
                    new dtm.stools.component.panels.editor.code.api.Position(2, 12));
            var item = new AutoCompleteItem("add()", "add", null, null, null,
                    AutoCompleteItem.Kind.METHOD, List.of(), false, null, range);
            var editor = select(item);
            assertTrue(editor.getBuffer().getText().contains("lista.add()"));
            editor.getBuffer().undo();
            assertEquals(SOURCE, editor.getBuffer().getText());
        });
    }

    @Test
    void caretMarkerPlacesTheCaretInsideTheParentheses() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = select(method("add(${0})", "add(E e) : boolean"));

            assertTrue(editor.getBuffer().getText().contains("    lista.add()\n"));
            assertEquals(2, editor.getCaretLine());
            assertEquals(14, editor.getCaretCol());
        });
    }

    @Test
    void plainCallKeepsTheCaretAfterTheParentheses() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = select(method("size()", "size() : int"));

            assertTrue(editor.getBuffer().getText().contains("    lista.size()\n"));
            assertEquals(16, editor.getCaretCol());
        });
    }

    @Test
    void onlyItemsWithTheMarkerBecomeSnippets() {
        assertTrue(method("add(${0})", "add").isSnippet());
        assertFalse(method("size()", "size").isSnippet());
        assertFalse(method("preco$0", "preco$0").isSnippet());
        assertTrue(AutoCompleteItem.supportsCaretMarker());
    }

    private static AutoCompleteItem method(String insert, String label) {
        return new AutoCompleteItem(insert, label, null, null, null,
                AutoCompleteItem.Kind.METHOD, List.of());
    }

    private static CodeEditorTextArea select(AutoCompleteItem item) {
        CodeEditorTextArea editor = new CodeEditorTextArea(SOURCE);
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        editor.setCaretPosition(2, 12);
        int trigger = editor.buffer.offsetOfLine(2) + 10;
        editor.autoCompletePopup = new TestPopup(editor, item, trigger);
        editor.applyAutoCompleteSelection();
        return editor;
    }

    private static final class TestPopup extends AutoCompletePopup {
        private final AutoCompleteItem item;
        private final int trigger;

        private TestPopup(CodeEditorTextArea owner, AutoCompleteItem item, int trigger) {
            super(owner);
            this.item = item;
            this.trigger = trigger;
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public AutoCompleteItem getSelectedItem() {
            return item;
        }

        @Override
        public int getTriggerOffset() {
            return trigger;
        }

        @Override
        public void hide() {
        }
    }
}
