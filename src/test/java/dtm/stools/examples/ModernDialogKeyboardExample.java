package dtm.stools.examples;

import dtm.stools.component.popup.ModernComponentDialog;
import dtm.stools.component.popup.ModernDialog;
import dtm.stools.component.popup.ModernInputDialog;

import javax.swing.*;
import java.awt.*;


public class ModernDialogKeyboardExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModernDialogKeyboardExample::createAndShow);
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("ModernDialog - Navegacao por teclado");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(560, 360);
        frame.setLocationRelativeTo(null);

        JLabel resultLabel = new JLabel("Use Tab para mover o foco e Enter para confirmar.");
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton confirmDialog = new JButton("ModernDialog (Tab + Enter)");
        confirmDialog.addActionListener(e -> {
            int option = ModernDialog.builder()
                    .title("Excluir item")
                    .message("Tem certeza? Use Tab para escolher e Enter para confirmar.")
                    .type(ModernDialog.Type.QUESTION)
                    .option("Excluir", 0, new Color(0xEF4444), Color.WHITE)
                    .option("Cancelar", 1)
                    .enterConfirms(true) // padrao; Enter aciona "Excluir" (botao primario)
                    .parent(frame)
                    .show();

            resultLabel.setText("ModernDialog retornou: " + option);
        });

        JButton inputDialog = new JButton("ModernInputDialog (Enter no campo)");
        inputDialog.addActionListener(e -> {
            String result = ModernInputDialog.builder()
                    .title("Novo nome")
                    .message("Digite e pressione Enter para confirmar.")
                    .confirmText("Salvar")
                    .cancelText("Cancelar")
                    .enterConfirms(true) // Enter confirma mesmo com o foco no campo de texto
                    .parent(frame)
                    .show();

            resultLabel.setText(result != null
                    ? "Input confirmado: " + result.trim()
                    : "Input cancelado.");
        });

        JButton componentDialog = new JButton("ModernComponentDialog (Enter desabilitado)");
        componentDialog.addActionListener(e -> {
            JComboBox<String> combo = new JComboBox<>(new String[]{"Pequeno", "Medio", "Grande"});

            String result = ModernComponentDialog.<String>builder(String.class)
                    .title("Escolher tamanho")
                    .message("Aqui o Enter NAO confirma; use Tab + Espaco nos botoes.")
                    .type(ModernDialog.Type.INFO)
                    .component(combo)
                    .result(context -> (String) combo.getSelectedItem())
                    .confirmText("Aplicar")
                    .cancelText("Cancelar")
                    .enterConfirms(false) // desliga o atalho Enter
                    .parent(frame)
                    .show();

            resultLabel.setText(result != null
                    ? "Componente confirmado: " + result
                    : "Componente cancelado.");
        });

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 10));
        buttons.add(confirmDialog);
        buttons.add(inputDialog);
        buttons.add(componentDialog);

        JPanel content = new JPanel(new BorderLayout(12, 16));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.add(resultLabel, BorderLayout.NORTH);
        content.add(buttons, BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.setVisible(true);
    }
}
