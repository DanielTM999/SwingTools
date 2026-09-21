package dtm.stools.examples;

import dtm.stools.defaults.AutoCompletePopupDefaults;

import javax.swing.SwingUtilities;

public class VisualStudioCodeAutoCompletePopupExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> AutoCompletePopupExampleSupport.launch(
                "AutoCompletePopup - Visual Studio Code",
                AutoCompletePopupDefaults.visualStudioCode()));
    }
}
