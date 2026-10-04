package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.search.SearchPanel;
import dtm.stools.component.panels.editor.code.search.SearchOptions;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorSearchPanelTest {

    @Test
    void codeEditorPreservesReplaceModeWhileFindPanelIsOpen() throws Exception {
        onEdt(() -> {
            CodeEditor editor = new CodeEditor("alpha beta");
            CodeEditorTextArea area = editor.getTextArea();

            area.setSelection(0, 0, 0, 5);
            invokeShortcut(area, KeyEvent.VK_H);
            SearchPanel panel = area.getSearchPanel();
            assertTrue(panel.isReplaceVisible());
            panel.getReplaceField().setText("replacement");

            area.setSelection(0, 6, 0, 10);
            invokeShortcut(area, KeyEvent.VK_F);

            assertTrue(panel.isReplaceVisible());
            assertEquals("beta", panel.getFindField().getText());
            assertEquals("replacement", panel.getReplaceField().getText());

            editor.hideSearchPanel();
            area.setSelection(0, 0, 0, 5);
            invokeShortcut(area, KeyEvent.VK_F);
            assertFalse(panel.isReplaceVisible());

            invokeShortcut(area, KeyEvent.VK_H);
            assertTrue(panel.isReplaceVisible());

            editor.setReadOnly(true);
            invokeShortcut(area, KeyEvent.VK_H);
            assertFalse(panel.isReplaceVisible());

            area.removeNotify();
            return null;
        });
    }

    @Test
    void directTextAreaPreservesReplaceModeWhileFindPanelIsOpen() throws Exception {
        onEdt(() -> {
            CodeEditorTextArea area = new CodeEditorTextArea("alpha beta");
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            area.addSearchRequestListener((selectedText, replaceMode) -> area.showSearchPanel(replaceMode));

            area.setSelection(0, 0, 0, 5);
            invokeShortcut(area, KeyEvent.VK_H);
            SearchPanel panel = area.getSearchPanel();
            assertTrue(panel.isReplaceVisible());
            panel.getReplaceField().setText("replacement");

            area.setSelection(0, 6, 0, 10);
            invokeShortcut(area, KeyEvent.VK_F);

            assertTrue(panel.isReplaceVisible());
            assertEquals("beta", panel.getFindField().getText());
            assertEquals("replacement", panel.getReplaceField().getText());

            area.hideSearchPanel();
            area.setSelection(0, 0, 0, 5);
            invokeShortcut(area, KeyEvent.VK_F);
            assertFalse(panel.isReplaceVisible());

            invokeShortcut(area, KeyEvent.VK_H);
            assertTrue(panel.isReplaceVisible());

            area.setReadOnly(true);
            invokeShortcut(area, KeyEvent.VK_H);
            assertFalse(panel.isReplaceVisible());

            area.removeNotify();
            return null;
        });
    }

    @Test
    void searchArrowsCenterMatchesAndClampAtDocumentEdges() throws Exception {
        onEdt(() -> {
            StringBuilder text = new StringBuilder();
            for (int line = 0; line < 50; line++) {
                if (line > 0) text.append('\n');
                text.append(line == 2 || line == 20 || line == 47 ? "needle" : "other");
            }
            CodeEditorTextArea area = new CodeEditorTextArea(text.toString());
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            JViewport viewport = new JViewport();
            viewport.setView(area);
            viewport.setSize(320, 120);
            Dimension preferred = area.getPreferredSize();
            area.setSize(Math.max(preferred.width, 320), preferred.height);
            area.searchUpdateQuery("needle", new SearchOptions());

            area.searchFindNext();
            assertMatchCentered(area, viewport, 20);

            area.searchFindNext();
            assertMatchCentered(area, viewport, 47);

            area.searchFindNext();
            assertMatchCentered(area, viewport, 2);

            area.searchFindPrev();
            assertMatchCentered(area, viewport, 47);

            area.searchFindPrev();
            assertMatchCentered(area, viewport, 20);
            area.removeNotify();
            return null;
        });
    }

    private static void assertMatchCentered(CodeEditorTextArea area, JViewport viewport, int line) {
        int lineHeight = area.getFontMetrics(area.getFont()).getHeight();
        int centeredY = area.yOfBufferLine(line) + lineHeight / 2 - viewport.getExtentSize().height / 2;
        int maxY = Math.max(0, area.getHeight() - viewport.getExtentSize().height);
        assertEquals(Math.max(0, Math.min(centeredY, maxY)), viewport.getViewPosition().y);
    }

    private static void invokeShortcut(CodeEditorTextArea area, int keyCode) {
        KeyStroke stroke = KeyStroke.getKeyStroke(keyCode, InputEvent.CTRL_DOWN_MASK);
        Object actionKey = area.getInputMap(JComponent.WHEN_FOCUSED).get(stroke);
        Action action = area.getActionMap().get(actionKey);
        action.actionPerformed(new ActionEvent(area, ActionEvent.ACTION_PERFORMED, actionKey.toString()));
    }

    private static <T> T onEdt(Callable<T> call) throws Exception {
        Object[] holder = new Object[1];
        Exception[] failure = new Exception[1];
        Runnable task = () -> {
            try {
                holder[0] = call.call();
            } catch (Exception ex) {
                failure[0] = ex;
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InvocationTargetException | InterruptedException ex) {
                throw new IllegalStateException(ex);
            }
        }
        if (failure[0] != null) throw failure[0];
        @SuppressWarnings("unchecked")
        T result = (T) holder[0];
        return result;
    }
}
