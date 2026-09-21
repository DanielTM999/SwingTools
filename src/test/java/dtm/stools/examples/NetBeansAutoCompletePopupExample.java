package dtm.stools.examples;

import dtm.stools.defaults.AutoCompletePopupDefaults;

import javax.swing.SwingUtilities;

public class NetBeansAutoCompletePopupExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> AutoCompletePopupExampleSupport.launch(
                "AutoCompletePopup - NetBeans",
                AutoCompletePopupDefaults.netBeans()));
    }
}
