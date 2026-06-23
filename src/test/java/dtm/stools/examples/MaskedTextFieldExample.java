package dtm.stools.examples;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.MaskedTextField;

import javax.swing.*;
import java.awt.*;

public class MaskedTextFieldExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MaskedTextFieldExample::createAndShow);
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("MaskedTextField - Mascaras alternativas");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(560, 620);
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = 0;

        // Alternativas: "#:#" OU ate 4 digitos
        MaskedTextField alternated = new MaskedTextField("#:#|####", 16);
        JLabel alternatedStatus = new JLabel("vazio");
        alternated.addEventListener(EventType.INPUT, e -> {
            String clean = e.tryGetValue();
            alternatedStatus.setText("limpo=\"" + clean + "\"  completo=" + alternated.isComplete());
        });
        row = addRow(content, gbc, row, "Mascara \"#:#|####\":", alternated, alternatedStatus);

        // Caracteres opcionais: 1 digito obrigatorio + ate 3 opcionais
        MaskedTextField optionalChars = new MaskedTextField("#[#][#][#]", 16);
        JLabel optionalStatus = new JLabel("vazio");
        optionalChars.addEventListener(EventType.INPUT, e ->
                optionalStatus.setText("limpo=\"" + e.tryGetValue() + "\"  completo=" + optionalChars.isComplete()));
        row = addRow(content, gbc, row, "Mascara \"#[#][#][#]\":", optionalChars, optionalStatus);

        // Placeholder customizado sobrepoe a dica gerada
        MaskedTextField withPlaceholder = new MaskedTextField("###.###.###-##|##.###.###/####-##", 20);
        withPlaceholder.setPlaceholder("CPF ou CNPJ");
        JLabel placeholderStatus = new JLabel("vazio");
        withPlaceholder.addEventListener(EventType.CHANGE, e ->
                placeholderStatus.setText("valor confirmado: " + e.tryGetValue()));
        row = addRow(content, gbc, row, "CPF/CNPJ (placeholder):", withPlaceholder, placeholderStatus);

        // Escape de literal: usa '[' e ']' como literais
        MaskedTextField escaped = new MaskedTextField("\\[##\\]", 16);
        JLabel escapedStatus = new JLabel("vazio");
        escaped.addEventListener(EventType.INPUT, e ->
                escapedStatus.setText("limpo=\"" + e.tryGetValue() + "\""));
        row = addRow(content, gbc, row, "Escape \"\\[##\\]\":", escaped, escapedStatus);

        // Quantificador 'um ou mais': 1+ digitos sem limite
        MaskedTextField oneOrMore = new MaskedTextField("#+", 16);
        JLabel oneOrMoreStatus = new JLabel("vazio");
        oneOrMore.addEventListener(EventType.INPUT, e ->
                oneOrMoreStatus.setText("limpo=\"" + e.tryGetValue() + "\"  completo=" + oneOrMore.isComplete()));
        row = addRow(content, gbc, row, "Um ou mais \"#+\":", oneOrMore, oneOrMoreStatus);

        // Escape do '+': literal entre dois digitos
        MaskedTextField escapedPlus = new MaskedTextField("#\\+#", 16);
        JLabel escapedPlusStatus = new JLabel("vazio");
        escapedPlus.addEventListener(EventType.INPUT, e ->
                escapedPlusStatus.setText("limpo=\"" + e.tryGetValue() + "\""));
        row = addRow(content, gbc, row, "Escape \"#\\+#\":", escapedPlus, escapedPlusStatus);

        // Separador digitavel: digite ':' para encerrar o primeiro grupo
        MaskedTextField separated = new MaskedTextField("#+:#+|#+", 16);
        JLabel separatedStatus = new JLabel("vazio");
        separated.addEventListener(EventType.INPUT, e ->
                separatedStatus.setText("limpo=\"" + e.tryGetValue() + "\"  completo=" + separated.isComplete()));
        row = addRow(content, gbc, row, "Separador \"#+:#+|#+\":", separated, separatedStatus);

        frame.setContentPane(content);
        frame.setVisible(true);
    }

    private static int addRow(JPanel panel, GridBagConstraints gbc, int row,
                              String label, JComponent field, JComponent status) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);

        gbc.gridx = 0;
        gbc.gridy = row + 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        status.setForeground(new Color(0x2563EB));
        panel.add(status, gbc);
        gbc.gridwidth = 1;

        return row + 2;
    }
}
