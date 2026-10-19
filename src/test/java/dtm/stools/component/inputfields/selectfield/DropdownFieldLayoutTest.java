package dtm.stools.component.inputfields.selectfield;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.JTextFieldListener;
import dtm.stools.component.inputfields.textfield.layout.FieldLayoutConfigurable;
import dtm.stools.component.inputfields.textfield.layout.FieldLayoutTarget;
import dtm.stools.component.inputfields.textfield.layout.MaterialLayout;
import dtm.stools.configs.UiTokens;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DropdownFieldLayoutTest {
    @Test
    void popupSelectionStaysVisibleAfterSelectingFirstItemAndRepaintingField() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            try {
                for (boolean dark : new boolean[]{false, true}) {
                    if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
                    UiTokens.refresh();
                    DropdownField field = new DropdownField("Novo", "Em andamento", "Fechado");
                    field.setLabel("Status");
                    field.setFieldLayoutManager(new MaterialLayout());
                    JPopupMenu popup = (JPopupMenu) field.getAccessibleContext().getAccessibleChild(0);
                    JList<?> list = ((ComboPopup) popup).getList();
                    for (int selected : new int[]{0, 2, 0}) {
                        list.setSelectedIndex(selected);
                        paintPopup(popup, 280);
                        field.setSelectedIndex(selected);
                        render(field, 280);
                        assertTrue(((JComponent) field.getRenderer()).isOpaque());
                        for (int highlighted = 0; highlighted < 3; highlighted++) {
                            list.setSelectedIndex(highlighted);
                            BufferedImage image = paintPopup(popup, 280);
                            int selectedPixels = 0;
                            for (int y = 0; y < image.getHeight(); y++) {
                                for (int x = 0; x < image.getWidth(); x++) {
                                    if (image.getRGB(x, y) == list.getSelectionBackground().getRGB()) selectedPixels++;
                                }
                            }
                            assertTrue(selectedPixels > 280,
                                    "Selection background disappeared after selecting " + selected + " and highlighting " + highlighted);
                        }
                    }
                    writeSelectionPreview(field, popup, list, dark);
                    field.setFieldLayoutManager(null);
                }
            } finally {
                try { UIManager.setLookAndFeel(original); }
                catch (UnsupportedLookAndFeelException e) { throw new RuntimeException(e); }
                UiTokens.refresh();
            }
        });
    }

    private static void writeSelectionPreview(DropdownField field, JPopupMenu popup, JList<?> list, boolean dark) {
        String directory = System.getProperty("dropdown.gallery.dir");
        if (directory == null) return;
        BufferedImage fieldImage = render(field, 280);
        list.setSelectedIndex(1);
        BufferedImage popupImage = paintPopup(popup, 280);
        Rectangle body = field.getFieldLayoutManager().getFieldBounds(field);
        int popupY = body.y + body.height;
        BufferedImage image = new BufferedImage(300, popupY + popupImage.getHeight() + 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(UIManager.getColor("Panel.background"));
            g.fillRect(0, 0, image.getWidth(), image.getHeight());
            g.drawImage(fieldImage, 10, 0, null);
            g.drawImage(popupImage, 10, popupY, null);
            Path output = Path.of(directory);
            Files.createDirectories(output);
            ImageIO.write(image, "png", output.resolve(dark ? "selection-dark.png" : "selection-light.png").toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally { g.dispose(); }
    }

    @Test
    void fieldPaintingPreservesExplicitTransparencyOfCustomRenderer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DropdownField field = new DropdownField("Novo", "Fechado");
            DefaultListCellRenderer renderer = new DefaultListCellRenderer();
            renderer.setOpaque(false);
            field.setCustomRenderer(renderer);
            field.setFieldLayoutManager(new MaterialLayout());
            field.setSelectedIndex(0);
            JPopupMenu popup = (JPopupMenu) field.getAccessibleContext().getAccessibleChild(0);
            for (int i = 0; i < 3; i++) {
                paintPopup(popup, 280);
                render(field, 280);
                assertFalse(renderer.isOpaque());
            }
            field.setFieldLayoutManager(null);
        });
    }

    @Test
    void materialPopupKeepsNativeThemeBorderPaddingColorsAndRenderer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            Map<String, Object> previous = new LinkedHashMap<>();
            try {
                for (boolean dark : new boolean[]{false, true}) {
                    if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
                    Map<String, Object> values = Map.of(
                            "ComboBox.popupBackground", new Color(0x314154),
                            "ComboBox.selectionBackground", new Color(0x52718F),
                            "ComboBox.selectionForeground", Color.YELLOW,
                            "ComboBox.padding", new Insets(5, 9, 5, 9),
                            "ComboBox.popupInsets", new Insets(3, 4, 3, 4),
                            "PopupMenu.border", BorderFactory.createLineBorder(new Color(0x667788), 2));
                    for (var entry : values.entrySet()) {
                        Object old = UIManager.put(entry.getKey(), entry.getValue());
                        if (!previous.containsKey(entry.getKey())) previous.put(entry.getKey(), old);
                    }
                    for (boolean editable : new boolean[]{false, true}) {
                        DropdownField nativeField = new DropdownField("minutos", "horas", "dias", "meses", "anos");
                        nativeField.setEditable(editable);
                        DropdownField material = new DropdownField("minutos", "horas", "dias", "meses", "anos");
                        material.setEditable(editable);
                        material.setLabel("Unidade de tempo");
                        material.setSelectedItem("meses");
                        material.setFieldLayoutManager(new MaterialLayout());
                        SwingUtilities.updateComponentTreeUI(material);
                        assertInstanceOf(FlatFieldLayoutComboBoxUI.class, material.getUI());
                        var expected = (JPopupMenu) nativeField.getAccessibleContext().getAccessibleChild(0);
                        var actual = (JPopupMenu) material.getAccessibleContext().getAccessibleChild(0);
                        JList<?> expectedList = ((ComboPopup) expected).getList();
                        JList<?> actualList = ((ComboPopup) actual).getList();
                        assertEquals(expected.getInsets(), actual.getInsets());
                        assertEquals(expected.getBackground(), actual.getBackground());
                        assertEquals(expectedList.getBackground(), actualList.getBackground());
                        assertEquals(expectedList.getSelectionBackground(), actualList.getSelectionBackground());
                        assertEquals(expectedList.getSelectionForeground(), actualList.getSelectionForeground());
                        assertEquals(expectedList.getCellBounds(0, 0).height, actualList.getCellBounds(0, 0).height);
                        var expectedScroller = (JScrollPane) expected.getComponent(0);
                        var actualScroller = (JScrollPane) actual.getComponent(0);
                        assertEquals(expectedScroller.getViewportBorder().getBorderInsets(expectedScroller),
                                actualScroller.getViewportBorder().getBorderInsets(actualScroller));
                        if (!editable) writePopupComparison(nativeField, material, expected, actual, dark);
                        AtomicInteger rendered = new AtomicInteger();
                        material.setCustomRenderer((list, value, index, selected, focused) -> {
                            rendered.incrementAndGet();
                            return new JLabel("Valor: " + value);
                        });
                        actualList.getCellBounds(0, 0);
                        assertTrue(rendered.get() > 0);
                        render(material, 280);
                        var popup = (FlatFieldLayoutComboBoxUI.FieldPopup) actual;
                        Rectangle body = material.getFieldLayoutManager().getFieldBounds(material);
                        assertEquals(body.y + body.height,
                                popup.computePopupBounds(0, material.getHeight(), 280, 120).y);
                        material.setFieldLayoutManager(null);
                    }
                }
            } finally {
                previous.forEach(UIManager::put);
                try { UIManager.setLookAndFeel(original); }
                catch (UnsupportedLookAndFeelException e) { throw new RuntimeException(e); }
                UiTokens.refresh();
            }
        });
    }

    @Test
    void arrowChangesDirectionWhenPopupOpensAndClosesWithoutLeakingListeners() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            try {
                for (String theme : new String[]{UIManager.getCrossPlatformLookAndFeelClassName(),
                        FlatLightLaf.class.getName(), FlatDarkLaf.class.getName()}) {
                    UIManager.setLookAndFeel(theme);
                    AtomicBoolean open = new AtomicBoolean();
                    DropdownField field = new DropdownField("Um", "Dois") {
                        @Override
                        public boolean isPopupVisible() { return open.get(); }
                    };
                    field.getAccessibleContext();
                    int listeners = field.getPopupMenuListeners().length;
                    for (int i = 0; i < 3; i++) {
                        field.setFieldLayoutManager(new MaterialLayout());
                        render(field, 280);
                        JButton arrow = (JButton) java.util.Arrays.stream(field.getComponents())
                                .filter(component -> component instanceof JButton).findFirst().orElseThrow();
                        BufferedImage closed = paintArrow(arrow);
                        open.set(true);
                        PopupMenuEvent event = new PopupMenuEvent(field);
                        for (PopupMenuListener listener : field.getPopupMenuListeners()) listener.popupMenuWillBecomeVisible(event);
                        BufferedImage opened = paintArrow(arrow);
                        int difference = 0;
                        for (int y = 0; y < closed.getHeight(); y++) {
                            for (int x = 0; x < closed.getWidth(); x++) {
                                if (closed.getRGB(x, y) != opened.getRGB(x, y)) difference++;
                            }
                        }
                        assertTrue(difference > 0);
                        open.set(false);
                        for (PopupMenuListener listener : field.getPopupMenuListeners()) listener.popupMenuWillBecomeInvisible(event);
                        BufferedImage closedAgain = paintArrow(arrow);
                        for (int y = 0; y < closed.getHeight(); y++) {
                            for (int x = 0; x < closed.getWidth(); x++) {
                                assertEquals(closed.getRGB(x, y), closedAgain.getRGB(x, y));
                            }
                        }
                        field.setFieldLayoutManager(null);
                        assertEquals(listeners, field.getPopupMenuListeners().length);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try { UIManager.setLookAndFeel(original); }
                catch (UnsupportedLookAndFeelException e) { throw new RuntimeException(e); }
                UiTokens.refresh();
            }
        });
    }

    private static BufferedImage paintArrow(JButton arrow) {
        BufferedImage image = new BufferedImage(arrow.getWidth(), arrow.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { arrow.paint(g); }
        finally { g.dispose(); }
        return image;
    }

    private static void writePopupComparison(DropdownField nativeField, DropdownField material,
                                             JPopupMenu nativePopup, JPopupMenu materialPopup, boolean dark) {
        String directory = System.getProperty("dropdown.gallery.dir");
        if (directory == null) return;
        BufferedImage gallery = new BufferedImage(640, 300, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = gallery.createGraphics();
        try {
            g.setColor(UIManager.getColor("Panel.background"));
            g.fillRect(0, 0, gallery.getWidth(), gallery.getHeight());
            g.drawImage(render(nativeField, 280), 20, 20, null);
            g.drawImage(render(material, 280), 340, 20, null);
            g.drawImage(paintPopup(nativePopup, 280), 20, 20 + nativeField.getHeight(), null);
            Rectangle body = material.getFieldLayoutManager().getFieldBounds(material);
            g.drawImage(paintPopup(materialPopup, 280), 340, 20 + body.y + body.height, null);
            Path output = Path.of(directory);
            Files.createDirectories(output);
            ImageIO.write(gallery, "png", output.resolve(dark ? "popup-dark.png" : "popup-light.png").toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally { g.dispose(); }
    }

    private static BufferedImage paintPopup(JPopupMenu popup, int width) {
        JList<?> list = ((ComboPopup) popup).getList();
        Rectangle cells = list.getModel().getSize() == 0 ? new Rectangle(0, 0, width, 1)
                : list.getCellBounds(0, list.getModel().getSize() - 1);
        JScrollPane scroller = (JScrollPane) popup.getComponent(0);
        Insets viewport = scroller.getViewportBorder() == null ? new Insets(0, 0, 0, 0)
                : scroller.getViewportBorder().getBorderInsets(scroller);
        Insets scrollInsets = scroller.getInsets();
        Insets popupInsets = popup.getInsets();
        Dimension scrollSize = new Dimension(width - popupInsets.left - popupInsets.right,
                cells.height + viewport.top + viewport.bottom + scrollInsets.top + scrollInsets.bottom);
        scroller.setPreferredSize(scrollSize);
        scroller.setMinimumSize(scrollSize);
        scroller.setMaximumSize(scrollSize);
        popup.setSize(width, scrollSize.height + popupInsets.top + popupInsets.bottom);
        layoutTree(popup);
        BufferedImage image = new BufferedImage(width, popup.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { popup.paint(g); }
        finally { g.dispose(); }
        return image;
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component component : container.getComponents()) {
            if (component instanceof Container child) layoutTree(child);
        }
    }

    @Test
    void popupAnchorsToVisibleFieldInsteadOfErrorFooter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (boolean editable : new boolean[]{false, true}) {
                DropdownField field = new DropdownField("Novo", "Em andamento", "Fechado");
                field.setEditable(editable);
                TrackingLayout layout = attach(field, "Status");
                for (String error : new String[]{null, "Selecione um status.\nO campo é obrigatório."}) {
                    layout.setError(error);
                    render(field, 280);
                    FieldLayoutComboPopup popup = (FieldLayoutComboPopup) field.getAccessibleContext().getAccessibleChild(0);
                    Rectangle body = layout.getFieldBounds(field);
                    Rectangle bounds = popup.computePopupBounds(0, field.getHeight(), field.getWidth(), 100);
                    Insets insets = popup.getInsets();
                    assertEquals(body.y + body.height, bounds.y);
                    assertTrue(bounds.y < field.getHeight());
                    assertEquals(body.x, bounds.x);
                    assertEquals(body.width, bounds.width + insets.left + insets.right);
                    field.setLocation(100, Toolkit.getDefaultToolkit().getScreenSize().height - field.getHeight());
                    bounds = popup.computePopupBounds(0, field.getHeight(), field.getWidth(), 100);
                    assertEquals(body.y, bounds.y + bounds.height + insets.top + insets.bottom);
                    field.setLocation(0, 0);
                }
                field.setFieldLayoutManager(null);
            }
        });
    }

    @Test
    void configuresTextAndDropdownThroughTheSameInterface() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (FieldLayoutConfigurable field : new FieldLayoutConfigurable[]{
                    new JTextFieldListener(), new DropdownFieldListener<String>(), new DropdownField()}) {
                MaterialLayout layout = new MaterialLayout();
                field.setFieldLayoutManager(layout);
                assertSame(layout, field.getFieldLayoutManager());
                field.setFieldLayoutManager(null);
                assertNull(field.getFieldLayoutManager());
            }
        });
    }

    @Test
    void restoresConventionalPresentationAndDoesNotAccumulateListeners() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DropdownField field = new DropdownField("Novo", "Fechado");
            field.setSelectedItem("Novo");
            field.setLabel("Status");
            field.getAccessibleContext().setAccessibleDescription("Ajuda");
            var ui = field.getUI();
            var model = field.getModel();
            var renderer = field.getRenderer();
            var insets = field.getInsets();
            var size = field.getPreferredSize();
            boolean opaque = field.isOpaque();
            int focus = field.getFocusListeners().length;
            int properties = field.getPropertyChangeListeners().length;
            int containers = field.getContainerListeners().length;
            MaterialLayout first = new MaterialLayout().setError("Escolha outro status");
            MaterialLayout second = new MaterialLayout();
            for (int i = 0; i < 3; i++) {
                field.setFieldLayoutManager(first);
                int attached = field.getFocusListeners().length;
                field.setFieldLayoutManager(first);
                assertEquals(attached, field.getFocusListeners().length);
                assertSame(model, field.getModel());
                assertSame(renderer, field.getRenderer());
                assertEquals("Novo", field.getSelectedItem());
                assertFalse(field.isOpaque());
                assertEquals("Status", field.getAccessibleContext().getAccessibleName());
                assertEquals("Escolha outro status", field.getAccessibleContext().getAccessibleDescription());
                assertTrue(field.getPreferredSize().height > size.height);
                field.setFieldLayoutManager(second);
                field.setFieldLayoutManager(null);
                assertSame(ui, field.getUI());
                assertSame(renderer, field.getRenderer());
                assertEquals(insets, field.getInsets());
                assertEquals(size, field.getPreferredSize());
                assertEquals(opaque, field.isOpaque());
                assertEquals("Ajuda", field.getAccessibleContext().getAccessibleDescription());
                assertEquals(focus, field.getFocusListeners().length);
                assertEquals(properties, field.getPropertyChangeListeners().length);
                assertEquals(containers, field.getContainerListeners().length);
            }
        });
    }

    @Test
    void restoresPreviousManagerWhenAnInstanceIsAlreadyInUse() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextFieldListener text = new JTextFieldListener();
            MaterialLayout shared = new MaterialLayout();
            text.setFieldLayoutManager(shared);
            DropdownField field = new DropdownField();
            var ui = field.getUI();
            assertThrows(IllegalStateException.class, () -> field.setFieldLayoutManager(shared));
            assertNull(field.getFieldLayoutManager());
            assertSame(ui, field.getUI());
            MaterialLayout previous = new MaterialLayout().setError("Anterior");
            field.setFieldLayoutManager(previous);
            assertThrows(IllegalStateException.class, () -> field.setFieldLayoutManager(shared));
            assertSame(previous, field.getFieldLayoutManager());
            assertEquals("Anterior", field.getAccessibleContext().getAccessibleDescription());
            text.setFieldLayoutManager(null);
            field.setFieldLayoutManager(shared);
            field.setFieldLayoutManager(null);
        });
    }

    @Test
    void labelTracksSelectionModelReplacementAndEditorText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DropdownField field = new DropdownField("Novo", "Fechado");
            TrackingLayout layout = attach(field, "Status");
            assertEquals(0f, layout.progress());
            field.setSelectedIndex(0);
            assertEquals(1f, layout.progress());
            field.setSelectedItem(null);
            assertEquals(0f, layout.progress());
            field.setModel(new DefaultComboBoxModel<>(new Object[]{"Outro"}));
            assertEquals(1f, layout.progress());
            field.setSelectedItem(null);
            field.setEditable(true);
            JTextComponent editor = (JTextComponent) field.getEditor().getEditorComponent();
            editor.setText("Valor ainda não confirmado");
            assertNull(field.getSelectedItem());
            assertFalse(field.isFieldContentEmpty());
            assertEquals(1f, layout.progress());
            editor.setText("");
            assertEquals(0f, layout.progress());
            layout.setError("Obrigatório");
            editor.setText("Outro");
            editor.setText("");
            assertEquals("Obrigatório", layout.getError());
            field.setLabel(null);
            assertEquals("", field.getLabel());
            assertEquals("", field.getAccessibleContext().getAccessibleName());
            field.setFieldLayoutManager(null);
        });
    }

    @Test
    void followsInternalFocusAndDetachesReplacedEditorsAndDocuments() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DropdownField field = new DropdownField();
            field.setEditable(true);
            TrackingLayout layout = attach(field, "Nome");
            JTextComponent oldEditor = (JTextComponent) field.getEditor().getEditorComponent();
            fireFocus(oldEditor, true, null);
            assertEquals(1f, layout.progress());
            fireFocus(oldEditor, false, field);
            assertEquals(1f, layout.progress());
            fireFocus(field, false, null);
            assertEquals(0f, layout.progress());
            PlainDocument oldDocument = (PlainDocument) oldEditor.getDocument();
            int documentListeners = oldDocument.getDocumentListeners().length;
            oldEditor.setDocument(new PlainDocument());
            assertTrue(oldDocument.getDocumentListeners().length < documentListeners);
            int previousChanges = layout.changes;
            try { oldDocument.insertString(0, "Documento desconectado", null); }
            catch (javax.swing.text.BadLocationException e) { throw new RuntimeException(e); }
            assertEquals(previousChanges, layout.changes);
            oldEditor.setText("Antes");
            assertEquals(1f, layout.progress());
            field.setEditor(new BasicComboBoxEditor());
            JTextComponent editor = (JTextComponent) field.getEditor().getEditorComponent();
            editor.setText("");
            int changes = layout.changes;
            oldEditor.setText("Editor desconectado");
            assertEquals(changes, layout.changes);
            fireFocus(oldEditor, true, null);
            assertEquals(0f, layout.progress());
            fireFocus(editor, true, null);
            assertEquals(1f, layout.progress());
            fireFocus(editor, false, null);
            PlainDocument document = (PlainDocument) editor.getDocument();
            int listeners = document.getDocumentListeners().length;
            field.setFieldLayoutManager(null);
            assertEquals(listeners - 1, document.getDocumentListeners().length);
            changes = layout.changes;
            editor.setText("Sem layout");
            assertEquals(changes, layout.changes);
        });
    }

    @Test
    void preservesCustomEditorPresentationAndUncommittedText() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DropdownField field = new DropdownField("Novo");
            field.setEditable(true);
            field.setEditor(new BasicComboBoxEditor());
            JTextComponent editor = (JTextComponent) field.getEditor().getEditorComponent();
            var border = BorderFactory.createLineBorder(Color.BLUE);
            editor.setBorder(border);
            editor.setOpaque(true);
            editor.setText("Pendente");
            field.setFieldLayoutManager(new MaterialLayout());
            assertEquals("Pendente", editor.getText());
            assertFalse(editor.isOpaque());
            field.setFieldLayoutManager(null);
            assertSame(border, editor.getBorder());
            assertTrue(editor.isOpaque());
            assertEquals("Pendente", editor.getText());
            assertNull(field.getSelectedItem());
        });
    }

    @Test
    void preservesKeyboardSearchPopupRendererAndChangeEvents() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            for (boolean material : new boolean[]{false, true}) {
                DropdownField field = new DropdownField("Ana", "Bruno", "Carlos");
                AtomicInteger changes = new AtomicInteger();
                field.addEventListener(EventType.CHANGE, e -> changes.incrementAndGet());
                AtomicInteger rendered = new AtomicInteger();
                field.setCustomRenderer((list, value, index, selected, focused) -> {
                    rendered.incrementAndGet();
                    return new JLabel("Item: " + value);
                });
                if (material) field.setFieldLayoutManager(new MaterialLayout());
                var popup = field.getAccessibleContext().getAccessibleChild(0);
                assertInstanceOf(ComboPopup.class, popup);
                assertEquals(3, ((ComboPopup) popup).getList().getModel().getSize());
                field.processKeyEvent(new KeyEvent(field, KeyEvent.KEY_TYPED, 1, 0, KeyEvent.VK_UNDEFINED, 'b'));
                assertNull(field.getSelectedItem());
                assertEquals(0, changes.get());
                field.processKeyEvent(new KeyEvent(field, KeyEvent.KEY_PRESSED, 2, 0, KeyEvent.VK_ENTER, '\n'));
                assertEquals("Bruno", field.getSelectedItem());
                assertEquals(1, changes.get());
                render(field, 280);
                assertTrue(rendered.get() > 0);
                field.setFieldLayoutManager(null);
            }
        });
    }

    @Test
    void editableKeyboardSearchCommitsOnlyOnEnter() throws Exception {
        AtomicReference<DropdownField> reference = new AtomicReference<>();
        AtomicInteger changes = new AtomicInteger();
        SwingUtilities.invokeAndWait(() -> {
            DropdownField field = new DropdownField("Ana", "Bruno", "Carlos");
            field.setEditable(true);
            field.setEditor(new BasicComboBoxEditor() {
                @Override
                protected JTextField createEditorComponent() {
                    return new JTextField() {
                        @Override
                        public boolean hasFocus() { return true; }
                    };
                }
            });
            field.setFieldLayoutManager(new MaterialLayout());
            field.addEventListener(EventType.CHANGE, e -> changes.incrementAndGet());
            ((JTextComponent) field.getEditor().getEditorComponent()).setText("br");
            reference.set(field);
        });
        SwingUtilities.invokeAndWait(() -> {
            DropdownField field = reference.get();
            assertNull(field.getSelectedItem());
            assertEquals(0, changes.get());
            JComponent editor = (JComponent) field.getEditor().getEditorComponent();
            Object actionKey = editor.getInputMap().get(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
            editor.getActionMap().get(actionKey).actionPerformed(
                    new java.awt.event.ActionEvent(editor, java.awt.event.ActionEvent.ACTION_PERFORMED, "enter"));
            assertEquals("Bruno", field.getSelectedItem());
            assertEquals(1, changes.get());
            field.setFieldLayoutManager(null);
        });
    }

    @Test
    void installsCustomManagersBeforeRequestingTheirGeometry() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DropdownField field = new DropdownField();
            MaterialLayout layout = new MaterialLayout() {
                private boolean installed;

                @Override
                public void install(FieldLayoutTarget target) {
                    installed = true;
                    super.install(target);
                }

                @Override
                public void uninstall(FieldLayoutTarget target) {
                    super.uninstall(target);
                    installed = false;
                }

                @Override
                public Insets getInsets(FieldLayoutTarget target) {
                    assertTrue(installed);
                    return super.getInsets(target);
                }
            };
            field.setFieldLayoutManager(layout);
            render(field, 280);
            field.setFieldLayoutManager(null);
        });
    }

    @Test
    void retainsPendingTextAndEditorPresentationAcrossThemeSwitches() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            try {
                FlatLightLaf.setup();
                DropdownField field = new DropdownField("Novo");
                field.setEditable(true);
                field.setLabel("Status");
                field.setFieldLayoutManager(new MaterialLayout().setError("Obrigatório"));
                ((JTextComponent) field.getEditor().getEditorComponent()).setText("Pendente");
                for (int i = 0; i < 4; i++) {
                    if (i % 2 == 0) FlatDarkLaf.setup(); else FlatLightLaf.setup();
                    SwingUtilities.updateComponentTreeUI(field);
                    JTextComponent editor = (JTextComponent) field.getEditor().getEditorComponent();
                    assertEquals("Pendente", editor.getText());
                    assertFalse(editor.isOpaque());
                    assertEquals(new Insets(0, 0, 0, 0), editor.getInsets());
                    assertEquals("Obrigatório", field.getAccessibleContext().getAccessibleDescription());
                    render(field, 280);
                }
                field.setFieldLayoutManager(null);
                assertFalse(FieldLayoutComboBoxUI.isInstalled(field.getUI()));
                assertEquals("Pendente", field.getEditor().getItem());
            } finally {
                try { UIManager.setLookAndFeel(original); }
                catch (UnsupportedLookAndFeelException e) { throw new RuntimeException(e); }
                UiTokens.refresh();
            }
        });
    }

    @Test
    void placeholderWaitsForFloatingLabelAndArrowStaysOutsideContent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DropdownField field = new DropdownField("Novo");
            field.setPlaceholder("Selecione");
            AtomicInteger placeholders = new AtomicInteger();
            field.setCustomRenderer((list, value, index, selected, focused) -> {
                if (value == null && index == -1) placeholders.incrementAndGet();
                return new JLabel(value == null ? "Selecione" : value.toString());
            });
            TrackingLayout layout = attach(field, "Status");
            render(field, 280);
            placeholders.set(0);
            render(field, 280);
            assertEquals(0, placeholders.get());
            fireFocus(field, true, null);
            render(field, 280);
            assertTrue(placeholders.get() > 0);
            Rectangle content = field.getFieldContentBounds();
            Rectangle body = layout.getFieldBounds(field);
            assertTrue(body.contains(content));
            JButton arrow = (JButton) java.util.Arrays.stream(field.getComponents())
                    .filter(component -> component instanceof JButton).findFirst().orElseThrow();
            assertTrue(arrow.getX() >= content.x + content.width);
            assertTrue(body.contains(arrow.getBounds()));
            field.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
            field.doLayout();
            assertTrue(arrow.getX() + arrow.getWidth() <= field.getFieldContentBounds().x);
            field.setFieldLayoutManager(null);
        });
    }

    @Test
    void reappliesAfterThemeChangesAndRendersBothModes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            LookAndFeel original = UIManager.getLookAndFeel();
            try {
                for (boolean dark : new boolean[]{false, true}) {
                    if (dark) FlatDarkLaf.setup(); else FlatLightLaf.setup();
                    UiTokens.refresh();
                    BufferedImage gallery = new BufferedImage(660, 600, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = gallery.createGraphics();
                    g.setColor(UIManager.getColor("Panel.background"));
                    g.fillRect(0, 0, gallery.getWidth(), gallery.getHeight());
                    try {
                        for (int i = 0; i < 8; i++) {
                            DropdownField field = new DropdownField("Novo", "Em andamento", "Fechado");
                            field.setEditable(i >= 4);
                            field.setPlaceholder("Selecione um status");
                            TrackingLayout layout = attach(field, i >= 4 ? "Status editável" : "Status");
                            if (i % 4 == 1) field.setSelectedItem("Em andamento");
                            if (i % 4 == 2) {
                                layout.setErrorColor(Color.MAGENTA).setError("Escolha um status para continuar. A mensagem permanece durante a edição.");
                            }
                            if (i % 4 == 3) {
                                field.setSelectedItem("Fechado");
                                field.setEnabled(false);
                            }
                            if (i == 5) ((JTextComponent) field.getEditor().getEditorComponent()).setText("Ainda digitando");
                            String pending = field.isEditable() ? field.getEditor().getItem().toString() : null;
                            field.updateUI();
                            assertSame(layout, field.getFieldLayoutManager());
                            assertTrue(FieldLayoutComboBoxUI.isInstalled(field.getUI()));
                            if (field.isEditable()) assertEquals(pending, field.getEditor().getItem().toString());
                            BufferedImage rendered = render(field, 290);
                            assertTrue(field.getFieldContentBounds().height > 0);
                            if (i % 4 == 2) {
                                Rectangle body = layout.getFieldBounds(field);
                                int colored = 0;
                                for (int y = body.y + body.height; y < rendered.getHeight(); y++) {
                                    for (int x = 0; x < rendered.getWidth(); x++) {
                                        if (rendered.getRGB(x, y) == Color.MAGENTA.getRGB()) colored++;
                                    }
                                }
                                assertTrue(colored > 0);
                            }
                            g.drawImage(rendered, 20 + (i / 4) * 330, 16 + (i % 4) * 145, null);
                            if (i == 5) {
                                field.setFieldLayoutManager(null);
                                assertEquals("Ainda digitando", field.getEditor().getItem());
                            } else field.setFieldLayoutManager(null);
                        }
                    } finally { g.dispose(); }
                    String directory = System.getProperty("dropdown.gallery.dir");
                    if (directory != null) {
                        Path output = Path.of(directory);
                        Files.createDirectories(output);
                        ImageIO.write(gallery, "png", output.resolve(dark ? "dark.png" : "light.png").toFile());
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                try { UIManager.setLookAndFeel(original); }
                catch (UnsupportedLookAndFeelException e) { throw new RuntimeException(e); }
                UiTokens.refresh();
            }
        });
    }

    private static TrackingLayout attach(DropdownField field, String label) {
        TrackingLayout layout = new TrackingLayout();
        field.setLabel(label);
        field.setFieldLayoutManager(layout);
        return layout;
    }

    private static void fireFocus(Component component, boolean gained, Component opposite) {
        FocusEvent event = new FocusEvent(component, gained ? FocusEvent.FOCUS_GAINED : FocusEvent.FOCUS_LOST, false, opposite);
        for (var listener : component.getFocusListeners()) {
            if (gained) listener.focusGained(event); else listener.focusLost(event);
        }
    }

    private static BufferedImage render(DropdownField field, int width) {
        field.setSize(width, field.getPreferredSize().height);
        field.setSize(width, field.getPreferredSize().height);
        field.doLayout();
        BufferedImage image = new BufferedImage(width, field.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try { field.paint(g); }
        finally { g.dispose(); }
        return image;
    }

    private static class TrackingLayout extends MaterialLayout {
        int changes;

        @Override
        public void fieldChanged(FieldLayoutTarget field) {
            changes++;
            super.fieldChanged(field);
        }

        float progress() { return getLabelProgress(); }
    }
}
