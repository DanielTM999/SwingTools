package dtm.stools.component.panels.editor.code;

import org.junit.jupiter.api.Test;

import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkupTagCloserTest {

    @Test
    void closesTheTagJustOpened() {
        assertEquals("</a>", MarkupTagCloser.closingTagFor("<a", ""));
        assertEquals("</ide.resision>", MarkupTagCloser.closingTagFor(
                "<properties>\n    <ide.resision", "\n</properties>"));
        assertEquals("</x:item-1>", MarkupTagCloser.closingTagFor("<x:item-1", ""));
    }

    @Test
    void closesTagsWithAttributesEvenWhenValuesContainBrackets() {
        assertEquals("</a>", MarkupTagCloser.closingTagFor("<a x=\"1>2\" y='<b>'", ""));
        assertEquals("</a>", MarkupTagCloser.closingTagFor("<a\n   x=\"1\"", ""));
    }

    @Test
    void leavesEverythingElseAlone() {
        assertNull(MarkupTagCloser.closingTagFor("</a", ""));
        assertNull(MarkupTagCloser.closingTagFor("<a/", ""));
        assertNull(MarkupTagCloser.closingTagFor("<?xml version=\"1.0\"?", ""));
        assertNull(MarkupTagCloser.closingTagFor("<!DOCTYPE html", ""));
        assertNull(MarkupTagCloser.closingTagFor("<!-- <a", ""));
        assertNull(MarkupTagCloser.closingTagFor("<![CDATA[ <a", ""));
        assertNull(MarkupTagCloser.closingTagFor("<a x=\"1", ""));
        assertNull(MarkupTagCloser.closingTagFor("<a>b -", ""));
        assertNull(MarkupTagCloser.closingTagFor("< a", ""));
        assertNull(MarkupTagCloser.closingTagFor("<1a", ""));
        assertNull(MarkupTagCloser.closingTagFor("", ""));
    }

    @Test
    void doesNotDuplicateAnExistingClosingTag() {
        assertNull(MarkupTagCloser.closingTagFor("<a", "</a>"));
        assertEquals("</a>", MarkupTagCloser.closingTagFor("<a", "</ab>"));
    }

    @Test
    void typingTheBracketInsertsTheClosingTagOnlyWhenEnabled() {
        TestTextArea enabled = new TestTextArea("<ide.resision");
        enabled.setAutoCloseMarkupTags(true);
        enabled.typeAtEnd('>');
        assertEquals("<ide.resision></ide.resision>", enabled.getBuffer().getText());
        assertEquals("<ide.resision>".length(), enabled.caret());

        TestTextArea disabled = new TestTextArea("<ide.resision");
        disabled.typeAtEnd('>');
        assertEquals("<ide.resision>", disabled.getBuffer().getText());
    }

    @Test
    void codeEditorDelegatesTheFlagToItsTextArea() {
        CodeEditor editor = new CodeEditor();
        assertFalse(editor.isAutoCloseMarkupTags());
        editor.setAutoCloseMarkupTags(true);
        assertTrue(editor.getTextArea().isAutoCloseMarkupTags());
    }

    private static final class TestTextArea extends CodeEditorTextArea {
        private TestTextArea(String text) {
            super(text);
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            setSize(2_000, 2_000);
        }

        private void typeAtEnd(char c) {
            setCaretPosition(0, getBuffer().length());
            KeyAdapter handler = createKeyHandler();
            handler.keyTyped(new KeyEvent(this, KeyEvent.KEY_TYPED, System.currentTimeMillis(),
                    0, KeyEvent.VK_UNDEFINED, c));
        }

        private int caret() {
            return caretOffset();
        }
    }
}
