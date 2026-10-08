package dtm.stools.component.panels.editor.pdf.ui.popup;

import dtm.stools.component.panels.editor.pdf.provider.PdfDialogProvider;
import dtm.stools.component.popup.ModernDialog;
import dtm.stools.component.popup.ModernInputDialog;
import dtm.stools.configs.UiTokens;

import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.util.List;
import java.awt.Component;
import java.util.Arrays;
import java.util.Optional;

public final class DefaultPdfDialogProvider implements PdfDialogProvider {
    @Override public String id() { return "pdf.dialog.default"; }

    @Override public Optional<String> input(Component parent, String title, String prompt) {
        return input(parent, title, prompt, "");
    }

    @Override public Optional<String> input(Component parent, String title, String prompt, String initial) {
        JTextField field = new JTextField(initial == null ? "" : initial, 24);
        field.selectAll();
        String value = ModernInputDialog.builder()
                .title(title)
                .message(prompt)
                .input(field)
                .valueSupplier(field::getText)
                .validationSource(field)
                .enterConfirms(true)
                .closeOnEsc(true)
                .show(parent);
        return Optional.ofNullable(value);
    }

    @Override public Optional<List<String>> editList(Component parent, String title, String prompt, List<String> values) {
        JTextArea area = new JTextArea(String.join("\n", values), 8, 26);
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(300, 170));
        String value = ModernInputDialog.builder()
                .title(title)
                .message(prompt)
                .input(scroll)
                .valueSupplier(area::getText)
                .enterConfirms(false)
                .closeOnEsc(true)
                .show(parent);
        if (value == null) return Optional.empty();
        return Optional.of(Arrays.stream(value.split("\\R")).map(String::strip).filter(item -> !item.isEmpty()).toList());
    }

    @Override public Optional<char[]> password(Component parent, String title, String prompt) {
        JPasswordField field = new JPasswordField(24);
        String value = ModernInputDialog.builder()
                .title(title)
                .message(prompt)
                .input(field)
                .valueSupplier(() -> field.getPassword().length == 0 ? "" : "*")
                .enterConfirms(true)
                .closeOnEsc(true)
                .show(parent);
        char[] secret = field.getPassword();
        field.setText("");
        if (value == null) {
            Arrays.fill(secret, '\0');
            return Optional.empty();
        }
        return Optional.of(secret);
    }

    @Override public boolean confirm(Component parent, String title, String message) {
        return ModernDialog.builder()
                .title(title)
                .message(message)
                .type(ModernDialog.Type.QUESTION)
                .option("Sim", JOptionPane.YES_OPTION)
                .option("Não", JOptionPane.NO_OPTION)
                .closeOnEsc(true)
                .show(parent) == JOptionPane.YES_OPTION;
    }

    @Override public void message(Component parent, String title, String message) {
        ModernDialog.builder()
                .title(title)
                .message(message)
                .type(ModernDialog.Type.INFO)
                .option("OK", JOptionPane.OK_OPTION)
                .enterConfirms(true)
                .closeOnEsc(true)
                .show(parent);
    }

    @Override public int choose(Component parent, String title, String message, String... options) {
        if (options.length == 0) return -1;
        ModernDialog.ModernDialogBuilder builder = ModernDialog.builder()
                .title(title)
                .message(message)
                .type(ModernDialog.Type.QUESTION)
                .accentColor(UiTokens.warning())
                .closeOnEsc(true);
        for (int index = 0; index < options.length; index++) builder.option(options[index], index);
        int choice = builder.show(parent);
        return choice >= 0 && choice < options.length ? choice : -1;
    }
}
