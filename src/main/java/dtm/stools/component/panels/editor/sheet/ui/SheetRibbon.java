package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.format.BuiltinFormats;
import dtm.stools.configs.UiTokens;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SheetRibbon extends JPanel {
    public static final String ICON = "sheet.icon";
    public static final String MENU = "sheet.menu";
    public static final String TOGGLE = "sheet.toggle";

    private final SheetEditor editor;
    private final JPanel tabStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel cards = new JPanel(new CardLayout());
    private final Map<String, SheetRibbonPage> pages = new LinkedHashMap<>();
    private final Map<String, JToggleButton> tabButtons = new LinkedHashMap<>();
    private final ButtonGroup tabGroup = new ButtonGroup();
    private final List<RibbonTab> tabs = new ArrayList<>();
    private final List<String> contextual = new ArrayList<>();
    private String current;
    private final List<JComboBox<String>> fontBoxes = new ArrayList<>(), sizeBoxes = new ArrayList<>(), formatBoxes = new ArrayList<>();
    private boolean updating;

    public SheetRibbon(SheetEditor editor) {
        super(new BorderLayout());
        this.editor = editor;
        setName("sheet.ribbon");
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.border()));
        tabStrip.setOpaque(false);
        tabStrip.setBorder(BorderFactory.createEmptyBorder(2, 6, 0, 6));
        add(tabStrip, BorderLayout.NORTH);
        cards.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        add(cards, BorderLayout.CENTER);
        for (RibbonTab t : SheetRibbonLayout.defaults(editor, this)) addTab(t);
        select(tabs.stream().anyMatch(t -> t.id().equals("home")) ? "home" : tabs.getFirst().id());
        addComponentListener(new ComponentAdapter() { @Override public void componentResized(ComponentEvent e) { adapt(); } });
    }

    public void addTab(RibbonTab tab) {
        tabs.add(tab);
        SheetRibbonPage page = new SheetRibbonPage(tab.groups(), this::fullGroup, this::compactGroup);
        pages.put(tab.id(), page);
        cards.add(page, tab.id());
        JToggleButton b = new JToggleButton(tab.title());
        b.putClientProperty("JButton.buttonType", "tab");
        b.setFocusable(false);
        b.setName("sheet.ribbon.tab." + tab.id());
        if (tab.contextual()) { b.setForeground(new Color(0x107C41)); b.setVisible(false); contextual.add(tab.id()); }
        b.addActionListener(e -> select(tab.id()));
        tabGroup.add(b);
        tabButtons.put(tab.id(), b);
        tabStrip.add(b);
    }

    public void addGroup(String tabId, RibbonGroup group) {
        List<RibbonTab> copy = new ArrayList<>(tabs);
        for (int k = 0; k < copy.size(); k++) {
            RibbonTab t = copy.get(k);
            if (!t.id().equals(tabId)) continue;
            List<RibbonGroup> groups = new ArrayList<>(t.groups());
            groups.add(group);
            RibbonTab next = new RibbonTab(t.id(), t.title(), groups, t.contextual());
            tabs.set(k, next);
            cards.remove(pages.get(tabId));
            SheetRibbonPage page = new SheetRibbonPage(groups, this::fullGroup, this::compactGroup);
            pages.put(tabId, page);
            cards.add(page, tabId);
            if (tabId.equals(current)) select(tabId);
            return;
        }
        addTab(new RibbonTab(tabId, tabId, List.of(group), false));
    }

    public void removeGroup(String tabId, String groupId) {
        for (int k = 0; k < tabs.size(); k++) {
            RibbonTab t = tabs.get(k);
            if (!t.id().equals(tabId)) continue;
            List<RibbonGroup> groups = new ArrayList<>(t.groups());
            groups.removeIf(g -> g.id().equals(groupId));
            tabs.set(k, new RibbonTab(t.id(), t.title(), groups, t.contextual()));
            cards.remove(pages.get(tabId));
            SheetRibbonPage page = new SheetRibbonPage(groups, this::fullGroup, this::compactGroup);
            pages.put(tabId, page);
            cards.add(page, tabId);
            if (tabId.equals(current)) select(tabId);
        }
    }

    public void select(String id) {
        current = id;
        JToggleButton b = tabButtons.get(id);
        if (b != null) b.setSelected(true);
        ((CardLayout) cards.getLayout()).show(cards, id);
        adapt();
    }

    public String selectedTab() { return current; }
    public List<RibbonTab> tabs() { return List.copyOf(tabs); }

    public void setContextualTabs(List<String> visible) {
        for (String id : contextual) {
            JToggleButton b = tabButtons.get(id);
            boolean show = visible.contains(id);
            if (b.isVisible() != show) b.setVisible(show);
            if (!show && id.equals(current)) select(tabs.getFirst().id());
        }
        tabStrip.revalidate();
    }

    private void adapt() {
        SheetRibbonPage page = pages.get(current);
        if (page != null && getWidth() > 0) page.adapt(getWidth() - 12);
    }

    private JComponent fullGroup(RibbonGroup g) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JPanel column = null;
        int count = 0;
        for (RibbonItem item : g.items()) {
            if (item.custom() != null) {
                if (column == null || count >= 3) { column = smallColumn(); panel.add(column); count = 0; }
                JComponent c = item.custom().get();
                column.add(row(c));
                count++;
                continue;
            }
            Action a = editor.getCommands().get(item.command());
            if (a == null) continue;
            if (item.large()) { panel.add(largeButton(a)); column = null; continue; }
            if (column == null || count >= 3) { column = smallColumn(); panel.add(column); count = 0; }
            column.add(row(smallButton(a, true)));
            count++;
        }
        return panel;
    }

    private JComponent compactGroup(RibbonGroup g) {
        JPanel panel = new JPanel(new GridLayout(3, 0, 1, 1));
        panel.setOpaque(false);
        for (RibbonItem item : g.items()) {
            if (item.custom() != null) continue;
            Action a = editor.getCommands().get(item.command());
            if (a != null) panel.add(smallButton(a, false));
        }
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(panel);
        return wrap;
    }

    private static JPanel smallColumn() {
        JPanel col = new JPanel(new GridLayout(3, 1, 0, 1));
        col.setOpaque(false);
        return col;
    }

    private static JComponent row(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        p.setOpaque(false);
        p.add(c);
        return p;
    }

    private AbstractButton largeButton(Action a) {
        AbstractButton b = Boolean.TRUE.equals(a.getValue(TOGGLE)) ? new JToggleButton(a) : new JButton(a);
        b.setIcon(SheetIcon.large(iconOf(a)));
        b.setVerticalTextPosition(SwingConstants.BOTTOM);
        b.setHorizontalTextPosition(SwingConstants.CENTER);
        b.setText(wrapLabel(String.valueOf(a.getValue(Action.NAME))));
        configure(b, a);
        b.setMargin(new Insets(2, 4, 2, 4));
        return b;
    }

    private AbstractButton smallButton(Action a, boolean label) {
        AbstractButton b = Boolean.TRUE.equals(a.getValue(TOGGLE)) ? new JToggleButton(a) : new JButton(a);
        b.setIcon(SheetIcon.small(iconOf(a)));
        if (!label || Boolean.TRUE.equals(a.getValue("sheet.iconOnly"))) b.setText(null);
        configure(b, a);
        b.setMargin(new Insets(1, 3, 1, 3));
        return b;
    }

    private void configure(AbstractButton b, Action a) {
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        Object tip = a.getValue(Action.SHORT_DESCRIPTION);
        b.setToolTipText(tip != null ? tip.toString() : String.valueOf(a.getValue(Action.NAME)));
        b.setName("sheet.ribbon.button." + a.getValue(Action.ACTION_COMMAND_KEY));
        Object menu = a.getValue(MENU);
        if (menu instanceof List<?> ids) {
            String text = b.getText();
            javax.swing.Icon icon = b.getIcon();
            for (java.awt.event.ActionListener l : b.getActionListeners()) b.removeActionListener(l);
            b.setAction(null);
            b.setIcon(icon);
            b.setText(text == null || text.isEmpty() ? null : text.endsWith("</html>") ? text.replace("</html>", " ▾</html>") : text + " ▾");
            b.setEnabled(a.isEnabled());
            a.addPropertyChangeListener(e -> { if ("enabled".equals(e.getPropertyName())) b.setEnabled(a.isEnabled()); });
            b.addActionListener(e -> {
                JPopupMenu popup = new JPopupMenu();
                fillMenu(popup, ids);
                popup.show(b, 0, b.getHeight());
            });
        }
    }

    private void fillMenu(JComponent menu, List<?> ids) {
        for (Object o : ids) {
            String id = String.valueOf(o);
            if (id.equals("-")) { if (menu instanceof JPopupMenu p) p.addSeparator(); else if (menu instanceof JMenu m) m.addSeparator(); continue; }
            Action sub = editor.getCommands().get(id);
            if (sub == null) continue;
            JMenuItem item;
            if (sub.getValue(MENU) instanceof List<?> nested) {
                JMenu m = new JMenu(String.valueOf(sub.getValue(Action.NAME)));
                m.setIcon(SheetIcon.small(iconOf(sub)));
                fillMenu(m, nested);
                item = m;
            } else {
                item = new JMenuItem(sub);
                item.setIcon(SheetIcon.small(iconOf(sub)));
            }
            menu.add(item);
        }
    }

    public static String iconOf(Action a) {
        Object icon = a.getValue(ICON);
        return icon == null ? "" : icon.toString();
    }

    private static String wrapLabel(String s) {
        if (s.length() < 10 || !s.contains(" ")) return s;
        int mid = s.length() / 2, split = s.indexOf(' ', mid);
        if (split < 0) split = s.lastIndexOf(' ');
        return "<html><center>" + s.substring(0, split) + "<br>" + s.substring(split + 1) + "</center></html>";
    }

    public JComboBox<String> fontBox() {
        JComboBox<String> fontBox = new JComboBox<>(new DefaultComboBoxModel<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        fontBox.setEditable(true);
        fontBox.setPreferredSize(new Dimension(150, 24));
        fontBox.setName("sheet.ribbon.font");
        fontBox.addActionListener(e -> { if (!updating && fontBox.getSelectedItem() != null) editor.applyFontFamily(fontBox.getSelectedItem().toString()); });
        fontBoxes.add(fontBox);
        return fontBox;
    }

    public JComboBox<String> sizeBox() {
        JComboBox<String> sizeBox = new JComboBox<>(new String[]{"8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "72"});
        sizeBox.setEditable(true);
        sizeBox.setPreferredSize(new Dimension(58, 24));
        sizeBox.setName("sheet.ribbon.size");
        sizeBox.addActionListener(e -> {
            if (updating || sizeBox.getSelectedItem() == null) return;
            try { editor.applyFontSize(Double.parseDouble(sizeBox.getSelectedItem().toString().replace(',', '.'))); } catch (NumberFormatException ignored) { }
        });
        sizeBoxes.add(sizeBox);
        return sizeBox;
    }

    public JComboBox<String> formatBox() {
        JComboBox<String> formatBox = new JComboBox<>(new String[]{"Geral", "Número", "Moeda", "Contábil", "Data Abreviada", "Data Completa", "Hora", "Porcentagem", "Fração", "Científico", "Texto", "Mais Formatos de Número…"});
        formatBox.setPreferredSize(new Dimension(140, 24));
        formatBox.setName("sheet.ribbon.numberFormat");
        formatBox.addActionListener(e -> {
            if (updating) return;
            String sel = String.valueOf(formatBox.getSelectedItem());
            String code = switch (sel) {
                case "Número" -> "#,##0.00";
                case "Moeda" -> BuiltinFormats.CURRENCY_BRL;
                case "Contábil" -> BuiltinFormats.ACCOUNTING_BRL;
                case "Data Abreviada" -> "dd/mm/yyyy";
                case "Data Completa" -> "dddd, d \"de\" mmmm \"de\" yyyy";
                case "Hora" -> "hh:mm:ss";
                case "Porcentagem" -> "0.00%";
                case "Fração" -> "# ?/?";
                case "Científico" -> "0.00E+00";
                case "Texto" -> "@";
                case "Geral" -> "General";
                default -> null;
            };
            if (code == null) editor.execute("sheet.format.cells");
            else editor.applyNumberFormat(code);
        });
        formatBoxes.add(formatBox);
        return formatBox;
    }

    public void updateState(String font, double size, String format) {
        updating = true;
        try {
            for (JComboBox<String> b : fontBoxes) b.setSelectedItem(font);
            for (JComboBox<String> b : sizeBoxes) b.setSelectedItem(size == Math.rint(size) ? String.valueOf((int) size) : String.valueOf(size));
            for (JComboBox<String> b : formatBoxes) b.setSelectedItem(formatName(format));
        } finally {
            updating = false;
        }
    }

    private static String formatName(String code) {
        if (code == null || code.equals("General")) return "Geral";
        if (code.equals("@")) return "Texto";
        if (code.contains("%")) return "Porcentagem";
        if (code.contains("E+")) return "Científico";
        if (code.contains("?/")) return "Fração";
        if (code.contains("R$") || code.contains("$")) return code.contains("*") ? "Contábil" : "Moeda";
        if (code.contains("dddd")) return "Data Completa";
        if (code.contains("d") && code.contains("y")) return "Data Abreviada";
        if (code.contains("h") || code.contains("s")) return "Hora";
        return "Número";
    }

    public JComponent colorButton(String id, String icon, String tooltip, boolean fill) {
        JButton b = new JButton(SheetIcon.small(icon));
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        b.setToolTipText(tooltip);
        b.setName("sheet.ribbon.button." + id);
        b.addActionListener(e -> ColorPalettePopup.show(b, 0, b.getHeight(), editor.getSession().getWorkbook().properties().theme(), fill ? "Sem Preenchimento" : "Automático",
                c -> { if (fill) editor.applyFill(c); else editor.applyFontColor(c); }));
        return b;
    }

    public JComponent styleButtons(String... ids) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        for (String id : ids) {
            if (id.equals("|")) { p.add(Box.createHorizontalStrut(4)); continue; }
            Action a = editor.getCommands().get(id);
            if (a != null) p.add(smallButton(a, false));
        }
        return p;
    }

    public static Font small(Font f) { return f.deriveFont(f.getSize2D() - 1f); }
}
