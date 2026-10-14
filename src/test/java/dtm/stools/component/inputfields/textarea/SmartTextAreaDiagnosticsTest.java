package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textarea.diagnostic.DiagnosticSeverity;
import dtm.stools.component.inputfields.textarea.diagnostic.DictionarySpellChecker;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnostic;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnosticAction;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnosticProvider;
import org.junit.jupiter.api.Test;

import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartTextAreaDiagnosticsTest {

    private static final TextDiagnosticProvider BAD_WORDS = text -> {
        List<TextDiagnostic> found = new ArrayList<>();
        Matcher matcher = Pattern.compile("bad").matcher(text);
        while (matcher.find()) found.add(TextDiagnostic.warning(matcher.start(), matcher.end(), "evite 'bad'"));
        return found;
    };

    @Test
    void providerResultsFollowEdits() throws Exception {
        SmartTextArea area = onEdt(() -> new SmartTextArea("a bad day").addDiagnosticProvider(BAD_WORDS));
        runDiagnostics(area);
        onEdt(() -> {
            List<TextDiagnostic> diagnostics = area.getDiagnostics();
            assertEquals(1, diagnostics.size());
            assertEquals(2, diagnostics.getFirst().start());
            assertEquals(5, diagnostics.getFirst().end());
            assertEquals(DiagnosticSeverity.WARNING, diagnostics.getFirst().severity());

            area.getDocument().insertString(0, "very ", null);
            TextDiagnostic shifted = area.getDiagnostics().getFirst();
            assertEquals(7, shifted.start());
            assertEquals(10, shifted.end());
            assertEquals(1, area.getDiagnosticsAt(8).size());
            assertTrue(area.getDiagnosticsAt(1).isEmpty());
            return null;
        });
    }

    @Test
    void staleResultsAreDiscarded() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        TextDiagnosticProvider slow = text -> {
            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            return List.of(TextDiagnostic.error(0, 1, "x"));
        };
        SmartTextArea area = onEdt(() -> new SmartTextArea("abc").addDiagnosticProvider(slow).setDiagnosticsDelay(60_000));
        Future<?> pending = onEdt(() -> {
            area.runDiagnostics();
            Future<?> future = area.pendingDiagnostics();
            area.replaceSelection("d");
            return future;
        });
        release.countDown();
        waitFor(pending);
        onEdt(() -> {
            assertTrue(area.getDiagnostics().isEmpty());
            return null;
        });
    }

    @Test
    void manualDiagnosticsAndEvents() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("linha um\nlinha dois");
            AtomicInteger events = new AtomicInteger();
            area.addEventListener(SmartTextArea.DIAGNOSTICS_CHANGED, e -> events.incrementAndGet());
            area.setDiagnostics(List.of(
                    TextDiagnostic.info(0, 5, "info"),
                    TextDiagnostic.error(9, 14, "erro"),
                    TextDiagnostic.hint(15, 15, "vazio vira 1 char")));
            assertEquals(3, area.getDiagnostics().size());
            assertEquals(1, events.get());

            assertEquals(DiagnosticSeverity.INFO, area.diagnosticSeverityByLine().get(0));
            assertEquals(DiagnosticSeverity.ERROR, area.diagnosticSeverityByLine().get(1));

            area.setCaretPosition(0);
            area.nextDiagnostic();
            assertEquals(0, area.getSelectionStart());
            area.nextDiagnostic();
            assertEquals(9, area.getSelectionStart());
            area.previousDiagnostic();
            assertEquals(0, area.getSelectionStart());

            area.clearDiagnostics();
            assertTrue(area.getDiagnostics().isEmpty());
            return null;
        });
    }

    @Test
    void replaceActionFixesAndUndoesInOneStep() throws Exception {
        DictionarySpellChecker checker = new DictionarySpellChecker(List.of("casa", "bonita"));
        SmartTextArea area = onEdt(() -> new SmartTextArea("casa Bonnita").addDiagnosticProvider(checker));
        runDiagnostics(area);
        onEdt(() -> {
            TextAreaDiagnosticEntry entry = area.diagnosticEntriesAt(6).getFirst();
            TextDiagnosticAction fix = entry.diagnostic().actions().getFirst();
            assertEquals("Bonita", fix.getLabel());
            area.applyDiagnosticAction(entry, fix);
            assertEquals("casa Bonita", area.getText());
            assertTrue(area.getDiagnostics().isEmpty());

            area.undo();
            assertEquals("casa Bonnita", area.getText());
            return null;
        });
    }

    @Test
    void addToDictionaryActionClearsDiagnostic() throws Exception {
        DictionarySpellChecker checker = new DictionarySpellChecker(List.of("ola"));
        SmartTextArea area = onEdt(() -> new SmartTextArea("ola mundo").addDiagnosticProvider(checker));
        runDiagnostics(area);
        Future<?> rerun = onEdt(() -> {
            TextAreaDiagnosticEntry entry = area.diagnosticEntriesAt(5).getFirst();
            TextDiagnosticAction add = entry.diagnostic().actions().getLast();
            area.applyDiagnosticAction(entry, add);
            return area.pendingDiagnostics();
        });
        waitFor(rerun);
        onEdt(() -> {
            assertTrue(checker.isKnown("mundo"));
            assertTrue(area.getDiagnostics().isEmpty());
            return null;
        });
    }

    @Test
    void spellCheckerRules() throws Exception {
        String dictionary = "3\nguarda/S\nchuva\nvocê\n# comentario\ncasa\n";
        DictionarySpellChecker checker = DictionarySpellChecker.fromStream(
                new ByteArrayInputStream(dictionary.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        assertEquals(4, checker.size());
        assertTrue(checker.isKnown("Guarda-chuva"));
        assertTrue(checker.isKnown("VOCÊ"));
        assertFalse(checker.isKnown("voce"));
        assertEquals("você", checker.suggest("voce").getFirst());
        assertEquals("Casa", checker.suggest("Csaa").getFirst());

        List<TextDiagnostic> diagnostics = checker.diagnose("Voce mora na casa? HTML x");
        assertEquals(List.of("Voce", "mora", "na"), diagnostics.stream().map(d -> "Voce mora na casa? HTML x".substring(d.start(), d.end())).toList());
        assertEquals(DiagnosticSeverity.ERROR, diagnostics.getFirst().severity());
        assertTrue(checker.suggest("xyzxyzxyz").isEmpty());
    }

    @Test
    void tooltipShowsDiagnosticUnderMouse() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("tudo certo erro aqui");
            SmartTextAreaScrollPane pane = area.createScrollPane();
            pane.setSize(400, 200);
            layoutTree(pane);
            area.setDiagnostics(List.of(TextDiagnostic.error(11, 15, "mensagem <x>").withSource("teste")));
            Rectangle2D start = area.modelToView2D(12);
            Point inside = new Point((int) start.getX() + 1, (int) start.getCenterY());
            String tooltip = area.getToolTipText(mouseAt(area, inside));
            assertNotNull(tooltip);
            assertTrue(tooltip.contains("mensagem &lt;x&gt;"));
            assertTrue(tooltip.contains("teste"));

            Rectangle2D outside = area.modelToView2D(2);
            assertEquals(null, area.getToolTipText(mouseAt(area, new Point((int) outside.getX(), (int) outside.getCenterY()))));
            paint(pane);
            return null;
        });
    }

    @Test
    void resizeGripChangesPreferredSize() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("texto");
            SmartTextAreaScrollPane pane = area.createScrollPane();
            pane.setSize(300, 150);
            layoutTree(pane);
            TextAreaResizeGrip grip = pane.getResizeGrip();
            assertEquals(TextAreaResize.BOTH, area.getResize());
            assertTrue(grip.isVisible());
            assertEquals(300 - pane.getInsets().right - grip.getPreferredSize().width, grip.getX());

            AtomicReference<Object> resized = new AtomicReference<>();
            area.addEventListener(EventType.RESIZE, e -> resized.set(e.getValue()));
            grip.begin(new Point());
            grip.resizeBy(40, 30);
            assertEquals(new Dimension(340, 180), pane.getPreferredSize());
            assertEquals(new Dimension(340, 180), resized.get());

            grip.resizeBy(-1000, -1000);
            assertTrue(pane.getPreferredSize().width >= 80);
            assertTrue(pane.getPreferredSize().height > 20);

            area.setResize(TextAreaResize.VERTICAL);
            pane.setPreferredSize(new Dimension(200, 100));
            grip.begin(new Point());
            grip.resizeBy(50, 50);
            assertEquals(200, pane.getPreferredSize().width);
            assertEquals(200, pane.getPreferredSize().height);

            grip.restore();
            assertFalse(pane.isPreferredSizeSet());

            area.setResize(TextAreaResize.NONE);
            assertFalse(grip.isVisible());
            return null;
        });
    }

    @Test
    void gripDoesNotOverlapVerticalScrollBar() throws Exception {
        onEdt(() -> {
            SmartTextArea area = new SmartTextArea("1\n2\n3\n4\n5\n6\n7\n8\n9\n10\n11\n12\n13\n14\n15\n16\n17\n18\n19\n20");
            SmartTextAreaScrollPane pane = area.createScrollPane();
            pane.setSize(300, 120);
            layoutTree(pane);
            layoutTree(pane);
            JScrollBar vertical = pane.getVerticalScrollBar();
            assertTrue(vertical.isVisible());
            assertTrue(vertical.getY() + vertical.getHeight() <= pane.getResizeGrip().getY());
            return null;
        });
    }

    private static MouseEvent mouseAt(Component component, Point point) {
        return new MouseEvent(component, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.x, point.y, 0, false);
    }

    private static void runDiagnostics(SmartTextArea area) throws Exception {
        Future<?> pending = onEdt(() -> {
            area.runDiagnostics();
            return area.pendingDiagnostics();
        });
        waitFor(pending);
    }

    private static void waitFor(Future<?> pending) throws Exception {
        if (pending != null) {
            try {
                pending.get(5, TimeUnit.SECONDS);
            } catch (java.util.concurrent.CancellationException ignored) {
            }
        }
        SwingUtilities.invokeAndWait(() -> {
        });
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
