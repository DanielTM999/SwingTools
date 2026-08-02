package dtm.stools.component.delegated;

import dtm.stools.component.panels.window.*;
import dtm.stools.controllers.component.AbstractWindowPanelController;
import dtm.stools.exceptions.DelegatedWindowException;

import java.awt.*;

public abstract class DelegatedWindowPanel<T extends AbstractWindowPanelController>
        extends WindowPanel implements DelegatedIWindowComponent {
    protected T controller;

    public DelegatedWindowPanel(String key, String title, Component content) {
        super(key, title, content);
        installControllerEvents();
    }

    public DelegatedWindowPanel(WindowConfig config) {
        super(config);
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
        onWindowEvent(EventWindowPanel.BEFORE_WINDOW_OPEN,
                event -> getOrCreateController().onBeforeOpen(this, event));
        onWindowEvent(EventWindowPanel.BEFORE_WINDOW_CLOSE,
                event -> getOrCreateController().onBeforeClose(this, event));
        onWindowEvent(EventWindowPanel.BEFORE_WINDOW_ACTIVATE,
                event -> getOrCreateController().onBeforeActivate(this, event));
        onWindowEvent(EventWindowPanel.BEFORE_WINDOW_MOVE,
                event -> getOrCreateController().onBeforeMove(this, event));
        onWindowEvent(EventWindowPanel.BEFORE_WINDOW_RESIZE,
                event -> getOrCreateController().onBeforeResize(this, event));
        onWindowEvent(EventWindowPanel.BEFORE_WINDOW_SNAP,
                event -> getOrCreateController().onBeforeSnap(this, event));
        onWindowEvent(EventWindowPanel.WINDOW_TITLE_CHANGE,
                event -> getOrCreateController().onTitleChanged(this, event));
        onWindowEvent(EventWindowPanel.WINDOW_BOUNDS_CHANGE,
                event -> getOrCreateController().onBoundsChanged(this, event));
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

    @Override protected void onOpened() {
        super.onOpened();
        getOrCreateController().onWindowOpen(this);
    }

    @Override protected void onClosed() {
        super.onClosed();
        getOrCreateController().onWindowClose(this);
        if (getCloseOperation() == WindowCloseOperation.REMOVE) disposeController();
    }

    @Override protected void onActivated() {
        super.onActivated();
        getOrCreateController().onActivated(this);
    }

    @Override protected void onDeactivated() {
        super.onDeactivated();
        if (controller != null) controller.onDeactivated(this);
    }

    @Override protected void onStateChanged(WindowState oldState, WindowState newState) {
        super.onStateChanged(oldState, newState);
        getOrCreateController().onStateChanged(this, oldState, newState);
    }

    @Override protected void onSnapped(WindowSnap oldSnap, WindowSnap newSnap) {
        super.onSnapped(oldSnap, newSnap);
        getOrCreateController().onSnapped(this, oldSnap, newSnap);
    }

    @Override protected void onWindowMoved(Rectangle oldBounds, Rectangle newBounds) {
        super.onWindowMoved(oldBounds, newBounds);
        getOrCreateController().onMoved(this, oldBounds, newBounds);
    }

    @Override protected void onWindowResized(Rectangle oldBounds, Rectangle newBounds) {
        super.onWindowResized(oldBounds, newBounds);
        getOrCreateController().onResized(this, oldBounds, newBounds);
    }

    @Override protected void onDisposed() {
        super.onDisposed();
        disposeController();
    }

    @Override public void disposeController() {
        if (controller != null) controller.onDispose(this);
        controller = null;
    }
}
