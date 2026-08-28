package dtm.stools.component.panels.window;

@FunctionalInterface
public interface WindowMinimizedBarFactory {
    WindowMinimizedBar createBar(WindowDesktopPanel desktop);
}
