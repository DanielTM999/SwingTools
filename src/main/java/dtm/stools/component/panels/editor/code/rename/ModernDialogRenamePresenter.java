package dtm.stools.component.panels.editor.code.rename;

import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import dtm.stools.component.popup.ModernInputDialog;
import dtm.stools.i18n.I18n;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ModernDialogRenamePresenter implements RenamePresenter {

    private final Consumer<ModernInputDialog.ModernInputDialogBuilder> customizer;

    public ModernDialogRenamePresenter() {
        this(null);
    }

    public ModernDialogRenamePresenter(Consumer<ModernInputDialog.ModernInputDialogBuilder> customizer) {
        this.customizer = customizer;
    }

    @Override
    public void present(RenameSession session) {
        if (session == null || session.isFinished()) return;
        String current = session.currentName();

        JTextField field = new JTextField(current);
        field.selectAll();

        Map<String, JCheckBox> checkBoxes = new LinkedHashMap<>();
        ModernInputDialog.ModernInputDialogBuilder builder = ModernInputDialog.builder()
                .parent(session.owner())
                .title(session.displayTitle())
                .message(current.isEmpty()
                        ? text("rename.prompt.empty", "Rename to:")
                        : text("rename.prompt.current", "Rename '{current}' to:").replace("{current}", current))
                .confirmText(text("rename.confirm", "Rename"))
                .showIcon(false)
                .disableConfirmWhenInvalid(true)
                .validationDelayMs(250)
                .onValidate(ctx -> {
                    String error = session.validate(ctx.value() == null ? "" : ctx.value().trim());
                    if (error != null) throw new IllegalArgumentException(error);
                });

        if (session.options().isEmpty()) {
            builder.input(field);
        } else {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            field.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(field);
            for (RenameOption option : session.options()) {
                JCheckBox checkBox = new JCheckBox(option.label(), option.defaultValue());
                checkBox.setOpaque(false);
                checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
                checkBox.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
                checkBoxes.put(option.id(), checkBox);
                panel.add(checkBox);
            }
            builder.input(panel)
                    .valueSupplier(field::getText)
                    .validationSource(field);
        }

        if (customizer != null) customizer.accept(builder);

        SwingUtilities.invokeLater(field::selectAll);
        String result = builder.show();
        if (result == null) {
            session.cancel();
            return;
        }
        Map<String, Boolean> values = new LinkedHashMap<>();
        for (Map.Entry<String, JCheckBox> entry : checkBoxes.entrySet()) {
            values.put(entry.getKey(), entry.getValue().isSelected());
        }
        if (!session.commit(result.trim(), values)) session.cancel();
    }

    protected String text(String key, String defaultValue) {
        return I18n.getText(CodeEditorTextArea.class, key, defaultValue);
    }
}
