package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.hover.HoverInfo;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Font;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorHoverSelectionTest {

    @Test
    void selectionSuppressesDocumentationByDefaultButNotOtherHoverListeners() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TestEditor editor = new TestEditor();
            AtomicInteger documentation = new AtomicInteger();
            AtomicInteger hoverEvents = new AtomicInteger();
            editor.setHoverDocumentationProvider(context -> new HoverInfo("documentation"));
            editor.documentationRequests = documentation;
            editor.addHoverListener((line, col, offset) -> hoverEvents.incrementAndGet());
            assertTrue(editor.isSuppressHoverDocumentationWhileSelecting());

            CompletableFuture<HoverInfo> pending = new CompletableFuture<>();
            editor.currentHoverTask = pending;
            editor.setSelection(0, 0, 0, 3);
            assertTrue(pending.isCancelled());
            editor.fireHoverAtStart();
            assertEquals(0, documentation.get());
            assertEquals(1, hoverEvents.get());
            editor.deliverPendingResult();
            assertEquals(0, editor.popupChecks);

            editor.setSuppressHoverDocumentationWhileSelecting(false);
            assertFalse(editor.isSuppressHoverDocumentationWhileSelecting());
            editor.fireHoverAtStart();
            assertEquals(2, hoverEvents.get());
            assertEquals(1, documentation.get());
            editor.deliverPendingResult();
            assertEquals(1, editor.popupChecks);
        });
    }

    @Test
    void activeDragSuppressesDocumentationBeforeTextIsSelected() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TestEditor editor = new TestEditor();
            editor.hoverSelectionInProgress = true;
            assertTrue(editor.isHoverDocumentationSuppressedBySelection());
            editor.setSuppressHoverDocumentationWhileSelecting(false);
            assertFalse(editor.isHoverDocumentationSuppressedBySelection());
        });
    }

    private static final class TestEditor extends CodeEditorTextArea {
        private AtomicInteger documentationRequests;
        private int popupChecks;

        private TestEditor() {
            super("object");
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        }

        private void fireHoverAtStart() {
            hoverLine = 0;
            hoverCol = 0;
            fireHoverEvent();
        }

        private void deliverPendingResult() {
            showHoverDocumentationResult(hoverDocumentationVersion.get(),
                    getBuffer().getText(), 0, 0, new HoverInfo("documentation"));
        }

        @Override
        public boolean canShowPopups() {
            popupChecks++;
            return false;
        }

        @Override
        protected void showHoverDocumentation(int line, int col) {
            documentationRequests.incrementAndGet();
        }
    }
}
