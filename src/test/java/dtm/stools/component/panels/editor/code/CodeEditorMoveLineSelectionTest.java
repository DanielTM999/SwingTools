package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.prototype.folding.FoldRule;
import org.junit.jupiter.api.Test;

import java.awt.Font;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorMoveLineSelectionTest {

    @Test
    void movesEveryTouchedLineAndKeepsPartialSelection() {
        CodeEditorTextArea area = area("zero\nalpha\nbeta\ngamma");
        area.setSelection(1, 2, 2, 2);

        area.moveLineUp();

        assertEquals("alpha\nbeta\nzero\ngamma", area.buffer.getText());
        assertEquals("pha\nbe", area.getSelectedTextOrEmpty());
        assertEquals(0, area.selectionStartLine);
        assertEquals(2, area.selectionStartCol);
        assertEquals(1, area.caretLine);
        assertEquals(2, area.caretCol);

        area.moveLineDown();
        area.moveLineDown();

        assertEquals("zero\ngamma\nalpha\nbeta", area.buffer.getText());
        assertEquals("pha\nbe", area.getSelectedTextOrEmpty());
        assertEquals(2, area.selectionStartLine);
        assertEquals(3, area.caretLine);
    }

    @Test
    void preservesBackwardSelectionThroughUndoAndRedo() {
        CodeEditorTextArea area = area("a\nbb\ncc\nd");
        area.setSelection(2, 1, 1, 1);

        area.moveLineDown();

        assertEquals("a\nd\nbb\ncc", area.buffer.getText());
        assertEquals("b\nc", area.getSelectedTextOrEmpty());
        assertEquals(3, area.selectionStartLine);
        assertEquals(2, area.caretLine);

        area.performUndo();

        assertEquals("a\nbb\ncc\nd", area.buffer.getText());
        assertEquals(2, area.selectionStartLine);
        assertEquals(1, area.caretLine);

        area.performRedo();

        assertEquals("a\nd\nbb\ncc", area.buffer.getText());
        assertEquals("b\nc", area.getSelectedTextOrEmpty());
        assertEquals(3, area.selectionStartLine);
        assertEquals(2, area.caretLine);
    }

    @Test
    void excludesLineAtSelectionEndColumnZero() {
        CodeEditorTextArea area = area("a\nb\nc\nd");
        area.setSelection(1, 0, 2, 0);

        area.moveLineDown();

        assertEquals("a\nc\nb\nd", area.buffer.getText());
        assertEquals("b\n", area.getSelectedTextOrEmpty());
        assertEquals(2, area.selectionStartLine);
        assertEquals(3, area.caretLine);

        area.moveLineDown();

        assertEquals("a\nc\nd\nb", area.buffer.getText());
        assertEquals("b", area.getSelectedTextOrEmpty());
        assertEquals(3, area.selectionStartLine);
        assertEquals(3, area.caretLine);
        assertEquals(1, area.caretCol);

        area.moveLineDown();

        assertEquals("a\nc\nd\nb", area.buffer.getText());
        assertEquals("b", area.getSelectedTextOrEmpty());
    }

    @Test
    void keepsCaretOnlyBehaviorAndSelectionAtDocumentBoundary() {
        CodeEditorTextArea area = area("a\nb\nc");
        area.setCaretPosition(1, 1);

        area.moveLineUp();

        assertEquals("b\na\nc", area.buffer.getText());
        assertEquals(0, area.caretLine);
        assertEquals(1, area.caretCol);
        assertFalse(area.hasSelection());

        area.setSelection(0, 0, 1, 0);
        area.moveLineUp();

        assertEquals("b\na\nc", area.buffer.getText());
        assertEquals("b\n", area.getSelectedTextOrEmpty());
        assertEquals(0, area.selectionStartLine);
        assertEquals(1, area.caretLine);
    }

    @Test
    void movesCollapsedRegionAsPartOfSelectedBlock() {
        CodeEditorTextArea area = area("before\n{\ninside\n}\nafter");
        area.setFoldingEnabled(true);
        area.addFoldRule(new FoldRule.Pair('{', '}'));
        area.toggleFold(1);
        area.setSelection(1, 0, 1, 1);

        area.moveLineUp();

        assertEquals("{\ninside\n}\nbefore\nafter", area.buffer.getText());
        assertEquals("{", area.getSelectedTextOrEmpty());
        assertTrue(area.getFoldRegions().stream().anyMatch(r -> r.startLine() == 0 && r.folded()),
                area.getFoldRegions().toString());

        area.moveLineDown();

        assertEquals("before\n{\ninside\n}\nafter", area.buffer.getText());
        assertEquals("{", area.getSelectedTextOrEmpty());
        assertTrue(area.getFoldRegions().stream().anyMatch(r -> r.startLine() == 1 && r.folded()),
                area.getFoldRegions().toString());
    }

    private static CodeEditorTextArea area(String text) {
        CodeEditorTextArea area = new CodeEditorTextArea(text);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        area.setSize(500, 300);
        return area;
    }
}
