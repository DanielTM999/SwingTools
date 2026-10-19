package dtm.stools.component.inputfields.textfield.layout;

import javax.swing.JComponent;
import java.awt.Rectangle;

public interface FieldLayoutTarget {
    JComponent getFieldComponent();
    String getLabel();
    boolean isFieldContentEmpty();
    Rectangle getFieldContentBounds();
}
