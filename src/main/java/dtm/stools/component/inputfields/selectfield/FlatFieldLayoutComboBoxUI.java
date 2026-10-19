package dtm.stools.component.inputfields.selectfield;

import com.formdev.flatlaf.ui.FlatComboBoxUI;
import com.formdev.flatlaf.ui.FlatUIUtils;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

final class FlatFieldLayoutComboBoxUI extends FlatComboBoxUI {
    Rectangle getContentBounds() {
        Rectangle bounds = rectangleForCurrentValue();
        bounds.width = Math.max(0, bounds.width);
        bounds.height = Math.max(0, bounds.height);
        return bounds;
    }

    @Override
    public void update(Graphics g, JComponent component) {
        paint(g, component);
    }

    @Override
    public void paint(Graphics g, JComponent component) {
        try {
            if (!comboBox.isEditable()) paintCurrentValue(g, getContentBounds(), comboBox.hasFocus());
        } finally { currentValuePane.removeAll(); }
    }

    @Override
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        FieldLayoutComboBoxUI.paintValue(g, bounds, comboBox, listBox, currentValuePane);
    }

    @Override
    protected JButton createArrowButton() {
        return new FlatComboBoxButton() {
            @Override
            protected void paintArrow(Graphics2D g) {
                int direction = comboBox.isPopupVisible() ? SwingConstants.NORTH : SwingConstants.SOUTH;
                FlatUIUtils.paintArrow(g, 0, 0, getWidth(), getHeight(), direction, chevron,
                        getArrowWidth(), getArrowThickness(), getXOffset(), getYOffset());
            }
        };
    }

    @Override
    protected ComboPopup createPopup() {
        return new FieldPopup(comboBox);
    }

    final class FieldPopup extends FlatComboPopup {
        FieldPopup(JComboBox<Object> comboBox) {
            super(comboBox);
        }

        @Override
        protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
            return FieldLayoutComboPopup.computeFieldPopupBounds(comboBox, getInsets(), new Rectangle(px, py, pw, ph),
                    bounds -> super.computePopupBounds(bounds.x, bounds.y, bounds.width, bounds.height));
        }
    }
}
