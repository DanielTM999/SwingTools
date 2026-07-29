package dtm.stools.component.panels.tab;

import com.formdev.flatlaf.FlatClientProperties;
import dtm.stools.component.annimation.ComponentAnimator;
import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class TabbedPanel extends PanelEventListener {

    @Getter
    private final JTabbedPane tabbedPane;
    private final Map<String, TabEntry> tabsByKey = new LinkedHashMap<>();
    private final Map<Component, String> keyByComponent = new LinkedHashMap<>();
    private final Map<String, Window> windowsByKey = new LinkedHashMap<>();
    @Getter
    private final TabStyle tabStyle = new TabStyle();
    private final DefaultTabHeaderRenderer headerRenderer = new DefaultTabHeaderRenderer();
    private final DefaultTabMenuFactory defaultTabMenuFactory = new DefaultTabMenuFactory();
    private final TabDragController dragController = new TabDragController(this);
    private final JPanel tabControlBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
    private final JButton newTabButton = new JButton("+");
    private final JButton tabListButton = new JButton("Tabs");
    private final List<String> mruKeys = new ArrayList<>();

    private String currentKey;

    @Getter
    private String lastKey;

    @Getter
    private boolean closeButtonsVisible = true;

    @Getter
    @Setter
    private boolean fireSelectionChangeOnClose = true;

    private boolean suppressSelectionEvent;
    private boolean tabDragEnabled = true;
    private boolean tabMenuEnabled = true;
    private boolean defaultTabMenuEnabled = true;
    private boolean splitTabsOnDragEnabled;
    private boolean autoRemoveEmptyTabGroupsEnabled = true;
    private boolean renameOnDoubleClickEnabled;
    private boolean closeOnMiddleClickEnabled = true;
    private boolean newTabButtonVisible;
    private boolean tabListButtonVisible;
    private boolean mruSwitchingEnabled = true;
    private boolean pinnedTabsShowTitle = true;
    private boolean tabWindowEnabled = true;
    private boolean tabWindowAlwaysOnTop;
    private Dimension defaultTabWindowSize = new Dimension(720, 480);
    private int splitDropZoneSize = 96;
    private TabHeaderFactory tabHeaderFactory;
    private TabMenuProvider tabMenuProvider;
    private TabGroupFactory tabGroupFactory;
    private TabSeparatorFactory tabSeparatorFactory;
    private Runnable newTabAction;
    private BiPredicate<TabbedPanel, TabEntry> closeConfirmationProvider;
    private JRootPane dockRootPane;
    private TabbedPanel dockOwner = this;

    public TabbedPanel() {
        this(JTabbedPane.TOP);
    }

    public TabbedPanel(int tabPlacement) {
        super(new BorderLayout(), true);
        this.tabbedPane = new JTabbedPane(tabPlacement);
        installTabbedPaneStyle();
        installTabControlBar();
        add(tabbedPane, BorderLayout.CENTER);
        installSelectionListener();
    }

    private void installTabControlBar() {
        tabControlBar.setOpaque(false);
        newTabButton.setFocusable(false);
        newTabButton.setToolTipText("Nova aba");
        newTabButton.addActionListener(e -> {
            if (newTabAction != null) newTabAction.run();
        });
        tabListButton.setFocusable(false);
        tabListButton.setToolTipText("Listar abas");
        tabListButton.addActionListener(e -> showTabListMenu());
        tabControlBar.add(tabListButton);
        tabControlBar.add(newTabButton);
        updateTabControlBar();
        add(tabControlBar, BorderLayout.NORTH);
    }

    private void updateTabControlBar() {
        newTabButton.setVisible(newTabButtonVisible);
        tabListButton.setVisible(tabListButtonVisible);
        tabControlBar.setVisible(newTabButtonVisible || tabListButtonVisible);
        revalidate();
        repaint();
    }

    public void setTabTitleForeground(String key, Color color) {
        TabEntry entry = tabsByKey.get(key);

        if (entry == null) {
            return;
        }

        entry.setTitleForeground(color);
        updateTabHeader(key);
    }

    public boolean isSplit() {
        return isDockModeEnabled() && getDockGroups().size() > 1;
    }

    public String getCurrentKey() {
        TabbedPanel focusedGroup = getFocusedTabGroup();
        return focusedGroup.currentKey;
    }

    public String getMainCurrentKey() {
        return currentKey;
    }

    public TabbedPanel getFocusedTabGroup() {
        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();

        if (focusOwner == null) {
            return resolveInsertionTarget();
        }

        for (TabbedPanel group : resolveGroupsForVisibility()) {
            if (focusOwner == group || SwingUtilities.isDescendingFrom(focusOwner, group)) {
                return group;
            }
        }

        return resolveInsertionTarget();
    }

    public void clearTabTitleForeground(String key) {
        setTabTitleForeground(key, null);
    }

    public String addTab(String title, Component component) {
        String key = createTabKey();
        addTab(key, title, component);
        return key;
    }

    public String addTab(String title, Component component, TabHeaderFactory headerFactory) {
        String key = createTabKey();
        addTab(key, title, null, component, true, null, headerFactory);
        return key;
    }

    public String addTab(TabConfig config) {
        Objects.requireNonNull(config, "config");
        TabbedPanel target = resolveInsertionTarget();
        return target.addTabLocal(config);
    }

    private String addTabLocal(TabConfig config) {
        String key = config.getKey() == null || config.getKey().isBlank() ? createTabKey() : config.getKey();
        TabEntry entry = addTabEntryLocal(
                key,
                config.getTitle(),
                config.getIcon(),
                config.getComponent(),
                config.isClosable(),
                config.getTooltip(),
                config.getHeaderFactory(),
                config.getSelectedIcon(),
                config.getTitleForeground(),
                config.getSelectedTitleForeground()
        );
        if (config.getBadge() != null) {
            entry.setBadge(config.getBadge());
        }
        entry.setPinned(config.isPinned());
        entry.setDirty(config.isDirty());
        enforcePinnedPlacement(key);
        updateTabHeader(key);
        return key;
    }

    public boolean isWithinRoot(Component component) {
        if (component == null) {
            return false;
        }

        if (isSameOrDescendant(component, this)) {
            return true;
        }

        if (!isDockModeEnabled()) {
            return false;
        }

        for (TabbedPanel group : getDockGroups()) {
            if (isSameOrDescendant(component, group)) {
                return true;
            }
        }

        return false;
    }

    public void addTab(String key, String title, Component component) {
        addTab(key, title, null, component, true, null);
    }

    public void addTab(String key, String title, Icon icon, Component component) {
        addTab(key, title, icon, component, true, null);
    }

    public void addTab(String key, String title, Component component, boolean closable) {
        addTab(key, title, null, component, closable, null);
    }

    public void addTab(String key, String title, Icon icon, Component component, boolean closable, String tooltip) {
        addTab(key, title, icon, component, closable, tooltip, null);
    }

    public void addTab(String key, String title, Icon icon, Component component, boolean closable, String tooltip, TabHeaderFactory headerFactory) {
        addTabEntry(key, title, icon, component, closable, tooltip, headerFactory, null, null, null);
    }

    public void addTab(String key, String title, Icon icon, Component component, boolean closable, String tooltip, TabHeaderFactory headerFactory, Color titleForeground) {
        addTabEntry(key, title, icon, component, closable, tooltip, headerFactory, null, titleForeground, null);
    }

    public void addTab(String key, String title, Icon icon, Component component, boolean closable, String tooltip, TabHeaderFactory headerFactory, Color titleForeground, Color selectedTitleForeground) {
        addTabEntry(key, title, icon, component, closable, tooltip, headerFactory, null, titleForeground, selectedTitleForeground);
    }

    protected TabEntry addTabEntry(String key, String title, Icon icon, Component component, boolean closable, String tooltip, TabHeaderFactory headerFactory, Icon selectedIcon, Color titleForeground, Color selectedTitleForeground) {
        TabbedPanel target = resolveInsertionTarget();
        return target.addTabEntryLocal(
                key,
                title,
                icon,
                component,
                closable,
                tooltip,
                headerFactory,
                selectedIcon,
                titleForeground,
                selectedTitleForeground
        );
    }

    private TabEntry addTabEntryLocal(String key, String title, Icon icon, Component component, boolean closable, String tooltip, TabHeaderFactory headerFactory, Icon selectedIcon, Color titleForeground, Color selectedTitleForeground) {
        requireKey(key);
        Objects.requireNonNull(component, "component");

        if (resolveDockOwner().resolveGroupForKey(key) != null) {
            throw new IllegalArgumentException("Tab key already registered: " + key);
        }

        TabEntry entry = new TabEntry(key, title, icon, component, closable, tooltip, titleForeground, selectedTitleForeground);
        entry.setHeaderFactory(headerFactory);
        entry.setSelectedIcon(selectedIcon);
        tabsByKey.put(key, entry);
        keyByComponent.put(component, key);

        tabbedPane.addTab(title, icon, component, tooltip);

        if (tabsByKey.size() == 1) {
            currentKey = key;
            tabbedPane.setSelectedComponent(component);
        }

        updateTabHeader(key);
        enforcePinnedPlacement(key);
        dispatchTabEvent(EventTabbedPanel.TAB_ADD, entry, null);
        return entry;
    }

    public boolean removeTab(String key) {
        TabbedPanel group = resolveGroupForKey(key);
        if (group != null && group != this) {
            return group.removeTab(key);
        }

        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return false;
        return removeTab(entry, EventTabbedPanel.TAB_REMOVE, true);
    }

    public boolean closeTab(String key) {
        TabbedPanel group = resolveGroupForKey(key);
        if (group != null && group != this) {
            return group.closeTab(key);
        }

        TabEntry entry = tabsByKey.get(key);
        if (entry == null || !entry.isClosable()) return false;

        TabEvent before = dispatchTabEvent(EventTabbedPanel.BEFORE_TAB_CLOSE, entry, null);
        if (before.isCanceled()) return false;
        if (closeConfirmationProvider != null && !closeConfirmationProvider.test(this, entry)) return false;

        return removeTab(entry, EventTabbedPanel.TAB_CLOSE, true);
    }

    public boolean closeCurrentTab() {
        TabbedPanel focusedGroup = getFocusedTabGroup();
        String key = focusedGroup.currentKey;

        return key != null && focusedGroup.closeTab(key);
    }

    public boolean closeAllTabs() {
        boolean changed = false;
        Collection<String> keys = this == resolveDockOwner() ? getKeys() : tabsByKey.keySet();
        for (String key : new ArrayList<>(keys)) {
            TabbedPanel group = resolveGroupForKey(key);
            TabEntry entry = group == null ? null : group.tabsByKey.get(key);
            if (entry != null && !entry.isPinned()) {
                changed |= closeTab(key);
            }
        }
        return changed;
    }

    public boolean closeAllTabsIncludingPinned() {
        boolean changed = false;
        Collection<String> keys = this == resolveDockOwner() ? getKeys() : tabsByKey.keySet();
        for (String key : new ArrayList<>(keys)) {
            changed |= closeTab(key);
        }
        return changed;
    }

    public boolean closeSavedTabs() {
        boolean changed = false;
        Collection<String> keys = this == resolveDockOwner() ? getKeys() : tabsByKey.keySet();
        for (String key : new ArrayList<>(keys)) {
            TabbedPanel group = resolveGroupForKey(key);
            TabEntry entry = group == null ? null : group.tabsByKey.get(key);
            if (entry != null && !entry.isDirty() && !entry.isPinned()) {
                changed |= closeTab(key);
            }
        }
        return changed;
    }

    public boolean closeOtherTabs(String key) {
        if (resolveGroupForKey(key) == null) return false;
        boolean changed = false;
        Collection<String> keys = this == resolveDockOwner() ? getKeys() : tabsByKey.keySet();
        for (String candidate : new ArrayList<>(keys)) {
            TabbedPanel group = resolveGroupForKey(candidate);
            TabEntry entry = group == null ? null : group.tabsByKey.get(candidate);
            if (!candidate.equals(key) && entry != null && !entry.isPinned()) {
                changed |= closeTab(candidate);
            }
        }
        return changed;
    }

    public void switchTo(String key) {
        TabbedPanel group = resolveGroupForKey(key);
        if (group != null && group != this) {
            group.switchTo(key);
            return;
        }

        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return;
        tabbedPane.setSelectedComponent(entry.getComponent());
    }

    public void switchTo(Component component) {
        TabbedPanel group = resolveGroupForComponent(component);
        if (group != null && group != this) {
            group.switchTo(component);
            return;
        }

        String key = keyByComponent.get(component);
        if (key != null) {
            switchTo(key);
        }
    }

    public void switchFirst() {
        if (!tabsByKey.isEmpty()) switchTo(tabsByKey.keySet().iterator().next());
    }

    public void switchLast() {
        if (tabsByKey.isEmpty()) return;
        List<String> keys = new ArrayList<>(tabsByKey.keySet());
        switchTo(keys.getLast());
    }

    public void switchNext() {
        int index = tabbedPane.getSelectedIndex();
        if (index >= 0 && index + 1 < tabbedPane.getTabCount()) {
            tabbedPane.setSelectedIndex(index + 1);
        }
    }

    public void switchPrevious() {
        int index = tabbedPane.getSelectedIndex();
        if (index > 0) {
            tabbedPane.setSelectedIndex(index - 1);
        }
    }

    public boolean moveTab(String key, int newIndex) {
        return moveTabInternal(key, newIndex, true, true);
    }

    boolean moveTabFromDrag(String key, int newIndex, int oldIndex) {
        if (moveTabInternal(key, newIndex, true, false)) {
            TabEntry moved = tabsByKey.get(key);
            dispatchTabEvent(EventTabbedPanel.TAB_DRAG, moved, new HashMap<>() {{
                put("oldIndex", oldIndex);
                put("newIndex", indexOf(key));
            }});
            return true;
        }
        return false;
    }

    boolean splitTabFromDrag(String key, Point point) {
        TabSplitPlacement placement = resolveSplitPlacement(point);
        return placement != null && splitTab(key, placement);
    }

    boolean transferTabFromDrag(String key, Point point) {
        TabbedPanel target = resolveTransferTarget(point);
        if (target == null) return false;

        Point targetPoint = SwingUtilities.convertPoint(tabbedPane, point, target.tabbedPane);
        int targetIndex = target.resolveDropIndex(targetPoint);
        return transferTabTo(key, target, targetIndex);
    }

    boolean hasTransferTarget(Point point) {
        return resolveTransferTarget(point) != null;
    }

    public boolean dockTab(String key, TabSplitPlacement placement) {
        return splitTab(key, placement);
    }

    public boolean splitTab(String key, TabSplitPlacement placement) {
        if (!isDockModeEnabled()) return false;

        TabEntry entry = tabsByKey.get(key);
        if (entry == null || placement == null) return false;
        if (!hasEnoughTabsToSplit()) return false;

        int oldIndex = indexOf(key);
        TabEvent before = dispatchTabEvent(EventTabbedPanel.BEFORE_TAB_SPLIT, entry, new HashMap<>() {{
            put("oldIndex", oldIndex);
            put("placement", placement);
        }});
        if (before.isCanceled()) return false;

        TabbedPanel target = createSplitTabGroup(entry, placement);
        if (target == null || !installSplitGroup(target, placement)) return false;

        TabConfig config = createTransferConfig(entry);

        if (!detachTabForTransfer(entry)) return false;
        target.addTabLocal(config);
        target.switchTo(entry.getKey());

        dispatchTabEvent(EventTabbedPanel.TAB_SPLIT, entry, new HashMap<>() {{
            put("oldIndex", oldIndex);
            put("placement", placement);
            put("target", target);
        }});
        return true;
    }

    public boolean transferTabTo(String key, TabbedPanel target) {
        return transferTabTo(key, target, -1);
    }

    public boolean reattachTabTo(String key, TabbedPanel target) {
        return transferTabTo(key, target);
    }

    public boolean reattachTabTo(String key, TabbedPanel target, int targetIndex) {
        return transferTabTo(key, target, targetIndex);
    }

    public boolean transferTabTo(String key, TabbedPanel target, int targetIndex) {
        Objects.requireNonNull(target, "target");
        if (!isDockModeEnabled() || !target.isDockModeEnabled() || target == this) return false;

        JRootPane rootPane = resolveDockRootPane();
        if (rootPane == null) rootPane = target.resolveDockRootPane();
        if (rootPane != null) {
            dockRootPane = rootPane;
            target.dockRootPane = rootPane;
        }

        TabEntry entry = tabsByKey.get(key);
        if (entry == null || target.containsLocal(key) || target.containsLocal(entry.getComponent())) return false;

        int oldIndex = indexOf(key);
        TabEvent before = dispatchTabEvent(EventTabbedPanel.BEFORE_TAB_TRANSFER, entry, new HashMap<>() {{
            put("oldIndex", oldIndex);
            put("target", target);
            put("targetIndex", targetIndex);
        }});
        if (before.isCanceled()) return false;

        TabConfig config = createTransferConfig(entry);
        if (!detachTabForTransfer(entry)) return false;

        target.addTabLocal(config);
        if (targetIndex >= 0) {
            target.moveTab(key, targetIndex);
        }
        target.switchTo(key);

        dispatchTabEvent(EventTabbedPanel.TAB_TRANSFER, entry, new HashMap<>() {{
            put("oldIndex", oldIndex);
            put("target", target);
            put("targetIndex", target.indexOf(key));
        }});
        return true;
    }

    public int reattachAllTabs() {
        if (!isDockModeEnabled()) return 0;

        TabbedPanel target = resolveInsertionTarget();
        int moved = 0;
        for (TabbedPanel group : new ArrayList<>(resolveGroupsForVisibility())) {
            if (group == target) continue;

            for (TabEntry entry : new ArrayList<>(group.tabsByKey.values())) {
                if (group.transferTabTo(entry.getKey(), target)) {
                    moved++;
                }
            }
        }
        return moved;
    }

    public int dockAllTabs() {
        return reattachAllTabs();
    }

    public int mergeDockGroups() {
        return reattachAllTabs();
    }

    public boolean openTabInWindow(String key) {
        return openTabInWindow(key, null, null);
    }

    public boolean openTabInWindow(String key, Dimension size) {
        return openTabInWindow(key, size, null);
    }

    public boolean openTabInWindowAt(String key, Point screenLocation) {
        return openTabInWindow(key, null, screenLocation);
    }

    public boolean openTabInWindow(String key, Dimension size, Point screenLocation) {
        if (!tabWindowEnabled) return false;

        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return false;

        Window existing = windowsByKey.get(key);
        if (existing != null) {
            existing.toFront();
            existing.requestFocus();
            return true;
        }

        int oldIndex = indexOf(key);
        TabEvent before = dispatchTabEvent(EventTabbedPanel.BEFORE_TAB_WINDOW, entry, new HashMap<>() {{
            put("oldIndex", oldIndex);
        }});
        if (before.isCanceled()) return false;

        TabConfig config = createTransferConfig(entry);
        Component component = entry.getComponent();
        String title = entry.getTitle();
        Icon icon = entry.getIcon();

        if (!detachTabForTransfer(entry)) return false;

        Window window = createTabWindow(key, config, oldIndex, component, title, icon, size, screenLocation);
        windowsByKey.put(key, window);
        window.setVisible(true);
        window.toFront();

        dispatchTabEvent(EventTabbedPanel.TAB_WINDOW_OPEN, entry, new HashMap<>() {{
            put("oldIndex", oldIndex);
            put("window", window);
        }});
        return true;
    }

    boolean detachTabToWindowFromDrag(String key, Point pointInTabbedPane) {
        if (!tabWindowEnabled || pointInTabbedPane == null) return false;
        if (!tabsByKey.containsKey(key)) return false;
        if (!tabbedPane.isShowing()) return false;

        Window ancestor = SwingUtilities.getWindowAncestor(this);
        if (ancestor == null) return false;

        Point screen = new Point(pointInTabbedPane);
        SwingUtilities.convertPointToScreen(screen, tabbedPane);

        if (ancestor.getBounds().contains(screen)) return false;

        return openTabInWindowAt(key, screen);
    }

    private Window createTabWindow(String key, TabConfig config, int oldIndex, Component component, String title, Icon icon, Dimension size, Point screenLocation) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog window = new JDialog(owner);
        window.setModalityType(Dialog.ModalityType.MODELESS);
        window.setTitle(title == null ? "" : title);
        window.setAlwaysOnTop(tabWindowAlwaysOnTop);
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        if (icon instanceof ImageIcon imageIcon && imageIcon.getImage() != null) {
            window.setIconImage(imageIcon.getImage());
        }

        window.getContentPane().setLayout(new BorderLayout());
        window.getContentPane().add(component, BorderLayout.CENTER);

        Dimension windowSize = size != null ? size : defaultTabWindowSize;
        if (windowSize != null && windowSize.width > 0 && windowSize.height > 0) {
            window.setSize(windowSize);
        } else {
            window.pack();
        }

        if (screenLocation != null) {
            window.setLocation(screenLocation.x - window.getWidth() / 2, Math.max(0, screenLocation.y - 12));
        } else {
            window.setLocationRelativeTo(owner);
        }

        window.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                reattachTabFromWindow(key, config, oldIndex, window);
            }
        });
        return window;
    }

    public boolean reattachTabFromWindow(String key) {
        return closeTabWindow(key);
    }

    private void reattachTabFromWindow(String key, TabConfig config, int oldIndex, JDialog window) {
        windowsByKey.remove(key);
        window.getContentPane().removeAll();

        TabbedPanel targetGroup = resolveInsertionTarget();
        if (!targetGroup.containsLocal(key)) {
            targetGroup.addTabLocal(config);
            int targetIndex = Math.max(0, Math.min(oldIndex, Math.max(0, targetGroup.getTabCount() - 1)));
            targetGroup.moveTab(key, targetIndex);
            targetGroup.switchTo(key);
        }

        window.dispose();

        TabEntry entry = targetGroup.tabsByKey.get(key);
        dispatchTabEvent(EventTabbedPanel.TAB_WINDOW_CLOSE, entry, new HashMap<>() {{
            put("newIndex", targetGroup.indexOf(key));
        }});
    }

    public boolean closeTabWindow(String key) {
        Window window = windowsByKey.get(key);
        if (window == null) return false;
        window.dispatchEvent(new java.awt.event.WindowEvent(window, java.awt.event.WindowEvent.WINDOW_CLOSING));
        return true;
    }

    public int reattachAllWindows() {
        int count = 0;
        for (String key : new ArrayList<>(windowsByKey.keySet())) {
            if (closeTabWindow(key)) count++;
        }
        return count;
    }

    public boolean isTabInWindow(String key) {
        return windowsByKey.containsKey(key);
    }

    public Set<String> getWindowedTabKeys() {
        return new java.util.LinkedHashSet<>(windowsByKey.keySet());
    }

    public boolean hasWindowedTabs() {
        return !windowsByKey.isEmpty();
    }

    public TabbedPanel setTabWindowEnabled(boolean enabled) {
        this.tabWindowEnabled = enabled;
        return this;
    }

    public boolean isTabWindowEnabled() {
        return tabWindowEnabled;
    }

    public TabbedPanel setTabWindowAlwaysOnTop(boolean alwaysOnTop) {
        this.tabWindowAlwaysOnTop = alwaysOnTop;
        for (Window window : windowsByKey.values()) {
            window.setAlwaysOnTop(alwaysOnTop);
        }
        return this;
    }

    public boolean isTabWindowAlwaysOnTop() {
        return tabWindowAlwaysOnTop;
    }

    public TabbedPanel setDefaultTabWindowSize(Dimension size) {
        this.defaultTabWindowSize = size;
        return this;
    }

    public Dimension getDefaultTabWindowSize() {
        return defaultTabWindowSize;
    }

    public TabbedPanel onBeforeTabWindow(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.BEFORE_TAB_WINDOW, listener);
    }

    public TabbedPanel onTabWindowOpen(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_WINDOW_OPEN, listener);
    }

    public TabbedPanel onTabWindowClose(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_WINDOW_CLOSE, listener);
    }

    public List<TabbedPanel> getDockGroups() {
        if (!isDockModeEnabled()) return List.of();

        JRootPane rootPane = resolveDockRootPane();
        if (rootPane == null) return List.of(this);

        TabbedPanel owner = resolveDockOwner();
        List<TabbedPanel> groups = new ArrayList<>();
        for (TabbedPanel group : collectTabbedPanels(rootPane.getContentPane())) {
            if (group.isDockModeEnabled() && group.resolveDockOwner() == owner) {
                groups.add(group);
            }
        }
        return groups;
    }

    public TabbedPanel setTitle(String key, String title) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return this;
        entry.setTitle(title);

        int index = tabbedPane.indexOfComponent(entry.getComponent());
        if (index >= 0) tabbedPane.setTitleAt(index, title);
        updateTabHeader(key);
        dispatchTabEvent(EventTabbedPanel.TAB_TITLE_CHANGE, entry, null);
        return this;
    }

    public TabbedPanel changeKey(String oldKey, String newKey) {
        if (oldKey == null || newKey == null) return this;
        if (oldKey.isBlank() || newKey.isBlank()) return this;
        if (oldKey.equals(newKey)) return this;
        if (!tabsByKey.containsKey(oldKey)) return this;
        if (tabsByKey.containsKey(newKey)) return this;

        Map<String, TabEntry> rebuilt = new LinkedHashMap<>();
        for (Map.Entry<String, TabEntry> mapEntry : tabsByKey.entrySet()) {
            String entryKey = mapEntry.getKey();
            TabEntry tabEntry = mapEntry.getValue();
            if (entryKey.equals(oldKey)) {
                tabEntry.setKey(newKey);
                rebuilt.put(newKey, tabEntry);
            } else {
                rebuilt.put(entryKey, tabEntry);
            }
        }
        tabsByKey.clear();
        tabsByKey.putAll(rebuilt);

        TabEntry renamed = tabsByKey.get(newKey);
        keyByComponent.put(renamed.getComponent(), newKey);

        for (int i = 0; i < mruKeys.size(); i++) {
            if (oldKey.equals(mruKeys.get(i))) {
                mruKeys.set(i, newKey);
            }
        }

        if (oldKey.equals(currentKey)) currentKey = newKey;
        if (oldKey.equals(lastKey)) lastKey = newKey;

        updateTabHeader(newKey);
        return this;
    }

    public TabbedPanel setIcon(String key, Icon icon) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return this;
        entry.setIcon(icon);

        int index = tabbedPane.indexOfComponent(entry.getComponent());
        if (index >= 0) tabbedPane.setIconAt(index, icon);
        updateTabHeader(key);
        dispatchTabEvent(EventTabbedPanel.TAB_ICON_CHANGE, entry, null);
        return this;
    }

    public TabbedPanel setSelectedIcon(String key, Icon icon) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return this;
        entry.setSelectedIcon(icon);
        updateTabHeader(key);
        dispatchTabEvent(EventTabbedPanel.TAB_ICON_CHANGE, entry, null);
        return this;
    }

    public TabbedPanel setTooltip(String key, String tooltip) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return this;
        entry.setTooltip(tooltip);

        int index = tabbedPane.indexOfComponent(entry.getComponent());
        if (index >= 0) tabbedPane.setToolTipTextAt(index, tooltip);
        dispatchTabEvent(EventTabbedPanel.TAB_TOOLTIP_CHANGE, entry, null);
        return this;
    }

    public TabbedPanel setBadge(String key, String badge) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return this;
        entry.setBadge(badge);
        updateTabHeader(key);
        dispatchTabEvent(EventTabbedPanel.TAB_BADGE_CHANGE, entry, null);
        return this;
    }

    public TabbedPanel clearBadge(String key) {
        return setBadge(key, null);
    }

    public TabbedPanel setTabDiagnostic(String key, Color color) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return this;
        entry.setDiagnosticColor(color);
        updateTabHeader(key);
        return this;
    }

    public TabbedPanel clearTabDiagnostic(String key) {
        return setTabDiagnostic(key, null);
    }

    public TabbedPanel setPinned(String key, boolean pinned) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null || entry.isPinned() == pinned) return this;
        entry.setPinned(pinned);
        enforcePinnedPlacement(key);
        updateTabHeader(key);
        dispatchTabEvent(EventTabbedPanel.TAB_PINNED_CHANGE, entry, null);
        return this;
    }

    public boolean isPinned(String key) {
        TabEntry entry = tabsByKey.get(key);
        return entry != null && entry.isPinned();
    }

    public TabbedPanel togglePinned(String key) {
        return setPinned(key, !isPinned(key));
    }

    public TabbedPanel setDirty(String key, boolean dirty) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null || entry.isDirty() == dirty) return this;
        entry.setDirty(dirty);
        updateTabHeader(key);
        dispatchTabEvent(EventTabbedPanel.TAB_DIRTY_CHANGE, entry, null);
        return this;
    }

    public boolean isDirty(String key) {
        TabEntry entry = tabsByKey.get(key);
        return entry != null && entry.isDirty();
    }

    public TabbedPanel toggleDirty(String key) {
        return setDirty(key, !isDirty(key));
    }

    public TabbedPanel setClosable(String key, boolean closable) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return this;
        entry.setClosable(closable);
        updateTabHeader(key);
        dispatchTabEvent(EventTabbedPanel.TAB_CLOSABLE_CHANGE, entry, null);
        return this;
    }

    public TabbedPanel setHeaderFactory(String key, TabHeaderFactory headerFactory) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return this;
        entry.setHeaderFactory(headerFactory);
        updateTabHeader(key);
        dispatchTabEvent(EventTabbedPanel.TAB_HEADER_FACTORY_CHANGE, entry, null);
        return this;
    }

    public TabbedPanel clearHeaderFactory(String key) {
        return setHeaderFactory(key, null);
    }

    public TabbedPanel setCloseButtonsVisible(boolean visible) {
        this.closeButtonsVisible = visible;
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabDragEnabled(boolean tabDragEnabled) {
        this.tabDragEnabled = tabDragEnabled;
        updateAllTabHeaders();
        return this;
    }

    public boolean isTabDragEnabled() {
        return tabDragEnabled;
    }

    public TabbedPanel setTabLayoutPolicy(int tabLayoutPolicy) {
        tabbedPane.setTabLayoutPolicy(tabLayoutPolicy);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setScrollableTabsEnabled(boolean enabled) {
        return setTabLayoutPolicy(enabled ? JTabbedPane.SCROLL_TAB_LAYOUT : JTabbedPane.WRAP_TAB_LAYOUT);
    }

    public TabbedPanel setMultiRowTabsEnabled(boolean enabled) {
        return setTabLayoutPolicy(enabled ? JTabbedPane.WRAP_TAB_LAYOUT : JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    public int getTabLayoutPolicy() {
        return tabbedPane.getTabLayoutPolicy();
    }

    public TabbedPanel setSplitTabsOnDragEnabled(boolean enabled) {
        this.splitTabsOnDragEnabled = enabled;
        return this;
    }

    public TabbedPanel setDockModeEnabled(boolean enabled) {
        return setSplitTabsOnDragEnabled(enabled);
    }

    public TabbedPanel setTabSplitEnabled(boolean enabled) {
        return setSplitTabsOnDragEnabled(enabled);
    }

    public TabbedPanel setMultiTabDragEnabled(boolean enabled) {
        return setSplitTabsOnDragEnabled(enabled);
    }

    public boolean isSplitTabsOnDragEnabled() {
        return splitTabsOnDragEnabled;
    }

    public boolean isDockModeEnabled() {
        return isSplitTabsOnDragEnabled();
    }

    public TabbedPanel setSplitDropZoneSize(int pixels) {
        this.splitDropZoneSize = Math.max(32, pixels);
        return this;
    }

    public int getSplitDropZoneSize() {
        return splitDropZoneSize;
    }

    public TabbedPanel setTabGroupFactory(TabGroupFactory tabGroupFactory) {
        this.tabGroupFactory = tabGroupFactory;
        return this;
    }

    public TabbedPanel setTabSeparatorFactory(TabSeparatorFactory tabSeparatorFactory) {
        this.tabSeparatorFactory = tabSeparatorFactory;
        return this;
    }

    public TabSeparatorFactory getTabSeparatorFactory() {
        return tabSeparatorFactory;
    }

    public TabbedPanel setAutoRemoveEmptyTabGroupsEnabled(boolean enabled) {
        this.autoRemoveEmptyTabGroupsEnabled = enabled;
        return this;
    }

    public TabbedPanel setCloseEmptyTabGroupsEnabled(boolean enabled) {
        return setAutoRemoveEmptyTabGroupsEnabled(enabled);
    }

    public boolean isAutoRemoveEmptyTabGroupsEnabled() {
        return autoRemoveEmptyTabGroupsEnabled;
    }

    public TabbedPanel setDragStartThreshold(int pixels) {
        dragController.setStartThreshold(pixels);
        return this;
    }

    public int getDragStartThreshold() {
        return dragController.getStartThreshold();
    }

    public TabbedPanel setDragAnimationEnabled(boolean dragAnimationEnabled) {
        dragController.setAnimationEnabled(dragAnimationEnabled);
        return this;
    }

    public boolean isDragAnimationEnabled() {
        return dragController.isAnimationEnabled();
    }

    public TabbedPanel setDragTabHeaderBackground(Color color) {
        dragController.setGhostBorderColor(color);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setDragGhostAlpha(float alpha) {
        dragController.setGhostAlpha(alpha);
        return this;
    }

    public TabbedPanel setDetachedTabPreviewEnabled(boolean enabled) {
        dragController.setDetachedPreviewEnabled(enabled);
        return this;
    }

    public boolean isDetachedTabPreviewEnabled() {
        return dragController.isDetachedPreviewEnabled();
    }

    public TabbedPanel setDetachedTabPreviewSize(Dimension size) {
        dragController.setDetachedPreviewSize(size);
        return this;
    }

    public Dimension getDetachedTabPreviewSize() {
        return dragController.getDetachedPreviewSize();
    }

    public TabbedPanel setDetachedTabPreviewAlpha(float alpha) {
        dragController.setDetachedPreviewAlpha(alpha);
        return this;
    }

    public float getDetachedTabPreviewAlpha() {
        return dragController.getDetachedPreviewAlpha();
    }

    public TabbedPanel setReorderTabsWhileDragging(boolean enabled) {
        dragController.setReorderWhileDragging(enabled);
        return this;
    }

    public TabbedPanel setSortTabsOnDropOnly(boolean enabled) {
        dragController.setReorderWhileDragging(!enabled);
        return this;
    }

    public boolean isReorderTabsWhileDragging() {
        return dragController.isReorderWhileDragging();
    }

    public TabbedPanel setSplitPreviewEnabled(boolean enabled) {
        dragController.setSplitPreviewEnabled(enabled);
        return this;
    }

    public boolean isSplitPreviewEnabled() {
        return dragController.isSplitPreviewEnabled();
    }

    public TabbedPanel setSplitPreviewColor(Color color) {
        dragController.setSplitPreviewColor(color);
        return this;
    }

    public TabbedPanel setSplitPreviewAlpha(float alpha) {
        dragController.setSplitPreviewAlpha(alpha);
        return this;
    }

    public TabbedPanel setTabMenuProvider(TabMenuProvider tabMenuProvider) {
        this.tabMenuProvider = tabMenuProvider;
        return this;
    }

    public TabbedPanel setTabHeaderFactory(TabHeaderFactory tabHeaderFactory) {
        this.tabHeaderFactory = tabHeaderFactory;
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel clearTabHeaderFactory() {
        return setTabHeaderFactory(null);
    }

    public TabbedPanel setDefaultTabIcon(Icon icon) {
        tabStyle.setDefaultTabIcon(icon);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setDefaultSelectedTabIcon(Icon icon) {
        tabStyle.setDefaultSelectedTabIcon(icon);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setDefaultTabIcons(Icon icon, Icon selectedIcon) {
        tabStyle.setDefaultTabIcon(icon);
        tabStyle.setDefaultSelectedTabIcon(selectedIcon);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setDragAnimator(ComponentAnimator<JComponent> dragAnimator) {
        dragController.setAnimator(dragAnimator);
        return this;
    }

    public TabbedPanel setDefaultTabMenuEnabled(boolean enabled) {
        this.defaultTabMenuEnabled = enabled;
        return this;
    }

    public boolean isDefaultTabMenuEnabled() {
        return defaultTabMenuEnabled;
    }

    public TabbedPanel setTabMenuEnabled(boolean enabled) {
        this.tabMenuEnabled = enabled;
        return this;
    }

    public boolean isTabMenuEnabled() {
        return tabMenuEnabled;
    }

    public TabbedPanel setRenameOnDoubleClickEnabled(boolean enabled) {
        this.renameOnDoubleClickEnabled = enabled;
        return this;
    }

    public boolean isRenameOnDoubleClickEnabled() {
        return renameOnDoubleClickEnabled;
    }

    public TabbedPanel setCloseOnMiddleClickEnabled(boolean enabled) {
        this.closeOnMiddleClickEnabled = enabled;
        return this;
    }

    public boolean isCloseOnMiddleClickEnabled() {
        return closeOnMiddleClickEnabled;
    }

    public TabbedPanel setNewTabButtonVisible(boolean visible) {
        this.newTabButtonVisible = visible;
        updateTabControlBar();
        return this;
    }

    public boolean isNewTabButtonVisible() {
        return newTabButtonVisible;
    }

    public TabbedPanel setNewTabAction(Runnable action) {
        this.newTabAction = action;
        return this;
    }

    public TabbedPanel setTabListButtonVisible(boolean visible) {
        this.tabListButtonVisible = visible;
        updateTabControlBar();
        return this;
    }

    public boolean isTabListButtonVisible() {
        return tabListButtonVisible;
    }

    public TabbedPanel setMruSwitchingEnabled(boolean enabled) {
        this.mruSwitchingEnabled = enabled;
        return this;
    }

    public boolean isMruSwitchingEnabled() {
        return mruSwitchingEnabled;
    }

    public TabbedPanel setPinnedTabsShowTitle(boolean showTitle) {
        this.pinnedTabsShowTitle = showTitle;
        updateAllTabHeaders();
        return this;
    }

    public boolean isPinnedTabsShowTitle() {
        return pinnedTabsShowTitle;
    }

    public TabbedPanel setCloseConfirmationProvider(BiPredicate<TabbedPanel, TabEntry> provider) {
        this.closeConfirmationProvider = provider;
        return this;
    }

    public TabbedPanel setCloseButtonText(String closeButtonText) {
        tabStyle.setCloseButtonText(closeButtonText);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setCloseButtonTooltip(String closeButtonTooltip) {
        tabStyle.setCloseButtonTooltip(closeButtonTooltip);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabTitleForeground(Color color) {
        tabStyle.setTabTitleForeground(color);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setSelectedTabTitleForeground(Color color) {
        tabStyle.setSelectedTabTitleForeground(color);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabHeaderBackground(Color color) {
        tabStyle.setTabHeaderBackground(color);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setSelectedTabHeaderBackground(Color color) {
        tabStyle.setSelectedTabHeaderBackground(color);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabBadgeBackground(Color color) {
        tabStyle.setTabBadgeBackground(color);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabBadgeForeground(Color color) {
        tabStyle.setTabBadgeForeground(color);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabTitleFont(Font font) {
        tabStyle.setTabTitleFont(font);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setSelectedTabTitleFont(Font font) {
        tabStyle.setSelectedTabTitleFont(font);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabBadgeFont(Font font) {
        tabStyle.setTabBadgeFont(font);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabHeaderPadding(Insets padding) {
        tabStyle.setTabHeaderPadding(padding);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabHeaderGap(int gap) {
        tabStyle.setTabHeaderGap(gap);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabBadgeArc(int arc) {
        tabStyle.setTabBadgeArc(arc);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel setTabStyle(Color titleForeground, Color selectedTitleForeground, Color headerBackground, Color selectedHeaderBackground) {
        tabStyle.setTabTitleForeground(titleForeground);
        tabStyle.setSelectedTabTitleForeground(selectedTitleForeground);
        tabStyle.setTabHeaderBackground(headerBackground);
        tabStyle.setSelectedTabHeaderBackground(selectedHeaderBackground);
        updateAllTabHeaders();
        return this;
    }

    public TabbedPanel onTabEvent(String eventType, Consumer<TabEvent> listener) {
        Objects.requireNonNull(listener, "listener");
        addEventListener(eventType, event -> {
            if (event instanceof TabEvent tabEvent) {
                listener.accept(tabEvent);
            }
        });
        return this;
    }

    public TabbedPanel onTabAdd(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_ADD, listener);
    }

    public TabbedPanel onBeforeTabClose(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.BEFORE_TAB_CLOSE, listener);
    }

    public TabbedPanel onTabClose(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_CLOSE, listener);
    }

    public TabbedPanel onBeforeTabRemove(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.BEFORE_TAB_REMOVE, listener);
    }

    public TabbedPanel onTabRemove(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_REMOVE, listener);
    }

    public TabbedPanel onBeforeTabMove(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.BEFORE_TAB_MOVE, listener);
    }

    public TabbedPanel onTabMove(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_MOVE, listener);
    }

    public TabbedPanel onTabDrag(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_DRAG, listener);
    }

    public TabbedPanel onBeforeTabSplit(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.BEFORE_TAB_SPLIT, listener);
    }

    public TabbedPanel onTabSplit(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_SPLIT, listener);
    }

    public TabbedPanel onTabMenu(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_MENU, listener);
    }

    public TabbedPanel onTabTitleChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_TITLE_CHANGE, listener);
    }

    public TabbedPanel onTabIconChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_ICON_CHANGE, listener);
    }

    public TabbedPanel onTabTooltipChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_TOOLTIP_CHANGE, listener);
    }

    public TabbedPanel onTabClosableChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_CLOSABLE_CHANGE, listener);
    }

    public TabbedPanel onTabBadgeChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_BADGE_CHANGE, listener);
    }

    public TabbedPanel onTabPinnedChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_PINNED_CHANGE, listener);
    }

    public TabbedPanel onTabDirtyChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_DIRTY_CHANGE, listener);
    }

    public TabbedPanel onTabHeaderFactoryChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventTabbedPanel.TAB_HEADER_FACTORY_CHANGE, listener);
    }

    public TabbedPanel onBeforeSelectionChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventType.BEFORE_CHANGE, listener);
    }

    public TabbedPanel onSelectionChange(Consumer<TabEvent> listener) {
        return onTabEvent(EventType.CHANGE, listener);
    }

    public boolean switchLastUsed() {
        if (!mruSwitchingEnabled || currentKey == null) return false;
        for (String key : new ArrayList<>(mruKeys)) {
            if (!Objects.equals(key, currentKey) && tabsByKey.containsKey(key)) {
                switchTo(key);
                return true;
            }
        }
        return false;
    }

    public void beginRename(String key) {
        if (!renameOnDoubleClickEnabled) return;
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return;
        int index = indexOf(key);
        if (index < 0) return;

        JTextField field = new JTextField(entry.getTitle());
        field.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        field.selectAll();
        tabbedPane.setTabComponentAt(index, field);
        boolean[] canceled = {false};

        Runnable commit = () -> {
            if (canceled[0]) return;
            String title = field.getText();
            setTitle(key, title == null || title.isBlank() ? entry.getTitle() : title.trim());
        };
        field.addActionListener(e -> commit.run());
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                commit.run();
            }
        });
        field.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "cancelRename");
        field.getActionMap().put("cancelRename", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                canceled[0] = true;
                updateTabHeader(key);
            }
        });
        SwingUtilities.invokeLater(field::requestFocusInWindow);
    }

    void handleHeaderMiddleClick(TabEntry entry) {
        if (entry != null && closeOnMiddleClickEnabled) {
            closeTab(entry.getKey());
        }
    }

    void handleHeaderDoubleClick(TabEntry entry) {
        if (entry != null && renameOnDoubleClickEnabled) {
            beginRename(entry.getKey());
        }
    }

    private void showTabListMenu() {
        JPopupMenu menu = new JPopupMenu();
        for (TabEntry entry : tabsByKey.values()) {
            String title = formatMenuTitle(entry);
            JMenuItem item = new JMenuItem(title, entry.getIcon());
            item.addActionListener(e -> switchTo(entry.getKey()));
            menu.add(item);
        }
        if (menu.getComponentCount() == 0) {
            JMenuItem empty = new JMenuItem("(sem abas)");
            empty.setEnabled(false);
            menu.add(empty);
        }
        menu.show(tabListButton, 0, tabListButton.getHeight());
    }

    private String formatMenuTitle(TabEntry entry) {
        StringBuilder title = new StringBuilder();
        if (entry.isPinned()) title.append("[fixa] ");
        if (entry.isDirty()) title.append("* ");
        title.append(entry.getTitle());
        return title.toString();
    }

    public Component getCurrent() {
        TabbedPanel focusedGroup = getFocusedTabGroup();

        String key = focusedGroup.currentKey;
        TabEntry entry = key == null
                ? null
                : focusedGroup.tabsByKey.get(key);

        return entry == null ? null : entry.getComponent();
    }

    public List<TabEntry> getVisibleTabs() {
        List<TabEntry> visible = new ArrayList<>();
        for (TabbedPanel group : resolveGroupsForVisibility()) {
            TabEntry entry = group.getCurrentEntry();
            if (entry != null) visible.add(entry);
        }
        return visible;
    }

    public List<String> getVisibleTabKeys() {
        List<String> keys = new ArrayList<>();
        for (TabEntry entry : getVisibleTabs()) {
            keys.add(entry.getKey());
        }
        return keys;
    }

    public List<Component> getVisibleTabComponents() {
        List<Component> components = new ArrayList<>();
        for (TabEntry entry : getVisibleTabs()) {
            components.add(entry.getComponent());
        }
        return components;
    }

    public boolean isTabVisible(String key) {
        if (key == null) return false;
        for (TabEntry entry : getVisibleTabs()) {
            if (key.equals(entry.getKey())) return true;
        }
        return false;
    }

    private TabEntry getCurrentEntry() {
        String key = currentKey;
        return key == null ? null : tabsByKey.get(key);
    }

    private List<TabbedPanel> resolveGroupsForVisibility() {
        List<TabbedPanel> groups = getDockGroups();
        if (groups.isEmpty()) return List.of(this);
        if (!groups.contains(this)) {
            List<TabbedPanel> combined = new ArrayList<>(groups.size() + 1);
            combined.add(this);
            combined.addAll(groups);
            return combined;
        }
        return groups;
    }

    private TabbedPanel resolveInsertionTarget() {
        if (this != resolveDockOwner() || SwingUtilities.getRootPane(this) != null) {
            return this;
        }

        List<TabbedPanel> groups = getDockGroups();
        if (groups.isEmpty()) {
            return this;
        }

        Component focusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (focusOwner != null) {
            for (TabbedPanel group : groups) {
                if (focusOwner == group || SwingUtilities.isDescendingFrom(focusOwner, group)) {
                    return group;
                }
            }
        }

        for (TabbedPanel group : groups) {
            if (group != this && group.getParent() != null) {
                return group;
            }
        }
        return this;
    }

    private TabbedPanel resolveGroupForKey(String key) {
        if (key == null) {
            return null;
        }
        if (tabsByKey.containsKey(key)) {
            return this;
        }
        if (this != resolveDockOwner()) {
            return null;
        }

        for (TabbedPanel group : getDockGroups()) {
            if (group != this && group.tabsByKey.containsKey(key)) {
                return group;
            }
        }
        return null;
    }

    private TabbedPanel resolveGroupForComponent(Component component) {
        if (component == null) {
            return null;
        }
        if (keyByComponent.containsKey(component)) {
            return this;
        }
        if (this != resolveDockOwner()) {
            return null;
        }

        for (TabbedPanel group : getDockGroups()) {
            if (group != this && group.keyByComponent.containsKey(component)) {
                return group;
            }
        }
        return null;
    }

    private boolean containsLocal(String key) {
        return key != null && tabsByKey.containsKey(key);
    }

    private boolean containsLocal(Component component) {
        return component != null && keyByComponent.containsKey(component);
    }

    private TabbedPanel resolveDockOwner() {
        return dockOwner == null ? this : dockOwner;
    }

    public Component find(String key) {
        TabbedPanel group = resolveGroupForKey(key);
        TabEntry entry = group == null ? null : group.tabsByKey.get(key);
        return entry == null ? null : entry.getComponent();
    }

    public boolean contains(String key) {
        return resolveGroupForKey(key) != null;
    }

    public boolean contains(Component component) {
        return resolveGroupForComponent(component) != null;
    }

    public TabEntry getEntry(String key) {
        TabbedPanel group = resolveGroupForKey(key);
        return group == null ? null : group.tabsByKey.get(key);
    }

    public List<TabEntry> getEntries() {
        return new ArrayList<>(tabsByKey.values());
    }

    public String getKeyOf(Component component) {
        TabbedPanel group = resolveGroupForComponent(component);
        return group == null ? null : group.keyByComponent.get(component);
    }

    public Set<String> getKeys() {
        if (this != resolveDockOwner()) {
            return tabsByKey.keySet();
        }

        Set<String> keys = new java.util.LinkedHashSet<>();
        for (TabbedPanel group : resolveGroupsForVisibility()) {
            keys.addAll(group.tabsByKey.keySet());
        }
        return keys;
    }

    public Collection<Component> getTabs() {
        List<Component> components = new ArrayList<>();
        for (TabEntry entry : tabsByKey.values()) {
            components.add(entry.getComponent());
        }
        return components;
    }

    public int getTabCount() {
        return tabsByKey.size();
    }

    public boolean isEmpty() {
        return tabsByKey.isEmpty();
    }

    public int indexOf(String key) {
        TabEntry entry = tabsByKey.get(key);
        return entry == null ? -1 : tabbedPane.indexOfComponent(entry.getComponent());
    }

    int resolveDropIndex(Point point) {
        int tabCount = tabbedPane.getTabCount();
        if (point == null || tabCount <= 0) return -1;

        int directIndex = tabbedPane.indexAtLocation(point.x, point.y);
        if (directIndex >= 0) return directIndex;

        int placement = tabbedPane.getTabPlacement();
        boolean vertical = placement == JTabbedPane.LEFT || placement == JTabbedPane.RIGHT;
        int coordinate = vertical ? point.y : point.x;
        int firstIndex = -1;
        int lastIndex = -1;
        int firstStart = Integer.MAX_VALUE;
        int lastEnd = Integer.MIN_VALUE;
        int nearestIndex = -1;
        int nearestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < tabCount; i++) {
            Rectangle bounds = tabbedPane.getBoundsAt(i);
            if (bounds == null) continue;

            int start = vertical ? bounds.y : bounds.x;
            int size = vertical ? bounds.height : bounds.width;
            int end = start + size;

            if (start < firstStart) {
                firstStart = start;
                firstIndex = i;
            }
            if (end > lastEnd) {
                lastEnd = end;
                lastIndex = i;
            }
            if (coordinate >= start && coordinate <= end) return i;

            int distance = coordinate < start ? start - coordinate : Math.max(0, coordinate - end);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestIndex = i;
            }
        }

        if (firstIndex >= 0 && coordinate < firstStart) return firstIndex;
        if (lastIndex >= 0 && coordinate > lastEnd) return lastIndex;
        return nearestIndex;
    }

    public JComponent createDefaultHeader(TabEntry entry, boolean selected) {
        return headerRenderer.createHeader(this, entry, selected);
    }

    protected JComponent createTabHeader(TabEntry entry, boolean selected) {
        TabHeaderFactory factory = entry.getHeaderFactory() != null ? entry.getHeaderFactory() : tabHeaderFactory;
        if (factory != null) {
            JComponent header = factory.createHeader(this, entry, selected);
            if (header != null) return header;
        }
        return createDefaultHeader(entry, selected);
    }

    void showTabMenu(TabEntry entry, MouseEvent event) {
        if (!tabMenuEnabled) return;

        JPopupMenu menu = null;
        if (tabMenuProvider != null) {
            menu = tabMenuProvider.createMenu(this, entry, event);
        } else if (defaultTabMenuEnabled) {
            menu = defaultTabMenuFactory.create(this, entry);
        }

        if (menu != null && menu.getComponentCount() > 0) {
            dispatchTabEvent(EventTabbedPanel.TAB_MENU, entry, null);
            menu.show(event.getComponent(), event.getX(), event.getY());
        }
    }

    TabSplitPlacement resolveSplitPlacement(Point point) {
        if (!splitTabsOnDragEnabled || point == null || !hasEnoughTabsToSplit()) return null;

        Rectangle content = getContentBoundsForSplit();
        if (content == null || content.width <= 0 || content.height <= 0) return null;

        int zone = Math.min(splitDropZoneSize, Math.max(32, Math.min(content.width, content.height) / 3));
        boolean insideVerticalContent = point.y >= content.y && point.y <= content.y + content.height;
        boolean insideHorizontalContent = point.x >= content.x && point.x <= content.x + content.width;

        if (insideVerticalContent && point.x <= content.x + zone) return TabSplitPlacement.LEFT;
        if (insideVerticalContent && point.x >= content.x + content.width - zone) return TabSplitPlacement.RIGHT;
        if (insideHorizontalContent && point.y >= content.y + content.height - zone) return TabSplitPlacement.BOTTOM;
        if (insideHorizontalContent && point.y >= content.y && point.y <= content.y + zone) return TabSplitPlacement.TOP;

        if (point.y > content.y + content.height) return TabSplitPlacement.BOTTOM;
        if (point.x < content.x) return TabSplitPlacement.LEFT;
        if (point.x > content.x + content.width) return TabSplitPlacement.RIGHT;
        return null;
    }

    private boolean hasEnoughTabsToSplit() {
        return tabbedPane.getTabCount() > 1;
    }

    Rectangle getSplitPreviewBounds(TabSplitPlacement placement) {
        Rectangle content = getContentBoundsForSplit();
        if (content == null || placement == null) return null;

        int zone = Math.min(splitDropZoneSize, Math.max(32, Math.min(content.width, content.height) / 3));
        if (placement == TabSplitPlacement.LEFT) {
            return new Rectangle(content.x, content.y, zone, content.height);
        }
        if (placement == TabSplitPlacement.RIGHT) {
            return new Rectangle(content.x + content.width - zone, content.y, zone, content.height);
        }
        if (placement == TabSplitPlacement.TOP) {
            return new Rectangle(content.x, content.y, content.width, zone);
        }
        return new Rectangle(content.x, content.y + content.height - zone, content.width, zone);
    }

    protected TabbedPanel createSplitTabGroup(TabEntry entry, TabSplitPlacement placement) {
        TabbedPanel target = tabGroupFactory == null ? null : tabGroupFactory.createGroup(this, entry, placement);
        if (target == null) {
            target = new TabbedPanel(tabbedPane.getTabPlacement());
        }
        copyConfigurationTo(target);
        return target;
    }

    protected void copyConfigurationTo(TabbedPanel target) {
        tabStyle.copyTo(target.tabStyle);
        target.dockOwner = resolveDockOwner();
        target.closeButtonsVisible = closeButtonsVisible;
        target.tabDragEnabled = tabDragEnabled;
        target.tabMenuEnabled = tabMenuEnabled;
        target.defaultTabMenuEnabled = defaultTabMenuEnabled;
        target.splitTabsOnDragEnabled = splitTabsOnDragEnabled;
        target.autoRemoveEmptyTabGroupsEnabled = autoRemoveEmptyTabGroupsEnabled;
        target.renameOnDoubleClickEnabled = renameOnDoubleClickEnabled;
        target.closeOnMiddleClickEnabled = closeOnMiddleClickEnabled;
        target.newTabButtonVisible = newTabButtonVisible;
        target.tabListButtonVisible = tabListButtonVisible;
        target.mruSwitchingEnabled = mruSwitchingEnabled;
        target.pinnedTabsShowTitle = pinnedTabsShowTitle;
        target.tabWindowEnabled = tabWindowEnabled;
        target.tabWindowAlwaysOnTop = tabWindowAlwaysOnTop;
        target.defaultTabWindowSize = defaultTabWindowSize;
        target.newTabAction = newTabAction;
        target.closeConfirmationProvider = closeConfirmationProvider;
        target.updateTabControlBar();
        target.splitDropZoneSize = splitDropZoneSize;
        target.tabHeaderFactory = tabHeaderFactory;
        target.tabMenuProvider = tabMenuProvider;
        target.tabGroupFactory = tabGroupFactory;
        target.tabSeparatorFactory = tabSeparatorFactory;
        target.dockRootPane = dockRootPane;
        target.tabbedPane.setTabLayoutPolicy(tabbedPane.getTabLayoutPolicy());
        target.dragController.setStartThreshold(dragController.getStartThreshold());
        target.dragController.setAnimationEnabled(dragController.isAnimationEnabled());
        target.dragController.setGhostAlpha(dragController.getGhostAlpha());
        target.dragController.setGhostBorderColor(dragController.getGhostBorderColor());
        target.dragController.setDetachedPreviewEnabled(dragController.isDetachedPreviewEnabled());
        target.dragController.setDetachedPreviewSize(dragController.getDetachedPreviewSize());
        target.dragController.setDetachedPreviewAlpha(dragController.getDetachedPreviewAlpha());
        target.dragController.setAnimator(dragController.getAnimator());
        target.dragController.setReorderWhileDragging(dragController.isReorderWhileDragging());
        target.dragController.setSplitPreviewEnabled(dragController.isSplitPreviewEnabled());
        target.dragController.setSplitPreviewColor(dragController.getSplitPreviewColor());
        target.dragController.setSplitPreviewAlpha(dragController.getSplitPreviewAlpha());
    }

    private Rectangle getContentBoundsForSplit() {
        Component selected = tabbedPane.getSelectedComponent();
        if (selected != null) {
            Rectangle bounds = selected.getBounds();
            if (bounds.width > 0 && bounds.height > 0) return bounds;
        }

        Insets insets = tabbedPane.getInsets();
        return new Rectangle(
                insets.left,
                insets.top + 32,
                Math.max(0, tabbedPane.getWidth() - insets.left - insets.right),
                Math.max(0, tabbedPane.getHeight() - insets.top - insets.bottom - 32)
        );
    }

    private TabConfig createTransferConfig(TabEntry entry) {
        return new TabConfig(entry.getKey(), entry.getTitle(), entry.getComponent())
                .icon(entry.getIcon())
                .selectedIcon(entry.getSelectedIcon())
                .closable(entry.isClosable())
                .tooltip(entry.getTooltip())
                .badge(entry.getBadge())
                .headerFactory(entry.getHeaderFactory())
                .pinned(entry.isPinned())
                .dirty(entry.isDirty())
                .titleForeground(entry.getTitleForeground())
                .selectedTitleForeground(entry.getSelectedTitleForeground());
    }

    private TabbedPanel resolveTransferTarget(Point point) {
        if (!splitTabsOnDragEnabled || point == null) return null;

        JRootPane rootPane = resolveDockRootPane();
        if (rootPane == null) return null;

        Point rootPoint = SwingUtilities.convertPoint(tabbedPane, point, rootPane);
        for (TabbedPanel candidate : collectTabbedPanels(rootPane.getContentPane())) {
            if (candidate == this || !candidate.isShowing() || !candidate.isTabDragEnabled()) continue;

            Container parent = candidate.getParent();
            if (parent == null) continue;

            Rectangle bounds = SwingUtilities.convertRectangle(parent, candidate.getBounds(), rootPane);
            if (bounds.contains(rootPoint)) return candidate;
        }
        return null;
    }

    private JRootPane resolveDockRootPane() {
        JRootPane rootPane = SwingUtilities.getRootPane(this);
        if (rootPane != null) {
            dockRootPane = rootPane;
            return rootPane;
        }
        return dockRootPane;
    }

    private List<TabbedPanel> collectTabbedPanels(Component component) {
        List<TabbedPanel> result = new ArrayList<>();
        collectTabbedPanels(component, result);
        return result;
    }

    private void collectTabbedPanels(Component component, List<TabbedPanel> result) {
        if (component == null) return;
        if (component instanceof TabbedPanel tabbedPanel) {
            result.add(tabbedPanel);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectTabbedPanels(child, result);
            }
        }
    }

    private boolean installSplitGroup(TabbedPanel target, TabSplitPlacement placement) {
        Container parent = getParent();
        if (parent == null) return false;

        JRootPane rootPane = resolveDockRootPane();
        if (rootPane != null) {
            dockRootPane = rootPane;
            target.dockRootPane = rootPane;
        }
        if (rootPane != null && rootPane.getContentPane() == this) {
            JSplitPane split = createSplitPane(target, placement);
            rootPane.setContentPane(split);
            rootPane.revalidate();
            rootPane.repaint();
            return true;
        }

        if (parent instanceof JSplitPane parentSplit) {
            boolean left = parentSplit.getLeftComponent() == this;
            boolean right = parentSplit.getRightComponent() == this;
            boolean top = parentSplit.getTopComponent() == this;
            boolean bottom = parentSplit.getBottomComponent() == this;
            if (!left && !right && !top && !bottom) return false;

            int dividerLocation = parentSplit.getDividerLocation();
            JSplitPane split = createSplitPane(target, placement);
            if (left) {
                parentSplit.setLeftComponent(split);
            } else if (right) {
                parentSplit.setRightComponent(split);
            } else if (top) {
                parentSplit.setTopComponent(split);
            } else {
                parentSplit.setBottomComponent(split);
            }
            parentSplit.setDividerLocation(dividerLocation);
            parentSplit.revalidate();
            parentSplit.repaint();
            return true;
        }

        if (parent instanceof JTabbedPane parentTabs) {
            int tabIndex = parentTabs.indexOfComponent(this);
            if (tabIndex < 0) return false;

            String tabTitle = parentTabs.getTitleAt(tabIndex);
            Icon tabIcon = parentTabs.getIconAt(tabIndex);
            Icon disabledIcon = parentTabs.getDisabledIconAt(tabIndex);
            String tabTooltip = parentTabs.getToolTipTextAt(tabIndex);
            Color tabBackground = parentTabs.getBackgroundAt(tabIndex);
            Color tabForeground = parentTabs.getForegroundAt(tabIndex);
            boolean enabled = parentTabs.isEnabledAt(tabIndex);
            int mnemonic = parentTabs.getMnemonicAt(tabIndex);
            int displayedMnemonicIndex = parentTabs.getDisplayedMnemonicIndexAt(tabIndex);
            Component tabHeader = parentTabs.getTabComponentAt(tabIndex);
            boolean selected = parentTabs.getSelectedIndex() == tabIndex;

            JSplitPane split = createSplitPane(target, placement);
            int insertionIndex = parentTabs.indexOfComponent(this);
            if (insertionIndex >= 0) {
                parentTabs.removeTabAt(insertionIndex);
            } else {
                insertionIndex = Math.min(tabIndex, parentTabs.getTabCount());
            }

            parentTabs.insertTab(tabTitle, tabIcon, split, tabTooltip, insertionIndex);
            if (disabledIcon != null) parentTabs.setDisabledIconAt(insertionIndex, disabledIcon);
            if (tabBackground != null) parentTabs.setBackgroundAt(insertionIndex, tabBackground);
            if (tabForeground != null) parentTabs.setForegroundAt(insertionIndex, tabForeground);
            parentTabs.setEnabledAt(insertionIndex, enabled);
            if (mnemonic > 0) parentTabs.setMnemonicAt(insertionIndex, mnemonic);
            if (displayedMnemonicIndex >= 0) parentTabs.setDisplayedMnemonicIndexAt(insertionIndex, displayedMnemonicIndex);
            if (tabHeader != null) parentTabs.setTabComponentAt(insertionIndex, tabHeader);
            if (selected) parentTabs.setSelectedIndex(insertionIndex);

            parentTabs.revalidate();
            parentTabs.repaint();
            return true;
        }

        LayoutManager layout = parent.getLayout();
        int index = parent.getComponentZOrder(this);
        Object constraints = getLayoutConstraints(parent, layout);
        Integer layer = parent instanceof JLayeredPane layeredPane ? layeredPane.getLayer(this) : null;
        int position = parent instanceof JLayeredPane layeredPane ? layeredPane.getPosition(this) : -1;
        parent.remove(this);
        JSplitPane split = createSplitPane(target, placement);

        if (layout instanceof BorderLayout) {
            parent.add(split, constraints == null ? BorderLayout.CENTER : constraints);
        } else if (layout instanceof GridBagLayout) {
            parent.add(split, constraints);
        } else if (parent instanceof JLayeredPane && layer != null) {
            parent.add(split, layer, position);
        } else if (index >= 0) {
            parent.add(split, Math.min(index, parent.getComponentCount()));
        } else {
            parent.add(split);
        }
        parent.revalidate();
        parent.repaint();
        return true;
    }

    private JSplitPane createSplitPane(TabbedPanel target, TabSplitPlacement placement) {
        boolean horizontal = placement == TabSplitPlacement.LEFT || placement == TabSplitPlacement.RIGHT;
        int orientation = horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;
        JSplitPane split = tabSeparatorFactory == null
                ? null
                : tabSeparatorFactory.createSeparator(this, target, placement, orientation);
        if (split == null) {
            split = new JSplitPane(orientation);
            split.setContinuousLayout(true);
            split.setResizeWeight(0.5);
            split.setBorder(null);
        } else {
            split.setOrientation(orientation);
        }

        if (placement == TabSplitPlacement.LEFT) {
            split.setLeftComponent(target);
            split.setRightComponent(this);
        } else if (placement == TabSplitPlacement.RIGHT) {
            split.setLeftComponent(this);
            split.setRightComponent(target);
        } else if (placement == TabSplitPlacement.TOP) {
            split.setTopComponent(target);
            split.setBottomComponent(this);
        } else {
            split.setTopComponent(this);
            split.setBottomComponent(target);
        }
        return split;
    }

    private Object getLayoutConstraints(Container parent, LayoutManager layout) {
        return getLayoutConstraints(parent, layout, this);
    }

    private Object getLayoutConstraints(Container parent, LayoutManager layout, Component component) {
        if (layout instanceof BorderLayout borderLayout) {
            return borderLayout.getConstraints(component);
        }
        if (layout instanceof GridBagLayout gridBagLayout) {
            return gridBagLayout.getConstraints(component);
        }
        return null;
    }

    private boolean collapseIfEmptyTabGroup() {
        if (!autoRemoveEmptyTabGroupsEnabled || getTabCount() > 0) return false;

        Container parent = getParent();
        if (!(parent instanceof JSplitPane splitPane)) return false;

        Component left = splitPane.getLeftComponent();
        Component right = splitPane.getRightComponent();
        Component sibling = left == this ? right : left;

        if (sibling == null || sibling == this) return false;

        Container grandParent = splitPane.getParent();
        if (grandParent == null) return false;

        JRootPane rootPane = SwingUtilities.getRootPane(splitPane);

        if (rootPane != null && rootPane.getContentPane() == splitPane && sibling instanceof Container siblingContainer) {
            splitPane.remove(sibling);
            rootPane.setContentPane(siblingContainer);
            rootPane.revalidate();
            rootPane.repaint();
            return true;
        }

        if (grandParent instanceof JSplitPane grandSplit) {
            splitPane.remove(sibling);

            if (grandSplit.getLeftComponent() == splitPane) {
                grandSplit.setLeftComponent(sibling);
            } else if (grandSplit.getRightComponent() == splitPane) {
                grandSplit.setRightComponent(sibling);
            } else if (grandSplit.getTopComponent() == splitPane) {
                grandSplit.setTopComponent(sibling);
            } else if (grandSplit.getBottomComponent() == splitPane) {
                grandSplit.setBottomComponent(sibling);
            } else {
                return false;
            }

            grandSplit.revalidate();
            grandSplit.repaint();
            return true;
        }

        if (grandParent instanceof JTabbedPane parentTabs) {
            int tabIndex = parentTabs.indexOfComponent(splitPane);
            if (tabIndex < 0) return false;

            boolean selected = parentTabs.getSelectedIndex() == tabIndex;

            splitPane.remove(sibling);
            parentTabs.setComponentAt(tabIndex, sibling);

            if (selected) {
                parentTabs.setSelectedIndex(tabIndex);
            }

            parentTabs.revalidate();
            parentTabs.repaint();

            Component top = parentTabs.getTopLevelAncestor();
            if (top instanceof Window window) {
                window.validate();
                window.repaint();
            }

            return true;
        }

        LayoutManager layout = grandParent.getLayout();
        int index = grandParent.getComponentZOrder(splitPane);
        Object constraints = getLayoutConstraints(grandParent, layout, splitPane);
        Integer layer = grandParent instanceof JLayeredPane layeredPane ? layeredPane.getLayer(splitPane) : null;
        int position = grandParent instanceof JLayeredPane layeredPane ? layeredPane.getPosition(splitPane) : -1;

        splitPane.remove(sibling);
        grandParent.remove(splitPane);

        if (layout instanceof BorderLayout) {
            grandParent.add(sibling, constraints == null ? BorderLayout.CENTER : constraints);
        } else if (layout instanceof GridBagLayout) {
            grandParent.add(sibling, constraints);
        } else if (grandParent instanceof JLayeredPane && layer != null) {
            grandParent.add(sibling, layer, position);
        } else if (index >= 0) {
            grandParent.add(sibling, Math.max(0, Math.min(index, grandParent.getComponentCount())));
        } else {
            grandParent.add(sibling);
        }

        grandParent.revalidate();
        grandParent.repaint();
        return true;
    }

    protected TabEvent dispatchTabEvent(String eventType, TabEntry entry, Map<String, Object> extraProps) {
        Map<String, Object> props = new HashMap<>();
        if (entry != null) {
            props.put("key", entry.getKey());
            props.put("title", entry.getTitle());
            props.put("component", entry.getComponent());
            props.put("closable", entry.isClosable());
            props.put("pinned", entry.isPinned());
            props.put("dirty", entry.isDirty());
            props.put("index", tabbedPane.indexOfComponent(entry.getComponent()));
        }
        if (extraProps != null) props.putAll(extraProps);

        TabEvent event = new TabEvent(this, entry, eventType, props);
        dispatchEventObject(eventType, event);
        TabbedPanel owner = resolveDockOwner();
        if (owner != this) {
            owner.dispatchEventObject(eventType, event);
        }
        return event;
    }

    protected void dispatchEventObject(String eventType, TabEvent event) {
        List<Consumer<EventComponent>> eventListeners = listeners.get(eventType);
        if (eventListeners == null || eventListeners.isEmpty()) return;

        for (Consumer<EventComponent> listener : eventListeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void updateAllTabHeaders() {
        for (String key : new ArrayList<>(tabsByKey.keySet())) {
            updateTabHeader(key);
        }
    }

    protected void updateTabHeader(String key) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return;

        int index = tabbedPane.indexOfComponent(entry.getComponent());
        if (index < 0) return;

        tabbedPane.setTitleAt(index, entry.getTitle());
        tabbedPane.setIconAt(index, entry.getIcon());
        tabbedPane.setToolTipTextAt(index, entry.getTooltip());

        JComponent header = createTabHeader(entry, Objects.equals(currentKey, key));

        installTabHeaderHover(header, header, entry, new boolean[]{false});

        dragController.install(header, entry);
        tabbedPane.setTabComponentAt(index, header);
    }

    private void installSelectionListener() {
        ChangeListener listener = e -> {
            if (suppressSelectionEvent) return;

            Component selected = tabbedPane.getSelectedComponent();
            String newKey = selected == null ? null : keyByComponent.get(selected);
            if (Objects.equals(currentKey, newKey)) return;

            String oldKey = currentKey;
            TabEntry next = newKey == null ? null : tabsByKey.get(newKey);
            TabEvent before = dispatchTabEvent(EventType.BEFORE_CHANGE, next, new HashMap<>() {{
                put("oldKey", oldKey);
                put("newKey", newKey);
            }});

            if (before.isCanceled()) {
                restoreSelection(oldKey);
                return;
            }

            lastKey = oldKey;
            currentKey = newKey;
            rememberSelection(newKey);
            updateAllTabHeaders();
            dispatchTabEvent(EventType.CHANGE, next, new HashMap<>() {{
                put("oldKey", oldKey);
                put("newKey", newKey);
            }});
        };
        tabbedPane.addChangeListener(listener);
    }

    private void restoreSelection(String key) {
        suppressSelectionEvent = true;
        if (key == null) {
            tabbedPane.setSelectedIndex(-1);
        } else {
            TabEntry entry = tabsByKey.get(key);
            if (entry != null) tabbedPane.setSelectedComponent(entry.getComponent());
        }
        suppressSelectionEvent = false;
    }

    private boolean removeTab(TabEntry entry, String eventType, boolean selectFallback) {
        int index = tabbedPane.indexOfComponent(entry.getComponent());
        if (index < 0) return false;

        if (EventTabbedPanel.TAB_REMOVE.equals(eventType)) {
            TabEvent before = dispatchTabEvent(EventTabbedPanel.BEFORE_TAB_REMOVE, entry, new HashMap<>() {{
                put("index", index);
            }});

            if (before.isCanceled()) {
                return false;
            }
        }

        boolean wasCurrent = Objects.equals(currentKey, entry.getKey());
        String oldKey = wasCurrent ? entry.getKey() : currentKey;

        suppressSelectionEvent = true;
        try {
            tabbedPane.removeTabAt(index);
        } finally {
            suppressSelectionEvent = false;
        }

        tabsByKey.remove(entry.getKey());
        keyByComponent.remove(entry.getComponent());
        mruKeys.remove(entry.getKey());

        if (wasCurrent) {
            String newKey = null;

            if (selectFallback && tabbedPane.getTabCount() > 0) {
                int fallbackIndex = Math.min(index, tabbedPane.getTabCount() - 1);
                Component fallbackComponent = tabbedPane.getComponentAt(fallbackIndex);
                newKey = keyByComponent.get(fallbackComponent);

                suppressSelectionEvent = true;
                try {
                    tabbedPane.setSelectedIndex(fallbackIndex);
                } finally {
                    suppressSelectionEvent = false;
                }
            }

            if (fireSelectionChangeOnClose) {
                fireSelectionChange(oldKey, newKey);
            } else {
                lastKey = oldKey;
                currentKey = newKey;
                rememberSelection(newKey);
                updateAllTabHeaders();
            }
        }

        dispatchTabEvent(eventType, entry, null);
        revalidate();
        repaint();
        collapseIfEmptyTabGroup();

        return true;
    }

    private boolean detachTabForTransfer(TabEntry entry) {
        int index = tabbedPane.indexOfComponent(entry.getComponent());
        if (index < 0) {
            return false;
        }

        boolean wasCurrent = Objects.equals(currentKey, entry.getKey());
        String oldKey = currentKey;

        suppressSelectionEvent = true;
        try {
            tabbedPane.removeTabAt(index);
        } finally {
            suppressSelectionEvent = false;
        }

        tabsByKey.remove(entry.getKey());
        keyByComponent.remove(entry.getComponent());
        mruKeys.remove(entry.getKey());

        if (wasCurrent) {
            String newKey = null;

            if (tabbedPane.getTabCount() > 0) {
                int fallbackIndex = Math.min(
                        index,
                        tabbedPane.getTabCount() - 1
                );

                Component fallbackComponent =
                        tabbedPane.getComponentAt(fallbackIndex);

                newKey = keyByComponent.get(fallbackComponent);

                suppressSelectionEvent = true;
                try {
                    tabbedPane.setSelectedIndex(fallbackIndex);
                } finally {
                    suppressSelectionEvent = false;
                }
            }

            fireSelectionChange(oldKey, newKey);
        }

        revalidate();
        repaint();
        collapseIfEmptyTabGroup();
        return true;
    }

    private boolean moveTabInternal(String key, int newIndex, boolean dispatchEvents, boolean updateHeaders) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return false;

        int oldIndex = tabbedPane.indexOfComponent(entry.getComponent());
        if (oldIndex < 0) return false;

        int targetIndex = constrainMoveTarget(entry, Math.max(0, Math.min(newIndex, tabbedPane.getTabCount() - 1)));
        if (oldIndex == targetIndex) return true;

        if (dispatchEvents) {
            TabEvent before = dispatchTabEvent(EventTabbedPanel.BEFORE_TAB_MOVE, entry, new HashMap<>() {{
                put("oldIndex", oldIndex);
                put("newIndex", targetIndex);
            }});
            if (before.isCanceled()) return false;
        }

        Component selectedBeforeMove = getLocalCurrent();
        Component tabHeader = tabbedPane.getTabComponentAt(oldIndex);
        suppressSelectionEvent = true;
        try {
            tabbedPane.removeTabAt(oldIndex);
            tabbedPane.insertTab(entry.getTitle(), entry.getIcon(), entry.getComponent(), entry.getTooltip(), targetIndex);
            if (tabHeader != null) {
                tabbedPane.setTabComponentAt(targetIndex, tabHeader);
            }
            if (selectedBeforeMove != null) tabbedPane.setSelectedComponent(selectedBeforeMove);
        } finally {
            suppressSelectionEvent = false;
        }

        reorderMapFromTabbedPane();
        if (updateHeaders) {
            updateAllTabHeaders();
        }

        if (dispatchEvents) {
            dispatchTabEvent(EventTabbedPanel.TAB_MOVE, entry, new HashMap<>() {{
                put("oldIndex", oldIndex);
                put("newIndex", indexOf(key));
            }});
        }
        return true;
    }

    private void reorderMapFromTabbedPane() {
        Map<String, TabEntry> reordered = new LinkedHashMap<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            String key = keyByComponent.get(component);
            if (key != null) {
                reordered.put(key, tabsByKey.get(key));
            }
        }
        tabsByKey.clear();
        tabsByKey.putAll(reordered);
    }

    private void rememberSelection(String key) {
        if (!mruSwitchingEnabled || key == null) return;
        mruKeys.remove(key);
        mruKeys.add(0, key);
    }

    private void enforcePinnedPlacement(String key) {
        TabEntry entry = tabsByKey.get(key);
        if (entry == null) return;
        int oldIndex = indexOf(key);
        if (oldIndex < 0) return;
        int target = entry.isPinned() ? countPinnedBefore(key) : countPinnedTabs();
        moveTabInternal(key, target, false, true);
    }

    private int constrainMoveTarget(TabEntry entry, int targetIndex) {
        int pinnedCount = countPinnedTabs();
        if (entry.isPinned()) {
            return Math.max(0, Math.min(targetIndex, Math.max(0, pinnedCount - 1)));
        }
        return Math.max(pinnedCount, targetIndex);
    }

    private int countPinnedTabs() {
        int count = 0;
        for (TabEntry entry : tabsByKey.values()) {
            if (entry.isPinned()) count++;
        }
        return count;
    }

    private int countPinnedBefore(String key) {
        int count = 0;
        for (TabEntry entry : tabsByKey.values()) {
            if (Objects.equals(entry.getKey(), key)) break;
            if (entry.isPinned()) count++;
        }
        return count;
    }

    protected String createTabKey() {
        return "tab-" + UUID.randomUUID();
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Tab key cannot be null or blank.");
        }
    }

    private void installTabbedPaneStyle() {
        tabbedPane.setUI(new StyledTabbedPaneUI(this));
        tabbedPane.setOpaque(false);
    }

    private void fireSelectionChange(String oldKey, String newKey) {
        if (Objects.equals(oldKey, newKey)) {
            return;
        }

        TabEntry next = newKey == null ? null : tabsByKey.get(newKey);

        lastKey = oldKey;
        currentKey = newKey;

        if (newKey != null) {
            rememberSelection(newKey);
        }

        updateAllTabHeaders();

        dispatchTabEvent(EventType.CHANGE, next, new HashMap<>() {{
            put("oldKey", oldKey);
            put("newKey", newKey);
        }});
    }

    private static boolean isSameOrDescendant(Component component, Container root) {
        return component == root || SwingUtilities.isDescendingFrom(component, root);
    }

    private Component getLocalCurrent() {
        TabEntry entry = currentKey == null
                ? null
                : tabsByKey.get(currentKey);

        return entry == null ? null : entry.getComponent();
    }

    private void installTabHeaderHover(
            JComponent header,
            Component component,
            TabEntry entry,
            boolean[] hovered
    ) {
        MouseAdapter hoverListener = new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent event) {
                if (hovered[0]) {
                    return;
                }

                hovered[0] = true;

                dispatchTabEvent(
                        EventTabbedPanel.TAB_HEADER_HOVER,
                        entry,
                        createHeaderMouseProperties(header, component, event)
                );
            }

            @Override
            public void mouseExited(MouseEvent event) {
                Point pointInHeader = SwingUtilities.convertPoint(
                        component,
                        event.getPoint(),
                        header
                );

                if (header.contains(pointInHeader)) {
                    return;
                }

                if (!hovered[0]) {
                    return;
                }

                hovered[0] = false;

                dispatchTabEvent(
                        EventTabbedPanel.TAB_HEADER_HOVER_EXIT,
                        entry,
                        createHeaderMouseProperties(header, component, event)
                );
            }
        };

        component.addMouseListener(hoverListener);

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installTabHeaderHover(
                        header,
                        child,
                        entry,
                        hovered
                );
            }
        }
    }


    private Map<String, Object> createHeaderMouseProperties(JComponent header, Component internalComponent, MouseEvent event) {
        Point pointInHeader = SwingUtilities.convertPoint(
                internalComponent,
                event.getPoint(),
                header
        );

        Map<String, Object> properties = new HashMap<>();

        properties.put("header", header);
        properties.put("internalComponent", internalComponent);
        properties.put("mouseEvent", event);
        properties.put("mouseX", pointInHeader.x);
        properties.put("mouseY", pointInHeader.y);

        return properties;
    }

}
