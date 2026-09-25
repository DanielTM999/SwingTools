package dtm.stools.component.panels.editor.sheet.controller;

import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.BooleanSupplier;

public class SheetAction extends AbstractAction {
    private final SheetCommandRegistry registry;
    private final String id;
    private final boolean edit;
    private final Runnable body;
    private BooleanSupplier selected;
    private BooleanSupplier available;
    private boolean whileEditing;

    SheetAction(SheetCommandRegistry registry, String id, String name, boolean edit, Runnable body) {
        super(name);
        this.registry = registry;
        this.id = id;
        this.edit = edit;
        this.body = body;
        putValue(ACTION_COMMAND_KEY, id);
        putValue(SHORT_DESCRIPTION, name);
    }

    public String id() { return id; }
    public boolean isEdit() { return edit; }
    public boolean runsWhileEditing() { return whileEditing; }
    Runnable body() { return body; }
    BooleanSupplier selectedState() { return selected; }
    BooleanSupplier availability() { return available; }

    public SheetAction selected(BooleanSupplier value) { selected = value; putValue(dtm.stools.component.panels.editor.sheet.ui.SheetRibbon.TOGGLE, Boolean.TRUE); return this; }
    public SheetAction when(BooleanSupplier value) { available = value; return this; }
    public SheetAction whileEditing() { whileEditing = true; return this; }
    public SheetAction iconOnly() { putValue("sheet.iconOnly", Boolean.TRUE); return this; }
    public SheetAction tip(String text) { putValue(SHORT_DESCRIPTION, text); return this; }
    public SheetAction menu(List<String> ids) { putValue(dtm.stools.component.panels.editor.sheet.ui.SheetRibbon.MENU, List.copyOf(ids)); return this; }

    @Override
    public void actionPerformed(ActionEvent e) { registry.perform(this); }
}
