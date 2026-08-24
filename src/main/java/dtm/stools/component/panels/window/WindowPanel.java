package dtm.stools.component.panels.window;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.panels.base.PanelEventListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

public class WindowPanel extends PanelEventListener {
    private static final int NORTH = 1;
    private static final int SOUTH = 2;
    private static final int WEST = 4;
    private static final int EAST = 8;

    protected final JPanel contentHost;
    protected final WindowTitleBar titleBar;
    protected final Map<WindowControl, WindowControlButton> controlButtons = new EnumMap<>(WindowControl.class);
    protected final Set<Component> titleBarDragSources = Collections.newSetFromMap(new IdentityHashMap<>());

    private final String windowKey;
    private String title;
    private Icon icon;
    private Component content;
    private WindowStyle windowStyle;
    private WindowState windowState = WindowState.NORMAL;
    private WindowSnap snap = WindowSnap.NONE;
    private WindowCloseOperation closeOperation;
    private Insets maximizedInsets;
    private Rectangle normalBounds;
    private boolean active;
    private boolean movable;
    private boolean resizable;
    private boolean closable;
    private boolean minimizable;
    private boolean maximizable;
    private boolean modal;
    private boolean closeOnEscape;
    private boolean snapEnabled;
    private Boolean snapLayoutsEnabled;
    private WindowSnapLayoutTrigger snapLayoutTrigger;
    private float animationAlpha = 1f;
    private WindowAnimationType activeAnimation;
    private Rectangle animationStartBounds;

    public WindowPanel(String key, String title, Component content) {
        this(new WindowConfig(key, title, content));
    }

    public WindowPanel(WindowConfig config) {
        super(new BorderLayout(), false);
        Objects.requireNonNull(config, "config");
        windowKey = config.getKey();
        title = config.getTitle();
        icon = config.getIcon();
        windowStyle = config.getStyle() == null ? createDefaultStyle() : config.getStyle().copy();
        closeOperation = config.getCloseOperation();
        maximizedInsets = config.getMaximizedInsets();
        movable = config.isMovable();
        resizable = config.isResizable();
        closable = config.isClosable();
        minimizable = config.isMinimizable();
        maximizable = config.isMaximizable();
        modal = config.isModal();
        closeOnEscape = config.isCloseOnEscape();
        snapEnabled = config.isSnapEnabled();
        snapLayoutsEnabled = config.getSnapLayoutsEnabled();
        snapLayoutTrigger = config.getSnapLayoutTrigger();
        setMinimumSize(config.getMinimumSize());
        setOpaque(false);
        setVisible(false);
        setFocusable(true);

        contentHost = createContentHost();
        titleBar = createTitleBar();
        configureTitleBar(titleBar);
        installControls();
        add(titleBar, BorderLayout.NORTH);
        add(contentHost, BorderLayout.CENTER);
        content(config.getContent());
        installInteractions();
        installKeyboardActions();
        updateStyle();
    }

    protected WindowStyle createDefaultStyle() { return new WindowStyle(); }

    protected JPanel createContentHost() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        return panel;
    }

    protected WindowTitleBar createTitleBar() { return new WindowTitleBar(this); }
    protected WindowControlButton createControlButton(WindowControl control) {
        return new WindowControlButton(control, windowStyle);
    }

    protected void configureTitleBar(WindowTitleBar bar) {}

    protected void configureControlButton(WindowControl control, WindowControlButton button) {
        button.setPreferredSize(new Dimension(windowStyle.getControlSize(), windowStyle.getControlSize()));
    }

    protected MouseAdapter createDragHandler() { return new DragHandler(); }
    protected MouseAdapter createResizeHandler() { return new ResizeHandler(); }

    protected void installInteractions() {
        MouseAdapter dragHandler = createDragHandler();
        titleBar.addMouseListener(dragHandler);
        titleBar.addMouseMotionListener(dragHandler);
        installTitleBarDragSources(titleBar, dragHandler);
        MouseAdapter resizeHandler = createResizeHandler();
        addMouseListener(resizeHandler);
        addMouseMotionListener(resizeHandler);
        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent event) { activate(); }
        });
        updateInteractionCursors();
    }

    protected void installTitleBarDragSources(Container container, MouseAdapter dragHandler) {
        for (Component child : container.getComponents()) {
            if (child instanceof AbstractButton) {
                child.setCursor(resolveDefaultCursor());
                continue;
            }
            if (titleBarDragSources.add(child)) {
                child.addMouseListener(dragHandler);
                child.addMouseMotionListener(dragHandler);
            }
            child.setCursor(resolveTitleBarCursor());
            if (child instanceof Container nested) installTitleBarDragSources(nested, dragHandler);
        }
    }

    void installTitleBarDragSource(Component component) {
        if (component == null || component instanceof AbstractButton) return;
        if (!titleBarDragSources.add(component)) return;
        MouseAdapter handler = createDragHandler();
        component.addMouseListener(handler);
        component.addMouseMotionListener(handler);
        component.setCursor(resolveTitleBarCursor());
        if (component instanceof Container container) installTitleBarDragSources(container, handler);
    }

    protected Cursor resolveDefaultCursor() { return Cursor.getDefaultCursor(); }
    protected Cursor resolveTitleBarCursor() {
        return movable && windowState == WindowState.NORMAL
                ? windowStyle.getTitleBarCursor()
                : resolveDefaultCursor();
    }

    protected Cursor resolveResizeCursor(int cursorType) {
        return Cursor.getPredefinedCursor(cursorType);
    }

    protected void updateInteractionCursors() {
        setCursor(resolveDefaultCursor());
        titleBar.setCursor(resolveTitleBarCursor());
        updateTitleBarChildCursors(titleBar);
    }

    protected void updateTitleBarChildCursors(Container container) {
        for (Component child : container.getComponents()) {
            child.setCursor(child instanceof AbstractButton ? resolveDefaultCursor() : resolveTitleBarCursor());
            if (child instanceof Container nested && !(child instanceof AbstractButton)) {
                updateTitleBarChildCursors(nested);
            }
        }
    }

    protected void installKeyboardActions() {
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.CTRL_DOWN_MASK), "closeWindow");
        getActionMap().put("closeWindow", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { close(); }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escapeWindow");
        getActionMap().put("escapeWindow", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (modal && closeOnEscape) close();
            }
        });
    }

    private void installControls() {
        installControl(WindowControl.MINIMIZE, this::minimize);
        installControl(WindowControl.MAXIMIZE_RESTORE, () -> {
            if (windowState == WindowState.MAXIMIZED) restore(); else maximize();
        });
        installControl(WindowControl.CLOSE, this::close);
        updateControlVisibility();
    }

    private void installControl(WindowControl control, Runnable action) {
        WindowControlButton button = createControlButton(control);
        configureControlButton(control, button);
        button.addActionListener(event -> action.run());
        controlButtons.put(control, button);
        titleBar.addControl(button);
    }

    protected void updateControlVisibility() {
        controlButtons.get(WindowControl.MINIMIZE).setVisible(minimizable);
        controlButtons.get(WindowControl.MAXIMIZE_RESTORE).setVisible(maximizable);
        controlButtons.get(WindowControl.CLOSE).setVisible(closable);
    }

    public WindowPanel open() {
        if (isVisible() && !isAnimating()) return activate();
        WindowEvent before = dispatchWindowEvent(EventWindowPanel.BEFORE_WINDOW_OPEN, Map.of());
        if (before.isCanceled()) return this;
        setVisible(true);
        if (windowState == WindowState.MINIMIZED) setWindowStateDirect(WindowState.NORMAL);
        activate();
        WindowDesktopPanel desktop = getDesktopPanel();
        Runnable completion = () -> {
            dispatchWindowEvent(EventWindowPanel.WINDOW_OPEN, Map.of());
            onOpened();
        };
        if (desktop != null) {
            desktop.windowOpened(this);
            desktop.animateWindowOpen(this, completion);
        } else {
            completion.run();
        }
        return this;
    }

    public WindowPanel close() {
        if (!closable || closeOperation == WindowCloseOperation.DO_NOTHING || isAnimating()) return this;
        WindowEvent before = dispatchWindowEvent(EventWindowPanel.BEFORE_WINDOW_CLOSE, Map.of());
        if (before.isCanceled()) return this;
        WindowDesktopPanel desktop = getDesktopPanel();
        Rectangle closeBounds = new Rectangle(getBounds());
        Runnable completion = () -> {
            setBounds(closeBounds);
            if (closeOperation == WindowCloseOperation.REMOVE && desktop != null) {
                if (!desktop.removeWindowAfterClose(this)) {
                    setAnimationAlpha(1f);
                    return;
                }
            } else {
                setVisible(false);
                setActiveDirect(false);
                if (desktop != null) desktop.windowClosed(this);
            }
            dispatchWindowEvent(EventWindowPanel.WINDOW_CLOSE, Map.of("operation", closeOperation));
            onClosed();
            setAnimationAlpha(1f);
        };
        if (desktop != null) desktop.animateWindowClose(this, completion); else completion.run();
        return this;
    }

    public WindowPanel activate() {
        WindowDesktopPanel desktop = getDesktopPanel();
        if (desktop != null) desktop.activateWindow(this);
        else setActiveDirect(true);
        return this;
    }

    public WindowPanel minimize() {
        if (!minimizable) return this;
        return changeState(WindowState.MINIMIZED);
    }

    public WindowPanel maximize() {
        if (!maximizable) return this;
        return changeState(WindowState.MAXIMIZED);
    }

    public WindowPanel restore() { return changeState(WindowState.NORMAL); }

    protected WindowPanel changeState(WindowState target) {
        Objects.requireNonNull(target, "target");
        if (isAnimating()) return this;
        if (target == windowState) {
            if (target == WindowState.NORMAL && snap != WindowSnap.NONE) {
                WindowSnap oldSnap = snap;
                snap = WindowSnap.NONE;
                setBounds(constrainBounds(getNormalBounds()));
                dispatchWindowEvent(EventWindowPanel.WINDOW_SNAP,
                        Map.of("oldSnap", oldSnap, "snap", WindowSnap.NONE));
                onSnapped(oldSnap, WindowSnap.NONE);
                WindowDesktopPanel desktop = getDesktopPanel();
                if (desktop != null) desktop.layoutChanged(this);
            }
            return this;
        }
        WindowState old = windowState;
        WindowEvent before = dispatchWindowEvent(EventWindowPanel.BEFORE_WINDOW_STATE_CHANGE,
                Map.of("oldState", old, "newState", target));
        if (before.isCanceled()) return this;
        WindowDesktopPanel desktop = getDesktopPanel();
        if (old == WindowState.NORMAL && isVisible()) normalBounds = new Rectangle(getBounds());
        setWindowStateDirect(target);
        if (desktop != null) desktop.applyWindowState(this, old, target);
        dispatchWindowEvent(EventWindowPanel.WINDOW_STATE_CHANGE,
                Map.of("oldState", old, "newState", target));
        onStateChanged(old, target);
        return this;
    }

    public WindowPanel applySnap(WindowSnap target) {
        if (!snapEnabled || target == null || isAnimating()) return this;
        if (target == WindowSnap.NONE) return restore();
        WindowDesktopPanel desktop = getDesktopPanel();
        if (desktop == null) return this;
        WindowEvent before = dispatchWindowEvent(EventWindowPanel.BEFORE_WINDOW_SNAP,
                Map.of("oldSnap", snap, "snap", target));
        if (before.isCanceled()) return this;
        if (windowState == WindowState.NORMAL && snap == WindowSnap.NONE) normalBounds = new Rectangle(getBounds());
        WindowSnap old = snap;
        snap = target;
        setWindowStateDirect(WindowState.NORMAL);
        setVisible(true);
        Rectangle targetBounds = desktop.getSnapPolicy().resolveBounds(desktop, this, target);
        desktop.animateWindow(this, WindowAnimationType.SNAP, targetBounds, 1f, 1f, () -> {
            dispatchWindowEvent(EventWindowPanel.WINDOW_SNAP, Map.of("oldSnap", old, "snap", target));
            onSnapped(old, target);
            desktop.layoutChanged(this);
        });
        return this;
    }

    public WindowPanel title(String value) {
        String old = title;
        title = value == null ? "" : value;
        updateStyle();
        dispatchWindowEvent(EventWindowPanel.WINDOW_TITLE_CHANGE, Map.of("oldTitle", old, "title", title));
        return this;
    }
    public WindowPanel icon(Icon value) {
        Icon old = icon;
        icon = value;
        updateStyle();
        dispatchNullableChange(EventWindowPanel.WINDOW_ICON_CHANGE, "oldIcon", old, "icon", value);
        return this;
    }
    public WindowPanel content(Component value) {
        Component old = content;
        content = Objects.requireNonNull(value, "content");
        contentHost.removeAll();
        contentHost.add(content, BorderLayout.CENTER);
        contentHost.revalidate(); contentHost.repaint(); reloadDomElements();
        dispatchNullableChange(EventWindowPanel.WINDOW_CONTENT_CHANGE, "oldContent", old, "content", value);
        return this;
    }
    public WindowPanel style(Consumer<WindowStyle> configurer) {
        if (configurer != null) configurer.accept(windowStyle);
        updateStyle(); return this;
    }
    public WindowPanel windowStyle(WindowStyle style) {
        windowStyle = Objects.requireNonNull(style, "style"); updateStyle(); return this;
    }
    public WindowPanel closeOperation(WindowCloseOperation value) {
        WindowCloseOperation old = closeOperation; closeOperation = Objects.requireNonNull(value);
        dispatchCapabilityChange("closeOperation", old, value); return this;
    }
    public WindowPanel movable(boolean value) {
        boolean old = movable; movable = value; updateInteractionCursors();
        dispatchCapabilityChange("movable", old, value); return this;
    }
    public WindowPanel resizable(boolean value) { boolean old = resizable; resizable = value; dispatchCapabilityChange("resizable", old, value); return this; }
    public WindowPanel closable(boolean value) { boolean old = closable; closable = value; updateControlVisibility(); dispatchCapabilityChange("closable", old, value); return this; }
    public WindowPanel minimizable(boolean value) { boolean old = minimizable; minimizable = value; updateControlVisibility(); dispatchCapabilityChange("minimizable", old, value); return this; }
    public WindowPanel maximizable(boolean value) { boolean old = maximizable; maximizable = value; updateControlVisibility(); dispatchCapabilityChange("maximizable", old, value); return this; }
    public WindowPanel modal(boolean value) {
        boolean old = modal; modal = value;
        dispatchWindowEvent(EventWindowPanel.WINDOW_MODAL_CHANGE, Map.of("oldModal", old, "modal", value));
        WindowDesktopPanel desktop = getDesktopPanel();
        if (desktop != null) desktop.windowModalityChanged(this, old, value);
        return this;
    }
    public WindowPanel closeOnEscape(boolean value) { boolean old = closeOnEscape; closeOnEscape = value; dispatchCapabilityChange("closeOnEscape", old, value); return this; }
    public WindowPanel snapEnabled(boolean value) { boolean old = snapEnabled; snapEnabled = value; dispatchCapabilityChange("snapEnabled", old, value); return this; }
    public WindowPanel snapLayoutsEnabled(boolean value) {
        Boolean old = snapLayoutsEnabled;
        snapLayoutsEnabled = value;
        dispatchCapabilityChange("snapLayoutsEnabled", old, value);
        return this;
    }
    public WindowPanel inheritSnapLayoutsEnabled() {
        Boolean old = snapLayoutsEnabled;
        snapLayoutsEnabled = null;
        dispatchCapabilityChange("snapLayoutsEnabled", old, null);
        return this;
    }
    public WindowPanel snapLayoutTrigger(WindowSnapLayoutTrigger value) {
        WindowSnapLayoutTrigger old = snapLayoutTrigger;
        snapLayoutTrigger = Objects.requireNonNull(value);
        dispatchCapabilityChange("snapLayoutTrigger", old, value);
        return this;
    }
    public WindowPanel inheritSnapLayoutTrigger() {
        WindowSnapLayoutTrigger old = snapLayoutTrigger;
        snapLayoutTrigger = null;
        dispatchCapabilityChange("snapLayoutTrigger", old, null);
        return this;
    }
    public WindowPanel maximizedInsets(Insets value) {
        Insets old = copyInsets(maximizedInsets);
        maximizedInsets = copyInsets(value);
        Map<String, Object> properties = new HashMap<>();
        properties.put("capability", "maximizedInsets");
        properties.put("oldValue", old);
        properties.put("value", copyInsets(maximizedInsets));
        dispatchWindowEvent(EventWindowPanel.WINDOW_CAPABILITY_CHANGE, properties);
        WindowDesktopPanel desktop = getDesktopPanel();
        if (desktop != null && windowState == WindowState.MAXIMIZED) {
            setBounds(desktop.resolveMaximizedBounds(this));
            desktop.layoutChanged(this);
        }
        return this;
    }

    protected void updateStyle() {
        int shadow = getEffectiveShadowSize();
        setBorder(BorderFactory.createEmptyBorder(shadow, shadow, shadow, shadow));
        contentHost.setBorder(BorderFactory.createEmptyBorder(
                windowStyle.getContentInsets().top, windowStyle.getContentInsets().left,
                windowStyle.getContentInsets().bottom, windowStyle.getContentInsets().right));
        contentHost.setBackground(windowStyle.getBackground());
        controlButtons.forEach((control, button) -> {
            button.setWindowStyle(windowStyle);
            if (control == WindowControl.MAXIMIZE_RESTORE) {
                button.setRestoreGlyph(windowState == WindowState.MAXIMIZED);
            }
            configureControlButton(control, button);
        });
        titleBar.updateFromWindow();
        updateInteractionCursors();
        revalidate(); repaint();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        Rectangle oldBounds = new Rectangle(getBounds());
        boolean changed = x != getX() || y != getY() || width != getWidth() || height != getHeight();
        super.setBounds(x, y, width, height);
        if (changed && contentHost != null) {
            refreshWindowLayout();
            dispatchWindowEvent(EventWindowPanel.WINDOW_BOUNDS_CHANGE,
                    Map.of("oldBounds", oldBounds, "newBounds", new Rectangle(getBounds())));
        }
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (contentHost != null) {
            contentHost.doLayout();
            layoutDescendants(contentHost);
        }
    }

    protected void refreshWindowLayout() {
        revalidate();
        doLayout();
        repaint();
    }

    protected void layoutDescendants(Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                nested.doLayout();
                layoutDescendants(nested);
            }
        }
    }

    @Override
    public void paint(Graphics graphics) {
        if (animationAlpha >= .999f) {
            super.paint(graphics);
            return;
        }
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setComposite(AlphaComposite.SrcOver.derive(Math.max(0f, animationAlpha)));
            super.paint(g);
        } finally {
            g.dispose();
        }
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            paintWindowShadow(g);
            paintWindowBackground(g);
            paintWindowBorder(g);
        } finally { g.dispose(); }
    }

    protected Rectangle getPaintBounds() {
        int shadow = getEffectiveShadowSize();
        return new Rectangle(shadow, shadow, Math.max(0, getWidth() - shadow * 2), Math.max(0, getHeight() - shadow * 2));
    }

    protected void paintWindowShadow(Graphics2D g) {
        Rectangle r = getPaintBounds();
        int size = getEffectiveShadowSize();
        if (size <= 0 || windowStyle.getShadowColor() == null) return;
        for (int i = size; i > 0; i--) {
            float ratio = (size - i + 1f) / (size + 1f);
            Color color = windowStyle.getShadowColor();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(color.getAlpha() * ratio * .28f)));
            g.fillRoundRect(r.x - i, r.y - i, r.width + i * 2, r.height + i * 2,
                    getEffectiveWindowArc() + i, getEffectiveWindowArc() + i);
        }
    }

    protected void paintWindowBackground(Graphics2D g) {
        Rectangle r = getPaintBounds();
        g.setColor(windowStyle.getBackground());
        g.fillRoundRect(r.x, r.y, r.width, r.height, getEffectiveWindowArc(), getEffectiveWindowArc());
    }

    protected void paintWindowBorder(Graphics2D g) {
        Rectangle r = getPaintBounds();
        g.setColor(active ? windowStyle.getActiveBorderColor() : windowStyle.getBorderColor());
        g.setStroke(new BasicStroke(windowStyle.getBorderWidth()));
        g.drawRoundRect(r.x, r.y, Math.max(0, r.width - 1), Math.max(0, r.height - 1),
                getEffectiveWindowArc(), getEffectiveWindowArc());
    }

    protected WindowEvent createWindowEvent(String type, Map<String, Object> properties) {
        Map<String, Object> props = new HashMap<>(properties == null ? Map.of() : properties);
        props.putIfAbsent("key", windowKey);
        props.putIfAbsent("state", windowState);
        return new WindowEvent(this, this, type, props);
    }

    protected WindowEvent dispatchWindowEvent(String type, Map<String, Object> properties) {
        WindowEvent event = createWindowEvent(type, properties);
        dispatchEventObject(type, event);
        WindowDesktopPanel desktop = getDesktopPanel();
        if (desktop != null) desktop.receiveWindowEvent(type, event);
        return event;
    }

    protected void dispatchNullableChange(String type, String oldName, Object oldValue,
                                          String newName, Object newValue) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(oldName, oldValue);
        properties.put(newName, newValue);
        dispatchWindowEvent(type, properties);
    }

    protected void dispatchCapabilityChange(String name, Object oldValue, Object newValue) {
        if (Objects.equals(oldValue, newValue)) return;
        Map<String, Object> properties = new HashMap<>();
        properties.put("capability", name);
        properties.put("oldValue", oldValue);
        properties.put("value", newValue);
        dispatchWindowEvent(EventWindowPanel.WINDOW_CAPABILITY_CHANGE, properties);
    }

    protected void dispatchEventObject(String type, WindowEvent event) {
        List<Consumer<EventComponent>> eventListeners = listeners.get(type);
        if (eventListeners == null) return;
        for (Consumer<EventComponent> listener : eventListeners) {
            try { listener.accept(event); } catch (Exception exception) { exception.printStackTrace(); }
        }
    }

    public WindowPanel onWindowEvent(String type, Consumer<WindowEvent> listener) {
        addEventListener(type, WindowEvent.class, listener); return this;
    }
    public WindowPanel onBeforeOpen(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_OPEN, listener); }
    public WindowPanel onOpen(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_OPEN, listener); }
    public WindowPanel onBeforeClose(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_CLOSE, listener); }
    public WindowPanel onClose(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_CLOSE, listener); }
    public WindowPanel onBeforeActivate(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_ACTIVATE, listener); }
    public WindowPanel onActivate(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_ACTIVATE, listener); }
    public WindowPanel onDeactivate(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_DEACTIVATE, listener); }
    public WindowPanel onBeforeMove(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_MOVE, listener); }
    public WindowPanel onMoveStart(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_MOVE_START, listener); }
    public WindowPanel onBeforeStateChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_STATE_CHANGE, listener); }
    public WindowPanel onStateChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_STATE_CHANGE, listener); }
    public WindowPanel onMove(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_MOVE, listener); }
    public WindowPanel onMoveEnd(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_MOVE_END, listener); }
    public WindowPanel onBeforeResize(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_RESIZE, listener); }
    public WindowPanel onResizeStart(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_RESIZE_START, listener); }
    public WindowPanel onResize(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_RESIZE, listener); }
    public WindowPanel onResizeEnd(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_RESIZE_END, listener); }
    public WindowPanel onBeforeSnap(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.BEFORE_WINDOW_SNAP, listener); }
    public WindowPanel onSnap(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_SNAP, listener); }
    public WindowPanel onBoundsChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_BOUNDS_CHANGE, listener); }
    public WindowPanel onTitleChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_TITLE_CHANGE, listener); }
    public WindowPanel onIconChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_ICON_CHANGE, listener); }
    public WindowPanel onContentChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_CONTENT_CHANGE, listener); }
    public WindowPanel onCapabilityChange(Consumer<WindowEvent> listener) { return onWindowEvent(EventWindowPanel.WINDOW_CAPABILITY_CHANGE, listener); }
    public WindowPanel onAnimationStart(Consumer<WindowAnimationEvent> listener) {
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_START, WindowAnimationEvent.class, listener); return this;
    }
    public WindowPanel onAnimationEnd(Consumer<WindowAnimationEvent> listener) {
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_END, WindowAnimationEvent.class, listener); return this;
    }
    public WindowPanel onAnimationProgress(Consumer<WindowAnimationEvent> listener) {
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_PROGRESS, WindowAnimationEvent.class, listener); return this;
    }
    public WindowPanel onAnimationCancel(Consumer<WindowAnimationEvent> listener) {
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_CANCEL, WindowAnimationEvent.class, listener); return this;
    }

    public WindowPanel cancelAnimation() {
        WindowDesktopPanel desktop = getDesktopPanel();
        if (desktop != null) desktop.cancelWindowAnimation(this);
        return this;
    }

    protected void onOpened() {}
    protected void onClosed() {}
    protected void onActivated() {}
    protected void onDeactivated() {}
    protected void onStateChanged(WindowState oldState, WindowState newState) {}
    protected void onSnapped(WindowSnap oldSnap, WindowSnap newSnap) {}
    protected void onWindowMoved(Rectangle oldBounds, Rectangle newBounds) {}
    protected void onWindowResized(Rectangle oldBounds, Rectangle newBounds) {}
    protected void onDisposed() {}

    void setActiveDirect(boolean value) {
        if (active == value) return;
        active = value;
        updateStyle();
        dispatchWindowEvent(value ? EventWindowPanel.WINDOW_ACTIVATE : EventWindowPanel.WINDOW_DEACTIVATE, Map.of());
        if (value) onActivated(); else onDeactivated();
    }

    void animationStarted(WindowAnimationRequest request) {
        activeAnimation = request.getType();
        animationStartBounds = request.getFromBounds();
        dispatchAnimationEvent(EventWindowPanel.WINDOW_ANIMATION_START, request.getType(), 0f,
                Map.of("fromBounds", request.getFromBounds(), "toBounds", request.getToBounds(),
                        "duration", request.getDurationMillis()));
    }

    void animationFrame(WindowAnimationType type, WindowAnimationFrame frame) {
        setAnimationAlpha(frame.alpha());
        setBounds(frame.bounds());
        dispatchAnimationEvent(EventWindowPanel.WINDOW_ANIMATION_PROGRESS, type, frame.progress(),
                Map.of("bounds", frame.bounds(), "alpha", frame.alpha()));
    }

    void animationEnded(WindowAnimationType type) {
        activeAnimation = null;
        animationStartBounds = null;
        dispatchAnimationEvent(EventWindowPanel.WINDOW_ANIMATION_END, type, 1f, Map.of());
    }

    void animationCanceled() {
        WindowAnimationType canceled = activeAnimation;
        if (canceled == null) return;
        activeAnimation = null;
        if (animationStartBounds != null) setBounds(animationStartBounds);
        animationStartBounds = null;
        dispatchAnimationEvent(EventWindowPanel.WINDOW_ANIMATION_CANCEL, canceled, 0f, Map.of());
    }

    protected WindowAnimationEvent dispatchAnimationEvent(String eventType, WindowAnimationType type,
                                                            float progress, Map<String, Object> properties) {
        WindowAnimationEvent event = new WindowAnimationEvent(this, this, eventType, type, progress, properties);
        dispatchEventObject(eventType, event);
        WindowDesktopPanel desktop = getDesktopPanel();
        if (desktop != null) desktop.receiveWindowEvent(eventType, event);
        return event;
    }

    void setAnimationAlpha(float value) {
        animationAlpha = Math.max(0f, Math.min(1f, value));
        repaint();
    }

    void setWindowStateDirect(WindowState state) {
        windowState = state;
        WindowControlButton button = controlButtons.get(WindowControl.MAXIMIZE_RESTORE);
        if (button != null) button.setRestoreGlyph(state == WindowState.MAXIMIZED);
        updateStyle();
    }
    void setSnapDirect(WindowSnap value) { snap = value == null ? WindowSnap.NONE : value; }
    void setNormalBoundsDirect(Rectangle value) { normalBounds = value == null ? null : new Rectangle(value); }

    protected Rectangle constrainBounds(Rectangle requested) {
        WindowDesktopPanel desktop = getDesktopPanel();
        return desktop == null ? requested : desktop.constrainWindowBounds(this, requested);
    }

    protected void applyInteractiveBounds(Rectangle bounds, boolean resize, String eventType) {
        Rectangle old = new Rectangle(getBounds());
        Rectangle constrained = constrainBounds(bounds);
        setBounds(constrained);
        if (windowState == WindowState.NORMAL && snap == WindowSnap.NONE) normalBounds = new Rectangle(constrained);
        Map<String, Object> props = Map.of("oldBounds", old, "newBounds", new Rectangle(constrained));
        dispatchWindowEvent(eventType, props);
        if (resize) onWindowResized(old, constrained); else onWindowMoved(old, constrained);
    }

    public String getWindowKey() { return windowKey; }
    public String getTitle() { return title; }
    public Icon getIcon() { return icon; }
    public Component getContent() { return content; }
    public WindowStyle getWindowStyle() { return windowStyle; }
    public WindowState getWindowState() { return windowState; }
    public WindowSnap getSnap() { return snap; }
    public Rectangle getNormalBounds() { return normalBounds == null ? new Rectangle(getBounds()) : new Rectangle(normalBounds); }
    public boolean isActive() { return active; }
    public boolean isMovable() { return movable; }
    public boolean isResizable() { return resizable; }
    public boolean isClosable() { return closable; }
    public boolean isMinimizable() { return minimizable; }
    public boolean isMaximizable() { return maximizable; }
    public boolean isModal() { return modal; }
    public boolean isCloseOnEscape() { return closeOnEscape; }
    public boolean isSnapEnabled() { return snapEnabled; }
    public boolean isSnapLayoutsEnabled() {
        if (snapLayoutsEnabled != null) return snapLayoutsEnabled;
        WindowDesktopPanel desktop = getDesktopPanel();
        return desktop == null || desktop.isSnapLayoutsEnabled();
    }
    public Boolean getSnapLayoutsEnabledOverride() { return snapLayoutsEnabled; }
    public WindowSnapLayoutTrigger getSnapLayoutTrigger() {
        if (snapLayoutTrigger != null) return snapLayoutTrigger;
        WindowDesktopPanel desktop = getDesktopPanel();
        return desktop == null ? WindowSnapLayoutTrigger.TOP_CENTER : desktop.getSnapLayoutTrigger();
    }
    public WindowSnapLayoutTrigger getSnapLayoutTriggerOverride() { return snapLayoutTrigger; }
    public WindowCloseOperation getCloseOperation() { return closeOperation; }
    public Insets getMaximizedInsets() { return copyInsets(maximizedInsets); }
    public int getEffectiveShadowSize() {
        return windowState == WindowState.MAXIMIZED ? 0 : windowStyle.getShadowSize();
    }
    public int getEffectiveWindowArc() {
        return windowState == WindowState.MAXIMIZED ? 0 : windowStyle.getArc();
    }
    public float getAnimationAlpha() { return animationAlpha; }
    public WindowAnimationType getActiveAnimation() { return activeAnimation; }
    public boolean isAnimating() { return activeAnimation != null; }
    public WindowTitleBar getTitleBar() { return titleBar; }
    public JPanel getContentHost() { return contentHost; }
    public WindowControlButton getControlButton(WindowControl control) { return controlButtons.get(control); }

    public WindowDesktopPanel getDesktopPanel() {
        Container parent = getParent();
        while (parent != null) {
            if (parent instanceof WindowDesktopPanel desktop) return desktop;
            parent = parent.getParent();
        }
        return null;
    }

    private static Insets copyInsets(Insets value) {
        return value == null ? null : new Insets(value.top, value.left, value.bottom, value.right);
    }

    protected class DragHandler extends MouseAdapter {
        private Point pressOnScreen;
        private Rectangle startBounds;

        @Override public void mousePressed(MouseEvent event) {
            if (!movable || windowState != WindowState.NORMAL || !SwingUtilities.isLeftMouseButton(event)) return;
            WindowEvent before = dispatchWindowEvent(EventWindowPanel.BEFORE_WINDOW_MOVE,
                    Map.of("oldBounds", new Rectangle(getBounds())));
            if (before.isCanceled()) return;
            activate();
            WindowDesktopPanel desktop = getDesktopPanel();
            if (desktop != null) desktop.cancelSnapLayoutDrag();
            pressOnScreen = event.getLocationOnScreen();
            startBounds = getBounds();
            dispatchWindowEvent(EventWindowPanel.WINDOW_MOVE_START, Map.of("oldBounds", new Rectangle(startBounds)));
        }

        @Override public void mouseDragged(MouseEvent event) {
            if (pressOnScreen == null || windowState != WindowState.NORMAL) return;
            Point current = event.getLocationOnScreen();
            Rectangle target = new Rectangle(startBounds);
            target.translate(current.x - pressOnScreen.x, current.y - pressOnScreen.y);
            snap = WindowSnap.NONE;
            applyInteractiveBounds(target, false, EventWindowPanel.WINDOW_MOVE);
            WindowDesktopPanel desktop = getDesktopPanel();
            if (desktop != null && snapEnabled) {
                Point location = SwingUtilities.convertPoint(
                        event.getComponent(), event.getPoint(), desktop);
                desktop.updateSnapLayoutDrag(WindowPanel.this, location);
            }
        }

        @Override public void mouseReleased(MouseEvent event) {
            if (pressOnScreen == null) return;
            Rectangle old = startBounds;
            pressOnScreen = null;
            dispatchWindowEvent(EventWindowPanel.WINDOW_MOVE_END,
                    Map.of("oldBounds", old, "newBounds", new Rectangle(getBounds())));
            WindowDesktopPanel desktop = getDesktopPanel();
            if (desktop != null && snapEnabled) {
                boolean layoutApplied = desktop.completeSnapLayoutDrag(WindowPanel.this);
                if (!layoutApplied) {
                    Point location = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), desktop);
                    WindowSnap resolved = desktop.getSnapPolicy().resolveSnap(desktop, WindowPanel.this, location);
                    if (resolved != WindowSnap.NONE) applySnap(resolved);
                }
            }
            if (desktop != null) desktop.layoutChanged(WindowPanel.this);
        }

        @Override public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(event) && maximizable) {
                if (windowState == WindowState.MAXIMIZED) restore(); else maximize();
            }
        }

        @Override public void mouseEntered(MouseEvent event) {
            event.getComponent().setCursor(resolveTitleBarCursor());
        }

        @Override public void mouseExited(MouseEvent event) {
            if (pressOnScreen == null) event.getComponent().setCursor(resolveTitleBarCursor());
        }
    }

    protected class ResizeHandler extends MouseAdapter {
        private int edges;
        private Point pressOnScreen;
        private Rectangle startBounds;

        @Override public void mouseMoved(MouseEvent event) {
            int cursorType = cursorFor(edgesAt(event.getPoint()));
            setCursor(cursorType == Cursor.DEFAULT_CURSOR ? resolveDefaultCursor() : resolveResizeCursor(cursorType));
        }

        @Override public void mouseExited(MouseEvent event) {
            if (pressOnScreen == null) setCursor(resolveDefaultCursor());
        }

        @Override public void mousePressed(MouseEvent event) {
            if (!resizable || windowState != WindowState.NORMAL || !SwingUtilities.isLeftMouseButton(event)) return;
            edges = edgesAt(event.getPoint());
            if (edges == 0) return;
            WindowEvent before = dispatchWindowEvent(EventWindowPanel.BEFORE_WINDOW_RESIZE,
                    Map.of("oldBounds", new Rectangle(getBounds()), "edges", edges));
            if (before.isCanceled()) return;
            activate();
            pressOnScreen = event.getLocationOnScreen();
            startBounds = getBounds();
            dispatchWindowEvent(EventWindowPanel.WINDOW_RESIZE_START, Map.of("oldBounds", new Rectangle(startBounds)));
        }

        @Override public void mouseDragged(MouseEvent event) {
            if (pressOnScreen == null) return;
            Point current = event.getLocationOnScreen();
            int dx = current.x - pressOnScreen.x;
            int dy = current.y - pressOnScreen.y;
            Rectangle target = new Rectangle(startBounds);
            if ((edges & WEST) != 0) { target.x += dx; target.width -= dx; }
            if ((edges & EAST) != 0) target.width += dx;
            if ((edges & NORTH) != 0) { target.y += dy; target.height -= dy; }
            if ((edges & SOUTH) != 0) target.height += dy;
            snap = WindowSnap.NONE;
            applyInteractiveBounds(target, true, EventWindowPanel.WINDOW_RESIZE);
        }

        @Override public void mouseReleased(MouseEvent event) {
            if (pressOnScreen == null) return;
            Rectangle old = startBounds;
            pressOnScreen = null;
            dispatchWindowEvent(EventWindowPanel.WINDOW_RESIZE_END,
                    Map.of("oldBounds", old, "newBounds", new Rectangle(getBounds())));
            WindowDesktopPanel desktop = getDesktopPanel();
            if (desktop != null) desktop.layoutChanged(WindowPanel.this);
            setCursor(resolveDefaultCursor());
        }

        private int edgesAt(Point point) {
            if (!resizable || windowState != WindowState.NORMAL) return 0;
            int size = windowStyle.getResizeHandleSize() + windowStyle.getShadowSize();
            int result = 0;
            if (point.y <= size) result |= NORTH;
            if (point.y >= getHeight() - size) result |= SOUTH;
            if (point.x <= size) result |= WEST;
            if (point.x >= getWidth() - size) result |= EAST;
            return result;
        }

        private int cursorFor(int value) {
            if (value == (NORTH | WEST)) return Cursor.NW_RESIZE_CURSOR;
            if (value == (NORTH | EAST)) return Cursor.NE_RESIZE_CURSOR;
            if (value == (SOUTH | WEST)) return Cursor.SW_RESIZE_CURSOR;
            if (value == (SOUTH | EAST)) return Cursor.SE_RESIZE_CURSOR;
            if ((value & NORTH) != 0) return Cursor.N_RESIZE_CURSOR;
            if ((value & SOUTH) != 0) return Cursor.S_RESIZE_CURSOR;
            if ((value & WEST) != 0) return Cursor.W_RESIZE_CURSOR;
            if ((value & EAST) != 0) return Cursor.E_RESIZE_CURSOR;
            return Cursor.DEFAULT_CURSOR;
        }
    }
}
