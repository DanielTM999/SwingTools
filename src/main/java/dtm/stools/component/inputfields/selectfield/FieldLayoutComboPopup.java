package dtm.stools.component.inputfields.selectfield;

import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.function.UnaryOperator;

final class FieldLayoutComboPopup extends BasicComboPopup {
    FieldLayoutComboPopup(JComboBox<Object> comboBox) {
        super(comboBox);
    }

    @Override
    protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
        return computeFieldPopupBounds(comboBox, getInsets(), new Rectangle(px, py, pw, ph),
                bounds -> super.computePopupBounds(bounds.x, bounds.y, bounds.width, bounds.height));
    }

    static Rectangle computeFieldPopupBounds(JComboBox<?> comboBox, Insets insets, Rectangle requested,
                                             UnaryOperator<Rectangle> computeBounds) {
        if (!(comboBox instanceof DropdownFieldListener<?> field) || field.getFieldLayoutManager() == null) {
            return computeBounds.apply(requested);
        }
        Rectangle body = field.getFieldLayoutManager().getFieldBounds(field);
        int width = Math.max(0, body.width - insets.left - insets.right);
        Rectangle bounds = computeBounds.apply(new Rectangle(body.x, body.y + body.height, width, requested.height));
        if (bounds.y == -requested.height - insets.top - insets.bottom) bounds.y += body.y;
        return bounds;
    }
}
