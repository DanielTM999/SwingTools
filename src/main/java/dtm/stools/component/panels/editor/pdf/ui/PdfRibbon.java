package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.component.panels.editor.pdf.api.PdfShapeStyle;
import dtm.stools.component.panels.editor.pdf.api.PdfTextStyle;
import dtm.stools.component.panels.editor.pdf.ui.popup.PdfColorPopup;
import dtm.stools.configs.UiTokens;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PdfRibbon extends JPanel {
    public static final String ICON = "pdf.icon";
    public static final String MENU = "pdf.menu";
    public static final String TOGGLE = "pdf.toggle";
    public static final String ICON_ONLY = "pdf.iconOnly";

    private final PdfEditor editor;
    private final JPanel tabStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel quick = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
    private final JPanel cards = new JPanel(new CardLayout());
    private final Map<String, PdfRibbonPage> pages = new LinkedHashMap<>();
    private final Map<String, JToggleButton> tabButtons = new LinkedHashMap<>();
    private final ButtonGroup tabGroup = new ButtonGroup();
    private final List<PdfRibbonTab> tabs = new ArrayList<>();
    private final List<String> contextual = new ArrayList<>();
    private final List<JComboBox<String>> fontBoxes = new ArrayList<>(), sizeBoxes = new ArrayList<>(), widthBoxes = new ArrayList<>();
    private final List<JButton> textColors = new ArrayList<>(), strokeColors = new ArrayList<>(), fillColors = new ArrayList<>();
    private final List<JToggleButton> boldButtons = new ArrayList<>(), italicButtons = new ArrayList<>();
    private String current;
    private boolean updating;

    public PdfRibbon(PdfEditor editor) {
        super(new BorderLayout());
        this.editor = editor;
        setName("pdf.ribbon");
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.border()));
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        tabStrip.setOpaque(false);
        tabStrip.setBorder(BorderFactory.createEmptyBorder(3, 6, 0, 6));
        quick.setOpaque(false);
        quick.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 8));
        header.add(tabStrip, BorderLayout.CENTER);
        header.add(quick, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        cards.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        add(cards, BorderLayout.CENTER);
        for (PdfRibbonTab tab : PdfRibbonLayout.defaults(editor, this)) addTab(tab);
        select(tabs.stream().anyMatch(tab -> tab.id().equals("home")) ? "home" : tabs.getFirst().id());
        rebuildQuickAccess();
        addComponentListener(new ComponentAdapter() { @Override public void componentResized(ComponentEvent event) { adapt(); } });
    }

    public void addTab(PdfRibbonTab tab) {
        tabs.add(tab);
        PdfRibbonPage page = page(tab.groups());
        pages.put(tab.id(), page);
        cards.add(page, tab.id());
        JToggleButton button = new JToggleButton(tab.title());
        button.putClientProperty("JButton.buttonType", "tab");
        button.setFocusable(false);
        button.setName("pdf.ribbon.tab." + tab.id());
        if (tab.contextual()) {
            button.setForeground(PdfIcon.RED);
            button.setVisible(false);
            contextual.add(tab.id());
        }
        button.addActionListener(event -> select(tab.id()));
        tabGroup.add(button);
        tabButtons.put(tab.id(), button);
        tabStrip.add(button);
    }

    public void addGroup(String tabId, PdfRibbonGroup group) {
        for (int index = 0; index < tabs.size(); index++) {
            PdfRibbonTab tab = tabs.get(index);
            if (!tab.id().equals(tabId)) continue;
            List<PdfRibbonGroup> groups = new ArrayList<>(tab.groups());
            groups.removeIf(existing -> existing.id().equals(group.id()));
            groups.add(group);
            replace(index, new PdfRibbonTab(tab.id(), tab.title(), groups, tab.contextual()));
            return;
        }
        addTab(new PdfRibbonTab(tabId, tabId, List.of(group), false));
    }

    public void removeGroup(String tabId, String groupId) {
        for (int index = 0; index < tabs.size(); index++) {
            PdfRibbonTab tab = tabs.get(index);
            if (!tab.id().equals(tabId)) continue;
            List<PdfRibbonGroup> groups = new ArrayList<>(tab.groups());
            if (!groups.removeIf(group -> group.id().equals(groupId))) return;
            replace(index, new PdfRibbonTab(tab.id(), tab.title(), groups, tab.contextual()));
        }
    }

    public void addItem(String tabId, String groupId, String title, PdfRibbonItem item) {
        PdfRibbonGroup existing = group(tabId, groupId);
        List<PdfRibbonItem> items = new ArrayList<>(existing == null ? List.of() : existing.items());
        items.add(item);
        addGroup(tabId, new PdfRibbonGroup(groupId, title, existing == null ? 15 : existing.priority(), "more", items));
    }

    public void removeItem(String tabId, String groupId, String command) {
        PdfRibbonGroup existing = group(tabId, groupId);
        if (existing == null) return;
        List<PdfRibbonItem> items = new ArrayList<>(existing.items());
        items.removeIf(item -> command.equals(item.command()));
        if (items.isEmpty()) removeGroup(tabId, groupId);
        else addGroup(tabId, new PdfRibbonGroup(groupId, existing.title(), existing.priority(), existing.icon(), items));
    }

    private PdfRibbonGroup group(String tabId, String groupId) {
        for (PdfRibbonTab tab : tabs)
            if (tab.id().equals(tabId)) for (PdfRibbonGroup group : tab.groups()) if (group.id().equals(groupId)) return group;
        return null;
    }

    private void replace(int index, PdfRibbonTab tab) {
        tabs.set(index, tab);
        PdfRibbonPage old = pages.get(tab.id());
        if (old != null) cards.remove(old);
        PdfRibbonPage page = page(tab.groups());
        pages.put(tab.id(), page);
        cards.add(page, tab.id());
        if (tab.id().equals(current)) select(tab.id());
    }

    public void applyTheme() {
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.border()));
        refresh();
    }

    public void refresh() {
        for (int index = 0; index < tabs.size(); index++) replace(index, tabs.get(index));
        rebuildQuickAccess();
        updateStyle(editor.getTextStyle(), editor.getShapeStyle());
    }

    private PdfRibbonPage page(List<PdfRibbonGroup> groups) {
        return new PdfRibbonPage(groups, this::fullGroup, this::compactGroup);
    }

    public void select(String id) {
        current = id;
        JToggleButton button = tabButtons.get(id);
        if (button != null) button.setSelected(true);
        ((CardLayout) cards.getLayout()).show(cards, id);
        adapt();
    }

    public String selectedTab() { return current; }
    public List<PdfRibbonTab> tabs() { return List.copyOf(tabs); }

    public void setContextualTabs(List<String> visible) {
        for (String id : contextual) {
            JToggleButton button = tabButtons.get(id);
            boolean show = visible.contains(id);
            if (button.isVisible() != show) button.setVisible(show);
            if (!show && id.equals(current)) select("home");
        }
        tabStrip.revalidate();
        tabStrip.repaint();
    }

    private void adapt() {
        PdfRibbonPage page = pages.get(current);
        if (page != null && getWidth() > 0) page.adapt(getWidth() - 14);
    }

    private void rebuildQuickAccess() {
        quick.removeAll();
        for (String id : List.of("pdf.save", "pdf.undo", "pdf.redo")) {
            Action action = editor.getCommands().get(id);
            if (action == null) continue;
            AbstractButton button = smallButton(action, false);
            button.setName("pdf.quick." + id);
            quick.add(button);
        }
        quick.revalidate();
        quick.repaint();
    }

    private JComponent fullGroup(PdfRibbonGroup group) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JPanel column = null;
        int count = 0;
        for (PdfRibbonItem item : group.items()) {
            if (item.custom() != null) {
                if (column == null || count >= 3) { column = column(); panel.add(column); count = 0; }
                column.add(row(item.custom().get()));
                count++;
                continue;
            }
            Action action = editor.getCommands().get(item.command());
            if (action == null) continue;
            if (item.large()) { panel.add(largeButton(action)); column = null; continue; }
            if (column == null || count >= 3) { column = column(); panel.add(column); count = 0; }
            column.add(row(smallButton(action, true)));
            count++;
        }
        return panel;
    }

    private JComponent compactGroup(PdfRibbonGroup group) {
        JPanel panel = new JPanel(new GridLayout(3, 0, 1, 1));
        panel.setOpaque(false);
        for (PdfRibbonItem item : group.items()) {
            if (item.custom() != null) continue;
            Action action = editor.getCommands().get(item.command());
            if (action != null) panel.add(smallButton(action, false));
        }
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(panel);
        return wrap;
    }

    private static JPanel column() {
        JPanel column = new JPanel(new GridLayout(3, 1, 0, 1));
        column.setOpaque(false);
        return column;
    }

    private static JComponent row(JComponent component) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
        row.setOpaque(false);
        row.add(component);
        return row;
    }

    private AbstractButton largeButton(Action action) {
        AbstractButton button = Boolean.TRUE.equals(action.getValue(TOGGLE)) ? new JToggleButton(action) : new JButton(action);
        button.setIcon(PdfIcon.large(iconOf(action)));
        button.setDisabledIcon(null);
        button.setVerticalTextPosition(SwingConstants.BOTTOM);
        button.setHorizontalTextPosition(SwingConstants.CENTER);
        button.setText(wrap(String.valueOf(action.getValue(Action.NAME))));
        configure(button, action);
        button.setMargin(new Insets(3, 6, 3, 6));
        button.setMinimumSize(new Dimension(52, 72));
        return button;
    }

    private AbstractButton smallButton(Action action, boolean label) {
        AbstractButton button = Boolean.TRUE.equals(action.getValue(TOGGLE)) ? new JToggleButton(action) : new JButton(action);
        button.setIcon(PdfIcon.small(iconOf(action)));
        if (!label || Boolean.TRUE.equals(action.getValue(ICON_ONLY))) button.setText(null);
        configure(button, action);
        button.setMargin(new Insets(2, 4, 2, 6));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        return button;
    }

    private void configure(AbstractButton button, Action action) {
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setFocusable(false);
        Object tip = action.getValue(Action.SHORT_DESCRIPTION);
        button.setToolTipText(tip != null ? tip.toString() : String.valueOf(action.getValue(Action.NAME)));
        button.setName("pdf.ribbon.button." + action.getValue(Action.ACTION_COMMAND_KEY));
        if (action.getValue(MENU) instanceof List<?> ids) {
            String text = button.getText();
            Icon icon = button.getIcon();
            for (var listener : button.getActionListeners()) button.removeActionListener(listener);
            button.setAction(null);
            button.setIcon(icon);
            button.setText(text == null || text.isEmpty() ? null : text.endsWith("</html>") ? text.replace("</html>", " ▾</html>") : text + " ▾");
            button.setEnabled(action.isEnabled());
            action.addPropertyChangeListener(event -> { if ("enabled".equals(event.getPropertyName())) button.setEnabled(action.isEnabled()); });
            button.addActionListener(event -> {
                JPopupMenu popup = new JPopupMenu();
                fillMenu(popup, ids);
                popup.show(button, 0, button.getHeight());
            });
        }
    }

    private void fillMenu(JComponent menu, List<?> ids) {
        for (Object value : ids) {
            String id = String.valueOf(value);
            if (id.equals("-")) {
                if (menu instanceof JPopupMenu popup) popup.addSeparator();
                else if (menu instanceof JMenu nested) nested.addSeparator();
                continue;
            }
            Action action = editor.getCommands().get(id);
            if (action == null) continue;
            JMenuItem item;
            if (action.getValue(MENU) instanceof List<?> nested) {
                JMenu submenu = new JMenu(String.valueOf(action.getValue(Action.NAME)));
                submenu.setIcon(PdfIcon.small(iconOf(action)));
                fillMenu(submenu, nested);
                item = submenu;
            } else {
                item = new JMenuItem(action);
                item.setIcon(PdfIcon.small(iconOf(action)));
            }
            menu.add(item);
        }
    }

    public static String iconOf(Action action) {
        Object icon = action.getValue(ICON);
        return icon == null ? "" : icon.toString();
    }

    private static String wrap(String text) {
        if (text.length() < 10 || !text.contains(" ")) return text;
        int middle = text.length() / 2, split = text.indexOf(' ', middle);
        if (split < 0) split = text.lastIndexOf(' ');
        return "<html><center>" + text.substring(0, split) + "<br>" + text.substring(split + 1) + "</center></html>";
    }

    public JComponent fontBox() {
        JComboBox<String> box = new JComboBox<>(PdfTextStyle.FAMILIES.toArray(String[]::new));
        box.setPreferredSize(new Dimension(118, 24));
        box.setName("pdf.ribbon.font");
        box.setToolTipText("Fonte");
        box.setFocusable(false);
        box.setSelectedItem(editor.getTextStyle().family());
        box.addActionListener(event -> {
            if (!updating && box.getSelectedItem() != null) editor.setTextStyle(editor.getTextStyle().withFamily(box.getSelectedItem().toString()));
        });
        fontBoxes.add(box);
        return box;
    }

    public JComponent sizeBox() {
        JComboBox<String> box = new JComboBox<>(new String[]{"8", "9", "10", "11", "12", "14", "16", "18", "20", "24", "28", "32", "36", "48", "72"});
        box.setEditable(true);
        box.setPreferredSize(new Dimension(58, 24));
        box.setName("pdf.ribbon.size");
        box.setToolTipText("Tamanho da fonte");
        box.setSelectedItem(format(editor.getTextStyle().size()));
        box.addActionListener(event -> {
            if (updating || box.getSelectedItem() == null) return;
            try {
                float size = Float.parseFloat(box.getSelectedItem().toString().replace(',', '.'));
                editor.setTextStyle(editor.getTextStyle().withSize(Math.max(2, Math.min(400, size))));
            } catch (NumberFormatException ignored) {
                box.setSelectedItem(format(editor.getTextStyle().size()));
            }
        });
        sizeBoxes.add(box);
        return box;
    }

    public JComponent textFormatButtons() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JToggleButton bold = toggle("bold", "Negrito");
        bold.addActionListener(event -> { if (!updating) editor.setTextStyle(editor.getTextStyle().withBold(bold.isSelected())); });
        boldButtons.add(bold);
        JToggleButton italic = toggle("italic", "Itálico");
        italic.addActionListener(event -> { if (!updating) editor.setTextStyle(editor.getTextStyle().withItalic(italic.isSelected())); });
        italicButtons.add(italic);
        JButton color = colorButton("color-text", "Cor do texto", editor.getTextStyle().color(), null,
                chosen -> { if (chosen != null) editor.setTextStyle(editor.getTextStyle().withColor(chosen)); });
        textColors.add(color);
        panel.add(bold);
        panel.add(italic);
        panel.add(Box.createHorizontalStrut(4));
        panel.add(color);
        return panel;
    }

    public JComponent shapeStyleButtons() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JButton stroke = colorButton("color-stroke", "Cor do contorno", editor.getShapeStyle().stroke(), null,
                chosen -> { if (chosen != null) editor.setShapeStyle(editor.getShapeStyle().withStroke(chosen)); });
        strokeColors.add(stroke);
        JButton fill = colorButton("color-fill", "Preenchimento", editor.getShapeStyle().fill(), "Sem preenchimento",
                chosen -> editor.setShapeStyle(editor.getShapeStyle().withFill(chosen)));
        fillColors.add(fill);
        panel.add(stroke);
        panel.add(fill);
        return panel;
    }

    public JComponent lineWidthBox() {
        JComboBox<String> box = new JComboBox<>(new String[]{"0.5", "1", "1.5", "2", "3", "4", "6", "8", "12"});
        box.setEditable(true);
        box.setPreferredSize(new Dimension(62, 24));
        box.setName("pdf.ribbon.lineWidth");
        box.setToolTipText("Espessura da linha");
        box.setSelectedItem(format(editor.getShapeStyle().lineWidth()));
        box.addActionListener(event -> {
            if (updating || box.getSelectedItem() == null) return;
            try {
                float width = Float.parseFloat(box.getSelectedItem().toString().replace(',', '.'));
                editor.setShapeStyle(editor.getShapeStyle().withLineWidth(Math.max(0, Math.min(72, width))));
            } catch (NumberFormatException ignored) {
                box.setSelectedItem(format(editor.getShapeStyle().lineWidth()));
            }
        });
        widthBoxes.add(box);
        return box;
    }

    public JComponent eraserSizeBox() {
        JComboBox<String> box = new JComboBox<>(new String[]{"4", "8", "12", "16", "24", "32", "48", "72"});
        box.setEditable(true);
        box.setPreferredSize(new Dimension(62, 24));
        box.setName("pdf.ribbon.eraserSize");
        box.setToolTipText("Tamanho do pincel da borracha (pt)");
        box.setSelectedItem(format(editor.getEraserSize()));
        box.addActionListener(event -> {
            if (updating || box.getSelectedItem() == null) return;
            try { editor.setEraserSize(Float.parseFloat(box.getSelectedItem().toString().replace(',', '.'))); }
            catch (NumberFormatException ignored) { box.setSelectedItem(format(editor.getEraserSize())); }
        });
        return box;
    }

    public void updateStyle(PdfTextStyle text, PdfShapeStyle shape) {
        updating = true;
        try {
            for (JComboBox<String> box : fontBoxes) box.setSelectedItem(text.family());
            for (JComboBox<String> box : sizeBoxes) box.setSelectedItem(format(text.size()));
            for (JToggleButton button : boldButtons) button.setSelected(text.bold());
            for (JToggleButton button : italicButtons) button.setSelected(text.italic());
            for (JButton button : textColors) button.setIcon(new PdfIcon("color-text", 16, text.color()));
            for (JButton button : strokeColors) button.setIcon(new PdfIcon("color-stroke", 16, shape.stroke()));
            for (JButton button : fillColors) button.setIcon(new PdfIcon("color-fill", 16, shape.fill() == null ? new Color(0, 0, 0, 0) : shape.fill()));
            for (JComboBox<String> box : widthBoxes) box.setSelectedItem(format(shape.lineWidth()));
        } finally { updating = false; }
    }

    private JToggleButton toggle(String icon, String tip) {
        JToggleButton button = new JToggleButton(PdfIcon.small(icon));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setFocusable(false);
        button.setToolTipText(tip);
        button.setName("pdf.ribbon.button." + icon);
        return button;
    }

    private JButton colorButton(String icon, String tip, Color initial, String noneLabel, java.util.function.Consumer<Color> chosen) {
        JButton button = new JButton(new PdfIcon(icon, 16, initial == null ? new Color(0, 0, 0, 0) : initial));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setFocusable(false);
        button.setToolTipText(tip);
        button.setName("pdf.ribbon.button." + icon);
        button.addActionListener(event -> PdfColorPopup.show(button, 0, button.getHeight(), noneLabel, chosen));
        return button;
    }

    private static String format(float value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }
}
