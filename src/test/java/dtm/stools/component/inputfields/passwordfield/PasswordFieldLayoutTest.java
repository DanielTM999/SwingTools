package dtm.stools.component.inputfields.passwordfield;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.JTextFieldListener;
import dtm.stools.component.inputfields.textfield.layout.MaterialLayout;
import dtm.stools.configs.UiTokens;
import org.junit.jupiter.api.Test;

import javax.accessibility.AccessibleRole;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.PlainDocument;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PasswordFieldLayoutTest {
    @Test
    void restoresNativeAppearanceAccessibilityAndListeners() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PasswordField field = new PasswordField().setLabel("Senha");
            JPasswordField input = field.getPasswordField();
            Insets insets = input.getInsets();
            Dimension size = field.getPreferredSize();
            var ui = input.getUI();
            var border = input.getBorder();
            boolean opaque = input.isOpaque();
            int focus = input.getFocusListeners().length;
            int component = input.getComponentListeners().length;
            int properties = input.getPropertyChangeListeners().length;
            input.getAccessibleContext().setAccessibleDescription("Ajuda");
            MaterialLayout first = new MaterialLayout().setError("Senha inválida");
            MaterialLayout second = new MaterialLayout().setError("Informe a senha");
            for (int i = 0; i < 3; i++) {
                field.setFieldLayoutManager(first).setFieldLayoutManager(first);
                assertSame(ui, input.getUI());
                assertSame(border, input.getBorder());
                assertFalse(input.isOpaque());
                assertTrue(field.getPreferredSize().height > size.height);
                assertEquals(focus + 1, input.getFocusListeners().length);
                assertEquals(component + 1, input.getComponentListeners().length);
                assertEquals(properties + 1, input.getPropertyChangeListeners().length);
                assertEquals(AccessibleRole.PASSWORD_TEXT, input.getAccessibleContext().getAccessibleRole());
                assertEquals("Senha", input.getAccessibleContext().getAccessibleName());
                field.setFieldLayoutManager(second);
                assertEquals("Informe a senha", input.getAccessibleContext().getAccessibleDescription());
                field.setFieldLayoutManager(null);
                assertEquals("Ajuda", input.getAccessibleContext().getAccessibleDescription());
                assertEquals(opaque, input.isOpaque());
                assertEquals(insets, input.getInsets());
                assertEquals(size, field.getPreferredSize());
                assertEquals(focus, input.getFocusListeners().length);
                assertEquals(component, input.getComponentListeners().length);
                assertEquals(properties, input.getPropertyChangeListeners().length);
            }
        });
    }

    @Test
    void labelTracksNativeFocusSilentUpdatesAndDocumentReplacement() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PasswordField field = new PasswordField().setLabel("Senha");
            TestLayout layout = new TestLayout();
            field.setFieldLayoutManager(layout);
            AtomicInteger changes = new AtomicInteger();
            field.addEventListener(EventType.CHANGE, e -> {
                assertNull(e.getValue());
                assertTrue(e.getProperties().isEmpty());
                changes.incrementAndGet();
            });
            assertEquals(0f, layout.progress());
            focus(field.getPasswordField(), true);
            assertEquals(1f, layout.progress());
            focus(field.getPasswordField(), false);
            assertEquals(0f, layout.progress());
            field.setPassword("secret".toCharArray(), false);
            assertEquals(1f, layout.progress());
            assertEquals(0, changes.get());
            field.clear(false);
            assertEquals(0f, layout.progress());
            assertEquals(0, changes.get());
            var old = field.getPasswordField().getDocument();
            PlainDocument replacement = new PlainDocument();
            field.getPasswordField().setDocument(replacement);
            int replaced = changes.get();
            try {
                old.insertString(0, "unused", null);
                assertEquals(0f, layout.progress());
                assertEquals(replaced, changes.get());
                replacement.insertString(0, "native", null);
            } catch (javax.swing.text.BadLocationException e) { throw new AssertionError(e); }
            assertEquals(1f, layout.progress());
            assertEquals(replaced + 1, changes.get());
            layout.setError("Erro persistente");
            field.clear(false);
            assertEquals("Erro persistente", layout.getError());
            assertEquals("Erro persistente", field.getPasswordField().getAccessibleContext().getAccessibleDescription());
        });
    }

    @Test
    void rejectsSharingBetweenPasswordAndTextAndRollsBack() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextFieldListener text = new JTextFieldListener();
            PasswordField password = new PasswordField();
            MaterialLayout shared = new MaterialLayout().setError("Texto");
            MaterialLayout previous = new MaterialLayout().setError("Senha");
            text.setFieldLayoutManager(shared);
            password.setFieldLayoutManager(previous);
            assertThrows(IllegalStateException.class, () -> password.setFieldLayoutManager(shared));
            assertSame(previous, password.getFieldLayoutManager());
            assertEquals("Senha", password.getPasswordField().getAccessibleContext().getAccessibleDescription());
            assertEquals("Texto", text.getAccessibleContext().getAccessibleDescription());
            text.setFieldLayoutManager(null);
            password.setFieldLayoutManager(shared);
            assertEquals("Texto", password.getPasswordField().getAccessibleContext().getAccessibleDescription());
            assertThrows(IllegalStateException.class, () -> text.setFieldLayoutManager(shared));
            assertNull(text.getFieldLayoutManager());
            password.setFieldLayoutManager(null);
        });
    }

    @Test
    void visibilityButtonStaysInEditorWithWrappedErrorsAndPreservesSelection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            PasswordField field = new PasswordField().setLabel("Senha").setPassword("secret".toCharArray(), false);
            MaterialLayout layout = new MaterialLayout().setError("A senha precisa de mais caracteres e de uma combinação válida");
            field.setFieldLayoutManager(layout);
            render(field, 220);
            JPasswordField input = field.getPasswordField();
            JButton eye = (JButton) input.getComponent(0);
            Rectangle body = layout.getFieldBounds(field);
            Rectangle content = field.getFieldContentBounds();
            assertTrue(body.contains(eye.getBounds()));
            assertEquals(content.y, eye.getY());
            assertEquals(content.height, eye.getHeight());
            assertTrue(content.x + content.width <= eye.getX());
            assertTrue(eye.getY() + eye.getHeight() < body.y + body.height);
            int wrappedHeight = input.getPreferredSize().height;
            input.select(1, 4);
            eye.doClick();
            assertEquals(0, input.getEchoChar());
            assertEquals(1, input.getSelectionStart());
            assertEquals(4, input.getSelectionEnd());
            input.getActionMap().get("password.toggleVisibility").actionPerformed(null);
            assertNotEquals(0, input.getEchoChar());
            layout.setError(null);
            assertTrue(input.getPreferredSize().height < wrappedHeight);
            field.setEditable(false);
            assertFalse(eye.isEnabled());
            field.setEditable(true).setEnabled(false);
            assertFalse(input.isEnabled());
            assertFalse(eye.isEnabled());
        });
    }

    @Test
    void paintsErrorAndKeepsLayoutAcrossLightAndDarkThemes() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var previous = UIManager.getLookAndFeel();
            BufferedImage gallery = new BufferedImage(600, 280, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = gallery.createGraphics();
            try {
                int column = 0;
                for (var theme : new javax.swing.LookAndFeel[]{new FlatLightLaf(), new FlatDarkLaf()}) {
                    UIManager.setLookAndFeel(theme);
                    PasswordField field = new PasswordField().setLabel("Senha de acesso").setPlaceholder("Digite sua senha");
                    MaterialLayout layout = new MaterialLayout().setErrorColor(Color.RED);
                    field.getPasswordField().getAccessibleContext().setAccessibleDescription("Ajuda externa");
                    field.setFieldLayoutManager(layout);
                    SwingUtilities.updateComponentTreeUI(field);
                    assertSame(layout, field.getFieldLayoutManager());
                    assertFalse(field.getPasswordField().isOpaque());
                    JButton eye = (JButton) field.getPasswordField().getComponent(0);
                    assertSame(field.getPasswordField(), eye.getParent());
                    field.getPasswordField().getActionMap().get("password.toggleVisibility").actionPerformed(null);
                    assertTrue(field.isPasswordVisible());
                    field.setPasswordVisible(false, false);
                    graphics.drawImage(render(field, 280), column * 300, 0, null);
                    field.setPassword("secret".toCharArray(), false);
                    layout.setError("Senha inválida. Verifique os dados informados.");
                    BufferedImage image = render(field, 280);
                    Rectangle body = layout.getFieldBounds(field);
                    int eyePixels = 0;
                    Rectangle eyeBounds = eye.getBounds();
                    for (int y = eyeBounds.y; y < eyeBounds.y + eyeBounds.height; y++) {
                        for (int x = eyeBounds.x; x < eyeBounds.x + eyeBounds.width; x++) {
                            if (image.getRGB(x, y) == UiTokens.foreground().getRGB()) eyePixels++;
                        }
                    }
                    assertTrue(eyePixels > 0);
                    int footerPixels = 0;
                    for (int y = body.y + body.height; y < image.getHeight(); y++) {
                        for (int x = 0; x < image.getWidth(); x++) {
                            if (image.getRGB(x, y) == Color.RED.getRGB()) footerPixels++;
                        }
                    }
                    assertTrue(footerPixels > 0);
                    graphics.drawImage(image, column * 300, 110, null);
                    layout.setError(null);
                    assertEquals("Ajuda externa", field.getPasswordField().getAccessibleContext().getAccessibleDescription());
                    field.setFieldLayoutManager(null);
                    column++;
                }
                Path path = Path.of("target", "password-material-gallery.png");
                Files.createDirectories(path.getParent());
                ImageIO.write(gallery, "png", path.toFile());
            } catch (Exception e) { throw new AssertionError(e); }
            finally {
                graphics.dispose();
                try { UIManager.setLookAndFeel(previous); }
                catch (javax.swing.UnsupportedLookAndFeelException e) { throw new AssertionError(e); }
            }
        });
    }

    private static BufferedImage render(PasswordField field, int width) {
        field.setSize(width, field.getPreferredSize().height);
        field.doLayout();
        field.setSize(width, field.getPreferredSize().height);
        field.doLayout();
        field.getPasswordField().doLayout();
        BufferedImage image = new BufferedImage(width, field.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try { field.paint(graphics); }
        finally { graphics.dispose(); }
        return image;
    }

    private static void focus(JPasswordField input, boolean gained) {
        FocusEvent event = new FocusEvent(input, gained ? FocusEvent.FOCUS_GAINED : FocusEvent.FOCUS_LOST);
        for (var listener : input.getFocusListeners()) {
            if (gained) listener.focusGained(event); else listener.focusLost(event);
        }
    }

    private static class TestLayout extends MaterialLayout {
        float progress() { return getLabelProgress(); }
    }
}
