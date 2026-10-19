package dtm.stools.component.inputfields.selectfield;

import com.formdev.flatlaf.ui.FlatComboBoxUI;
import dtm.stools.configs.UiTokens;
import dtm.stools.utils.PaintUtils;

import javax.swing.JButton;
import javax.swing.CellRendererPane;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Path2D;

final class FieldLayoutComboBoxUI extends BasicComboBoxUI {
    static ComboBoxUI create(ComboBoxUI conventionalUI) {
        return conventionalUI instanceof FlatComboBoxUI ? new FlatFieldLayoutComboBoxUI() : new FieldLayoutComboBoxUI();
    }

    static boolean isInstalled(ComboBoxUI ui) {
        return ui instanceof FieldLayoutComboBoxUI || ui instanceof FlatFieldLayoutComboBoxUI;
    }

    @Override
    protected ComboPopup createPopup() {
        return new FieldLayoutComboPopup(comboBox);
    }

    Rectangle getContentBounds() {
        Rectangle bounds = rectangleForCurrentValue();
        bounds.width = Math.max(0, bounds.width);
        bounds.height = Math.max(0, bounds.height);
        return bounds;
    }

    @Override
    public void paint(Graphics g, JComponent component) {
        try {
            if (!comboBox.isEditable()) paintCurrentValue(g, getContentBounds(), comboBox.hasFocus());
        } finally { currentValuePane.removeAll(); }
    }

    @Override
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        paintValue(g, bounds, comboBox, listBox, currentValuePane);
    }

    static void paintValue(Graphics g, Rectangle bounds, JComboBox<Object> comboBox,
                           JList<Object> listBox, CellRendererPane currentValuePane) {
        if (comboBox instanceof DropdownFieldListener<?> field
                && field.isFieldContentEmpty() && !field.isFieldPlaceholderVisible()) return;
        ListCellRenderer<Object> renderer = comboBox.getRenderer();
        Component rendered = renderer.getListCellRendererComponent(listBox, comboBox.getSelectedItem(), -1, false, false);
        rendered.setFont(comboBox.getFont());
        rendered.setEnabled(comboBox.isEnabled());
        currentValuePane.add(rendered);
        Color foreground = rendered.getForeground();
        if (!comboBox.isEnabled()) rendered.setForeground(UiTokens.disabled(comboBox.getForeground()));
        boolean opaque = rendered instanceof JComponent component && component.isOpaque();
        if (rendered instanceof JComponent component) component.setOpaque(false);
        Graphics content = g.create();
        try {
            content.clipRect(bounds.x, bounds.y, bounds.width, bounds.height);
            currentValuePane.paintComponent(content, rendered, comboBox,
                    bounds.x, bounds.y, bounds.width, bounds.height, true);
        } finally {
            content.dispose();
            rendered.setForeground(foreground);
            if (rendered instanceof JComponent component) component.setOpaque(opaque);
        }
    }

    @Override
    protected JButton createArrowButton() {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics graphics) {
                Graphics2D g = PaintUtils.antialias((Graphics2D) graphics.create());
                try {
                    Color color = comboBox.getForeground();
                    g.setColor(isEnabled() ? color : UiTokens.disabled(color));
                    float half = UiTokens.scale(4);
                    float x = getWidth() / 2f;
                    float y = getHeight() / 2f;
                    float direction = comboBox.isPopupVisible() ? -1f : 1f;
                    Path2D.Float arrow = new Path2D.Float();
                    arrow.moveTo(x - half, y - direction * half / 2);
                    arrow.lineTo(x, y + direction * half / 2);
                    arrow.lineTo(x + half, y - direction * half / 2);
                    g.draw(arrow);
                } finally { g.dispose(); }
            }
        };
        button.setName("ComboBox.arrowButton");
        button.setBorder(null);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setFocusable(false);
        return button;
    }
}
