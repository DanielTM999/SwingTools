package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.model.FilterCriteria;
import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class FilterMenuPopup {
    private FilterMenuPopup() {}

    public static JPopupMenu show(Component invoker, Rectangle anchor, FilterMenuRequest request) {
        JPopupMenu popup = new JPopupMenu();
        popup.setName("sheet.filterMenu");
        JPanel root = new JPanel(new BorderLayout(0, 4));
        root.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(link("Classificar de A a Z", () -> { popup.setVisible(false); request.sortAscending().run(); }));
        top.add(link("Classificar de Z a A", () -> { popup.setVisible(false); request.sortDescending().run(); }));
        top.add(Box.createVerticalStrut(4));
        JButton clear = link("Limpar Filtro de \"" + request.column() + "\"", () -> { popup.setVisible(false); request.clear().run(); });
        clear.setEnabled(request.current() != null);
        top.add(clear);
        top.add(link(request.numeric() ? "Filtros de Número…" : request.dates() ? "Filtros de Data…" : "Filtros de Texto…", () -> { popup.setVisible(false); request.custom().run(); }));
        if (request.numeric()) top.add(link("10 Primeiros…", () -> { popup.setVisible(false); request.top10().run(); }));
        root.add(top, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 4));
        JTextField search = new JTextField();
        search.putClientProperty("JTextField.placeholderText", "Pesquisar");
        center.add(search, BorderLayout.NORTH);
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(UiTokens.surface());
        Map<String, JCheckBox> boxes = new LinkedHashMap<>();
        Set<String> selected = request.current() == null || request.current().values() == null ? null : new LinkedHashSet<>(request.current().values());
        boolean blanksSelected = request.current() == null || request.current().values() == null || request.current().includeBlanks();
        JCheckBox all = new JCheckBox("(Selecionar Tudo)", true);
        all.setOpaque(false);
        list.add(all);
        for (String v : request.values()) {
            String label = v.isEmpty() ? "(Vazias)" : v;
            boolean on = v.isEmpty() ? blanksSelected : selected == null || selected.contains(v);
            JCheckBox b = new JCheckBox(label, on);
            b.setOpaque(false);
            boxes.put(v, b);
            list.add(b);
            b.addActionListener(e -> all.setSelected(boxes.values().stream().filter(Component::isVisible).allMatch(JCheckBox::isSelected)));
        }
        all.setSelected(boxes.values().stream().allMatch(JCheckBox::isSelected));
        all.addActionListener(e -> { for (JCheckBox b : boxes.values()) if (b.isVisible()) b.setSelected(all.isSelected()); });
        search.getDocument().addDocumentListener(new DocumentListener() {
            private void run() {
                String q = search.getText().strip().toLowerCase(Locale.ROOT);
                for (Map.Entry<String, JCheckBox> e : boxes.entrySet()) {
                    boolean show = q.isEmpty() || e.getKey().toLowerCase(Locale.ROOT).contains(q);
                    e.getValue().setVisible(show);
                    if (!q.isEmpty()) e.getValue().setSelected(show);
                }
                list.revalidate();
                list.repaint();
            }
            @Override public void insertUpdate(DocumentEvent e) { run(); }
            @Override public void removeUpdate(DocumentEvent e) { run(); }
            @Override public void changedUpdate(DocumentEvent e) { run(); }
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(240, 220));
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        center.add(scroll, BorderLayout.CENTER);
        root.add(center, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.TRAILING, 6, 0));
        JButton ok = new JButton("OK");
        JButton cancel = new JButton("Cancelar");
        ok.addActionListener(e -> {
            popup.setVisible(false);
            if (boxes.values().stream().allMatch(JCheckBox::isSelected)) { request.clear().run(); return; }
            Set<String> values = new LinkedHashSet<>();
            boolean blanks = false;
            for (Map.Entry<String, JCheckBox> en : boxes.entrySet()) {
                if (!en.getValue().isSelected()) continue;
                if (en.getKey().isEmpty()) blanks = true; else values.add(en.getKey());
            }
            request.apply().accept(FilterCriteria.values(values, blanks));
        });
        cancel.addActionListener(e -> popup.setVisible(false));
        buttons.add(ok);
        buttons.add(cancel);
        root.add(buttons, BorderLayout.SOUTH);
        popup.add(root);
        popup.show(invoker, anchor.x + anchor.width - 250, anchor.y + anchor.height);
        search.requestFocusInWindow();
        return popup;
    }

    private static JButton link(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setAlignmentX(0f);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, b.getPreferredSize().height));
        b.addActionListener(e -> action.run());
        return b;
    }

    static List<String> sorted(List<String> values) { return new ArrayList<>(values); }

    static JComponent label(String text) { return new JLabel(text); }
}
