package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.api.SheetSelection;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.provider.SheetCommandEntry;
import dtm.stools.component.panels.editor.sheet.provider.SheetCommandPaletteContext;
import dtm.stools.component.panels.editor.sheet.provider.SheetCommandPaletteProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetConfirmationProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetConfirmationRequest;
import dtm.stools.component.panels.editor.sheet.provider.SheetContextMenuContext;
import dtm.stools.component.panels.editor.sheet.provider.SheetContextMenuProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetDialogProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetDialogRequest;
import dtm.stools.component.panels.editor.sheet.provider.SheetFileDialogProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetFileDialogRequest;
import dtm.stools.component.panels.editor.sheet.provider.SheetPopupHandle;
import dtm.stools.component.panels.editor.sheet.provider.SheetProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetSearchPopupProvider;
import dtm.stools.component.panels.editor.sheet.ui.SheetGeometry;
import dtm.stools.component.panels.editor.sheet.ui.SheetIcon;
import dtm.stools.component.panels.editor.sheet.ui.SheetRibbon;
import dtm.stools.component.panels.editor.sheet.ui.popup.DefaultCommandPaletteProvider;
import dtm.stools.component.panels.editor.sheet.ui.popup.DefaultConfirmationProvider;
import dtm.stools.component.panels.editor.sheet.ui.popup.DefaultDialogProvider;
import dtm.stools.component.panels.editor.sheet.ui.popup.DefaultFileDialogProvider;
import dtm.stools.component.panels.editor.sheet.ui.popup.DefaultSearchPopupProvider;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SheetPopups {
    private final SheetEditor editor;
    private final SheetDialogProvider defaultDialogs = new DefaultDialogProvider();
    private final SheetFileDialogProvider defaultFiles = new DefaultFileDialogProvider();
    private final SheetConfirmationProvider defaultConfirmations = new DefaultConfirmationProvider();
    private final SheetSearchPopupProvider defaultSearch = new DefaultSearchPopupProvider();
    private final SheetCommandPaletteProvider defaultPalette = new DefaultCommandPaletteProvider();
    private SheetPopupHandle search = SheetPopupHandle.closed(), palette = SheetPopupHandle.closed();
    private boolean showingError;

    public SheetPopups(SheetEditor editor) { this.editor = editor; }

    private <T> T first(Class<T> type, T fallback) {
        List<T> list = editor.providers(type);
        return list.isEmpty() ? fallback : list.getFirst();
    }

    public SheetDialogProvider dialogs() { return first(SheetDialogProvider.class, defaultDialogs); }
    public SheetFileDialogProvider files() { return first(SheetFileDialogProvider.class, defaultFiles); }
    public SheetConfirmationProvider confirmations() { return first(SheetConfirmationProvider.class, defaultConfirmations); }
    public SheetSearchPopupProvider searchProvider() { return first(SheetSearchPopupProvider.class, defaultSearch); }
    public SheetCommandPaletteProvider paletteProvider() { return first(SheetCommandPaletteProvider.class, defaultPalette); }

    public void resetProviders() {
        for (SheetProvider p : editor.getProviders())
            if (p instanceof SheetDialogProvider || p instanceof SheetFileDialogProvider || p instanceof SheetConfirmationProvider || p instanceof SheetSearchPopupProvider || p instanceof SheetCommandPaletteProvider)
                editor.removeProvider(p.id());
    }

    private boolean interactive() { return editor.isShowing(); }

    public <T> Optional<T> dialog(String id, String title, JComponent content, Supplier<T> result) {
        return dialog(SheetDialogRequest.of(editor, id, title, content, result));
    }

    public <T> Optional<T> dialog(String id, String title, JComponent content, Supplier<T> result, Consumer<T> validate) {
        return dialog(SheetDialogRequest.of(editor, id, title, content, result).withValidation(validate));
    }

    public <T> Optional<T> dialog(SheetDialogRequest<T> request) {
        if (!interactive()) return Optional.empty();
        return dialogs().show(request);
    }

    public void info(String title, String message) {
        if (!interactive()) return;
        confirmations().confirm(SheetConfirmationRequest.info(editor, title, message));
    }

    public void warn(String title, String message) {
        if (!interactive()) return;
        confirmations().confirm(SheetConfirmationRequest.warn(editor, title, message));
    }

    public boolean confirm(String title, String message) {
        if (!interactive()) return false;
        return confirmations().confirm(SheetConfirmationRequest.yesNo(editor, title, message)) == 0;
    }

    public int choose(String title, String message, boolean warning, int defaultOption, String... options) {
        if (!interactive()) return defaultOption;
        return confirmations().confirm(new SheetConfirmationRequest(editor, title, message, List.of(options), defaultOption, warning));
    }

    public Optional<String> prompt(String title, String label, String initial) {
        JTextField field = new JTextField(initial == null ? "" : initial, 28);
        JPanel p = new JPanel(new BorderLayout(0, 6));
        p.add(new JLabel(label), BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        field.selectAll();
        return dialog("sheet.prompt", title, p, () -> field.getText().strip());
    }

    public void error(Throwable failure) {
        if (!interactive() || showingError) return;
        showingError = true;
        try {
            String msg = failure.getMessage() == null || failure.getMessage().isBlank() ? failure.getClass().getSimpleName() : failure.getMessage();
            warn("Planilha", msg);
        } finally {
            showingError = false;
        }
    }

    public Optional<Path> chooseFile(SheetFileDialogRequest.Mode mode, String title, List<SheetFileDialogRequest.Filter> filters, Path directory, String suggested) {
        if (!interactive()) return Optional.empty();
        return files().choose(new SheetFileDialogRequest(editor, mode, title, filters, directory, suggested));
    }

    public void openSearch(boolean replace) {
        if (search.isOpen()) { search.toFront(); return; }
        search = searchProvider().open(new SheetSearchSupport(editor), replace);
    }

    public void openPalette() {
        if (palette.isOpen()) { palette.toFront(); return; }
        palette = paletteProvider().open(new SheetCommandPaletteContext() {
            @Override public Component owner() { return editor; }
            @Override public Locale locale() { return editor.getConfig().locale(); }
            @Override public List<SheetCommandEntry> commands() { return editor.commandRegistry().entries(); }
            @Override public boolean execute(String id) { return editor.execute(id); }
        });
    }

    public void showCommandMenu(List<?> ids) {
        JPopupMenu menu = new JPopupMenu();
        fill(menu, ids);
        Rectangle r = activeCellRect();
        menu.show(editor.getCanvas(), r.x, r.y + r.height);
    }

    public Rectangle activeCellRect() {
        SheetGeometry g = editor.getCanvas().geometry();
        SheetSelection sel = editor.getSession().getSelection();
        CellAddress a = sel.active();
        Rectangle r = g.cellRect(a.row(), a.column());
        if (r.x < g.headerWidth() || r.x > editor.getCanvas().getWidth()) r.x = g.headerWidth() + 10;
        if (r.y < g.headerHeight() || r.y > editor.getCanvas().getHeight()) r.y = g.headerHeight() + 10;
        return r;
    }

    public void fill(JComponent menu, List<?> ids) {
        for (Object o : ids) {
            String id = String.valueOf(o);
            if (id.equals("-")) {
                if (menu instanceof JPopupMenu p) p.addSeparator(); else if (menu instanceof JMenu m) m.addSeparator();
                continue;
            }
            Action a = editor.getCommands().get(id);
            if (a == null) continue;
            JMenuItem item;
            if (a.getValue(SheetRibbon.MENU) instanceof List<?> nested) {
                JMenu m = new JMenu(String.valueOf(a.getValue(Action.NAME)));
                m.setIcon(SheetIcon.small(SheetRibbon.iconOf(a)));
                m.setEnabled(a.isEnabled());
                fill(m, nested);
                item = m;
            } else {
                item = new JMenuItem(a);
                item.setIcon(SheetIcon.small(SheetRibbon.iconOf(a)));
                item.setToolTipText(null);
            }
            item.setName("sheet.menu." + id);
            menu.add(item);
        }
    }

    public void showContextMenu(SheetContextMenuContext context, Component invoker, int x, int y) {
        JPopupMenu menu = new JPopupMenu();
        List<String> ids = switch (context.target()) {
            case CELL -> List.of("sheet.cut", "sheet.copy", "sheet.paste", "sheet.paste.menu", "-", "sheet.insert.cellsDialog", "sheet.delete.cellsDialog", "sheet.clear.contents", "-",
                    "sheet.context.filter.menu", "sheet.context.sort.menu", "-", "sheet.table.format", "sheet.data.validation.list.pick", "-", "sheet.comment.new", "sheet.note.new", "-",
                    "sheet.format.cells", "sheet.names.define", "sheet.link.insert");
            case ROW_HEADER -> List.of("sheet.cut", "sheet.copy", "sheet.paste", "sheet.paste.menu", "-", "sheet.insert.rows", "sheet.delete.rows", "sheet.clear.contents", "-",
                    "sheet.format.cells", "sheet.row.height", "sheet.row.hide", "sheet.row.unhide");
            case COLUMN_HEADER -> List.of("sheet.cut", "sheet.copy", "sheet.paste", "sheet.paste.menu", "-", "sheet.insert.columns", "sheet.delete.columns", "sheet.clear.contents", "-",
                    "sheet.format.cells", "sheet.column.width", "sheet.column.hide", "sheet.column.unhide");
            case SHEET_TAB -> List.of("sheet.sheet.insert", "sheet.sheet.delete", "sheet.sheet.rename", "sheet.sheet.duplicate", "sheet.sheet.tabColor.menu", "-",
                    "sheet.protect.sheet", "-", "sheet.sheet.hide", "sheet.sheet.unhide");
            case OBJECT -> List.of("sheet.cut", "sheet.copy", "sheet.object.delete", "-", "sheet.object.bringFront", "sheet.object.sendBack", "-", "sheet.chart.editData", "sheet.object.altText");
        };
        fill(menu, ids);
        for (SheetContextMenuProvider p : editor.providers(SheetContextMenuProvider.class)) {
            try { p.contribute(editor, context, menu); } catch (RuntimeException failure) { editor.reportError(failure); }
        }
        if (menu.getComponentCount() > 0) menu.show(invoker, x, y);
    }

    public void dispose() {
        search.close();
        palette.close();
    }
}
