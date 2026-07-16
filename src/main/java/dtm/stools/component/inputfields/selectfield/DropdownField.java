package dtm.stools.component.inputfields.selectfield;

import dtm.stools.component.events.EventType;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

public class DropdownField extends DropdownFieldListener<Object> {

    private static final long TYPE_AHEAD_RESET_DELAY_MS = 1000L;
    private static final String TYPE_AHEAD_ENTER_ACTION = "DropdownField.typeAheadEnter";

    private Function<Object, String> displayFunction = Objects::toString;
    private ListCellRenderer<? super Object> customRenderer = null;
    private String placeholder;
    private java.awt.Color placeholderColor = UIManager.getColor("TextField.inactiveForeground") != null
            ? UIManager.getColor("TextField.inactiveForeground")
            : java.awt.Color.GRAY;

    private boolean typeAheadSelectionEnabled = true;
    private boolean committingTypeAheadSelection = false;
    private int typeAheadPopupIndex = -1;
    private long lastTypeAheadAt = 0L;
    private String typeAheadBuffer = "";
    private JTextComponent editorTextComponent;
    private DocumentListener editorDocumentListener;
    private Object editorEnterActionKey;
    private Action editorEnterAction;

    private final KeyAdapter editorTypeAheadKeyHandler = new KeyAdapter() {
        @Override
        public void keyPressed(KeyEvent e) {
            if (!typeAheadSelectionEnabled || !isEditable()) return;

            if (handleTypeAheadNavigation(e.getKeyCode())) {
                e.consume();
            }
        }
    };

    public DropdownField(){
        init();
    }

    public DropdownField(Object... dataSource){
        init();
        setDataSource(dataSource);
    }

    public <T> DropdownField(Collection<T> dataSource){
        init();
        setDataSource(dataSource);
    }

    public void setDisplayText(Function<Object, String> displayFunction){
        this.displayFunction = (displayFunction != null) ? displayFunction : Objects::toString;
        applyRenderer();
        repaint();
    }

    public <T> void setDataSource(Collection<T> dataSource){
        clearTypeAheadSelection(false);
        if(dataSource == null) {
            setModel(new DefaultComboBoxModel<>());
            return;
        }

        setModel(new DefaultComboBoxModel<>(dataSource.toArray(Object[]::new)));
        setSelectedItem(null);
    }

    public void setDataSource(Object... dataSource){
        clearTypeAheadSelection(false);
        setModel(new DefaultComboBoxModel<>(dataSource));
        setSelectedItem(null);
    }

    public void setCustomRenderer(ListCellRenderer<? super Object> renderer){
        this.customRenderer = renderer;
        applyRenderer();
    }

    public boolean select(Object value){
        if(!contains(value)) return false;

        setSelectedItem(value);
        dispachEvent(EventType.CHANGE, this::getSelectedItem);
        return true;
    }

    public boolean isEmpty(){
        return getItemCount() == 0;
    }

    public int getItemSize(){
        return getItemCount();
    }

    public void clear(){
        clearTypeAheadSelection(false);
        setModel(new DefaultComboBoxModel<>());
        setSelectedItem(null);
        dispachEvent(EventType.CLEAR, () -> null);
    }

    public boolean contains(Object value){
        return IntStream.range(0, getItemCount())
                .mapToObj(this::getItemAt)
                .anyMatch(o -> Objects.equals(o, value));
    }

    public void selectFirst(){
        if(getItemCount() > 0){
            setSelectedIndex(0);
            dispachEvent(EventType.CHANGE, this::getSelectedItem);
        }
    }

    public void selectLast(){
        if(getItemCount() > 0){
            setSelectedIndex(getItemCount() - 1);
            dispachEvent(EventType.CHANGE, this::getSelectedItem);
        }
    }

    public void reload(){
        applyRenderer();
        refreshTypeAheadSelection();
        repaint();
    }

    public List<Object> getDataSource(){
        int size = getItemCount();
        return IntStream.range(0, size)
                .mapToObj(this::getItemAt)
                .toList();
    }

    public void setPlaceholder(String placeholder){
        this.placeholder = placeholder;
        applyRenderer();
    }

    public boolean isTypeAheadSelectionEnabled() {
        return typeAheadSelectionEnabled;
    }

    public void setTypeAheadSelectionEnabled(boolean typeAheadSelectionEnabled) {
        this.typeAheadSelectionEnabled = typeAheadSelectionEnabled;
        if (!typeAheadSelectionEnabled) {
            clearTypeAheadSelection(true);
        } else {
            refreshTypeAheadSelection();
        }
    }

    public <T> List<T> getDataSource(Class<T> type) {
        int size = getItemCount();
        return IntStream.range(0, size)
                .mapToObj(this::getItemAt)
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    private void applyRenderer(){
        if(customRenderer != null){
            super.setRenderer(customRenderer);
            return;
        }

        super.setRenderer(new DefaultListCellRenderer(){
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {

                java.awt.Component comp;

                if (value == null && index == -1 && placeholder != null) {
                    comp = super.getListCellRendererComponent(list, placeholder, index, isSelected, cellHasFocus);
                    comp.setForeground(placeholderColor);
                    return comp;
                }

                String text;
                try {
                    text = (value != null) ? displayFunction.apply(value) : "";
                } catch (Exception ex) {
                    text = "";
                }

                comp = super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);

                if (!isSelected) {
                    comp.setForeground(list.getForeground());
                }

                return comp;
            }
        });
    }

    private void init() {
        applyRenderer();
        configInternalChangeEvent();
        configTypeAheadSelection();
    }

    private void configInternalChangeEvent() {
        this.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                if (!committingTypeAheadSelection) {
                    clearTypeAheadSelection(false);
                }
                dispachEvent(EventType.CHANGE, this::getSelectedItem);
            }
        });
    }

    private void configTypeAheadSelection() {
        setKeySelectionManager((keyChar, model) -> {
            if (typeAheadSelectionEnabled) return -1;
            return findFirstKeySelectionMatch(keyChar, model);
        });
        installEditorTypeAhead();
    }

    @Override
    public void setEditable(boolean aFlag) {
        if (!aFlag) {
            uninstallEditorTypeAhead();
        }
        super.setEditable(aFlag);
        if (aFlag) {
            installEditorTypeAhead();
        }
    }

    @Override
    public void setEditor(ComboBoxEditor anEditor) {
        uninstallEditorTypeAhead();
        super.setEditor(anEditor);
        installEditorTypeAhead();
    }

    @Override
    public void processKeyEvent(KeyEvent e) {
        if (typeAheadSelectionEnabled && !isEditable()) {
            if (e.getID() == KeyEvent.KEY_TYPED && isTypeAheadCharacter(e)) {
                updateBufferedTypeAheadSelection(e.getKeyChar());
                e.consume();
                return;
            }

            if (e.getID() == KeyEvent.KEY_PRESSED) {
                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && !typeAheadBuffer.isEmpty()) {
                    typeAheadBuffer = typeAheadBuffer.substring(0, typeAheadBuffer.length() - 1);
                    updateTypeAheadSelection(typeAheadBuffer);
                    e.consume();
                    return;
                }

                if (handleTypeAheadNavigation(e.getKeyCode())) {
                    e.consume();
                    return;
                }
            }
        }

        super.processKeyEvent(e);
    }

    private void installEditorTypeAhead() {
        if (!isEditable() || getEditor() == null) return;

        java.awt.Component editorComponent = getEditor().getEditorComponent();
        if (!(editorComponent instanceof JTextComponent textComponent)) return;

        if (editorTextComponent == textComponent && editorDocumentListener != null) return;

        uninstallEditorTypeAhead();

        editorTextComponent = textComponent;
        editorDocumentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateEditorTypeAheadSelection();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateEditorTypeAheadSelection();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateEditorTypeAheadSelection();
            }
        };

        editorTextComponent.getDocument().addDocumentListener(editorDocumentListener);
        editorTextComponent.addKeyListener(editorTypeAheadKeyHandler);
        installEditorEnterAction();
    }

    private void uninstallEditorTypeAhead() {
        if (editorTextComponent == null) return;

        uninstallEditorEnterAction();

        if (editorDocumentListener != null) {
            editorTextComponent.getDocument().removeDocumentListener(editorDocumentListener);
        }

        editorTextComponent.removeKeyListener(editorTypeAheadKeyHandler);
        editorDocumentListener = null;
        editorTextComponent = null;
    }

    private void installEditorEnterAction() {
        InputMap inputMap = editorTextComponent.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = editorTextComponent.getActionMap();
        KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

        editorEnterActionKey = inputMap.get(enterKeyStroke);
        editorEnterAction = editorEnterActionKey != null ? actionMap.get(editorEnterActionKey) : null;

        inputMap.put(enterKeyStroke, TYPE_AHEAD_ENTER_ACTION);
        actionMap.put(TYPE_AHEAD_ENTER_ACTION, new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                if (typeAheadSelectionEnabled && commitTypeAheadSelection()) {
                    return;
                }

                if (editorEnterAction != null) {
                    editorEnterAction.actionPerformed(e);
                }
            }
        });
    }

    private void uninstallEditorEnterAction() {
        InputMap inputMap = editorTextComponent.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = editorTextComponent.getActionMap();
        KeyStroke enterKeyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

        if (editorEnterActionKey != null) {
            inputMap.put(enterKeyStroke, editorEnterActionKey);
        } else {
            inputMap.remove(enterKeyStroke);
        }

        actionMap.remove(TYPE_AHEAD_ENTER_ACTION);
        editorEnterActionKey = null;
        editorEnterAction = null;
    }

    private void updateEditorTypeAheadSelection() {
        if (!typeAheadSelectionEnabled || committingTypeAheadSelection || editorTextComponent == null) return;
        if (!editorTextComponent.hasFocus()) return;

        SwingUtilities.invokeLater(() -> {
            if (!typeAheadSelectionEnabled || committingTypeAheadSelection || editorTextComponent == null) return;
            updateTypeAheadSelection(editorTextComponent.getText());
        });
    }

    private void refreshTypeAheadSelection() {
        if (isEditable() && editorTextComponent != null) {
            updateTypeAheadSelection(editorTextComponent.getText());
        } else if (!typeAheadBuffer.isEmpty()) {
            updateTypeAheadSelection(typeAheadBuffer);
        }
    }

    private void updateBufferedTypeAheadSelection(char keyChar) {
        long now = System.currentTimeMillis();
        if (now - lastTypeAheadAt > TYPE_AHEAD_RESET_DELAY_MS) {
            typeAheadBuffer = "";
        }

        lastTypeAheadAt = now;
        typeAheadBuffer += keyChar;
        updateTypeAheadSelection(typeAheadBuffer);
    }

    private void updateTypeAheadSelection(String text) {
        if (text == null || text.trim().isEmpty()) {
            clearTypeAheadSelection(true);
            return;
        }

        int matchIndex = findTypeAheadMatch(text);
        if (matchIndex < 0) {
            clearTypeAheadSelection(true);
            return;
        }

        typeAheadPopupIndex = matchIndex;
        showTypeAheadPopup(matchIndex);
    }

    private void showTypeAheadPopup(int index) {
        if (isShowing() && !isPopupVisible()) {
            setPopupVisible(true);
        }

        highlightPopupIndex(index);
        SwingUtilities.invokeLater(() -> highlightPopupIndex(index));
    }

    private boolean handleTypeAheadNavigation(int keyCode) {
        if (typeAheadPopupIndex < 0) return false;

        return switch (keyCode) {
            case KeyEvent.VK_ENTER -> commitTypeAheadSelection();
            case KeyEvent.VK_UP -> moveTypeAheadSelection(-1);
            case KeyEvent.VK_DOWN -> moveTypeAheadSelection(1);
            case KeyEvent.VK_ESCAPE -> {
                clearTypeAheadSelection(true);
                yield true;
            }
            default -> false;
        };
    }

    private boolean commitTypeAheadSelection() {
        if (typeAheadPopupIndex < 0 || typeAheadPopupIndex >= getItemCount()) return false;

        committingTypeAheadSelection = true;
        try {
            setSelectedIndex(typeAheadPopupIndex);
            if (isPopupVisible()) {
                setPopupVisible(false);
            }
            clearTypeAheadSelection(false);
        } finally {
            committingTypeAheadSelection = false;
        }

        return true;
    }

    private boolean moveTypeAheadSelection(int offset) {
        if (getItemCount() == 0) return false;

        int nextIndex = typeAheadPopupIndex + offset;
        if (nextIndex < 0) {
            nextIndex = 0;
        } else if (nextIndex >= getItemCount()) {
            nextIndex = getItemCount() - 1;
        }

        typeAheadPopupIndex = nextIndex;
        showTypeAheadPopup(nextIndex);
        return true;
    }

    private void highlightPopupIndex(int index) {
        JList<Object> popupList = getPopupList();
        if (popupList == null || index < 0 || index >= popupList.getModel().getSize()) return;

        popupList.setSelectedIndex(index);
        popupList.ensureIndexIsVisible(index);
        popupList.repaint();
    }

    @SuppressWarnings("unchecked")
    private JList<Object> getPopupList() {
        Object child = getAccessibleContext().getAccessibleChild(0);
        if (child instanceof ComboPopup comboPopup) {
            return (JList<Object>) comboPopup.getList();
        }

        return null;
    }

    private void clearTypeAheadSelection(boolean closePopup) {
        typeAheadPopupIndex = -1;
        typeAheadBuffer = "";
        lastTypeAheadAt = 0L;

        if (closePopup && isPopupVisible()) {
            setPopupVisible(false);
        }
    }

    private int findTypeAheadMatch(String text) {
        String query = normalize(text);
        int containsMatch = -1;

        for (int i = 0; i < getItemCount(); i++) {
            String value = normalize(getDisplayText(getItemAt(i)));
            if (value.startsWith(query)) return i;
            if (containsMatch < 0 && value.contains(query)) {
                containsMatch = i;
            }
        }

        return containsMatch;
    }

    private int findFirstKeySelectionMatch(char keyChar, ComboBoxModel<?> model) {
        String query = normalize(String.valueOf(keyChar));
        for (int i = 0; i < model.getSize(); i++) {
            if (normalize(getDisplayText(model.getElementAt(i))).startsWith(query)) {
                return i;
            }
        }

        return -1;
    }

    private String getDisplayText(Object value) {
        try {
            return value != null ? displayFunction.apply(value) : "";
        } catch (Exception ex) {
            return "";
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isTypeAheadCharacter(KeyEvent e) {
        if (e.isAltDown() || e.isControlDown() || e.isMetaDown()) return false;

        char keyChar = e.getKeyChar();
        return keyChar != KeyEvent.CHAR_UNDEFINED
                && !Character.isISOControl(keyChar);
    }

    @Override
    public String toString(){
        return Objects.toString(getSelectedItem());
    }

}
