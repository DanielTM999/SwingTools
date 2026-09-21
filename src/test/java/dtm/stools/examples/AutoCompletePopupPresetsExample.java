package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.panels.editor.code.autocomplete.AutoCompletePopupFactory;
import dtm.stools.defaults.AutoCompletePopupDefaults;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/** Lets the four built-in popup presets be selected from one example. */
public class AutoCompletePopupPresetsExample {

    private static final String INTELLIJ = "IntelliJ IDEA";
    private static final String VS_CODE = "Visual Studio Code";
    private static final String ECLIPSE = "Eclipse";
    private static final String NET_BEANS = "NetBeans";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AutoCompletePopupPresetsExample::choosePreset);
    }

    private static void choosePreset() {
        FlatDarkLaf.setup();
        String selected = (String) JOptionPane.showInputDialog(
                null,
                "Escolha o estilo do autocomplete:",
                "AutoCompletePopup - Presets",
                JOptionPane.PLAIN_MESSAGE,
                null,
                new String[]{INTELLIJ, VS_CODE, ECLIPSE, NET_BEANS},
                INTELLIJ);

        if (selected == null) return;

        AutoCompletePopupFactory factory = switch (selected) {
            case VS_CODE -> AutoCompletePopupDefaults.visualStudioCode();
            case ECLIPSE -> AutoCompletePopupDefaults.eclipse();
            case NET_BEANS -> AutoCompletePopupDefaults.netBeans();
            default -> AutoCompletePopupDefaults.intellij();
        };
        AutoCompletePopupExampleSupport.launch("AutoCompletePopup - " + selected, factory);
    }
}
