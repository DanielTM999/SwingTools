package dtm.stools.component.menu.bar;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventSubscription;
import dtm.stools.component.events.EventType;
import dtm.stools.component.menu.bar.action.MenuAction;
import dtm.stools.component.menu.bar.config.GradientStop;
import dtm.stools.component.menu.bar.config.MenuBarConfig;
import dtm.stools.component.menu.bar.config.MenuBarStyle;
import dtm.stools.component.menu.bar.config.MenuConfig;
import dtm.stools.component.menu.bar.config.MenuItemConfig;
import dtm.stools.component.menu.bar.config.MenuSchema;
import dtm.stools.component.menu.bar.event.EventMenuBar;
import dtm.stools.component.menu.bar.event.MenuBarEvent;
import dtm.stools.component.menu.bar.tree.MenuNode;
import dtm.stools.component.menu.bar.tree.MenuTreeEditor;
import lombok.Getter;
import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MenuBar extends JMenuBar implements EventListenerComponent {

    public static final String MENU_OPEN = EventMenuBar.MENU_OPEN;
    public static final String MENU_CLOSE = EventMenuBar.MENU_CLOSE;
    public static final String BEFORE_ITEM_CLICK = EventMenuBar.BEFORE_ITEM_CLICK;
    public static final String ITEM_CLICK = EventMenuBar.ITEM_CLICK;
    private static final String ACTION_PROPERTY = MenuBar.class.getName() + ".action";
    private static final String ACTION_BINDING_PROPERTY = MenuBar.class.getName() + ".actionBinding";
    private static final String CLICK_HANDLER_PROPERTY = MenuBar.class.getName() + ".clickHandler";
    private static final String MOUSE_ENTER_HANDLER_PROPERTY = MenuBar.class.getName() + ".mouseEnterHandler";
    private static final String MOUSE_EXIT_HANDLER_PROPERTY = MenuBar.class.getName() + ".mouseExitHandler";
    private static final String HOVER_ACTIVE_PROPERTY = MenuBar.class.getName() + ".hoverActive";
    public static final String ITEM_MOUSE_ENTER = "itemMouseEnter";
    public static final String ITEM_MOUSE_EXIT = "itemMouseExit";

    protected final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();
    private final Map<String, Menu> menus = new LinkedHashMap<>();
    private final List<Component> leading = new ArrayList<>();
    private final List<Component> trailing = new ArrayList<>();
    private final Component glue = Box.createHorizontalGlue();

    private MenuBarConfig config = MenuBarConfig.modern();

    @Getter
    private MenuBarStyle style = config.getStyle();

    private JLabel brandIconLabel;
    private int brandIconSize = 24;

    public MenuBar() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        super.add(glue);
        applyStyle();
    }

    public MenuBar(MenuBarStyle style) {
        this();
        setStyle(style);
    }

    public MenuBar(MenuBarConfig config) {
        this();
        setConfig(config);
    }

    public static MenuConfig menuConfig(String id, String text) {
        return new MenuConfig(id, text);
    }

    public static MenuItemConfig itemConfig(String id, String text) {
        return new MenuItemConfig(id, text);
    }

    public static MenuItemConfig itemConfig(Action action) {
        return MenuItemConfig.fromAction(action);
    }

    public static MenuAction action(String id, String text) {
        return MenuAction.of(id, text);
    }

    public static MenuSchema schema(MenuConfig... menus) {
        return MenuSchema.of(menus);
    }

    public MenuBar setConfig(MenuBarConfig config) {
        this.config = config == null ? MenuBarConfig.modern() : config.copy();
        this.style = this.config.getStyle();
        applyStyle();
        return this;
    }

    public MenuBarConfig getConfig() {
        return config.copy();
    }

    public MenuBar configure(Consumer<MenuBarConfig> configurer) {
        if (configurer != null) {
            MenuBarConfig next = config.copy();
            configurer.accept(next);
            setConfig(next);
        }
        return this;
    }

    public MenuBar style(Consumer<MenuBarStyle> styleConfigurer) {
        if (styleConfigurer != null) {
            enableCustomDecorations();
            styleConfigurer.accept(style);
            applyStyle();
        }
        return this;
    }

    public MenuBar useModernDefaults() {
        return setConfig(MenuBarConfig.modern());
    }

    public MenuBar useLookAndFeelStyle() {
        return setConfig(MenuBarConfig.lookAndFeel());
    }

    public MenuBar useLookAndFeelGradient() {
        return useLookAndFeelGradient(0.18f, 0);
    }

    public MenuBar useLookAndFeelGradient(float intensity) {
        return useLookAndFeelGradient(intensity, 0);
    }

    public MenuBar useLookAndFeelGradient(float intensity, double angleDegrees) {
        enableCustomDecorations();
        Color base = resolveBackground();
        Color end = blend(base, luminance(base) < 0.5 ? Color.WHITE : Color.BLACK, Math.max(0f, Math.min(1f, intensity)));
        style.setBackground(base);
        style.setBackgroundGradientEnd(end);
        style.setBackgroundGradientAngle(angleDegrees);
        applyStyle();
        return this;
    }

    public MenuBar useAccentGradient() {
        return useAccentGradient(0.35f, 0);
    }

    public MenuBar useAccentGradient(float intensity) {
        return useAccentGradient(intensity, 0);
    }

    public MenuBar useAccentGradient(float intensity, double angleDegrees) {
        enableCustomDecorations();
        Color base = resolveBackground();
        Color accent = resolveAccent();
        style.setBackground(base);
        style.setBackgroundGradientEnd(blend(base, accent, Math.max(0f, Math.min(1f, intensity))));
        style.setBackgroundGradientAngle(angleDegrees);
        applyStyle();
        return this;
    }

    public MenuBar setBackgroundGradient(Color start, Color end, double angleDegrees) {
        enableCustomDecorations();
        style.setBackground(start);
        style.setBackgroundGradientEnd(end);
        style.setBackgroundGradientAngle(angleDegrees);
        style.setBackgroundGradientStops(null);
        style.setBackgroundGradientColors(null);
        applyStyle();
        return this;
    }

    public MenuBar setBackgroundGradient(double angleDegrees, GradientStop... stops) {
        enableCustomDecorations();
        style.backgroundGradient(angleDegrees, stops);
        applyStyle();
        return this;
    }

    public MenuBar setBackgroundGradient(GradientStop... stops) {
        return setBackgroundGradient(style.getBackgroundGradientAngle(), stops);
    }

    public MenuBar setGradientAngle(double angleDegrees) {
        enableCustomDecorations();
        style.setBackgroundGradientAngle(angleDegrees);
        applyStyle();
        return this;
    }

    public MenuBar clearBackgroundGradient() {
        style.setBackgroundGradientEnd(null);
        applyStyle();
        return this;
    }

    public Menu addMenu(String id, String text) {
        Menu menu = createMenu(id, text);
        menus.put(menu.getMenuId(), menu);
        super.add(menu, indexOfGlue());
        applyMenuStyle(menu);
        revalidate();
        repaint();
        return menu;
    }

    public Menu addMenu(MenuConfig config) {
        Objects.requireNonNull(config, "config");
        Menu menu = addMenu(config.getId(), config.getText());
        menu.setIcon(resolveConfigIcon(config.getIcon(), config.getIconId()));
        if (config.getMnemonic() != null) menu.setMnemonic(config.getMnemonic());
        menu.setToolTipText(config.getTooltip());
        menu.setEnabled(config.isEnabled());
        if (config.getBuilder() != null) config.getBuilder().accept(menu);
        return menu;
    }

    public Menu menu(String id, String text) {
        return addMenu(id, text);
    }

    public MenuBar menu(String id, String text, Consumer<Menu> builder) {
        Menu menu = addMenu(id, text);
        if (builder != null) builder.accept(menu);
        return this;
    }

    public MenuBar menu(MenuConfig config) {
        addMenu(config);
        return this;
    }

    public MenuBar load(MenuSchema schema) {
        if (schema == null) return this;
        for (MenuConfig menu : schema.getMenus()) {
            addMenu(menu);
        }
        return this;
    }

    public MenuNode getMenuTree() {
        MenuNode root = MenuNode.root();
        for (Menu m : menus.values()) {
            root.add(captureMenuNode(m));
        }
        return root;
    }

    public MenuBar setMenuTree(MenuNode tree) {
        if (tree == null) return this;
        MenuNode root = tree.getType() == MenuNode.Type.ROOT
                ? tree
                : MenuNode.root().add(tree);
        clearMenus();
        Map<String, ButtonGroup> groups = new HashMap<>();
        for (MenuNode child : root.getChildren()) {
            if (child.getType() != MenuNode.Type.MENU) continue;
            Menu menu = applyMenuFromNode(child);
            populateMenuFromNode(menu, child, groups);
        }
        revalidate();
        repaint();
        return this;
    }

    public MenuBar updateMenuTree(Consumer<MenuNode> mutator) {
        MenuNode tree = getMenuTree();
        if (mutator != null) mutator.accept(tree);
        return setMenuTree(tree);
    }

    public MenuTreeEditor editMenuTree() {
        return new MenuTreeEditor(this);
    }

    private MenuNode captureMenuNode(Menu menu) {
        MenuNode node = MenuNode.menu(menu.getMenuId(), menu.getText())
                .icon(menu.getIcon())
                .tooltip(menu.getToolTipText())
                .enabled(menu.isEnabled());
        if (menu.getMnemonic() != 0) node.mnemonic(menu.getMnemonic());
        for (Component c : menu.getMenuComponents()) {
            MenuNode child = captureComponentNode(c);
            if (child != null) node.add(child);
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private MenuNode captureComponentNode(Component component) {
        if (component instanceof Menu sub) {
            return captureMenuNode(sub);
        }
        if (component instanceof Section section) {
            return MenuNode.section(section.getText());
        }
        if (component instanceof JSeparator) {
            return MenuNode.separator();
        }
        if (component instanceof JCheckBoxMenuItem cb) {
            MenuNode n = MenuNode.check(idOf(cb), cb.getText(), cb.isSelected())
                    .icon(cb.getIcon())
                    .tooltip(cb.getToolTipText())
                    .enabled(cb.isEnabled())
                    .accelerator(cb.getAccelerator());
            if (cb.getMnemonic() != 0) n.mnemonic(cb.getMnemonic());
            return n;
        }
        if (component instanceof JRadioButtonMenuItem rb) {
            MenuNode n = MenuNode.radio(idOf(rb), rb.getText(), null, rb.isSelected())
                    .icon(rb.getIcon())
                    .tooltip(rb.getToolTipText())
                    .enabled(rb.isEnabled())
                    .accelerator(rb.getAccelerator());
            if (rb.getMnemonic() != 0) n.mnemonic(rb.getMnemonic());
            return n;
        }
        if (component instanceof JMenuItem item) {
            MenuNode n = MenuNode.item(idOf(item), item.getText())
                    .icon(item.getIcon())
                    .tooltip(item.getToolTipText())
                    .enabled(item.isEnabled())
                    .accelerator(item.getAccelerator());
            if (item.getMnemonic() != 0) n.mnemonic(item.getMnemonic());
            Object actionProp = item.getClientProperty(ACTION_PROPERTY);
            if (actionProp instanceof Action swingAction) n.action(swingAction);
            Object handlerProp = item.getClientProperty(CLICK_HANDLER_PROPERTY);
            if (handlerProp instanceof Consumer) {
                n.onClick((Consumer<MenuBarEvent>) handlerProp);
            }
            Object enterProp = item.getClientProperty(MOUSE_ENTER_HANDLER_PROPERTY);
            if (enterProp instanceof Consumer) {
                n.onMouseEnter((Consumer<MenuBarEvent>) enterProp);
            }
            Object exitProp = item.getClientProperty(MOUSE_EXIT_HANDLER_PROPERTY);
            if (exitProp instanceof Consumer) {
                n.onMouseExit((Consumer<MenuBarEvent>) exitProp);
            }
            return n;
        }
        return null;
    }

    private String idOf(JMenuItem item) {
        if (item instanceof Item mi) return mi.getItemId();
        String name = item.getName();
        return name == null ? "" : name;
    }

    private Menu applyMenuFromNode(MenuNode node) {
        Menu menu = addMenu(node.getId(), node.getText());
        Icon icon = node.getIcon() != null ? node.getIcon() : config.resolveIcon(node.getIconId());
        if (icon != null) menu.setIcon(icon);
        if (node.getMnemonic() != null) menu.setMnemonic(node.getMnemonic());
        menu.setToolTipText(node.getTooltip());
        menu.setEnabled(node.isEnabled());
        return menu;
    }

    private void populateMenuFromNode(Menu menu, MenuNode parentNode, Map<String, ButtonGroup> groups) {
        for (MenuNode child : parentNode.getChildren()) {
            switch (child.getType()) {
                case MENU -> {
                    Menu sub = menu.addSubMenu(child.getId(), child.getText());
                    Icon icon = child.getIcon() != null ? child.getIcon() : config.resolveIcon(child.getIconId());
                    if (icon != null) sub.setIcon(icon);
                    if (child.getMnemonic() != null) sub.setMnemonic(child.getMnemonic());
                    sub.setToolTipText(child.getTooltip());
                    sub.setEnabled(child.isEnabled());
                    installHoverHandlers(sub, menu, child);
                    populateMenuFromNode(sub, child, groups);
                }
                case ITEM -> {
                    Icon icon = child.getIcon() != null ? child.getIcon() : config.resolveIcon(child.getIconId());
                    Item item = menu.addItem(child.getId(), child.getText(), icon, child.getAccelerator());
                    if (child.getMnemonic() != null) item.setMnemonic(child.getMnemonic());
                    item.setToolTipText(child.getTooltip());
                    item.setEnabled(child.isEnabled());
                    if (child.getAction() != null) bindAction(item, child.getAction());
                    if (child.getClickHandler() != null) item.putClientProperty(CLICK_HANDLER_PROPERTY, child.getClickHandler());
                    installHoverHandlers(item, menu, child);
                }
                case CHECK -> {
                    JCheckBoxMenuItem item = menu.addCheckItem(child.getId(), child.getText(), child.isSelected());
                    item.setEnabled(child.isEnabled());
                    item.setToolTipText(child.getTooltip());
                    if (child.getMnemonic() != null) item.setMnemonic(child.getMnemonic());
                    if (child.getAccelerator() != null) item.setAccelerator(child.getAccelerator());
                    Icon icon = child.getIcon() != null ? child.getIcon() : config.resolveIcon(child.getIconId());
                    if (icon != null) item.setIcon(icon);
                    installHoverHandlers(item, menu, child);
                }
                case RADIO -> {
                    ButtonGroup group = child.getRadioGroup() == null
                            ? null
                            : groups.computeIfAbsent(child.getRadioGroup(), k -> new ButtonGroup());
                    JRadioButtonMenuItem item = menu.addRadioItem(child.getId(), child.getText(), child.isSelected(), group);
                    item.setEnabled(child.isEnabled());
                    item.setToolTipText(child.getTooltip());
                    if (child.getMnemonic() != null) item.setMnemonic(child.getMnemonic());
                    if (child.getAccelerator() != null) item.setAccelerator(child.getAccelerator());
                    Icon icon = child.getIcon() != null ? child.getIcon() : config.resolveIcon(child.getIconId());
                    if (icon != null) item.setIcon(icon);
                    installHoverHandlers(item, menu, child);
                }
                case SEPARATOR -> menu.addSeparator();
                case SECTION -> {
                    Section section = new Section(child.getText());
                    menu.add(section);
                    applyMenuItemStyle(section);
                }
                default -> { }
            }
        }
    }

    public Menu getMenu(String id) {
        return menus.get(id);
    }

    public JMenuItem getItem(String path) {
        if (path == null || path.isBlank()) return null;
        String[] parts = path.split("\\.");
        if (parts.length == 0) return null;
        Menu menu = menus.get(parts[0]);
        if (menu == null) return null;
        if (parts.length == 1) return menu;
        return menu.findItem(parts, 1);
    }

    private static void bindAction(JMenuItem item, Action action) {
        Object previous = item.getClientProperty(ACTION_BINDING_PROPERTY);
        if (previous instanceof ActionEnabledBinding binding) {
            binding.detach();
            item.putClientProperty(ACTION_BINDING_PROPERTY, null);
        }
        item.putClientProperty(ACTION_PROPERTY, action);
        if (action == null) {
            return;
        }
        item.setEnabled(action.isEnabled());
        item.putClientProperty(ACTION_BINDING_PROPERTY, new ActionEnabledBinding(item, action));
    }

    private static final class ActionEnabledBinding implements PropertyChangeListener {

        private final WeakReference<JMenuItem> item;
        private final Action action;

        private ActionEnabledBinding(JMenuItem item, Action action) {
            this.item = new WeakReference<>(item);
            this.action = action;
            action.addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent event) {
            JMenuItem target = item.get();
            if (target == null) {
                detach();
                return;
            }
            if ("enabled".equals(event.getPropertyName())) {
                target.setEnabled(action.isEnabled());
            }
        }

        private void detach() {
            action.removePropertyChangeListener(this);
        }
    }

    public MenuBar setItemEnabled(String path, boolean enabled) {
        JMenuItem item = getItem(path);
        if (item != null) {
            Object action = item.getClientProperty(ACTION_PROPERTY);
            if (action instanceof Action swingAction) {
                swingAction.setEnabled(enabled);
            }
            item.setEnabled(enabled);
        }
        return this;
    }

    public MenuBar enable(String path) {
        return setItemEnabled(path, true);
    }

    public MenuBar disable(String path) {
        return setItemEnabled(path, false);
    }

    public MenuBar setItemText(String path, String text) {
        JMenuItem item = getItem(path);
        if (item != null) {
            item.setText(Objects.requireNonNullElse(text, ""));
            revalidate();
            repaint();
        }
        return this;
    }

    public MenuBar setItemIcon(String path, Icon icon) {
        JMenuItem item = getItem(path);
        if (item != null) item.setIcon(icon);
        return this;
    }

    public MenuBar setItemIcon(String path, String iconId) {
        return setItemIcon(path, config.resolveIcon(iconId));
    }

    public MenuBar setItemSelected(String path, boolean selected) {
        JMenuItem item = getItem(path);
        if (item instanceof AbstractButton button) {
            button.setSelected(selected);
        }
        return this;
    }

    public Map<String, Menu> getMenus() {
        return Map.copyOf(menus);
    }

    public MenuBar removeMenu(String id) {
        Menu menu = menus.remove(id);
        if (menu != null) {
            remove(menu);
            revalidate();
            repaint();
        }
        return this;
    }

    public MenuBar clearMenus() {
        for (Menu menu : new ArrayList<>(menus.values())) {
            remove(menu);
        }
        menus.clear();
        revalidate();
        repaint();
        return this;
    }

    public MenuBar addLeading(Component component) {
        return addPreMenu(component);
    }

    public MenuBar addPreMenu(Component component) {
        if (component == null) return this;
        styleSideComponent(component);
        leading.add(component);
        super.add(component, leading.size() - 1);
        revalidate();
        repaint();
        return this;
    }

    public MenuBar addLeadingStrut(int width) {
        return addPreMenuStrut(width);
    }

    public MenuBar addPreMenuStrut(int width) {
        return addPreMenu(Box.createHorizontalStrut(Math.max(0, width)));
    }

    public MenuBar removePreMenu(Component component) {
        if (component == null) return this;
        if (leading.remove(component)) {
            super.remove(component);
            if (component == brandIconLabel) brandIconLabel = null;
            revalidate();
            repaint();
        }
        return this;
    }

    public MenuBar clearPreMenu() {
        for (Component component : new ArrayList<>(leading)) {
            super.remove(component);
        }
        leading.clear();
        brandIconLabel = null;
        revalidate();
        repaint();
        return this;
    }

    public List<Component> getPreMenuComponents() {
        return List.copyOf(leading);
    }

    public MenuBar addTrailing(Component component) {
        if (component == null) return this;
        styleSideComponent(component);
        trailing.add(component);
        super.add(component);
        revalidate();
        repaint();
        return this;
    }

    public MenuBar addTrailingStrut(int width) {
        return addTrailing(Box.createHorizontalStrut(Math.max(0, width)));
    }

    public MenuBar setBrandIcon(Image image) {
        return setBrandIconLabelIcon(image == null ? null : new ImageIcon(scaleToBrandSize(image)));
    }

    public MenuBar setBrandIcon(Icon icon) {
        return setBrandIconLabelIcon(icon);
    }

    public MenuBar setBrandIcon(String iconId) {
        return setBrandIconLabelIcon(config.resolveIcon(iconId));
    }

    public MenuBar useDefaultBrandIcon() {
        return setBrandIcon(buildDefaultBrandIcon());
    }

    public MenuBar removeBrandIcon() {
        if (brandIconLabel != null) {
            leading.remove(brandIconLabel);
            super.remove(brandIconLabel);
            brandIconLabel = null;
            revalidate();
            repaint();
        }
        return this;
    }

    public MenuBar setBrandIconSize(int size) {
        this.brandIconSize = Math.max(8, size);
        if (brandIconLabel != null && brandIconLabel.getIcon() instanceof ImageIcon imageIcon) {
            brandIconLabel.setIcon(new ImageIcon(scaleToBrandSize(imageIcon.getImage())));
            revalidate();
            repaint();
        }
        return this;
    }

    public int getBrandIconSize() {
        return brandIconSize;
    }

    public JLabel getBrandIconLabel() {
        return brandIconLabel;
    }

    protected Image buildDefaultBrandIcon() {
        int size = Math.max(brandIconSize, 32);
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setColor(resolveAccent());
            g.fill(new Ellipse2D.Double(2, 2, size - 4, size - 4));
            g.setColor(luminance(resolveAccent()) < 0.55 ? Color.WHITE : new Color(0x111827));
            Font font = resolveMenuFont().deriveFont(Font.BOLD, size * 0.55f);
            g.setFont(font);
            String letter = "S";
            FontMetrics fm = g.getFontMetrics();
            int x = (size - fm.stringWidth(letter)) / 2;
            int y = (size - fm.getHeight()) / 2 + fm.getAscent();
            g.drawString(letter, x, y);
        } finally {
            g.dispose();
        }
        return img;
    }

    private Image scaleToBrandSize(Image image) {
        if (image == null || brandIconSize <= 0) return image;
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w == brandIconSize && h == brandIconSize) return image;
        return image.getScaledInstance(brandIconSize, brandIconSize, Image.SCALE_SMOOTH);
    }

    private MenuBar setBrandIconLabelIcon(Icon icon) {
        if (icon == null) return removeBrandIcon();
        if (brandIconLabel == null) {
            brandIconLabel = new JLabel(icon);
            brandIconLabel.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 8));
            styleSideComponent(brandIconLabel);
            leading.add(0, brandIconLabel);
            super.add(brandIconLabel, 0);
        } else {
            brandIconLabel.setIcon(icon);
        }
        revalidate();
        repaint();
        return this;
    }

    public JLabel addBrand(String text) {
        JLabel label = new JLabel(Objects.requireNonNullElse(text, ""));
        label.setFont(resolveBrandFont());
        label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 10));
        label.setForeground(resolveAccent());
        addLeading(label);
        return label;
    }

    public GradientLabel addBrand(String text, Color start, Color end) {
        GradientLabel label = new GradientLabel(Objects.requireNonNullElse(text, ""), start, end);
        label.setFont(resolveBrandFont());
        label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 10));
        addLeading(label);
        return label;
    }

    public GradientLabel addGradientLabel(String text, Color start, Color end) {
        GradientLabel label = new GradientLabel(Objects.requireNonNullElse(text, ""), start, end);
        label.setFont(resolveMenuFont().deriveFont(Font.BOLD));
        label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
        addTrailing(label);
        return label;
    }

    public MenuBar setStyle(MenuBarStyle style) {
        enableCustomDecorations();
        this.style = style == null ? MenuBarStyle.modern() : style;
        this.config.style(this.style);
        applyStyle();
        return this;
    }

    public EventSubscription onMenuOpen(Consumer<MenuBarEvent> listener) {
        return onMenuEvent(MENU_OPEN, listener);
    }

    public EventSubscription onMenuClose(Consumer<MenuBarEvent> listener) {
        return onMenuEvent(MENU_CLOSE, listener);
    }

    public EventSubscription onItemClick(Consumer<MenuBarEvent> listener) {
        return onMenuEvent(ITEM_CLICK, listener);
    }

    public EventSubscription onBeforeItemClick(Consumer<MenuBarEvent> listener) {
        return onMenuEvent(BEFORE_ITEM_CLICK, listener);
    }

    public EventSubscription onSelect(Consumer<MenuBarEvent> listener) {
        return onMenuEvent(EventType.SELECT, listener);
    }

    public EventSubscription onMenuEvent(String eventType, Consumer<MenuBarEvent> listener) {
        return addEventListener(eventType, MenuBarEvent.class, listener);
    }

    public MenuBar setBarBackground(Color color) {
        enableCustomDecorations();
        style.setBackground(color);
        applyStyle();
        return this;
    }

    public MenuBar setBarForeground(Color color) {
        enableCustomDecorations();
        style.setForeground(color);
        applyStyle();
        return this;
    }

    public MenuBar setAccentColor(Color color) {
        enableCustomDecorations();
        style.setAccent(color);
        applyStyle();
        return this;
    }

    public MenuBar setMenuPadding(Insets padding) {
        enableCustomDecorations();
        style.setMenuPadding(padding);
        applyStyle();
        return this;
    }

    public MenuBar setBackgroundGradient(Color start, Color end) {
        return setBackgroundGradient(start, end, style.getBackgroundGradientAngle());
    }

    private void enableCustomDecorations() {
        config.customPaintingEnabled(true)
                .popupDecorated(true)
                .handCursorEnabled(true)
                .sideComponentsTransparent(true);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (style != null) {
            SwingUtilities.invokeLater(this::applyStyle);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!config.isCustomPaintingEnabled()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = resolveBackground();
            Color bgEnd = style.getBackgroundGradientEnd();
            float[] stops = style.getBackgroundGradientStops();
            Color[] stopColors = style.getBackgroundGradientColors();
            int w = getWidth();
            int h = getHeight();
            if (stops != null && stopColors != null && stops.length >= 2 && stops.length == stopColors.length) {
                double rad = Math.toRadians(style.getBackgroundGradientAngle());
                double cx = w / 2.0, cy = h / 2.0;
                double dx = Math.cos(rad), dy = Math.sin(rad);
                double half = (Math.abs(dx) * w + Math.abs(dy) * h) / 2.0;
                float x1 = (float) (cx - dx * half);
                float y1 = (float) (cy - dy * half);
                float x2 = (float) (cx + dx * half);
                float y2 = (float) (cy + dy * half);
                if (x1 == x2 && y1 == y2) {
                    x2 = x1 + 1f;
                }
                g2.setPaint(new LinearGradientPaint(x1, y1, x2, y2, stops, stopColors));
                g2.fillRect(0, 0, w, h);
            } else if (bgEnd != null) {
                double rad = Math.toRadians(style.getBackgroundGradientAngle());
                double cx = w / 2.0, cy = h / 2.0;
                double dx = Math.cos(rad), dy = Math.sin(rad);
                double half = (Math.abs(dx) * w + Math.abs(dy) * h) / 2.0;
                float x1 = (float) (cx - dx * half);
                float y1 = (float) (cy - dy * half);
                float x2 = (float) (cx + dx * half);
                float y2 = (float) (cy + dy * half);
                g2.setPaint(new GradientPaint(x1, y1, bg, x2, y2, bgEnd));
                g2.fillRect(0, 0, w, h);
            } else if (style.getBackground() != null) {
                g2.setColor(bg);
                g2.fillRect(0, 0, w, h);
            }

            if (style.isDrawBottomBorder()) {
                g2.setColor(resolveBorderColor());
                g2.fillRect(0, h - 1, w, 1);
            }
        } finally {
            g2.dispose();
        }
    }

    private int indexOfGlue() {
        for (int i = 0; i < getComponentCount(); i++) {
            if (getComponent(i) == glue) return i;
        }
        return getComponentCount();
    }

    private void styleSideComponent(Component component) {
        if (component instanceof JComponent jc) {
            if (config.isSideComponentsTransparent()) {
                jc.setOpaque(false);
            }
            if (jc.getForeground() == null) jc.setForeground(resolveForeground());
        }
    }

    protected Menu createMenu(String id, String text) {
        return new Menu(this, id, text);
    }

    protected Item createMenuItem(Menu menu, String id, String text, Icon icon, KeyStroke accelerator) {
        return new Item(this, menu, id, text, icon, accelerator);
    }

    protected JCheckBoxMenuItem createCheckItem(Menu menu, String id, String text, boolean selected) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(text, selected);
        item.setName(id);
        item.addActionListener(e -> dispatchItemClick(menu, item, id, item.isSelected()));
        return item;
    }

    protected JRadioButtonMenuItem createRadioItem(Menu menu, String id, String text, boolean selected) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(text, selected);
        item.setName(id);
        item.addActionListener(e -> dispatchItemClick(menu, item, id, item.isSelected()));
        return item;
    }

    protected void applyStyle() {
        if (config.isCustomPaintingEnabled()) {
            setOpaque(isBarBackgroundOpaque());
            setBorder(BorderFactory.createEmptyBorder(
                    style.getBarPadding().top,
                    style.getBarPadding().left,
                    style.getBarPadding().bottom,
                    style.getBarPadding().right
            ));
        } else {
            setOpaque(true);
            Border border = UIManager.getBorder("MenuBar.border");
            setBorder(border);
        }

        for (Menu menu : menus.values()) {
            applyMenuStyle(menu);
        }
        for (Component c : leading) styleSideComponent(c);
        for (Component c : trailing) styleSideComponent(c);
        repaint();
    }

    private boolean isBarBackgroundOpaque() {
        Color[] stopColors = style.getBackgroundGradientColors();
        float[] stops = style.getBackgroundGradientStops();
        if (stops != null && stopColors != null && stops.length >= 2 && stops.length == stopColors.length) {
            for (Color color : stopColors) {
                if (color == null || color.getAlpha() < 255) return false;
            }
            return true;
        }
        Color bgEnd = style.getBackgroundGradientEnd();
        if (bgEnd != null) {
            Color background = resolveBackground();
            return background != null && background.getAlpha() == 255 && bgEnd.getAlpha() == 255;
        }
        if (style.getBackground() != null) {
            return style.getBackground().getAlpha() == 255;
        }
        return false;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        if (style != null && style.getBarHeight() > 0) {
            d.height = Math.max(d.height, style.getBarHeight());
        }
        return d;
    }

    protected void applyMenuStyle(JMenu menu) {
        boolean custom = config.isCustomPaintingEnabled();
        menu.setOpaque(!custom);
        menu.setForeground(resolveForeground());
        menu.setFont(resolveMenuFont());
        if (menu instanceof Menu modernMenu) {
            modernMenu.setContentAreaFilled(!custom);
            modernMenu.setBorderPainted(!custom);
        }
        if (custom) {
            menu.setBorder(BorderFactory.createEmptyBorder(
                    style.getMenuPadding().top,
                    style.getMenuPadding().left,
                    style.getMenuPadding().bottom,
                    style.getMenuPadding().right
            ));
        } else {
            menu.setBorder(UIManager.getBorder("Menu.border"));
        }
        if (config.isHandCursorEnabled()) {
            menu.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            menu.setCursor(Cursor.getDefaultCursor());
        }

        JPopupMenu popup = menu.getPopupMenu();
        if (custom && config.isPopupDecorated()) {
            popup.setBorder(new RoundedPopupBorder(
                    style.getPopupArc(),
                    resolvePopupBorderColor(),
                    style.getPopupPadding()
            ));
        } else {
            popup.setBorder(UIManager.getBorder("PopupMenu.border"));
        }
        popup.setBackground(resolvePopupBackground());

        for (int i = 0; i < menu.getItemCount(); i++) {
            JMenuItem item = menu.getItem(i);
            if (item != null) {
                applyMenuItemStyle(item);
            }
        }
    }

    protected void applyMenuItemStyle(JMenuItem item) {
        if (item instanceof JMenu submenu) {
            applyMenuStyle(submenu);
            return;
        }
        boolean custom = config.isCustomPaintingEnabled();
        item.setOpaque(!custom);
        item.setBackground(resolvePopupBackground());
        item.setForeground(resolveForeground());
        item.setFont(resolveItemFont());
        item.setContentAreaFilled(!custom);
        item.setBorderPainted(!custom);
        if (custom) {
            item.setBorder(BorderFactory.createEmptyBorder(
                    style.getItemPadding().top,
                    style.getItemPadding().left,
                    style.getItemPadding().bottom,
                    style.getItemPadding().right
            ));
            item.setPreferredSize(new Dimension(
                    Math.max(item.getPreferredSize().width, style.getMinItemWidth()),
                    Math.max(item.getPreferredSize().height, style.getItemHeight())
            ));
        } else {
            item.setBorder(UIManager.getBorder("MenuItem.border"));
            item.setPreferredSize(null);
        }
        if (config.isHandCursorEnabled()) {
            item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            item.setCursor(Cursor.getDefaultCursor());
        }
    }

    protected void dispatchMenuEvent(String type, Menu menu) {
        Map<String, Object> props = new HashMap<>();
        props.put("menuId", menu.getMenuId());
        props.put("menuText", menu.getText());
        dispatchEvent(type, menu, menu, menu, null, props);
    }

    protected void dispatchItemClick(Menu menu, JMenuItem item, String itemId, Object value) {
        Map<String, Object> props = new HashMap<>();
        props.put("menuId", menu.getMenuId());
        props.put("menuText", menu.getText());
        props.put("itemId", itemId);
        props.put("itemText", item.getText());
        props.put("selected", value instanceof Boolean selected && selected);

        Object eventValue = value == null ? item : value;
        MenuBarEvent before = dispatchEvent(BEFORE_ITEM_CLICK, item, eventValue, menu, item, props, true);
        if (before.isCanceled()) return;

        runItemHandler(item, before);
        dispatchEvent(ITEM_CLICK, item, eventValue, menu, item, props);
        dispatchEvent(EventType.SELECT, item, eventValue, menu, item, props);
    }

    protected void dispachEvent(String eventType, Component component, Object value, Map<String, Object> props) {
        dispatchEvent(eventType, component, value, null, component instanceof JMenuItem item ? item : null, props);
    }

    protected MenuBarEvent dispatchEvent(String eventType, Component component, Object value, Map<String, Object> props) {
        return dispatchEvent(eventType, component, value, null, component instanceof JMenuItem item ? item : null, props);
    }

    protected MenuBarEvent dispatchEvent(String eventType,
                                         Component component,
                                         Object value,
                                         Menu menu,
                                         JMenuItem item,
                                         Map<String, Object> props) {
        return dispatchEvent(eventType, component, value, menu, item, props, false);
    }

    protected MenuBarEvent dispatchEvent(String eventType,
                                         Component component,
                                         Object value,
                                         Menu menu,
                                         JMenuItem item,
                                         Map<String, Object> props,
                                         boolean cancelable) {
        MenuBarEvent event = new MenuBarEvent(this, component, menu, item, value, eventType, props, cancelable);
        if (listeners.isEmpty()) return event;

        List<Consumer<EventComponent>> eventListeners = listeners.get(eventType);
        if (eventListeners == null || eventListeners.isEmpty()) return event;

        eventListeners.forEach(listener -> {
            try { listener.accept(event); } catch (Exception e) { e.printStackTrace(); }
        });
        return event;
    }

    @SuppressWarnings("unchecked")
    private void runItemHandler(JMenuItem item, MenuBarEvent event) {
        Object handler = item.getClientProperty(CLICK_HANDLER_PROPERTY);
        if (handler instanceof Consumer<?>) {
            ((Consumer<MenuBarEvent>) handler).accept(event);
        }

        Object action = item.getClientProperty(ACTION_PROPERTY);
        if (action instanceof Action swingAction) {
            swingAction.actionPerformed(new ActionEvent(item, ActionEvent.ACTION_PERFORMED, event.getItemId()));
        }
    }

    private void installHoverHandlers(JMenuItem item, Menu menu, MenuNode node) {
        Consumer<MenuBarEvent> enter = node.getMouseEnterHandler();
        Consumer<MenuBarEvent> exit = node.getMouseExitHandler();
        if (enter == null && exit == null) return;

        if (enter != null) item.putClientProperty(MOUSE_ENTER_HANDLER_PROPERTY, enter);
        if (exit != null) item.putClientProperty(MOUSE_EXIT_HANDLER_PROPERTY, exit);

        item.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                fireMouseEnterIfInactive(item, menu);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                fireMouseExitIfActive(item, menu);
            }
        });

        if (item instanceof JMenu jMenu) {
            jMenu.addMenuListener(new MenuListener() {
                @Override
                public void menuSelected(MenuEvent e) {
                    fireMouseEnterIfInactive(item, menu);
                }

                @Override
                public void menuDeselected(MenuEvent e) {
                    fireMouseExitIfActive(item, menu);
                }

                @Override
                public void menuCanceled(MenuEvent e) {
                    fireMouseExitIfActive(item, menu);
                }
            });
        }
    }

    private void fireMouseEnterIfInactive(JMenuItem item, Menu menu) {
        if (Boolean.TRUE.equals(item.getClientProperty(HOVER_ACTIVE_PROPERTY))) return;
        item.putClientProperty(HOVER_ACTIVE_PROPERTY, Boolean.TRUE);
        fireHoverEvent(item, menu, ITEM_MOUSE_ENTER, MOUSE_ENTER_HANDLER_PROPERTY);
    }

    private void fireMouseExitIfActive(JMenuItem item, Menu menu) {
        if (!Boolean.TRUE.equals(item.getClientProperty(HOVER_ACTIVE_PROPERTY))) return;
        item.putClientProperty(HOVER_ACTIVE_PROPERTY, Boolean.FALSE);
        fireHoverEvent(item, menu, ITEM_MOUSE_EXIT, MOUSE_EXIT_HANDLER_PROPERTY);
    }

    @SuppressWarnings("unchecked")
    private void fireHoverEvent(JMenuItem item, Menu menu, String eventType, String handlerProperty) {
        Map<String, Object> props = new HashMap<>();
        props.put("menuId", menu.getMenuId());
        props.put("menuText", menu.getText());
        props.put("itemId", idOf(item));
        props.put("itemText", item.getText());

        MenuBarEvent event = dispatchEvent(eventType, item, item, menu, item, props);

        Object handler = item.getClientProperty(handlerProperty);
        if (handler instanceof Consumer<?>) {
            try { ((Consumer<MenuBarEvent>) handler).accept(event); } catch (Exception ex) { ex.printStackTrace(); }
        }
    }

    private Icon resolveConfigIcon(Icon icon, String iconId) {
        return icon != null ? icon : config.resolveIcon(iconId);
    }

    @Override
    public EventSubscription addEventListener(String eventType, Consumer<EventComponent> event) {
        if (eventType == null || eventType.isBlank() || event == null) return () -> {};
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        return () -> removeEventListner(eventType, event);
    }

    @Override
    public void removeEventListner(String eventType, Consumer<EventComponent> event) {
        List<Consumer<EventComponent>> eventListeners = listeners.get(eventType);
        if (eventListeners != null) eventListeners.remove(event);
    }

    @Override
    public void removeEventListner(String eventType) {
        listeners.remove(eventType);
    }

    @Override
    public Map<String, List<Consumer<EventComponent>>> getEventListners() {
        return new ConcurrentHashMap<>(listeners);
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
    }

    protected Color resolveBackground() {
        if (style.getBackground() != null) return style.getBackground();
        Color color = UIManager.getColor("MenuBar.background");
        if (color != null) return color;
        color = UIManager.getColor("Panel.background");
        return color != null ? color : getBackground();
    }

    protected Color resolvePopupBackground() {
        if (style.getPopupBackground() != null) return style.getPopupBackground();
        Color color = UIManager.getColor("PopupMenu.background");
        return color != null ? color : resolveBackground();
    }

    protected Color resolveForeground() {
        if (style.getForeground() != null) return style.getForeground();
        Color color = UIManager.getColor("MenuBar.foreground");
        if (color != null) return color;
        color = UIManager.getColor("Menu.foreground");
        if (color != null) return color;
        color = UIManager.getColor("Label.foreground");
        return color != null ? color : Color.BLACK;
    }

    protected Color resolveHoverForeground() {
        if (style.getHoverForeground() != null) return style.getHoverForeground();
        Color color = UIManager.getColor("Menu.selectionForeground");
        if (color != null) return color;
        return resolveForeground();
    }

    protected Color resolveSelectedForeground() {
        if (style.getSelectedForeground() != null) return style.getSelectedForeground();
        Color color = UIManager.getColor("MenuItem.selectionForeground");
        if (color != null) return color;
        Color accent = resolveAccent();
        return luminance(accent) < 0.55 ? Color.WHITE : new Color(0x111827);
    }

    protected Color resolveBorderColor() {
        if (style.getBorderColor() != null) return style.getBorderColor();
        Color color = UIManager.getColor("Separator.foreground");
        if (color != null) return color;
        return blend(resolveForeground(), resolveBackground(), 0.82f);
    }

    protected Color resolvePopupBorderColor() {
        if (style.getPopupBorderColor() != null) return style.getPopupBorderColor();
        return resolveBorderColor();
    }

    protected Color resolveHoverBackground() {
        if (style.getHoverBackground() != null) return style.getHoverBackground();
        Color color = UIManager.getColor("Menu.selectionBackground");
        if (color != null) return color;
        return blend(resolveForeground(), resolveBackground(), 0.90f);
    }

    protected Color resolveSelectedBackground() {
        if (style.getSelectedBackground() != null) return style.getSelectedBackground();
        Color color = UIManager.getColor("MenuItem.selectionBackground");
        if (color != null) return color;
        return resolveAccent();
    }

    protected Color resolveAccent() {
        if (style.getAccent() != null) return style.getAccent();
        Color color = UIManager.getColor("Component.accentColor");
        if (color != null) return color;
        color = UIManager.getColor("TextField.selectionBackground");
        return color != null ? color : new Color(0x3574F0);
    }

    protected Color resolveSeparatorColor() {
        if (style.getSeparatorColor() != null) return style.getSeparatorColor();
        return blend(resolveForeground(), resolvePopupBackground(), 0.85f);
    }

    protected Font resolveMenuFont() {
        return style.getMenuFont() != null ? style.getMenuFont() : getFont();
    }

    protected Font resolveItemFont() {
        return style.getItemFont() != null ? style.getItemFont() : getFont();
    }

    protected Font resolveBrandFont() {
        Font base = resolveMenuFont();
        return base.deriveFont(Font.BOLD, base.getSize() + 1f);
    }

    protected Color blend(Color from, Color to, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        int a = Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped);
        return new Color(r, g, b, a);
    }

    protected double luminance(Color c) {
        return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
    }

    public static class Menu extends JMenu {
        private final MenuBar owner;
        private final String menuId;
        private final Map<String, JMenuItem> items = new LinkedHashMap<>();
        private boolean hovered;

        protected Menu(MenuBar owner, String id, String text) {
            super(Objects.requireNonNullElse(text, ""));
            this.owner = owner;
            this.menuId = Objects.requireNonNullElse(id, "").trim();
            setName(menuId);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            installMenuEvents();
        }

        public String getMenuId() {
            return menuId;
        }

        public MenuBar end() {
            return owner;
        }

        public Item addItem(String id, String text) {
            return addItem(id, text, null, null);
        }

        public Item addItem(String id, String text, KeyStroke accelerator) {
            return addItem(id, text, null, accelerator);
        }

        public Item addItem(String id, String text, Icon icon, KeyStroke accelerator) {
            Item item = owner.createMenuItem(this, id, text, icon, accelerator);
            items.put(item.getItemId(), item);
            add(item);
            owner.applyMenuItemStyle(item);
            return item;
        }

        public Item addItem(MenuItemConfig config) {
            Objects.requireNonNull(config, "config");
            Item item = addItem(
                    config.getId(),
                    config.getText(),
                    owner.resolveConfigIcon(config.getIcon(), config.getIconId()),
                    config.getAccelerator()
            );
            if (config.getMnemonic() != null) item.setMnemonic(config.getMnemonic());
            item.setToolTipText(config.getTooltip());
            item.setEnabled(config.isEnabled());
            if (config.getAction() != null) {
                bindAction(item, config.getAction());
            }
            if (config.getClickHandler() != null) {
                item.putClientProperty(CLICK_HANDLER_PROPERTY, config.getClickHandler());
            }
            if (config.getConfigurer() != null) config.getConfigurer().accept(item);
            return item;
        }

        public Item addAction(Action action) {
            return addItem(MenuItemConfig.fromAction(action));
        }

        public Menu addItem(String id, String text, Consumer<Item> config) {
            Item item = addItem(id, text);
            if (config != null) config.accept(item);
            return this;
        }

        public Menu item(MenuItemConfig config) {
            addItem(config);
            return this;
        }

        public Menu item(Action action) {
            addAction(action);
            return this;
        }

        public JCheckBoxMenuItem addCheckItem(String id, String text, boolean selected) {
            JCheckBoxMenuItem item = owner.createCheckItem(this, id, text, selected);
            items.put(id, item);
            add(item);
            owner.applyMenuItemStyle(item);
            return item;
        }

        public JRadioButtonMenuItem addRadioItem(String id, String text, boolean selected, ButtonGroup group) {
            JRadioButtonMenuItem item = owner.createRadioItem(this, id, text, selected);
            if (group != null) group.add(item);
            items.put(id, item);
            add(item);
            owner.applyMenuItemStyle(item);
            return item;
        }

        public Menu addSeparatorLine() {
            addSeparator();
            return this;
        }

        public Menu addSection(String text) {
            if (getMenuComponentCount() > 0) {
                addSeparator();
            }
            Section section = new Section(text);
            add(section);
            owner.applyMenuItemStyle(section);
            return this;
        }

        public Menu section(String text) {
            return addSection(text);
        }

        public Menu addSubMenu(String id, String text) {
            Menu sub = owner.createMenu(id, text);
            items.put(sub.getMenuId(), sub);
            add(sub);
            owner.applyMenuStyle(sub);
            return sub;
        }

        public Menu addSubMenu(MenuConfig config) {
            Objects.requireNonNull(config, "config");
            Menu sub = addSubMenu(config.getId(), config.getText());
            sub.setIcon(owner.resolveConfigIcon(config.getIcon(), config.getIconId()));
            if (config.getMnemonic() != null) sub.setMnemonic(config.getMnemonic());
            sub.setToolTipText(config.getTooltip());
            sub.setEnabled(config.isEnabled());
            if (config.getBuilder() != null) config.getBuilder().accept(sub);
            return sub;
        }

        public Menu addSubMenu(String id, String text, Consumer<Menu> builder) {
            Menu sub = addSubMenu(id, text);
            if (builder != null) builder.accept(sub);
            return this;
        }

        public Menu subMenu(String id, String text, Consumer<Menu> builder) {
            return addSubMenu(id, text, builder);
        }

        public Menu addSubMenu(Menu menu) {
            if (menu == null) return this;
            items.put(menu.getMenuId(), menu);
            add(menu);
            owner.applyMenuStyle(menu);
            return this;
        }

        public JMenuItem getItem(String id) {
            return items.get(id);
        }

        private JMenuItem findItem(String[] parts, int index) {
            if (parts == null || index >= parts.length) return this;
            JMenuItem item = items.get(parts[index]);
            if (item instanceof Menu submenu && index + 1 < parts.length) {
                return submenu.findItem(parts, index + 1);
            }
            return index + 1 == parts.length ? item : null;
        }

        public Map<String, JMenuItem> getItems() {
            return Map.copyOf(items);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!owner.config.isCustomPaintingEnabled()) {
                super.paintComponent(g);
                return;
            }
            boolean active = hovered || isSelected() || isPopupMenuVisible();
            if (active) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int arc = owner.style.getHoverArc();
                    boolean open = isSelected() || isPopupMenuVisible();
                    Color bg = open ? owner.resolveHoverBackground() : owner.blend(owner.resolveHoverBackground(), owner.resolveBackground(), 0.55f);
                    g2.setColor(bg);
                    g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), arc, arc));

                    if (open && owner.style.isAccentIndicator()) {
                        int h = Math.max(1, owner.style.getAccentIndicatorHeight());
                        int pad = Math.max(arc / 2, 6);
                        g2.setColor(owner.resolveAccent());
                        g2.fillRoundRect(pad, getHeight() - h, getWidth() - pad * 2, h, h, h);
                    }
                } finally {
                    g2.dispose();
                }
            }
            Color original = getForeground();
            try {
                if (isSelected() || isPopupMenuVisible()) {
                    setForeground(owner.resolveHoverForeground());
                } else if (hovered) {
                    setForeground(owner.resolveHoverForeground());
                }
                super.paintComponent(g);
            } finally {
                setForeground(original);
            }
        }

        private void installMenuEvents() {
            addMenuListener(new MenuListener() {
                @Override public void menuSelected(MenuEvent e) {
                    owner.dispatchMenuEvent(MENU_OPEN, Menu.this);
                    repaint();
                }
                @Override public void menuDeselected(MenuEvent e) {
                    owner.dispatchMenuEvent(MENU_CLOSE, Menu.this);
                    repaint();
                }
                @Override public void menuCanceled(MenuEvent e) {
                    owner.dispatchMenuEvent(MENU_CLOSE, Menu.this);
                    repaint();
                }
            });

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }
            });
        }
    }

    public static class Item extends JMenuItem {
        private final MenuBar owner;
        @Getter
        private final Menu menu;
        @Getter
        private final String itemId;
        private boolean hovered;

        protected Item(MenuBar owner, Menu menu, String id, String text, Icon icon, KeyStroke accelerator) {
            super(Objects.requireNonNullElse(text, ""), icon);
            this.owner = owner;
            this.menu = menu;
            this.itemId = Objects.requireNonNullElse(id, "").trim();
            setName(itemId);
            setAccelerator(accelerator);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            addActionListener(e -> owner.dispatchItemClick(menu, this, itemId, this));
            installHover();
        }

        public Item setShortcut(KeyStroke shortcut) {
            setAccelerator(shortcut);
            return this;
        }

        public Item setShortcut(String shortcut) {
            return setShortcut(KeyStroke.getKeyStroke(shortcut));
        }

        public Item setHint(String tooltip) {
            setToolTipText(tooltip);
            return this;
        }

        public Item onClick(Consumer<MenuBarEvent> handler) {
            putClientProperty(CLICK_HANDLER_PROPERTY, handler);
            return this;
        }

        public Item action(Action action) {
            bindAction(this, action);
            if (action == null) {
                setEnabled(true);
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (!owner.config.isCustomPaintingEnabled()) {
                super.paintComponent(g);
                return;
            }
            boolean active = hovered || isArmed();
            Color fg = owner.resolveForeground();
            if (active) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int arc = owner.style.getItemArc();
                    g2.setColor(owner.resolveSelectedBackground());
                    g2.fill(new RoundRectangle2D.Float(2, 1, getWidth() - 4, getHeight() - 2, arc, arc));
                    fg = owner.resolveSelectedForeground();
                } finally {
                    g2.dispose();
                }
            }
            Color original = getForeground();
            try {
                setForeground(fg);
                super.paintComponent(g);
            } finally {
                setForeground(original);
            }
        }

        private void installHover() {
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }
    }

    protected static class Section extends JMenuItem {
        public Section(String text) {
            super(Objects.requireNonNullElse(text, ""));
            setEnabled(false);
            setFocusable(false);
            setOpaque(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
        }
    }

    public static class GradientLabel extends JLabel {
        private Color startColor;
        private Color endColor;

        public GradientLabel(String text, Color start, Color end) {
            super(Objects.requireNonNullElse(text, ""));
            this.startColor = Objects.requireNonNullElse(start, new Color(0x3574F0));
            this.endColor = Objects.requireNonNullElse(end, new Color(0xB94EFF));
            setOpaque(false);
        }

        public GradientLabel setColors(Color start, Color end) {
            if (start != null) this.startColor = start;
            if (end != null) this.endColor = end;
            repaint();
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            String text = getText();
            if (text == null || text.isEmpty()) {
                super.paintComponent(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(getFont());

                FontMetrics fm = g2.getFontMetrics();
                Insets in = getInsets();
                int textWidth = fm.stringWidth(text);
                int x = in.left;
                int y = in.top + fm.getAscent() + (getHeight() - in.top - in.bottom - fm.getHeight()) / 2;

                int alignment = getHorizontalAlignment();
                int available = getWidth() - in.left - in.right;
                if (alignment == CENTER) x = in.left + Math.max(0, (available - textWidth) / 2);
                else if (alignment == RIGHT || alignment == TRAILING) x = getWidth() - in.right - textWidth;

                g2.setPaint(new GradientPaint(x, 0, startColor, x + textWidth, 0, endColor));
                g2.drawString(text, x, y);
            } finally {
                g2.dispose();
            }
        }
    }

    protected static class RoundedPopupBorder extends AbstractBorder {
        private final int arc;
        private final Color color;
        private final Insets padding;

        public RoundedPopupBorder(int arc, Color color, Insets padding) {
            this.arc = Math.max(0, arc);
            this.color = color != null ? color : new Color(0, 0, 0, 60);
            this.padding = padding != null ? padding : new Insets(6, 6, 6, 6);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (c instanceof JComponent jc) {
                    Color bg = jc.getBackground();
                    if (bg != null) {
                        g2.setColor(bg);
                        g2.fill(new RoundRectangle2D.Float(x, y, width - 1, height - 1, arc, arc));
                    }
                }
                g2.setColor(color);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1.5f, height - 1.5f, arc, arc));
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(padding.top, padding.left, padding.bottom, padding.right);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = padding.top;
            insets.left = padding.left;
            insets.bottom = padding.bottom;
            insets.right = padding.right;
            return insets;
        }
    }
}
