package dtm.stools.examples;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.NumberField;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.util.Locale;

public class NumberFieldExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NumberFieldExample::createAndShow);
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("NumberField - locale, limites e passo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(720, 480);
        frame.setLocationRelativeTo(null);

        JTextArea log = new JTextArea(7, 48);
        log.setEditable(false);
        log.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setBorder(BorderFactory.createEmptyBorder(20, 20, 8, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(7, 7, 7, 7);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        NumberField decimalBr = observe("Decimal pt-BR", new NumberField(Locale.forLanguageTag("pt-BR"))
                .setDecimalPlaces(2)
                .setRange(BigDecimal.ZERO, new BigDecimal("1000"))
                .setStep(new BigDecimal("0.25"))
                .setValue(new BigDecimal("10.50"), false), log);
        addRow(fields, gbc, 0, "Decimal pt-BR (0..1000, passo 0,25)", decimalBr);

        NumberField integer = observe("Inteiro", new NumberField(Locale.forLanguageTag("pt-BR"))
                .setDecimalPlaces(0)
                .setRange(BigDecimal.ZERO, new BigDecimal("100"))
                .setStep(BigDecimal.ONE)
                .setValue(new BigDecimal("25"), false), log);
        addRow(fields, gbc, 1, "Inteiro (0..100)", integer);

        NumberField decimalUs = observe("Decimal en-US", new NumberField(Locale.US)
                .setDecimalPlaces(3)
                .setRange(new BigDecimal("-10"), new BigDecimal("10"))
                .setStep(new BigDecimal("0.125"))
                .setValue(new BigDecimal("1.375"), false), log);
        addRow(fields, gbc, 2, "Decimal en-US (-10..10, passo 0.125)", decimalUs);

        NumberField optional = observe("Opcional", new NumberField(Locale.getDefault())
                .setDecimalPlaces(4)
                .setStep(new BigDecimal("0.0001")), log);
        addRow(fields, gbc, 3, "Opcional, sem limites (vazio permitido)", optional);

        JLabel help = new JLabel("Use ↑/↓ ou a roda com foco. Enter e perda de foco confirmam e limitam o valor.");
        help.setForeground(new Color(0x475569));

        JPanel content = new JPanel(new BorderLayout(10, 10));
        content.add(fields, BorderLayout.NORTH);
        content.add(help, BorderLayout.CENTER);
        content.add(new JScrollPane(log), BorderLayout.SOUTH);
        frame.setContentPane(content);
        frame.setVisible(true);
    }

    private static NumberField observe(String name, NumberField field, JTextArea log) {
        field.setColumns(14);
        field.addEventListener(EventType.INPUT, event ->
                append(log, name + " INPUT  value=" + event.getValue()));
        field.addEventListener(EventType.CHANGE, event ->
                append(log, name + " CHANGE value=" + event.getValue()));
        field.addEventListener(EventType.SUBMIT, event ->
                append(log, name + " SUBMIT value=" + event.getValue()));
        return field;
    }

    private static void append(JTextArea log, String message) {
        log.append(message + System.lineSeparator());
        log.setCaretPosition(log.getDocument().getLength());
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, int row,
                               String label, NumberField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }
}
