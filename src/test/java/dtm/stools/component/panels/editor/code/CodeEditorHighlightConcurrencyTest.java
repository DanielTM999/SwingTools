package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.prototype.Token;
import dtm.stools.component.panels.editor.code.prototype.constants.TokenType;
import dtm.stools.component.panels.editor.code.provider.TokenClassifierCodeEditorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenColorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenRenderCodeEditorProvider;
import dtm.stools.component.panels.editor.code.provider.TokenizerCodeEditorProvider;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodeEditorHighlightConcurrencyTest {

    @Test
    void defaultsSyntaxHighlightDebounceTo75Milliseconds() {
        assertEquals(75, new CodeEditorTextArea().getSyntaxHighlightDebounceMs());
    }

    @Test
    void discardsAStaleTokenizationAndRendersTheCurrentSnapshotOnTheEdt() throws Exception {
        CodeEditorTextArea editor = new CodeEditorTextArea();
        CountDownLatch firstTokenizationStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstTokenization = new CountDownLatch(1);
        CountDownLatch currentSnapshotRendered = new CountDownLatch(1);
        List<String> renderedTexts = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean renderedOutsideEdt = new AtomicBoolean();

        TokenizerCodeEditorProvider tokenizer = (text, classifier) -> {
            if ("old".equals(text)) {
                firstTokenizationStarted.countDown();
                awaitIgnoringInterrupts(releaseFirstTokenization);
            }
            return text.isEmpty()
                    ? List.of()
                    : List.of(new Token(0, text.length(), TokenType.IDENTIFIER, text));
        };
        TokenClassifierCodeEditorProvider classifier = token -> TokenType.IDENTIFIER;
        TokenColorProvider colors = type -> Color.WHITE;
        TokenRenderCodeEditorProvider renderer = (tokens, colorProvider, textArea) -> {
            if (!SwingUtilities.isEventDispatchThread()) {
                renderedOutsideEdt.set(true);
            }
            String rendered = tokens.isEmpty() ? "" : tokens.iterator().next().getText();
            renderedTexts.add(rendered);
            if ("new".equals(rendered)) {
                currentSnapshotRendered.countDown();
            }
        };

        SwingUtilities.invokeAndWait(() -> {
            editor.setSyntaxHighlightDebounceMs(0);
            editor.addProvider(tokenizer);
            editor.addProvider(classifier);
            editor.addProvider(colors);
            editor.addProvider(renderer);
            editor.setText("old");
        });
        assertTrue(firstTokenizationStarted.await(5, TimeUnit.SECONDS));

        SwingUtilities.invokeAndWait(() -> editor.setText("new"));
        releaseFirstTokenization.countDown();

        assertTrue(currentSnapshotRendered.await(5, TimeUnit.SECONDS));
        SwingUtilities.invokeAndWait(() -> { });

        assertEquals(List.of("new"), renderedTexts);
        assertTrue(!renderedOutsideEdt.get(), "o renderer deve executar na EDT");
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
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
