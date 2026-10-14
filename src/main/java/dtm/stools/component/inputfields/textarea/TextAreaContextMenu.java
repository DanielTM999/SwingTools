package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnostic;
import dtm.stools.component.inputfields.textarea.diagnostic.TextDiagnosticAction;
import dtm.stools.i18n.I18n;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.Component;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

final class TextAreaContextMenu extends JPopupMenu {

    private static final int MAX_DIAGNOSTICS = 3;

    private final SmartTextArea area;
    private final JMenuItem undo;
    private final JMenuItem redo;
    private final JMenuItem cut;
    private final JMenuItem copy;
    private final JMenuItem paste;
    private final JMenuItem delete;
    private final JMenuItem selectAll;
    private final JSeparator searchSeparator = new JSeparator();
    private final JMenuItem find;
    private final JMenuItem replace;
    private final List<Component> diagnosticItems = new ArrayList<>();

    TextAreaContextMenu(SmartTextArea area) {
        this.area = area;
        int menu = SmartTextArea.menuShortcutMask();
        undo = item("undo", "Desfazer", KeyStroke.getKeyStroke(KeyEvent.VK_Z, menu), area::undo);
        redo = item("redo", "Refazer", KeyStroke.getKeyStroke(KeyEvent.VK_Y, menu), area::redo);
        cut = item("cut", "Recortar", KeyStroke.getKeyStroke(KeyEvent.VK_X, menu), area::cut);
        copy = item("copy", "Copiar", KeyStroke.getKeyStroke(KeyEvent.VK_C, menu), area::copy);
        paste = item("paste", "Colar", KeyStroke.getKeyStroke(KeyEvent.VK_V, menu), area::paste);
        delete = item("delete", "Excluir", KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), () -> area.replaceSelection(""));
        selectAll = item("selectAll", "Selecionar tudo", KeyStroke.getKeyStroke(KeyEvent.VK_A, menu), area::selectAll);
        find = item("find", "Buscar…", KeyStroke.getKeyStroke(KeyEvent.VK_F, menu), area::openSearch);
        replace = item("replace", "Substituir…", KeyStroke.getKeyStroke(KeyEvent.VK_H, menu), area::openReplace);

        add(undo);
        add(redo);
        addSeparator();
        add(cut);
        add(copy);
        add(paste);
        add(delete);
        addSeparator();
        add(selectAll);
        add(searchSeparator);
        add(find);
        add(replace);

        addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                refreshState();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });
    }

    void refreshState() {
        refreshDiagnostics();
        boolean editable = area.isEditable() && area.isEnabled();
        boolean hasSelection = area.getSelectionStart() != area.getSelectionEnd();
        undo.setEnabled(editable && area.canUndo());
        redo.setEnabled(editable && area.canRedo());
        cut.setEnabled(editable && hasSelection);
        copy.setEnabled(hasSelection);
        paste.setEnabled(editable && clipboardHasText());
        delete.setEnabled(editable && hasSelection);
        selectAll.setEnabled(area.getDocument().getLength() > 0);
        boolean search = area.isSearchAvailable();
        searchSeparator.setVisible(search);
        find.setVisible(search);
        replace.setVisible(search);
        replace.setEnabled(editable);
    }

    private void refreshDiagnostics() {
        diagnosticItems.forEach(this::remove);
        diagnosticItems.clear();
        List<TextAreaDiagnosticEntry> entries = area.diagnosticEntriesAt(area.getCaretPosition());
        int index = 0;
        boolean editable = area.isEditable() && area.isEnabled();
        for (TextAreaDiagnosticEntry entry : entries.subList(0, Math.min(entries.size(), MAX_DIAGNOSTICS))) {
            TextDiagnostic diagnostic = entry.diagnostic();
            JMenuItem header = new JMenuItem(SmartTextArea.severityLabel(diagnostic.severity()) + ": " + shorten(diagnostic.message()));
            header.setEnabled(false);
            header.setToolTipText(diagnostic.message());
            insertDiagnosticItem(header, index++);
            for (TextDiagnosticAction action : diagnostic.actions()) {
                JMenuItem item = new JMenuItem(action.getLabel());
                item.setFont(item.getFont().deriveFont(Font.BOLD));
                item.setEnabled(editable);
                item.addActionListener(e -> area.applyDiagnosticAction(entry, action));
                insertDiagnosticItem(item, index++);
            }
        }
        if (index > 0) insertDiagnosticItem(new JSeparator(), index);
    }

    private void insertDiagnosticItem(Component component, int index) {
        insert(component, index);
        diagnosticItems.add(component);
    }

    private static String shorten(String message) {
        String line = message == null ? "" : message.lines().findFirst().orElse("");
        return line.length() > 70 ? line.substring(0, 69) + "…" : line;
    }

    private JMenuItem item(String key, String fallback, KeyStroke accelerator, Runnable action) {
        JMenuItem item = new JMenuItem(I18n.getText(SmartTextArea.class, key, fallback));
        item.setAccelerator(accelerator);
        item.addActionListener(e -> action.run());
        return item;
    }

    private static boolean clipboardHasText() {
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch (RuntimeException ex) {
            return true;
        }
    }
}
