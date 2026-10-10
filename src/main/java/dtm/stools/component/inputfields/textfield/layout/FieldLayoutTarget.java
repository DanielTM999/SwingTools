package dtm.stools.component.inputfields.textfield.layout;

import javax.swing.JTextField;
import java.awt.Rectangle;

public interface FieldLayoutTarget {
    JTextField getFieldComponent();
    String getLabel();
    boolean isFieldContentEmpty();
    Rectangle getFieldContentBounds();
}
