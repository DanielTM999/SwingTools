package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.ghost.GhostTextSuggestion;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Font;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorGhostTextImportTest {

    private static final String SOURCE = "package demo;\n\nclass Demo {\n    LocalDa\n}\n";

    @Test
    void acceptingGhostAppliesImportAndKeepsCaretAfterText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = editor();
            editor.setCaretPosition(3, 11);
            int anchor = editor.caretOffset();
            editor.setActiveGhostText(new GhostTextSuggestion("te", List.of(
                    TextEdit.insert(new Position(2, 0), "import java.time.LocalDate;\n"))), 3, 11, anchor);

            assertTrue(editor.acceptGhostText());

            String text = editor.getBuffer().getText();
            assertEquals("package demo;\n\nimport java.time.LocalDate;\nclass Demo {\n    LocalDate\n}\n", text);
            assertEquals(4, editor.getCaretLine());
            assertEquals(13, editor.getCaretCol());
            assertFalse(editor.hasGhostText());
        });
    }

    @Test
    void singleUndoRevertsGhostAndImport() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = editor();
            editor.setCaretPosition(3, 11);
            int anchor = editor.caretOffset();
            editor.setActiveGhostText(new GhostTextSuggestion("te", List.of(
                    TextEdit.insert(new Position(2, 0), "import java.time.LocalDate;\n"))), 3, 11, anchor);

            editor.acceptGhostText();
            editor.performUndo();

            assertEquals(SOURCE, editor.getBuffer().getText());
        });
    }

    @Test
    void plainGhostStillInsertsOnlyText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = editor();
            editor.setCaretPosition(3, 11);
            editor.setActiveGhostText("te", 3, 11, editor.caretOffset());

            assertTrue(editor.acceptGhostText());

            assertEquals(SOURCE.replace("LocalDa", "LocalDate"), editor.getBuffer().getText());
        });
    }

    private static CodeEditorTextArea editor() {
        CodeEditorTextArea editor = new CodeEditorTextArea(SOURCE);
        editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        return editor;
    }

    @Test
    void visibleItemsAreFilteredLocallyByPrefix() {
        AutoCompleteItem string = new AutoCompleteItem("String", "String", null, null, null,
                AutoCompleteItem.Kind.CLASS, List.of());
        AutoCompleteItem stream = new AutoCompleteItem("Stream", "Stream", null, null, null,
                AutoCompleteItem.Kind.CLASS, List.of());

        assertEquals(List.of(string), CodeEditorTextAreaCompletion.filterVisibleItems(
                List.of(string, stream), "stri"));
        assertEquals(List.of(), CodeEditorTextAreaCompletion.filterVisibleItems(
                List.of(string, stream), "x"));
    }
}
