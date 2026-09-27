package dtm.stools.defaults;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompleteItem;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopupFactory;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void unusedItemsRenderWithFadedTitle() throws Exception {
        AutoCompletePopup popup = AutoCompletePopupDefaults.visualStudioCode().create(new JPanel());
        JList<AutoCompleteItem> list = listOf(popup);
        AutoCompleteItem used = new AutoCompleteItem("run()", "run", null, null, null, AutoCompleteItem.Kind.METHOD);

        Color usedColor = titleColor(list, used);
        Color unusedColor = titleColor(list, used.withUnused(true));

        assertNotEquals(usedColor, unusedColor);
        assertTrue(contrast(unusedColor, list.getBackground()) < contrast(usedColor, list.getBackground()));
    }

    @SuppressWarnings("unchecked")
    private static JList<AutoCompleteItem> listOf(AutoCompletePopup popup) throws Exception {
        Field field = AutoCompletePopup.class.getDeclaredField("list");
        field.setAccessible(true);
        return (JList<AutoCompleteItem>) field.get(popup);
    }

    private static Color titleColor(JList<AutoCompleteItem> list, AutoCompleteItem item) {
        Component cell = list.getCellRenderer().getListCellRendererComponent(list, item, 0, false, false);
        JLabel title = findLabel(cell, item.label());
        assertNotNull(title);
        return title.getForeground();
    }

    private static JLabel findLabel(Component component, String text) {
        if (component instanceof JLabel label && text.equals(label.getText())) {
            return label;
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JLabel found = findLabel(child, text);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static int contrast(Color a, Color b) {
        return Math.abs(a.getRed() - b.getRed()) + Math.abs(a.getGreen() - b.getGreen())
                + Math.abs(a.getBlue() - b.getBlue());
    }
}
