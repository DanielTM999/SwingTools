package dtm.stools.component.panels.editor.sheet.ui.popup;

import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

public final class ValidationListPopup {
    private ValidationListPopup() {}

    public static JPopupMenu show(Component invoker, Rectangle cell, List<String> items, String current, Consumer<String> pick) {
        JPopupMenu popup = new JPopupMenu();
        popup.setName("sheet.validationList");
        JList<String> list = new JList<>(items.toArray(String[]::new));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(Math.min(8, Math.max(1, items.size())));
        int idx = items.indexOf(current);
        if (idx >= 0) { list.setSelectedIndex(idx); list.ensureIndexIsVisible(idx); }
        Runnable accept = () -> { String v = list.getSelectedValue(); popup.setVisible(false); if (v != null) pick.accept(v); };
        list.addMouseListener(new MouseAdapter() { @Override public void mouseReleased(MouseEvent e) { accept.run(); } });
        list.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) { e.consume(); accept.run(); }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) { e.consume(); popup.setVisible(false); }
            }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(Math.max(cell.width + 20, 140), Math.min(200, 22 * Math.max(1, items.size()) + 6)));
        popup.add(scroll);
        popup.show(invoker, cell.x, cell.y + cell.height);
        list.requestFocusInWindow();
        return popup;
    }
}
