package dtm.stools.component.inputfields.textarea;

import dtm.stools.component.icon.VectorIcon;
import dtm.stools.component.icon.TintedIconLoader;
import dtm.stools.component.inputfields.textfield.MaskedTextField;
import dtm.stools.configs.UiTokens;
import dtm.stools.i18n.I18n;
import dtm.stools.utils.ColorUtils;
import dtm.stools.utils.PaintUtils;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class TextAreaSearchBar extends JPanel {

    private static final int MAX_HIGHLIGHTS = 5000;
    private static final int ICON_SIZE = 14;

    private final SmartTextArea area;
    private final MaskedTextField findField = new MaskedTextField(18);
    private final MaskedTextField replaceField = new MaskedTextField(18);
    private final JToggleButton expandToggle = new JToggleButton();
    private final JToggleButton caseToggle = new JToggleButton("Aa");
    private final JToggleButton wordToggle = new JToggleButton("ab");
    private final JToggleButton regexToggle = new JToggleButton(".*");
    private final JLabel countLabel = new JLabel();
    private final JButton previousButton = new JButton();
    private final JButton nextButton = new JButton();
    private final JButton closeButton = new JButton();
    private final JButton replaceButton = new JButton(text("search.replaceOne", "Substituir"));
    private final JButton replaceAllButton = new JButton(text("search.replaceAll", "Todos"));
    private final JPanel replaceRow = new JPanel();
    private final List<Object> highlightTags = new ArrayList<>();
    private final Timer refreshTimer = new Timer(150, e -> refresh(false));

    private List<TextAreaSearchMatch> matches = List.of();
    private Pattern pattern;
    private int current = -1;
    private Object currentTag;
    private boolean active;
    private boolean floating = true;
    private Document observedDocument;

    private final DocumentListener areaDocumentListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {
            refreshTimer.restart();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            refreshTimer.restart();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
        }
    };

    TextAreaSearchBar(SmartTextArea area) {
        this.area = area;
        refreshTimer.setRepeats(false);
        setOpaque(false);
        setVisible(false);
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(new EmptyBorder(UiTokens.scale(5), UiTokens.scale(6), UiTokens.scale(5), UiTokens.scale(6)));

        findField.setPlaceholder(text("search.find", "Buscar"));
        findField.setToolTipText(text("search.find", "Buscar"));
        replaceField.setPlaceholder(text("search.replace", "Substituir"));
        replaceField.setToolTipText(text("search.replace", "Substituir"));

        styleTool(expandToggle, text("search.toggleReplace", "Alternar substituição"));
        styleTool(caseToggle, text("search.matchCase", "Diferenciar maiúsculas/minúsculas"));
        styleTool(wordToggle, text("search.wholeWord", "Palavra inteira"));
        styleTool(regexToggle, text("search.regex", "Expressão regular"));
        styleTool(previousButton, text("search.previous", "Resultado anterior (Shift+F3)"));
        styleTool(nextButton, text("search.next", "Próximo resultado (F3)"));
        styleTool(closeButton, text("search.close", "Fechar (Esc)"));
        replaceButton.setToolTipText(text("search.replaceCurrentTooltip", "Substituir resultado atual"));
        replaceAllButton.setToolTipText(text("search.replaceAllTooltip", "Substituir todos os resultados"));
        replaceButton.setFocusable(false);
        replaceAllButton.setFocusable(false);
        countLabel.setBorder(new EmptyBorder(0, UiTokens.scale(6), 0, UiTokens.scale(4)));

        JPanel findRow = row();
        findRow.add(expandToggle);
        findRow.add(Box.createHorizontalStrut(UiTokens.scale(4)));
        findRow.add(findField);
        findRow.add(Box.createHorizontalStrut(UiTokens.scale(2)));
        findRow.add(caseToggle);
        findRow.add(wordToggle);
        findRow.add(regexToggle);
        findRow.add(countLabel);
        findRow.add(previousButton);
        findRow.add(nextButton);
        findRow.add(closeButton);

        replaceRow.setLayout(new BoxLayout(replaceRow, BoxLayout.X_AXIS));
        replaceRow.setOpaque(false);
        replaceRow.setAlignmentX(LEFT_ALIGNMENT);
        replaceRow.setBorder(new EmptyBorder(UiTokens.scale(4), 0, 0, 0));
        replaceRow.add(Box.createHorizontalStrut(expandToggle.getPreferredSize().width + UiTokens.scale(4)));
        replaceRow.add(replaceField);
        replaceRow.add(Box.createHorizontalStrut(UiTokens.scale(4)));
        replaceRow.add(replaceButton);
        replaceRow.add(Box.createHorizontalStrut(UiTokens.scale(2)));
        replaceRow.add(replaceAllButton);
        replaceRow.add(Box.createHorizontalGlue());
        replaceRow.setVisible(false);

        add(findRow);
        add(replaceRow);

        applyTheme();
        wire();
        area.addPropertyChangeListener("document", e -> documentSwapped());
    }

    void setFloating(boolean floating) {
        if (this.floating == floating) return;
        this.floating = floating;
        revalidate();
        repaint();
    }

    boolean isFloating() {
        return floating;
    }

    boolean isActive() {
        return active;
    }

    void open(boolean replace) {
        active = true;
        observe(area.getDocument());
        String selected = area.getSelectedText();
        if (selected != null && !selected.isEmpty() && selected.indexOf('\n') < 0) findField.setText(selected);
        setReplaceVisible(replace && area.isEditable());
        setVisible(true);
        relayoutHost();
        refresh(true);
        findField.requestFocusInWindow();
        findField.selectAll();
    }

    void close() {
        if (!active) return;
        active = false;
        refreshTimer.stop();
        observe(null);
        clearHighlights();
        matches = List.of();
        current = -1;
        setVisible(false);
        relayoutHost();
        area.requestFocusInWindow();
    }

    void findNext() {
        if (!ensureMatches()) return;
        int from = area.getSelectionStart() != area.getSelectionEnd() ? area.getSelectionStart() + 1 : area.getSelectionStart();
        select(TextAreaSearch.indexAtOrAfter(matches, from));
    }

    void findPrevious() {
        if (!ensureMatches()) return;
        select(TextAreaSearch.indexBefore(matches, area.getSelectionStart()));
    }

    void replaceCurrent() {
        if (!area.isEditable() || !ensureMatches()) return;
        if (current < 0) {
            findNext();
            return;
        }
        TextAreaSearchMatch match = matches.get(current);
        if (area.getSelectionStart() != match.start() || area.getSelectionEnd() != match.end()) {
            select(current);
            return;
        }
        String replacement = TextAreaSearch.replacementFor(pattern, area.getText(), match, replaceField.getText(), regexToggle.isSelected());
        area.beginCompoundEdit();
        try {
            Document document = area.getDocument();
            document.remove(match.start(), match.end() - match.start());
            document.insertString(match.start(), replacement, null);
        } catch (BadLocationException ex) {
            UIManager.getLookAndFeel().provideErrorFeedback(area);
        } finally {
            area.endCompoundEdit();
        }
        area.setCaretPosition(Math.min(match.start() + replacement.length(), area.getDocument().getLength()));
        refresh(false);
        if (!matches.isEmpty()) select(TextAreaSearch.indexAtOrAfter(matches, area.getCaretPosition()));
    }

    void replaceAll() {
        if (!area.isEditable()) return;
        refresh(false);
        if (matches.isEmpty()) return;
        String text = area.getText();
        String typed = replaceField.getText();
        boolean regex = regexToggle.isSelected();
        List<String> replacements = new ArrayList<>(matches.size());
        for (TextAreaSearchMatch match : matches) {
            replacements.add(TextAreaSearch.replacementFor(pattern, text, match, typed, regex));
        }
        area.beginCompoundEdit();
        try {
            Document document = area.getDocument();
            for (int i = matches.size() - 1; i >= 0; i--) {
                TextAreaSearchMatch match = matches.get(i);
                document.remove(match.start(), match.end() - match.start());
                document.insertString(match.start(), replacements.get(i), null);
            }
        } catch (BadLocationException ex) {
            UIManager.getLookAndFeel().provideErrorFeedback(area);
        } finally {
            area.endCompoundEdit();
        }
        refresh(false);
    }

    void setQuery(String query) {
        findField.setText(query == null ? "" : query);
    }

    void setReplaceText(String replacement) {
        replaceField.setText(replacement == null ? "" : replacement);
    }

    void setOptions(boolean caseSensitive, boolean wholeWord, boolean regex) {
        caseToggle.setSelected(caseSensitive);
        wordToggle.setSelected(wholeWord);
        regexToggle.setSelected(regex);
        if (active) refresh(false);
    }

    int getMatchCount() {
        return matches.size();
    }

    int getCurrentIndex() {
        return current;
    }

    void refresh(boolean reveal) {
        refreshTimer.stop();
        clearHighlights();
        if (!active) return;
        String query = findField.getText();
        pattern = TextAreaSearch.compile(query, caseToggle.isSelected(), wordToggle.isSelected(), regexToggle.isSelected());
        matches = TextAreaSearch.findAll(area.getText(), pattern);
        current = matches.isEmpty() ? -1 : TextAreaSearch.indexAtOrAfter(matches, area.getSelectionStart());
        paintMatches();
        if (reveal && current >= 0) select(current);
        else updateCount(!query.isEmpty() && pattern == null);
    }

    @Override
    public void updateUI() {
        super.updateUI();
        if (findField != null) applyTheme();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = PaintUtils.antialias((Graphics2D) g.create());
        try {
            if (!floating) {
                g2.setColor(UiTokens.surface());
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(UiTokens.border());
                g2.fillRect(0, getHeight() - 1, getWidth(), 1);
                return;
            }
            Rectangle bounds = new Rectangle(0, 0, getWidth() - 1, getHeight() - 1);
            int arc = UiTokens.radius(UiTokens.Radius.MD);
            PaintUtils.fillRoundRect(g2, bounds, arc, UiTokens.surface());
            PaintUtils.drawRoundRect(g2, bounds, arc, UiTokens.border(), 1f);
        } finally {
            g2.dispose();
        }
    }

    private boolean ensureMatches() {
        if (!active) return false;
        if (refreshTimer.isRunning() || matches.isEmpty()) refresh(false);
        if (matches.isEmpty()) {
            UIManager.getLookAndFeel().provideErrorFeedback(area);
            return false;
        }
        return true;
    }

    private void select(int index) {
        if (index < 0 || index >= matches.size()) return;
        current = index;
        TextAreaSearchMatch match = matches.get(index);
        area.setCaretPosition(match.start());
        area.moveCaretPosition(match.end());
        try {
            Rectangle2D start = area.modelToView2D(match.start());
            Rectangle2D end = area.modelToView2D(match.end());
            if (start != null && end != null) {
                Rectangle visible = start.getBounds().union(end.getBounds());
                visible.grow(UiTokens.scale(24), UiTokens.scale(24) + getHeight());
                area.scrollRectToVisible(visible);
            }
        } catch (BadLocationException ignored) {
        }
        paintCurrent();
        updateCount(false);
    }

    private void paintMatches() {
        Highlighter highlighter = area.getHighlighter();
        if (highlighter == null) return;
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(ColorUtils.withAlpha(UiTokens.warning(), 0.35f));
        int limit = Math.min(matches.size(), MAX_HIGHLIGHTS);
        for (int i = 0; i < limit; i++) {
            TextAreaSearchMatch match = matches.get(i);
            try {
                highlightTags.add(highlighter.addHighlight(match.start(), match.end(), painter));
            } catch (BadLocationException ignored) {
            }
        }
        paintCurrent();
    }

    private void paintCurrent() {
        Highlighter highlighter = area.getHighlighter();
        if (highlighter == null) return;
        if (currentTag != null) {
            highlighter.removeHighlight(currentTag);
            currentTag = null;
        }
        if (current < 0 || current >= matches.size()) return;
        TextAreaSearchMatch match = matches.get(current);
        Highlighter.HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(ColorUtils.withAlpha(UiTokens.primary(), 0.45f));
        try {
            currentTag = highlighter.addHighlight(match.start(), match.end(), painter);
        } catch (BadLocationException ignored) {
        }
    }

    private void clearHighlights() {
        Highlighter highlighter = area.getHighlighter();
        if (highlighter != null) {
            for (Object tag : highlightTags) highlighter.removeHighlight(tag);
            if (currentTag != null) highlighter.removeHighlight(currentTag);
        }
        highlightTags.clear();
        currentTag = null;
    }

    private void updateCount(boolean invalidPattern) {
        if (invalidPattern) {
            countLabel.setText(text("search.invalidRegex", "Regex inválida"));
            countLabel.setForeground(UiTokens.danger());
            return;
        }
        countLabel.setForeground(UiTokens.muted());
        if (findField.getText().isEmpty()) {
            countLabel.setText(" ");
        } else if (matches.isEmpty()) {
            countLabel.setText(text("search.noResults", "Nenhum resultado"));
        } else if (current < 0) {
            countLabel.setText(text("search.total", "{total} resultados").replace("{total}", String.valueOf(matches.size())));
        } else {
            countLabel.setText(text("search.count", "{current} de {total}")
                    .replace("{current}", String.valueOf(current + 1))
                    .replace("{total}", String.valueOf(matches.size())));
        }
    }

    private void setReplaceVisible(boolean visible) {
        replaceRow.setVisible(visible);
        expandToggle.setSelected(visible);
        reloadIcons();
        relayoutHost();
    }

    private void documentSwapped() {
        if (!active) return;
        observe(area.getDocument());
        refresh(false);
    }

    private void observe(Document document) {
        if (observedDocument != null) observedDocument.removeDocumentListener(areaDocumentListener);
        observedDocument = document;
        if (observedDocument != null) observedDocument.addDocumentListener(areaDocumentListener);
    }

    private void relayoutHost() {
        revalidate();
        Container host = getParent();
        if (host != null) {
            host.revalidate();
            host.repaint();
        }
    }

    private void wire() {
        findField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                refresh(true);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                refresh(true);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        });
        caseToggle.addActionListener(e -> refresh(true));
        wordToggle.addActionListener(e -> refresh(true));
        regexToggle.addActionListener(e -> refresh(true));
        previousButton.addActionListener(e -> findPrevious());
        nextButton.addActionListener(e -> findNext());
        closeButton.addActionListener(e -> close());
        expandToggle.addActionListener(e -> setReplaceVisible(expandToggle.isSelected() && area.isEditable()));
        replaceButton.addActionListener(e -> replaceCurrent());
        replaceAllButton.addActionListener(e -> replaceAll());
        findField.addActionListener(e -> findNext());
        replaceField.addActionListener(e -> replaceCurrent());

        findField.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK), "smartTextArea.search.previous");
        findField.getActionMap().put("smartTextArea.search.previous", new TextAreaAction(this::findPrevious, () -> active));

        InputMap input = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "smartTextArea.search.close");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "smartTextArea.search.next");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK), "smartTextArea.search.previous");
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, SmartTextArea.menuShortcutMask()), "smartTextArea.search.toggleReplace");
        getActionMap().put("smartTextArea.search.close", new TextAreaAction(this::close, () -> active));
        getActionMap().put("smartTextArea.search.next", new TextAreaAction(this::findNext, () -> active));
        getActionMap().put("smartTextArea.search.previous", new TextAreaAction(this::findPrevious, () -> active));
        getActionMap().put("smartTextArea.search.toggleReplace",
                new TextAreaAction(() -> setReplaceVisible(!replaceRow.isVisible()), () -> active && area.isEditable()));
    }

    private void applyTheme() {
        Font small = UiTokens.fontSmall();
        countLabel.setFont(small);
        countLabel.setForeground(UiTokens.muted());
        for (AbstractButton toggle : List.of(caseToggle, wordToggle, regexToggle)) {
            toggle.setFont(small.deriveFont(Font.BOLD));
        }
        replaceButton.setFont(small);
        replaceAllButton.setFont(small);
        findField.setPlaceholderColor(UiTokens.muted());
        replaceField.setPlaceholderColor(UiTokens.muted());
        reloadIcons();
    }

    private void reloadIcons() {
        Color tint = UiTokens.foreground();
        icon(previousButton, "/drawables/arrowUpNoTail.png", VectorIcon.Kind.UP, tint);
        icon(nextButton, "/drawables/arrowDownNoTail.png", VectorIcon.Kind.DOWN, tint);
        icon(closeButton, "/drawables/close.png", VectorIcon.Kind.CLOSE, tint);
        boolean expanded = replaceRow.isVisible();
        icon(expandToggle, expanded ? "/drawables/arrowDownNoTail.png" : "/drawables/arrowRightNoTail.png",
                expanded ? VectorIcon.Kind.DOWN : VectorIcon.Kind.RIGHT, tint);
        expandToggle.setEnabled(area.isEditable());
    }

    private JPanel row() {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    private static void icon(AbstractButton button, String resource, VectorIcon.Kind fallback, Color tint) {
        Icon icon = TintedIconLoader.load(resource, UiTokens.scale(ICON_SIZE), tint);
        button.setIcon(icon != null ? icon : new VectorIcon(fallback, UiTokens.scale(ICON_SIZE), tint));
        button.setText(null);
    }

    private static void styleTool(AbstractButton button, String tooltip) {
        button.setToolTipText(tooltip);
        button.setFocusable(false);
        button.setFocusPainted(false);
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setMargin(new Insets(2, 5, 2, 5));
    }

    private static String text(String key, String fallback) {
        return I18n.getText(SmartTextArea.class, key, fallback);
    }
}
