package dtm.stools.controllers.component;

import dtm.stools.component.panels.window.*;

import java.awt.*;

public abstract class AbstractWindowPanelController extends BindingAbstractViewController<WindowPanel> {
    public void onBeforeOpen(WindowPanel window, WindowEvent event) {}
    public void onBeforeActivate(WindowPanel window, WindowEvent event) {}
    public void onBeforeMove(WindowPanel window, WindowEvent event) {}
    public void onBeforeResize(WindowPanel window, WindowEvent event) {}
    public void onBeforeSnap(WindowPanel window, WindowEvent event) {}
    public void onWindowOpen(WindowPanel window) {}
    public void onBeforeClose(WindowPanel window, WindowEvent event) {}
    public void onWindowClose(WindowPanel window) {}
    public void onActivated(WindowPanel window) {}
    public void onDeactivated(WindowPanel window) {}
    public void onStateChanged(WindowPanel window, WindowState oldState, WindowState newState) {}
    public void onMoved(WindowPanel window, Rectangle oldBounds, Rectangle newBounds) {}
    public void onResized(WindowPanel window, Rectangle oldBounds, Rectangle newBounds) {}
    public void onSnapped(WindowPanel window, WindowSnap oldSnap, WindowSnap newSnap) {}
    public void onTitleChanged(WindowPanel window, WindowEvent event) {}
    public void onBoundsChanged(WindowPanel window, WindowEvent event) {}
    public void onAnimationStart(WindowPanel window, WindowAnimationEvent event) {}
    public void onAnimationProgress(WindowPanel window, WindowAnimationEvent event) {}
    public void onAnimationEnd(WindowPanel window, WindowAnimationEvent event) {}
    public void onAnimationCancel(WindowPanel window, WindowAnimationEvent event) {}
    public void onDispose(WindowPanel window) {}
}
