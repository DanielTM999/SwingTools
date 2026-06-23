package dtm.stools.component.panels;

import dtm.stools.component.annimation.ComponentAnimator;
import dtm.stools.component.events.EventType;
import dtm.stools.component.events.KeyPanelContextChangeEvent;
import dtm.stools.component.panels.base.PanelEventListener;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class KeyPanel extends PanelEventListener {

    private final Map<String, JPanel> panelsByKey = new ConcurrentHashMap<>();
    private final Map<JPanel, String> keyByPanel = new ConcurrentHashMap<>();
    private final List<String> order = new ArrayList<>();
    private final AtomicReference<String> currentKey = new AtomicReference<>();
    private final AtomicReference<String> lastKey = new AtomicReference<>();
    private final AtomicReference<ComponentAnimator<JPanel>> animator = new AtomicReference<>();
    private final AtomicLong idGenerator = new AtomicLong();


    public KeyPanel() {
        super(new KeyPanelLayout());
        setOpaque(true);
    }

    public KeyPanel(boolean opaque) {
        super(new KeyPanelLayout());
        setOpaque(opaque);
    }

    public void register(String key, JPanel panel) {
        register(key, panel, false);
    }

    public void register(String key, JPanel panel, boolean call) {
        panelsByKey.put(key, panel);
        keyByPanel.put(panel, key);
        order.add(key);
        panel.setVisible(false);
        add(panel);
        if (call) {
            switchTo(key);
        }
    }

    public String register(JPanel panel) {
        String key = "panel-" + idGenerator.incrementAndGet();
        register(key, panel);
        return key;
    }

    public void unregister(String key) {
        JPanel panel = panelsByKey.remove(key);
        if (panel == null) return;
        keyByPanel.remove(panel);
        order.remove(key);
        remove(panel);
        if (Objects.equals(currentKey.get(), key)) {
            currentKey.set(null);
            if (!order.isEmpty()) {
                switchTo(order.getFirst());
            }
        }
        revalidate();
        repaint();
    }

    public void unregisterAll(){
        panelsByKey.clear();
        keyByPanel.clear();
        order.clear();
        removeAll();
        revalidate();
        repaint();
    }

    public void unregister(JPanel panel) {
        String key = keyByPanel.get(panel);
        if (key != null) unregister(key);
    }

    @Override
    public void setLayout(LayoutManager mgr) {
        if (!(mgr instanceof KeyPanelLayout)) {
            throw new UnsupportedOperationException(
                    "KeyPanel manages its own layout. Modify the layout of the registered panels, not the KeyPanel itself."
            );
        }
        super.setLayout(mgr);
    }

    public void switchTo(String key) {
        if (!panelsByKey.containsKey(key)) return;

        String oldKey = currentKey.get();

        if (key.equals(oldKey)) return;

        JPanel current = oldKey != null ? panelsByKey.get(oldKey) : null;
        JPanel next = panelsByKey.get(key);

        KeyPanelContextChangeEventImple eventImple = dispatchChangeEventBefore(current, next);

        if(eventImple.canceled) return;

        lastKey.set((oldKey == null ? key : oldKey));
        currentKey.set(key);

        if (animator.get() != null && current != null) {
            animateSwitch(current, next);
        } else {
            instantSwitch(next);
        }
    }

    public void switchTo(JPanel panel) {
        String key = keyByPanel.get(panel);
        if (key != null) switchTo(key);
    }

    public void switchFirst() {
        if (!order.isEmpty()) switchTo(order.getFirst());
    }

    public void switchLast() {
        if (!order.isEmpty()) switchTo(order.getLast());
    }

    public void switchToLastPanel() {
        String last = lastKey.get();
        if (last != null && panelsByKey.containsKey(last)) {
            switchTo(last);
        }
    }

    public void switchNext() {
        if (order.isEmpty()) return;
        String key = currentKey.get();
        int index = order.indexOf(key);
        if (index < 0 || index + 1 >= order.size()) return;
        switchTo(order.get(index + 1));
    }

    public void switchPrevious() {
        if (order.isEmpty()) return;
        String key = currentKey.get();
        int index = order.indexOf(key);
        if (index <= 0) return;
        switchTo(order.get(index - 1));
    }

    public boolean currentIs(JComponent panel){
        return getCurrent() == panel;
    }

    public JPanel getCurrent() {
        String key = currentKey.get();
        return key == null ? null : panelsByKey.get(key);
    }

    public String getCurrentKey() {
        return currentKey.get();
    }

    public JPanel find(String key) {
        return panelsByKey.get(key);
    }

    public boolean contains(String key) {
        return panelsByKey.containsKey(key);
    }

    public boolean contains(JPanel panel) {
        return keyByPanel.containsKey(panel);
    }

    public String getKeyOf(JPanel panel) {
        return keyByPanel.get(panel);
    }

    public Set<String> getKeys() {
        return panelsByKey.keySet();
    }

    public Collection<JPanel> getPanels() {
        return panelsByKey.values();
    }

    public int getSizePanel() {
        return panelsByKey.size();
    }

    public boolean isEmpty() {
        return panelsByKey.isEmpty();
    }

    public void setAnimator(ComponentAnimator<JPanel> animator) {
        this.animator.set(animator);
    }

    private void instantSwitch(JPanel next) {
        panelsByKey.forEach((k, p) -> {
            boolean active = Objects.equals(k, currentKey.get());
            p.setVisible(active);
            if (active) {
                p.setBounds(0, 0, getWidth(), getHeight());
            } else {
                p.setBounds(0, 0, 0, 0);
            }
        });
        revalidate();
        repaint();
        dispatchChangeEventAfter(getCurrent(), next);
    }

    private void animateSwitch(JPanel current, JPanel next) {
        animator.get().animate(current, next, () -> {
            next.setBounds(0, 0, getWidth(), getHeight());
            next.setVisible(true);
            current.setVisible(false);
            revalidate();
            repaint();
            dispatchChangeEventAfter(current, next);
        });
    }

    private void dispatchChangeEventAfter(JPanel current, JPanel next) {
        KeyPanelContextChangeEventImple eventImple = new KeyPanelContextChangeEventImple();
        eventImple.current = current;
        eventImple.next = next;
        eventImple.key = getCurrentKey();

        dispachEvent(EventType.CHANGE, eventImple, new HashMap<String, Object>(){{
            put("id", getCurrentKey());
        }});
    }

    private KeyPanelContextChangeEventImple dispatchChangeEventBefore(JPanel current, JPanel next) {
        KeyPanelContextChangeEventImple eventImple = new KeyPanelContextChangeEventImple();
        eventImple.current = current;
        eventImple.next = next;
        eventImple.key = getCurrentKey();

        dispachEvent(EventType.BEFORE_CHANGE, eventImple, new HashMap<String, Object>(){{
            put("id", getCurrentKey());
        }});

        return eventImple;
    }

    private void notifyPanelsResized(int w, int h) {
        String current = currentKey.get();
        if (current == null) return;

        panelsByKey.forEach((k, p) -> {
            if (Objects.equals(k, current)) {
                p.setBounds(0, 0, w, h);
                p.revalidate();
                p.doLayout();
                if (p instanceof KeyPanel nested) {
                    nested.notifyPanelsResized(w, h);
                }
            } else {
                p.setBounds(0, 0, 0, 0);
            }
        });
    }

    static class KeyPanelLayout implements LayoutManager2 {

        @Override
        public void layoutContainer(Container parent) {
            int w = parent.getWidth();
            int h = parent.getHeight();

            for (Component comp : parent.getComponents()) {
                if (!comp.isVisible()) {
                    comp.setBounds(0, 0, 0, 0);
                } else {
                    comp.setBounds(0, 0, w, h);
                    if (comp instanceof JComponent jc) {
                        jc.revalidate();
                    }
                }
            }

            if (parent instanceof KeyPanel keyPanel) {
                keyPanel.notifyPanelsResized(w, h);
                keyPanel.dispachEvent(EventType.RESIZE, new Dimension(w, h));
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return parent.getSize();
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(0, 0);
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override public void addLayoutComponent(Component comp, Object constraints) {}
        @Override public void addLayoutComponent(String name, Component comp) {}
        @Override public void removeLayoutComponent(Component comp) {}
        @Override public float getLayoutAlignmentX(Container target) { return 0.5f; }
        @Override public float getLayoutAlignmentY(Container target) { return 0.5f; }
        @Override public void invalidateLayout(Container target) {}
    }


    private static class KeyPanelContextChangeEventImple implements KeyPanelContextChangeEvent{

        private JPanel current;
        private JPanel next;
        private String key;
        private boolean canceled = false;

        @Override
        public JPanel getCurrentPanel() {
            return current;
        }

        @Override
        public JPanel getNextPanel() {
            return next;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public void cancel() {
            this.canceled = true;
        }
    }
}