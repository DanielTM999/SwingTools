package dtm.stools.component.inputfields.textfield;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.layout.MaterialLayout;
import dtm.stools.configs.UiTokens;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MaterialLayoutTest {
    @Test
    void layoutIsOptionalAndDetachingRestoresNativePresentation() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener(20);
            assertNull(field.getFieldLayoutManager());
            Insets originalInsets = field.getInsets();
            Dimension originalSize = field.getPreferredSize();
            var originalUI = field.getUI();
            var originalBorder = field.getBorder();
            boolean opaque = field.isOpaque();
            field.setLabel("Nome");
            assertEquals(originalSize, field.getPreferredSize());
            field.setFieldLayoutManager(new MaterialLayout());
            assertSame(originalUI, field.getUI());
            assertSame(originalBorder, field.getBorder());
            assertFalse(field.isOpaque());
            assertTrue(field.getPreferredSize().height > originalSize.height);
            field.setFieldLayoutManager(null);
            assertEquals(opaque, field.isOpaque());
            assertEquals(originalInsets, field.getInsets());
            assertEquals(originalSize, field.getPreferredSize());
            assertSame(originalUI, field.getUI());
            assertSame(originalBorder, field.getBorder());
            return null;
        });
    }

    @Test
    void layoutReplacementDoesNotLeakListenersOrAccessibleErrors() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener();
            field.getAccessibleContext().setAccessibleDescription("Ajuda original");
            int focus = field.getFocusListeners().length;
            int component = field.getComponentListeners().length;
            int properties = field.getPropertyChangeListeners().length;
            int document = ((AbstractDocument) field.getDocument()).getDocumentListeners().length;
            MaterialLayout first = new MaterialLayout().setError("Primeiro erro");
            MaterialLayout second = new MaterialLayout().setError("Segundo erro");
            for (int i = 0; i < 3; i++) {
                field.setFieldLayoutManager(first);
                field.setFieldLayoutManager(first);
                assertEquals(focus + 1, field.getFocusListeners().length);
                assertEquals(component + 1, field.getComponentListeners().length);
                assertEquals(properties + 1, field.getPropertyChangeListeners().length);
                assertEquals(document, ((AbstractDocument) field.getDocument()).getDocumentListeners().length);
                field.setFieldLayoutManager(second);
                assertEquals("Segundo erro", field.getAccessibleContext().getAccessibleDescription());
                first.setError("Layout desconectado");
                assertEquals("Segundo erro", field.getAccessibleContext().getAccessibleDescription());
                field.setFieldLayoutManager(null);
                assertEquals("Ajuda original", field.getAccessibleContext().getAccessibleDescription());
                assertEquals(focus, field.getFocusListeners().length);
                assertEquals(component, field.getComponentListeners().length);
                assertEquals(properties, field.getPropertyChangeListeners().length);
            }
            return null;
        });
    }

    @Test
    void rejectsSharingAndPreservesPreviousLayoutOnFailure() throws Exception {
        onEdt(() -> {
            JTextFieldListener first = new JTextFieldListener();
            JTextFieldListener second = new JTextFieldListener();
            MaterialLayout shared = new MaterialLayout().setError("Erro A");
            MaterialLayout existing = new MaterialLayout().setError("Erro B");
            first.setFieldLayoutManager(shared);
            second.setFieldLayoutManager(existing);
            assertThrows(IllegalStateException.class, () -> second.setFieldLayoutManager(shared));
            assertSame(existing, second.getFieldLayoutManager());
            assertEquals("Erro B", second.getAccessibleContext().getAccessibleDescription());
            assertEquals("Erro A", first.getAccessibleContext().getAccessibleDescription());
            first.setFieldLayoutManager(null);
            second.setFieldLayoutManager(shared);
            assertSame(shared, second.getFieldLayoutManager());
            assertEquals("Erro A", second.getAccessibleContext().getAccessibleDescription());
            second.setFieldLayoutManager(null);
            return null;
        });
    }

    @Test
    void labelFollowsFocusAndProgrammaticText() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener();
            TestLayout layout = attach(field, "Nome");
            assertEquals(0f, layout.progress());
            focus(field, true);
            assertEquals(1f, layout.progress());
            focus(field, false);
            assertEquals(0f, layout.progress());
            field.setText("Daniel");
            assertEquals(1f, layout.progress());
            focus(field, true);
            focus(field, false);
            assertEquals(1f, layout.progress());
            field.setText(null);
            assertEquals(0f, layout.progress());
            return null;
        });
    }

    @Test
    void maskHintWaitsForRaisedLabelAndUsesCleanValue() throws Exception {
        onEdt(() -> {
            MaskedTextField field = new MaskedTextField("###.###.###-##", 20);
            TestLayout layout = attach(field, "CPF");
            assertTrue(field.isFieldContentEmpty());
            assertEquals(0f, layout.progress());
            assertFalse(layout.isPlaceholderVisible(field));
            focus(field, true);
            assertTrue(layout.isPlaceholderVisible(field));
            field.setText("123");
            assertEquals("123", field.getCleanText());
            focus(field, false);
            assertEquals(1f, layout.progress());
            field.setCleanText("");
            assertEquals(0f, layout.progress());
            field.setLabel("");
            assertTrue(layout.isPlaceholderVisible(field));
            return null;
        });
    }

    @Test
    void documentReplacementDetachesOldDocumentAndDoesNotAddInputEvents() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener();
            TestLayout layout = attach(field, "Texto");
            PlainDocument old = (PlainDocument) field.getDocument();
            PlainDocument replacement = new PlainDocument();
            replacement.insertString(0, "Novo", null);
            AtomicInteger inputs = new AtomicInteger();
            field.addEventListener(EventType.INPUT, e -> inputs.incrementAndGet());
            field.setDocument(replacement);
            assertEquals(1f, layout.progress());
            replacement.remove(0, replacement.getLength());
            assertEquals(0f, layout.progress());
            old.insertString(0, "Antigo", null);
            assertEquals(0f, layout.progress());
            assertEquals(0, inputs.get());
            return null;
        });
    }

    @Test
    void preservesMaskedInputAndSubmitEventCounts() throws Exception {
        onEdt(() -> {
            MaskedTextField field = new MaskedTextField("###", 10);
            AtomicInteger inputs = new AtomicInteger();
            AtomicInteger submits = new AtomicInteger();
            field.addEventListener(EventType.INPUT, e -> inputs.incrementAndGet());
            field.addEventListener(EventType.SUBMIT, e -> submits.incrementAndGet());
            field.setText("12");
            int baseline = inputs.get();
            field.setText("");
            attach(field, "Código");
            int before = inputs.get();
            field.setText("12");
            assertEquals(baseline, inputs.get() - before);
            field.postActionEvent();
            assertEquals(1, submits.get());
            assertEquals("12", field.getCleanText());
            return null;
        });
    }

    @Test
    void errorPersistsAndClearingRestoresAccessibleDescription() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener();
            field.getAccessibleContext().setAccessibleDescription("Ajuda");
            MaterialLayout layout = attach(field, "E-mail");
            layout.setError("E-mail inválido").setErrorColor(Color.MAGENTA);
            field.setText("a@b.com");
            field.setText("");
            assertEquals("E-mail inválido", layout.getError());
            assertEquals("E-mail inválido", field.getAccessibleContext().getAccessibleDescription());
            assertEquals(Color.MAGENTA, layout.getErrorColor());
            layout.setError("").setErrorColor(null);
            assertNull(layout.getError());
            assertEquals("Ajuda", field.getAccessibleContext().getAccessibleDescription());
            assertEquals(UiTokens.danger(), layout.getErrorColor());
            field.setLabel("Contato");
            assertEquals("Contato", field.getAccessibleContext().getAccessibleName());
            return null;
        });
    }

    @Test
    void clearButtonRemainsInEditorAndEmitsOneClearWithLongError() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener(20);
            MaterialLayout layout = attach(field, "Nome");
            layout.setError("Mensagem longa para explicar o erro e sugerir como corrigir o conteúdo do campo.");
            field.setText("Ana");
            AtomicInteger clears = new AtomicInteger();
            field.addEventListener(EventType.CLEAR, e -> clears.incrementAndGet());
            render(field, 200);
            Rectangle content = field.getFieldContentBounds();
            int x = field.getWidth() - UiTokens.space(3) - 8;
            click(field, x, field.getHeight() - 8);
            assertEquals("Ana", field.getText());
            click(field, x, content.y + content.height / 2);
            assertEquals("", field.getText());
            assertEquals(1, clears.get());
            assertNotNull(layout.getError());
            return null;
        });
    }

    @Test
    void wrapsErrorsWithoutMovingEditorAndHandlesFontAndReadOnly() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener(20);
            MaterialLayout layout = attach(field, "Texto");
            render(field, 280);
            int baseHeight = field.getHeight();
            Rectangle baseContent = field.getFieldContentBounds();
            layout.setError("Erro");
            assertEquals(baseHeight, field.getPreferredSize().height);
            layout.setError("Erro longo com vários detalhes que precisam aparecer abaixo do campo sem cortar a mensagem.");
            render(field, 160);
            int narrowHeight = field.getHeight();
            assertTrue(narrowHeight > baseHeight);
            assertEquals(baseContent.y, field.getFieldContentBounds().y);
            assertEquals(baseContent.height, field.getFieldContentBounds().height);
            render(field, 600);
            assertTrue(field.getHeight() < narrowHeight);
            layout.setError("palavrasemespacos".repeat(10) + "\nOutra linha");
            render(field, 100);
            assertTrue(field.getHeight() > narrowHeight);
            layout.setError(null);
            field.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 32));
            assertTrue(field.getPreferredSize().height > baseHeight);
            field.setEditable(false);
            field.setText("Texto selecionável");
            field.selectAll();
            assertEquals(field.getText(), field.getSelectedText());
            render(field, 320);
            return null;
        });
    }

    @Test
    void leadingIconAndSelectionStayInsideEditor() throws Exception {
        onEdt(() -> {
            JTextFieldListener field = new JTextFieldListener(20);
            MaterialLayout layout = attach(field, "Pesquisar");
            layout.setError("Mensagem de erro que ocupa mais de uma linha neste campo estreito.");
            field.setIcon(new TestIcon());
            field.setText("Texto");
            BufferedImage rendered = render(field, 180);
            Rectangle content = field.getFieldContentBounds();
            assertEquals(UiTokens.space(3) + 16 + field.getIconGap(), content.x);
            assertTrue(countColor(rendered, Color.GREEN, new Rectangle(0, content.y, content.x, content.height)) > 0);
            assertEquals(0, countColor(rendered, Color.GREEN,
                    new Rectangle(0, content.y + content.height, field.getWidth(), field.getHeight() - content.y - content.height)));
            field.selectAll();
            assertEquals("Texto", field.getSelectedText());
            return null;
        });
    }

    @Test
    void inheritedLayoutsWorkWithNumberCurrencySearchAndBreadcrumbModes() throws Exception {
        onEdt(() -> {
            NumberField number = new NumberField(Locale.forLanguageTag("pt-BR"));
            CurrencyField currency = new CurrencyField();
            SearchTextField<String> search = new SearchTextField<>();
            PathTextField path = new PathTextField("/", 20);
            PathSearchTextField<String> pathSearch = new PathSearchTextField<>("/", 20);
            for (JTextFieldListener field : List.of(number, currency, search, path, pathSearch)) {
                attach(field, "Campo herdado").setError("Erro");
                render(field, 300);
                assertTrue(field.getPreferredSize().height > 56);
            }
            for (JTextFieldListener field : List.of(path, pathSearch)) {
                field.setText("/pasta/arquivo");
                render(field, 300);
                Rectangle content = field.getFieldContentBounds();
                assertTrue(content.contains(field.getComponent(0).getBounds()));
            }
            path.enterEditMode();
            assertFalse(path.getComponent(0).isVisible());
            render(path, 300);
            path.exitEditMode(true);
            assertTrue(path.getComponent(0).isVisible());
            assertEquals(List.of("pasta", "arquivo"), path.getSegments());
            pathSearch.enterEditMode();
            render(pathSearch, 300);
            pathSearch.exitEditMode(true);
            assertEquals(List.of("pasta", "arquivo"), pathSearch.getSegments());
            assertEquals("/pasta/arquivo", pathSearch.getText());
            return null;
        });
    }

    @Test
    void animationCompletesAndDetachingOrRemovalStopsTimer() throws Exception {
        TestField field = onEdt(() -> {
            TestField next = new TestField();
            attach(next, "Animado");
            next.pretendShowing = true;
            focus(next, true);
            return next;
        });
        Thread.sleep(250);
        TestLayout layout = onEdt(() -> {
            TestLayout current = (TestLayout) field.getFieldLayoutManager();
            assertEquals(1f, current.progress());
            focus(field, false);
            field.removeNotify();
            assertEquals(0f, current.progress());
            focus(field, true);
            field.setFieldLayoutManager(null);
            return current;
        });
        float detachedProgress = onEdt(layout::progress);
        Thread.sleep(250);
        onEdt(() -> {
            assertEquals(detachedProgress, layout.progress());
            assertNull(field.getFieldLayoutManager());
            return null;
        });
    }

    @Test
    void autocompleteStillShowsBelowErrorAndSelectsByKeyboard() throws Exception {
        PopupFactory originalFactory = onEdt(PopupFactory::getSharedInstance);
        AtomicInteger popupY = new AtomicInteger(-1);
        AtomicInteger hidden = new AtomicInteger();
        AtomicInteger selected = new AtomicInteger();
        SearchTextField<String> field = onEdt(() -> {
            PopupFactory.setSharedInstance(new PopupFactory() {
                @Override
                public Popup getPopup(Component owner, Component contents, int x, int y) {
                    popupY.set(y);
                    return new Popup() {
                        @Override
                        public void show() {}
                        @Override
                        public void hide() { hidden.incrementAndGet(); }
                    };
                }
            });
            SearchTextField<String> search = new SearchTextField<>() {
                @Override
                public boolean isShowing() { return true; }
                @Override
                public Point getLocationOnScreen() { return new Point(40, 40); }
            };
            attach(search, "Pesquisar").setError("Erro persistente abaixo do campo");
            render(search, 300);
            search.setDataSource(List.of("Ana", "Bruno"));
            search.addSearchOption(value -> value);
            search.addEventListener(EventType.SELECT, e -> selected.incrementAndGet());
            search.showAllSuggestions();
            return search;
        });
        try {
            long deadline = System.nanoTime() + 3_000_000_000L;
            while (popupY.get() < 0 && System.nanoTime() < deadline) Thread.sleep(20);
            onEdt(() -> {
                assertTrue(popupY.get() >= 40 + field.getHeight());
                pressKey(field, KeyEvent.VK_DOWN);
                assertEquals("Ana", field.getSelectedSuggestion());
                pressKey(field, KeyEvent.VK_ENTER);
                assertEquals("Ana", field.getText());
                assertEquals(1, selected.get());
                assertTrue(hidden.get() > 0);
                assertNotNull(((MaterialLayout) field.getFieldLayoutManager()).getError());
                return null;
            });
        } finally {
            onEdt(() -> {
                field.closePopup();
                field.removeNotify();
                field.setFieldLayoutManager(null);
                PopupFactory.setSharedInstance(originalFactory);
                return null;
            });
        }
    }

    @Test
    void themeSwitchPreservesLayoutCustomColorAndNativeBorderOnDetach() throws Exception {
        onEdt(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            try {
                FlatLightLaf.setup();
                JTextFieldListener field = new JTextFieldListener(20);
                MaterialLayout layout = attach(field, "Nome").setError("Erro").setErrorColor(Color.MAGENTA);
                focus(field, true);
                assertTrue(countColor(render(field, 300), Color.MAGENTA, new Rectangle(0, 0, 300, field.getHeight())) > 50);
                FlatDarkLaf.setup();
                SwingUtilities.updateComponentTreeUI(field);
                assertSame(layout, field.getFieldLayoutManager());
                assertFalse(field.isOpaque());
                assertEquals(Color.MAGENTA, layout.getErrorColor());
                assertTrue(countColor(render(field, 300), Color.MAGENTA, new Rectangle(0, 0, 300, field.getHeight())) > 50);
                var border = field.getBorder();
                field.setFieldLayoutManager(null);
                assertSame(border, field.getBorder());
            } finally { UIManager.setLookAndFeel(original); UiTokens.refresh(); }
            return null;
        });
    }

    @Test
    void rendersGalleryForVisualReview() throws Exception {
        String directory = System.getProperty("materialLayout.qaDir");
        onEdt(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            try {
                for (boolean dark : new boolean[]{false, true}) {
                    if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
                    UiTokens.refresh();
                    JTextFieldListener empty = new JTextFieldListener(20);
                    attach(empty, "Nome completo");
                    JTextFieldListener focused = new JTextFieldListener(20);
                    attach(focused, "Campo com foco"); focus(focused, true);
                    MaskedTextField masked = new MaskedTextField("###.###.###-##", 20);
                    attach(masked, "CPF com máscara"); focus(masked, true);
                    MaskedTextField partial = new MaskedTextField("###.###.###-##", 20);
                    attach(partial, "CPF parcial").setError("Complete os 11 dígitos do CPF."); partial.setText("12345");
                    JTextFieldListener icon = new JTextFieldListener(20);
                    attach(icon, "Pesquisar"); icon.setIcon(new TestIcon()); icon.setText("Exemplo");
                    PathTextField path = new PathTextField("/", 20);
                    attach(path, "Caminho"); path.setText("/documentos/projeto");
                    PathSearchTextField<String> editPath = new PathSearchTextField<>("/", 20);
                    attach(editPath, "Caminho em edição"); editPath.setText("/documentos/projeto"); editPath.enterEditMode();
                    JTextFieldListener large = new JTextFieldListener(20);
                    attach(large, "Fonte maior"); large.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24)); large.setText("Exemplo");
                    JTextFieldListener narrow = new JTextFieldListener(10);
                    attach(narrow, "Rótulo longo em campo pequeno").setError("Uma mensagem de erro longa que precisa caber sem cortar o texto.");
                    JTextFieldListener disabled = new JTextFieldListener(20);
                    attach(disabled, "Desabilitado"); disabled.setText("Texto"); disabled.setEnabled(false);
                    List<JTextFieldListener> fields = List.of(empty, focused, masked, partial, icon, path, editPath, large, narrow, disabled);
                    BufferedImage gallery = new BufferedImage(760, 740, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = gallery.createGraphics();
                    try {
                        g.setColor(UIManager.getColor("Panel.background"));
                        g.fillRect(0, 0, gallery.getWidth(), gallery.getHeight());
                        for (int i = 0; i < fields.size(); i++) {
                            BufferedImage rendered = render(fields.get(i), i == 8 ? 180 : 340);
                            g.drawImage(rendered, 24 + (i % 2) * 380, 20 + (i / 2) * 142, null);
                        }
                    } finally { g.dispose(); }
                    if (directory != null) {
                        Path output = Path.of(directory);
                        Files.createDirectories(output);
                        ImageIO.write(gallery, "png", output.resolve(dark ? "dark.png" : "light.png").toFile());
                    }
                }
            } finally { UIManager.setLookAndFeel(original); UiTokens.refresh(); }
            return null;
        });
    }

    private static TestLayout attach(JTextFieldListener field, String label) {
        TestLayout layout = new TestLayout();
        field.setLabel(label);
        field.setFieldLayoutManager(layout);
        return layout;
    }

    private static void focus(JTextFieldListener field, boolean gained) {
        FocusEvent event = new FocusEvent(field, gained ? FocusEvent.FOCUS_GAINED : FocusEvent.FOCUS_LOST);
        for (var listener : field.getFocusListeners()) {
            if (gained) listener.focusGained(event); else listener.focusLost(event);
        }
    }

    private static void click(JTextFieldListener field, int x, int y) {
        field.dispatchEvent(new MouseEvent(field, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                0, x, y, 1, false, MouseEvent.BUTTON1));
    }

    private static void pressKey(JTextFieldListener field, int code) {
        KeyEvent event = new KeyEvent(field, KeyEvent.KEY_PRESSED, System.currentTimeMillis(),
                0, code, KeyEvent.CHAR_UNDEFINED);
        for (var listener : field.getKeyListeners()) listener.keyPressed(event);
    }

    private static BufferedImage render(JTextFieldListener field, int width) {
        field.setSize(width, field.getPreferredSize().height);
        field.setSize(width, field.getPreferredSize().height);
        field.doLayout();
        for (Component child : field.getComponents()) if (child instanceof Container container) container.doLayout();
        BufferedImage image = new BufferedImage(width, field.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { field.paint(g); } finally { g.dispose(); }
        return image;
    }

    private static int countColor(BufferedImage image, Color color, Rectangle bounds) {
        int count = 0;
        for (int y = bounds.y; y < Math.min(image.getHeight(), bounds.y + bounds.height); y++) {
            for (int x = bounds.x; x < Math.min(image.getWidth(), bounds.x + bounds.width); x++) {
                if (image.getRGB(x, y) == color.getRGB()) count++;
            }
        }
        return count;
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try { result.set(action.call()); } catch (Throwable t) { failure.set(t); }
        });
        if (failure.get() instanceof Error error) throw error;
        if (failure.get() instanceof Exception exception) throw exception;
        return result.get();
    }

    private static class TestLayout extends MaterialLayout {
        float progress() { return getLabelProgress(); }
    }

    private static class TestField extends JTextFieldListener {
        boolean pretendShowing;
        @Override
        public boolean isShowing() { return pretendShowing; }
    }

    private static class TestIcon implements Icon {
        @Override
        public int getIconWidth() { return 16; }
        @Override
        public int getIconHeight() { return 16; }
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, 16, 16);
        }
    }
}
