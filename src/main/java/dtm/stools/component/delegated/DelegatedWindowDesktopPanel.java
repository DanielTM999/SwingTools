package dtm.stools.component.delegated;

import dtm.stools.component.panels.window.*;
import dtm.stools.controllers.component.AbstractWindowDesktopController;
import dtm.stools.exceptions.DelegatedWindowException;

public abstract class DelegatedWindowDesktopPanel<T extends AbstractWindowDesktopController>
        extends WindowDesktopPanel implements DelegatedIWindowComponent {
    protected T controller;

    protected DelegatedWindowDesktopPanel() {
        this(false);
    }

    protected DelegatedWindowDesktopPanel(boolean minimizedBarAutoHideEnabled) {
        this(minimizedBarAutoHideEnabled, null);
    }

    protected DelegatedWindowDesktopPanel(boolean minimizedBarAutoHideEnabled,
                                          WindowMinimizedBarFactory minimizedBarFactory) {
        super(minimizedBarAutoHideEnabled, minimizedBarFactory);
        installControllerEvents();
    }

    protected abstract T newController();

    protected T getOrCreateController() {
        if (controller == null) {
            controller = newController();
            if (controller == null) {
                throw new DelegatedWindowException("Falha ao obter o controller",
                        new NullPointerException("controller null"));
            }
        }
        return controller;
    }

    public T getController() { return getOrCreateController(); }

    protected void installControllerEvents() {
        onWindowEvent(dtm.stools.component.panels.window.EventWindowPanel.BEFORE_WINDOW_ADD,
                event -> getOrCreateController().onBeforeWindowAdd(this, event));
        onWindowEvent(dtm.stools.component.panels.window.EventWindowPanel.BEFORE_WINDOW_REMOVE,
                event -> getOrCreateController().onBeforeWindowRemove(this, event));
        onWindowEvent(dtm.stools.component.panels.window.EventWindowPanel.BEFORE_LAYOUT_RESTORE,
                event -> getOrCreateController().onBeforeLayoutRestore(this, event));
        onWindowEvent(dtm.stools.component.panels.window.EventWindowPanel.WINDOW_ORDER_CHANGE,
                event -> getOrCreateController().onWindowOrderChanged(this, event));
        onWindowEvent(dtm.stools.component.panels.window.EventWindowPanel.MINIMIZED_BAR_CHANGE,
                event -> getOrCreateController().onMinimizedBarChanged(this, event));
        onWindowEvent(EventWindowPanel.MINIMIZED_BAR_EXPAND,
                event -> getOrCreateController().onMinimizedBarExpanded(this, event));
        onWindowEvent(EventWindowPanel.MINIMIZED_BAR_COLLAPSE,
                event -> getOrCreateController().onMinimizedBarCollapsed(this, event));
        onWindowEvent(EventWindowPanel.MINIMIZED_BAR_AUTO_HIDE_CHANGE,
                event -> getOrCreateController().onMinimizedBarAutoHideChanged(this, event));
        onWindowEvent(EventWindowPanel.MINIMIZED_BAR_MENU_CHANGE,
                event -> getOrCreateController().onMinimizedBarContextMenuChanged(this, event));
        onWindowEvent(EventWindowPanel.MINIMIZED_BAR_MENU_OPEN,
                event -> getOrCreateController().onMinimizedBarMenuOpened(this, event));
        onWindowEvent(EventWindowPanel.MINIMIZED_BAR_MENU_CLOSE,
                event -> getOrCreateController().onMinimizedBarMenuClosed(this, event));
        onWindowEvent(EventWindowPanel.BEFORE_MINIMIZED_BAR_MENU_ACTION,
                event -> getOrCreateController().onBeforeMinimizedBarMenuAction(this, event));
        onWindowEvent(EventWindowPanel.MINIMIZED_BAR_MENU_ACTION,
                event -> getOrCreateController().onMinimizedBarMenuAction(this, event));
        onWindowEvent(EventWindowPanel.SNAP_LAYOUTS_CHANGE,
                event -> getOrCreateController().onSnapLayoutsChanged(this, event));
        onWindowEvent(EventWindowPanel.SNAP_LAYOUT_TRIGGER_CHANGE,
                event -> getOrCreateController().onSnapLayoutTriggerChanged(this, event));
        onWindowEvent(EventWindowPanel.SNAP_LAYOUT_DRAG_OPEN,
                event -> getOrCreateController().onSnapLayoutDragOpened(this, event));
        onWindowEvent(EventWindowPanel.SNAP_LAYOUT_DRAG_CLOSE,
                event -> getOrCreateController().onSnapLayoutDragClosed(this, event));
        onWindowEvent(EventWindowPanel.SNAP_LAYOUT_MENU_OPEN,
                event -> getOrCreateController().onSnapLayoutMenuOpened(this, event));
        onWindowEvent(EventWindowPanel.SNAP_LAYOUT_MENU_CLOSE,
                event -> getOrCreateController().onSnapLayoutMenuClosed(this, event));
        onWindowEvent(EventWindowPanel.BEFORE_SNAP_LAYOUT_SELECT,
                event -> getOrCreateController().onBeforeSnapLayoutSelect(this, event));
        onWindowEvent(EventWindowPanel.SNAP_LAYOUT_SELECT,
                event -> getOrCreateController().onSnapLayoutSelected(this, event));
        onWindowEvent(EventWindowPanel.SNAP_LAYOUT_PREVIEW_CHANGE,
                event -> getOrCreateController().onSnapLayoutPreviewChanged(this, event));
        onWindowEvent(EventWindowPanel.SNAP_ASSIST_CHANGE,
                event -> getOrCreateController().onSnapAssistChanged(this, event));
        onWindowEvent(EventWindowPanel.SNAP_ASSIST_OPEN,
                event -> getOrCreateController().onSnapAssistOpened(this, event));
        onWindowEvent(EventWindowPanel.SNAP_ASSIST_CLOSE,
                event -> getOrCreateController().onSnapAssistClosed(this, event));
        onWindowEvent(EventWindowPanel.BEFORE_SNAP_ASSIST_SELECT,
                event -> getOrCreateController().onBeforeSnapAssistSelect(this, event));
        onWindowEvent(EventWindowPanel.SNAP_ASSIST_SELECT,
                event -> getOrCreateController().onSnapAssistSelected(this, event));
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_START, WindowAnimationEvent.class,
                event -> getOrCreateController().onAnimationStart(this, event));
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_PROGRESS, WindowAnimationEvent.class,
                event -> getOrCreateController().onAnimationProgress(this, event));
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_END, WindowAnimationEvent.class,
                event -> getOrCreateController().onAnimationEnd(this, event));
        addEventListener(EventWindowPanel.WINDOW_ANIMATION_CANCEL, WindowAnimationEvent.class,
                event -> getOrCreateController().onAnimationCancel(this, event));
    }

    @Override protected void onInit() {
        super.onInit();
        getOrCreateController().onInit(this);
    }

    @Override protected void onLoad() {
        super.onLoad();
        getOrCreateController().onLoad(this);
    }

    @Override protected void onRemoved() {
        super.onRemoved();
        if (controller != null) controller.onRemoved(this);
    }

    @Override protected void onWindowAdded(WindowPanel window) {
        super.onWindowAdded(window);
        getOrCreateController().onWindowAdded(this, window);
    }

    @Override protected void onWindowRemoved(WindowPanel window) {
        super.onWindowRemoved(window);
        getOrCreateController().onWindowRemoved(this, window);
    }

    @Override protected void onActiveWindowChanged(WindowPanel oldWindow, WindowPanel newWindow) {
        super.onActiveWindowChanged(oldWindow, newWindow);
        getOrCreateController().onActiveWindowChanged(this, oldWindow, newWindow);
    }

    @Override protected void onModalChanged(WindowPanel window, boolean opened) {
        super.onModalChanged(window, opened);
        getOrCreateController().onModalChanged(this, window, opened);
    }

    @Override protected void onLayoutChanged(WindowPanel source) {
        super.onLayoutChanged(source);
        getOrCreateController().onLayoutChanged(this, source);
    }

    @Override protected void onLayoutRestored(WindowLayoutSnapshot snapshot) {
        super.onLayoutRestored(snapshot);
        getOrCreateController().onLayoutRestored(this, snapshot);
    }

    @Override public void disposeController() {
        if (controller != null) controller.onDispose(this);
        controller = null;
    }
}
