package dtm.stools.component.inputfields.textarea;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.function.BooleanSupplier;

final class TextAreaAction extends AbstractAction {

    private final Runnable action;
    private final BooleanSupplier enabled;

    TextAreaAction(Runnable action, BooleanSupplier enabled) {
        this.action = action;
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled.getAsBoolean();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (isEnabled()) action.run();
    }
}
