package dtm.stools.component.panels.tab;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TabbedPanelSplitTest {

    @Test
    void addsNewTabToAttachedGroupAfterOriginalGroupIsCollapsed() throws Exception {
        onEdt(() -> {
            Fixture fixture = createFixture();
            JLabel first = new JLabel("first");
            JLabel second = new JLabel("second");
            fixture.tabs.addTab("first", "First", first);
            fixture.tabs.addTab("second", "Second", second);

            assertTrue(fixture.tabs.splitTab("second", TabSplitPlacement.RIGHT));
            assertTrue(fixture.tabs.closeTab("first"));
            assertFalse(SwingUtilities.isDescendingFrom(fixture.tabs, fixture.rootPane));

            JLabel reopened = new JLabel("reopened");
            fixture.tabs.addTab("reopened", "Reopened", reopened);

            assertTrue(SwingUtilities.isDescendingFrom(reopened, fixture.rootPane));
            assertTrue(fixture.tabs.contains("reopened"));
            assertSame(reopened, fixture.tabs.find("reopened"));
            assertTrue(fixture.tabs.closeTab("reopened"));
            return null;
        });
    }

    @Test
    void rootReceivesCloseEventsFromSplitGroups() throws Exception {
        onEdt(() -> {
            Fixture fixture = createFixture();
            fixture.tabs.addTab("first", "First", new JLabel("first"));
            fixture.tabs.addTab("second", "Second", new JLabel("second"));
            AtomicInteger closeEvents = new AtomicInteger();
            fixture.tabs.onTabClose(event -> closeEvents.incrementAndGet());

            assertTrue(fixture.tabs.splitTab("second", TabSplitPlacement.RIGHT));
            TabbedPanel splitGroup = findGroup(fixture.tabs, "second");
            assertTrue(splitGroup.closeTab("second"));

            assertEquals(1, closeEvents.get());
            assertFalse(fixture.tabs.contains("second"));
            return null;
        });
    }

    @Test
    void rootCanCancelCloseRequestedBySplitGroup() throws Exception {
        onEdt(() -> {
            Fixture fixture = createFixture();
            fixture.tabs.addTab("first", "First", new JLabel("first"));
            fixture.tabs.addTab("second", "Second", new JLabel("second"));
            fixture.tabs.onBeforeTabClose(event -> {
                if ("second".equals(event.getKey())) {
                    event.cancel();
                }
            });

            assertTrue(fixture.tabs.splitTab("second", TabSplitPlacement.RIGHT));
            TabbedPanel splitGroup = findGroup(fixture.tabs, "second");

            assertFalse(splitGroup.closeTab("second"));
            assertTrue(fixture.tabs.contains("second"));
            return null;
        });
    }

    @Test
    void addsNewTabWhenCollapsedRootLeavesNestedSplitBehind() throws Exception {
        onEdt(() -> {
            Fixture fixture = createFixture();
            fixture.tabs.addTab("first", "First", new JLabel("first"));
            fixture.tabs.addTab("second", "Second", new JLabel("second"));
            fixture.tabs.addTab("third", "Third", new JLabel("third"));
            fixture.tabs.addTab("fourth", "Fourth", new JLabel("fourth"));

            assertTrue(fixture.tabs.splitTab("second", TabSplitPlacement.RIGHT));
            TabbedPanel secondGroup = findGroup(fixture.tabs, "second");
            assertTrue(fixture.tabs.transferTabTo("third", secondGroup));
            assertTrue(secondGroup.splitTab("third", TabSplitPlacement.BOTTOM));
            assertTrue(fixture.tabs.closeTab("first"));
            assertTrue(fixture.tabs.closeTab("fourth"));
            assertFalse(SwingUtilities.isDescendingFrom(fixture.tabs, fixture.rootPane));

            JLabel reopened = new JLabel("reopened");
            fixture.tabs.addTab("reopened", "Reopened", reopened);

            assertTrue(SwingUtilities.isDescendingFrom(reopened, fixture.rootPane));
            assertTrue(fixture.tabs.contains("reopened"));
            assertEquals(2, fixture.tabs.getDockGroups().size());
            return null;
        });
    }

    @Test
    void createsDetachedPreviewSnapshotWithinConfiguredBounds() throws Exception {
        onEdt(() -> {
            TabbedPanel tabs = new TabbedPanel();
            JPanel content = new JPanel();
            content.setBackground(new Color(0x123456));
            content.setSize(900, 600);
            tabs.addTab("editor", "Editor.java", content);

            TabDragController controller = new TabDragController(tabs);
            controller.setDetachedPreviewSize(new Dimension(320, 180));
            controller.setDetachedPreviewAlpha(0.76f);
            BufferedImage image = controller.createDetachedPreviewImage(tabs.getEntry("editor"));

            assertTrue(image.getWidth() <= 320);
            assertTrue(image.getHeight() <= 180);
            assertTrue(image.getWidth() >= 160);
            assertEquals(0.76f, controller.getDetachedPreviewAlpha());
            assertTrue((image.getRGB(image.getWidth() / 2, image.getHeight() - 2) >>> 24) > 0);
            return null;
        });
    }

    @Test
    void copiesDetachedPreviewConfigurationToSplitGroups() throws Exception {
        onEdt(() -> {
            Fixture fixture = createFixture();
            fixture.tabs.setDetachedTabPreviewEnabled(true)
                    .setDetachedTabPreviewSize(new Dimension(300, 170))
                    .setDetachedTabPreviewAlpha(0.74f);
            fixture.tabs.addTab("first", "First", new JLabel("first"));
            fixture.tabs.addTab("second", "Second", new JLabel("second"));

            assertTrue(fixture.tabs.splitTab("second", TabSplitPlacement.RIGHT));
            TabbedPanel splitGroup = findGroup(fixture.tabs, "second");

            assertTrue(splitGroup.isDetachedTabPreviewEnabled());
            assertEquals(new Dimension(300, 170), splitGroup.getDetachedTabPreviewSize());
            assertEquals(0.74f, splitGroup.getDetachedTabPreviewAlpha());
            return null;
        });
    }

    private static Fixture createFixture() {
        JRootPane rootPane = new JRootPane();
        JPanel host = new JPanel(new BorderLayout());
        TabbedPanel tabs = new TabbedPanel();
        tabs.setDockModeEnabled(true);
        host.add(tabs, BorderLayout.CENTER);
        rootPane.setContentPane(host);
        return new Fixture(rootPane, tabs);
    }

    private static TabbedPanel findGroup(TabbedPanel root, String key) {
        Component component = root.find(key);
        return root.getDockGroups().stream()
                .filter(group -> group.getTabbedPane().indexOfComponent(component) >= 0)
                .findFirst()
                .orElseThrow();
    }

    private static <T> T onEdt(Callable<T> action) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return action.call();
        }

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
        if (throwable instanceof Exception exception) {
            throw exception;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        if (throwable != null) {
            throw new InvocationTargetException(throwable);
        }
        return result.get();
    }

    private record Fixture(JRootPane rootPane, TabbedPanel tabs) {
    }
}
