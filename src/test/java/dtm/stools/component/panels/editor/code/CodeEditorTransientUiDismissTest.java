package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.hover.HoverDocumentationPopup;
import dtm.stools.component.panels.editor.code.hover.HoverInfo;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorTransientUiDismissTest {

    @Test
    void dismissIsNoOpWhenNothingIsVisible() {
        TestTextArea textArea = new TestTextArea();
        int before = textArea.autoCompleteVersion.get();

        textArea.dismissTransientUi();

        assertFalse(textArea.hasTransientUiVisible());
        assertTrue(before == textArea.autoCompleteVersion.get());
    }

    @Test
    void hidingByHierarchyDismissesHoverDocumentation() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        TestTextArea textArea = new TestTextArea();
        JFrame frame = showInFrame(textArea);
        try {
            HoverDocumentationPopup popup = onEdt(() -> {
                HoverDocumentationPopup created = textArea.hoverPopup();
                created.show(HoverInfo.markdown("documentation"), 10, 10);
                return created;
            });
            assertTrue(popup.isVisible());

            onEdt(() -> {
                textArea.setVisible(false);
                return null;
            });

            assertFalse(popup.isVisible());
        } finally {
            disposeFrame(frame);
        }
    }

    @Test
    void removeNotifyDisposesFoldPreviewWindow() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());

        TestTextArea textArea = new TestTextArea();
        JFrame frame = showInFrame(textArea);
        try {
            onEdt(() -> {
                textArea.createFoldPreviewWindow();
                return null;
            });
            assertNotNull(textArea.foldPreviewWindow);

            onEdt(() -> {
                frame.getContentPane().remove(textArea);
                frame.getContentPane().revalidate();
                return null;
            });

            assertNull(textArea.foldPreviewWindow);
        } finally {
            disposeFrame(frame);
        }
    }

    private static JFrame showInFrame(TestTextArea textArea) throws Exception {
        return onEdt(() -> {
            JFrame frame = new JFrame("CodeEditorTransientUiDismissTest");
            frame.setLayout(new BorderLayout());
            frame.getContentPane().add(textArea, BorderLayout.CENTER);
            frame.setSize(600, 400);
            frame.setVisible(true);
            return frame;
        });
    }

    private static void disposeFrame(JFrame frame) throws Exception {
        onEdt(() -> {
            frame.dispose();
            return null;
        });
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

    private static final class TestTextArea extends CodeEditorTextArea {

        private TestTextArea() {
            super("class Demo {\n    int value = 1;\n}\n");
        }

        private HoverDocumentationPopup hoverPopup() {
            return getOrCreateHoverDocumentationPopup();
        }

        private void createFoldPreviewWindow() {
            foldPreviewOwnerWindow = SwingUtilities.getWindowAncestor(this);
            foldPreviewWindow = new JWindow(foldPreviewOwnerWindow);
        }
    }
}
