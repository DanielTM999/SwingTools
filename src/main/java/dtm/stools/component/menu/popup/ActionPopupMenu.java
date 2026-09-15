package dtm.stools.component.menu.popup;

import dtm.stools.component.menu.popup.style.ActionMenuStyle;
import dtm.stools.component.menu.popup.style.BorderFactorySupplier;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ActionPopupMenu extends JPopupMenu implements ActionMenuSupport<ActionPopupMenu> {

    private Integer preferredPopupWidth;
    private Integer preferredPopupHeight;
    private PopupSession popupSession;

    @Getter
    private ActionMenuStyle actionMenuStyle = new ActionMenuStyle();

    @Getter
    private boolean useRootStyleForChildren = false;

    public ActionPopupMenu() {
        super();
    }

    public ActionPopupMenu(String label) {
        super(label);
    }

    public static ActionPopupMenu create() {
        return new ActionPopupMenu();
    }

    public static ActionPopupMenu create(String label) {
        return new ActionPopupMenu(label);
    }

    public ActionPopupMenu useRootStyleForChildren(boolean useRootStyleForChildren) {
        this.useRootStyleForChildren = useRootStyleForChildren;
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu enableRootStyleForChildren() {
        return useRootStyleForChildren(true);
    }

    public ActionPopupMenu disableRootStyleForChildren() {
        return useRootStyleForChildren(false);
    }

    public ActionPopupMenu style(ActionMenuStyle style) {
        this.actionMenuStyle = style != null ? style : new ActionMenuStyle();
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu background(Color color) {
        this.actionMenuStyle.background(color);
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu foreground(Color color) {
        this.actionMenuStyle.foreground(color);
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu selectionBackground(Color color) {
        this.actionMenuStyle.selectionBackground(color);
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu selectionForeground(Color color) {
        this.actionMenuStyle.selectionForeground(color);
        applyStyleToTree();
        return this;
    }

    public ActionPopupMenu popupSize(int width, int height) {
        if (width > 0) {
            this.preferredPopupWidth = width;
        }

        if (height > 0) {
            this.preferredPopupHeight = height;
        }

        revalidate();
        repaint();

        return this;
    }

    public ActionPopupMenu preferredPopupWidth(int width) {
        if (width > 0) {
            this.preferredPopupWidth = width;
        }

        revalidate();
        repaint();

        return this;
    }

    public ActionPopupMenu preferredPopupHeight(int height) {
        if (height > 0) {
            this.preferredPopupHeight = height;
        }

        revalidate();
        repaint();

        return this;
    }

    public ActionPopupMenu minPopupSize(int width, int height) {
        setMinimumSize(new Dimension(width, height));
        return this;
    }

    public ActionPopupMenu maxPopupSize(int width, int height) {
        setMaximumSize(new Dimension(width, height));
        return this;
    }

    public ActionPopupMenu preferredPopupSize(Dimension size) {
        Objects.requireNonNull(size, "size não pode ser null");

        this.preferredPopupWidth = size.width > 0 ? size.width : null;
        this.preferredPopupHeight = size.height > 0 ? size.height : null;

        revalidate();
        repaint();

        return this;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension preferredSize = super.getPreferredSize();

        int width = preferredSize.width;
        int height = preferredSize.height;

        if (preferredPopupWidth != null) {
            width = Math.max(preferredPopupWidth, preferredSize.width);
        }

        if (preferredPopupHeight != null) {
            height = Math.max(preferredPopupHeight, preferredSize.height);
        }

        return new Dimension(width, height);
    }

    public ActionPopupMenu border(Border border) {
        setBorder(border);
        return this;
    }

    public ActionPopupMenu border(BorderFactorySupplier supplier) {
        if (supplier != null) {
            setBorder(supplier.create());
        }

        return this;
    }

    @Override
    public ActionPopupMenu item(String text, ActionListener actionListener) {
        return item(text, null, true, actionListener);
    }

    @Override
    public ActionPopupMenu item(String text, Icon icon, ActionListener actionListener) {
        return item(text, icon, true, actionListener);
    }

    @Override
    public ActionPopupMenu item(String text, boolean enabled, ActionListener actionListener) {
        return item(text, null, enabled, actionListener);
    }

    @Override
    public ActionPopupMenu item(
            String text,
            Icon icon,
            boolean enabled,
            ActionListener actionListener
    ) {
        JMenuItem item = new JMenuItem(requireText(text));

        if (icon != null) {
            item.setIcon(icon);
        }

        item.setEnabled(enabled);

        if (actionListener != null) {
            item.addActionListener(actionListener);
        }

        applyStyle(item, actionMenuStyle);

        add(item);
        return this;
    }

    @Override
    public ActionPopupMenu item(Action action) {
        Objects.requireNonNull(action, "action não pode ser null");

        JMenuItem item = new JMenuItem(action);
        applyStyle(item, actionMenuStyle);

        add(item);
        return this;
    }

    public ActionPopupMenu item(JMenuItem item) {
        Objects.requireNonNull(item, "item não pode ser null");

        applyStyle(item, actionMenuStyle);

        add(item);
        return this;
    }

    @Override
    public ActionPopupMenu checkItem(
            String text,
            boolean selected,
            ActionListener actionListener
    ) {
        return checkItem(text, null, selected, true, actionListener);
    }

    @Override
    public ActionPopupMenu checkItem(
            String text,
            Icon icon,
            boolean selected,
            boolean enabled,
            ActionListener actionListener
    ) {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(requireText(text));

        if (icon != null) {
            item.setIcon(icon);
        }

        item.setSelected(selected);
        item.setEnabled(enabled);

        if (actionListener != null) {
            item.addActionListener(actionListener);
        }

        applyStyle(item, actionMenuStyle);

        add(item);
        return this;
    }

    @Override
    public ActionPopupMenu radioItem(
            String text,
            ButtonGroup group,
            boolean selected,
            ActionListener actionListener
    ) {
        return radioItem(text, null, group, selected, true, actionListener);
    }

    @Override
    public ActionPopupMenu radioItem(
            String text,
            Icon icon,
            ButtonGroup group,
            boolean selected,
            boolean enabled,
            ActionListener actionListener
    ) {
        Objects.requireNonNull(group, "group não pode ser null");

        JRadioButtonMenuItem item = new JRadioButtonMenuItem(requireText(text));

        if (icon != null) {
            item.setIcon(icon);
        }

        item.setSelected(selected);
        item.setEnabled(enabled);

        if (actionListener != null) {
            item.addActionListener(actionListener);
        }

        applyStyle(item, actionMenuStyle);

        group.add(item);
        add(item);

        return this;
    }

    @Override
    public ActionPopupMenu submenu(String text, Consumer<ActionMenu> builder) {
        return submenu(text, null, true, builder);
    }

    @Override
    public ActionPopupMenu submenu(
            String text,
            Icon icon,
            Consumer<ActionMenu> builder
    ) {
        return submenu(text, icon, true, builder);
    }

    @Override
    public ActionPopupMenu submenu(
            String text,
            Icon icon,
            boolean enabled,
            Consumer<ActionMenu> builder
    ) {
        JMenu menu = new JMenu(requireText(text));

        if (icon != null) {
            menu.setIcon(icon);
        }

        menu.setEnabled(enabled);

        ActionMenu actionMenu = new ActionMenu(menu, actionMenuStyle, useRootStyleForChildren);

        applyStyle(menu, actionMenu.resolveStyle());

        if (builder != null) {
            builder.accept(actionMenu);
        }

        add(menu);
        return this;
    }

    @Override
    public ActionPopupMenu custom(Component component) {
        Objects.requireNonNull(component, "component não pode ser null");

        applyStyle(component, actionMenuStyle);

        add(component);
        return this;
    }

    @Override
    public ActionPopupMenu separator() {
        addSeparator();
        return this;
    }

    public ActionPopupMenu when(boolean condition, Consumer<ActionPopupMenu> builder) {
        if (condition && builder != null) {
            builder.accept(this);
        }

        return this;
    }

    public ActionPopupMenu when(
            Supplier<Boolean> condition,
            Consumer<ActionPopupMenu> builder
    ) {
        if (
                condition != null &&
                        Boolean.TRUE.equals(condition.get()) &&
                        builder != null
        ) {
            builder.accept(this);
        }

        return this;
    }

    public void showAt(MouseEvent event) {
        Objects.requireNonNull(event, "event não pode ser null");

        Component component = event.getComponent();
        int screenX = event.getXOnScreen();
        int screenY = event.getYOnScreen();

        runOnEventDispatchThread(() -> {
            if (component == null || !component.isShowing()) {
                showAtScreen(screenX, screenY);
                return;
            }

            Point point = new Point(screenX, screenY);
            SwingUtilities.convertPointFromScreen(point, component);

            prepareForPresentation();
            ActionPopupMenu.super.show(component, point.x, point.y);
        });
    }

    public void showAt(int x, int y) {
        runOnEventDispatchThread(() -> showAtScreen(x, y));
    }

    public ActionPopupMenu showAt(Component invoker, int x, int y) {
        prepareForPresentation();
        ActionPopupMenu.super.show(invoker, x, y);
        return this;
    }

    public ActionPopupMenu showAt(Component invoker, Point point) {
        Objects.requireNonNull(point, "point não pode ser null");

        prepareForPresentation();
        ActionPopupMenu.super.show(invoker, point.x, point.y);
        return this;
    }

    private void showAtScreen(int x, int y) {
        prepareForPresentation();

        GraphicsConfiguration graphicsConfiguration = graphicsConfigurationAt(x, y);
        Dimension popupSize = getPreferredSize();
        Point popupLocation = fitPopupToScreen(x, y, popupSize, graphicsConfiguration);
        JDialog anchor = new JDialog(
                (Frame) null,
                "",
                false,
                graphicsConfiguration
        );

        anchor.setUndecorated(true);
        anchor.setType(Window.Type.UTILITY);
        anchor.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        anchor.setSize(
                Math.max(1, popupSize.width),
                Math.max(1, popupSize.height)
        );
        anchor.setLocation(popupLocation);
        anchor.setFocusableWindowState(true);
        anchor.setAutoRequestFocus(true);

        if (anchor.isAlwaysOnTopSupported()) {
            anchor.setAlwaysOnTop(true);
        }

        PopupSession session = new PopupSession(anchor, isLightWeightPopupEnabled());
        popupSession = session;

        session.popupMenuListener = new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent event) {
                scheduleSessionCleanup(session);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent event) {
                scheduleSessionCleanup(session);
            }
        };

        session.windowFocusListener = new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent event) {
                closePopupSession(session);
            }
        };

        session.awtEventListener = event -> {
            if (
                    event instanceof MouseEvent mouseEvent
                            && mouseEvent.getID() == MouseEvent.MOUSE_PRESSED
                            && !isPointInsideSelectedMenu(
                                    mouseEvent.getXOnScreen(),
                                    mouseEvent.getYOnScreen()
                            )
            ) {
                closePopupSession(session);
            }
        };

        addPopupMenuListener(session.popupMenuListener);
        anchor.addWindowFocusListener(session.windowFocusListener);
        Toolkit.getDefaultToolkit().addAWTEventListener(
                session.awtEventListener,
                AWTEvent.MOUSE_EVENT_MASK
        );

        setLightWeightPopupEnabled(true);

        try {
            anchor.setVisible(true);
            anchor.toFront();
            anchor.requestFocus();
            ActionPopupMenu.super.show(anchor.getContentPane(), 0, 0);
        } catch (RuntimeException | Error exception) {
            finishPopupSession(session);
            throw exception;
        }
    }

    private void prepareForPresentation() {
        PopupSession session = popupSession;

        if (session != null) {
            closePopupSession(session);
        } else if (isVisible()) {
            setVisible(false);
        }
    }

    private void closePopupSession(PopupSession session) {
        if (session == null || session.cleaned || popupSession != session) {
            return;
        }

        try {
            if (isVisible()) {
                setVisible(false);
            }

            MenuSelectionManager.defaultManager().clearSelectedPath();
        } finally {
            finishPopupSession(session);
        }
    }

    private void scheduleSessionCleanup(PopupSession session) {
        SwingUtilities.invokeLater(() -> finishPopupSession(session));
    }

    private void finishPopupSession(PopupSession session) {
        if (session == null || session.cleaned) {
            return;
        }

        session.cleaned = true;

        if (popupSession == session) {
            popupSession = null;
        }

        removePopupMenuListener(session.popupMenuListener);
        session.anchor.removeWindowFocusListener(session.windowFocusListener);
        Toolkit.getDefaultToolkit().removeAWTEventListener(session.awtEventListener);

        if (session.anchor.isDisplayable()) {
            session.anchor.dispose();
        }

        setLightWeightPopupEnabled(session.lightWeightPopupEnabled);
    }

    private boolean isPointInsideSelectedMenu(int screenX, int screenY) {
        Point screenPoint = new Point(screenX, screenY);

        for (MenuElement element : MenuSelectionManager.defaultManager().getSelectedPath()) {
            Component component = element.getComponent();

            if (component == null || !component.isShowing()) {
                continue;
            }

            Point location = component.getLocationOnScreen();
            Rectangle bounds = new Rectangle(location, component.getSize());

            if (bounds.contains(screenPoint)) {
                return true;
            }
        }

        return false;
    }

    private GraphicsConfiguration graphicsConfigurationAt(int x, int y) {
        Point point = new Point(x, y);
        GraphicsEnvironment environment = GraphicsEnvironment.getLocalGraphicsEnvironment();

        for (GraphicsDevice device : environment.getScreenDevices()) {
            GraphicsConfiguration configuration = device.getDefaultConfiguration();

            if (configuration.getBounds().contains(point)) {
                return configuration;
            }
        }

        return environment
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
    }

    private Point fitPopupToScreen(
            int x,
            int y,
            Dimension popupSize,
            GraphicsConfiguration graphicsConfiguration
    ) {
        Rectangle screen = new Rectangle(graphicsConfiguration.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsConfiguration);

        screen.x += insets.left;
        screen.y += insets.top;
        screen.width -= insets.left + insets.right;
        screen.height -= insets.top + insets.bottom;

        int fittedX = Math.max(
                screen.x,
                Math.min(x, screen.x + Math.max(0, screen.width - popupSize.width))
        );
        int fittedY = Math.max(
                screen.y,
                Math.min(y, screen.y + Math.max(0, screen.height - popupSize.height))
        );

        return new Point(fittedX, fittedY);
    }

    private void runOnEventDispatchThread(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }

    private static final class PopupSession {

        private final JDialog anchor;
        private final boolean lightWeightPopupEnabled;
        private PopupMenuListener popupMenuListener;
        private WindowAdapter windowFocusListener;
        private AWTEventListener awtEventListener;
        private boolean cleaned;

        private PopupSession(JDialog anchor, boolean lightWeightPopupEnabled) {
            this.anchor = anchor;
            this.lightWeightPopupEnabled = lightWeightPopupEnabled;
        }
    }

    protected void applyStyleToTree() {
        applyStyle(this, actionMenuStyle);

        for (Component component : getComponents()) {
            applyStyleRecursive(component, actionMenuStyle);
        }

        revalidate();
        repaint();
    }

    protected void applyStyleRecursive(Component component, ActionMenuStyle style) {
        applyStyle(component, style);

        if (component instanceof JMenu menu) {
            for (Component child : menu.getMenuComponents()) {
                applyStyleRecursive(child, style);
            }
        }
    }

    protected void applyStyle(Component component, ActionMenuStyle style) {
        if (component == null || isEmpty(style)) {
            return;
        }

        if (style.getBackground() != null) {
            component.setBackground(style.getBackground());
        }

        if (style.getForeground() != null) {
            component.setForeground(style.getForeground());
        }

        if (component instanceof JMenuItem menuItem) {
            applySelectionStyle(menuItem, style);
        }

        if (component instanceof JMenu menu) {
            JPopupMenu popupMenu = menu.getPopupMenu();

            if (style.getBackground() != null) {
                popupMenu.setBackground(style.getBackground());
            }

            if (style.getForeground() != null) {
                popupMenu.setForeground(style.getForeground());
            }
        }
    }

    protected boolean isEmpty(ActionMenuStyle style) {
        return style == null
                || (
                style.getBackground() == null
                        && style.getForeground() == null
                        && style.getSelectionBackground() == null
                        && style.getSelectionForeground() == null
        );
    }

    protected String requireText(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text não pode ser null ou vazio");
        }

        return text;
    }

    protected void applySelectionStyle(JMenuItem menuItem, ActionMenuStyle style) {
        if (menuItem == null || isEmpty(style)) {
            return;
        }

        StringBuilder flatLafStyle = new StringBuilder();

        if (style.getSelectionBackground() != null) {
            flatLafStyle.append("selectionBackground: ")
                    .append(toHex(style.getSelectionBackground()))
                    .append(";");
        }

        if (style.getSelectionForeground() != null) {
            flatLafStyle.append("selectionForeground: ")
                    .append(toHex(style.getSelectionForeground()))
                    .append(";");
        }

        if (!flatLafStyle.isEmpty()) {
            menuItem.putClientProperty("FlatLaf.style", flatLafStyle.toString());
        }
    }

    protected String toHex(Color color) {
        return String.format(
                "#%02x%02x%02x",
                color.getRed(),
                color.getGreen(),
                color.getBlue()
        );
    }
}
