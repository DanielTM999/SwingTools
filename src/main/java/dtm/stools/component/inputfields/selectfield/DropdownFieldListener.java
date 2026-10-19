package dtm.stools.component.inputfields.selectfield;

import dtm.stools.component.events.EventComponent;
import dtm.stools.component.events.EventListenerComponent;
import dtm.stools.component.events.EventSubscription;
import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.layout.FieldLayoutConfigurable;
import dtm.stools.component.inputfields.textfield.layout.FieldLayoutManager;
import dtm.stools.utils.PaintUtils;
import lombok.NonNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DropdownFieldListener<T> extends JComboBox<T> implements EventListenerComponent, FieldLayoutConfigurable {

    protected final Map<String, List<Consumer<EventComponent>>> listeners = new ConcurrentHashMap<>();
    private String label = "";
    private FieldLayoutManager fieldLayoutManager;
    private ComboBoxUI conventionalUI;
    private boolean originalOpaque;
    private JTextComponent layoutEditor;
    private Document layoutDocument;
    private Border originalEditorBorder;
    private boolean originalEditorOpaque;
    private final PopupMenuListener layoutPopupListener = new PopupMenuListener() {
        @Override
        public void popupMenuWillBecomeVisible(PopupMenuEvent e) { repaint(); }

        @Override
        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) { repaint(); }

        @Override
        public void popupMenuCanceled(PopupMenuEvent e) { repaint(); }
    };
    private final DocumentListener layoutDocumentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) { fieldContentChanged(); }

        @Override
        public void removeUpdate(DocumentEvent e) { fieldContentChanged(); }

        @Override
        public void changedUpdate(DocumentEvent e) { fieldContentChanged(); }
    };
    private final PropertyChangeListener layoutEditorListener = e -> {
        if ("document".equals(e.getPropertyName())) {
            bindLayoutDocument();
            fieldContentChanged();
        }
    };

    @Override
    public JComboBox<T> getFieldComponent() { return this; }

    @Override
    public String getLabel() { return label == null ? "" : label; }

    public void setLabel(String label) {
        String previous = getLabel();
        this.label = label == null ? "" : label;
        getAccessibleContext().setAccessibleName(this.label);
        firePropertyChange("label", previous, this.label);
        refreshFieldLayout();
    }

    @Override
    public FieldLayoutManager getFieldLayoutManager() { return fieldLayoutManager; }

    @Override
    public void setFieldLayoutManager(FieldLayoutManager next) {
        FieldLayoutManager previous = fieldLayoutManager;
        if (previous == next) return;
        if (previous == null) originalOpaque = isOpaque();
        if (previous != null) previous.uninstall(this);
        fieldLayoutManager = next;
        try {
            if (next != null) {
                next.install(this);
                installFieldUI();
            } else {
                restoreFieldUI();
            }
        } catch (RuntimeException | Error failure) {
            if (next != null) next.uninstall(this);
            fieldLayoutManager = previous;
            if (previous != null) {
                previous.install(this);
                installFieldUI();
            } else {
                restoreFieldUI();
            }
            refreshFieldLayout();
            throw failure;
        }
        firePropertyChange("fieldLayoutManager", previous, next);
        refreshFieldLayout();
    }

    @Override
    public boolean isFieldContentEmpty() {
        if (isEditable() && getEditor() != null) {
            Component component = getEditor().getEditorComponent();
            if (component instanceof JTextComponent text) return text.getDocument().getLength() == 0;
            Object value = getEditor().getItem();
            return value == null || value.toString().isEmpty();
        }
        return getSelectedItem() == null;
    }

    @Override
    public Rectangle getFieldContentBounds() {
        if (getUI() instanceof FieldLayoutComboBoxUI ui) return ui.getContentBounds();
        if (getUI() instanceof FlatFieldLayoutComboBoxUI ui) return ui.getContentBounds();
        Insets insets = getInsets();
        return new Rectangle(insets.left, insets.top,
                Math.max(0, getWidth() - insets.left - insets.right),
                Math.max(0, getHeight() - insets.top - insets.bottom));
    }

    protected boolean isFieldPlaceholderVisible() {
        return fieldLayoutManager == null || fieldLayoutManager.isPlaceholderVisible(this);
    }

    private void installFieldUI() {
        if (!FieldLayoutComboBoxUI.isInstalled(getUI())) {
            conventionalUI = getUI();
            replaceFieldUI(FieldLayoutComboBoxUI.create(conventionalUI));
        }
        removePopupMenuListener(layoutPopupListener);
        addPopupMenuListener(layoutPopupListener);
        setOpaque(false);
        bindLayoutEditor();
    }

    private void restoreFieldUI() {
        removePopupMenuListener(layoutPopupListener);
        unbindLayoutEditor();
        if (conventionalUI != null) {
            ComboBoxUI previous = conventionalUI;
            conventionalUI = null;
            replaceFieldUI(previous);
        }
        setOpaque(originalOpaque);
    }

    private void replaceFieldUI(ComboBoxUI ui) {
        boolean editing = isEditable() && getEditor() != null;
        Object value = editing ? getEditor().getItem() : null;
        setUI(ui);
        if (editing && getEditor() != null) getEditor().setItem(value);
    }

    private void bindLayoutEditor() {
        Component component = isEditable() && getEditor() != null ? getEditor().getEditorComponent() : null;
        if (component == layoutEditor) return;
        unbindLayoutEditor();
        if (fieldLayoutManager == null || !(component instanceof JTextComponent text)) return;
        layoutEditor = text;
        originalEditorBorder = text.getBorder();
        originalEditorOpaque = text.isOpaque();
        text.setBorder(BorderFactory.createEmptyBorder());
        text.setOpaque(false);
        text.addPropertyChangeListener(layoutEditorListener);
        bindLayoutDocument();
    }

    private void bindLayoutDocument() {
        if (layoutDocument != null) layoutDocument.removeDocumentListener(layoutDocumentListener);
        layoutDocument = layoutEditor == null ? null : layoutEditor.getDocument();
        if (layoutDocument != null) layoutDocument.addDocumentListener(layoutDocumentListener);
    }

    private void unbindLayoutEditor() {
        if (layoutDocument != null) layoutDocument.removeDocumentListener(layoutDocumentListener);
        layoutDocument = null;
        if (layoutEditor == null) return;
        layoutEditor.removePropertyChangeListener(layoutEditorListener);
        layoutEditor.setBorder(originalEditorBorder);
        layoutEditor.setOpaque(originalEditorOpaque);
        layoutEditor = null;
    }

    private void fieldContentChanged() {
        if (fieldLayoutManager != null) fieldLayoutManager.fieldChanged(this);
        repaint();
    }

    private void refreshFieldLayout() {
        revalidate();
        repaint();
    }

    @Override
    public void contentsChanged(ListDataEvent e) {
        super.contentsChanged(e);
        fieldContentChanged();
    }

    @Override
    public void setModel(ComboBoxModel<T> model) {
        super.setModel(model);
        fieldContentChanged();
    }

    @Override
    public void setEditable(boolean editable) {
        super.setEditable(editable);
        bindLayoutEditor();
        fieldContentChanged();
    }

    @Override
    public void setEditor(ComboBoxEditor editor) {
        unbindLayoutEditor();
        super.setEditor(editor);
        bindLayoutEditor();
        fieldContentChanged();
    }

    @Override
    public void updateUI() {
        boolean editing = fieldLayoutManager != null && isEditable() && getEditor() != null;
        Object value = editing ? getEditor().getItem() : null;
        unbindLayoutEditor();
        super.updateUI();
        if (fieldLayoutManager != null) {
            installFieldUI();
            if (editing && getEditor() != null) getEditor().setItem(value);
            fieldLayoutManager.themeChanged(this);
        }
    }

    @Override
    public Insets getInsets() {
        return fieldLayoutManager == null ? super.getInsets() : fieldLayoutManager.getInsets(this);
    }

    @Override
    public Insets getInsets(Insets insets) {
        Insets resolved = getInsets();
        if (insets == null) return resolved;
        insets.set(resolved.top, resolved.left, resolved.bottom, resolved.right);
        return insets;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension natural = super.getPreferredSize();
        return fieldLayoutManager == null || isPreferredSizeSet() ? natural
                : fieldLayoutManager.getPreferredSize(this, natural);
    }

    @Override
    public Dimension getMinimumSize() {
        Dimension natural = super.getMinimumSize();
        return fieldLayoutManager == null || isMinimumSizeSet() ? natural
                : fieldLayoutManager.getMinimumSize(this, natural);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (fieldLayoutManager != null) {
            Graphics2D background = PaintUtils.antialias((Graphics2D) g.create());
            try { fieldLayoutManager.paintBackground(background, this); }
            finally { background.dispose(); }
        }
        super.paintComponent(g);
    }

    @Override
    protected void paintBorder(Graphics g) {
        if (fieldLayoutManager == null) {
            super.paintBorder(g);
            return;
        }
        Graphics2D border = PaintUtils.antialias((Graphics2D) g.create());
        try { fieldLayoutManager.paintBorder(border, this); }
        finally { border.dispose(); }
    }

    @Override
    protected void paintChildren(Graphics g) {
        super.paintChildren(g);
        if (fieldLayoutManager == null) return;
        Graphics2D overlay = PaintUtils.antialias((Graphics2D) g.create());
        try { fieldLayoutManager.paintOverlay(overlay, this); }
        finally { overlay.dispose(); }
    }

    @Override
    public EventSubscription addEventListener(String eventType, Consumer<EventComponent> event) {
        if(eventType == null || eventType.isEmpty()) return () -> {};

        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(event);
        return () -> removeEventListner(eventType, event);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        if (fieldLayoutManager != null) fieldLayoutManager.fieldShown(this);
        SwingUtilities.invokeLater(() -> {
            dispachEvent(EventType.LOAD, this, this);
        });
    }

    @Override
    public void removeNotify() {
        if (fieldLayoutManager != null) fieldLayoutManager.fieldRemoved(this);
        super.removeNotify();
    }

    @Override
    public void removeAllListeners() {
        listeners.clear();
    }

    @Override
    public void removeEventListner(String eventType, Consumer<EventComponent> event) {
        listeners.get(eventType).remove(event);
    }

    @Override
    public void removeEventListner(String eventType) {
        listeners.remove(eventType);
    }

    @Override
    public Map<String, List<Consumer<EventComponent>>> getEventListners() {
        return new ConcurrentHashMap<>(listeners);
    }

    protected void dispachEvent(String eventType, Object value){
        dispachEvent(eventType, DropdownFieldListener.this, value);
    }

    protected <S> void dispachEvent(String eventType, Supplier<S> value){
        dispachEvent(eventType, DropdownFieldListener.this, value);
    }

    protected void dispachEvent(String eventType, Component component, Object value){
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
                    public <S> S tryGetValue() {
                        try {
                            return (S) value;
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public String getEventType() {
                        return eventType;
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

    protected <S> void dispachEvent(String eventType, Component component, Supplier<S> value){
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
                    public S tryGetValue() {
                        try {
                            return (S) value.get();
                        } catch (Exception e) {
                            return null;
                        }
                    }

                    @Override
                    public String getEventType() {
                        return eventType;
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

    public Object getValue() {
        return getSelectedItem();
    }

    public <S> S getValue(@NonNull Class<S> type) {
        Object value = getSelectedItem();
        return (type.isInstance(value)) ? type.cast(value) : null;
    }

    @SuppressWarnings("unchecked")
    public <S> S getValueAs() {
        try{
            Object value = getSelectedItem();
            return (S) value;
        }catch (Exception e){
            return null;
        }
    }

}
