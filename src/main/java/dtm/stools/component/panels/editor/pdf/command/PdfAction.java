package dtm.stools.component.panels.editor.pdf.command;

import dtm.stools.component.panels.editor.pdf.ui.PdfRibbon;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class PdfAction extends AbstractAction {
    private final String id;
    private final Runnable body;
    private final Consumer<Throwable> errors;
    private BooleanSupplier available;
    private BooleanSupplier selected;
    private boolean edit;

    public PdfAction(String id, String name, Runnable body, Consumer<Throwable> errors) {
        super(name);
        this.id = id;
        this.body = body;
        this.errors = errors;
        putValue(ACTION_COMMAND_KEY, id);
        putValue(SHORT_DESCRIPTION, name);
    }

    public String id() { return id; }
    public boolean isEdit() { return edit; }

    public PdfAction icon(String name) { putValue(PdfRibbon.ICON, name); return this; }
    public PdfAction tip(String text) { putValue(SHORT_DESCRIPTION, text); return this; }
    public PdfAction menu(List<String> ids) { putValue(PdfRibbon.MENU, List.copyOf(ids)); return this; }
    public PdfAction iconOnly() { putValue(PdfRibbon.ICON_ONLY, Boolean.TRUE); return this; }
    public PdfAction when(BooleanSupplier value) { available = value; return this; }
    public PdfAction edits() { edit = true; return this; }
    public PdfAction selected(BooleanSupplier value) {
        selected = value;
        putValue(PdfRibbon.TOGGLE, Boolean.TRUE);
        putValue(SELECTED_KEY, value.getAsBoolean());
        return this;
    }

    public void refresh(boolean writable) {
        boolean enabled = (!edit || writable) && (available == null || available.getAsBoolean());
        if (isEnabled() != enabled) setEnabled(enabled);
        if (selected != null) {
            boolean value = selected.getAsBoolean();
            if (!Boolean.valueOf(value).equals(getValue(SELECTED_KEY))) putValue(SELECTED_KEY, value);
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        try { body.run(); }
        catch (RuntimeException error) { errors.accept(error); }
        finally { if (selected != null) putValue(SELECTED_KEY, selected.getAsBoolean()); }
    }
}
