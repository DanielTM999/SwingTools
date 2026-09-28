package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRegion;
import dtm.stools.component.panels.editor.code.prototype.folding.FoldRule;
import org.junit.jupiter.api.Test;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorAutoImportViewTest {

    private static final String SOURCE = "package demo;\n\n"
            + "class First {\n  void run() {\n    work();\n  }\n}\n\n"
            + "class Second {\n  void run() {\n    work();\n  }\n}\n";

    @Test
    void importBeforeCollapsedCodeKeepsFoldCaretAndVisibleLine() throws Exception {
        AtomicReference<CodeEditorTextArea> editorRef = new AtomicReference<>();
        AtomicReference<JViewport> viewportRef = new AtomicReference<>();
        AtomicReference<Integer> topLineRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = new CodeEditorTextArea(SOURCE);
            editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            editor.setFoldingEnabled(true);
            editor.addFoldRule(FoldRule.pair('{', '}'));
            editor.toggleFold(2);
            editor.setCaretPosition(9, 5);

            JViewport viewport = new JViewport();
            viewport.setView(editor);
            viewport.setSize(new Dimension(280, 45));
            editor.setSize(editor.getPreferredSize());
            viewport.setViewPosition(new Point(0, editor.yOfBufferLine(8)));
            topLineRef.set(editor.bufferLineAtY(viewport.getViewPosition().y));

            assertEquals(1, editor.applyEdits(List.of(
                    TextEdit.insert(new Position(2, 0), "import java.util.List;\n\n"))));
            editorRef.set(editor);
            viewportRef.set(viewport);
        });
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = editorRef.get();
            JViewport viewport = viewportRef.get();
            assertTrue(editor.getFoldRegions().stream().anyMatch(r -> r.startLine() == 4 && r.folded()));
            assertEquals(11, editor.getCaretLine());
            assertEquals(5, editor.getCaretCol());
            assertEquals(topLineRef.get() + 2, editor.bufferLineAtY(viewport.getViewPosition().y));
        });
        Thread.sleep(200);
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = editorRef.get();
            assertTrue(editor.getFoldRegions().stream().anyMatch(r -> r.startLine() == 4 && r.folded()));
            assertEquals(topLineRef.get() + 2,
                    editor.bufferLineAtY(viewportRef.get().getViewPosition().y));
        });
    }

    @Test
    void importInsideAnEarlierBlockKeepsLaterCollapsedBlock() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = new CodeEditorTextArea(SOURCE);
            editor.setFoldingEnabled(true);
            editor.addFoldRule(FoldRule.pair('{', '}'));
            editor.toggleFold(8);

            editor.applyEdits(List.of(TextEdit.insert(new Position(4, 0), "    added();\n")));

            FoldRegion second = editor.getFoldRegions().stream()
                    .filter(region -> region.startLine() == 9)
                    .findFirst().orElseThrow();
            assertTrue(second.folded());
        });
    }

    @Test
    void undoAndRedoKeepTheCollapsedRegion() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            CodeEditorTextArea editor = new CodeEditorTextArea(SOURCE);
            editor.setFoldingEnabled(true);
            editor.addFoldRule(FoldRule.pair('{', '}'));
            editor.toggleFold(2);

            editor.applyEdits(List.of(TextEdit.insert(new Position(2, 0), "import java.util.List;\n\n")));
            editor.performUndo();
            assertTrue(editor.getFoldRegions().stream().anyMatch(r -> r.startLine() == 2 && r.folded()));

            editor.performRedo();
            assertTrue(editor.getFoldRegions().stream().anyMatch(r -> r.startLine() == 4 && r.folded()));
        });
    }

    @Test
    void completionWithAdditionalImportKeepsEarlierFold() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            String source = SOURCE.replace("    work();\n  }\n}\n", "    Lis value;\n  }\n}\n");
            CodeEditorTextArea editor = new CodeEditorTextArea(source);
            editor.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
            editor.setFoldingEnabled(true);
            editor.addFoldRule(FoldRule.pair('{', '}'));
            editor.toggleFold(2);
            int line = 10;
            editor.setCaretPosition(line, 7);
            int trigger = editor.buffer.offsetOfLine(line) + 4;
            AutoCompleteItem item = new AutoCompleteItem("List", "List", null, null, null,
                    AutoCompleteItem.Kind.CLASS,
                    List.of(TextEdit.insert(new Position(2, 0), "import java.util.List;\n\n")));
            editor.autoCompletePopup = new TestPopup(editor, item, trigger);

            editor.applyAutoCompleteSelection();

            assertTrue(editor.getFoldRegions().stream().anyMatch(r -> r.startLine() == 4 && r.folded()));
            assertTrue(editor.getBuffer().getText().contains("import java.util.List;"));
            assertTrue(editor.getBuffer().getText().contains("List value;"));
        });
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
