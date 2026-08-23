package dtm.stools.examples;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.checkfield.CheckBoxField;
import dtm.stools.component.inputfields.checkfield.RadioGroupField;
import dtm.stools.component.inputfields.duallistfield.DualListField;
import dtm.stools.component.inputfields.pinfield.PinField;
import dtm.stools.component.inputfields.ratingfield.RatingField;
import dtm.stools.component.inputfields.segmentedfield.SegmentedField;
import dtm.stools.component.inputfields.sliderfield.SliderField;
import dtm.stools.component.inputfields.stepperfield.StepperField;
import dtm.stools.component.inputfields.textarea.TextAreaField;
import dtm.stools.component.events.EventListenerComponent;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;

public class ModernFieldsExample {

    private static final JTextArea LOG = new JTextArea(8, 40);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModernFieldsExample::createAndShow);
    }

    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
        } catch (Exception ignored) {
        }

        JFrame frame = new JFrame("Componentes modernos - campos de formulário");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(980, 780);
        frame.setLocationRelativeTo(null);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        int row = 0;
        row = addRow(content, row, "CheckBoxField", buildCheckBoxes());
        row = addRow(content, row, "RadioGroupField", buildRadioGroup());
        row = addRow(content, row, "SegmentedField", buildSegmented());
        row = addRow(content, row, "SliderField", buildSlider());
        row = addRow(content, row, "RatingField", buildRating());
        row = addRow(content, row, "PinField", buildPin());
        row = addRow(content, row, "StepperField", buildStepper());
        row = addRow(content, row, "TextAreaField", buildTextArea());
        addRow(content, row, "DualListField", buildDualList());

        LOG.setEditable(false);
        LOG.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        frame.add(new JScrollPane(content), BorderLayout.CENTER);
        frame.add(new JScrollPane(LOG), BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private static Component buildCheckBoxes() {
        JPanel panel = row();
        CheckBoxField simple = new CheckBoxField("Aceito os termos");
        CheckBoxField checked = new CheckBoxField("Receber novidades", true);
        CheckBoxField partial = new CheckBoxField("Seleção parcial").setIndeterminate(true);
        CheckBoxField disabled = new CheckBoxField("Desabilitado", true);
        disabled.setEnabled(false);

        observe("CheckBoxField", simple);
        observe("CheckBoxField", checked);

        panel.add(simple);
        panel.add(checked);
        panel.add(partial);
        panel.add(disabled);
        return panel;
    }

    private static Component buildRadioGroup() {
        RadioGroupField<String> group = new RadioGroupField<>();
        group.setOptions(new LinkedHashMap<>(java.util.Map.of()) {{
            put("Mensal", "MONTHLY");
            put("Trimestral", "QUARTERLY");
            put("Anual", "YEARLY");
        }});
        group.setSelectedValue("MONTHLY", false);
        observe("RadioGroupField", group);
        return group;
    }

    private static Component buildSegmented() {
        SegmentedField<String> segmented = new SegmentedField<>();
        segmented.addSegment("Dia", "DAY")
                .addSegment("Semana", "WEEK")
                .addSegment("Mês", "MONTH");
        observe("SegmentedField", segmented);
        return segmented;
    }

    private static Component buildSlider() {
        JPanel panel = row();
        SliderField basic = new SliderField(0, 100, 40);
        SliderField stepped = new SliderField(0, 10, 6).setStep(1).setShowValue(true).setShowTicks(true).setTickCount(11);
        observe("SliderField", basic);
        observe("SliderField", stepped);
        panel.add(basic);
        panel.add(stepped);
        return panel;
    }

    private static Component buildRating() {
        JPanel panel = row();
        RatingField stars = new RatingField(5, 3);
        RatingField half = new RatingField(5, 3.5).setAllowHalf(true);
        RatingField readOnly = new RatingField(5, 4).setReadOnly(true);
        observe("RatingField", stars);
        observe("RatingField", half);
        panel.add(stars);
        panel.add(half);
        panel.add(readOnly);
        return panel;
    }

    private static Component buildPin() {
        JPanel panel = row();
        PinField code = new PinField(6);
        PinField masked = new PinField(4).setMasked(true);
        observe("PinField", code);
        observe("PinField", masked);
        panel.add(code);
        panel.add(masked);
        return panel;
    }

    private static Component buildStepper() {
        StepperField stepper = new StepperField(BigDecimal.valueOf(1));
        stepper.setRange(BigDecimal.ZERO, BigDecimal.valueOf(10))
                .setStep(BigDecimal.ONE)
                .setDecimalPlaces(0);
        observe("StepperField", stepper);
        return stepper;
    }

    private static Component buildTextArea() {
        TextAreaField area = new TextAreaField("Descreva o chamado...");
        area.setMaxLength(280).setAutoGrow(true);
        area.setPreferredSize(new Dimension(420, 110));
        observe("TextAreaField", area);
        return area;
    }

    private static Component buildDualList() {
        DualListField<String> dual = new DualListField<>(List.of(
                "Administrador", "Financeiro", "Suporte", "Vendas",
                "Estoque", "Compras", "Auditoria", "Relatórios"));
        dual.setTitles("Perfis disponíveis", "Perfis do usuário")
                .setReorderable(true)
                .setShowFilter(true);
        dual.setPreferredSize(new Dimension(620, 240));
        observe("DualListField", dual);
        return dual;
    }

    private static JPanel row() {
        JPanel panel = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 18, 0));
        panel.setOpaque(false);
        return panel;
    }

    private static void observe(String name, EventListenerComponent component) {
        component.addEventListener(EventType.CHANGE, event ->
                log(name + " -> " + event.getValue()));
    }

    private static void log(String message) {
        LOG.append(message + System.lineSeparator());
        LOG.setCaretPosition(LOG.getDocument().getLength());
    }

    private static int addRow(JPanel parent, int row, String title, Component component) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(10, 0, 10, 18);

        JLabel label = new JLabel(title);
        label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD));
        parent.add(label, labelConstraints);

        GridBagConstraints fieldConstraints = new GridBagConstraints();
        fieldConstraints.gridx = 1;
        fieldConstraints.gridy = row;
        fieldConstraints.weightx = 1;
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
        fieldConstraints.anchor = GridBagConstraints.WEST;
        fieldConstraints.insets = new Insets(10, 0, 10, 0);
        parent.add(component, fieldConstraints);

        return row + 1;
    }
}
