package dtm.stools.examples;

import dtm.stools.defaults.AutoCompletePopupDefaults;

import javax.swing.SwingUtilities;

public class EclipseAutoCompletePopupExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> AutoCompletePopupExampleSupport.launch(
                "AutoCompletePopup - Eclipse",
                AutoCompletePopupDefaults.eclipse()));
    }
}
