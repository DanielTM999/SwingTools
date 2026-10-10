package dtm.stools.examples;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.command.*;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.event.*;
import java.util.List;

public final class CommandPaletteExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            JFrame frame = new JFrame("CommandPalette"); frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            JLabel status = new JLabel("Ctrl+Shift+P abre a paleta"); JCheckBox enable = new JCheckBox("Habilitar salvar", true);
            CommandPalette palette = new CommandPalette(() -> List.of(
                new CommandEntry("save", "Salvar", "Arquivo", "Gravar alterações", "Ctrl+S", enable.isSelected()),
                new CommandEntry("preferences", "Preferências", "Aplicação", "Configuração da interface", "", true)),
                id -> { status.setText("Executado: " + id); return true; });
            JButton open = new JButton("Abrir comandos"); open.addActionListener(e -> palette.open(frame));
            AutoCloseable registration = palette.installShortcut(frame.getRootPane(), KeyStroke.getKeyStroke("control shift P"));
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    palette.close();
                    try { registration.close(); } catch (Exception error) { throw new IllegalStateException(error); }
                }
            });
            JPanel panel = new JPanel(new BorderLayout(12, 12)); panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            panel.add(status, BorderLayout.NORTH); panel.add(enable); panel.add(open, BorderLayout.SOUTH);
            frame.setContentPane(panel); frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true);
        });
    }
}
