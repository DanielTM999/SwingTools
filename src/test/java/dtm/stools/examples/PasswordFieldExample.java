package dtm.stools.examples;

import com.formdev.flatlaf.FlatLightLaf;
import dtm.stools.component.form.*;
import dtm.stools.component.inputfields.passwordfield.PasswordField;
import javax.swing.*;
import java.awt.BorderLayout;
import java.util.Arrays;

public final class PasswordFieldExample {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatLightLaf.setup();
            PasswordField password = new PasswordField().setPlaceholder("Senha de acesso");
            FormPanel form = new FormPanel().addField(new FormField("password", "Senha", password).setRequired(true));
            JButton submit = new JButton("Validar"); JLabel status = new JLabel(" ");
            submit.addActionListener(e -> {
                if (!form.isFormValid()) { status.setText("Preencha a senha"); return; }
                char[] value = password.getPassword();
                try { status.setText("Senha preenchida; conteúdo não exibido"); }
                finally { Arrays.fill(value, '\0'); }
            });
            JPanel panel = new JPanel(new BorderLayout(8, 8)); panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            panel.add(form); panel.add(submit, BorderLayout.SOUTH); panel.add(status, BorderLayout.NORTH);
            JFrame frame = new JFrame("PasswordField"); frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel); frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true);
        });
    }
}
