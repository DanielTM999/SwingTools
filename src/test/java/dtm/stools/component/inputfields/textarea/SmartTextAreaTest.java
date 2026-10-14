package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.events.EventType;
import dtm.stools.models.SearchPanelMode;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import javax.swing.text.PlainDocument;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartTextAreaTest {

    @Test
    void typingIsUndoneWordByWord() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea();
            type(area, "abc def");
            assertEquals("abc def", area.getText());

            area.undo();
            assertEquals("abc", area.getText());
            area.undo();
            assertEquals("", area.getText());
            assertFalse(area.canUndo());

            area.redo();
            assertEquals("abc", area.getText());
            return null;
        });
    }

    @Test
    void initialTextIsNotUndoable() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("hello");
            assertFalse(area.canUndo());
            return null;
        });
    }

    @Test
    void setTextIsUndoneInOneStep() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("one");
            area.setText("two");
            area.undo();
            assertEquals("one", area.getText());
            return null;
        });
    }

    @Test
    void duplicatesLineAndUndoesInOneStep() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("a\nb\nc");
            area.setCaretPosition(2);
            area.duplicateLines();
            assertEquals("a\nb\nb\nc", area.getText());
            assertEquals(4, area.getCaretPosition());

            area.setCaretPosition(area.getDocument().getLength());
            area.duplicateLines();
            assertEquals("a\nb\nb\nc\nc", area.getText());

            area.undo();
            assertEquals("a\nb\nb\nc", area.getText());
            area.undo();
            assertEquals("a\nb\nc", area.getText());
            return null;
        });
    }

    @Test
    void movesLinesKeepingSelection() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("one\ntwo\nthree");
            area.select(4, 7);
            area.moveLinesDown();
            assertEquals("one\nthree\ntwo", area.getText());
            assertEquals("two", area.getSelectedText());

            area.moveLinesUp();
            assertEquals("one\ntwo\nthree", area.getText());
            assertEquals("two", area.getSelectedText());

            area.setCaretPosition(0);
            area.moveLinesUp();
            assertEquals("one\ntwo\nthree", area.getText());

            area.undo();
            assertEquals("one\nthree\ntwo", area.getText());
            return null;
        });
    }

    @Test
    void deletesLines() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("one\ntwo\nthree");
            area.setCaretPosition(5);
            area.deleteLines();
            assertEquals("one\nthree", area.getText());

            area.setCaretPosition(area.getDocument().getLength());
            area.deleteLines();
            assertEquals("one", area.getText());
            return null;
        });
    }

    @Test
    void indentsAndOutdentsSelectedLines() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("a\nb\n\nc");
            area.setUseSpacesForTab(true);
            area.setTabSize(4);
            area.select(0, area.getDocument().getLength());
            area.indentSelection();
            assertEquals("    a\n    b\n\n    c", area.getText());

            area.outdentSelection();
            assertEquals("a\nb\n\nc", area.getText());

            area.undo();
            assertEquals("    a\n    b\n\n    c", area.getText());
            area.undo();
            assertEquals("a\nb\n\nc", area.getText());
            return null;
        });
    }

    @Test
    void tabInsertsSpacesUpToNextStop() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("ab");
            area.setUseSpacesForTab(true);
            area.setTabSize(4);
            area.setCaretPosition(2);
            area.indentSelection();
            assertEquals("ab  ", area.getText());
            return null;
        });
    }

    @Test
    void enterKeepsIndentation() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("\t  item");
            area.setCaretPosition(area.getDocument().getLength());
            area.getActionMap().get("smartTextArea.newline").actionPerformed(null);
            assertEquals("\t  item\n\t  ", area.getText());

            area.undo();
            assertEquals("\t  item", area.getText());
            return null;
        });
    }

    @Test
    void maxLengthTruncatesAndFiresEvent() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea();
            AtomicInteger reached = new AtomicInteger();
            area.addEventListener(SmartTextArea.LIMIT_REACHED, e -> reached.incrementAndGet());
            area.setMaxLength(5);
            area.replaceSelection("abcdefgh");
            assertEquals("abcde", area.getText());
            assertEquals(1, reached.get());

            area.setMaxLength(-1);
            area.replaceSelection("xyz");
            assertEquals("abcdexyz", area.getText());
            return null;
        });
    }

    @Test
    void firesInputEvents() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea();
            AtomicReference<Object> value = new AtomicReference<>();
            area.addEventListener(EventType.INPUT, e -> value.set(e.getValue()));
            area.replaceSelection("hi");
            assertEquals("hi", value.get());
            return null;
        });
    }

    @Test
    void newDocumentKeepsUndoAndLimit() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea();
            area.setMaxLength(3);
            area.setDocument(new PlainDocument());
            area.replaceSelection("abcdef");
            assertEquals("abc", area.getText());
            area.undo();
            assertEquals("", area.getText());
            return null;
        });
    }

    @Test
    void searchHandlesOptionsAndInvalidRegex() {
        String text = "Cat cat catalog CAT";
        assertEquals(4, TextAreaSearch.findAll(text, TextAreaSearch.compile("cat", false, false, false)).size());
        assertEquals(1, TextAreaSearch.findAll(text, TextAreaSearch.compile("cat", true, true, false)).size());
        assertEquals(3, TextAreaSearch.findAll(text, TextAreaSearch.compile("cat", false, true, false)).size());
        assertEquals(3, TextAreaSearch.findAll(text, TextAreaSearch.compile("c.t\\b", false, false, true)).size());
        assertNull(TextAreaSearch.compile("(", false, false, true));
        assertTrue(TextAreaSearch.findAll(text, null).isEmpty());

        List<TextAreaSearchMatch> matches = TextAreaSearch.findAll(text, TextAreaSearch.compile("cat", false, false, false));
        assertEquals(1, TextAreaSearch.indexAtOrAfter(matches, 1));
        assertEquals(0, TextAreaSearch.indexAtOrAfter(matches, 100));
        assertEquals(3, TextAreaSearch.indexBefore(matches, 0));

        Pattern pattern = TextAreaSearch.compile("(\\w+)@(\\w+)", false, false, true);
        String mail = "x joe@host y";
        TextAreaSearchMatch match = TextAreaSearch.findAll(mail, pattern).getFirst();
        assertEquals("host at joe", TextAreaSearch.replacementFor(pattern, mail, match, "$2 at $1", true));
        assertEquals("$9", TextAreaSearch.replacementFor(pattern, mail, match, "$9", true));
    }

    @Test
    void replaceAllIsUndoneInOneStep() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("foo bar foo baz foo");
            SmartTextAreaScrollPane pane = area.createScrollPane();
            assertTrue(area.openReplace());
            TextAreaSearchBar bar = pane.getSearchBar();
            bar.setQuery("foo");
            assertEquals(3, bar.getMatchCount());
            bar.setReplaceText("x");
            bar.replaceAll();
            assertEquals("x bar x baz x", area.getText());
            assertEquals(0, bar.getMatchCount());

            area.undo();
            assertEquals("foo bar foo baz foo", area.getText());
            return null;
        });
    }

    @Test
    void findNextCyclesThroughMatches() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("ab ab ab");
            SmartTextAreaScrollPane pane = area.createScrollPane();
            area.openSearch();
            TextAreaSearchBar bar = pane.getSearchBar();
            bar.setQuery("ab");
            assertEquals(0, bar.getCurrentIndex());
            area.findNext();
            assertEquals(3, area.getSelectionStart());
            area.findNext();
            area.findNext();
            assertEquals(0, area.getSelectionStart());
            area.findPrevious();
            assertEquals(6, area.getSelectionStart());

            area.closeSearch();
            assertFalse(area.isSearchOpen());
            assertFalse(area.getActionMap().get("smartTextArea.closeSearch").isEnabled());
            return null;
        });
    }

    @Test
    void embeddedSearchReservesSpaceAboveViewport() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("abc abc");
            area.setSearchMode(SearchPanelMode.EMBEDDED);
            SmartTextAreaScrollPane pane = area.createScrollPane();
            pane.setSize(400, 200);
            layoutTree(pane);
            int topBefore = pane.getInsets().top;

            area.openSearch();
            layoutTree(pane);
            TextAreaSearchBar bar = pane.getSearchBar();
            assertFalse(bar.isFloating());
            assertTrue(pane.getInsets().top > topBefore);
            assertTrue(bar.getY() + bar.getHeight() <= pane.getViewport().getY());
            assertTrue(bar.getWidth() >= pane.getViewport().getWidth());

            area.closeSearch();
            layoutTree(pane);
            assertEquals(topBefore, pane.getInsets().top);

            area.setSearchMode(SearchPanelMode.FLOATING);
            area.openSearch();
            layoutTree(pane);
            assertTrue(bar.isFloating());
            assertEquals(topBefore, pane.getInsets().top);
            assertTrue(pane.getViewport().getBounds().contains(bar.getBounds()));
            return null;
        });
    }

    @Test
    void searchIsUnavailableOutsideSmartScrollPane() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("abc");
            assertFalse(area.openSearch());
            area.createScrollPane();
            area.setSearchEnabled(false);
            assertFalse(area.openSearch());
            return null;
        });
    }

    @Test
    void rendersWithGutterAndGrowsWithLineCount() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea(lines(99));
            area.setPlaceholder("Digite aqui");
            SmartTextAreaScrollPane pane = area.createScrollPane();
            pane.setSize(400, 300);
            layoutTree(pane);
            assertNotNull(pane.getRowHeader());

            Component gutter = pane.getRowHeader().getView();
            int narrow = gutter.getPreferredSize().width;
            area.append("\nline 100");
            assertTrue(gutter.getPreferredSize().width > narrow);

            layoutTree(pane);
            assertNotNull(area.currentLineBounds());
            paint(pane);

            area.setLineNumbersVisible(false);
            assertNull(pane.getRowHeader().getView());

            area.setText("");
            paint(pane);
            return null;
        });
    }

    private static String lines(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 1; i <= count; i++) {
            if (i > 1) builder.append('\n');
            builder.append("line ").append(i);
        }
        return builder.toString();
    }

    private static void type(SmartTextArea area, String text) {
        for (char c : text.toCharArray()) area.replaceSelection(String.valueOf(c));
    }

    private static void layoutTree(Component component) {
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) layoutTree(child);
        }
    }

    private static void paint(Component component) {
        BufferedImage image = new BufferedImage(Math.max(1, component.getWidth()), Math.max(1, component.getHeight()), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        try {
            component.paint(g2);
        } finally {
            g2.dispose();
        }
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Exception> failure = new AtomicReference<>();
        AtomicReference<Error> error = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result.set(action.call());
                } catch (Exception e) {
                    failure.set(e);
                } catch (Error e) {
                    error.set(e);
                }
            });
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
        if (error.get() != null) throw error.get();
        if (failure.get() != null) throw failure.get();
        return result.get();
    }
}
