package dtm.stools.component.inputfields.tagfield;

import dtm.stools.component.events.EventType;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.i18n.I18n;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class TagInputField extends PanelEventListener {

    private static String text(String key, String defaultValue) {
        return I18n.getText(TagInputField.class, key, defaultValue);
    }

    public static final String TAG_ADD = "tagAdd";
    public static final String TAG_REMOVE = "tagRemove";
    public static final String TAG_CLICK = "tagClick";

    private final List<String> tags = new ArrayList<>();
    private final JPanel tagPanel;
    private final JTextField inputField;
    private final JButton addButton;

    private TagRenderer tagRenderer = this::createDefaultTagComponent;
    private Predicate<String> tagValidator = tag -> true;
    private UnaryOperator<String> tagNormalizer = UnaryOperator.identity();

    private boolean allowDuplicates;
    private boolean caseSensitiveDuplicates;
    private boolean commitOnFocusLost = true;
    private boolean addButtonVisible = true;
    private boolean removeButtonVisible = true;
    private int maxTags = -1;
    private String placeholder = text("placeholder.addTag", "Adicionar tag");
    private String separatorsRegex = "[,;\\n]";

    private Color tagBackground = new Color(0xE8F0FE);
    private Color tagForeground = new Color(0x1D4ED8);
    private Color tagBorderColor = new Color(0xBFDBFE);
    private Color tagRemoveForeground = new Color(0x1D4ED8);

    private boolean internalTextUpdate;

    public TagInputField() {
        this(0);
    }

    public TagInputField(int columns) {
        super(new BorderLayout(4, 4), true);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("TextField.inactiveForeground"), 1),
                new EmptyBorder(4, 4, 4, 4)
        ));
        setBackground(UIManager.getColor("TextField.background"));

        tagPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        tagPanel.setOpaque(false);

        inputField = createInputField(columns);
        inputField.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        inputField.setOpaque(false);

        addButton = createAddButton();

        JPanel inputRow = new JPanel(new BorderLayout(4, 0));
        inputRow.setOpaque(false);
        inputRow.add(inputField, BorderLayout.CENTER);
        inputRow.add(addButton, BorderLayout.EAST);

        add(tagPanel, BorderLayout.CENTER);
        add(inputRow, BorderLayout.SOUTH);

        installListeners();
        refreshAddButtonVisibility();
    }

    protected JTextField createInputField(int columns) {
        return new PlaceholderTextField(columns);
    }

    protected JButton createAddButton() {
        JButton button = new JButton("+");
        button.setFocusable(false);
        button.setMargin(new Insets(2, 8, 2, 8));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(text("tooltip.addTag", "Adicionar tag"));
        return button;
    }

    protected JComponent createDefaultTagComponent(TagInputField field, String tag, int index, boolean removable) {
        JPanel chip = new JPanel(new BorderLayout(4, 0));
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(2, 0, 2, 0));
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel label = new JLabel(tag);
        label.setForeground(tagForeground);
        label.setBorder(new EmptyBorder(3, 8, 3, removable ? 0 : 8));
        chip.add(label, BorderLayout.CENTER);

        if (removable) {
            JButton remove = new JButton("x");
            remove.setFocusable(false);
            remove.setBorderPainted(false);
            remove.setContentAreaFilled(false);
            remove.setForeground(tagRemoveForeground);
            remove.setMargin(new Insets(0, 4, 0, 6));
            remove.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            remove.setToolTipText(text("tooltip.removeTag", "Remover tag"));
            remove.addActionListener(e -> removeTagAt(index));
            chip.add(remove, BorderLayout.EAST);
        }

        chip.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                dispatchTagEvent(TAG_CLICK, tag, index);
            }
        });

        return new RoundedChip(chip);
    }

    private void installListeners() {
        addButton.addActionListener(e -> {
            commitInput();
            inputField.requestFocusInWindow();
        });

        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_TAB) {
                    commitInput();
                    dispatchEvent(EventType.SUBMIT, getTags());
                    e.consume();
                } else if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && inputField.getText().isEmpty()) {
                    removeLastTag();
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (c == ',' || c == ';' || c == '\n') {
                    commitInput();
                    e.consume();
                }
            }
        });

        inputField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                if (commitOnFocusLost) {
                    commitInput();
                }
            }
        });

        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { dispatchInput(); }
            @Override public void removeUpdate(DocumentEvent e) { dispatchInput(); }
            @Override public void changedUpdate(DocumentEvent e) { dispatchInput(); }
        });
    }

    public boolean addTag(String tag) {
        return addTag(tag, true);
    }

    public boolean addTag(String tag, boolean fireEvent) {
        String normalized = normalizeTag(tag);
        if (!canAddTag(normalized)) return false;

        tags.add(normalized);
        refreshTags();

        int index = tags.size() - 1;
        if (fireEvent) {
            dispatchTagEvent(TAG_ADD, normalized, index);
            dispatchChange(normalized, index);
        }
        return true;
    }

    public int addTags(Collection<String> values) {
        if (values == null || values.isEmpty()) return 0;

        int added = 0;
        for (String value : values) {
            if (addTag(value, false)) {
                added++;
            }
        }

        if (added > 0) {
            refreshTags();
            dispatchChange(null, -1);
        }
        return added;
    }

    public boolean removeTag(String tag) {
        int index = indexOfTag(tag);
        return index >= 0 && removeTagAt(index);
    }

    public boolean removeTagAt(int index) {
        if (index < 0 || index >= tags.size()) return false;

        String removed = tags.remove(index);
        refreshTags();
        dispatchTagEvent(TAG_REMOVE, removed, index);
        dispatchChange(removed, index);
        return true;
    }

    public boolean removeLastTag() {
        return removeTagAt(tags.size() - 1);
    }

    public void clearTags() {
        if (tags.isEmpty()) return;

        tags.clear();
        refreshTags();
        dispatchEvent(EventType.CLEAR, getTags());
        dispatchChange(null, -1);
    }

    public void setTags(Collection<String> values) {
        tags.clear();
        if (values != null) {
            Set<String> uniqueValues = new LinkedHashSet<>(values);
            Collection<String> source = allowDuplicates ? values : uniqueValues;
            for (String value : source) {
                String normalized = normalizeTag(value);
                if (canAddTag(normalized)) {
                    tags.add(normalized);
                }
            }
        }
        refreshTags();
        dispatchChange(null, -1);
    }

    public List<String> getTags() {
        return List.copyOf(tags);
    }

    public String getText() {
        return inputField.getText();
    }

    public void setText(String text) {
        internalTextUpdate = true;
        inputField.setText(text == null ? "" : text);
        internalTextUpdate = false;
        dispatchInput();
    }

    public JTextField getInputField() {
        return inputField;
    }

    public JPanel getTagPanel() {
        return tagPanel;
    }

    public JButton getAddButton() {
        return addButton;
    }

    public TagInputField setPlaceholder(String placeholder) {
        this.placeholder = Objects.requireNonNullElse(placeholder, "");
        inputField.repaint();
        return this;
    }

    public TagInputField setTagRenderer(TagRenderer tagRenderer) {
        if (tagRenderer != null) {
            this.tagRenderer = tagRenderer;
            refreshTags();
        }
        return this;
    }

    public TagInputField setTagValidator(Predicate<String> tagValidator) {
        this.tagValidator = tagValidator == null ? tag -> true : tagValidator;
        return this;
    }

    public TagInputField setTagNormalizer(UnaryOperator<String> tagNormalizer) {
        this.tagNormalizer = tagNormalizer == null ? UnaryOperator.identity() : tagNormalizer;
        return this;
    }

    public TagInputField setAllowDuplicates(boolean allowDuplicates) {
        this.allowDuplicates = allowDuplicates;
        return this;
    }

    public TagInputField setCaseSensitiveDuplicates(boolean caseSensitiveDuplicates) {
        this.caseSensitiveDuplicates = caseSensitiveDuplicates;
        return this;
    }

    public TagInputField setCommitOnFocusLost(boolean commitOnFocusLost) {
        this.commitOnFocusLost = commitOnFocusLost;
        return this;
    }

    public TagInputField setAddButtonVisible(boolean visible) {
        this.addButtonVisible = visible;
        refreshAddButtonVisibility();
        return this;
    }

    public TagInputField setRemoveButtonVisible(boolean visible) {
        this.removeButtonVisible = visible;
        refreshTags();
        return this;
    }

    public TagInputField setMaxTags(int maxTags) {
        this.maxTags = maxTags;
        refreshAddButtonVisibility();
        return this;
    }

    public TagInputField setSeparatorsRegex(String separatorsRegex) {
        this.separatorsRegex = (separatorsRegex == null || separatorsRegex.isBlank()) ? "[,;\\n]" : separatorsRegex;
        return this;
    }

    public TagInputField setTagColors(Color background, Color foreground, Color border) {
        if (background != null) this.tagBackground = background;
        if (foreground != null) this.tagForeground = foreground;
        if (border != null) this.tagBorderColor = border;
        refreshTags();
        return this;
    }

    public TagInputField setTagRemoveForeground(Color color) {
        if (color != null) {
            this.tagRemoveForeground = color;
            refreshTags();
        }
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        inputField.setEnabled(enabled);
        addButton.setEnabled(enabled);
        for (Component component : tagPanel.getComponents()) {
            component.setEnabled(enabled);
        }
    }

    protected void commitInput() {
        List<String> parsed = parseTags(inputField.getText());
        if (parsed.isEmpty()) return;

        int added = addTags(parsed);
        if (added > 0) {
            internalTextUpdate = true;
            inputField.setText("");
            internalTextUpdate = false;
            dispatchInput();
        }
    }

    protected List<String> parseTags(String text) {
        if (text == null || text.isBlank()) return List.of();

        List<String> parsed = new ArrayList<>();
        for (String part : text.split(separatorsRegex)) {
            String normalized = normalizeTag(part);
            if (!normalized.isBlank()) {
                parsed.add(normalized);
            }
        }
        return parsed;
    }

    protected String normalizeTag(String tag) {
        String value = tag == null ? "" : tag.trim();
        value = tagNormalizer.apply(value);
        return value == null ? "" : value.trim();
    }

    protected boolean canAddTag(String tag) {
        if (tag == null || tag.isBlank()) return false;
        if (maxTags >= 0 && tags.size() >= maxTags) return false;
        if (!allowDuplicates && containsTag(tag)) return false;
        return tagValidator.test(tag);
    }

    protected boolean containsTag(String tag) {
        return indexOfTag(tag) >= 0;
    }

    protected int indexOfTag(String tag) {
        String normalized = normalizeTag(tag);
        for (int i = 0; i < tags.size(); i++) {
            String current = tags.get(i);
            if (caseSensitiveDuplicates ? current.equals(normalized) : current.equalsIgnoreCase(normalized)) {
                return i;
            }
        }
        return -1;
    }

    protected void refreshTags() {
        tagPanel.removeAll();
        for (int i = 0; i < tags.size(); i++) {
            tagPanel.add(tagRenderer.render(this, tags.get(i), i, removeButtonVisible && isEnabled()));
        }
        refreshAddButtonVisibility();
        tagPanel.revalidate();
        tagPanel.repaint();
    }

    protected void refreshAddButtonVisibility() {
        boolean canAddMore = maxTags < 0 || tags.size() < maxTags;
        addButton.setVisible(addButtonVisible && canAddMore);
        inputField.setEnabled(isEnabled() && canAddMore);
        revalidate();
        repaint();
    }

    protected void dispatchInput() {
        if (internalTextUpdate) return;
        dispatchEvent(EventType.INPUT, inputField, inputField.getText(), Map.of("tags", getTags()));
    }

    protected void dispatchChange(String tag, int index) {
        Map<String, Object> props = new HashMap<>();
        props.put("tag", tag);
        props.put("index", index);
        dispatchEvent(EventType.CHANGE, this, getTags(), props);
    }

    protected void dispatchTagEvent(String eventType, String tag, int index) {
        dispatchEvent(eventType, this, tag, Map.of("tag", tag, "index", index, "tags", getTags()));
    }

    public interface TagRenderer {
        JComponent render(TagInputField field, String tag, int index, boolean removable);
    }

    private class PlaceholderTextField extends JTextField {
        PlaceholderTextField(int columns) {
            super(columns);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty() || placeholder.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(UIManager.getColor("TextField.inactiveForeground"));
                FontMetrics fm = g2.getFontMetrics();
                int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(placeholder, getInsets().left + 2, y);
            } finally {
                g2.dispose();
            }
        }
    }

    private class RoundedChip extends JComponent {
        private final JComponent content;

        RoundedChip(JComponent content) {
            this.content = content;
            setLayout(new BorderLayout());
            setOpaque(false);
            add(content, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(tagBackground);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(tagBorderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }
}
