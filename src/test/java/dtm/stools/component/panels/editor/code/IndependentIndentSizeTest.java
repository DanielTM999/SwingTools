package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.format.FormatContext;
import dtm.stools.component.panels.editor.code.prototype.TextBuffer;
import dtm.stools.component.panels.editor.code.multicaret.Caret;
import org.junit.jupiter.api.Test;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.awt.event.KeyEvent;
import static org.junit.jupiter.api.Assertions.*;

class IndependentIndentSizeTest {
    @Test void unsetSizeFollowsLegacyTabWidthAndCanBeRestored() throws Exception {
        onEdt(() -> {
            Area area = new Area();
            area.setTabSize(3);
            area.key(KeyEvent.VK_TAB, 0);
            assertEquals("   ", area.getText());
            area.setIndentSize(2);
            area.setTabSize(8);
            assertEquals(2, area.getIndentSize());
            area.setIndentSize(0);
            assertEquals(8, area.getIndentSize());
        });
    }

    @Test void tabIndentAndOutdentCombineTabsWithSpaces() throws Exception {
        onEdt(() -> {
            Area area = configured();
            area.key(KeyEvent.VK_TAB, 0);
            assertEquals("\t ", area.getText());
            assertEquals(4, area.columns());
            area.key(KeyEvent.VK_TAB, 0);
            assertEquals(8, area.columns());
            area.key(KeyEvent.VK_TAB, KeyEvent.SHIFT_DOWN_MASK);
            assertEquals("\t ", area.getText());
            assertEquals(4, area.columns());
        });
    }

    @Test void smartEnterAndBackspaceUseIndentWidthInsteadOfTabWidth() throws Exception {
        onEdt(() -> {
            Area area = configured();
            area.setText("\t {");
            area.caret(3);
            area.key(KeyEvent.VK_ENTER, 0);
            assertEquals("\t {\n\t\t  ", area.getText());
            area.key(KeyEvent.VK_BACK_SPACE, 0);
            assertEquals("\t {\n\t ", area.getText());
            assertEquals(2, area.getCaretCol());
        });
    }

    @Test void multipleCaretsAdvanceByTheirOwnInsertedLengths() throws Exception {
        onEdt(() -> {
            Area area = configured();
            area.setText(" \n  ");
            area.caret(1);
            area.extraCarets.add(new Caret(1, 2));
            area.key(KeyEvent.VK_TAB, 0);
            assertEquals(5, area.widthOf(0));
            assertEquals(6, area.widthOf(1));
        });
    }

    @Test void formatterContextPreservesLegacyConstructorAndExposesIndependentSize() {
        var buffer = new TextBuffer("text");
        var legacy = new FormatContext(buffer, 0, 4, 8, false, FormatContext.Scope.DOCUMENT);
        assertEquals(8, legacy.indentSize());
        var independent = new FormatContext(buffer, 0, 4, 8, false, FormatContext.Scope.DOCUMENT, 2);
        assertEquals(8, independent.tabSize());
        assertEquals(2, independent.indentSize());
    }

    private static Area configured() {
        Area area = new Area();
        area.setTabSize(3);
        area.setStripBlankLines(false);
        area.setIndentSize(4);
        area.setUseSpacesForTab(false);
        return area;
    }

    private static void onEdt(Runnable action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Object previous = UIManager.put("defaultFont", new Font(Font.MONOSPACED, Font.PLAIN, 14));
            try { action.run(); } finally { UIManager.put("defaultFont", previous); }
        });
    }

    private static class Area extends CodeEditorTextArea {
        Area() { setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14)); }
        String getText() { return buffer.getText(); }
        void caret(int offset) { setCaretFromOffset(offset); }
        int columns() { return widthOf(getCaretLine()); }
        int widthOf(int line) { return indentColumnsOf(buffer.lineAt(line)); }
        void key(int code, int modifiers) {
            createKeyHandler().keyPressed(new KeyEvent(this, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                    modifiers, code, KeyEvent.CHAR_UNDEFINED));
        }
    }
}
