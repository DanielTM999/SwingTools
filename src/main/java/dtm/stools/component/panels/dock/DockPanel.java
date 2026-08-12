package dtm.stools.component.panels.dock;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.component.panels.tab.*;
import lombok.Getter;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class DockPanel extends PanelEventListener {
    private final JPanel content = new JPanel(new BorderLayout());
    private final Map<DockRegion, TabbedPanel> groups = new EnumMap<>(DockRegion.class);
    private final Map<DockRegion, List<String>> splitDockKeys = new EnumMap<>(DockRegion.class);
    private final Map<String, TabbedPanel> splitGroupsByKey = new LinkedHashMap<>();
    private final Map<DockRegion, Component> displayedRegionComponents = new EnumMap<>(DockRegion.class);
    private final Map<DockRegion, Dimension> preferredRegionSizes = new EnumMap<>(DockRegion.class);
    private final EnumSet<DockRegion> explicitRegionPreferredSizes = EnumSet.noneOf(DockRegion.class);
    private final Map<String, DockEntry> docksByKey = new LinkedHashMap<>();
    private final Map<Component, String> keyByComponent = new LinkedHashMap<>();
    private final EnumSet<DockRegion> dockDragLockedRegions = EnumSet.noneOf(DockRegion.class);
    private final EnumSet<DockRegion> collapsedRegions = EnumSet.noneOf(DockRegion.class);
    private final Map<String, Dimension> rememberedDockSizes = new HashMap<>();
    private final Map<String, DockRegion> rememberedDockRegions = new HashMap<>();
    private boolean preserveDockSizeOnReopen = true;
    private boolean adjustingDividers;
    private boolean suppressTabEvents;
    private boolean dockDragEnabled = true;
    private boolean dockHorizontalDropEnabled;
    private int dockDragStartThreshold = 6;
    private int dockDropZoneSize = 120;
    @Getter
    private Color dockDropPreviewColor = new Color(0x2563EB);
    @Getter
    private float dockDropPreviewAlpha = 0.22f;
    private boolean splitOneTouchExpandableEnabled = true;
    @Getter
    private DockRegionLayout dockRegionLayout = DockRegionLayout.SPLIT;
    @Getter
    private DockDragPolicy dragPolicy = new DockDragPolicy() {};
    @Getter
    private DockGroupFactory dockGroupFactory;
    @Getter
    private DockSeparatorFactory dockSeparatorFactory;
    private DragSession dragSession;
    private DockRegion previewRegion;
    private boolean previewAllowed;

    public DockPanel() {
        this(null);
    }

    public DockPanel(DockGroupFactory dockGroupFactory) {
        super(new BorderLayout(), false);
        this.dockGroupFactory = dockGroupFactory;
        installDefaults();
        add(content, BorderLayout.CENTER);
        rebuildLayout();
    }

    private void installDefaults() {
        preferredRegionSizes.put(DockRegion.LEFT, new Dimension(260, 1));
        preferredRegionSizes.put(DockRegion.RIGHT, new Dimension(260, 1));
        preferredRegionSizes.put(DockRegion.TOP, new Dimension(1, 180));
        preferredRegionSizes.put(DockRegion.BOTTOM, new Dimension(1, 220));
        preferredRegionSizes.put(DockRegion.TOP_LEFT, new Dimension(260, 220));
        preferredRegionSizes.put(DockRegion.BOTTOM_LEFT, new Dimension(260, 220));
        preferredRegionSizes.put(DockRegion.TOP_RIGHT, new Dimension(260, 220));
        preferredRegionSizes.put(DockRegion.BOTTOM_RIGHT, new Dimension(260, 220));

        for (DockRegion region : DockRegion.values()) {
            TabbedPanel group = resolveDockGroup(region);
            groups.put(region, group);
            splitDockKeys.put(region, new ArrayList<>());
            installGroupListeners(region, group);
        }
    }

    protected TabbedPanel resolveDockGroup(DockRegion region) {
        TabbedPanel group = dockGroupFactory == null ? null : dockGroupFactory.createGroup(this, region);
        return group == null ? createDockGroup(region) : configureDockGroup(region, group);
    }

    protected TabbedPanel createDockGroup(DockRegion region) {
        TabbedPanel tabbedPanel = new TabbedPanel(JTabbedPane.TOP);
        return configureDockGroup(region, tabbedPanel);
    }

    protected TabbedPanel configureDockGroup(DockRegion region, TabbedPanel group) {
        Objects.requireNonNull(group, "group");
        group.setTabDragEnabled(false);
        group.setSplitTabsOnDragEnabled(false);
        group.setTabListButtonVisible(false);
        group.setNewTabButtonVisible(false);
        return group;
    }

    private void installGroupListeners(DockRegion region, TabbedPanel group) {
        group.addEventListener(EventTabbedPanel.BEFORE_TAB_CLOSE, event -> {
            if (suppressTabEvents) return;
            TabEvent tabEvent = (TabEvent) event;
            DockEntry entry = docksByKey.get(tabEvent.getKey());
            DockEvent dockEvent = dispatchDockEvent(EventDockPanel.BEFORE_DOCK_CLOSE, entry, Map.of("region", region));
            if (dockEvent.isCanceled()) tabEvent.cancel();
        });

        group.addEventListener(EventTabbedPanel.TAB_CLOSE, event -> {
            if (suppressTabEvents) return;
            TabEvent tabEvent = (TabEvent) event;
            DockEntry entry = removeEntry(tabEvent.getKey());
            dispatchDockEvent(EventDockPanel.DOCK_CLOSE, entry, Map.of("region", region));
            afterRegionContentChange(region);
        });

        group.addEventListener(EventTabbedPanel.BEFORE_TAB_REMOVE, event -> {
            if (suppressTabEvents) return;
            TabEvent tabEvent = (TabEvent) event;
            DockEntry entry = docksByKey.get(tabEvent.getKey());
            DockEvent dockEvent = dispatchDockEvent(EventDockPanel.BEFORE_DOCK_REMOVE, entry, Map.of("region", region));
            if (dockEvent.isCanceled()) tabEvent.cancel();
        });

        group.addEventListener(EventTabbedPanel.TAB_REMOVE, event -> {
            if (suppressTabEvents) return;
            TabEvent tabEvent = (TabEvent) event;
            forgetRememberedDockSize(tabEvent.getKey());
            DockEntry entry = removeEntry(tabEvent.getKey());
            dispatchDockEvent(EventDockPanel.DOCK_REMOVE, entry, Map.of("region", region));
            afterRegionContentChange(region);
        });

        group.addEventListener(EventType.CHANGE, event -> {
            if (suppressTabEvents) return;
            TabEvent tabEvent = (TabEvent) event;
            DockEntry entry = docksByKey.get(tabEvent.getKey());
            dispatchDockEvent(EventDockPanel.DOCK_SELECT, entry, Map.of("region", region));
            rebuildLayout();
        });
    }

    public String addDock(String title, Component component) {
        return addDock(new DockConfig(title, component));
    }

    public String addDock(String title, Component component, Dimension preferredSize) {
        return addDock(new DockConfig(title, component).preferredSize(preferredSize));
    }

    public String addDock(String title, Component component, DockRegion region) {
        return addDock(new DockConfig(title, component).region(region));
    }

    public String addDock(String title, Component component, DockRegion region, Dimension preferredSize) {
        return addDock(new DockConfig(title, component).region(region).preferredSize(preferredSize));
    }

    public String addDock(String key, String title, Component component, DockRegion region) {
        return addDock(new DockConfig(key, title, component).region(region));
    }

    public String addDock(String key, String title, Component component, Dimension preferredSize) {
        return addDock(new DockConfig(key, title, component).preferredSize(preferredSize));
    }

    public String addDock(String key, String title, Component component, DockRegion region, Dimension preferredSize) {
        return addDock(new DockConfig(key, title, component).region(region).preferredSize(preferredSize));
    }

    public String addDock(DockConfig config) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(config.getComponent(), "component");

        String key = config.getKey() == null || config.getKey().isBlank() ? createDockKey() : config.getKey();
        if (docksByKey.containsKey(key)) {
            throw new IllegalArgumentException("Dock key already registered: " + key);
        }
        if (keyByComponent.containsKey(config.getComponent())) {
            throw new IllegalArgumentException("Component already registered in DockPanel");
        }

        DockEntry entry = new DockEntry(key, config);
        restoreRememberedDockSize(entry);
        replaceDocksInRegionIfNeeded(entry.getRegion(), key);
        docksByKey.put(key, entry);
        keyByComponent.put(config.getComponent(), key);

        addEntryToGroup(entry, config);
        rebuildLayout();
        dispatchDockEvent(EventDockPanel.DOCK_ADD, entry, Map.of("region", entry.getRegion()));
        return key;
    }

    public boolean removeDock(String key) {
        DockEntry entry = docksByKey.get(key);
        if (entry == null) return false;
        return getGroupForDock(entry).removeTab(key);
    }

    public boolean closeDock(String key) {
        DockEntry entry = docksByKey.get(key);
        if (entry == null) return false;
        return getGroupForDock(entry).closeTab(key);
    }

    public boolean moveDock(String key, DockRegion targetRegion) {
        return moveDock(key, targetRegion, false);
    }

    protected boolean moveDock(String key, DockRegion targetRegion, boolean fromDrag) {
        targetRegion = targetRegion == null ? DockRegion.CENTER : targetRegion;
        DockEntry entry = docksByKey.get(key);
        if (entry == null || entry.getRegion() == targetRegion) return false;

        captureVisibleRegionSizes();

        DockRegion oldRegion = entry.getRegion();
        DockRegion newRegion = targetRegion;
        DockDropContext context = new DockDropContext(
                this,
                entry,
                oldRegion,
                newRegion,
                dragSession == null ? null : dragSession.location
        );
        if (!canDropDock(context)) return false;

        DockEvent before = dispatchDockEvent(EventDockPanel.BEFORE_DOCK_MOVE, entry, new HashMap<>() {{
            put("oldRegion", oldRegion);
            put("newRegion", newRegion);
            put("fromDrag", fromDrag);
        }});
        if (before.isCanceled()) return false;

        TabbedPanel oldGroup = getGroupForDock(entry);
        TabEntry tab = oldGroup.getEntry(key);
        entry.updateFrom(tab, oldRegion);
        replaceDocksInRegionIfNeeded(newRegion, key);

        suppressTabEvents = true;
        try {
            if (!oldGroup.removeTab(key)) return false;
            removeSplitGroup(key, oldRegion);
            entry.setRegion(newRegion);
            forgetRememberedDockSize(key);
            entry.setPreferredSize(null);
            addEntryToGroup(entry, null);
        } finally {
            suppressTabEvents = false;
        }

        rebuildLayout();
        dispatchDockEvent(EventDockPanel.DOCK_MOVE, entry, new HashMap<>() {{
            put("oldRegion", oldRegion);
            put("newRegion", newRegion);
            put("fromDrag", fromDrag);
        }});
        return true;
    }

    public void switchTo(String key) {
        DockEntry entry = docksByKey.get(key);
        if (entry == null) return;
        getGroupForDock(entry).switchTo(key);
    }

    public boolean containsDock(String key) {
        return docksByKey.containsKey(key);
    }

    public boolean containsDock(Component component) {
        return keyByComponent.containsKey(component);
    }

    public Component findDock(String key) {
        DockEntry entry = docksByKey.get(key);
        return entry == null ? null : entry.getComponent();
    }

    public DockEntry getDock(String key) {
        return docksByKey.get(key);
    }

    public String getKeyOf(Component component) {
        return keyByComponent.get(component);
    }

    public List<DockEntry> getDocks() {
        return new ArrayList<>(docksByKey.values());
    }

    public List<DockEntry> getDocks(DockRegion region) {
        List<DockEntry> result = new ArrayList<>();
        if (isSplitDocksInSameRegionEnabled()) {
            for (String key : splitDockKeys.getOrDefault(region, List.of())) {
                DockEntry entry = docksByKey.get(key);
                if (entry != null) result.add(entry);
            }
            return result;
        }
        TabbedPanel group = groups.get(region);
        if (group == null) return result;
        for (TabEntry tab : group.getEntries()) {
            DockEntry entry = docksByKey.get(tab.getKey());
            if (entry != null) result.add(entry);
        }
        return result;
    }

    public Collection<Component> getDockComponents() {
        List<Component> components = new ArrayList<>();
        for (DockEntry entry : docksByKey.values()) {
            components.add(entry.getComponent());
        }
        return components;
    }

    public TabbedPanel getGroup(DockRegion region) {
        if (isSplitDocksInSameRegionEnabled()) {
            List<String> keys = splitDockKeys.getOrDefault(region, List.of());
            if (!keys.isEmpty()) return splitGroupsByKey.get(keys.getFirst());
        }
        return groups.get(region);
    }

    public TabbedPanel getCenterGroup() {
        return getGroup(DockRegion.CENTER);
    }

    public int getDockCount() {
        return docksByKey.size();
    }

    public boolean isRegionVisible(DockRegion region) {
        if (isSplitDocksInSameRegionEnabled()) {
            return !splitDockKeys.getOrDefault(region, List.of()).isEmpty();
        }
        TabbedPanel group = groups.get(region);
        return group != null && !group.isEmpty();
    }

    public boolean isRegionDisplayed(DockRegion region) {
        return isRegionVisible(region) && !collapsedRegions.contains(region);
    }

    public boolean isRegionCollapsed(DockRegion region) {
        return region != null && collapsedRegions.contains(region);
    }

    public DockPanel setRegionCollapsed(DockRegion region, boolean collapsed) {
        if (region == null || region == DockRegion.CENTER) return this;
        if (collapsed == collapsedRegions.contains(region)) return this;
        captureVisibleRegionSizes();
        if (collapsed) {
            collapsedRegions.add(region);
        } else {
            collapsedRegions.remove(region);
        }
        rebuildLayout();
        return this;
    }

    public DockPanel toggleRegionCollapsed(DockRegion region) {
        if (region == null || region == DockRegion.CENTER) return this;
        return setRegionCollapsed(region, !collapsedRegions.contains(region));
    }

    public DockPanel setRegionPreferredSize(DockRegion region, Dimension size) {
        if (region == null || region == DockRegion.CENTER) return this;
        if (size == null) {
            preferredRegionSizes.remove(region);
            explicitRegionPreferredSizes.remove(region);
        } else {
            preferredRegionSizes.put(region, new Dimension(size));
            explicitRegionPreferredSizes.add(region);
        }
        applyPreferredRegionSizes();
        rebuildLayout();
        return this;
    }

    public Dimension getRegionPreferredSize(DockRegion region) {
        Dimension size = preferredRegionSizes.get(region);
        return size == null ? null : new Dimension(size);
    }

    public DockPanel setRegionPreferredSize(String key, Dimension size) {
        return setDockPreferredSize(key, size);
    }

    public DockPanel setDockPreferredSize(String key, Dimension size) {
        DockEntry entry = docksByKey.get(key);
        if (entry == null) return this;
        entry.setPreferredSize(size);
        rebuildLayout();
        return this;
    }

    public DockPanel setDockPreferredSize(Component component, Dimension size) {
        String key = keyByComponent.get(component);
        return key == null ? this : setDockPreferredSize(key, size);
    }

    public Dimension getDockPreferredSize(String key) {
        DockEntry entry = docksByKey.get(key);
        return entry == null ? null : entry.getPreferredSize();
    }

    public Dimension getDockPreferredSize(Component component) {
        String key = keyByComponent.get(component);
        return key == null ? null : getDockPreferredSize(key);
    }

    public DockPanel setDockDragEnabled(boolean enabled) {
        this.dockDragEnabled = enabled;
        return this;
    }

    public boolean isDockDragEnabled() {
        return dockDragEnabled;
    }

    public DockPanel setDockDragLockedRegions(DockRegion first, DockRegion... regions) {
        dockDragLockedRegions.clear();
        if (first != null) dockDragLockedRegions.add(first);
        if (regions != null) {
            for (DockRegion region : regions) {
                if (region != null) dockDragLockedRegions.add(region);
            }
        }
        return this;
    }

    public DockPanel setDockDragLockedRegions(Collection<DockRegion> regions) {
        dockDragLockedRegions.clear();
        if (regions != null) {
            for (DockRegion region : regions) {
                if (region != null) dockDragLockedRegions.add(region);
            }
        }
        return this;
    }

    public DockPanel setDockRegionDragEnabled(DockRegion region, boolean enabled) {
        if (region == null) return this;
        if (enabled) {
            dockDragLockedRegions.remove(region);
        } else {
            dockDragLockedRegions.add(region);
        }
        return this;
    }

    public DockPanel clearDockDragLockedRegions() {
        dockDragLockedRegions.clear();
        return this;
    }

    public EnumSet<DockRegion> getDockDragLockedRegions() {
        return dockDragLockedRegions.clone();
    }

    public boolean isDockRegionDragEnabled(DockRegion region) {
        return region != null && !dockDragLockedRegions.contains(region);
    }

    public boolean isDockRegionDropEnabled(DockRegion region) {
        return region != null && !dockDragLockedRegions.contains(region);
    }

    public DockPanel setDockHorizontalDropEnabled(boolean enabled) {
        this.dockHorizontalDropEnabled = enabled;
        return this;
    }

    public boolean isDockHorizontalDropEnabled() {
        return dockHorizontalDropEnabled;
    }

    public DockPanel setDockDragStartThreshold(int pixels) {
        this.dockDragStartThreshold = Math.max(0, pixels);
        return this;
    }

    public int getDockDragStartThreshold() {
        return dockDragStartThreshold;
    }

    public DockPanel setDockDropZoneSize(int pixels) {
        this.dockDropZoneSize = Math.max(32, pixels);
        return this;
    }

    public int getDockDropZoneSize() {
        return dockDropZoneSize;
    }

    public DockPanel setDockDropPreviewColor(Color color) {
        this.dockDropPreviewColor = color == null ? new Color(0x2563EB) : color;
        repaint();
        return this;
    }

    public DockPanel setDockDropPreviewAlpha(float alpha) {
        this.dockDropPreviewAlpha = Math.max(0f, Math.min(1f, alpha));
        repaint();
        return this;
    }

    public DockPanel setSplitOneTouchExpandableEnabled(boolean enabled) {
        this.splitOneTouchExpandableEnabled = enabled;
        rebuildLayout();
        return this;
    }

    public boolean isSplitOneTouchExpandableEnabled() {
        return splitOneTouchExpandableEnabled;
    }

    public DockPanel setDockSeparatorFactory(DockSeparatorFactory dockSeparatorFactory) {
        this.dockSeparatorFactory = dockSeparatorFactory;
        rebuildLayout();
        return this;
    }

    public DockPanel setDockRegionLayout(DockRegionLayout layout) {
        DockRegionLayout next = layout == null ? DockRegionLayout.SPLIT : layout;
        if (dockRegionLayout == next) return this;
        if (!docksByKey.isEmpty()) {
            throw new IllegalStateException("DockRegionLayout must be configured before adding docks.");
        }
        dockRegionLayout = next;
        rebuildLayout();
        return this;
    }

    public boolean isSingleDockPerRegionEnabled() {
        return dockRegionLayout == DockRegionLayout.SINGLE;
    }

    public DockPanel setSingleDockPerRegionEnabled(boolean enabled) {
        return setDockRegionLayout(enabled ? DockRegionLayout.SINGLE : DockRegionLayout.TABS);
    }

    public DockPanel setTabsPerRegionEnabled(boolean enabled) {
        return setDockRegionLayout(enabled ? DockRegionLayout.TABS : DockRegionLayout.SINGLE);
    }

    public boolean isSplitDocksInSameRegionEnabled() {
        return dockRegionLayout == DockRegionLayout.SPLIT;
    }

    public DockPanel setSplitDocksInSameRegionEnabled(boolean enabled) {
        return setDockRegionLayout(enabled ? DockRegionLayout.SPLIT : DockRegionLayout.TABS);
    }

    public DockPanel setPreserveDockSizeOnReopen(boolean enabled) {
        this.preserveDockSizeOnReopen = enabled;
        return this;
    }

    public boolean isPreserveDockSizeOnReopen() {
        return preserveDockSizeOnReopen;
    }

    public DockPanel setDragPolicy(DockDragPolicy dragPolicy) {
        this.dragPolicy = dragPolicy == null ? new DockDragPolicy() {} : dragPolicy;
        return this;
    }

    public DockPanel setDockGroupFactory(DockGroupFactory dockGroupFactory) {
        if (!docksByKey.isEmpty()) {
            throw new IllegalStateException("DockGroupFactory must be configured before adding docks.");
        }
        this.dockGroupFactory = dockGroupFactory;

        for (DockRegion region : DockRegion.values()) {
            TabbedPanel old = groups.get(region);
            if (old != null) old.removeAllListeners();

            TabbedPanel group = resolveDockGroup(region);
            groups.put(region, group);
            installGroupListeners(region, group);
        }
        rebuildLayout();
        return this;
    }

    public DockLayoutSnapshot createLayoutSnapshot() {
        DockLayoutSnapshot snapshot = new DockLayoutSnapshot();
        for (DockRegion region : DockRegion.values()) {
            List<String> keys = new ArrayList<>();
            for (DockEntry entry : getDocks(region)) keys.add(entry.getKey());
            snapshot.putRegionKeys(region, keys);
            TabbedPanel group = getGroup(region);
            snapshot.putSelectedKey(region, group == null ? null : group.getCurrentKey());
            snapshot.putPreferredSize(region, getRegionPreferredSize(region));
        }
        return snapshot;
    }

    public void restoreLayout(DockLayoutSnapshot snapshot) {
        if (snapshot == null) return;

        for (Map.Entry<DockRegion, Dimension> entry : snapshot.getPreferredSizes().entrySet()) {
            if (entry.getKey() != DockRegion.CENTER) {
                preferredRegionSizes.put(entry.getKey(), entry.getValue());
                explicitRegionPreferredSizes.add(entry.getKey());
            }
        }

        suppressTabEvents = true;
        try {
            for (DockRegion region : DockRegion.values()) {
                List<String> keys = snapshot.getKeys(region);
                for (String key : keys) {
                    DockEntry dock = docksByKey.get(key);
                    if (dock != null && dock.getRegion() != region) {
                        moveDockSilently(dock, region);
                    }
                }
                if (isSplitDocksInSameRegionEnabled()) {
                    List<String> order = splitDockKeys.get(region);
                    order.removeIf(keys::contains);
                    order.addAll(keys.stream().filter(splitGroupsByKey::containsKey).toList());
                } else {
                    TabbedPanel group = groups.get(region);
                    for (int i = 0; i < keys.size(); i++) {
                        group.moveTab(keys.get(i), i);
                    }
                }
            }

            for (DockRegion region : DockRegion.values()) {
                String selectedKey = snapshot.getSelectedKey(region);
                TabbedPanel group = getGroupForKey(selectedKey);
                if (selectedKey != null && group != null && group.contains(selectedKey)) {
                    group.switchTo(selectedKey);
                }
            }
        } finally {
            suppressTabEvents = false;
        }

        rebuildLayout();
        dispatchDockEvent(EventDockPanel.LAYOUT_CHANGE, null, Map.of("snapshot", snapshot));
    }

    public DockPanel onDockEvent(String eventType, Consumer<DockEvent> listener) {
        addEventListener(eventType, DockEvent.class, listener);
        return this;
    }

    public DockPanel onDockAdd(Consumer<DockEvent> listener) {
        return onDockEvent(EventDockPanel.DOCK_ADD, listener);
    }

    public DockPanel onBeforeDockClose(Consumer<DockEvent> listener) {
        return onDockEvent(EventDockPanel.BEFORE_DOCK_CLOSE, listener);
    }

    public DockPanel onDockClose(Consumer<DockEvent> listener) {
        return onDockEvent(EventDockPanel.DOCK_CLOSE, listener);
    }

    public DockPanel onBeforeDockMove(Consumer<DockEvent> listener) {
        return onDockEvent(EventDockPanel.BEFORE_DOCK_MOVE, listener);
    }

    public DockPanel onDockMove(Consumer<DockEvent> listener) {
        return onDockEvent(EventDockPanel.DOCK_MOVE, listener);
    }

    public DockPanel onDockDrop(Consumer<DockEvent> listener) {
        return onDockEvent(EventDockPanel.DOCK_DROP, listener);
    }

    public DockPanel onDockSelect(Consumer<DockEvent> listener) {
        return onDockEvent(EventDockPanel.DOCK_SELECT, listener);
    }

    public DockPanel addExternalDragHandle(JComponent handle, String dockKey) {
        if (handle == null || dockKey == null || dockKey.isBlank()) {
            return this;
        }
        if (!markDragHandlerInstalled(handle, dockKey)) {
            return this;
        }
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                beginPossibleDockDrag(dockKey, e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updatePossibleDockDrag(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                finishPossibleDockDrag(e);
            }
        };
        handle.addMouseListener(adapter);
        handle.addMouseMotionListener(adapter);
        return this;
    }

    private void addEntryToGroup(DockEntry entry, DockConfig sourceConfig) {
        TabConfig tabConfig = new TabConfig(entry.getKey(), entry.getTitle(), entry.getComponent())
                .icon(entry.getIcon())
                .selectedIcon(entry.getSelectedIcon())
                .closable(entry.isClosable())
                .tooltip(entry.getTooltip())
                .headerFactory(createDockHeaderFactory(entry))
                .pinned(entry.isPinned())
                .dirty(entry.isDirty());

        if (entry.getBadge() != null) {
            tabConfig.badge(entry.getBadge());
        }
        if (sourceConfig != null && sourceConfig.getBadge() != null) {
            tabConfig.badge(sourceConfig.getBadge());
        }
        TabbedPanel group = getOrCreateGroupForDock(entry);
        group.addTab(tabConfig);
        installContentDragHandle(entry);
    }

    private TabbedPanel getOrCreateGroupForDock(DockEntry entry) {
        if (!isSplitDocksInSameRegionEnabled()) return groups.get(entry.getRegion());

        TabbedPanel group = resolveDockGroup(entry.getRegion());
        installGroupListeners(entry.getRegion(), group);
        splitGroupsByKey.put(entry.getKey(), group);
        splitDockKeys.get(entry.getRegion()).add(entry.getKey());
        return group;
    }

    private TabbedPanel getGroupForDock(DockEntry entry) {
        return entry == null ? null : getGroupForKey(entry.getKey());
    }

    private TabbedPanel getGroupForKey(String key) {
        if (key == null) return null;
        DockEntry entry = docksByKey.get(key);
        if (entry == null) return splitGroupsByKey.get(key);
        if (!isSplitDocksInSameRegionEnabled()) return groups.get(entry.getRegion());
        return splitGroupsByKey.get(key);
    }

    private void removeSplitGroup(String key, DockRegion region) {
        if (!isSplitDocksInSameRegionEnabled() || key == null) return;
        splitGroupsByKey.remove(key);
        splitDockKeys.getOrDefault(region, List.of()).remove(key);
    }

    private List<TabbedPanel> getRegionGroups(DockRegion region) {
        if (!isSplitDocksInSameRegionEnabled()) return List.of(groups.get(region));
        List<TabbedPanel> result = new ArrayList<>();
        for (String key : splitDockKeys.getOrDefault(region, List.of())) {
            TabbedPanel group = splitGroupsByKey.get(key);
            if (group != null) result.add(group);
        }
        return result;
    }

    protected JComponent createDockTabHeader(TabbedPanel tabs, TabEntry tabEntry, DockEntry dockEntry, boolean selected) {
        JComponent header;
        if (dockEntry.getHeaderFactory() != null) {
            header = dockEntry.getHeaderFactory().createHeader(tabs, tabEntry, selected);
        } else {
            header = tabs.createDefaultHeader(tabEntry, selected);
        }
        installDockDragHandlers(header, dockEntry.getKey());
        return header;
    }

    private TabHeaderFactory createDockHeaderFactory(DockEntry dockEntry) {
        return (tabs, tabEntry, selected) -> createDockTabHeader(tabs, tabEntry, dockEntry, selected);
    }

    private void replaceDocksInRegionIfNeeded(DockRegion region, String incomingKey) {
        if (!isSingleDockPerRegionEnabled() || region == null) return;
        TabbedPanel group = groups.get(region);
        if (group == null || group.isEmpty()) return;

        List<String> keys = new ArrayList<>();
        for (TabEntry entry : group.getEntries()) {
            if (!Objects.equals(entry.getKey(), incomingKey)) {
                keys.add(entry.getKey());
            }
        }

        for (String key : keys) {
            forceCloseDockForRegionReplace(key, region, incomingKey);
        }
    }

    private void forceCloseDockForRegionReplace(String key, DockRegion region, String incomingKey) {
        DockEntry replaced = docksByKey.get(key);
        if (replaced == null) return;

        TabbedPanel group = groups.get(region);
        suppressTabEvents = true;
        try {
            if (group != null) group.removeTab(key);
            removeSplitGroup(key, region);
            forgetRememberedDockSize(key);
            removeEntry(key);
        } finally {
            suppressTabEvents = false;
        }

        dispatchDockEvent(EventDockPanel.DOCK_CLOSE, replaced, new HashMap<>() {{
            put("region", region);
            put("reason", "regionReplace");
            put("replacementKey", incomingKey);
        }});
    }

    private void installDockDragHandlers(JComponent header, String key) {
        if (!markDragHandlerInstalled(header, key)) return;
        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                beginPossibleDockDrag(key, e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                updatePossibleDockDrag(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                finishPossibleDockDrag(e);
            }
        };
        installDockDragHandlers(header, adapter);
    }

    private void installContentDragHandle(DockEntry entry) {
        if (entry == null || !entry.isDraggable()) return;

        Component handle = entry.getDragHandle();
        if (handle == null && entry.getDragHandleName() != null && !entry.getDragHandleName().isBlank()) {
            handle = findNamedComponent(entry.getComponent(), entry.getDragHandleName());
        }
        if (!(handle instanceof JComponent jHandle)) return;

        installDockDragHandlers(jHandle, entry.getKey());
    }

    private void installDockDragHandlers(Component component, MouseAdapter adapter) {
        component.addMouseListener(adapter);
        component.addMouseMotionListener(adapter);

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                installDockDragHandlers(child, adapter);
            }
        }
    }

    private boolean markDragHandlerInstalled(JComponent component, String key) {
        String property = "dtm.stools.dock.dragHandler." + key;
        if (Boolean.TRUE.equals(component.getClientProperty(property))) return false;
        component.putClientProperty(property, Boolean.TRUE);
        return true;
    }

    private Component findNamedComponent(Component component, String name) {
        if (component == null || name == null) return null;
        if (name.equals(component.getName())) return component;
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                Component found = findNamedComponent(child, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    protected boolean canStartDockDrag(DockEntry entry) {
        return dockDragEnabled
                && entry != null
                && isDockRegionDragEnabled(entry.getRegion())
                && dragPolicy.canStartDrag(entry);
    }

    protected boolean canDropDock(DockDropContext context) {
        if (context == null || context.getEntry() == null || context.getTargetRegion() == null) return false;
        if (!isDockRegionDropEnabled(context.getTargetRegion())) return false;
        if (!dragPolicy.canDrop(context)) return false;

        DockEntry entry = context.getEntry();
        return entry.getDropPredicate() == null || entry.getDropPredicate().test(context);
    }

    protected DockRegion resolveDockDropRegion(Point location) {
        if (location == null) return null;
        Rectangle bounds = content.getBounds();
        if (!bounds.contains(location)) return null;

        int zone = Math.min(dockDropZoneSize, Math.max(32, Math.min(bounds.width, bounds.height) / 3));
        boolean left = location.x <= bounds.x + zone;
        boolean right = location.x >= bounds.x + bounds.width - zone;
        boolean top = location.y <= bounds.y + zone;
        boolean bottom = location.y >= bounds.y + bounds.height - zone;

        if (dockHorizontalDropEnabled) {
            if (left && top) return DockRegion.TOP_LEFT;
            if (left && bottom) return DockRegion.BOTTOM_LEFT;
            if (right && top) return DockRegion.TOP_RIGHT;
            if (right && bottom) return DockRegion.BOTTOM_RIGHT;

            if (location.x <= bounds.x + zone) return DockRegion.LEFT;
            if (location.x >= bounds.x + bounds.width - zone) return DockRegion.RIGHT;
        }

        if (location.y <= bounds.y + zone) return DockRegion.TOP;
        if (location.y >= bounds.y + bounds.height - zone) return DockRegion.BOTTOM;

        for (DockRegion region : DockRegion.values()) {
            if (region == DockRegion.CENTER || !isRegionVisible(region)) continue;
            if (!dockHorizontalDropEnabled && region.isHorizontalSplitRegion()) continue;
            Rectangle regionBounds = getRegionBounds(region);
            if (regionBounds != null && regionBounds.contains(location)) return region;
        }
        return DockRegion.CENTER;
    }

    protected Rectangle getDockDropPreviewBounds(DockRegion region) {
        if (region == null) return null;
        Rectangle regionBounds = getRegionBounds(region);
        if (regionBounds != null && regionBounds.width > 0 && regionBounds.height > 0) {
            return regionBounds;
        }

        Rectangle bounds = content.getBounds();
        if (bounds.width <= 0 || bounds.height <= 0) return null;

        Dimension preferred = resolveRegionPreferredSize(region);
        int zone = Math.min(dockDropZoneSize, Math.max(48, Math.min(bounds.width, bounds.height) / 3));
        if (region == DockRegion.LEFT) {
            int width = preferred == null ? zone : Math.min(Math.max(preferred.width, zone), Math.max(zone, bounds.width / 2));
            return new Rectangle(bounds.x, bounds.y, width, bounds.height);
        }
        if (region == DockRegion.RIGHT) {
            int width = preferred == null ? zone : Math.min(Math.max(preferred.width, zone), Math.max(zone, bounds.width / 2));
            return new Rectangle(bounds.x + bounds.width - width, bounds.y, width, bounds.height);
        }
        if (region == DockRegion.TOP) {
            int height = preferred == null ? zone : Math.min(Math.max(preferred.height, zone), Math.max(zone, bounds.height / 2));
            return new Rectangle(bounds.x, bounds.y, bounds.width, height);
        }
        if (region == DockRegion.BOTTOM) {
            int height = preferred == null ? zone : Math.min(Math.max(preferred.height, zone), Math.max(zone, bounds.height / 2));
            return new Rectangle(bounds.x, bounds.y + bounds.height - height, bounds.width, height);
        }
        if (region == DockRegion.TOP_LEFT || region == DockRegion.BOTTOM_LEFT || region == DockRegion.TOP_RIGHT || region == DockRegion.BOTTOM_RIGHT) {
            int width = preferred == null ? zone : Math.min(Math.max(preferred.width, zone), Math.max(zone, bounds.width / 2));
            int height = preferred == null ? zone : Math.min(Math.max(preferred.height, zone), Math.max(zone, bounds.height / 2));
            int x = region.isLeftSide() ? bounds.x : bounds.x + bounds.width - width;
            int y = region.isTopSide() ? bounds.y : bounds.y + bounds.height - height;
            return new Rectangle(x, y, width, height);
        }
        return getRegionBounds(DockRegion.CENTER);
    }

    private Rectangle getRegionBounds(DockRegion region) {
        Component component = displayedRegionComponents.get(region);
        if (component == null || !component.isShowing()) return null;
        return SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), this);
    }

    private void beginPossibleDockDrag(String key, MouseEvent event) {
        DockEntry entry = docksByKey.get(key);
        if (!canStartDockDrag(entry)) return;

        Point location = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), this);
        dragSession = new DragSession(entry, location);
    }

    private void updatePossibleDockDrag(MouseEvent event) {
        if (dragSession == null) return;

        Point location = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), this);
        dragSession.location = location;
        if (!dragSession.active && dragSession.start.distance(location) < dockDragStartThreshold) return;

        if (!dragSession.active) {
            dragSession.active = true;
            dispatchDockEvent(EventDockPanel.DOCK_DRAG_START, dragSession.entry, Map.of(
                    "sourceRegion", dragSession.sourceRegion,
                    "location", new Point(location)
            ));
        }

        DockRegion target = resolveDockDropRegion(location);
        DockDropContext context = new DockDropContext(this, dragSession.entry, dragSession.sourceRegion, target, location);
        previewAllowed = target != null
                && target != dragSession.sourceRegion
                && canDropDock(context);
        previewRegion = previewAllowed ? target : null;

        dispatchDockEvent(EventDockPanel.DOCK_DRAG_OVER, dragSession.entry, new HashMap<>() {{
            put("sourceRegion", dragSession.sourceRegion);
            put("targetRegion", target);
            put("location", new Point(location));
            put("allowed", previewAllowed);
        }});
        repaint();
    }

    private void finishPossibleDockDrag(MouseEvent event) {
        if (dragSession == null) return;

        DragSession session = dragSession;
        Point location = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), this);
        if (!session.active) {
            clearDragPreview();
            return;
        }

        DockRegion target = resolveDockDropRegion(location);
        DockDropContext context = new DockDropContext(this, session.entry, session.sourceRegion, target, location);
        boolean allowed = session.active
                && target != null
                && target != session.sourceRegion
                && canDropDock(context);

        if (!allowed) {
            dispatchDockEvent(EventDockPanel.DOCK_DRAG_CANCEL, session.entry, new HashMap<>() {{
                put("sourceRegion", session.sourceRegion);
                put("targetRegion", target);
                put("location", new Point(location));
            }});
            clearDragPreview();
            return;
        }

        boolean moved = moveDock(session.entry.getKey(), target, true);
        clearDragPreview();
        if (moved) {
            dispatchDockEvent(EventDockPanel.DOCK_DROP, session.entry, Map.of(
                    "sourceRegion", session.sourceRegion,
                    "targetRegion", target,
                    "location", new Point(location)
            ));
        }
    }

    private void clearDragPreview() {
        dragSession = null;
        previewRegion = null;
        previewAllowed = false;
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (!previewAllowed || previewRegion == null || dragSession == null || !dragSession.active) return;

        Rectangle bounds = getDockDropPreviewBounds(previewRegion);
        if (bounds == null || bounds.width <= 0 || bounds.height <= 0) return;

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float alpha = dockDropPreviewAlpha;
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g2.setColor(dockDropPreviewColor);
        g2.fillRoundRect(bounds.x + 4, bounds.y + 4, Math.max(0, bounds.width - 8), Math.max(0, bounds.height - 8), 10, 10);
        g2.setComposite(AlphaComposite.SrcOver.derive(Math.min(1f, alpha + 0.2f)));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bounds.x + 4, bounds.y + 4, Math.max(0, bounds.width - 8), Math.max(0, bounds.height - 8), 10, 10);
        g2.dispose();
    }

    private void moveDockSilently(DockEntry entry, DockRegion targetRegion) {
        DockRegion oldRegion = entry.getRegion();
        TabbedPanel oldGroup = getGroupForDock(entry);
        TabEntry tab = oldGroup.getEntry(entry.getKey());
        entry.updateFrom(tab, oldRegion);
        if (!oldGroup.removeTab(entry.getKey())) return;
        removeSplitGroup(entry.getKey(), oldRegion);
        entry.setRegion(targetRegion);
        addEntryToGroup(entry, null);
    }

    private DockEntry removeEntry(String key) {
        DockEntry entry = docksByKey.remove(key);
        if (entry != null) {
            keyByComponent.remove(entry.getComponent());
            removeSplitGroup(key, entry.getRegion());
        }
        return entry;
    }

    private void afterRegionContentChange(DockRegion region) {
        captureVisibleRegionSizes();
        rebuildLayout();
        if (!isRegionVisible(region)) {
            dispatchDockEvent(EventDockPanel.REGION_EMPTY, null, Map.of("region", region));
        }
    }

    private void rebuildLayout() {
        adjustingDividers = true;
        applyPreferredRegionSizes();

        content.removeAll();
        displayedRegionComponents.clear();
        Component layout = createRegionLayout(DockRegion.CENTER);

        if (isRegionDisplayed(DockRegion.TOP)) {
            layout = createSplit(DockRegion.TOP, createRegionLayout(DockRegion.TOP), layout);
        }

        Component leftRail = createSideRail(DockRegion.TOP_LEFT, DockRegion.LEFT, DockRegion.BOTTOM_LEFT);
        if (leftRail != null) {
            layout = createSplit(DockRegion.LEFT, leftRail, layout);
        }

        Component rightRail = createSideRail(DockRegion.TOP_RIGHT, DockRegion.RIGHT, DockRegion.BOTTOM_RIGHT);
        if (rightRail != null) {
            layout = createSplit(DockRegion.RIGHT, layout, rightRail);
        }

        if (isRegionDisplayed(DockRegion.BOTTOM)) {
            layout = createSplit(DockRegion.BOTTOM, layout, createRegionLayout(DockRegion.BOTTOM));
        }

        content.add(layout, BorderLayout.CENTER);
        content.revalidate();
        content.repaint();
        // A tab selection event can trigger this rebuild while JTabbedPane is inserting a tab.
        // Changing its UI synchronously would uninstall BasicTabbedPaneUI mid-event.
        SwingUtilities.invokeLater(this::updateDockGroupHeaderVisibility);
        dispatchDockEvent(EventDockPanel.LAYOUT_CHANGE, null, Map.of("layout", layout));
        SwingUtilities.invokeLater(() -> adjustingDividers = false);
    }

    private Component createSideRail(DockRegion topRegion, DockRegion middleRegion, DockRegion bottomRegion) {
        boolean topVisible = isRegionDisplayed(topRegion);
        boolean middleVisible = isRegionDisplayed(middleRegion);
        boolean bottomVisible = isRegionDisplayed(bottomRegion);
        if (!topVisible && !middleVisible && !bottomVisible) return null;

        Component layout = middleVisible ? createRegionLayout(middleRegion) : null;
        if (topVisible) {
            Component top = createRegionLayout(topRegion);
            layout = layout == null ? top : createVerticalSplit(topRegion, top, layout);
        }
        if (bottomVisible) {
            Component bottom = createRegionLayout(bottomRegion);
            layout = layout == null ? bottom : createVerticalSplit(bottomRegion, layout, bottom);
        }
        if (layout instanceof JComponent component) {
            Dimension size = resolveSideRailPreferredSize(topRegion, middleRegion, bottomRegion);
            if (size != null) component.setPreferredSize(size);
        }
        return layout;
    }

    private Component createRegionLayout(DockRegion region) {
        List<TabbedPanel> regionGroups = getRegionGroups(region);
        if (regionGroups.isEmpty()) return groups.get(region);

        Component layout = regionGroups.getFirst();
        for (int index = 1; index < regionGroups.size(); index++) {
            JSplitPane split = createSameRegionSplit(region, layout, regionGroups.get(index));
            int leftCount = index;
            int totalCount = index + 1;
            SwingUtilities.invokeLater(() -> split.setDividerLocation((double) leftCount / totalCount));
            layout = split;
        }
        displayedRegionComponents.put(region, layout);
        return layout;
    }

    private JSplitPane createSameRegionSplit(DockRegion region, Component first, Component second) {
        int orientation = region == DockRegion.TOP || region == DockRegion.BOTTOM || region == DockRegion.CENTER
                ? JSplitPane.HORIZONTAL_SPLIT
                : JSplitPane.VERTICAL_SPLIT;
        JSplitPane split = createSeparator(region, first, second, orientation, false);
        split.setResizeWeight(0.5);
        return split;
    }

    private Dimension resolveSideRailPreferredSize(DockRegion topRegion, DockRegion middleRegion, DockRegion bottomRegion) {
        int width = 0;
        for (DockRegion region : List.of(topRegion, middleRegion, bottomRegion)) {
            if (!isRegionDisplayed(region)) continue;
            Dimension preferred = resolveRegionPreferredSize(region);
            if (preferred != null) width = Math.max(width, preferred.width);
        }
        return width > 0 ? new Dimension(width, 1) : null;
    }

    private JSplitPane createSplit(DockRegion region, Component first, Component second) {
        boolean horizontal = region == DockRegion.LEFT || region == DockRegion.RIGHT;
        int orientation = horizontal ? JSplitPane.HORIZONTAL_SPLIT : JSplitPane.VERTICAL_SPLIT;
        JSplitPane split = createSeparator(region, first, second, orientation);
        split.setResizeWeight(region == DockRegion.RIGHT || region == DockRegion.BOTTOM ? 1.0 : 0.0);
        applyDividerLocation(region, split);
        return split;
    }

    private JSplitPane createVerticalSplit(DockRegion region, Component first, Component second) {
        JSplitPane split = createSeparator(region, first, second, JSplitPane.VERTICAL_SPLIT);
        split.setResizeWeight(region == DockRegion.BOTTOM_LEFT || region == DockRegion.BOTTOM_RIGHT ? 1.0 : 0.0);
        applyDividerLocation(region, split);
        return split;
    }

    private JSplitPane createSeparator(DockRegion region, Component first, Component second, int orientation) {
        return createSeparator(region, first, second, orientation, true);
    }

    private JSplitPane createSeparator(DockRegion region, Component first, Component second, int orientation, boolean captureResize) {
        JSplitPane split = dockSeparatorFactory == null
                ? null
                : dockSeparatorFactory.createSeparator(this, region, first, second, orientation);
        if (split == null) {
            split = new JSplitPane(orientation);
            split.setContinuousLayout(true);
            split.setBorder(null);
            split.setOneTouchExpandable(isSplitOneTouchExpandableEnabled());
        } else {
            split.setOrientation(orientation);
        }

        if (orientation == JSplitPane.HORIZONTAL_SPLIT) {
            split.setLeftComponent(first);
            split.setRightComponent(second);
        } else {
            split.setTopComponent(first);
            split.setBottomComponent(second);
        }
        if (captureResize) installDividerResizeCapture(region, split);
        return split;
    }

    private void installDividerResizeCapture(DockRegion region, JSplitPane split) {
        split.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY, evt -> {
            if (adjustingDividers) return;
            rememberRegionSizeFromResize(region);
        });
    }

    private void rememberRegionSizeFromResize(DockRegion region) {
        if (!preserveDockSizeOnReopen || region == null || region == DockRegion.CENTER) return;
        if (!isRegionVisible(region)) return;

        Component component = displayedRegionComponents.get(region);
        if (component == null || component.getWidth() <= 0 || component.getHeight() <= 0) return;

        Dimension size;
        if (region == DockRegion.LEFT || region == DockRegion.RIGHT) {
            size = new Dimension(component.getWidth(), 1);
        } else if (region == DockRegion.TOP || region == DockRegion.BOTTOM) {
            size = new Dimension(1, component.getHeight());
        } else {
            size = new Dimension(component.getWidth(), component.getHeight());
        }

        for (DockEntry dock : getDocks(region)) {
            dock.setPreferredSize(new Dimension(size));
            rememberedDockSizes.put(dock.getKey(), new Dimension(size));
            rememberedDockRegions.put(dock.getKey(), region);
        }
    }

    private void restoreRememberedDockSize(DockEntry entry) {
        if (!preserveDockSizeOnReopen || entry == null) return;
        Dimension remembered = rememberedDockSizes.get(entry.getKey());
        DockRegion region = rememberedDockRegions.get(entry.getKey());
        if (remembered != null && region == entry.getRegion()) {
            entry.setPreferredSize(new Dimension(remembered));
        }
    }

    private void forgetRememberedDockSize(String key) {
        rememberedDockSizes.remove(key);
        rememberedDockRegions.remove(key);
    }

    private void captureVisibleRegionSizes() {
        for (DockRegion region : DockRegion.values()) {
            if (region == DockRegion.CENTER || !isRegionVisible(region)) continue;

            Component component = displayedRegionComponents.get(region);
            if (component == null || component.getWidth() <= 0 || component.getHeight() <= 0) continue;

            if (region == DockRegion.LEFT || region == DockRegion.RIGHT) {
                preferredRegionSizes.put(region, new Dimension(component.getWidth(), 1));
            } else if (region == DockRegion.TOP || region == DockRegion.BOTTOM) {
                preferredRegionSizes.put(region, new Dimension(1, component.getHeight()));
            } else {
                preferredRegionSizes.put(region, new Dimension(component.getWidth(), component.getHeight()));
            }
            explicitRegionPreferredSizes.add(region);
        }
    }

    private Dimension resolveRegionPreferredSize(DockRegion region) {
        DockEntry selectedDock = getSelectedDock(region);
        Dimension dockPreferred = selectedDock == null ? null : selectedDock.getPreferredSize();
        if (dockPreferred != null) return dockPreferred;

        Dimension regionPreferred = preferredRegionSizes.get(region);
        if (explicitRegionPreferredSizes.contains(region)) {
            return regionPreferred == null ? null : new Dimension(regionPreferred);
        }

        Dimension componentPreferred = selectedDock == null ? null : selectedDock.getComponent().getPreferredSize();
        if (hasUsablePreferredSize(componentPreferred)) {
            return new Dimension(componentPreferred);
        }

        return regionPreferred == null ? null : new Dimension(regionPreferred);
    }

    private boolean hasUsablePreferredSize(Dimension preferredSize) {
        return preferredSize != null && (preferredSize.width > 0 || preferredSize.height > 0);
    }

    private DockEntry getSelectedDock(DockRegion region) {
        TabbedPanel group = getGroup(region);
        if (group == null || group.isEmpty()) return null;

        String key = group.getCurrentKey();
        if (key == null) {
            List<TabEntry> entries = group.getEntries();
            if (!entries.isEmpty()) key = entries.getFirst().getKey();
        }
        return key == null ? null : docksByKey.get(key);
    }

    private void applyDividerLocation(DockRegion region, JSplitPane split) {
        Dimension preferred = resolveRegionPreferredSize(region);
        if (preferred == null) return;

        if (region == DockRegion.LEFT) {
            split.setDividerLocation(Math.max(1, preferred.width));
        } else if (region == DockRegion.TOP || region == DockRegion.TOP_LEFT || region == DockRegion.TOP_RIGHT) {
            split.setDividerLocation(Math.max(1, preferred.height));
        } else if (region == DockRegion.RIGHT) {
            SwingUtilities.invokeLater(() -> {
                int width = split.getWidth();
                if (width > 0) split.setDividerLocation(Math.max(1, width - preferred.width));
            });
        } else if (region == DockRegion.BOTTOM || region == DockRegion.BOTTOM_LEFT || region == DockRegion.BOTTOM_RIGHT) {
            SwingUtilities.invokeLater(() -> {
                int height = split.getHeight();
                if (height > 0) split.setDividerLocation(Math.max(1, height - preferred.height));
            });
        }
    }

    private void applyPreferredRegionSizes() {
        for (DockRegion region : DockRegion.values()) {
            Dimension preferred = resolveRegionPreferredSize(region);
            for (TabbedPanel group : getRegionGroups(region)) {
                group.setPreferredSize(preferred == null ? null : new Dimension(preferred));
            }
        }
    }

    private void updateDockGroupHeaderVisibility() {
        for (DockRegion region : groups.keySet()) {
            for (TabbedPanel group : getRegionGroups(region)) {
                updateDockGroupHeaderVisibility(group);
            }
        }
    }

    private void updateDockGroupHeaderVisibility(DockRegion region) {
        for (TabbedPanel group : getRegionGroups(region)) {
            updateDockGroupHeaderVisibility(group);
        }
    }

    private void updateDockGroupHeaderVisibility(TabbedPanel group) {
        if (group == null) return;

        boolean hideHeader = shouldHideGroupHeader(group);
        boolean alreadyHidden = group.getTabbedPane().getUI() instanceof HiddenDockTabbedPaneUI;

        if (hideHeader) {
            group.getTabbedPane().setUI(new HiddenDockTabbedPaneUI());
            group.updateAllTabHeaders();
            scrubSingleHiddenTabHeader(group);
            group.getTabbedPane().revalidate();
            group.getTabbedPane().repaint();
        } else if (alreadyHidden) {
            group.getTabbedPane().updateUI();
            group.updateAllTabHeaders();
            group.getTabbedPane().revalidate();
            group.getTabbedPane().repaint();
        }
    }

    private void scrubSingleHiddenTabHeader(TabbedPanel group) {
        if (group == null || group.getTabCount() != 1) return;
        TabEntry tab = group.getEntries().getFirst();
        int index = group.indexOf(tab.getKey());
        if (index < 0) return;
        group.getTabbedPane().setTabComponentAt(index, null);
        group.getTabbedPane().setTitleAt(index, "");
        group.getTabbedPane().setIconAt(index, null);
    }

    protected boolean shouldHideGroupHeader(TabbedPanel group) {
        if (group == null || group.getTabCount() != 1) return false;
        TabEntry tab = group.getEntries().getFirst();
        DockEntry dock = docksByKey.get(tab.getKey());
        return dock != null && !dock.isTabHeaderVisible();
    }

    private DockEvent dispatchDockEvent(String eventType, DockEntry entry, Map<String, Object> extraProps) {
        Map<String, Object> props = new HashMap<>();
        if (entry != null) {
            props.put("key", entry.getKey());
            props.put("title", entry.getTitle());
            props.put("component", entry.getComponent());
            props.put("region", entry.getRegion());
            props.put("closable", entry.isClosable());
            props.put("pinned", entry.isPinned());
            props.put("dirty", entry.isDirty());
        }
        if (extraProps != null) props.putAll(extraProps);

        DockEvent event = new DockEvent(this, entry, eventType, props);
        dispatchEventObject(eventType, event);
        return event;
    }

    protected void dispatchEventObject(String eventType, DockEvent event) {
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

    private String createDockKey() {
        String key;
        do {
            key = "dock-" + UUID.randomUUID();
        } while (docksByKey.containsKey(key));
        return key;
    }

    private static final class DragSession {
        private final DockEntry entry;
        private final DockRegion sourceRegion;
        private final Point start;
        private Point location;
        private boolean active;

        private DragSession(DockEntry entry, Point start) {
            this.entry = entry;
            this.sourceRegion = entry.getRegion();
            this.start = new Point(start);
            this.location = new Point(start);
        }
    }

    private static final class HiddenDockTabbedPaneUI extends BasicTabbedPaneUI {
        @Override
        protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
            return 0;
        }

        @Override
        protected int calculateTabAreaWidth(int tabPlacement, int vertRunCount, int maxTabWidth) {
            return 0;
        }

        @Override
        protected Insets getTabAreaInsets(int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }

        @Override
        protected Insets getContentBorderInsets(int tabPlacement) {
            return new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex) {
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        }
    }
}
