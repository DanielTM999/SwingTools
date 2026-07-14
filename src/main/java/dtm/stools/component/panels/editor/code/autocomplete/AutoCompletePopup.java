package dtm.stools.component.panels.editor.code.autocomplete;

import dtm.stools.i18n.I18n;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

public class AutoCompletePopup {

    private static final String COMPLETIONS_CARD = "completions";
    private static final String LOADING_CARD = "loading";

    private static String text(String key, String defaultValue) {
        return I18n.getText(AutoCompletePopup.class, key, defaultValue);
    }

    protected final JComponent owner;
    protected final JPopupMenu popup = new JPopupMenu();
    protected final DefaultListModel<AutoCompleteItem> model = new DefaultListModel<>();
    protected final JList<AutoCompleteItem> list = new JList<>(model);
    protected final JScrollPane scroll;
    protected final JPanel centerPanel = new JPanel(new CardLayout());
    protected final JPanel loadingPanel = new JPanel(new GridBagLayout());
    protected final JLabel detailLabel = new JLabel();
    protected final JLabel loadingLabel = new JLabel(text("loading", "Buscando sugestoes..."));
    protected final LoadingSpinner loadingSpinner = new LoadingSpinner();
    protected Runnable acceptHandler;
    protected boolean loading;

    @Getter
    protected int triggerOffset;

    @Getter
    protected String triggerPrefix = "";

    @Getter
    protected Dimension popupSize = new Dimension(360, 180);

    public AutoCompletePopup(JComponent owner) {
        this.owner = owner;
        Color popupBackground = resolveColor("PopupMenu.background", "ToolTip.background", new Color(0xFFFFFF));
        Color popupForeground = resolveColor("PopupMenu.foreground", "ToolTip.foreground", new Color(0x111827));

        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(createCellRenderer());
        list.setFocusable(false);
        list.setOpaque(true);
        list.setBackground(popupBackground);
        list.setFixedCellHeight(38);
        list.setVisibleRowCount(6);

        scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(true);
        scroll.getViewport().setBackground(popupBackground);
        scroll.setPreferredSize(popupSize);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        loadingSpinner.setForeground(blend(popupForeground, popupBackground, 0.20f));
        loadingLabel.setForeground(popupForeground);
        loadingLabel.setFont(loadingLabel.getFont().deriveFont(Font.BOLD, Math.max(11f, loadingLabel.getFont().getSize2D())));
        loadingPanel.setOpaque(true);
        loadingPanel.setBackground(popupBackground);
        loadingPanel.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel loadingContent = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        loadingContent.setOpaque(false);
        loadingContent.add(loadingSpinner);
        loadingContent.add(loadingLabel);
        loadingPanel.add(loadingContent);

        detailLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, blend(popupForeground, popupBackground, 0.88f)),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        detailLabel.setOpaque(true);
        detailLabel.setBackground(popupBackground);
        detailLabel.setForeground(blend(popupForeground, popupBackground, 0.35f));

        centerPanel.setOpaque(false);
        centerPanel.add(scroll, COMPLETIONS_CARD);
        centerPanel.add(loadingPanel, LOADING_CARD);

        popup.setBorder(new RoundedPopupBorder(blend(popupForeground, popupBackground, 0.78f), 10));
        popup.setBackground(popupBackground);
        popup.setLayout(new BorderLayout());
        popup.add(centerPanel, BorderLayout.CENTER);
        popup.add(detailLabel, BorderLayout.SOUTH);
        popup.setFocusable(false);

        list.addListSelectionListener(e -> updateDetail());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (loading || e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() < 2) return;
                int index = list.locationToIndex(e.getPoint());
                if (index < 0) return;
                Rectangle cell = list.getCellBounds(index, index);
                if (cell == null || !cell.contains(e.getPoint())) return;
                list.setSelectedIndex(index);
                if (acceptHandler != null) {
                    acceptHandler.run();
                }
            }
        });
    }

    protected ListCellRenderer<AutoCompleteItem> createCellRenderer() {
        return new CompletionCellRenderer();
    }

    protected String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    protected void updateDetail() {
        AutoCompleteItem sel = list.getSelectedValue();
        if (sel == null || sel.description() == null || sel.description().isBlank()) {
            detailLabel.setVisible(false);
        } else {
            detailLabel.setText("<html>" + escapeHtml(sel.description()) + "</html>");
            detailLabel.setVisible(true);
        }
    }

    public void show(List<AutoCompleteItem> items, int x, int y, String prefix, int insertOffset) {
        if (items == null || items.isEmpty()) {
            hide();
            return;
        }
        loading = false;
        loadingSpinner.stop();
        ((CardLayout) centerPanel.getLayout()).show(centerPanel, COMPLETIONS_CARD);
        model.clear();
        for (AutoCompleteItem it : items) model.addElement(it);
        list.setSelectedIndex(0);
        this.triggerPrefix = prefix == null ? "" : prefix;
        this.triggerOffset = insertOffset;
        updateDetail();
        scroll.setPreferredSize(resolveListSize(items.size()));
        centerPanel.setPreferredSize(scroll.getPreferredSize());
        centerPanel.revalidate();
        popup.pack();
        popup.show(owner, x, y);
    }

    public void showLoading(int x, int y, String prefix, int insertOffset) {
        model.clear();
        loading = true;
        this.triggerPrefix = prefix == null ? "" : prefix;
        this.triggerOffset = insertOffset;
        detailLabel.setVisible(false);
        int width = Math.min(popupSize.width, 300);
        loadingPanel.setPreferredSize(new Dimension(width, 54));
        centerPanel.setPreferredSize(loadingPanel.getPreferredSize());
        ((CardLayout) centerPanel.getLayout()).show(centerPanel, LOADING_CARD);
        loadingSpinner.start();
        centerPanel.revalidate();
        popup.pack();
        popup.show(owner, x, y);
    }

    public void hide() {
        loading = false;
        loadingSpinner.stop();
        popup.setVisible(false);
    }

    public boolean isVisible() {
        return popup.isVisible();
    }

    public AutoCompleteItem getSelectedItem() {
        return list.getSelectedValue();
    }

    public boolean isLoading() {
        return loading;
    }

    public void moveSelection(int delta) {
        int size = model.size();
        if (size == 0) return;
        int idx = list.getSelectedIndex() + delta;
        if (idx < 0) idx = 0;
        if (idx >= size) idx = size - 1;
        list.setSelectedIndex(idx);
        list.ensureIndexIsVisible(idx);
    }

    public void setPopupSize(Dimension size) {
        this.popupSize = size;
        scroll.setPreferredSize(size);
    }

    public void setAcceptHandler(Runnable acceptHandler) {
        this.acceptHandler = acceptHandler;
    }

    protected Dimension resolveListSize(int itemCount) {
        int rows = Math.max(1, Math.min(itemCount, list.getVisibleRowCount()));
        int height = rows * list.getFixedCellHeight();
        if (itemCount > rows) height = Math.min(popupSize.height, height + 2);
        return new Dimension(popupSize.width, Math.min(popupSize.height, height));
    }

    protected static Color resolveColor(String primaryKey, String fallbackKey, Color fallback) {
        Color color = UIManager.getColor(primaryKey);
        if (color != null) return color;
        color = UIManager.getColor(fallbackKey);
        return color != null ? color : fallback;
    }

    protected static Color blend(Color from, Color to, float amount) {
        float clamped = Math.max(0f, Math.min(1f, amount));
        int r = Math.round(from.getRed() + (to.getRed() - from.getRed()) * clamped);
        int g = Math.round(from.getGreen() + (to.getGreen() - from.getGreen()) * clamped);
        int b = Math.round(from.getBlue() + (to.getBlue() - from.getBlue()) * clamped);
        int a = Math.round(from.getAlpha() + (to.getAlpha() - from.getAlpha()) * clamped);
        return new Color(r, g, b, a);
    }

    protected static class CompletionCellRenderer extends JPanel implements ListCellRenderer<AutoCompleteItem> {
        private final JLabel iconLabel = new JLabel("", SwingConstants.CENTER);
        private final JLabel titleLabel = new JLabel();
        private final JLabel detailLabel = new JLabel();
        private final JLabel kindLabel = new JLabel();

        CompletionCellRenderer() {
            setLayout(new BorderLayout(8, 0));
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            setOpaque(true);

            iconLabel.setPreferredSize(new Dimension(22, 22));
            iconLabel.setOpaque(true);

            JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 0));
            textPanel.setOpaque(false);
            textPanel.add(titleLabel);
            textPanel.add(detailLabel);

            kindLabel.setBorder(BorderFactory.createEmptyBorder(1, 6, 1, 6));
            kindLabel.setOpaque(true);

            add(iconLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
            add(kindLabel, BorderLayout.EAST);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends AutoCompleteItem> list,
                                                      AutoCompleteItem item,
                                                      int index,
                                                      boolean isSelected,
                                                      boolean cellHasFocus) {
            Color bg = resolveColor("PopupMenu.background", "ToolTip.background", Color.WHITE);
            Color fg = resolveColor("PopupMenu.foreground", "ToolTip.foreground", new Color(0x111827));
            Color selectionBg = resolveColor("List.selectionBackground", "MenuItem.selectionBackground", new Color(0xDBEAFE));
            Color selectionFg = resolveColor("List.selectionForeground", "MenuItem.selectionForeground", new Color(0x111827));
            Color rowBg = isSelected ? selectionBg : bg;
            Color rowFg = isSelected ? selectionFg : fg;
            Color subtle = blend(rowFg, rowBg, 0.46f);

            setBackground(rowBg);
            titleLabel.setForeground(rowFg);
            titleLabel.setFont(list.getFont().deriveFont(Font.BOLD));
            detailLabel.setForeground(subtle);
            detailLabel.setFont(list.getFont().deriveFont(Math.max(10f, list.getFont().getSize2D() - 1f)));

            String label = item == null ? "" : item.label() != null ? item.label() : item.insertText();
            String detail = item == null || item.detail() == null ? "" : item.detail();
            titleLabel.setText(label);
            detailLabel.setText(detail.isBlank() ? " " : detail);

            Icon icon = item == null ? null : item.icon();
            iconLabel.setIcon(icon);
            iconLabel.setText(icon == null ? kindInitial(item) : "");
            iconLabel.setForeground(rowFg);
            iconLabel.setBackground(blend(rowFg, rowBg, 0.86f));
            iconLabel.setBorder(BorderFactory.createLineBorder(blend(rowFg, rowBg, 0.76f)));

            kindLabel.setText(kindText(item));
            kindLabel.setForeground(subtle);
            kindLabel.setBackground(blend(rowFg, rowBg, isSelected ? 0.82f : 0.92f));
            kindLabel.setFont(list.getFont().deriveFont(Font.PLAIN, Math.max(9f, list.getFont().getSize2D() - 2f)));
            return this;
        }

        private String kindInitial(AutoCompleteItem item) {
            if (item == null || item.kind() == null) return "T";
            return item.isSnippet() ? "S" : "T";
        }

        private String kindText(AutoCompleteItem item) {
            if (item == null || item.kind() == null) return text("kind.text", "text");
            return item.isSnippet() ? text("kind.snippet", "snippet") : text("kind.text", "text");
        }
    }

    protected static class RoundedPopupBorder extends AbstractBorder {
        private final Color color;
        private final int arc;

        RoundedPopupBorder(Color color, int arc) {
            this.color = color;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.draw(new RoundRectangle2D.Float(x + 0.5f, y + 0.5f, width - 1.5f, height - 1.5f, arc, arc));
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(6, 6, 6, 6);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.top = 6;
            insets.left = 6;
            insets.bottom = 6;
            insets.right = 6;
            return insets;
        }
    }

    protected static class LoadingSpinner extends JComponent {
        private int frame;
        private final Timer timer = new Timer(70, e -> {
            frame = (frame + 1) % 24;
            repaint();
        });

        LoadingSpinner() {
            setPreferredSize(new Dimension(20, 20));
            setMinimumSize(new Dimension(20, 20));
            setOpaque(false);
        }

        void start() {
            if (!timer.isRunning()) timer.start();
        }

        void stop() {
            timer.stop();
            frame = 0;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight());
                float stroke = Math.max(2f, size / 9f);
                float pad = stroke + 1f;
                Color base = getForeground() != null ? getForeground() : new Color(0x2563EB);

                g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 42));
                g2.drawOval(Math.round(pad), Math.round(pad), Math.round(size - pad * 2), Math.round(size - pad * 2));

                int start = 90 - frame * 15;
                g2.setColor(base);
                g2.drawArc(Math.round(pad), Math.round(pad), Math.round(size - pad * 2), Math.round(size - pad * 2), start, 115);
            } finally {
                g2.dispose();
            }
        }
    }
}
