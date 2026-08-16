package dtm.stools.examples;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.switchfield.SwitchField;

import javax.swing.*;
import java.awt.*;

public class SwitchFieldExample {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SwitchFieldExample::createAndShow);
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("SwitchField - tamanhos e geometria");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(680, 520);
        frame.setLocationRelativeTo(null);

        JPanel examples = new JPanel(new GridBagLayout());
        examples.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel eventStatus = new JLabel("Altere um switch para visualizar o evento.");
        eventStatus.setForeground(new Color(0x2563EB));

        SwitchField standard = observe("Padrao", new SwitchField(), eventStatus);
        addRow(examples, gbc, 0, "Padrao (56 x 30)", standard);

        SwitchField withText = observe("Com texto", new SwitchField(true)
                .setShowText(true)
                .setTexts("SIM", "NAO")
                .setSwitchSize(82, 34), eventStatus);
        addRow(examples, gbc, 1, "Texto interno", withText);

        SwitchField compact = observe("Compacto", new SwitchField()
                .setSwitchSize(44, 22)
                .setThumbPadding(2)
                .setThumbSize(16)
                .setTrackArc(10), eventStatus);
        addRow(examples, gbc, 2, "Compacto customizado", compact);

        SwitchField large = observe("Grande", new SwitchField(true)
                .setSwitchSize(110, 48)
                .setTrackInsets(new Insets(2, 4, 2, 4))
                .setThumbSize(34)
                .setThumbPadding(4)
                .setTrackArc(20)
                .setFocusStrokeWidth(3f)
                .setFocusGap(2)
                .setColors(new Color(0x16A34A), new Color(0x475569), Color.WHITE), eventStatus);
        addRow(examples, gbc, 3, "Grande + geometria", large);

        SwitchField disabled = new SwitchField(true)
                .setShowText(true)
                .setTexts("ON", "OFF")
                .setSwitchSize(76, 32)
                .setDisabledColor(new Color(0x9CA3AF));
        disabled.setEnabled(false);
        addRow(examples, gbc, 4, "Desabilitado", disabled);

        JPanel liveEditor = createLiveEditor(large);
        liveEditor.setBorder(BorderFactory.createTitledBorder("Ajuste o switch grande em tempo real"));

        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(examples, BorderLayout.CENTER);
        content.add(liveEditor, BorderLayout.EAST);
        content.add(eventStatus, BorderLayout.SOUTH);
        frame.setContentPane(content);
        frame.setVisible(true);
    }

    private static JPanel createLiveEditor(SwitchField field) {
        JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
        JSpinner width = new JSpinner(new SpinnerNumberModel(110, 30, 220, 2));
        JSpinner height = new JSpinner(new SpinnerNumberModel(48, 16, 100, 2));
        JSpinner thumb = new JSpinner(new SpinnerNumberModel(34, 0, 90, 1));
        JSpinner padding = new JSpinner(new SpinnerNumberModel(4, 0, 20, 1));

        Runnable update = () -> field
                .setSwitchSize((Integer) width.getValue(), (Integer) height.getValue())
                .setThumbSize((Integer) thumb.getValue())
                .setThumbPadding((Integer) padding.getValue());
        width.addChangeListener(event -> update.run());
        height.addChangeListener(event -> update.run());
        thumb.addChangeListener(event -> update.run());
        padding.addChangeListener(event -> update.run());

        panel.add(labeled("Largura", width));
        panel.add(labeled("Altura", height));
        panel.add(labeled("Thumb (0 = auto)", thumb));
        panel.add(labeled("Padding", padding));
        return panel;
    }

    private static JComponent labeled(String text, JComponent component) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.add(new JLabel(text), BorderLayout.WEST);
        row.add(component, BorderLayout.CENTER);
        return row;
    }

    private static SwitchField observe(String name, SwitchField field, JLabel status) {
        field.addEventListener(EventType.CHANGE, event ->
                status.setText(name + ": selected=" + event.getValue()
                        + "  oldValue=" + event.getProperties().get("oldValue")));
        return field;
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, int row,
                               String label, SwitchField field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(field, gbc);
    }
}
