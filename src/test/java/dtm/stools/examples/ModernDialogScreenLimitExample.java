package dtm.stools.examples;

import dtm.stools.component.popup.ModernDialog;
import dtm.stools.context.Dialogs;

import javax.swing.*;
import java.awt.*;

public class ModernDialogScreenLimitExample {

    private static final String LONG_MESSAGE =
            ("Linha de exemplo para demonstrar a rolagem da mensagem.<br>").repeat(120);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModernDialogScreenLimitExample::createAndShow);
    }

    private static void createAndShow() {
        JFrame frame = new JFrame("ModernDialog - limite da tela");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JCheckBox limitToScreen = new JCheckBox("Limitar dialog a tela", true);

        JButton modernDialogButton = new JButton("Abrir com ModernDialog");
        modernDialogButton.addActionListener(event -> ModernDialog.builder()
                .title("Mensagem extensa")
                .message(LONG_MESSAGE)
                .type(ModernDialog.Type.INFO)
                .limitToScreen(limitToScreen.isSelected())
                .parent(frame)
                .show());

        JButton dialogsFacadeButton = new JButton("Abrir com Dialogs");
        dialogsFacadeButton.addActionListener(event -> Dialogs.builder()
                .title("Mensagem extensa pela fachada")
                .message(LONG_MESSAGE)
                .type(ModernDialog.Type.INFO)
                .limitToScreen(limitToScreen.isSelected())
                .parent(frame)
                .show());

        JLabel hint = new JLabel(
                "<html>Ativado por padrao. Desative para comparar; use Esc para fechar.</html>"
        );

        JPanel content = new JPanel(new GridLayout(0, 1, 0, 10));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.add(limitToScreen);
        content.add(modernDialogButton);
        content.add(dialogsFacadeButton);
        content.add(hint);

        frame.setContentPane(content);
        frame.pack();
        frame.setMinimumSize(new Dimension(420, frame.getHeight()));
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
