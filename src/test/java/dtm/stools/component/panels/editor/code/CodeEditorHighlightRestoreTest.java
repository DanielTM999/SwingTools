package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.provider.TokenRenderCodeEditorProvider;
import dtm.stools.component.panels.editor.code.provider.def.DefaultTokenClassifierProvider;
import dtm.stools.component.panels.editor.code.provider.def.DefaultTokenColorProvider;
import dtm.stools.component.panels.editor.code.provider.def.DefaultTokenizerProvider;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GraphicsEnvironment;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class CodeEditorHighlightRestoreTest {

    @Test
    void restoresTheHighlightCancelledByBeingAttachedInTheSameEventCycle() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "o editor precisa de ambiente grafico");
        AtomicInteger renders = new AtomicInteger();
        CodeEditor editor = editorWith(renders);
        JFrame frame = new JFrame();
        JPanel host = new JPanel(new BorderLayout());
        frame.setContentPane(host);
        frame.setSize(400, 300);

        try {
            SwingUtilities.invokeAndWait(() -> {
                frame.setVisible(true);
                editor.setText("class Demo { int size; }");
                editor.applySyntaxHighlight();
                host.add(editor, BorderLayout.CENTER);
                host.revalidate();
                host.remove(editor);
                host.revalidate();
                host.add(editor, BorderLayout.CENTER);
                host.revalidate();
            });
            awaitRenders(renders, 1);

            assertTrue(renders.get() > 0, "o destaque cancelado deveria ser refeito ao anexar");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    @Test
    void doesNotRetokenizeWhenTheHighlightIsAlreadyCurrent() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "o editor precisa de ambiente grafico");
        AtomicInteger renders = new AtomicInteger();
        CodeEditor editor = editorWith(renders);
        JFrame frame = new JFrame();
        JPanel host = new JPanel(new BorderLayout());
        frame.setContentPane(host);
        frame.setSize(400, 300);

        try {
            SwingUtilities.invokeAndWait(() -> {
                frame.setVisible(true);
                editor.setText("class Demo { int size; }");
                host.add(editor, BorderLayout.CENTER);
                host.revalidate();
            });
            awaitRenders(renders, 1);
            int settled = renders.get();

            SwingUtilities.invokeAndWait(() -> {
                host.remove(editor);
                host.revalidate();
                host.add(editor, BorderLayout.CENTER);
                host.revalidate();
            });
            Thread.sleep(400);
            SwingUtilities.invokeAndWait(() -> {
            });

            assertEquals(settled, renders.get(), "nao deveria retokenizar um destaque ja atual");
        } finally {
            SwingUtilities.invokeAndWait(frame::dispose);
        }
    }

    private static CodeEditor editorWith(AtomicInteger renders) {
        CodeEditor editor = new CodeEditor();
        editor.addProvider(new DefaultTokenizerProvider());
        editor.addProvider(new DefaultTokenClassifierProvider());
        editor.addProvider(new DefaultTokenColorProvider());
        editor.addProvider((TokenRenderCodeEditorProvider)
                (tokens, colors, area) -> renders.incrementAndGet());
        editor.setSyntaxHighlightEnabled(true);
        return editor;
    }

    private static void awaitRenders(AtomicInteger renders, int expected) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline && renders.get() < expected) {
            SwingUtilities.invokeAndWait(() -> {
            });
            Thread.sleep(50);
        }
    }
}
