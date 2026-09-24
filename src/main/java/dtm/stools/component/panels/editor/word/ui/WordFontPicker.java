package dtm.stools.component.panels.editor.word.ui;

import javax.swing.*;
import com.formdev.flatlaf.util.UIScale;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public final class WordFontPicker extends JPanel {
    private final JButton display = new JButton();
    private final JPopupMenu popup = new JPopupMenu();
    private final JTextField filter = new JTextField();
    private final DefaultListModel<String> model = new DefaultListModel<>();
    private final JList<String> list = new JList<>(model);
    private final Map<String,Font> previews = new HashMap<>();
    private List<String> families = List.of();
    private String current = "";
    private Consumer<String> chooser = f -> {};

    public WordFontPicker() {
        super(new BorderLayout());
        setOpaque(false);
        display.setHorizontalAlignment(SwingConstants.LEFT);
        display.setFocusable(true);
        display.setPreferredSize(UIScale.scale(new Dimension(170,28)));
        display.setToolTipText("Fonte");
        display.getAccessibleContext().setAccessibleName("Fonte");
        add(display,BorderLayout.CENTER);
        list.setCellRenderer((l,value,index,selected,focus) -> {
            JLabel label = new JLabel(value);
            label.setFont(previews.computeIfAbsent(value,f -> new Font(f,Font.PLAIN,13)));
            label.setOpaque(true); label.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));
            label.setBackground(selected ? l.getSelectionBackground() : l.getBackground()); label.setForeground(selected ? l.getSelectionForeground() : l.getForeground());
            return label;
        });
        list.setVisibleRowCount(14);
        filter.putClientProperty("JTextField.placeholderText","Filtrar fontes");
        filter.getAccessibleContext().setAccessibleName("Filtrar fontes");
        filter.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refilter(); }
            public void removeUpdate(DocumentEvent e) { refilter(); }
            public void changedUpdate(DocumentEvent e) { refilter(); }
        });
        filter.addKeyListener(new KeyAdapter() { @Override public void keyPressed(KeyEvent e) {
            int i = list.getSelectedIndex();
            if (e.getKeyCode() == KeyEvent.VK_DOWN && i < model.size()-1) { list.setSelectedIndex(i+1); list.ensureIndexIsVisible(i+1); }
            else if (e.getKeyCode() == KeyEvent.VK_UP && i > 0) { list.setSelectedIndex(i-1); list.ensureIndexIsVisible(i-1); }
            else if (e.getKeyCode() == KeyEvent.VK_ENTER) choose(list.getSelectedValue());
            else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) popup.setVisible(false);
        } });
        list.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { choose(list.getSelectedValue()); } });
        JPanel content = new JPanel(new BorderLayout(0,4)); content.setBorder(BorderFactory.createEmptyBorder(4,4,4,4));
        content.add(filter,BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list); scroll.setPreferredSize(UIScale.scale(new Dimension(260,300)));
        content.add(scroll,BorderLayout.CENTER);
        popup.add(content);
        display.addActionListener(e -> open());
        refreshDisplay();
    }
    public void setChooser(Consumer<String> value) { chooser = Objects.requireNonNull(value); }
    public List<String> getFamilies() { return families; }
    public void setFamilies(List<String> value) { families = List.copyOf(value); refilter(); refreshDisplay(); }
    public String getCurrentFamily() { return current; }
    public void setCurrentFamily(String family) { current = family == null ? "" : family; refreshDisplay(); }
    public boolean isCurrentAllowed() { return families.stream().anyMatch(f -> f.equalsIgnoreCase(current)); }
    public List<String> visibleFamilies() { List<String> r = new ArrayList<>(); for (int i = 0; i < model.size(); i++) r.add(model.get(i)); return r; }
    public void setFilter(String text) { filter.setText(text); }
    private void refreshDisplay() {
        boolean allowed = current.isEmpty() || isCurrentAllowed();
        display.setText(current.isEmpty() ? " " : allowed ? current : current + " (fora da lista)");
        display.setFont(UIManager.getFont("Button.font") == null ? display.getFont() : UIManager.getFont("Button.font").deriveFont(allowed ? Font.PLAIN : Font.ITALIC));
        display.setToolTipText(allowed ? "Fonte: " + current : "A fonte \"" + current + "\" é usada no documento, mas não está na lista permitida");
    }
    private void refilter() {
        String q = filter.getText().strip().toLowerCase(Locale.ROOT);
        model.clear();
        for (String f : families) if (q.isEmpty() || f.toLowerCase(Locale.ROOT).contains(q)) model.addElement(f);
        int index = families.indexOf(current);
        if (index >= 0 && model.contains(current)) list.setSelectedValue(current,true); else if (!model.isEmpty()) list.setSelectedIndex(0);
    }
    private void open() {
        if (!isEnabled()) return;
        filter.setText(""); refilter();
        popup.show(display,0,display.getHeight());
        SwingUtilities.invokeLater(filter::requestFocusInWindow);
    }
    private void choose(String family) {
        popup.setVisible(false);
        if (family != null) chooser.accept(family);
    }
    @Override public void setEnabled(boolean enabled) { super.setEnabled(enabled); display.setEnabled(enabled); }
}
