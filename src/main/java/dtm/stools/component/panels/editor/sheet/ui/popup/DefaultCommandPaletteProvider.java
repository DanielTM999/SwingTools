package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.provider.SheetCommandEntry;
import dtm.stools.component.panels.editor.sheet.provider.SheetCommandPaletteContext;
import dtm.stools.component.panels.editor.sheet.provider.SheetCommandPaletteProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetPopupHandle;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Normalizer;
import java.util.Locale;

public final class DefaultCommandPaletteProvider implements SheetCommandPaletteProvider {
    @Override public String id() { return "sheet.popup.palette.default"; }

    @Override
    public SheetPopupHandle open(SheetCommandPaletteContext context) {
        SheetDialogActivity<Void> d = new SheetDialogActivity<>(context.owner(), "Comandos", Dialog.ModalityType.MODELESS);
        JTextField search = new JTextField(36);
        DefaultListModel<SheetCommandEntry> model = new DefaultListModel<>();
        JList<SheetCommandEntry> list = new JList<>(model);
        list.setCellRenderer((l, v, i, sel, focus) -> {
            JLabel label = new JLabel("<html><b>" + v.name() + "</b> <span style='color:gray'>" + v.group() + (v.shortcut().isEmpty() ? "" : " · " + v.shortcut()) + "</span></html>");
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(3, 8, 3, 8));
            label.setEnabled(v.enabled());
            if (sel) { label.setBackground(l.getSelectionBackground()); label.setForeground(l.getSelectionForeground()); }
            return label;
        });
        Runnable filter = () -> {
            model.clear();
            String q = norm(search.getText());
            for (SheetCommandEntry e : context.commands()) if (q.isEmpty() || norm(e.name()).contains(q) || norm(e.group()).contains(q) || e.id().toLowerCase(Locale.ROOT).contains(q)) model.addElement(e);
            if (!model.isEmpty()) list.setSelectedIndex(0);
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter.run(); }
            @Override public void removeUpdate(DocumentEvent e) { filter.run(); }
            @Override public void changedUpdate(DocumentEvent e) { filter.run(); }
        });
        Runnable run = () -> {
            SheetCommandEntry e = list.getSelectedValue();
            if (e == null || !e.enabled()) return;
            d.dispose();
            context.execute(e.id());
        };
        search.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) { list.setSelectedIndex(Math.min(model.size() - 1, list.getSelectedIndex() + 1)); list.ensureIndexIsVisible(list.getSelectedIndex()); e.consume(); }
                if (e.getKeyCode() == KeyEvent.VK_UP) { list.setSelectedIndex(Math.max(0, list.getSelectedIndex() - 1)); list.ensureIndexIsVisible(list.getSelectedIndex()); e.consume(); }
                if (e.getKeyCode() == KeyEvent.VK_ENTER) { run.run(); e.consume(); }
            }
        });
        list.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) run.run(); } });
        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.add(search, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(520, 320));
        content.add(scroll, BorderLayout.CENTER);
        d.setBody(content);
        d.addAction("Executar", run, true);
        filter.run();
        d.open();
        search.requestFocusInWindow();
        return new SheetPopupHandle() {
            @Override public boolean isOpen() { return d.isDisplayable(); }
            @Override public void toFront() { d.toFront(); }
            @Override public void close() { d.dispose(); }
        };
    }

    private static String norm(String s) {
        return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    static KeyStroke none() { return null; }
}
