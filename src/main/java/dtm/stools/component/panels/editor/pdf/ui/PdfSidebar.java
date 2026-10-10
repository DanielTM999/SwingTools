package dtm.stools.component.panels.editor.pdf.ui;

import dtm.stools.component.panels.editor.pdf.PdfEditor;
import dtm.stools.configs.UiTokens;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PdfSidebar extends JPanel {
    private final PdfEditor editor;
    private final PdfThumbnailStrip thumbnails;
    private final JScrollPane thumbnailScroll;
    private final JPanel cards = new JPanel(new CardLayout());
    private final JPanel tools = new JPanel();
    private final JLabel title = new JLabel("Páginas");
    private final JLabel count = new JLabel();
    private final JTextField search = new JTextField();
    private final DefaultListModel<String> results = new DefaultListModel<>();
    private final JList<String> resultList = new JList<>(results);
    private final List<Integer> resultPages = new ArrayList<>();
    private final JTextArea pageText = new JTextArea();
    private final ButtonGroup sections = new ButtonGroup();
    private final List<JToggleButton> sectionButtons = new ArrayList<>();
    private String section = "pages";

    public PdfSidebar(PdfEditor editor) {
        super(new BorderLayout());
        this.editor = editor;
        setName("pdf.sidebar");
        setMinimumSize(new Dimension(140, 120));
        setPreferredSize(new Dimension(214, 400));
        thumbnails = new PdfThumbnailStrip(editor);
        thumbnailScroll = new JScrollPane(thumbnails, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        thumbnailScroll.setBorder(BorderFactory.createEmptyBorder());
        thumbnailScroll.getVerticalScrollBar().setUnitIncrement(24);
        thumbnailScroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent event) {
                thumbnails.revalidate();
                SwingUtilities.invokeLater(thumbnails::scrollToCurrent);
            }
        });
        add(header(), BorderLayout.NORTH);
        cards.setOpaque(false);
        cards.add(thumbnailScroll, "pages");
        tools.setOpaque(false);
        tools.setLayout(new BoxLayout(tools, BoxLayout.Y_AXIS));
        tools.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        JScrollPane toolScroll = new JScrollPane(tools);
        toolScroll.setBorder(BorderFactory.createEmptyBorder());
        toolScroll.getViewport().setOpaque(false);
        toolScroll.setOpaque(false);
        cards.add(toolScroll, "tools");
        cards.add(textSection(), "text");
        add(cards, BorderLayout.CENTER);
        applyTheme();
        show("pages");
    }

    public void applyTheme() {
        setBackground(UiTokens.surfaceAlt());
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UiTokens.border()));
        thumbnailScroll.getViewport().setBackground(UiTokens.surfaceAlt());
        title.setForeground(UiTokens.foreground());
        count.setForeground(UiTokens.muted());
        repaint();
    }

    private JComponent header() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        strip.setOpaque(false);
        strip.setBorder(BorderFactory.createEmptyBorder(6, 6, 2, 6));
        strip.add(sectionButton("pages", "pages", "Miniaturas das páginas"));
        strip.add(sectionButton("tools", "tools", "Ferramentas de edição"));
        strip.add(sectionButton("text", "search", "Buscar e ver o texto da página"));
        header.add(strip, BorderLayout.NORTH);
        JPanel caption = new JPanel(new BorderLayout());
        caption.setOpaque(false);
        caption.setBorder(BorderFactory.createEmptyBorder(4, 12, 6, 12));
        title.setFont(UiTokens.fontBold());
        count.setFont(UiTokens.fontSmall());
        caption.add(title, BorderLayout.WEST);
        caption.add(count, BorderLayout.EAST);
        header.add(caption, BorderLayout.SOUTH);
        return header;
    }

    private JToggleButton sectionButton(String id, String icon, String tip) {
        JToggleButton button = new JToggleButton(PdfIcon.small(icon));
        button.putClientProperty("JButton.buttonType", "toolBarButton");
        button.setFocusable(false);
        button.setToolTipText(tip);
        button.setName("pdf.sidebar." + id);
        button.addActionListener(event -> show(id));
        sections.add(button);
        sectionButtons.add(button);
        button.putClientProperty("pdf.section", id);
        return button;
    }

    private JComponent textSection() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        search.putClientProperty("JTextField.placeholderText", "Buscar no documento");
        search.putClientProperty("JTextField.showClearButton", true);
        search.setName("pdf.sidebar.search");
        search.addActionListener(event -> runSearch());
        resultList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultList.setVisibleRowCount(5);
        resultList.addListSelectionListener(event -> {
            int index = resultList.getSelectedIndex();
            if (!event.getValueIsAdjusting() && index >= 0 && index < resultPages.size()) editor.setCurrentPage(resultPages.get(index));
        });
        JScrollPane resultScroll = new JScrollPane(resultList);
        resultScroll.setPreferredSize(new Dimension(180, 110));
        pageText.setEditable(false);
        pageText.setLineWrap(true);
        pageText.setWrapStyleWord(true);
        pageText.setFont(UiTokens.fontSmall());
        pageText.setName("pdf.text.panel");
        JScrollPane textScroll = new JScrollPane(pageText);
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.setOpaque(false);
        top.add(search, BorderLayout.NORTH);
        top.add(resultScroll, BorderLayout.CENTER);
        JLabel textTitle = new JLabel("Texto da página");
        textTitle.setFont(UiTokens.fontSmall().deriveFont(Font.BOLD));
        textTitle.setForeground(UiTokens.muted());
        JPanel bottom = new JPanel(new BorderLayout(0, 4));
        bottom.setOpaque(false);
        bottom.add(textTitle, BorderLayout.NORTH);
        bottom.add(textScroll, BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(.35);
        split.setOpaque(false);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private void runSearch() {
        String query = search.getText();
        results.clear();
        resultPages.clear();
        if (query == null || query.isBlank()) return;
        results.addElement("Buscando…");
        editor.searchAsync(query, found -> {
            results.clear();
            resultPages.clear();
            for (Map.Entry<Integer, String> entry : found.entrySet()) {
                resultPages.add(entry.getKey());
                results.addElement("Página " + (entry.getKey() + 1) + " — " + entry.getValue());
            }
            if (found.isEmpty()) results.addElement("Nenhum resultado");
        });
    }

    public void show(String id) {
        section = id;
        ((CardLayout) cards.getLayout()).show(cards, id);
        for (JToggleButton button : sectionButtons) button.setSelected(id.equals(button.getClientProperty("pdf.section")));
        title.setText(switch (id) { case "tools" -> "Ferramentas"; case "text" -> "Buscar e texto"; default -> "Páginas"; });
        count.setVisible(id.equals("pages"));
        if (id.equals("text")) {
            refreshText();
            SwingUtilities.invokeLater(search::requestFocusInWindow);
        }
    }

    public String section() { return section; }
    public PdfThumbnailStrip thumbnails() { return thumbnails; }

    public void setTools(List<Action> actions) {
        tools.removeAll();
        for (Action action : actions) {
            if (action == null) { tools.add(Box.createVerticalStrut(8)); continue; }
            AbstractButton button = Boolean.TRUE.equals(action.getValue(PdfRibbon.TOGGLE)) ? new JToggleButton(action) : new JButton(action);
            button.setIcon(PdfIcon.small(PdfRibbon.iconOf(action)));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setIconTextGap(8);
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
            button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            button.setFocusable(false);
            button.putClientProperty("JButton.buttonType", "toolBarButton");
            Object tip = action.getValue(Action.SHORT_DESCRIPTION);
            if (tip != null) button.setToolTipText(tip.toString());
            tools.add(button);
        }
        tools.revalidate();
        tools.repaint();
    }

    public void pagesChanged() {
        count.setText(editor.getPageCount() == 1 ? "1 página" : editor.getPageCount() + " páginas");
        thumbnails.revalidate();
        thumbnails.repaint();
        refreshText();
    }

    public void currentPageChanged() {
        thumbnails.repaint();
        thumbnails.scrollToCurrent();
        if (section.equals("text")) refreshText();
    }

    public void repaintThumbnails() { thumbnails.repaint(); }

    private void refreshText() {
        if (!section.equals("text")) return;
        int page = editor.getCurrentPage();
        editor.pageTextAsync(page, text -> { if (page == editor.getCurrentPage()) { pageText.setText(text); pageText.setCaretPosition(0); } });
    }
}
