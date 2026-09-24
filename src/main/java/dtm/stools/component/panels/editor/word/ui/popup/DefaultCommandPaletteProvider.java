package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.provider.*;
import dtm.stools.configs.UiTokens;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;

public final class DefaultCommandPaletteProvider implements WordCommandPaletteProvider {
    @Override public String id() { return "word.popup.palette.default"; }

    @Override public WordPopupHandle show(WordCommandPaletteContext context) {
        Window owner = context.owner() instanceof Window w ? w : SwingUtilities.getWindowAncestor(context.owner());
        WordDialogActivity<Void> dialog = new WordDialogActivity<>(context.owner(),"Comandos",Dialog.ModalityType.MODELESS);
        dialog.setName("word.palette.dialog");
        JTextField search = new JTextField();
        DefaultListModel<WordCommandEntry> model = new DefaultListModel<>();
        JList<WordCommandEntry> list = new JList<>(model);
        list.setCellRenderer((l,a,i,selected,focus) -> {
            JLabel label = new JLabel(a.name() + (a.group().isBlank() ? "" : "   ·  " + a.group()));
            label.setBorder(BorderFactory.createEmptyBorder(8,12,8,12)); label.setOpaque(true);
            label.setBackground(selected ? UiTokens.accent() : UiTokens.surface()); label.setForeground(selected ? Color.WHITE : UiTokens.foreground());
            return label;
        });
        Runnable filter = () -> {
            model.clear();
            String q = search.getText().toLowerCase(context.locale());
            for (WordCommandEntry e : context.commands()) if (e.enabled() && (e.name().toLowerCase(context.locale()).contains(q) || e.group().toLowerCase(context.locale()).contains(q))) model.addElement(e);
            if (!model.isEmpty()) list.setSelectedIndex(0);
        };
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter.run(); }
            public void removeUpdate(DocumentEvent e) { filter.run(); }
            public void changedUpdate(DocumentEvent e) { filter.run(); }
        });
        Runnable run = () -> { WordCommandEntry a = list.getSelectedValue(); dialog.dispose(); if (a != null) context.execute(a.id()); };
        search.addActionListener(e -> run.run());
        search.addKeyListener(new KeyAdapter() { @Override public void keyPressed(KeyEvent e) {
            int i = list.getSelectedIndex();
            if (e.getKeyCode() == KeyEvent.VK_DOWN && i < model.size()-1) { list.setSelectedIndex(i+1); list.ensureIndexIsVisible(i+1); e.consume(); }
            if (e.getKeyCode() == KeyEvent.VK_UP && i > 0) { list.setSelectedIndex(i-1); list.ensureIndexIsVisible(i-1); e.consume(); }
        } });
        list.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) run.run(); } });
        JPanel content = new JPanel(new BorderLayout(0, 12));
        content.add(search, BorderLayout.NORTH); content.add(new JScrollPane(list), BorderLayout.CENTER);
        content.setPreferredSize(new Dimension(440, 340));
        search.putClientProperty("JTextField.placeholderText", "Pesquisar comandos…");
        dialog.setBody(content);
        dialog.addAction("Fechar", dialog::dispose, false);
        dialog.addAction("Executar", run, true);
        dialog.onClosed(context::closed);
        filter.run(); dialog.open(); search.requestFocusInWindow();
        return new WordPopupHandle() {
            public boolean isOpen() { return dialog.isDisplayable(); }
            public void toFront() { dialog.toFront(); search.requestFocusInWindow(); }
            public void close() { dialog.dispose(); }
        };
    }
}
