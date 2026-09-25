package dtm.stools.component.panels.editor;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.provider.SheetAiProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetAiRequest;
import dtm.stools.component.panels.editor.sheet.provider.SheetAiResponse;
import dtm.stools.component.panels.editor.sheet.provider.SheetProvider;
import dtm.stools.component.panels.editor.sheet.ui.popup.AiAssistantPanel;
import dtm.stools.component.panels.editor.word.WordEditor;
import dtm.stools.component.panels.editor.word.provider.WordProvider;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JTextArea;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class EditorLifecycleTest {
    private static <T> T edt(java.util.concurrent.Callable<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { result.set(action.call()); } catch (Throwable error) { failure.set(error); }
        });
        if (failure.get() != null) throw new AssertionError(failure.get());
        return result.get();
    }

    private static Object field(Object owner, String name) throws Exception {
        Field field = owner.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(owner);
    }

    private static <T extends Component> T find(Container parent, Class<T> type, java.util.function.Predicate<T> match) {
        for (Component component : parent.getComponents()) {
            if (type.isInstance(component) && match.test(type.cast(component))) return type.cast(component);
            if (component instanceof Container nested) {
                T found = find(nested, type, match);
                if (found != null) return found;
            }
        }
        return null;
    }

    @Test void sheetPausesVisualTimersButKeepsSessionAndRecovery() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        JFrame frame = edt(JFrame::new);
        SheetEditor editor = edt(SheetEditor::new);
        try {
            edt(() -> {
                editor.addProvider(new SheetProvider() { public String id() { return "lifecycle.sheet"; } });
                frame.add(editor); frame.setSize(900, 650); frame.setVisible(true);
                assertTrue(((Timer) field(editor.getCanvas(), "marquee")).isRunning());
                assertTrue(((Timer) field(editor, "recoveryTimer")).isRunning());
                frame.setVisible(false);
                assertFalse(((Timer) field(editor.getCanvas(), "marquee")).isRunning());
                assertTrue(((Timer) field(editor, "recoveryTimer")).isRunning());
                editor.input("A1", "42");
                assertEquals(1, editor.getProviders().size());
                frame.setVisible(true);
                assertTrue(((Timer) field(editor.getCanvas(), "marquee")).isRunning());
                frame.remove(editor);
                assertFalse(((Timer) field(editor.getCanvas(), "marquee")).isRunning());
                frame.add(editor); frame.validate();
                assertEquals("42", editor.getText("A1"));
                return null;
            });
            edt(() -> {
                assertTrue(((Timer) field(editor.getCanvas(), "marquee")).isRunning());
                editor.close(); editor.close();
                assertFalse(((Timer) field(editor.getCanvas(), "marquee")).isRunning());
                assertFalse(((Timer) field(editor, "recoveryTimer")).isRunning());
                return null;
            });
        } finally { edt(() -> { editor.close(); frame.dispose(); return null; }); }
    }

    @Test void wordRecreatesLayoutWorkerAndPreservesProviders() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        JFrame frame = edt(JFrame::new);
        WordEditor editor = edt(WordEditor::new);
        try {
            CountDownLatch refreshed = new CountDownLatch(1);
            edt(() -> {
                editor.addProvider(new WordProvider() { public String id() { return "lifecycle.word"; } });
                frame.add(editor); frame.setSize(900, 650); frame.setVisible(true);
                editor.getCanvas().scheduleLayout();
                ExecutorService first = (ExecutorService) field(editor.getCanvas(), "executor");
                assertNotNull(first);
                frame.setVisible(false);
                assertTrue(first.isShutdown());
                assertNull(field(editor.getCanvas(), "executor"));
                editor.setText("Changed while hidden");
                assertFalse(editor.getCanvas().isLayoutCurrent());
                editor.getCanvas().addPropertyChangeListener("layoutSnapshot", e -> refreshed.countDown());
                frame.setVisible(true);
                assertNotNull(field(editor.getCanvas(), "executor"));
                assertTrue(editor.getProvider("lifecycle.word").isPresent());
                return null;
            });
            assertTrue(refreshed.await(10, TimeUnit.SECONDS));
            edt(() -> {
                assertTrue(editor.getCanvas().isLayoutCurrent());
                frame.remove(editor);
                assertNull(field(editor.getCanvas(), "executor"));
                frame.add(editor); frame.validate();
                editor.close(); editor.close();
                assertNull(field(editor.getCanvas(), "executor"));
                return null;
            });
        } finally { edt(() -> { editor.close(); frame.dispose(); return null; }); }
    }

    @Test void sheetAiRequestSurvivesHidingItsDialog() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        JFrame frame = edt(JFrame::new);
        SheetEditor editor = edt(SheetEditor::new);
        CompletableFuture<SheetAiResponse> answer = new CompletableFuture<>();
        AtomicReference<SheetAiRequest> request = new AtomicReference<>();
        SheetAiProvider provider = new SheetAiProvider() {
            public String id() { return "lifecycle.ai"; }
            public java.util.concurrent.CompletionStage<SheetAiResponse> ask(SheetAiRequest value) {
                request.set(value);
                return answer;
            }
        };
        try {
            edt(() -> {
                editor.addProvider(provider);
                frame.add(editor); frame.setSize(900, 650); frame.setVisible(true);
                AiAssistantPanel.open(editor, provider);
                Object aiState = editor.getClientProperty(AiAssistantPanel.class.getName() + ".state");
                Dialog dialog = (Dialog) field(aiState, "dialog");
                JButton send = find(dialog, JButton.class, b -> "Enviar".equals(b.getText()));
                assertNotNull(send);
                send.doClick();
                assertNotNull(request.get());
                frame.setVisible(false);
                assertFalse(request.get().cancelled().getAsBoolean());
                return null;
            });
            answer.complete(new SheetAiResponse("Resposta mantida", null, null));
            edt(() -> {
                frame.setVisible(true);
                AiAssistantPanel.open(editor, provider);
                return null;
            });
            String shown = "";
            for (int i = 0; i < 100; i++) {
                shown = edt(() -> {
                    Object aiState = editor.getClientProperty(AiAssistantPanel.class.getName() + ".state");
                    Dialog dialog = (Dialog) field(aiState, "dialog");
                    JTextArea result = find(dialog, JTextArea.class, area -> !area.isEditable());
                    return result == null ? "" : result.getText();
                });
                if ("Resposta mantida".equals(shown)) break;
                Thread.sleep(20);
            }
            assertEquals("Resposta mantida", shown);
            edt(() -> {
                editor.close();
                assertTrue(request.get().cancelled().getAsBoolean());
                return null;
            });
        } finally { edt(() -> { editor.close(); frame.dispose(); return null; }); }
    }
}
