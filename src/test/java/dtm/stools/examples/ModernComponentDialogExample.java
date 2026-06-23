package dtm.stools.examples;

import com.formdev.flatlaf.FlatDarkLaf;
import dtm.stools.component.popup.ModernComponentDialog;
import dtm.stools.component.popup.ModernDialog;
import dtm.stools.context.Dialogs;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ModernComponentDialogExample {

    private static final Color APP_BACKGROUND = new Color(0x20242C);
    private static final Color PANEL_BACKGROUND = new Color(0x2A303A);
    private static final Color TEXT_COLOR = new Color(0xE5E7EB);
    private static final Color MUTED_TEXT_COLOR = new Color(0xAAB2C0);
    private static final Color ACCENT_COLOR = new Color(0x3B82F6);

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ModernComponentDialogExample::createAndShow);
    }

    private static void createAndShow() {
        FlatDarkLaf.setup();

        JFrame frame = new JFrame("ModernComponentDialog Example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setMinimumSize(new Dimension(680, 420));
        frame.setSize(760, 460);
        frame.setLocationRelativeTo(null);

        JTextArea output = new JTextArea("Nenhum dialog confirmado.");
        output.setEditable(false);
        output.setLineWrap(true);
        output.setWrapStyleWord(true);
        output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        output.setForeground(TEXT_COLOR);
        output.setBackground(new Color(0x1B1F27));
        output.setBorder(new EmptyBorder(14, 16, 14, 16));

        JButton formButton = button("Abrir formulario interno");
        formButton.addActionListener(event -> {
            Usuario usuario = showFormDialog(frame);

            if (usuario == null) {
                output.setText("Formulario cancelado.");
                return;
            }

            output.setText("Formulario retornou:\n" + usuario);
        });

        JButton customButton = button("Abrir componente customizado");
        customButton.addActionListener(event -> {
            Endereco endereco = showCustomComponentDialog(frame);

            if (endereco == null) {
                output.setText("Componente customizado cancelado.");
                return;
            }

            output.setText("Componente customizado retornou:\n" + endereco);
        });

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        actions.setOpaque(false);
        actions.add(formButton);
        actions.add(customButton);

        JLabel title = new JLabel("ModernComponentDialog");
        title.setForeground(TEXT_COLOR);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20F));

        JLabel subtitle = new JLabel("Exemplo de popup moderno com componente Swing e retorno tipado.");
        subtitle.setForeground(MUTED_TEXT_COLOR);

        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.CENTER);
        header.add(actions, BorderLayout.SOUTH);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setBackground(APP_BACKGROUND);
        content.setBorder(new EmptyBorder(24, 24, 24, 24));
        content.add(header, BorderLayout.NORTH);
        content.add(new JScrollPane(output), BorderLayout.CENTER);

        frame.setContentPane(content);
        frame.setVisible(true);
    }

    private static Usuario showFormDialog(Component parent) {
        return Dialogs.componentBuilder(Usuario.class)
                .parent(parent)
                .title("Novo usuario")
                .message("Preencha os dados do usuario para retornar um objeto Usuario.")
                .type(ModernDialog.Type.QUESTION)
                .showIcon(false)
                .accentColor(ACCENT_COLOR)
                .confirmText("Salvar")
                .cancelText("Cancelar")
                .form(form -> form
                        .field("nome", "Nome", textField())
                        .field("perfil", "Perfil", combo("Admin", "Operador", "Visitante"))
                        .field("ativo", "Ativo", checkBox(true)))
                .validateOnChange(true)
                .validationDelayMs(300)
                .onValidate(context -> {
                    ModernComponentDialog.FormPanel form = context.form();

                    if (form.text("nome").isBlank()) {
                        throw new IllegalArgumentException("Informe o nome.");
                    }
                })
                .result(context -> {
                    ModernComponentDialog.FormPanel form = context.form();
                    return new Usuario(
                            form.text("nome").trim(),
                            String.valueOf(form.value("perfil")),
                            Boolean.TRUE.equals(form.value("ativo"))
                    );
                })
                .show();
    }

    private static Endereco showCustomComponentDialog(Component parent) {
        JTextField rua = textField();
        JTextField numero = textField();
        JTextField cidade = textField();

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.add(label("Rua"), constraints(0, 0, 0));
        panel.add(rua, constraints(1, 0, 1));
        panel.add(label("Numero"), constraints(0, 1, 0));
        panel.add(numero, constraints(1, 1, 1));
        panel.add(label("Cidade"), constraints(0, 2, 0));
        panel.add(cidade, constraints(1, 2, 1));

        return ModernComponentDialog.builder(Endereco.class)
                .title("Endereco")
                .message("Este dialog recebe um JPanel pronto e monta o retorno por lambda.")
                .type(ModernDialog.Type.INFO)
                .showIcon(false)
                .component(panel)
                .confirmText("Usar endereco")
                .cancelText("Cancelar")
                .applyValidationOnChange(true)
                .onValidate(context -> {
                    if (rua.getText().isBlank()) {
                        throw new IllegalArgumentException("Informe a rua.");
                    }
                    if (cidade.getText().isBlank()) {
                        throw new IllegalArgumentException("Informe a cidade.");
                    }
                })
                .result(context -> new Endereco(
                        rua.getText().trim(),
                        numero.getText().trim(),
                        cidade.getText().trim()
                ))
                .parent(parent)
                .show();
    }

    private static JButton button(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private static JLabel label(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        return label;
    }

    private static JTextField textField() {
        JTextField field = new JTextField();
        field.setPreferredSize(new Dimension(220, 30));
        return field;
    }

    private static JCheckBox checkBox(boolean selected) {
        JCheckBox checkBox = new JCheckBox();
        checkBox.setOpaque(false);
        checkBox.setSelected(selected);
        return checkBox;
    }

    private static JComboBox<String> combo(String... values) {
        JComboBox<String> combo = new JComboBox<>(values);
        combo.setPreferredSize(new Dimension(220, 30));
        return combo;
    }

    private static GridBagConstraints constraints(int x, int y, double weightx) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = x;
        constraints.gridy = y;
        constraints.weightx = weightx;
        constraints.fill = x == 0 ? GridBagConstraints.NONE : GridBagConstraints.HORIZONTAL;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(0, 0, 8, x == 0 ? 10 : 0);
        return constraints;
    }

    private record Usuario(String nome, String perfil, boolean ativo) {
    }

    private record Endereco(String rua, String numero, String cidade) {
    }
}
