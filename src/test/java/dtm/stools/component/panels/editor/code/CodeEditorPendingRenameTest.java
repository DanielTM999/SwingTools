package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.Position;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.utils.LoadingIndicator;
import dtm.stools.component.panels.editor.code.utils.LoadingSpinner;
import dtm.stools.component.panels.editor.code.utils.LoadingSpinnerContext;
import dtm.stools.component.panels.editor.code.utils.LoadingSpinnerFactory;
import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.api.TextEdit;
import dtm.stools.component.panels.editor.code.provider.RenameContext;
import dtm.stools.component.panels.editor.code.provider.RenameProvider;
import dtm.stools.component.panels.editor.code.rename.RenamePrepareContext;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorPendingRenameTest {

    private static final String SOURCE = "int nome = 1;\nnome++;";
    private static final String RENAMED = "int codigo = 1;\ncodigo++;";

    @Test
    void blocksEditingUntilTheRenameIsAppliedAndThenRunsTheSettledCallbacks() throws Exception {
        TestEditor editor = new TestEditor();
        BlockingProvider provider = new BlockingProvider();
        AtomicReference<String> textWhenSettled = new AtomicReference<>();
        CountDownLatch settled = new CountDownLatch(1);

        startRename(editor, provider);
        assertTrue(provider.started.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(editor.isRenamePending());
            editor.type(0, "x");
            assertEquals(0, editor.applyEdits(List.of(TextEdit.replace(range(0, 0, 0, 3), "long"))));
            assertEquals(SOURCE, editor.getBuffer().getText());
        });
        editor.whenRenameSettled(() -> {
            textWhenSettled.set(editor.getBuffer().getText());
            settled.countDown();
        });

        provider.release.countDown();

        assertTrue(settled.await(5, TimeUnit.SECONDS));
        assertEquals(RENAMED, textWhenSettled.get());
        assertEquals(1, provider.applied.get());
        SwingUtilities.invokeAndWait(() -> {
            assertFalse(editor.isRenamePending());
            editor.type(0, "x");
            assertEquals("x" + RENAMED, editor.getBuffer().getText());
        });
    }

    @Test
    void escapeCancelsThePendingRenameAndIgnoresTheLateResult() throws Exception {
        TestEditor editor = new TestEditor();
        BlockingProvider provider = new BlockingProvider();
        CountDownLatch settled = new CountDownLatch(1);

        startRename(editor, provider);
        assertTrue(provider.started.await(5, TimeUnit.SECONDS));
        editor.whenRenameSettled(settled::countDown);
        SwingUtilities.invokeAndWait(() -> {
            KeyEvent escape = new KeyEvent(editor, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0,
                    KeyEvent.VK_ESCAPE, KeyEvent.CHAR_UNDEFINED);
            assertTrue(editor.handleLinkedRenameKey(escape));
            assertFalse(editor.isRenamePending());
        });
        assertTrue(settled.await(5, TimeUnit.SECONDS));

        provider.release.countDown();
        assertTrue(provider.finished.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals(SOURCE, editor.getBuffer().getText());
            assertEquals(0, provider.applied.get());
        });
    }

    @Test
    void hostReplacingTheTextDiscardsTheRenameButStillSettles() throws Exception {
        TestEditor editor = new TestEditor();
        BlockingProvider provider = new BlockingProvider();
        CountDownLatch settled = new CountDownLatch(1);

        startRename(editor, provider);
        assertTrue(provider.started.await(5, TimeUnit.SECONDS));
        editor.whenRenameSettled(settled::countDown);
        SwingUtilities.invokeAndWait(() -> editor.setText("recarregado do disco"));

        provider.release.countDown();

        assertTrue(settled.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("recarregado do disco", editor.getBuffer().getText());
            assertEquals(0, provider.applied.get());
            assertFalse(editor.isRenamePending());
        });
    }

    @Test
    void settledCallbackRunsImmediatelyWithoutAPendingRename() throws Exception {
        TestEditor editor = new TestEditor();
        AtomicBoolean ran = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            editor.whenRenameSettled(() -> ran.set(true));
            assertTrue(ran.get());
        });
    }

    @Test
    void nonBlockingModeLetsTheUserTypeAndDiscardsThePendingRename() throws Exception {
        TestEditor editor = new TestEditor();
        BlockingProvider provider = new BlockingProvider();
        CountDownLatch settled = new CountDownLatch(1);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(editor.isBlockEditsWhileRenamePending());
            editor.setBlockEditsWhileRenamePending(false);
        });

        startRename(editor, provider);
        assertTrue(provider.started.await(5, TimeUnit.SECONDS));
        editor.whenRenameSettled(settled::countDown);
        SwingUtilities.invokeAndWait(() -> {
            assertTrue(editor.isRenamePending());
            editor.type(0, "x");
            assertFalse(editor.isRenamePending());
        });
        assertTrue(settled.await(5, TimeUnit.SECONDS));

        provider.release.countDown();
        assertTrue(provider.finished.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> {
            assertEquals("x" + SOURCE, editor.getBuffer().getText());
            assertEquals(0, provider.applied.get());
        });
    }

    @Test
    void loadingSpinnerFactoryReplacesTheDefaultIndicatorEverywhere() throws Exception {
        TestEditor editor = new TestEditor();
        List<LoadingSpinnerContext.Usage> usages = new CopyOnWriteArrayList<>();
        LoadingSpinnerFactory factory = context -> {
            usages.add(context.usage());
            return new FakeIndicator();
        };
        SwingUtilities.invokeAndWait(() -> {
            AutoCompletePopup popup = editor.getOrCreateAutoCompletePopup();
            assertTrue(popup.getLoadingSpinnerFactory() != null);

            editor.setLoadingSpinnerFactory(factory);

            assertTrue(popup.getLoadingIndicator().getComponent().getParent() != null);
            assertTrue(popup.getLoadingIndicator() instanceof FakeIndicator);
            LoadingIndicator rename = editor.createLoadingIndicator(
                    new LoadingSpinnerContext(LoadingSpinnerContext.Usage.RENAME, 14, null));
            assertTrue(rename instanceof FakeIndicator);
            assertEquals(List.of(LoadingSpinnerContext.Usage.AUTOCOMPLETE, LoadingSpinnerContext.Usage.RENAME), usages);

            editor.setLoadingSpinnerFactory(context -> null);
            assertTrue(editor.createLoadingIndicator(new LoadingSpinnerContext(
                    LoadingSpinnerContext.Usage.RENAME, 14, null)) instanceof LoadingSpinner);
            assertTrue(popup.getLoadingIndicator() instanceof LoadingSpinner);

            editor.setLoadingSpinnerFactory(null);
            assertTrue(editor.createLoadingIndicator(new LoadingSpinnerContext(
                    LoadingSpinnerContext.Usage.RENAME, 14, null)) instanceof LoadingSpinner);
        });
    }

    private static void startRename(TestEditor editor, RenameProvider provider) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            editor.setText(SOURCE);
            editor.executeRename(provider, new RenamePrepareContext(SOURCE, new Position(0, 4), 4, "nome"),
                    "codigo", Map.of(), false);
        });
    }

    private static Range range(int startLine, int startCol, int endLine, int endCol) {
        return new Range(new Position(startLine, startCol), new Position(endLine, endCol));
    }

    private static final class BlockingProvider implements RenameProvider {
        private final CountDownLatch started = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch finished = new CountDownLatch(1);
        private final AtomicInteger applied = new AtomicInteger();

        @Override
        public List<TextEdit> computeRenameEdits(RenameContext context) {
            started.countDown();
            try {
                release.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            finished.countDown();
            return List.of(TextEdit.replace(range(1, 0, 1, 4), "codigo"),
                    TextEdit.replace(range(0, 4, 0, 8), "codigo"));
        }

        @Override
        public void onRenameApplied(RenameContext context, List<TextEdit> appliedEdits) {
            applied.incrementAndGet();
        }
    }

    private static final class FakeIndicator implements LoadingIndicator {
        private final JLabel label = new JLabel("...");

        @Override
        public JComponent getComponent() {
            return label;
        }

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }
    }

    private static final class TestEditor extends CodeEditorTextArea {
        TestEditor() {
            setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 12));
        }

        void type(int offset, String text) {
            insertText(offset, text);
        }
    }
}
