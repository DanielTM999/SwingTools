package dtm.stools.examples;

import dtm.stools.component.popup.ModernInputDialog;

import javax.swing.*;
import java.awt.*;

public class ModernInputDialogExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModernInputDialogExample::createAndShow);
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("ModernInputDialog Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(520, 320);
        frame.setLocationRelativeTo(null);
        frame.setLayout(new BorderLayout());

        JLabel resultLabel = new JLabel("Nenhum arquivo criado.");
        resultLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton button = new JButton("Criar arquivo");
        button.addActionListener(e -> {
            String result = ModernInputDialog.builder()
                    .title("Novo arquivo")
                    .message("Informe o nome do arquivo.")
                    .confirmText("Criar")
                    .cancelText("Cancelar")
                    .draggable(true)
                    .validationDelayMs(350)
                    .accentColor(new Color(0x3B82F6))
                    .errorColor(new Color(0xEF4444))
                    .confirmButtonColor(new Color(0x22C55E))
                    .confirmButtonForeground(Color.WHITE)
                    .cancelButtonColor(new Color(0x374151))
                    .cancelButtonForeground(Color.WHITE)
                    .onValidate(context -> {
                        String value = context.value();

                        if (value == null || value.isBlank()) {
                            return;
                        }

                        String fileName = value.trim();

                        if (fileName.contains("/") || fileName.contains("\\")) {
                            throw new IllegalArgumentException("O nome não pode conter barras.");
                        }

                        if (fileName.equals(".") || fileName.equals("..")) {
                            throw new IllegalArgumentException("Nome de arquivo inválido.");
                        }

                        if (!fileName.contains(".")) {
                            throw new IllegalArgumentException("Informe a extensão. Exemplo: Main.java");
                        }

                        if (fileName.length() < 5) {
                            throw new IllegalArgumentException("O nome do arquivo está muito curto.");
                        }
                    })
                    .onSubmit(context -> {
                        String value = context.value();

                        if (value == null || value.isBlank()) {
                            throw new IllegalArgumentException("Informe o nome do arquivo.");
                        }

                        String fileName = value.trim();

                        System.out.println("Confirmado: " + fileName);
                    })
                    .parent(frame)
                    .show();

            if (result != null) {
                resultLabel.setText("Arquivo confirmado: " + result.trim());
            } else {
                resultLabel.setText("Operação cancelada.");
            }
        });

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        content.add(resultLabel, BorderLayout.CENTER);
        content.add(button, BorderLayout.SOUTH);

        frame.setContentPane(content);
        frame.setVisible(true);
    }
}
