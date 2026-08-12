package dtm.stools.component.panels.dock;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockPanelSplitRegionTest {

    @Test
    void displaysMultipleBottomDocksInResizableSplitGroups() throws Exception {
        onEdt(() -> {
            DockPanel dock = new DockPanel();
            JLabel build = new JLabel("Build");
            JLabel terminal = new JLabel("Terminal");

            dock.addDock(new DockConfig("build", "Build", build)
                    .bottom()
                    .tabHeaderVisible(false));
            dock.addDock("terminal", "Terminal", terminal, DockRegion.BOTTOM);

            JRootPane root = new JRootPane();
            root.setContentPane(dock);
            root.setSize(900, 600);
            root.doLayout();
            dock.doLayout();

            assertEquals(DockRegionLayout.SPLIT, dock.getDockRegionLayout());
            assertEquals(2, dock.getDocks(DockRegion.BOTTOM).size());
            assertTrue(SwingUtilities.getAncestorOfClass(JSplitPane.class, build) != null);
            assertTrue(SwingUtilities.getAncestorOfClass(JSplitPane.class, terminal) != null);
            assertSame(build, dock.findDock("build"));
            assertSame(terminal, dock.findDock("terminal"));
            return null;
        });
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) return action.call();

        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            try {
                result.set(action.call());
            } catch (Throwable throwable) {
                failure.set(throwable);
            }
        });

        Throwable throwable = failure.get();
        if (throwable instanceof Exception exception) throw exception;
        if (throwable instanceof Error error) throw error;
        if (throwable != null) throw new InvocationTargetException(throwable);
        return result.get();
    }
}
