package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoCompletePopupFactoryTest {

    @Test
    void factoryReceivesOwnerImmediatelyAndConfiguredHookIsPreserved() {
        TestTextArea textArea = new TestTextArea();
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<JComponent> owner = new AtomicReference<>();
        TestPopup popup = new TestPopup(textArea);

        textArea.setAutoCompletePopupFactory(component -> {
            calls.incrementAndGet();
            owner.set(component);
            return popup;
        });

        assertEquals(1, calls.get());
        assertSame(textArea, owner.get());
        assertSame(popup, textArea.getAutoCompletePopup());
        assertSame(popup, textArea.configuredPopup);
        assertEquals(1, textArea.configureCalls);
        assertNotNull(popup.acceptHandler());
    }

    @Test
    void replacingFactoryHidesPreviousPopup() {
        TestTextArea textArea = new TestTextArea();
        TestPopup first = new TestPopup(textArea);
        TestPopup second = new TestPopup(textArea);

        textArea.setAutoCompletePopupFactory(owner -> first);
        textArea.setAutoCompletePopupFactory(owner -> second);

        assertTrue(first.hidden);
        assertFalse(second.hidden);
        assertSame(second, textArea.getAutoCompletePopup());
    }

    @Test
    void nullFactoryAndNullFactoryResultAreRejectedWithoutReplacingCurrentPopup() {
        TestTextArea textArea = new TestTextArea();
        TestPopup current = new TestPopup(textArea);
        textArea.setAutoCompletePopupFactory(owner -> current);

        assertThrows(NullPointerException.class, () -> textArea.setAutoCompletePopupFactory(null));
        assertThrows(NullPointerException.class, () -> textArea.setAutoCompletePopupFactory(owner -> null));

        assertSame(current, textArea.getAutoCompletePopup());
        assertFalse(current.hidden);
    }

    @Test
    void defaultPopupCreationRemainsLazyAndUsesConfigureHook() {
        TestTextArea textArea = new TestTextArea();

        assertEquals(0, textArea.configureCalls);
        AutoCompletePopup popup = textArea.createDefaultPopup();

        assertNotNull(popup);
        assertSame(popup, textArea.configuredPopup);
        assertEquals(1, textArea.configureCalls);
    }

    @Test
    void codeEditorDelegatesFactoryToItsTextArea() {
        CodeEditor editor = new CodeEditor();
        AtomicReference<JComponent> owner = new AtomicReference<>();
        TestPopup popup = new TestPopup(editor.getTextArea());

        editor.setAutoCompletePopupFactory(component -> {
            owner.set(component);
            return popup;
        });

        assertSame(editor.getTextArea(), owner.get());
        assertSame(popup, editor.getTextArea().getAutoCompletePopup());
    }

    private static final class TestTextArea extends CodeEditorTextArea {
        private int configureCalls;
        private AutoCompletePopup configuredPopup;

        @Override
        protected void configureAutoCompletePopup(AutoCompletePopup popup) {
            super.configureAutoCompletePopup(popup);
            configureCalls++;
            configuredPopup = popup;
        }

        private AutoCompletePopup createDefaultPopup() {
            return getOrCreateAutoCompletePopup();
        }
    }

    private static final class TestPopup extends AutoCompletePopup {
        private boolean hidden;

        private TestPopup(JComponent owner) {
            super(owner);
        }

        @Override
        public void hide() {
            hidden = true;
            super.hide();
        }

        private Runnable acceptHandler() {
            return acceptHandler;
        }
    }
}
