package dtm.stools.component.panels.editor.sheet.ui.popup;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

public class SheetForm extends JPanel {
    private int row;

    public SheetForm() {
        super(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    }

    private GridBagConstraints base(int x, int width) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = row;
        c.gridwidth = width;
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        return c;
    }

    public <T extends JComponent> T add(String label, T component) {
        GridBagConstraints l = base(0, 1);
        l.weightx = 0;
        add(new JLabel(label), l);
        GridBagConstraints c = base(1, 1);
        c.weightx = 1;
        add(component, c);
        row++;
        return component;
    }

    public <T extends JComponent> T full(T component) {
        GridBagConstraints c = base(0, 2);
        c.weightx = 1;
        add(component, c);
        row++;
        return component;
    }

    public <T extends JComponent> T grow(T component) {
        GridBagConstraints c = base(0, 2);
        c.weightx = 1;
        c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        add(component, c);
        row++;
        return component;
    }

    public JLabel section(String title) {
        JLabel l = new JLabel(title);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        l.setBorder(BorderFactory.createEmptyBorder(row == 0 ? 0 : 8, 0, 2, 0));
        return full(l);
    }

    public static JTextField text(String value, int columns) { return new JTextField(value == null ? "" : value, columns); }

    public static JSpinner number(double value, double min, double max, double step) {
        return new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(max, value)), min, max, step));
    }

    public static JSpinner integer(int value, int min, int max) {
        return new JSpinner(new SpinnerNumberModel(Math.max(min, Math.min(max, value)), min, max, 1));
    }

    @SafeVarargs
    public static <T> JComboBox<T> combo(T... items) { return new JComboBox<>(items); }

    public static JCheckBox check(String label, boolean value) { return new JCheckBox(label, value); }

    public static double number(JSpinner s) { return ((Number) s.getValue()).doubleValue(); }
    public static int integer(JSpinner s) { return ((Number) s.getValue()).intValue(); }
}
