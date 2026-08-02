package dtm.stools.component.panels.window;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.panels.base.PanelEventListener;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;

public class WindowDesktopPanel extends PanelEventListener {
    protected static final int WINDOW_LAYER = 100;
    protected static final int SNAP_PREVIEW_LAYER = 175;
    protected static final int SNAP_ASSIST_LAYER = 185;
    protected static final int SNAP_DRAG_SELECTOR_LAYER = 195;
    protected static final int MODAL_OVERLAY_LAYER = 200;
    protected static final int MODAL_WINDOW_LAYER = 300;

    protected final JLayeredPane layeredPane;
    protected final WindowModalOverlay modalOverlay;
    protected final WindowSnapPreviewOverlay snapPreviewOverlay;
    protected final WindowSnapAssistOverlay snapAssistOverlay;
    protected final WindowSnapDragSelector snapDragSelector;
    protected final WindowMinimizedBar minimizedBar;
    protected final Map<String, WindowPanel> windowsByKey = new LinkedHashMap<>();
    protected final Map<WindowPanel, AbstractButton> minimizedButtons = new LinkedHashMap<>();
    protected final Map<WindowPanel, SnapLayoutBinding> snapLayoutBindings = new IdentityHashMap<>();

    private WindowPlacementPolicy placementPolicy;
    private WindowSnapPolicy snapPolicy;
    private WindowAnimator windowAnimator;
    private WindowMinimizedMenuFactory minimizedMenuFactory;
    private boolean minimizedBarContextMenuEnabled;
    private boolean snapLayoutsEnabled = true;
    private boolean snapAssistEnabled = true;
    private WindowSnapLayoutTrigger snapLayoutTrigger = WindowSnapLayoutTrigger.TOP_CENTER;
    private int snapLayoutTopActivationDistance = 28;
    private int snapLayoutTopActivationWidth = 260;
    private int snapLayoutHoverDelayMillis = 450;
    private Insets maximizedInsets = new Insets(0, 0, 0, 0);
    private WindowPanel activeWindow;

    public WindowDesktopPanel() {
        this(false);
    }

    public WindowDesktopPanel(boolean minimizedBarAutoHideEnabled) {
        super(new WindowDesktopLayout(), false);
        setLayout(createDesktopLayout());
        setOpaque(true);
        Color desktopBackground = UIManager.getColor("Desktop.background");
        if (desktopBackground == null) desktopBackground = UIManager.getColor("Panel.background");
        setBackground(desktopBackground == null ? new Color(0x303236) : desktopBackground.darker());
        layeredPane = createLayeredPane();
        modalOverlay = createModalOverlay();
        snapPreviewOverlay = createSnapPreviewOverlay();
        snapAssistOverlay = createSnapAssistOverlay();
        snapDragSelector = createSnapDragSelector();
        minimizedBar = createMinimizedWindowBar();
        placementPolicy = createPlacementPolicy();
        snapPolicy = createSnapPolicy();
        windowAnimator = createWindowAnimator();
        minimizedMenuFactory = createMinimizedMenuFactory();
        configureLayeredPane(layeredPane);
        configureModalOverlay(modalOverlay);
        configureSnapPreviewOverlay(snapPreviewOverlay);
        configureSnapAssistOverlay(snapAssistOverlay);
        configureSnapDragSelector(snapDragSelector);
        configureMinimizedBar(minimizedBar);
        installMinimizedBarEvents(minimizedBar);
        minimizedBar.autoHideEnabled(minimizedBarAutoHideEnabled);
        layeredPane.add(snapPreviewOverlay, Integer.valueOf(SNAP_PREVIEW_LAYER));
        layeredPane.add(snapAssistOverlay, Integer.valueOf(SNAP_ASSIST_LAYER));
        layeredPane.add(snapDragSelector, Integer.valueOf(SNAP_DRAG_SELECTOR_LAYER));
        add(layeredPane, BorderLayout.CENTER);
        add(minimizedBar, BorderLayout.SOUTH);
        installDesktopListeners();
    }

    protected JLayeredPane createLayeredPane() { return new JLayeredPane(); }
    protected WindowModalOverlay createModalOverlay() { return new WindowModalOverlay(); }
    protected WindowSnapPreviewOverlay createSnapPreviewOverlay() { return new WindowSnapPreviewOverlay(); }
    protected WindowSnapAssistOverlay createSnapAssistOverlay() { return new WindowSnapAssistOverlay(this); }
    protected WindowSnapDragSelector createSnapDragSelector() { return new WindowSnapDragSelector(); }
    protected WindowMinimizedBar createMinimizedWindowBar() { return new WindowMinimizedBar(); }
    protected WindowPanel createWindow(WindowConfig config) { return new WindowPanel(config); }
    protected WindowPlacementPolicy createPlacementPolicy() { return new DefaultWindowPlacementPolicy(); }
    protected WindowSnapPolicy createSnapPolicy() { return new DefaultWindowSnapPolicy(); }
    protected WindowAnimator createWindowAnimator() { return new DefaultWindowAnimator(); }
    protected WindowMinimizedMenuFactory createMinimizedMenuFactory() {
        return new DefaultWindowMinimizedMenuFactory();
    }
    protected WindowSnapLayoutPopup createSnapLayoutPopup(WindowPanel window) {
        return new WindowSnapLayoutPopup(this, window);
    }
    protected LayoutManager createDesktopLayout() { return new WindowDesktopLayout(); }
    protected void configureLayeredPane(JLayeredPane pane) { pane.setOpaque(false); }
    protected void configureModalOverlay(WindowModalOverlay overlay) { overlay.setVisible(false); }
    protected void configureSnapPreviewOverlay(WindowSnapPreviewOverlay overlay) { overlay.setVisible(false); }
    protected void configureSnapAssistOverlay(WindowSnapAssistOverlay overlay) { overlay.setVisible(false); }
    protected void configureSnapDragSelector(WindowSnapDragSelector selector) { selector.setVisible(false); }
    protected void configureMinimizedBar(WindowMinimizedBar bar) {}
    protected void installMinimizedBarEvents(WindowMinimizedBar bar) {
        bar.addPropertyChangeListener(WindowMinimizedBar.PROPERTY_EXPANDED, event -> {
            boolean expanded = Boolean.TRUE.equals(event.getNewValue());
            dispatchDesktopEvent(expanded ? EventWindowPanel.MINIMIZED_BAR_EXPAND
                            : EventWindowPanel.MINIMIZED_BAR_COLLAPSE,
                    null, Map.of("expanded", expanded));
            onMinimizedBarExpansionChanged(expanded);
        });
        bar.addPropertyChangeListener(WindowMinimizedBar.PROPERTY_AUTO_HIDE, event -> {
            boolean enabled = Boolean.TRUE.equals(event.getNewValue());
            dispatchDesktopEvent(EventWindowPanel.MINIMIZED_BAR_AUTO_HIDE_CHANGE,
                    null, Map.of("enabled", enabled));
            onMinimizedBarAutoHideChanged(enabled);
        });
    }
    protected void configureWindow(WindowPanel window, WindowConfig config) {}
    protected void configureSnapLayoutPopup(WindowPanel window, WindowSnapLayoutPopup popup) {}

    protected boolean canShowSnapLayouts(WindowPanel window) {
        return window != null && windowsByKey.get(window.getWindowKey()) == window
                && window.isSnapEnabled() && window.isSnapLayoutsEnabled()
                && window.getSnapLayoutTrigger() != WindowSnapLayoutTrigger.DISABLED
                && window.isMaximizable() && !window.isModal()
                && window.getWindowState() != WindowState.MINIMIZED;
    }

    protected void installSnapLayoutSupport(WindowPanel window) {
        uninstallSnapLayoutSupport(window);
        if (!canShowSnapLayouts(window)) return;
        if (!window.getSnapLayoutTrigger().supportsMaximizeButton()) return;
        WindowControlButton button = window.getControlButton(WindowControl.MAXIMIZE_RESTORE);
        if (button == null) return;
        WindowSnapLayoutPopup popup = createSnapLayoutPopup(window);
        if (popup == null) return;
        configureSnapLayoutPopup(window, popup);
        configureSnapLayoutPopupEvents(window, popup);
        javax.swing.Timer timer = new javax.swing.Timer(snapLayoutHoverDelayMillis,
                event -> showSnapLayouts(window));
        timer.setRepeats(false);
        MouseAdapter listener = createSnapLayoutHoverListener(window, popup, timer);
        button.addMouseListener(listener);
        snapLayoutBindings.put(window, new SnapLayoutBinding(button, popup, timer, listener));
    }

    protected MouseAdapter createSnapLayoutHoverListener(WindowPanel window,
                                                           WindowSnapLayoutPopup popup,
                                                           javax.swing.Timer timer) {
        return new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent event) {
                if (canShowSnapLayouts(window)) timer.restart();
            }

            @Override public void mouseExited(MouseEvent event) { timer.stop(); }

            @Override public void mousePressed(MouseEvent event) {
                timer.stop();
                if (popup.isVisible()) popup.setVisible(false);
            }
        };
    }

    protected void configureSnapLayoutPopupEvents(WindowPanel window, WindowSnapLayoutPopup popup) {
        popup.addPopupMenuListener(new PopupMenuListener() {
            private boolean opened;

            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                opened = true;
                dispatchDesktopEvent(EventWindowPanel.SNAP_LAYOUT_MENU_OPEN, window,
                        Map.of("popup", popup));
                onSnapLayoutMenuChanged(window, popup, true);
            }

            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent event) { closed(); }
            @Override public void popupMenuCanceled(PopupMenuEvent event) { closed(); }

            private void closed() {
                if (!opened) return;
                opened = false;
                clearSnapLayoutPreview();
                dispatchDesktopEvent(EventWindowPanel.SNAP_LAYOUT_MENU_CLOSE, window,
                        Map.of("popup", popup));
                onSnapLayoutMenuChanged(window, popup, false);
            }
        });
    }

    protected void uninstallSnapLayoutSupport(WindowPanel window) {
        SnapLayoutBinding binding = snapLayoutBindings.remove(window);
        if (binding == null) return;
        binding.timer.stop();
        binding.popup.setVisible(false);
        binding.button.removeMouseListener(binding.listener);
    }
    protected void paintDesktopBackground(Graphics2D graphics) {
        Color color = getBackground();
        if (color != null) { graphics.setColor(color); graphics.fillRect(0, 0, getWidth(), getHeight()); }
    }
    protected boolean canActivateWindow(WindowPanel window) {
        WindowPanel modal = getTopModalWindow();
        return modal == null || modal == window;
    }
    protected boolean canPlaceWindow(WindowPanel window, Rectangle bounds) { return true; }

    protected void installDesktopListeners() {
        layeredPane.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                resizeModalOverlay();
                resizeSnapOverlays();
                for (WindowPanel window : getWindows()) {
                    if (window.getWindowState() == WindowState.MAXIMIZED) {
                        window.setBounds(resolveMaximizedBounds(window));
                    } else if (window.getSnap() != WindowSnap.NONE) {
                        window.setBounds(snapPolicy.resolveBounds(WindowDesktopPanel.this, window, window.getSnap()));
                    } else {
                        window.setBounds(constrainWindowBounds(window, window.getBounds()));
                    }
                }
            }
        });
    }

    public WindowPanel openWindow(WindowConfig config) {
        Objects.requireNonNull(config, "config");
        WindowPanel window = createWindow(config);
        configureWindow(window, config);
        addWindow(window, config.getBounds());
        return containsWindow(window.getWindowKey()) ? window.open() : window;
    }

    public WindowPanel addWindow(WindowPanel window) { return addWindow(window, null); }

    protected WindowPanel addWindow(WindowPanel window, Rectangle requestedBounds) {
        Objects.requireNonNull(window, "window");
        String key = window.getWindowKey();
        if (windowsByKey.containsKey(key)) throw new IllegalArgumentException("Window key already registered: " + key);
        if (windowsByKey.containsValue(window)) throw new IllegalArgumentException("Window already registered");
        WindowEvent before = dispatchDesktopEvent(EventWindowPanel.BEFORE_WINDOW_ADD, window, Map.of());
        if (before.isCanceled()) return window;
        windowsByKey.put(key, window);
        layeredPane.add(window, Integer.valueOf(WINDOW_LAYER));
        Rectangle bounds = resolveInitialBounds(window, requestedBounds);
        window.setBounds(constrainWindowBounds(window, bounds));
        window.setNormalBoundsDirect(window.getBounds());
        installSnapLayoutSupport(window);
        dispatchDesktopEvent(EventWindowPanel.WINDOW_ADD, window, Map.of());
        onWindowAdded(window);
        revalidate(); repaint();
        return window;
    }

    protected Rectangle resolveInitialBounds(WindowPanel window, Rectangle requestedBounds) {
        return placementPolicy.resolveInitialBounds(this, window, requestedBounds);
    }

    public boolean closeWindow(String key) {
        WindowPanel window = windowsByKey.get(key);
        if (window == null) return false;
        boolean wasVisible = window.isVisible();
        WindowCloseOperation operation = window.getCloseOperation();
        window.close();
        return operation == WindowCloseOperation.REMOVE
                ? !windowsByKey.containsKey(key)
                : wasVisible && !window.isVisible();
    }

    public boolean removeWindow(String key) {
        WindowPanel window = windowsByKey.get(key);
        return window != null && removeWindow(window);
    }

    public boolean removeWindow(WindowPanel window) {
        if (window == null || windowsByKey.get(window.getWindowKey()) != window) return false;
        WindowEvent before = dispatchDesktopEvent(EventWindowPanel.BEFORE_WINDOW_REMOVE, window, Map.of());
        if (before.isCanceled()) return false;
        removeWindowDirect(window);
        window.onDisposed();
        return true;
    }

    boolean removeWindowAfterClose(WindowPanel window) {
        if (window == null || windowsByKey.get(window.getWindowKey()) != window) return false;
        WindowEvent before = dispatchDesktopEvent(EventWindowPanel.BEFORE_WINDOW_REMOVE, window, Map.of());
        if (before.isCanceled()) return false;
        removeWindowDirect(window);
        if (!windowsByKey.containsKey(window.getWindowKey())) {
            dispatchDesktopEvent(EventWindowPanel.WINDOW_CLOSE, window,
                    Map.of("operation", WindowCloseOperation.REMOVE));
            return true;
        }
        return false;
    }

    protected void removeWindowDirect(WindowPanel window) {
        clearSnapLayoutPreview();
        if (snapDragSelector.getWindow() == window) cancelSnapLayoutDrag();
        if (snapAssistOverlay.getSourceWindow() == window
                || snapAssistOverlay.getCandidates().contains(window)) closeSnapAssist();
        if (activeWindow == window) {
            activeWindow = null;
            window.setActiveDirect(false);
        }
        windowsByKey.remove(window.getWindowKey());
        removeMinimizedButton(window);
        uninstallSnapLayoutSupport(window);
        layeredPane.remove(window);
        updateModalState();
        dispatchDesktopEvent(EventWindowPanel.WINDOW_REMOVE, window, Map.of());
        onWindowRemoved(window);
        activateTopWindow();
        revalidate(); repaint();
    }

    public boolean activateWindow(String key) {
        WindowPanel window = windowsByKey.get(key);
        return window != null && activateWindow(window);
    }

    public boolean activateWindow(WindowPanel window) {
        if (window == null || !windowsByKey.containsValue(window) || !canActivateWindow(window)) return false;
        if (window.getWindowState() == WindowState.MINIMIZED) window.restore();
        if (!window.isVisible()) window.setVisible(true);
        if (activeWindow == window) {
            moveWindowToFront(window);
            return true;
        }
        WindowEvent before = window.dispatchWindowEvent(EventWindowPanel.BEFORE_WINDOW_ACTIVATE,
                Map.of("oldWindowKey", activeWindow == null ? "" : activeWindow.getWindowKey()));
        if (before.isCanceled()) return false;
        WindowPanel old = activeWindow;
        activeWindow = window;
        if (old != null) old.setActiveDirect(false);
        moveWindowToFront(window);
        window.setActiveDirect(true);
        window.requestFocusInWindow();
        Map<String, Object> properties = new HashMap<>();
        properties.put("oldWindow", old);
        dispatchDesktopEvent(EventWindowPanel.ACTIVE_WINDOW_CHANGE, window, properties);
        onActiveWindowChanged(old, window);
        return true;
    }

    protected void moveWindowToFront(WindowPanel window) {
        int layer = window.isModal() ? MODAL_WINDOW_LAYER : WINDOW_LAYER;
        layeredPane.setLayer(window, layer);
        layeredPane.moveToFront(window);
        if (window.isModal()) {
            layeredPane.setLayer(modalOverlay, MODAL_OVERLAY_LAYER);
            layeredPane.moveToFront(modalOverlay);
            layeredPane.setLayer(window, MODAL_WINDOW_LAYER);
            layeredPane.moveToFront(window);
        }
        dispatchDesktopEvent(EventWindowPanel.WINDOW_ORDER_CHANGE, window,
                Map.of("zOrder", layeredPane.getComponentZOrder(window)));
        repaint();
    }

    void applyWindowState(WindowPanel window, WindowState oldState, WindowState newState) {
        switch (newState) {
            case MINIMIZED -> {
                Rectangle area = getAvailableDesktopBounds();
                Rectangle target = new Rectangle(
                        Math.max(0, Math.min(window.getX(), Math.max(0, area.width - 180))),
                        Math.max(0, area.height - 30), 180, 28);
                animateWindow(window, WindowAnimationType.MINIMIZE, target, 1f, 0f, () -> {
                    window.setVisible(false);
                    window.setAnimationAlpha(1f);
                    addMinimizedButton(window);
                    if (activeWindow == window) {
                        activeWindow = null;
                        window.setActiveDirect(false);
                    }
                    activateTopWindow();
                });
            }
            case MAXIMIZED -> {
                removeMinimizedButton(window);
                window.setSnapDirect(WindowSnap.NONE);
                window.setVisible(true);
                animateWindow(window, WindowAnimationType.MAXIMIZE, resolveMaximizedBounds(window), 1f, 1f, null);
                activateWindow(window);
            }
            case NORMAL -> {
                removeMinimizedButton(window);
                window.setSnapDirect(WindowSnap.NONE);
                Rectangle normal = window.getNormalBounds();
                window.setVisible(true);
                WindowAnimationType type = oldState == WindowState.MINIMIZED
                        ? WindowAnimationType.RESTORE : WindowAnimationType.RESTORE;
                if (oldState == WindowState.MINIMIZED) window.setAnimationAlpha(0f);
                animateWindow(window, type, constrainWindowBounds(window, normal),
                        oldState == WindowState.MINIMIZED ? 0f : 1f, 1f, null);
                activateWindow(window);
            }
        }
        updateModalState();
        layoutChanged(window);
    }

    protected void addMinimizedButton(WindowPanel window) {
        if (minimizedButtons.containsKey(window)) return;
        AbstractButton button = createMinimizedWindowButton(window);
        configureMinimizedWindowButton(window, button);
        minimizedButtons.put(window, button);
        minimizedBar.add(button);
        minimizedBar.setAvailable(true);
        minimizedBar.revalidate(); minimizedBar.repaint();
        dispatchDesktopEvent(EventWindowPanel.MINIMIZED_BAR_CHANGE, window,
                Map.of("minimized", true, "count", minimizedButtons.size()));
    }

    protected AbstractButton createMinimizedWindowButton(WindowPanel window) {
        return minimizedBar.createWindowButton(window);
    }

    protected void configureMinimizedWindowButton(WindowPanel window, AbstractButton button) {
        button.setComponentPopupMenu(null);
        if (!minimizedBarContextMenuEnabled || minimizedMenuFactory == null) return;
        JPopupMenu menu = minimizedMenuFactory.createMenu(this, window);
        if (menu == null) return;
        configureMinimizedWindowMenu(window, menu);
        button.setComponentPopupMenu(menu);
    }

    protected void configureMinimizedWindowMenu(WindowPanel window, JPopupMenu menu) {
        menu.addPopupMenuListener(new PopupMenuListener() {
            private boolean opened;

            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent event) {
                opened = true;
                minimizedBar.setPopupActive(true);
                dispatchDesktopEvent(EventWindowPanel.MINIMIZED_BAR_MENU_OPEN, window,
                        Map.of("menu", menu));
                onMinimizedBarMenuChanged(window, menu, true);
            }

            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent event) { menuClosed(); }
            @Override public void popupMenuCanceled(PopupMenuEvent event) { menuClosed(); }

            private void menuClosed() {
                if (!opened) return;
                opened = false;
                minimizedBar.setPopupActive(false);
                dispatchDesktopEvent(EventWindowPanel.MINIMIZED_BAR_MENU_CLOSE, window,
                        Map.of("menu", menu));
                onMinimizedBarMenuChanged(window, menu, false);
            }
        });
    }

    void performMinimizedMenuAction(WindowPanel window, WindowMinimizedMenuAction action) {
        WindowEvent before = dispatchDesktopEvent(EventWindowPanel.BEFORE_MINIMIZED_BAR_MENU_ACTION,
                window, Map.of("action", action));
        if (before.isCanceled()) return;
        switch (action) {
            case RESTORE -> window.restore().activate();
            case MAXIMIZE -> maximizeFromMinimizedMenu(window);
            case CLOSE -> window.close();
        }
        dispatchDesktopEvent(EventWindowPanel.MINIMIZED_BAR_MENU_ACTION,
                window, Map.of("action", action));
        onMinimizedBarMenuAction(window, action);
    }

    protected void maximizeFromMinimizedMenu(WindowPanel window) {
        if (window.getWindowState() != WindowState.MINIMIZED) {
            window.maximize();
            return;
        }
        window.addEventListenerOnce(EventWindowPanel.WINDOW_ANIMATION_END,
                WindowAnimationEvent.class, event -> {
                    if (event.getAnimationType() == WindowAnimationType.RESTORE) {
                        SwingUtilities.invokeLater(window::maximize);
                    }
                });
        window.restore();
    }

    protected void removeMinimizedButton(WindowPanel window) {
        AbstractButton button = minimizedButtons.remove(window);
        if (button != null) minimizedBar.remove(button);
        minimizedBar.setAvailable(!minimizedButtons.isEmpty());
        minimizedBar.revalidate(); minimizedBar.repaint();
        if (button != null) {
            dispatchDesktopEvent(EventWindowPanel.MINIMIZED_BAR_CHANGE, window,
                    Map.of("minimized", false, "count", minimizedButtons.size()));
        }
    }

    protected void animateWindowBounds(WindowPanel window, Rectangle target, Runnable completion) {
        animateWindow(window, WindowAnimationType.RESTORE, target, 1f, 1f, completion);
    }

    protected WindowAnimationRequest createAnimationRequest(WindowPanel window, WindowAnimationType type,
                                                             Rectangle from, Rectangle to,
                                                             float fromAlpha, float toAlpha) {
        return new WindowAnimationRequest(window, type, from, to, fromAlpha, toAlpha,
                resolveAnimationDuration(type));
    }

    protected int resolveAnimationDuration(WindowAnimationType type) {
        return switch (type) {
            case OPEN -> 190;
            case CLOSE -> 140;
            case MINIMIZE -> 180;
            case RESTORE, MAXIMIZE, SNAP -> 210;
        };
    }

    protected Rectangle scaledBounds(Rectangle bounds, float scale) {
        int width = Math.max(1, Math.round(bounds.width * scale));
        int height = Math.max(1, Math.round(bounds.height * scale));
        return new Rectangle(bounds.x + (bounds.width - width) / 2,
                bounds.y + (bounds.height - height) / 2, width, height);
    }

    void animateWindowOpen(WindowPanel window, Runnable completion) {
        Rectangle target = new Rectangle(window.getBounds());
        Rectangle from = scaledBounds(target, .96f);
        window.setBounds(from);
        window.setAnimationAlpha(0f);
        animateWindow(window, WindowAnimationType.OPEN, target, 0f, 1f, completion);
    }

    void animateWindowClose(WindowPanel window, Runnable completion) {
        animateWindow(window, WindowAnimationType.CLOSE, scaledBounds(window.getBounds(), .96f),
                1f, 0f, completion);
    }

    void animateWindow(WindowPanel window, WindowAnimationType type, Rectangle target,
                       float fromAlpha, float toAlpha, Runnable completion) {
        if (windowAnimator.cancel(window)) window.animationCanceled();
        Rectangle from = new Rectangle(window.getBounds());
        WindowAnimationRequest request = createAnimationRequest(window, type, from, target, fromAlpha, toAlpha);
        window.animationStarted(request);
        windowAnimator.animate(request, frame -> window.animationFrame(type, frame), () -> {
            window.setBounds(target);
            window.setAnimationAlpha(toAlpha);
            window.animationEnded(type);
            if (completion != null) completion.run();
        });
    }

    void windowOpened(WindowPanel window) {
        if (window.isModal()) {
            updateModalState();
            dispatchDesktopEvent(EventWindowPanel.MODAL_OPEN, window, Map.of());
            onModalChanged(window, true);
        }
    }

    void windowClosed(WindowPanel window) {
        removeMinimizedButton(window);
        if (activeWindow == window) activeWindow = null;
        if (window.isModal()) {
            updateModalState();
            dispatchDesktopEvent(EventWindowPanel.MODAL_CLOSE, window, Map.of());
            onModalChanged(window, false);
        }
        activateTopWindow();
    }

    void windowModalityChanged(WindowPanel window, boolean oldValue, boolean newValue) {
        updateModalState();
        onModalChanged(window, newValue);
    }

    protected void updateModalState() {
        WindowPanel modal = getTopModalWindow();
        if (modal == null) {
            layeredPane.remove(modalOverlay);
            modalOverlay.setVisible(false);
        } else {
            if (modalOverlay.getParent() != layeredPane) layeredPane.add(modalOverlay, Integer.valueOf(MODAL_OVERLAY_LAYER));
            resizeModalOverlay();
            modalOverlay.setVisible(true);
            moveWindowToFront(modal);
        }
        layeredPane.revalidate(); layeredPane.repaint();
    }

    protected void resizeModalOverlay() { modalOverlay.setBounds(getAvailableDesktopBounds()); }
    protected void resizeSnapOverlays() {
        Rectangle bounds = getAvailableDesktopBounds();
        snapPreviewOverlay.setBounds(bounds);
        snapAssistOverlay.setBounds(bounds);
        snapAssistOverlay.doLayout();
    }

    protected WindowPanel getTopModalWindow() {
        return windowsByKey.values().stream()
                .filter(WindowPanel::isModal)
                .filter(WindowPanel::isVisible)
                .filter(window -> window.getWindowState() != WindowState.MINIMIZED)
                .min(Comparator.comparingInt(layeredPane::getComponentZOrder))
                .orElse(null);
    }

    protected void activateTopWindow() {
        WindowPanel modal = getTopModalWindow();
        if (modal != null) { activateWindow(modal); return; }
        windowsByKey.values().stream()
                .filter(WindowPanel::isVisible)
                .filter(window -> window.getWindowState() != WindowState.MINIMIZED)
                .min(Comparator.comparingInt(layeredPane::getComponentZOrder))
                .ifPresent(this::activateWindow);
    }

    public WindowDesktopPanel cascade() {
        Rectangle area = getAvailableDesktopBounds();
        int offset = 30;
        int index = 0;
        for (WindowPanel window : visibleNormalWindows()) {
            int width = Math.min(Math.max(420, window.getMinimumSize().width), area.width);
            int height = Math.min(Math.max(280, window.getMinimumSize().height), area.height);
            int x = (index * offset) % Math.max(1, area.width - width + 1);
            int y = (index * offset) % Math.max(1, area.height - height + 1);
            window.setSnapDirect(WindowSnap.NONE);
            window.setBounds(new Rectangle(x, y, width, height));
            window.setNormalBoundsDirect(window.getBounds());
            index++;
        }
        layoutChanged(null); return this;
    }

    public WindowDesktopPanel tileHorizontal() { return tile(false); }
    public WindowDesktopPanel tileVertical() { return tile(true); }

    protected WindowDesktopPanel tile(boolean vertical) {
        List<WindowPanel> windows = visibleNormalWindows();
        if (windows.isEmpty()) return this;
        Rectangle area = getAvailableDesktopBounds();
        for (int index = 0; index < windows.size(); index++) {
            WindowPanel window = windows.get(index);
            Rectangle bounds = vertical
                    ? sliceVertical(area, index, windows.size())
                    : sliceHorizontal(area, index, windows.size());
            window.setSnapDirect(WindowSnap.NONE);
            window.setBounds(bounds);
            window.setNormalBoundsDirect(bounds);
        }
        layoutChanged(null); return this;
    }

    protected Rectangle sliceVertical(Rectangle area, int index, int count) {
        int start = area.x + area.width * index / count;
        int end = area.x + area.width * (index + 1) / count;
        return new Rectangle(start, area.y, end - start, area.height);
    }

    protected Rectangle sliceHorizontal(Rectangle area, int index, int count) {
        int start = area.y + area.height * index / count;
        int end = area.y + area.height * (index + 1) / count;
        return new Rectangle(area.x, start, area.width, end - start);
    }

    protected List<WindowPanel> visibleNormalWindows() {
        return windowsByKey.values().stream().filter(WindowPanel::isVisible)
                .filter(window -> window.getWindowState() == WindowState.NORMAL).toList();
    }

    public WindowLayoutSnapshot captureLayout() {
        List<WindowLayoutSnapshot.WindowSnapshot> snapshots = new ArrayList<>();
        for (WindowPanel window : windowsByKey.values()) {
            snapshots.add(new WindowLayoutSnapshot.WindowSnapshot(window.getWindowKey(), window.getBounds(),
                    window.getNormalBounds(), window.getWindowState(), window.getSnap(),
                    layeredPane.getComponentZOrder(window), window.isVisible()));
        }
        return new WindowLayoutSnapshot(snapshots, activeWindow == null ? null : activeWindow.getWindowKey());
    }

    public WindowDesktopPanel restoreLayout(WindowLayoutSnapshot snapshot) {
        if (snapshot == null) return this;
        WindowEvent before = dispatchDesktopEvent(EventWindowPanel.BEFORE_LAYOUT_RESTORE, null,
                Map.of("snapshot", snapshot));
        if (before.isCanceled()) return this;
        List<WindowLayoutSnapshot.WindowSnapshot> ordered = new ArrayList<>(snapshot.windows());
        ordered.sort(Comparator.comparingInt(WindowLayoutSnapshot.WindowSnapshot::zOrder).reversed());
        for (WindowLayoutSnapshot.WindowSnapshot item : ordered) {
            WindowPanel window = windowsByKey.get(item.key());
            if (window == null) continue;
            removeMinimizedButton(window);
            window.setWindowStateDirect(item.state());
            window.setSnapDirect(item.snap());
            window.setNormalBoundsDirect(item.normalBounds());
            Rectangle bounds = item.bounds();
            if (bounds != null) window.setBounds(constrainWindowBounds(window, bounds));
            window.setVisible(item.visible() && item.state() != WindowState.MINIMIZED);
            if (item.state() == WindowState.MINIMIZED) addMinimizedButton(window);
            moveWindowToFront(window);
        }
        updateModalState();
        if (snapshot.activeWindowKey() != null) activateWindow(snapshot.activeWindowKey());
        layoutChanged(null);
        dispatchDesktopEvent(EventWindowPanel.LAYOUT_RESTORE, null, Map.of("snapshot", snapshot));
        onLayoutRestored(snapshot);
        return this;
    }

    public Rectangle constrainWindowBounds(WindowPanel window, Rectangle requested) {
        Rectangle area = getAvailableDesktopBounds();
        Rectangle result = requested == null ? new Rectangle(area) : new Rectangle(requested);
        Dimension minimum = window.getMinimumSize();
        if (area.width <= 0 || area.height <= 0) {
            result.width = Math.max(minimum.width, result.width);
            result.height = Math.max(minimum.height, result.height);
            return canPlaceWindow(window, result) ? result : new Rectangle(window.getBounds());
        }
        result.width = Math.max(minimum.width, Math.min(result.width, Math.max(minimum.width, area.width)));
        result.height = Math.max(minimum.height, Math.min(result.height, Math.max(minimum.height, area.height)));
        int titleVisible = Math.min(90, result.width);
        result.x = Math.max(area.x - result.width + titleVisible, Math.min(result.x, area.x + area.width - titleVisible));
        result.y = Math.max(area.y, Math.min(result.y, area.y + Math.max(0, area.height - 32)));
        return canPlaceWindow(window, result) ? result : new Rectangle(window.getBounds());
    }

    public Rectangle getAvailableDesktopBounds() {
        return new Rectangle(0, 0, Math.max(0, layeredPane.getWidth()), Math.max(0, layeredPane.getHeight()));
    }

    protected Rectangle resolveMaximizedBounds(WindowPanel window) {
        Rectangle area = getAvailableDesktopBounds();
        Insets insets = window == null ? null : window.getMaximizedInsets();
        if (insets == null) insets = maximizedInsets;
        int x = area.x + Math.max(0, insets.left);
        int y = area.y + Math.max(0, insets.top);
        int width = Math.max(0, area.width - Math.max(0, insets.left) - Math.max(0, insets.right));
        int height = Math.max(0, area.height - Math.max(0, insets.top) - Math.max(0, insets.bottom));
        return new Rectangle(x, y, width, height);
    }

    public WindowPanel findWindow(String key) { return windowsByKey.get(key); }
    public boolean containsWindow(String key) { return windowsByKey.containsKey(key); }
    public List<WindowPanel> getWindows() { return List.copyOf(windowsByKey.values()); }
    public WindowPanel getActiveWindow() { return activeWindow; }
    public JLayeredPane getLayeredPane() { return layeredPane; }
    public WindowModalOverlay getModalOverlay() { return modalOverlay; }
    public WindowSnapPreviewOverlay getSnapPreviewOverlay() { return snapPreviewOverlay; }
    public WindowSnapAssistOverlay getSnapAssistOverlay() { return snapAssistOverlay; }
    public WindowSnapDragSelector getSnapDragSelector() { return snapDragSelector; }
    public WindowMinimizedBar getMinimizedBar() { return minimizedBar; }
    public WindowPlacementPolicy getPlacementPolicy() { return placementPolicy; }
    public WindowSnapPolicy getSnapPolicy() { return snapPolicy; }
    public WindowAnimator getWindowAnimator() { return windowAnimator; }
    public WindowDesktopPanel placementPolicy(WindowPlacementPolicy value) { placementPolicy = Objects.requireNonNull(value); return this; }
    public WindowDesktopPanel snapPolicy(WindowSnapPolicy value) { snapPolicy = Objects.requireNonNull(value); return this; }
    public WindowDesktopPanel snapLayoutsEnabled(boolean enabled) {
        if (snapLayoutsEnabled == enabled) return this;
        snapLayoutsEnabled = enabled;
        if (!enabled) {
            clearSnapLayoutPreview();
            closeSnapAssist();
            cancelSnapLayoutDrag();
        }
        for (WindowPanel window : new ArrayList<>(windowsByKey.values())) {
            installSnapLayoutSupport(window);
        }
        dispatchDesktopEvent(EventWindowPanel.SNAP_LAYOUTS_CHANGE, null,
                Map.of("enabled", enabled));
        onSnapLayoutsEnabledChanged(enabled);
        return this;
    }
    public WindowDesktopPanel snapLayoutHoverDelay(int milliseconds) {
        int normalized = Math.max(0, milliseconds);
        if (snapLayoutHoverDelayMillis == normalized) return this;
        snapLayoutHoverDelayMillis = normalized;
        for (WindowPanel window : new ArrayList<>(windowsByKey.values())) {
            installSnapLayoutSupport(window);
        }
        return this;
    }
    public boolean isSnapLayoutsEnabled() { return snapLayoutsEnabled; }

    public WindowDesktopPanel snapLayoutTrigger(WindowSnapLayoutTrigger trigger) {
        WindowSnapLayoutTrigger normalized = Objects.requireNonNull(trigger);
        if (snapLayoutTrigger == normalized) return this;
        WindowSnapLayoutTrigger old = snapLayoutTrigger;
        snapLayoutTrigger = normalized;
        cancelSnapLayoutDrag();
        for (WindowPanel window : new ArrayList<>(windowsByKey.values())) {
            installSnapLayoutSupport(window);
        }
        dispatchDesktopEvent(EventWindowPanel.SNAP_LAYOUT_TRIGGER_CHANGE, null,
                Map.of("oldTrigger", old, "trigger", normalized));
        onSnapLayoutTriggerChanged(old, normalized);
        return this;
    }
    public WindowDesktopPanel snapLayoutTopActivationDistance(int pixels) {
        snapLayoutTopActivationDistance = Math.max(4, pixels);
        return this;
    }
    public WindowDesktopPanel snapLayoutTopActivationWidth(int pixels) {
        snapLayoutTopActivationWidth = Math.max(80, pixels);
        return this;
    }
    public WindowSnapLayoutTrigger getSnapLayoutTrigger() { return snapLayoutTrigger; }
    public int getSnapLayoutTopActivationDistance() { return snapLayoutTopActivationDistance; }
    public int getSnapLayoutTopActivationWidth() { return snapLayoutTopActivationWidth; }
    public WindowDesktopPanel snapAssistEnabled(boolean enabled) {
        if (snapAssistEnabled == enabled) return this;
        snapAssistEnabled = enabled;
        if (!enabled) closeSnapAssist();
        dispatchDesktopEvent(EventWindowPanel.SNAP_ASSIST_CHANGE, null,
                Map.of("enabled", enabled));
        onSnapAssistEnabledChanged(enabled);
        return this;
    }
    public boolean isSnapAssistEnabled() { return snapAssistEnabled; }
    public int getSnapLayoutHoverDelayMillis() { return snapLayoutHoverDelayMillis; }

    public WindowSnapLayoutPopup getSnapLayoutPopup(WindowPanel window) {
        SnapLayoutBinding binding = snapLayoutBindings.get(window);
        return binding == null ? null : binding.popup;
    }

    public boolean showSnapLayouts(WindowPanel window) {
        SnapLayoutBinding binding = snapLayoutBindings.get(window);
        if (binding == null || !canShowSnapLayouts(window)
                || !window.getSnapLayoutTrigger().supportsMaximizeButton()
                || !binding.button.isShowing()) return false;
        if (!binding.popup.isVisible()) {
            Dimension popupSize = binding.popup.getPreferredSize();
            int x = Math.min(0, binding.button.getWidth() - popupSize.width);
            binding.popup.show(binding.button, x, binding.button.getHeight() + 2);
        }
        return true;
    }

    public WindowDesktopPanel hideSnapLayouts(WindowPanel window) {
        SnapLayoutBinding binding = snapLayoutBindings.get(window);
        if (binding != null) {
            binding.timer.stop();
            binding.popup.setVisible(false);
        }
        clearSnapLayoutPreview();
        return this;
    }

    public boolean updateSnapLayoutDrag(WindowPanel window, Point desktopPoint) {
        if (!canShowSnapLayouts(window)
                || !window.getSnapLayoutTrigger().supportsTopCenter()
                || desktopPoint == null) {
            if (snapDragSelector.getWindow() == window) cancelSnapLayoutDrag();
            return false;
        }
        Rectangle area = getAvailableDesktopBounds();
        int center = area.x + area.width / 2;
        boolean activationPoint = desktopPoint.y <= area.y + snapLayoutTopActivationDistance
                && Math.abs(desktopPoint.x - center) <= snapLayoutTopActivationWidth / 2;
        if (!snapDragSelector.isVisible()) {
            if (!activationPoint) return false;
            snapDragSelector.open(window, area);
            layeredPane.moveToFront(snapDragSelector);
            dispatchDesktopEvent(EventWindowPanel.SNAP_LAYOUT_DRAG_OPEN, window,
                    Map.of("location", new Point(desktopPoint)));
            onSnapLayoutDragChanged(window, true);
        } else if (snapDragSelector.getWindow() != window) {
            cancelSnapLayoutDrag();
            if (!activationPoint) return false;
            snapDragSelector.open(window, area);
            layeredPane.moveToFront(snapDragSelector);
        }
        if (desktopPoint.y > snapDragSelector.getY() + snapDragSelector.getHeight() + 46
                || desktopPoint.x < snapDragSelector.getX() - 46
                || desktopPoint.x > snapDragSelector.getX() + snapDragSelector.getWidth() + 46) {
            cancelSnapLayoutDrag();
            return false;
        }
        WindowSnapDragSelector.Selection selection = snapDragSelector.updateSelection(desktopPoint);
        if (selection != null) {
            previewSnapLayout(window, selection.getLayout(), selection.getSnap());
        }
        return true;
    }

    public boolean completeSnapLayoutDrag(WindowPanel window) {
        if (!snapDragSelector.isVisible() || snapDragSelector.getWindow() != window) return false;
        WindowSnapDragSelector.Selection selection = snapDragSelector.getSelection();
        cancelSnapLayoutDrag();
        return selection != null && applySnapLayout(window, selection.getLayout(), selection.getSnap());
    }

    public WindowDesktopPanel cancelSnapLayoutDrag() {
        if (!snapDragSelector.isVisible()) return this;
        WindowPanel window = snapDragSelector.getWindow();
        snapDragSelector.close();
        clearSnapLayoutPreview();
        dispatchDesktopEvent(EventWindowPanel.SNAP_LAYOUT_DRAG_CLOSE, window, Map.of());
        onSnapLayoutDragChanged(window, false);
        return this;
    }

    public boolean previewSnapLayout(WindowPanel window, List<WindowSnap> layout, WindowSnap selected) {
        if (!canShowSnapLayouts(window) || layout == null || selected == null
                || !layout.contains(selected)) return false;
        resizeSnapOverlays();
        List<Rectangle> zones = layout.stream()
                .filter(snap -> snap != null && snap != WindowSnap.NONE)
                .map(snap -> snapPolicy.resolveBounds(this, window, snap))
                .toList();
        Rectangle selectedBounds = snapPolicy.resolveBounds(this, window, selected);
        snapPreviewOverlay.preview(zones, selectedBounds);
        layeredPane.moveToFront(snapPreviewOverlay);
        dispatchDesktopEvent(EventWindowPanel.SNAP_LAYOUT_PREVIEW_CHANGE, window,
                Map.of("visible", true, "layout", List.copyOf(layout),
                        "snap", selected, "bounds", new Rectangle(selectedBounds)));
        onSnapLayoutPreviewChanged(window, selected, selectedBounds, true);
        return true;
    }

    public WindowDesktopPanel clearSnapLayoutPreview() {
        if (!snapPreviewOverlay.isVisible()) return this;
        Rectangle oldBounds = snapPreviewOverlay.getSelectedBounds();
        snapPreviewOverlay.clearPreview();
        dispatchDesktopEvent(EventWindowPanel.SNAP_LAYOUT_PREVIEW_CHANGE, null,
                Map.of("visible", false, "bounds",
                        oldBounds == null ? new Rectangle() : oldBounds));
        onSnapLayoutPreviewChanged(null, WindowSnap.NONE, oldBounds, false);
        return this;
    }

    public boolean applySnapLayout(WindowPanel window, WindowSnap snap) {
        if (!canShowSnapLayouts(window) || snap == null || snap == WindowSnap.NONE) return false;
        WindowEvent before = dispatchDesktopEvent(EventWindowPanel.BEFORE_SNAP_LAYOUT_SELECT,
                window, Map.of("snap", snap));
        if (before.isCanceled()) return false;
        hideSnapLayouts(window);
        window.applySnap(snap);
        if (window.getSnap() != snap) return false;
        window.activate();
        dispatchDesktopEvent(EventWindowPanel.SNAP_LAYOUT_SELECT,
                window, Map.of("snap", snap));
        onSnapLayoutSelected(window, snap);
        return true;
    }

    public boolean applySnapLayout(WindowPanel window, List<WindowSnap> layout, WindowSnap snap) {
        if (layout == null || !layout.contains(snap)) return false;
        boolean applied = applySnapLayout(window, snap);
        if (applied && snapAssistEnabled) openSnapAssist(window, layout, snap);
        return applied;
    }

    public boolean openSnapAssist(WindowPanel source, List<WindowSnap> layout, WindowSnap occupiedZone) {
        if (!snapAssistEnabled || source == null || layout == null || layout.size() < 2) return false;
        List<WindowSnap> remaining = layout.stream()
                .filter(zone -> zone != null && zone != WindowSnap.NONE && zone != occupiedZone)
                .distinct().toList();
        List<WindowPanel> candidates = resolveSnapAssistCandidates(source);
        if (remaining.isEmpty() || candidates.isEmpty()) return false;
        closeSnapAssist();
        resizeSnapOverlays();
        if (!snapAssistOverlay.open(source, remaining, candidates)) return false;
        layeredPane.moveToFront(snapAssistOverlay);
        dispatchDesktopEvent(EventWindowPanel.SNAP_ASSIST_OPEN, source,
                Map.of("layout", List.copyOf(layout), "occupiedZone", occupiedZone,
                        "remainingZones", List.copyOf(remaining), "candidates", List.copyOf(candidates)));
        onSnapAssistChanged(source, true);
        return true;
    }

    protected List<WindowPanel> resolveSnapAssistCandidates(WindowPanel source) {
        return windowsByKey.values().stream()
                .filter(window -> window != source && !window.isModal() && window.isSnapEnabled())
                .filter(WindowPanel::isVisible)
                .filter(window -> window.getWindowState() != WindowState.MINIMIZED)
                .toList();
    }

    public boolean applySnapAssistSelection(WindowPanel window, WindowSnap snap) {
        if (!snapAssistOverlay.isAssistVisible() || window == null || snap == null
                || !snapAssistOverlay.getCandidates().contains(window)
                || !snapAssistOverlay.getRemainingZones().contains(snap)) return false;
        WindowEvent before = dispatchDesktopEvent(EventWindowPanel.BEFORE_SNAP_ASSIST_SELECT,
                window, Map.of("snap", snap, "sourceWindow", snapAssistOverlay.getSourceWindow()));
        if (before.isCanceled()) return false;
        window.applySnap(snap);
        if (window.getSnap() != snap) return false;
        window.activate();
        dispatchDesktopEvent(EventWindowPanel.SNAP_ASSIST_SELECT, window,
                Map.of("snap", snap));
        onSnapAssistWindowSelected(window, snap);
        snapAssistOverlay.completeSelection(window, snap);
        return true;
    }

    public WindowDesktopPanel closeSnapAssist() {
        snapAssistOverlay.close();
        return this;
    }

    void snapAssistClosed() {
        dispatchDesktopEvent(EventWindowPanel.SNAP_ASSIST_CLOSE, null, Map.of());
        onSnapAssistChanged(null, false);
    }
    public WindowDesktopPanel windowAnimator(WindowAnimator value) { windowAnimator = Objects.requireNonNull(value); return this; }
    public WindowDesktopPanel animationsEnabled(boolean enabled) {
        if (windowAnimator instanceof DefaultWindowAnimator animator) animator.enabled(enabled);
        return this;
    }
    public WindowDesktopPanel animationDuration(int milliseconds) {
        if (windowAnimator instanceof DefaultWindowAnimator animator) animator.durationMillis(milliseconds);
        return this;
    }
    public WindowDesktopPanel maximizedInsets(Insets value) {
        maximizedInsets = value == null ? new Insets(0, 0, 0, 0)
                : new Insets(Math.max(0, value.top), Math.max(0, value.left),
                Math.max(0, value.bottom), Math.max(0, value.right));
        for (WindowPanel window : windowsByKey.values()) {
            if (window.getWindowState() == WindowState.MAXIMIZED && window.getMaximizedInsets() == null) {
                window.setBounds(resolveMaximizedBounds(window));
            }
        }
        if (!windowsByKey.isEmpty()) layoutChanged(null);
        return this;
    }
    public Insets getMaximizedInsets() {
        return new Insets(maximizedInsets.top, maximizedInsets.left,
                maximizedInsets.bottom, maximizedInsets.right);
    }
    public WindowDesktopPanel minimizedBarAutoHideEnabled(boolean enabled) {
        minimizedBar.autoHideEnabled(enabled);
        return this;
    }
    public WindowDesktopPanel taskbarAutoHideEnabled(boolean enabled) {
        return minimizedBarAutoHideEnabled(enabled);
    }
    public WindowDesktopPanel expandMinimizedBar() { minimizedBar.expand(); return this; }
    public WindowDesktopPanel collapseMinimizedBar() { minimizedBar.collapse(); return this; }
    public boolean isMinimizedBarAutoHideEnabled() { return minimizedBar.isAutoHideEnabled(); }
    public WindowDesktopPanel minimizedBarContextMenuEnabled(boolean enabled) {
        if (minimizedBarContextMenuEnabled == enabled) return this;
        minimizedBarContextMenuEnabled = enabled;
        minimizedButtons.forEach(this::configureMinimizedWindowButton);
        dispatchDesktopEvent(EventWindowPanel.MINIMIZED_BAR_MENU_CHANGE, null,
                Map.of("enabled", enabled));
        onMinimizedBarContextMenuChanged(enabled);
        return this;
    }
    public WindowDesktopPanel taskbarContextMenuEnabled(boolean enabled) {
        return minimizedBarContextMenuEnabled(enabled);
    }
    public WindowDesktopPanel minimizedBarMenuFactory(WindowMinimizedMenuFactory factory) {
        minimizedMenuFactory = factory;
        minimizedButtons.forEach(this::configureMinimizedWindowButton);
        return this;
    }
    public boolean isMinimizedBarContextMenuEnabled() { return minimizedBarContextMenuEnabled; }
    public WindowMinimizedMenuFactory getMinimizedMenuFactory() { return minimizedMenuFactory; }

    public boolean cancelWindowAnimation(WindowPanel window) {
        if (window == null || !windowsByKey.containsValue(window)) return false;
        if (!windowAnimator.cancel(window)) return false;
        window.setAnimationAlpha(1f);
        window.animationCanceled();
        return true;
    }

    protected WindowEvent createDesktopEvent(String type, WindowPanel window, Map<String, Object> properties) {
        return new WindowEvent(this, window, type, properties);
    }

    protected WindowEvent dispatchDesktopEvent(String type, WindowPanel window, Map<String, Object> properties) {
        WindowEvent event = createDesktopEvent(type, window, properties);
        dispatchEventObject(type, event);
        return event;
    }

    void receiveWindowEvent(String type, WindowEvent event) {
        dispatchEventObject(type, event);
        WindowPanel window = event.getWindow();
        if (window != null && minimizedButtons.containsKey(window)
                && (EventWindowPanel.WINDOW_TITLE_CHANGE.equals(type)
                || EventWindowPanel.WINDOW_ICON_CHANGE.equals(type)
                || EventWindowPanel.WINDOW_CAPABILITY_CHANGE.equals(type))) {
            updateMinimizedWindowButton(window);
        }
        if (window != null && windowsByKey.containsValue(window)
                && EventWindowPanel.WINDOW_CAPABILITY_CHANGE.equals(type)) {
            installSnapLayoutSupport(window);
        }
    }

    protected void updateMinimizedWindowButton(WindowPanel window) {
        AbstractButton button = minimizedButtons.get(window);
        if (button == null) return;
        button.setText(window.getTitle());
        button.setIcon(window.getIcon());
        configureMinimizedWindowButton(window, button);
    }

    protected void dispatchEventObject(String type, WindowEvent event) {
        List<Consumer<EventComponent>> eventListeners = listeners.get(type);
        if (eventListeners == null) return;
        for (Consumer<EventComponent> listener : eventListeners) {
            try { listener.accept(event); } catch (Exception exception) { exception.printStackTrace(); }
        }
    }

    void layoutChanged(WindowPanel source) {
        dispatchDesktopEvent(EventWindowPanel.LAYOUT_CHANGE, source,
                Map.of("snapshot", captureLayout()));
        onLayoutChanged(source);
    }

    public WindowDesktopPanel onWindowEvent(String type, Consumer<WindowEvent> listener) {
        addEventListener(type, WindowEvent.class, listener); return this;
    }
    public WindowDesktopPanel onBeforeWindowAdd(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_ADD, listener); }
    public WindowDesktopPanel onWindowAdd(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_ADD, listener); }
    public WindowDesktopPanel onBeforeWindowRemove(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_REMOVE, listener); }
    public WindowDesktopPanel onWindowRemove(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_REMOVE, listener); }
    public WindowDesktopPanel onBeforeWindowClose(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_CLOSE, listener); }
    public WindowDesktopPanel onWindowClose(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_CLOSE, listener); }
    public WindowDesktopPanel onActiveWindowChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.ACTIVE_WINDOW_CHANGE, listener); }
    public WindowDesktopPanel onWindowOrderChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_ORDER_CHANGE, listener); }
    public WindowDesktopPanel onMinimizedBarChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.MINIMIZED_BAR_CHANGE, listener); }
    public WindowDesktopPanel onMinimizedBarExpand(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.MINIMIZED_BAR_EXPAND, listener); }
    public WindowDesktopPanel onMinimizedBarCollapse(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.MINIMIZED_BAR_COLLAPSE, listener); }
    public WindowDesktopPanel onMinimizedBarAutoHideChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.MINIMIZED_BAR_AUTO_HIDE_CHANGE, listener); }
    public WindowDesktopPanel onMinimizedBarContextMenuChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.MINIMIZED_BAR_MENU_CHANGE, listener); }
    public WindowDesktopPanel onMinimizedBarMenuOpen(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.MINIMIZED_BAR_MENU_OPEN, listener); }
    public WindowDesktopPanel onMinimizedBarMenuClose(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.MINIMIZED_BAR_MENU_CLOSE, listener); }
    public WindowDesktopPanel onBeforeMinimizedBarMenuAction(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_MINIMIZED_BAR_MENU_ACTION, listener); }
    public WindowDesktopPanel onMinimizedBarMenuAction(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.MINIMIZED_BAR_MENU_ACTION, listener); }
    public WindowDesktopPanel onSnapLayoutsChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_LAYOUTS_CHANGE, listener); }
    public WindowDesktopPanel onSnapLayoutTriggerChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_LAYOUT_TRIGGER_CHANGE, listener); }
    public WindowDesktopPanel onSnapLayoutDragOpen(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_LAYOUT_DRAG_OPEN, listener); }
    public WindowDesktopPanel onSnapLayoutDragClose(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_LAYOUT_DRAG_CLOSE, listener); }
    public WindowDesktopPanel onSnapLayoutMenuOpen(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_LAYOUT_MENU_OPEN, listener); }
    public WindowDesktopPanel onSnapLayoutMenuClose(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_LAYOUT_MENU_CLOSE, listener); }
    public WindowDesktopPanel onBeforeSnapLayoutSelect(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_SNAP_LAYOUT_SELECT, listener); }
    public WindowDesktopPanel onSnapLayoutSelect(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_LAYOUT_SELECT, listener); }
    public WindowDesktopPanel onSnapLayoutPreviewChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_LAYOUT_PREVIEW_CHANGE, listener); }
    public WindowDesktopPanel onSnapAssistChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_ASSIST_CHANGE, listener); }
    public WindowDesktopPanel onSnapAssistOpen(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_ASSIST_OPEN, listener); }
    public WindowDesktopPanel onSnapAssistClose(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_ASSIST_CLOSE, listener); }
    public WindowDesktopPanel onBeforeSnapAssistSelect(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_SNAP_ASSIST_SELECT, listener); }
    public WindowDesktopPanel onSnapAssistSelect(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.SNAP_ASSIST_SELECT, listener); }
    public WindowDesktopPanel onBeforeLayoutRestore(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_LAYOUT_RESTORE, listener); }
    public WindowDesktopPanel onLayoutRestore(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.LAYOUT_RESTORE, listener); }
    public WindowDesktopPanel onLayoutChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.LAYOUT_CHANGE, listener); }
    public WindowDesktopPanel onAnimationStart(Consumer<WindowAnimationEvent> listener) {
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_START, WindowAnimationEvent.class, listener); return this;
    }
    public WindowDesktopPanel onAnimationProgress(Consumer<WindowAnimationEvent> listener) {
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_PROGRESS, WindowAnimationEvent.class, listener); return this;
    }
    public WindowDesktopPanel onAnimationEnd(Consumer<WindowAnimationEvent> listener) {
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_END, WindowAnimationEvent.class, listener); return this;
    }
    public WindowDesktopPanel onAnimationCancel(Consumer<WindowAnimationEvent> listener) {
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_CANCEL, WindowAnimationEvent.class, listener); return this;
    }

    protected void onWindowAdded(WindowPanel window) {}
    protected void onWindowRemoved(WindowPanel window) {}
    protected void onActiveWindowChanged(WindowPanel oldWindow, WindowPanel newWindow) {}
    protected void onModalChanged(WindowPanel window, boolean opened) {}
    protected void onLayoutChanged(WindowPanel source) {}
    protected void onLayoutRestored(WindowLayoutSnapshot snapshot) {}
    protected void onMinimizedBarExpansionChanged(boolean expanded) {}
    protected void onMinimizedBarAutoHideChanged(boolean enabled) {}
    protected void onMinimizedBarContextMenuChanged(boolean enabled) {}
    protected void onMinimizedBarMenuChanged(WindowPanel window, JPopupMenu menu, boolean opened) {}
    protected void onMinimizedBarMenuAction(WindowPanel window, WindowMinimizedMenuAction action) {}
    protected void onSnapLayoutsEnabledChanged(boolean enabled) {}
    protected void onSnapLayoutTriggerChanged(WindowSnapLayoutTrigger oldTrigger,
                                              WindowSnapLayoutTrigger newTrigger) {}
    protected void onSnapLayoutDragChanged(WindowPanel window, boolean opened) {}
    protected void onSnapLayoutMenuChanged(WindowPanel window, WindowSnapLayoutPopup popup, boolean opened) {}
    protected void onSnapLayoutSelected(WindowPanel window, WindowSnap snap) {}
    protected void onSnapLayoutPreviewChanged(WindowPanel window, WindowSnap snap,
                                              Rectangle bounds, boolean visible) {}
    protected void onSnapAssistEnabledChanged(boolean enabled) {}
    protected void onSnapAssistChanged(WindowPanel source, boolean opened) {}
    protected void onSnapAssistWindowSelected(WindowPanel window, WindowSnap snap) {}

    protected static class SnapLayoutBinding {
        protected final WindowControlButton button;
        protected final WindowSnapLayoutPopup popup;
        protected final javax.swing.Timer timer;
        protected final MouseAdapter listener;

        protected SnapLayoutBinding(WindowControlButton button, WindowSnapLayoutPopup popup,
                                    javax.swing.Timer timer, MouseAdapter listener) {
            this.button = button;
            this.popup = popup;
            this.timer = timer;
            this.listener = listener;
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try { paintDesktopBackground(g); } finally { g.dispose(); }
    }
}
