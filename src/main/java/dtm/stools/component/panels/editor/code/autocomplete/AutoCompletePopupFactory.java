package dtm.stools.component.panels.editor.code.autocomplete;

import javax.swing.JComponent;

@FunctionalInterface
public interface AutoCompletePopupFactory {
    AutoCompletePopup create(JComponent owner);
}
