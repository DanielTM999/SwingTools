package dtm.stools.defaults;

import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopup;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import java.awt.Dimension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AdditionalAutoCompletePopupDefaultsTest {

    @Test
    void eclipsePresetUsesACompactEightRowLayout() {
        AutoCompletePopup eclipse = AutoCompletePopupDefaults.eclipse().create(new JPanel());

        assertEquals(new Dimension(440, 208), eclipse.getPopupSize());
        assertEquals(128, eclipse.getDetailMaxHeight());
    }

    @Test
    void netBeansPresetUsesItsOwnSevenRowLayout() {
        AutoCompletePopup eclipse = AutoCompletePopupDefaults.eclipse().create(new JPanel());
        AutoCompletePopup netBeans = AutoCompletePopupDefaults.netBeans().create(new JPanel());

        assertEquals(new Dimension(460, 224), netBeans.getPopupSize());
        assertEquals(136, netBeans.getDetailMaxHeight());
        assertNotEquals(eclipse.getClass(), netBeans.getClass());
    }
}
