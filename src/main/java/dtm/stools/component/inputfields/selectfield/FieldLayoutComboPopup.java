package dtm.stools.component.inputfields.selectfield;

import javax.swing.JComboBox;
import javax.swing.plaf.basic.BasicComboPopup;
import java.awt.Insets;
import java.awt.Rectangle;

final class FieldLayoutComboPopup extends BasicComboPopup {
    FieldLayoutComboPopup(JComboBox<Object> comboBox) {
        super(comboBox);
    }

    @Override
    protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
        if (!(comboBox instanceof DropdownFieldListener<?> field) || field.getFieldLayoutManager() == null) {
            return super.computePopupBounds(px, py, pw, ph);
        }
        Rectangle body = field.getFieldLayoutManager().getFieldBounds(field);
        Insets insets = getInsets();
        int width = Math.max(0, body.width - insets.left - insets.right);
        Rectangle bounds = super.computePopupBounds(body.x, body.y + body.height, width, ph);
        if (bounds.y == -ph - insets.top - insets.bottom) bounds.y += body.y;
        return bounds;
    }
}
