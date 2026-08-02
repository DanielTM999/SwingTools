package dtm.stools.controllers.component;

import dtm.stools.component.panels.window.WindowDesktopPanel;
import dtm.stools.component.panels.window.WindowAnimationEvent;
import dtm.stools.component.panels.window.WindowEvent;
import dtm.stools.component.panels.window.WindowLayoutSnapshot;
import dtm.stools.component.panels.window.WindowPanel;

public abstract class AbstractWindowDesktopController extends BindingAbstractViewController<WindowDesktopPanel> {
    public void onBeforeWindowAdd(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onBeforeWindowRemove(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onBeforeLayoutRestore(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onWindowAdded(WindowDesktopPanel desktop, WindowPanel window) {}
    public void onWindowRemoved(WindowDesktopPanel desktop, WindowPanel window) {}
    public void onActiveWindowChanged(WindowDesktopPanel desktop, WindowPanel oldWindow, WindowPanel newWindow) {}
    public void onModalChanged(WindowDesktopPanel desktop, WindowPanel window, boolean opened) {}
    public void onLayoutChanged(WindowDesktopPanel desktop, WindowPanel source) {}
    public void onLayoutRestored(WindowDesktopPanel desktop, WindowLayoutSnapshot snapshot) {}
    public void onWindowOrderChanged(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onMinimizedBarChanged(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onMinimizedBarExpanded(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onMinimizedBarCollapsed(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onMinimizedBarAutoHideChanged(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onMinimizedBarContextMenuChanged(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onMinimizedBarMenuOpened(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onMinimizedBarMenuClosed(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onBeforeMinimizedBarMenuAction(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onMinimizedBarMenuAction(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapLayoutsChanged(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapLayoutTriggerChanged(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapLayoutDragOpened(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapLayoutDragClosed(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapLayoutMenuOpened(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapLayoutMenuClosed(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onBeforeSnapLayoutSelect(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapLayoutSelected(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapLayoutPreviewChanged(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapAssistChanged(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapAssistOpened(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapAssistClosed(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onBeforeSnapAssistSelect(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onSnapAssistSelected(WindowDesktopPanel desktop, WindowEvent event) {}
    public void onAnimationStart(WindowDesktopPanel desktop, WindowAnimationEvent event) {}
    public void onAnimationProgress(WindowDesktopPanel desktop, WindowAnimationEvent event) {}
    public void onAnimationEnd(WindowDesktopPanel desktop, WindowAnimationEvent event) {}
    public void onAnimationCancel(WindowDesktopPanel desktop, WindowAnimationEvent event) {}
    public void onDispose(WindowDesktopPanel desktop) {}
}
