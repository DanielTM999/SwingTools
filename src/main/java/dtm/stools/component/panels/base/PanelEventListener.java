package dtm.stools.component.panels.base;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventSubscription;
import dtm.stools.component.events.EventType;
import dtm.stools.component.accessibility.AccessibleControls;
import dtm.stools.component.panels.BlockingPanel;

import javax.swing.*;
import javax.accessibility.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class PanelEventListener extends BlockingPanel implements EventListenerComponent {

    @Override
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            AccessibleControls.Model model = AccessibleControls.forControl(this);
            if (model != null) accessibleContext = new AccessibleControl(model);
        }
        return super.getAccessibleContext();
    }

    /** Uses the real panel's geometry, focus and parent, adding semantics for painted controls. */
    protected class AccessibleControl extends AccessibleJPanel implements AccessibleAction, AccessibleValue, AccessibleSelection {
        private final AccessibleControls.Model model;
        private final Map<Integer, JRadioButton> virtualOptions = new HashMap<>();
        private boolean selected, mixed, editable;
        private Number value;
        private int selection;
        private String text;

        AccessibleControl(AccessibleControls.Model model) {
            this.model = model;
            selected = model.selected(); mixed = model.mixed(); editable = model.editable();
            value = model.value(); selection = model.selectedIndex(); text = model.text();
            PanelEventListener.this.addPropertyChangeListener(e -> {
                if ("text".equals(e.getPropertyName()) && accessibleName == null) firePropertyChange(ACCESSIBLE_NAME_PROPERTY, e.getOldValue(), e.getNewValue());
                if ("options".equals(e.getPropertyName())) firePropertyChange(ACCESSIBLE_VISIBLE_DATA_PROPERTY, e.getOldValue(), e.getNewValue());
                refreshSemantics();
            });
        }
        private void refreshSemantics() {
            boolean nextSelected = model.selected(), nextMixed = model.mixed(), nextEditable = model.editable();
            Number nextValue = model.value(); int nextSelection = model.selectedIndex(); String nextText = model.text();
            if (selected != nextSelected) firePropertyChange(ACCESSIBLE_STATE_PROPERTY, selected ? AccessibleState.CHECKED : null, nextSelected ? AccessibleState.CHECKED : null);
            if (mixed != nextMixed) firePropertyChange(ACCESSIBLE_STATE_PROPERTY, mixed ? AccessibleState.INDETERMINATE : null, nextMixed ? AccessibleState.INDETERMINATE : null);
            if (editable != nextEditable) firePropertyChange(ACCESSIBLE_STATE_PROPERTY, editable ? AccessibleState.EDITABLE : null, nextEditable ? AccessibleState.EDITABLE : null);
            if (!java.util.Objects.equals(value, nextValue)) firePropertyChange(ACCESSIBLE_VALUE_PROPERTY, value, nextValue);
            if (selection != nextSelection) {
                firePropertyChange(ACCESSIBLE_SELECTION_PROPERTY, selection, nextSelection);
                if (virtualOptions.containsKey(selection)) virtualOptions.get(selection).getAccessibleContext().firePropertyChange(ACCESSIBLE_STATE_PROPERTY, AccessibleState.CHECKED, null);
                if (virtualOptions.containsKey(nextSelection)) virtualOptions.get(nextSelection).getAccessibleContext().firePropertyChange(ACCESSIBLE_STATE_PROPERTY, null, AccessibleState.CHECKED);
            }
            if (!java.util.Objects.equals(text, nextText)) firePropertyChange(ACCESSIBLE_TEXT_PROPERTY, null, 0);
            selected = nextSelected; mixed = nextMixed; editable = nextEditable;
            value = nextValue; selection = nextSelection; text = nextText;
        }
        @Override
        public AccessibleRole getAccessibleRole() { return model.role(); }
        @Override
        public String getAccessibleName() {
            String explicit = super.getAccessibleName();
            return explicit != null ? explicit : model.name();
        }
        @Override
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            if (model.selected()) { states.add(AccessibleState.CHECKED); states.add(AccessibleState.SELECTED); }
            if (model.mixed()) states.add(AccessibleState.INDETERMINATE);
            if (model.numeric() && model.editable()) states.add(AccessibleState.EDITABLE);
            return states;
        }
        @Override
        public AccessibleAction getAccessibleAction() { return model.actionCount() > 0 ? this : null; }
        @Override
        public int getAccessibleActionCount() { return model.actionCount(); }
        @Override
        public String getAccessibleActionDescription(int index) { return index >= 0 && index < model.actionCount() ? model.actionName(index) : null; }
        @Override
        public boolean doAccessibleAction(int index) {
            if (index < 0 || index >= model.actionCount() || !isEnabled() || !model.editable()) return false;
            return onEdt(() -> model.action(index));
        }
        @Override
        public AccessibleValue getAccessibleValue() { return model.numeric() ? this : null; }
        @Override
        public Number getCurrentAccessibleValue() { return model.value(); }
        @Override
        public Number getMinimumAccessibleValue() { return model.minimum(); }
        @Override
        public Number getMaximumAccessibleValue() { return model.maximum(); }
        @Override
        public boolean setCurrentAccessibleValue(Number next) {
            if (!model.numeric() || next == null || !isEnabled() || !model.editable() || !Double.isFinite(next.doubleValue())) return false;
            java.math.BigDecimal number = new java.math.BigDecimal(next.toString());
            if (model.minimum() != null && number.compareTo(new java.math.BigDecimal(model.minimum().toString())) < 0) return false;
            if (model.maximum() != null && number.compareTo(new java.math.BigDecimal(model.maximum().toString())) > 0) return false;
            return onEdt(() -> model.value(next));
        }
        @Override
        public AccessibleSelection getAccessibleSelection() { return model.selection() ? this : null; }
        @Override
        public int getAccessibleChildrenCount() { return model.selection() ? model.options().size() : super.getAccessibleChildrenCount(); }
        @Override
        public Accessible getAccessibleChild(int index) {
            if (!model.selection()) return super.getAccessibleChild(index);
            if (index < 0 || index >= model.options().size()) return null;
            Accessible nativeChild = model.child(index); if (nativeChild != null) return nativeChild;
            JRadioButton option = virtualOptions.computeIfAbsent(index, i -> new JRadioButton() {
                @Override
                public boolean isSelected() { return AccessibleControl.this.model.selectedIndex() == i; }
                @Override
                public boolean isEnabled() { return PanelEventListener.this.isEnabled(); }
                @Override
                public AccessibleContext getAccessibleContext() {
                    if (accessibleContext == null) accessibleContext = new AccessibleJRadioButton() {
                        @Override
                        public int getAccessibleIndexInParent() { return i; }
                    };
                    return accessibleContext;
                }
            });
            if (option.getActionListeners().length == 0) option.addActionListener(e -> { if (isEnabled()) model.select(index); });
            option.setText(model.options().get(index));
            option.getAccessibleContext().setAccessibleParent(PanelEventListener.this);
            int width = PanelEventListener.this.getWidth() / Math.max(1, model.options().size());
            option.setBounds(index * width, 0, width, PanelEventListener.this.getHeight());
            return option;
        }
        @Override
        public int getAccessibleSelectionCount() { return model.selectedIndex() >= 0 ? 1 : 0; }
        @Override
        public Accessible getAccessibleSelection(int index) { return index == 0 && model.selectedIndex() >= 0 ? getAccessibleChild(model.selectedIndex()) : null; }
        @Override
        public boolean isAccessibleChildSelected(int index) { return index >= 0 && index == model.selectedIndex(); }
        @Override
        public void addAccessibleSelection(int index) {
            if (isEnabled() && index >= 0 && index < model.options().size()) onEdt(() -> model.select(index));
        }
        @Override
        public void removeAccessibleSelection(int index) { if (isAccessibleChildSelected(index)) clearAccessibleSelection(); }
        @Override
        public void clearAccessibleSelection() { if (isEnabled()) onEdt(model::clearSelection); }
        @Override
        public void selectAllAccessibleSelection() {}
        @Override
        public AccessibleText getAccessibleText() {
            String value = model.text();
            if (value == null) return super.getAccessibleText();
            JTextField delegate = new JTextField(value);
            delegate.setFont(PanelEventListener.this.getFont()); delegate.setSize(PanelEventListener.this.getSize());
            return delegate.getAccessibleContext().getAccessibleText();
        }
        private boolean onEdt(Runnable action) {
            if (SwingUtilities.isEventDispatchThread()) { action.run(); return true; }
            try { SwingUtilities.invokeAndWait(action); return true; }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; }
            catch (java.lang.reflect.InvocationTargetException e) { return false; }
        }
    }

    protected final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();

    protected PanelEventListener(){}

    protected PanelEventListener(boolean opaque){
        setOpaque(opaque);
    }

    protected PanelEventListener(LayoutManager layout){
        this(layout, false);
    }

    protected PanelEventListener(LayoutManager layout, boolean opaque){
        super(layout);
        setOpaque(opaque);
    }

    @Override
    public EventSubscription addEventListener(String eventType, Consumer<EventComponent> event) {
        if(eventType == null || eventType.isEmpty()) return () -> {};

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        return () -> removeEventListner(eventType, event);
    }

    @Override
    public void removeEventListner(String eventType, Consumer<EventComponent> event) {
        List<Consumer<EventComponent>> eventListeners = listeners.get(eventType);

        if (eventListeners == null) {
            return;
        }

        eventListeners.remove(event);

        if (eventListeners.isEmpty()) {
            listeners.remove(eventType);
        }
    }

    @Override
    public void removeEventListner(String eventType) {
        listeners.remove(eventType);
    }

    @Override
    public Map<String, List<Consumer<EventComponent>>> getEventListners() {
        return new ConcurrentHashMap<>(listeners);
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(() -> {
            dispatchEvent(EventType.LOAD, this, this, new HashMap<>());
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    protected void dispatchEvent(String eventType, Object value){
        dispatchEvent(eventType, PanelEventListener.this, value, new HashMap<>());
    }

    protected <T> void dispatchEvent(String eventType, Supplier<T> value){
        dispatchEvent(eventType, PanelEventListener.this, value);
    }

    protected void dispatchEvent(String eventType, Object value, Map<String, Object> props){
        dispatchEvent(eventType, PanelEventListener.this, value, props);
    }

    protected <T> void dispatchEvent(String eventType, Supplier<T> value, Map<String, Object> props){
        dispatchEvent(eventType, PanelEventListener.this, value, props);
    }

    protected void dispatchEvent(String eventType, Component component, Object value, Map<String, Object> props){
        if (listeners != null && !listeners.isEmpty()) {
            List<Consumer<EventComponent>> listeners = this.listeners.get(eventType);
            if(listeners != null && !listeners.isEmpty()){
                EventComponent event = new EventComponent() {
                    @Override
                    public Component getComponent() {
                        return component;
                    }

                    @Override
                    public Object getValue() {
                        return value;
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> T tryGetValue() {
                        try {
                            return (T) value;
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public String getEventType() {
                        return eventType;
                    }

                    @Override
                    public Map<String, Object> getProperties() {
                        return props;
                    }
                };
                listeners.forEach(listener -> {
                    try{
                        listener.accept(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

    protected <T> void dispatchEvent(String eventType, Component component, Supplier<T> value){
        dispatchEvent(eventType, component, value, new HashMap<>());
    }

    protected <T> void dispatchEvent(String eventType, Component component, Supplier<T> value, Map<String, Object> props){
        if (listeners != null && !listeners.isEmpty()) {
            List<Consumer<EventComponent>> listeners = this.listeners.get(eventType);
            if(listeners != null && !listeners.isEmpty()){
                EventComponent event = new EventComponent() {
                    @Override
                    public Component getComponent() {
                        return component;
                    }

                    @Override
                    public Object getValue() {
                        return value.get();
                    }

                    @SuppressWarnings("unchecked")
                    @Override
                    public T tryGetValue() {
                        try {
                            return value.get();
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public String getEventType() {
                        return eventType;
                    }

                    @Override
                    public Map<String, Object> getProperties() {
                        return props;
                    }
                };
                listeners.forEach(listener -> {
                    try{
                        listener.accept(event);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }
        }
    }

}
