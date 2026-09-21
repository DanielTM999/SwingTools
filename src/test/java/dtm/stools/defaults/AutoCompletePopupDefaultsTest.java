package dtm.stools.defaults;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopupFactory;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class AutoCompletePopupDefaultsTest {

    @Test
    void intellijFactoryCreatesIndependentCompactPopups() {
        AutoCompletePopupFactory factory = AutoCompletePopupDefaults.intellij();

        AutoCompletePopup first = factory.create(new JPanel());
        AutoCompletePopup second = factory.create(new JPanel());

        assertNotNull(first);
        assertNotSame(first, second);
        assertEquals(new Dimension(420, 192), first.getPopupSize());
        assertEquals(120, first.getDetailMaxHeight());
    }

    @Test
    void visualStudioCodeFactoryUsesItsOwnWiderLayout() {
        AutoCompletePopup intellij = AutoCompletePopupDefaults.intellij().create(new JPanel());
        AutoCompletePopup visualStudioCode = AutoCompletePopupDefaults.visualStudioCode().create(new JPanel());

        assertEquals(new Dimension(480, 216), visualStudioCode.getPopupSize());
        assertEquals(132, visualStudioCode.getDetailMaxHeight());
        assertNotEquals(intellij.getClass(), visualStudioCode.getClass());
    }
}
