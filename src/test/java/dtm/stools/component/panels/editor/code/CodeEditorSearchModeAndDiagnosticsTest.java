package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticSeverity;
import dtm.stools.component.panels.editor.code.search.SearchPanel;
import dtm.stools.models.SearchPanelMode;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorSearchModeAndDiagnosticsTest {

    @Test
    void searchPanelDefaultsToEmbedded() throws Exception {
        onEdt(() -> {
            CodeEditor editor = new CodeEditor("alpha beta");
            assertEquals(SearchPanelMode.EMBEDDED, editor.getSearchPanelMode());
            invokeShortcut(editor.getTextArea(), KeyEvent.VK_F, ctrl());
            SearchPanel panel = editor.getTextArea().getSearchPanel();
            assertFalse(panel.isFloating());
            assertFalse(panel.getParent() instanceof JLayeredPane);
            assertTrue(panel.isOpaque());
            return null;
        });
    }

    @Test
    void floatingModeMountsCardOverTheViewport() throws Exception {
        onEdt(() -> {
            CodeEditor editor = new CodeEditor("alpha beta\ngamma");
            editor.setSearchPanelMode(SearchPanelMode.FLOATING);
            editor.setSize(900, 400);
            layoutTree(editor);

            invokeShortcut(editor.getTextArea(), KeyEvent.VK_F, ctrl());
            SearchPanel panel = editor.getTextArea().getSearchPanel();
            assertTrue(panel.isFloating());
            assertFalse(panel.isOpaque());
            JLayeredPane overlay = assertInstanceOf(JLayeredPane.class, panel.getParent());
            assertEquals(JLayeredPane.MODAL_LAYER, overlay.getLayer(panel));

            layoutTree(editor);
            CodeEditorScrollPane scrollPane = editor.getScrollPane();
            Rectangle viewport = SwingUtilities.convertRectangle(scrollPane, scrollPane.getViewport().getBounds(), overlay);
            Rectangle card = panel.getBounds();
            assertTrue(card.width > 0 && card.height > 0);
            assertTrue(viewport.contains(card), viewport + " should contain " + card);

            Component widget = editor.getInspectionWidget();
            if (widget.isVisible()) assertTrue(widget.getY() >= card.y + card.height);

            editor.getTextArea().hideSearchPanel();
            assertFalse(panel.isVisible());
            layoutTree(editor);

            editor.setSearchPanelMode(SearchPanelMode.EMBEDDED);
            invokeShortcut(editor.getTextArea(), KeyEvent.VK_F, ctrl());
            assertFalse(panel.isFloating());
            assertFalse(panel.getParent() instanceof JLayeredPane);
            return null;
        });
    }

    @Test
    void switchingModeWhileOpenRemountsThePanel() throws Exception {
        onEdt(() -> {
            CodeEditor editor = new CodeEditor("alpha beta");
            invokeShortcut(editor.getTextArea(), KeyEvent.VK_H, ctrl());
            SearchPanel panel = editor.getTextArea().getSearchPanel();
            assertTrue(panel.isReplaceVisible());

            editor.setSearchPanelMode(SearchPanelMode.FLOATING);
            assertTrue(panel.isVisible());
            assertTrue(panel.isFloating());
            assertTrue(panel.isReplaceVisible());
            assertInstanceOf(JLayeredPane.class, panel.getParent());
            return null;
        });
    }

    @Test
    void f2CyclesThroughDiagnosticsSkippingFadeOnly() throws Exception {
        onEdt(() -> {
            CodeEditor editor = new CodeEditor("aaa bbb\nccc ddd\neee fff");
            CodeEditorTextArea area = editor.getTextArea();
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            area.getDiagnostics().addAll(List.of(
                    new Diagnostic(2, 4, 2, 7, DiagnosticSeverity.ERROR, "erro", "teste", null),
                    new Diagnostic(0, 4, 0, 7, DiagnosticSeverity.WARNING, "aviso", null, null),
                    new Diagnostic(1, 0, 1, 3, DiagnosticSeverity.HINT, "apagado", null, null, true)));
            area.setCaretPosition(0, 0);

            invokeShortcut(area, KeyEvent.VK_F2, 0);
            assertSelection(editor, 4, 7);

            invokeShortcut(area, KeyEvent.VK_F2, 0);
            assertSelection(editor, 20, 23);

            invokeShortcut(area, KeyEvent.VK_F2, 0);
            assertSelection(editor, 4, 7);

            invokeShortcut(area, KeyEvent.VK_F2, InputEvent.SHIFT_DOWN_MASK);
            assertSelection(editor, 20, 23);

            invokeShortcut(area, KeyEvent.VK_F2, InputEvent.SHIFT_DOWN_MASK);
            assertSelection(editor, 4, 7);
            return null;
        });
    }

    @Test
    void diagnosticHoverHtmlEscapesMessage() throws Exception {
        onEdt(() -> {
            CodeEditorTextArea area = new CodeEditorTextArea("x");
            String html = area.diagnosticHoverHtml(new Diagnostic(0, 0, 0, 1, DiagnosticSeverity.ERROR, "a < b", "lint", null));
            assertTrue(html.contains("a &lt; b"));
            assertTrue(html.contains("lint"));
            assertTrue(html.startsWith("<b>"));
            return null;
        });
    }

    @Test
    void diagnosticKeysAreConfigurableAndBookmarksKeepF8() throws Exception {
        onEdt(() -> {
            CodeEditor editor = new CodeEditor("abc");
            CodeEditorTextArea area = editor.getTextArea();
            assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), editor.getNextDiagnosticKeyStroke());
            assertEquals(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), area.getToggleBookmarkKeyStroke());
            assertEquals(CodeEditorTextAreaState.ACTION_TOGGLE_BOOKMARK,
                    area.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0)));

            KeyStroke custom = KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0);
            editor.setNextDiagnosticKeyStroke(custom);
            assertEquals(CodeEditorTextAreaState.ACTION_NEXT_DIAGNOSTIC, area.getInputMap(JComponent.WHEN_FOCUSED).get(custom));
            assertEquals(null, area.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)));
            return null;
        });
    }

    private static void assertSelection(CodeEditor editor, int start, int end) {
        assertEquals(start, editor.getSelectionStartOffset());
        assertEquals(end, editor.getSelectionEndOffset());
    }

    private static int ctrl() {
        return InputEvent.CTRL_DOWN_MASK;
    }

    private static void invokeShortcut(CodeEditorTextArea area, int keyCode, int modifiers) {
        KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        Object key = area.getInputMap(JComponent.WHEN_FOCUSED).get(stroke);
        assertNotNull(key, "no binding for " + stroke);
        Action action = area.getActionMap().get(key);
        assertNotNull(action, "no action for " + key);
        action.actionPerformed(new ActionEvent(area, ActionEvent.ACTION_PERFORMED, String.valueOf(key)));
    }

    private static void layoutTree(Component component) {
        component.doLayout();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) layoutTree(child);
        }
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        Object[] result = new Object[1];
        Exception[] failure = new Exception[1];
        Error[] error = new Error[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result[0] = action.call();
                } catch (Exception e) {
                    failure[0] = e;
                } catch (Error e) {
                    error[0] = e;
                }
            });
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        }
        if (error[0] != null) throw error[0];
        if (failure[0] != null) throw failure[0];
        @SuppressWarnings("unchecked")
        T value = (T) result[0];
        return value;
    }
}
