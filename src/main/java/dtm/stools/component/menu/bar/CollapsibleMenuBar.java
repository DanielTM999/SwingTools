package dtm.stools.component.menu.bar;

import dtm.stools.component.menu.bar.tree.MenuNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CollapsibleMenuBar extends MenuBar {

    public static final String DEFAULT_COLLAPSE_BUTTON_TEXT = "\u2630";
    public static final String DEFAULT_COLLAPSE_BUTTON_RESOURCE = "/drawables/hamburger.png";
    public static final int DEFAULT_COLLAPSE_BUTTON_ICON_SIZE = 22;
    public static final int DEFAULT_COLLAPSE_BUTTON_SIZE = 40;

    protected final JButton collapseButton;
    private final Set<Menu> watchedMenus = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<Component> hiddenWhenMenuRevealed = Collections.newSetFromMap(new WeakHashMap<>());

    private Class<?> collapseButtonResourceBase = CollapsibleMenuBar.class;
    private String collapseButtonResource = DEFAULT_COLLAPSE_BUTTON_RESOURCE;
    private Image collapseButtonImage;
    private Icon collapseButtonIcon;
    private Color collapseButtonColor;
    private int collapseButtonIconSize = DEFAULT_COLLAPSE_BUTTON_ICON_SIZE;
    private int collapseButtonSize = DEFAULT_COLLAPSE_BUTTON_SIZE;

    private volatile boolean collapsed;
    private volatile boolean collapseButtonVisibleWhenExpanded;
    private volatile boolean autoCollapseAfterMenuClose = true;
    private volatile boolean transientExpanded;
    private boolean syncingInternally;

    public CollapsibleMenuBar() {
        collapseButton = createCollapseButton();
        configureCollapseButton(collapseButton);
        installPreMenuComponents();
        installMenuSync();
        setCollapsed(false);
    }

    protected JButton createCollapseButton() {
        return new JButton();
    }

    protected void configureCollapseButton(JButton button) {
        button.setFocusable(false);
        button.setMargin(new Insets(0, 0, 0, 0));
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setVerticalAlignment(SwingConstants.CENTER);
        applyCollapseButtonSize();
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText("Menu");
        button.addActionListener(e -> handleCollapseButtonClick());
        button.addPropertyChangeListener("UI", e -> updateCollapseButtonIcon());
        updateCollapseButtonIcon();
    }

    protected void installPreMenuComponents() {
        preMenu(collapseButton);
    }

    protected void handleCollapseButtonClick() {
        if (collapsed) {
            revealAndOpenFirstMenu();
        } else {
            setCollapsed(true);
        }
    }

    public CollapsibleMenuBar collapseButton(Consumer<JButton> configurer) {
        if (configurer != null) {
            configurer.accept(collapseButton);
            revalidate();
            repaint();
        }
        return this;
    }

    public CollapsibleMenuBar setCollapseButtonResource(String resourcePath) {
        return setCollapseButtonResource(CollapsibleMenuBar.class, resourcePath);
    }

    public CollapsibleMenuBar setCollapseButtonResource(Class<?> resourceBase, String resourcePath) {
        this.collapseButtonResourceBase = resourceBase == null ? CollapsibleMenuBar.class : resourceBase;
        this.collapseButtonResource = normalizeResourcePath(resourcePath);
        this.collapseButtonImage = null;
        this.collapseButtonIcon = null;
        updateCollapseButtonIcon();
        return this;
    }

    public CollapsibleMenuBar setCollapseButtonImage(Image image) {
        this.collapseButtonImage = image;
        this.collapseButtonIcon = null;
        updateCollapseButtonIcon();
        return this;
    }

    public CollapsibleMenuBar setCollapseButtonIcon(Icon icon) {
        this.collapseButtonIcon = icon;
        updateCollapseButtonIcon();
        return this;
    }

    public CollapsibleMenuBar setCollapseButtonColor(Color color) {
        this.collapseButtonColor = color;
        updateCollapseButtonIcon();
        return this;
    }

    public CollapsibleMenuBar useDefaultCollapseButtonColor() {
        return setCollapseButtonColor(null);
    }

    public CollapsibleMenuBar setCollapseButtonIconSize(int size) {
        this.collapseButtonIconSize = Math.max(8, size);
        updateCollapseButtonIcon();
        return this;
    }

    public int getCollapseButtonIconSize() {
        return collapseButtonIconSize;
    }

    public CollapsibleMenuBar setCollapseButtonSize(int size) {
        this.collapseButtonSize = Math.max(24, size);
        applyCollapseButtonSize();
        return this;
    }

    public int getCollapseButtonSize() {
        return collapseButtonSize;
    }

    public CollapsibleMenuBar setCollapsed(boolean collapsed) {
        runOnEdt(() -> {
            this.collapsed = collapsed;
            this.transientExpanded = false;
            syncCollapsedMenus();
        });
        return this;
    }

    public CollapsibleMenuBar toggleCollapsed() {
        return setCollapsed(!collapsed);
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public CollapsibleMenuBar setCollapseButtonVisibleWhenExpanded(boolean visible) {
        this.collapseButtonVisibleWhenExpanded = visible;
        syncCollapsedMenus();
        return this;
    }

    public boolean isCollapseButtonVisibleWhenExpanded() {
        return collapseButtonVisibleWhenExpanded;
    }

    public CollapsibleMenuBar setAutoCollapseAfterMenuClose(boolean autoCollapseAfterMenuClose) {
        this.autoCollapseAfterMenuClose = autoCollapseAfterMenuClose;
        return this;
    }

    public boolean isAutoCollapseAfterMenuClose() {
        return autoCollapseAfterMenuClose;
    }

    public JButton getCollapseButton() {
        return collapseButton;
    }

    public CollapsibleMenuBar preMenu(Component component) {
        addPreMenu(component);
        hidePreMenuWhenMenuRevealed(component);
        return this;
    }

    public CollapsibleMenuBar preMenuStrut(int width) {
        return preMenu(Box.createHorizontalStrut(Math.max(0, width)));
    }

    public CollapsibleMenuBar configurePreMenu(Consumer<CollapsibleMenuBar> configurer) {
        if (configurer != null) {
            configurer.accept(this);
            revalidate();
            repaint();
        }
        return this;
    }

    public CollapsibleMenuBar clearPreMenuComponents() {
        clearPreMenu();
        hiddenWhenMenuRevealed.clear();
        return this;
    }

    public CollapsibleMenuBar resetDefaultPreMenu() {
        clearPreMenu();
        hiddenWhenMenuRevealed.clear();
        installPreMenuComponents();
        syncCollapsedMenus();
        return this;
    }

    public CollapsibleMenuBar removeFromPreMenu(Component component) {
        removePreMenu(component);
        hiddenWhenMenuRevealed.remove(component);
        return this;
    }

    public CollapsibleMenuBar addPreMenuBeforeButton(Component component) {
        return addPreMenuAt(component, collapseButtonIndex());
    }

    public CollapsibleMenuBar addPreMenuAfterButton(Component component) {
        return addPreMenuAt(component, collapseButtonIndex() + 1);
    }

    public CollapsibleMenuBar addPreMenuAt(Component component, int index) {
        if (component == null) return this;
        List<Component> components = getPreMenuComponents();
        clearPreMenu();

        int target = Math.max(0, Math.min(index, components.size()));
        for (int i = 0; i <= components.size(); i++) {
            if (i == target) addPreMenu(component);
            if (i < components.size()) addPreMenu(components.get(i));
        }

        hiddenWhenMenuRevealed.add(component);
        syncCollapsedMenus();
        return this;
    }

    public CollapsibleMenuBar hidePreMenuWhenMenuRevealed(Component component) {
        if (component != null) {
            hiddenWhenMenuRevealed.add(component);
            syncCollapsedMenus();
        }
        return this;
    }

    public CollapsibleMenuBar keepPreMenuWhenMenuRevealed(Component component) {
        if (component != null) {
            hiddenWhenMenuRevealed.remove(component);
            syncCollapsedMenus();
        }
        return this;
    }

    public CollapsibleMenuBar setPreMenuHiddenWhenMenuRevealed(Component component, boolean hidden) {
        return hidden ? hidePreMenuWhenMenuRevealed(component) : keepPreMenuWhenMenuRevealed(component);
    }

    public CollapsibleMenuBar hideAllPreMenuWhenMenuRevealed() {
        hiddenWhenMenuRevealed.addAll(getPreMenuComponents());
        syncCollapsedMenus();
        return this;
    }

    public CollapsibleMenuBar keepAllPreMenuWhenMenuRevealed() {
        hiddenWhenMenuRevealed.clear();
        syncCollapsedMenus();
        return this;
    }

    public CollapsibleMenuBar moveCollapseButtonToStart() {
        return moveCollapseButtonTo(0);
    }

    public CollapsibleMenuBar moveCollapseButtonToEnd() {
        return moveCollapseButtonTo(getPreMenuComponents().size() - 1);
    }

    public CollapsibleMenuBar moveCollapseButtonTo(int index) {
        removePreMenu(collapseButton);
        return addPreMenuAt(collapseButton, index);
    }

    @Override
    public Menu addMenu(String id, String text) {
        return supplyOnEdt(() -> {
            Menu menu = super.addMenu(id, text);
            watchMenu(menu);
            menu.setVisible(!collapsed);
            return menu;
        });
    }

    @Override
    public CollapsibleMenuBar setMenuTree(MenuNode tree) {
        super.setMenuTree(tree);
        syncCollapsedMenus();
        return this;
    }

    @Override
    public CollapsibleMenuBar updateMenuTree(java.util.function.Consumer<MenuNode> mutator) {
        super.updateMenuTree(mutator);
        syncCollapsedMenus();
        return this;
    }

    @Override
    public CollapsibleMenuBar removeMenu(String id) {
        super.removeMenu(id);
        syncCollapsedMenus();
        return this;
    }

    @Override
    public CollapsibleMenuBar clearMenus() {
        super.clearMenus();
        syncCollapsedMenus();
        return this;
    }

    protected void syncCollapsedMenus() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::syncCollapsedMenus);
            return;
        }
        boolean previous = syncingInternally;
        syncingInternally = true;
        try {
            for (Menu menu : getMenus().values()) {
                watchMenu(menu);
                menu.setVisible(!collapsed);
            }
            syncPreMenuVisibility();
            revalidate();
            repaintAfterLayoutChange();
        } finally {
            syncingInternally = previous;
        }
    }

    /**
     * Repinta uma faixa horizontal completa ao redor da barra após uma mudança
     * de layout (colapsar/expandir, troca de ícone/tamanho).
     *
     * <p>O container direto da barra pode encolher junto com ela (ex.: um
     * cabeçalho que dimensiona o host pelo {@code preferredSize} da barra).
     * Repintar apenas o pai deixaria a área liberada — agora ocupada por
     * componentes vizinhos — com resíduo visual. Por isso a repintura é feita
     * em um ancestral estável (o {@code JRootPane}), cobrindo toda a largura.
     */
    protected void repaintAfterLayoutChange() {
        repaint();
        JRootPane rootPane = getRootPane();
        if (rootPane == null || getWidth() <= 0 || getHeight() <= 0) {
            Container parent = getParent();
            if (parent != null) parent.repaint();
            return;
        }
        Rectangle band = SwingUtilities.convertRectangle(
                this, new Rectangle(0, 0, getWidth(), getHeight()), rootPane);
        int top = Math.max(0, band.y - 4);
        int height = band.height + 8;
        rootPane.repaint(0, top, rootPane.getWidth(), height);
    }

    protected void syncPreMenuVisibility() {
        boolean menuRevealed = !collapsed;
        for (Component component : getPreMenuComponents()) {
            boolean hide = menuRevealed && hiddenWhenMenuRevealed.contains(component);
            boolean collapseButtonRule = component == collapseButton && !collapseButtonVisibleWhenExpanded;
            component.setVisible(!hide && !(menuRevealed && collapseButtonRule));
        }
    }

    protected void revealAndOpenFirstMenu() {
        runOnEdt(() -> {
            transientExpanded = true;
            collapsed = false;
            syncCollapsedMenus();
            validateMenuHierarchyForPopup();
            SwingUtilities.invokeLater(() -> {
                validateMenuHierarchyForPopup();
                flushPendingPaint();
                Menu first = firstVisibleMenu();
                if (first != null && first.isEnabled()) {
                    first.doClick(0);
                }
            });
        });
    }

    private void flushPendingPaint() {
        if (isShowing() && getWidth() > 0 && getHeight() > 0) {
            paintImmediately(0, 0, getWidth(), getHeight());
        }
    }

    protected Menu firstVisibleMenu() {
        for (Menu menu : getMenus().values()) {
            if (menu.isVisible()) return menu;
        }
        return null;
    }

    protected void watchMenu(Menu menu) {
        if (!watchedMenus.add(menu)) return;
        menu.addMenuListener(new MenuListener() {
            @Override public void menuSelected(MenuEvent e) {
            }

            @Override public void menuDeselected(MenuEvent e) {
                scheduleAutoCollapse();
            }

            @Override public void menuCanceled(MenuEvent e) {
                scheduleAutoCollapse();
            }
        });
    }

    protected void scheduleAutoCollapse() {
        if (!autoCollapseAfterMenuClose || !transientExpanded) return;
        SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(() -> {
            if (!isAnyMenuOpen()) {
                transientExpanded = false;
                setCollapsed(true);
            }
        }));
    }

    protected boolean isAnyMenuOpen() {
        for (Menu menu : getMenus().values()) {
            if (menu.isSelected() || menu.isPopupMenuVisible()) {
                return true;
            }
        }
        return false;
    }

    protected void validateMenuHierarchyForPopup() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::validateMenuHierarchyForPopup);
            return;
        }
        invalidate();

        Container parent = getParent();
        while (parent != null) {
            parent.invalidate();
            if (parent instanceof JComponent component) {
                component.revalidate();
            }
            parent = parent.getParent();
        }

        Container topLevel = getTopLevelAncestor();
        if (topLevel != null) {
            topLevel.validate();
        } else {
            doLayout();
        }
    }

    protected int collapseButtonIndex() {
        List<Component> components = getPreMenuComponents();
        int index = components.indexOf(collapseButton);
        return index < 0 ? 0 : index;
    }

    protected void updateCollapseButtonIcon() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::updateCollapseButtonIcon);
            return;
        }
        Icon icon = createCollapseButtonIcon();
        if (icon != null) {
            collapseButton.setText(null);
            collapseButton.setIcon(icon);
        } else {
            collapseButton.setIcon(null);
            collapseButton.setText(DEFAULT_COLLAPSE_BUTTON_TEXT);
        }
        revalidate();
        repaintAfterLayoutChange();
    }

    protected void applyCollapseButtonSize() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::applyCollapseButtonSize);
            return;
        }
        Dimension size = new Dimension(collapseButtonSize, collapseButtonSize);
        collapseButton.setPreferredSize(size);
        collapseButton.setMinimumSize(size);
        revalidate();
        repaintAfterLayoutChange();
    }

    protected Icon createCollapseButtonIcon() {
        if (collapseButtonIcon != null) {
            return collapseButtonIcon;
        }

        Image image = collapseButtonImage != null ? collapseButtonImage : loadCollapseButtonImage();
        if (image == null) {
            return null;
        }

        Color color = resolveCollapseButtonColor();
        BufferedImage tinted = tintImage(image, color);
        return new ImageIcon(toBufferedImage(tinted, collapseButtonIconSize, collapseButtonIconSize));
    }

    protected Image loadCollapseButtonImage() {
        if (collapseButtonResource == null || collapseButtonResource.isBlank()) {
            return null;
        }
        try (InputStream in = collapseButtonResourceBase.getResourceAsStream(collapseButtonResource)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            return null;
        }
    }

    protected Color resolveCollapseButtonColor() {
        if (collapseButtonColor != null) {
            return collapseButtonColor;
        }

        Color color = UIManager.getColor("MenuBar.foreground");
        if (color == null) color = UIManager.getColor("TitlePane.foreground");
        if (color == null) color = UIManager.getColor("Button.foreground");
        if (color == null) color = UIManager.getColor("Label.foreground");
        return color == null ? Color.WHITE : color;
    }

    protected BufferedImage tintImage(Image image, Color color) {
        BufferedImage source = toBufferedImage(image, image.getWidth(null), image.getHeight(null));
        BufferedImage tinted = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                int pixel = source.getRGB(x, y);
                int alpha = (pixel >>> 24) & 0xff;
                if (alpha == 0) {
                    tinted.setRGB(x, y, pixel);
                } else {
                    tinted.setRGB(x, y, (alpha << 24) | (color.getRGB() & 0x00ffffff));
                }
            }
        }

        return tinted;
    }

    protected BufferedImage toBufferedImage(Image image, int width, int height) {
        int w = Math.max(1, width);
        int h = Math.max(1, height);
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = buffered.createGraphics();
        try {
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.drawImage(image, 0, 0, w, h, null);
        } finally {
            g2.dispose();
        }
        return buffered;
    }

    private String normalizeResourcePath(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return DEFAULT_COLLAPSE_BUTTON_RESOURCE;
        }
        return resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
    }

    private void installMenuSync() {
        addContainerListener(new ContainerAdapter() {
            @Override public void componentAdded(ContainerEvent e) {
                if (syncingInternally) return;
                syncCollapsedMenus();
            }

            @Override public void componentRemoved(ContainerEvent e) {
                if (syncingInternally) return;
                syncCollapsedMenus();
            }
        });
    }

    private void runOnEdt(Runnable action) {
        if (action == null) return;
        if (SwingUtilities.isEventDispatchThread()) {
            action.run();
        } else {
            SwingUtilities.invokeLater(action);
        }
    }

    private <T> T supplyOnEdt(Supplier<T> supplier) {
        if (supplier == null) return null;
        if (SwingUtilities.isEventDispatchThread()) {
            return supplier.get();
        }
        AtomicReference<T> ref = new AtomicReference<>();
        AtomicReference<RuntimeException> error = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    ref.set(supplier.get());
                } catch (RuntimeException e) {
                    error.set(e);
                }
            });
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return null;
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException re) throw re;
            throw new RuntimeException(cause);
        }
        if (error.get() != null) throw error.get();
        return ref.get();
    }
}
