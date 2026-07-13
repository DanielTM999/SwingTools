package dtm.stools.component.panels.editor.code.search;

import dtm.stools.component.icon.TintedIconLoader;
import dtm.stools.component.inputfields.textfield.MaskedTextField;
import dtm.stools.component.panels.editor.code.CodeEditorTextArea;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SearchPanel extends JPanel {

    @Getter
    protected final CodeEditorTextArea editor;

    @Getter
    protected final MaskedTextField findField = new MaskedTextField(24);

    @Getter
    protected final MaskedTextField replaceField = new MaskedTextField(24);

    @Getter
    protected final JToggleButton caseToggle = new JToggleButton("Aa");

    @Getter
    protected final JToggleButton wordToggle = new JToggleButton("W");

    @Getter
    protected final JToggleButton regexToggle = new JToggleButton(".*");

    @Getter
    protected final JLabel countLabel = new JLabel("0 results");

    @Getter
    protected final JButton prevButton = new JButton();

    @Getter
    protected final JButton nextButton = new JButton();

    @Getter
    protected final JButton replaceButton = new JButton("Replace");

    @Getter
    protected final JButton replaceAllButton = new JButton("All");

    @Getter
    protected final JButton closeButton = new JButton();

    @Getter
    protected final JToggleButton expandToggle = new JToggleButton();

    @Getter
    protected boolean replaceVisible = false;

    protected final JPanel replaceRow;

    @Getter
    protected String prevIconResource = "/drawables/arrowUpNoTail.png";
    @Getter
    protected String nextIconResource = "/drawables/arrowDownNoTail.png";
    @Getter
    protected String closeIconResource = "/drawables/close.png";
    @Getter
    protected String expandCollapsedIconResource = "/drawables/arrowRightNoTail.png";
    @Getter
    protected String expandExpandedIconResource = "/drawables/arrowDownNoTail.png";

    @Getter
    protected int iconSize = 14;

    public SearchPanel(CodeEditorTextArea editor) {
        this.editor = editor;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(true);
        setBackground(resolvePanelBackground());
        setBorder(new CompoundBorder(
                new BottomLineBorder(resolveBorderColor(), 1),
                new EmptyBorder(6, 8, 6, 8)
        ));

        styleField(findField, "Find");
        styleField(replaceField, "Replace");

        styleToggle(expandToggle, "Toggle replace");
        styleToggle(caseToggle, "Match case");
        styleToggle(wordToggle, "Whole word");
        styleToggle(regexToggle, "Regex");
        styleIconButton(prevButton, "Previous match");
        styleIconButton(nextButton, "Next match");
        styleIconButton(closeButton, "Close (Esc)");
        styleActionButton(replaceButton, "Replace current match");
        styleActionButton(replaceAllButton, "Replace all matches");
        reloadIcons();

        countLabel.setBorder(new EmptyBorder(0, 8, 0, 8));
        countLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        countLabel.setFont(countLabel.getFont().deriveFont(Font.PLAIN, 11f));

        JPanel findRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        findRow.setOpaque(false);
        findRow.add(expandToggle);
        findRow.add(findField);
        findRow.add(Box.createHorizontalStrut(2));
        findRow.add(caseToggle);
        findRow.add(wordToggle);
        findRow.add(regexToggle);
        findRow.add(Box.createHorizontalStrut(2));
        findRow.add(prevButton);
        findRow.add(nextButton);
        findRow.add(countLabel);
        findRow.add(Box.createHorizontalGlue());
        findRow.add(closeButton);

        replaceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        replaceRow.setOpaque(false);
        replaceRow.setBorder(new EmptyBorder(4, 0, 0, 0));
        int indent = expandToggle.getPreferredSize().width;
        replaceRow.add(Box.createHorizontalStrut(indent));
        replaceRow.add(replaceField);
        replaceRow.add(Box.createHorizontalStrut(2));
        replaceRow.add(replaceButton);
        replaceRow.add(replaceAllButton);

        add(findRow);
        add(replaceRow);

        replaceRow.setVisible(replaceVisible);
        expandToggle.setSelected(replaceVisible);
        updateExpandIcon();

        wireActions();
    }

    protected void styleField(JTextField field, String placeholder) {
        field.setToolTipText(placeholder);
        if (field instanceof MaskedTextField maskedTextField) {
            maskedTextField.setPlaceholder(placeholder);
            Color placeholderColor = UIManager.getColor("Label.disabledForeground");
            if (placeholderColor != null) {
                maskedTextField.setPlaceholderColor(placeholderColor);
            }
        }
        Color border = resolveBorderColor();
        Color focusBorder = resolveAccentColor();
        Border base = new RoundedLineBorder(border, 1, 8);
        Border padded = new CompoundBorder(base, new EmptyBorder(3, 8, 3, 8));
        field.setBorder(padded);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(new CompoundBorder(
                        new RoundedLineBorder(focusBorder, 1, 8),
                        new EmptyBorder(3, 8, 3, 8)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(padded);
            }
        });
    }

    protected void styleToggle(AbstractButton b, String tooltip) {
        b.setToolTipText(tooltip);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setMargin(new Insets(2, 6, 2, 6));
        b.setFont(b.getFont().deriveFont(Font.BOLD, 11f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installHoverChrome(b, true);
    }

    protected void styleIconButton(AbstractButton b, String tooltip) {
        b.setToolTipText(tooltip);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setMargin(new Insets(2, 6, 2, 6));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        installHoverChrome(b, false);
    }

    protected void styleActionButton(AbstractButton b, String tooltip) {
        b.setToolTipText(tooltip);
        b.setFocusPainted(false);
        b.setMargin(new Insets(3, 10, 3, 10));
        b.setFont(b.getFont().deriveFont(Font.PLAIN, 11f));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    protected void installHoverChrome(AbstractButton b, boolean toggle) {
        Color hover = resolveHoverColor();
        Color selected = resolveSelectedColor();
        Runnable refresh = () -> {
            boolean isSelected = toggle && b.isSelected();
            if (isSelected) {
                b.setContentAreaFilled(true);
                b.setOpaque(true);
                b.setBackground(selected);
            } else if (Boolean.TRUE.equals(b.getClientProperty("search.hovered"))) {
                b.setContentAreaFilled(true);
                b.setOpaque(true);
                b.setBackground(hover);
            } else {
                b.setContentAreaFilled(false);
                b.setOpaque(false);
            }
            b.repaint();
        };
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.putClientProperty("search.hovered", Boolean.TRUE);
                refresh.run();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                b.putClientProperty("search.hovered", Boolean.FALSE);
                refresh.run();
            }
        });
        b.addChangeListener(e -> refresh.run());
    }

    protected Color resolvePanelBackground() {
        Color base = UIManager.getColor("Panel.background");
        if (base == null) base = getBackground();
        if (base == null) base = new Color(245, 245, 245);
        return shift(base, isDarkTheme() ? 6 : -4);
    }

    protected Color resolveBorderColor() {
        Color c = UIManager.getColor("Component.borderColor");
        if (c == null) c = UIManager.getColor("Separator.foreground");
        if (c == null) c = isDarkTheme() ? new Color(80, 80, 80) : new Color(200, 200, 200);
        return c;
    }

    protected Color resolveAccentColor() {
        Color c = UIManager.getColor("Component.focusColor");
        if (c == null) c = UIManager.getColor("Focus.color");
        if (c == null) c = UIManager.getColor("Component.focusedBorderColor");
        if (c == null) c = new Color(74, 144, 226);
        return c;
    }

    protected Color resolveHoverColor() {
        Color base = UIManager.getColor("Panel.background");
        if (base == null) base = getBackground();
        if (base == null) base = new Color(245, 245, 245);
        return shift(base, isDarkTheme() ? 18 : -18);
    }

    protected Color resolveSelectedColor() {
        Color accent = resolveAccentColor();
        return new Color(accent.getRed(), accent.getGreen(), accent.getBlue(),
                isDarkTheme() ? 90 : 60);
    }

    protected boolean isDarkTheme() {
        Color bg = UIManager.getColor("Panel.background");
        if (bg == null) return false;
        int luma = (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000;
        return luma < 128;
    }

    protected Color shift(Color base, int amount) {
        int r = clamp(base.getRed() + amount);
        int g = clamp(base.getGreen() + amount);
        int b = clamp(base.getBlue() + amount);
        return new Color(r, g, b);
    }

    protected int clamp(int v) {
        return Math.max(0, Math.min(255, v));
    }

    protected static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int arc;

        public RoundedLineBorder(Color color, int thickness, int arc) {
            this.color = color;
            this.thickness = thickness;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(thickness));
                g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.left = insets.right = insets.top = insets.bottom = thickness;
            return insets;
        }
    }

    protected static class BottomLineBorder extends AbstractBorder {
        private final Color color;
        private final int thickness;

        public BottomLineBorder(Color color, int thickness) {
            this.color = color;
            this.thickness = thickness;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(color);
            g.fillRect(x, y + height - thickness, width, thickness);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, thickness, 0);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = insets.left = insets.right = 0;
            insets.bottom = thickness;
            return insets;
        }
    }

    protected void wireActions() {
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { onQueryChanged(); }
            public void removeUpdate(DocumentEvent e) { onQueryChanged(); }
            public void changedUpdate(DocumentEvent e) { onQueryChanged(); }
        };
        findField.getDocument().addDocumentListener(dl);
        caseToggle.addActionListener(e -> onQueryChanged());
        wordToggle.addActionListener(e -> onQueryChanged());
        regexToggle.addActionListener(e -> onQueryChanged());

        nextButton.addActionListener(e -> editor.searchFindNext());
        prevButton.addActionListener(e -> editor.searchFindPrev());
        replaceButton.addActionListener(e -> editor.searchReplaceCurrent(replaceField.getText()));
        replaceAllButton.addActionListener(e -> editor.searchReplaceAll(replaceField.getText()));
        closeButton.addActionListener(e -> editor.hideSearchPanel());

        expandToggle.addActionListener(e -> setReplaceVisible(expandToggle.isSelected()));

        findField.addActionListener(e -> editor.searchFindNext());

        InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "search.close");
        am.put("search.close", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { editor.hideSearchPanel(); }
        });
    }

    protected void onQueryChanged() {
        SearchOptions opts = getOptions();
        editor.searchUpdateQuery(findField.getText(), opts);
    }

    public SearchOptions getOptions() {
        return new SearchOptions(caseToggle.isSelected(), wordToggle.isSelected(),
                regexToggle.isSelected(), true);
    }

    public void setReplaceVisible(boolean visible) {
        this.replaceVisible = visible;
        replaceRow.setVisible(visible);
        expandToggle.setSelected(visible);
        updateExpandIcon();
        revalidate();
        repaint();
    }

    protected void updateExpandIcon() {
        Color tint = resolveIconColor();
        Icon icon = TintedIconLoader.load(
                replaceVisible ? expandExpandedIconResource : expandCollapsedIconResource,
                iconSize, tint);
        if (icon != null) {
            expandToggle.setIcon(icon);
            expandToggle.setText(null);
        } else {
            expandToggle.setIcon(null);
            expandToggle.setText(replaceVisible ? "\u25BC" : "\u25B6");
        }
    }

    protected Color resolveIconColor() {
        Color c = UIManager.getColor("Button.foreground");
        if (c == null) c = UIManager.getColor("Label.foreground");
        if (c == null) c = getForeground();
        return c;
    }

    public void reloadIcons() {
        Color tint = resolveIconColor();
        Icon prev = TintedIconLoader.load(prevIconResource, iconSize, tint);
        Icon next = TintedIconLoader.load(nextIconResource, iconSize, tint);
        Icon close = TintedIconLoader.load(closeIconResource, iconSize, tint);
        if (prev != null) { prevButton.setIcon(prev); prevButton.setText(null); }
        else { prevButton.setIcon(null); prevButton.setText("\u25B2"); }
        if (next != null) { nextButton.setIcon(next); nextButton.setText(null); }
        else { nextButton.setIcon(null); nextButton.setText("\u25BC"); }
        if (close != null) { closeButton.setIcon(close); closeButton.setText(null); }
        else { closeButton.setIcon(null); closeButton.setText("\u2715"); }
        updateExpandIcon();
    }

    public void setPrevIconResource(String resource) {
        this.prevIconResource = resource;
        reloadIcons();
    }

    public void setNextIconResource(String resource) {
        this.nextIconResource = resource;
        reloadIcons();
    }

    public void setCloseIconResource(String resource) {
        this.closeIconResource = resource;
        reloadIcons();
    }

    public void setExpandCollapsedIconResource(String resource) {
        this.expandCollapsedIconResource = resource;
        updateExpandIcon();
    }

    public void setExpandExpandedIconResource(String resource) {
        this.expandExpandedIconResource = resource;
        updateExpandIcon();
    }

    public void setIconSize(int size) {
        this.iconSize = Math.max(8, size);
        reloadIcons();
    }

    public void focusFindField() {
        findField.requestFocusInWindow();
        findField.selectAll();
    }

    public void setQuery(String q) {
        findField.setText(q == null ? "" : q);
    }

    public void updateMatchCount(int current, int total) {
        if (total <= 0) {
            countLabel.setText("No results");
        } else {
            countLabel.setText((current + 1) + " of " + total);
        }
    }
}
