package dtm.stools.component.panels.editor.code.rename;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

public class DefaultLinkedRenamePopupFactory implements LinkedRenamePopupFactory {

    @Override
    public JComponent createContent(LinkedRenamePopupContext context) {
        Color bg = UIManager.getColor("ToolTip.background");
        if (bg == null) bg = UIManager.getColor("Panel.background");
        Color fg = UIManager.getColor("ToolTip.foreground");
        if (fg == null) fg = UIManager.getColor("Label.foreground");
        Color border = UIManager.getColor("Component.borderColor");
        if (border == null) border = accent();

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(bg);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        RenameSession session = context.session();
        if (session != null) {
            JLabel title = new JLabel(session.displayTitle());
            title.setForeground(fg);
            title.setFont(title.getFont().deriveFont(Font.BOLD));
            title.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(title);
            for (RenameOption option : session.options()) {
                JCheckBox checkBox = new JCheckBox(option.label(), context.optionValue(option.id(), option.defaultValue()));
                checkBox.setOpaque(false);
                checkBox.setFocusable(false);
                checkBox.setForeground(fg);
                checkBox.setAlignmentX(Component.LEFT_ALIGNMENT);
                checkBox.addActionListener(ev -> {
                    context.setOption(option.id(), checkBox.isSelected());
                    context.refocus();
                });
                panel.add(checkBox);
            }
        }
        if (context.hint() != null && !context.hint().isBlank()) {
            JLabel hint = new JLabel(context.hint());
            hint.setForeground(fg);
            hint.setFont(hint.getFont().deriveFont(hint.getFont().getSize2D() - 1f));
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(hint);
        }
        return panel;
    }

    protected Color accent() {
        Color color = UIManager.getColor("Component.focusColor");
        if (color == null) color = UIManager.getColor("Component.accentColor");
        return color == null ? new Color(0x3B82F6) : color;
    }
}
