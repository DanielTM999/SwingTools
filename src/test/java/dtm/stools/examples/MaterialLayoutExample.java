package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.*;
import dtm.stools.component.inputfields.textfield.layout.MaterialLayout;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;

public class MaterialLayoutExample {
    public static void main(String[] args) { SwingUtilities.invokeLater(MaterialLayoutExample::show); }

    private static void show() {
        FlatLightLaf.setup();
        JFrame frame = new JFrame("MaterialLayout — SwingTools");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JTextFieldListener name = configure(new JTextFieldListener(20), "Nome");
        MaskedTextField cpf = configure(new MaskedTextField("###.###.###-##", 20), "CPF");
        MaterialLayout cpfLayout = (MaterialLayout) cpf.getFieldLayoutManager();
        cpfLayout.setError("Informe os 11 dígitos do CPF. O erro permanece enquanto você digita.");
        NumberField number = configure(new NumberField(Locale.forLanguageTag("pt-BR")), "Quantidade");
        SearchTextField<String> search = configure(new SearchTextField<>(), "Pesquisar");
        search.setDataSource(java.util.List.of("Ana", "Bruno", "Carlos", "Daniel"));
        search.addSearchOption(value -> value);
        search.setIcon(UIManager.getIcon("OptionPane.informationIcon"));
        PathTextField path = configure(new PathTextField("/", 20), "Caminho");
        path.setText("/documentos/projeto");
        JTextFieldListener disabled = configure(new JTextFieldListener(20), "Desabilitado");
        disabled.setText("Texto");
        disabled.setEnabled(false);

        int row = 0;
        for (JTextFieldListener field : new JTextFieldListener[]{name, cpf, number, search, path, disabled}) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = row++;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.insets = new Insets(0, 0, 8, 0);
            form.add(field, c);
        }

        JLabel status = new JLabel("Use Tab para focar. Clique no caminho para editar.");
        cpf.addEventListener(EventType.SUBMIT, e -> status.setText("CPF enviado: " + e.getValue()));
        JButton validate = new JButton("Validar CPF");
        validate.addActionListener(e -> cpfLayout.setError(cpf.isComplete() ? null : "Informe um CPF completo."));
        JButton removeError = new JButton("Remover erro");
        removeError.addActionListener(e -> cpfLayout.setError(null));
        JButton color = new JButton("Trocar cor do erro");
        color.addActionListener(e -> cpfLayout.setErrorColor(new Color(0xC05621)).setError("CPF incompleto."));
        JCheckBox material = new JCheckBox("Material no nome", true);
        MaterialLayout nameLayout = (MaterialLayout) name.getFieldLayoutManager();
        material.addActionListener(e -> name.setFieldLayoutManager(material.isSelected() ? nameLayout : null));
        JCheckBox dark = new JCheckBox("Tema escuro");
        dark.addActionListener(e -> {
            if (dark.isSelected()) FlatDarkLaf.setup(); else FlatLightLaf.setup();
            SwingUtilities.updateComponentTreeUI(frame);
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(validate);
        actions.add(removeError);
        actions.add(color);
        actions.add(material);
        actions.add(dark);
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.setBorder(BorderFactory.createEmptyBorder(0, 24, 16, 24));
        footer.add(actions, BorderLayout.CENTER);
        footer.add(status, BorderLayout.SOUTH);
        frame.add(new JScrollPane(form), BorderLayout.CENTER);
        frame.add(footer, BorderLayout.SOUTH);
        frame.setSize(820, 760);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        validate.requestFocusInWindow();
    }

    private static <T extends JTextFieldListener> T configure(T field, String label) {
        field.setLabel(label);
        field.setFieldLayoutManager(new MaterialLayout());
        return field;
    }
}
