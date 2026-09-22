package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.api.Range;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlight;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightContext;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightPalette;
import dtm.stools.component.panels.editor.code.documenthighlight.DocumentHighlightProvider;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorDocumentHighlightTest {

    @Test
    void defaultsDocumentHighlightDebounceTo300Milliseconds() {
        assertEquals(200, new TestEditor().getDocumentHighlightDebounceMs());
    }

    @Test
    void providerReceivesCaretSnapshotAndEditorOwnsResolvedHighlightState() throws Exception {
        TestEditor editor = new TestEditor();
        AtomicReference<DocumentHighlightContext> received = new AtomicReference<>();

        SwingUtilities.invokeAndWait(() -> {
            editor.setDocumentHighlightDebounceMs(0);
            editor.setText("alpha beta");
            editor.setCaretPosition(0, 2);
            editor.addProvider((DocumentHighlightProvider) context -> {
                received.set(context);
                return List.of(DocumentHighlight.read(Range.of(0, 0, 0, 5)));
            });
        });

        assertTrue(waitUntil(() -> editor.highlightCount() == 1));
        DocumentHighlightContext context = received.get();
        assertEquals("alpha beta", context.buffer().getText());
        assertEquals(0, context.line());
        assertEquals(2, context.col());
        assertEquals(2, context.offset());
        assertEquals(0, editor.firstHighlightStart());
        assertEquals(5, editor.firstHighlightEnd());
        assertEquals(DocumentHighlight.Kind.READ, editor.firstHighlightKind());
    }

    @Test
    void staleProviderResultCannotReplaceCurrentCaretHighlights() throws Exception {
        TestEditor editor = new TestEditor();
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            editor.setDocumentHighlightDebounceMs(0);
            editor.setText("first second");
            editor.setCaretPosition(0, 1);
            editor.addProvider((DocumentHighlightProvider) context -> {
                if (context.offset() == 1) {
                    firstStarted.countDown();
                    awaitIgnoringInterrupts(releaseFirst);
                    return List.of(DocumentHighlight.text(Range.of(0, 0, 0, 5)));
                }
                return List.of(DocumentHighlight.write(Range.of(0, 6, 0, 12)));
            });
        });

        assertTrue(firstStarted.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> editor.setCaretPosition(0, 8));
        assertTrue(waitUntil(() -> editor.highlightCount() == 1
                && editor.firstHighlightStart() == 6));

        releaseFirst.countDown();
        SwingUtilities.invokeAndWait(() -> { });
        assertEquals(6, editor.firstHighlightStart());
        assertEquals(DocumentHighlight.Kind.WRITE, editor.firstHighlightKind());
    }

    @Test
    void keepsPreviousHighlightsVisibleUntilReplacementIsReady() throws Exception {
        TestEditor editor = new TestEditor();
        CountDownLatch replacementStarted = new CountDownLatch(1);
        CountDownLatch releaseReplacement = new CountDownLatch(1);

        SwingUtilities.invokeAndWait(() -> {
            editor.setDocumentHighlightDebounceMs(0);
            editor.setText("first second");
            editor.setCaretPosition(0, 1);
            editor.addProvider((DocumentHighlightProvider) context -> {
                if (context.offset() == 1) {
                    return List.of(DocumentHighlight.read(Range.of(0, 0, 0, 5)));
                }
                replacementStarted.countDown();
                awaitIgnoringInterrupts(releaseReplacement);
                return List.of(DocumentHighlight.write(Range.of(0, 6, 0, 12)));
            });
        });

        assertTrue(waitUntil(() -> editor.highlightCount() == 1
                && editor.firstHighlightStart() == 0));
        SwingUtilities.invokeAndWait(() -> editor.setCaretPosition(0, 8));
        assertTrue(replacementStarted.await(5, TimeUnit.SECONDS));

        assertEquals(1, editor.highlightCount());
        assertEquals(0, editor.firstHighlightStart());
        assertEquals(DocumentHighlight.Kind.READ, editor.firstHighlightKind());

        releaseReplacement.countDown();
        assertTrue(waitUntil(() -> editor.highlightCount() == 1
                && editor.firstHighlightStart() == 6));
        assertEquals(DocumentHighlight.Kind.WRITE, editor.firstHighlightKind());
    }

    @Test
    void disablingClearsHighlightsAndPaletteFallsBackPerMissingColor() throws Exception {
        TestEditor editor = new TestEditor();
        SwingUtilities.invokeAndWait(() -> {
            editor.setDocumentHighlightDebounceMs(0);
            editor.setText("value");
            editor.addProvider((DocumentHighlightProvider) context ->
                    List.of(DocumentHighlight.text(Range.of(0, 0, 0, 5))));
        });
        assertTrue(waitUntil(() -> editor.highlightCount() == 1));

        Color custom = new Color(1, 2, 3, 4);
        SwingUtilities.invokeAndWait(() -> {
            editor.setDocumentHighlightPalette(new DocumentHighlightPalette(custom, null, null));
            editor.setDocumentHighlightsEnabled(false);
        });

        assertEquals(0, editor.highlightCount());
        assertEquals(custom, editor.getDocumentHighlightPalette().text());
        assertEquals(DocumentHighlightPalette.defaults().read(), editor.getDocumentHighlightPalette().read());
    }

    @Test
    void syntaxRangeReplacementDoesNotOwnHighlightsAndRemoveNotifyClearsThem() throws Exception {
        TestEditor editor = new TestEditor();
        SwingUtilities.invokeAndWait(() -> {
            editor.setDocumentHighlightDebounceMs(0);
            editor.setText("value");
            editor.addProvider((DocumentHighlightProvider) context ->
                    List.of(DocumentHighlight.text(Range.of(0, 0, 0, 5))));
        });
        assertTrue(waitUntil(() -> editor.highlightCount() == 1));

        SwingUtilities.invokeAndWait(() -> editor.replaceStyledRanges(List.of()));
        assertEquals(1, editor.highlightCount());

        SwingUtilities.invokeAndWait(editor::removeNotify);
        assertEquals(0, editor.highlightCount());
    }

    private static boolean waitUntil(CheckedBoolean condition) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            AtomicReference<Boolean> result = new AtomicReference<>(false);
            SwingUtilities.invokeAndWait(() -> result.set(condition.get()));
            if (result.get()) return true;
            Thread.sleep(10);
        }
        return false;
    }

    private static void awaitIgnoringInterrupts(CountDownLatch latch) {
        boolean interrupted = false;
        while (latch.getCount() > 0) {
            try {
                latch.await();
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }
        if (interrupted) Thread.currentThread().interrupt();
    }

    @FunctionalInterface
    private interface CheckedBoolean {
        boolean get();
    }

    private static final class TestEditor extends CodeEditorTextArea {
        private TestEditor() {
            setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        }

        int highlightCount() {
            return resolvedDocumentHighlights.size();
        }

        int firstHighlightStart() {
            return resolvedDocumentHighlights.getFirst().startOffset();
        }

        int firstHighlightEnd() {
            return resolvedDocumentHighlights.getFirst().endOffset();
        }

        DocumentHighlight.Kind firstHighlightKind() {
            return resolvedDocumentHighlights.getFirst().kind();
        }
    }
}
