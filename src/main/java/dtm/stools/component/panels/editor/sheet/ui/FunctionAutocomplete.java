package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.function.SheetFunction;
import dtm.stools.component.panels.editor.sheet.model.DefinedName;
import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FunctionAutocomplete {
    public record Item(String name, String insert, String detail, boolean function) {
        @Override public String toString() { return name; }
    }

    private final JWindow window;
    private final DefaultListModel<Item> model = new DefaultListModel<>();
    private final JList<Item> list = new JList<>(model);
    private final JLabel detail = new JLabel();
    private JTextComponent target;
    private int replaceStart, replaceEnd;

    public FunctionAutocomplete(Component owner) {
        Window w = SwingUtilities.getWindowAncestor(owner);
        window = new JWindow(w);
        window.setFocusableWindowState(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(8);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean s, boolean f) {
                JLabel c = (JLabel) super.getListCellRendererComponent(l, v, i, s, f);
                Item it = (Item) v;
                c.setText((it.function() ? "ƒx  " : "▭  ") + it.name());
                c.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                return c;
            }
        });
        list.addMouseListener(new MouseAdapter() { @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) accept(); } });
        list.addListSelectionListener(e -> { Item it = list.getSelectedValue(); detail.setText(it == null ? " " : "<html><div style='width:260px'>" + it.detail() + "</div></html>"); });
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(UiTokens.border()));
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        detail.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        panel.add(detail, BorderLayout.SOUTH);
        window.setContentPane(panel);
    }

    public boolean isVisible() { return window.isVisible(); }
    public void hide() { window.setVisible(false); }

    public void update(JTextComponent text, FunctionRegistry functions, FormulaLocale locale, List<DefinedName> names) {
        target = text;
        String t = text.getText();
        int caret = text.getCaretPosition();
        String prefix = FormulaHighlighter.currentIdentifier(t, caret);
        if (prefix == null || prefix.isEmpty()) { hide(); return; }
        String upper = prefix.toUpperCase(locale.names() == null ? Locale.ROOT : Locale.forLanguageTag("pt-BR"));
        List<Item> items = new ArrayList<>();
        for (SheetFunction f : functions.all()) {
            String display = locale.localizeFunction(f.name());
            if (display.startsWith(upper) || f.name().startsWith(upper) && !display.equals(f.name()) && items.size() < 3)
                items.add(new Item(display, display + "(", FunctionSignatures.description(f), true));
            if (items.size() > 60) break;
        }
        for (DefinedName n : names) if (n.name().toUpperCase(Locale.ROOT).startsWith(prefix.toUpperCase(Locale.ROOT)) && !n.hidden()) items.add(new Item(n.name(), n.name(), "=" + n.formula(), false));
        if (items.isEmpty()) { hide(); return; }
        items.sort((a, b) -> a.name().length() != b.name().length() && a.name().startsWith(b.name()) ? 1 : a.name().compareTo(b.name()));
        model.clear();
        items.forEach(model::addElement);
        list.setSelectedIndex(0);
        replaceStart = caret - prefix.length();
        replaceEnd = caret;
        try {
            Rectangle r = text.modelToView2D(replaceStart).getBounds();
            Point p = new Point(r.x, r.y + r.height + 2);
            SwingUtilities.convertPointToScreen(p, text);
            window.setSize(new Dimension(300, Math.min(240, 60 + items.size() * 20)));
            window.setLocation(p);
            window.setVisible(true);
        } catch (BadLocationException | RuntimeException e) { hide(); }
    }

    public void move(int delta) {
        int n = model.size();
        if (n == 0) return;
        int i = Math.floorMod(list.getSelectedIndex() + delta, n);
        list.setSelectedIndex(i);
        list.ensureIndexIsVisible(i);
    }

    public boolean accept() {
        Item it = list.getSelectedValue();
        if (it == null || target == null) return false;
        try {
            target.getDocument().remove(replaceStart, replaceEnd - replaceStart);
            target.getDocument().insertString(replaceStart, it.insert(), null);
            target.setCaretPosition(replaceStart + it.insert().length());
        } catch (BadLocationException ignored) { }
        hide();
        return true;
    }

    public void dispose() { window.dispose(); }
}
