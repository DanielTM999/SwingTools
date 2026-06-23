package dtm.stools.component.panels.editor.code.hover;

import dtm.stools.component.panels.editor.code.CodeEditorScrollPane;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;

public class HoverDocumentationPopup {

    protected final JComponent owner;
    protected final JPopupMenu popup = new JPopupMenu();
    protected final JPanel panel = new DocumentationPanel();
    protected final JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    protected final JButton copyButton = new JButton("Copy");
    protected final JEditorPane contentPane = new JEditorPane();
    protected final JScrollPane scrollPane = new JScrollPane(contentPane);
    protected final MarkdownSupport markdownSupport = MarkdownSupport.create();
    protected final CodeEditorScrollPane.CodeEditorScrollBarUI scrollBarUI = new CodeEditorScrollPane.CodeEditorScrollBarUI();
    protected boolean mouseInside;
    protected HoverInfo currentInfo;
    @Setter
    protected Runnable mouseExitHandler;

    @Getter
    @Setter
    protected Dimension maxSize = new Dimension(560, 320);

    @Getter
    protected boolean textSelectionEnabled = true;

    public HoverDocumentationPopup(JComponent owner) {
        this.owner = owner;
        panel.setLayout(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new DocumentationBorder(),
                BorderFactory.createEmptyBorder(9, 12, 11, 12)
        ));

        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 7, 0));
        configureCopyButton();
        headerPanel.add(copyButton);

        contentPane.setEditable(false);
        contentPane.setOpaque(false);
        contentPane.setBorder(null);
        contentPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        contentPane.setDragEnabled(false);

        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        configureScrollBar();

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        popup.setLayout(new BorderLayout());
        popup.setBorder(BorderFactory.createEmptyBorder(4, 4, 7, 7));
        popup.setOpaque(false);
        popup.add(panel, BorderLayout.CENTER);
        popup.setFocusable(true);

        installMouseTracking(popup);
        installMouseTracking(panel);
        installMouseTracking(headerPanel);
        installMouseTracking(copyButton);
        installMouseTracking(scrollPane);
        installMouseTracking(scrollPane.getViewport());
        installMouseTracking(contentPane);
        applyTextSelectionEnabled();
    }

    public void show(HoverInfo info, int x, int y) {
        if (info == null || info.content() == null || info.content().isBlank()) {
            hide();
            return;
        }
        currentInfo = info;
        applyTheme();
        contentPane.setText(renderHtml(info));
        contentPane.setCaretPosition(0);

        int contentWidth = Math.max(220, maxSize.width - 32);
        contentPane.setSize(new Dimension(contentWidth, Short.MAX_VALUE));
        Dimension textSize = contentPane.getPreferredSize();
        int headerWidth = headerPanel.getPreferredSize().width;
        int width = Math.min(maxSize.width - 32, Math.max(Math.max(280, headerWidth), textSize.width));

        contentPane.setSize(new Dimension(width, Short.MAX_VALUE));
        textSize = contentPane.getPreferredSize();
        int headerHeight = headerPanel.isVisible() ? headerPanel.getPreferredSize().height : 0;
        int height = Math.min(maxSize.height - 34 - headerHeight, Math.max(54, textSize.height));
        scrollPane.setPreferredSize(new Dimension(width, height));

        popup.pack();
        popup.show(owner, x, y);
        mouseInside = isMousePointerInside();
    }

    public void hide() {
        mouseInside = false;
        currentInfo = null;
        contentPane.select(0, 0);
        popup.setVisible(false);
    }

    public boolean isVisible() {
        return popup.isVisible();
    }

    public Rectangle getBoundsInOwner() {
        if (!popup.isVisible()) return null;
        try {
            Point location = popup.getLocationOnScreen();
            SwingUtilities.convertPointFromScreen(location, owner);
            return new Rectangle(location, popup.getSize());
        } catch (IllegalComponentStateException ex) {
            return null;
        }
    }

    public boolean isMouseInside() {
        return mouseInside || isMousePointerInside();
    }

    public void setTextSelectionEnabled(boolean textSelectionEnabled) {
        this.textSelectionEnabled = textSelectionEnabled;
        applyTextSelectionEnabled();
    }

    public boolean isTextCopyEnabled() {
        return textSelectionEnabled;
    }

    public void setTextCopyEnabled(boolean enabled) {
        setTextSelectionEnabled(enabled);
    }

    protected void applyTextSelectionEnabled() {
        contentPane.setFocusable(textSelectionEnabled);
        contentPane.setEnabled(true);
        contentPane.setCursor(Cursor.getPredefinedCursor(
                textSelectionEnabled ? Cursor.TEXT_CURSOR : Cursor.DEFAULT_CURSOR
        ));
        contentPane.setTransferHandler(textSelectionEnabled ? new TransferHandler("text") : null);
        copyButton.setVisible(textSelectionEnabled);
        if (!textSelectionEnabled) {
            contentPane.select(0, 0);
        }
    }

    protected void configureCopyButton() {
        copyButton.setFocusable(false);
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        copyButton.setBorder(BorderFactory.createEmptyBorder(4, 9, 4, 9));
        copyButton.setMargin(new Insets(0, 0, 0, 0));
        copyButton.setContentAreaFilled(false);
        copyButton.setOpaque(false);
        copyButton.addActionListener(e -> copyCurrentContent());
    }

    protected void configureScrollBar() {
        scrollBarUI.setThickness(5);
        scrollBarUI.setMinThumbSize(30);
        scrollBarUI.setThumbArc(5);
        scrollBarUI.setThumbAlpha(0.58f);

        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        verticalScrollBar.setUI(scrollBarUI);
        verticalScrollBar.setOpaque(false);
        verticalScrollBar.setUnitIncrement(12);
    }

    protected void copyCurrentContent() {
        if (!textSelectionEnabled || currentInfo == null || currentInfo.content() == null) return;
        String selected = contentPane.getSelectedText();
        String text = selected != null && !selected.isBlank() ? selected : currentInfo.content();
        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(text), null);
        } catch (IllegalStateException | HeadlessException ignored) {
        }
    }

    protected String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    protected String renderHtml(HoverInfo info) {
        if (info.markdown()) {
            return "<html><body>"
                    + markdownSupport.render(info.content(), this::escapeHtml)
                    + "</body></html>";
        }
        if (info.html()) {
            String html = info.content().trim();
            if (html.toLowerCase().contains("<html")) {
                return html;
            }
            return "<html><body>" + html + "</body></html>";
        }
        return "<html><body><div class='content'>"
                + escapeHtml(info.content()).replace("\n", "<br>")
                + "</div></body></html>";
    }

    protected void applyTheme() {
        Color editorBg = owner.getBackground();
        Color tooltipBg = UIManager.getColor("ToolTip.background");
        Color tooltipFg = UIManager.getColor("ToolTip.foreground");
        Color background = tooltipBg != null ? tooltipBg : (isDark(editorBg) ? new Color(0x1F2937) : Color.WHITE);
        Color foreground = tooltipFg != null ? tooltipFg : (isDark(background) ? new Color(0xE5E7EB) : new Color(0x111827));
        Color secondary = isDark(background) ? new Color(0xAEB7C2) : new Color(0x4B5563);
        Color codeBg = isDark(background) ? shift(background, 18) : shift(background, -8);
        Color softBg = isDark(background) ? shift(background, 14) : shift(background, -6);
        Color buttonBorder = isDark(background) ? new Color(255, 255, 255, 38) : new Color(17, 24, 39, 34);

        panel.setBackground(background);
        copyButton.setForeground(foreground);
        copyButton.setBackground(softBg);
        copyButton.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(buttonBorder, 8),
                BorderFactory.createEmptyBorder(4, 9, 4, 9)
        ));
        contentPane.setForeground(foreground);
        contentPane.setFont(resolveFont());
        contentPane.setCaretColor(isDark(background) ? new Color(0xBFDBFE) : new Color(0x1D4ED8));
        contentPane.setSelectionColor(isDark(background) ? new Color(37, 99, 235, 150) : new Color(147, 197, 253, 180));
        contentPane.setSelectedTextColor(foreground);

        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet css = kit.getStyleSheet();
        String family = contentPane.getFont().getFamily().replace("'", "\\'");
        int size = contentPane.getFont().getSize();
        css.addRule("body { margin: 0; padding: 0; font-family: '" + family + "'; font-size: " + size
                + "pt; color: " + toCss(foreground) + "; background: " + toCss(background) + "; }");
        css.addRule(".content { line-height: 1.42; }");
        css.addRule("p { margin: 0 0 8px 0; }");
        css.addRule("ul, ol { margin-top: 4px; margin-bottom: 8px; }");
        css.addRule("li { margin-bottom: 3px; }");
        css.addRule("code, pre { font-family: Consolas, 'Courier New', monospace; background: " + toCss(codeBg)
                + "; color: " + toCss(foreground) + "; }");
        css.addRule("pre { margin: 7px 0; padding: 8px; }");
        css.addRule("h1, h2, h3, h4 { margin: 0 0 8px 0; font-weight: bold; }");
        css.addRule("blockquote { margin: 6px 0; padding-left: 9px; color: " + toCss(secondary) + "; }");
        css.addRule("a { color: " + toCss(isDark(background) ? new Color(0x93C5FD) : new Color(0x2563EB)) + "; }");
        css.addRule("small { color: " + toCss(secondary) + "; }");
        contentPane.setEditorKit(kit);
    }

    protected Font resolveFont() {
        Font font = owner.getFont();
        if (font == null) font = UIManager.getFont("ToolTip.font");
        if (font == null) font = UIManager.getFont("Label.font");
        if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        return font.deriveFont(Math.max(12f, font.getSize2D()));
    }

    protected void installMouseTracking(Component component) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                mouseInside = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
                SwingUtilities.invokeLater(() -> {
                    mouseInside = isMousePointerInside();
                    if (!mouseInside && mouseExitHandler != null) {
                        mouseExitHandler.run();
                    }
                });
            }
        });
    }

    protected boolean isMousePointerInside() {
        if (!popup.isVisible()) return false;
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        if (pointerInfo == null) return false;
        try {
            Point location = popup.getLocationOnScreen();
            Dimension size = popup.getSize();
            return new Rectangle(location, size).contains(pointerInfo.getLocation());
        } catch (IllegalComponentStateException ex) {
            return false;
        }
    }

    protected boolean isDark(Color c) {
        if (c == null) return false;
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 < 128;
    }

    protected Color shift(Color color, int amount) {
        return new Color(
                clamp(color.getRed() + amount),
                clamp(color.getGreen() + amount),
                clamp(color.getBlue() + amount),
                color.getAlpha()
        );
    }

    protected int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    protected String toCss(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    protected static class DocumentationPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(0, 0, 0, 44));
            g2.fillRoundRect(4, 6, getWidth() - 6, getHeight() - 7, 14, 14);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth() - 6, getHeight() - 8, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    protected static class DocumentationBorder extends AbstractBorder {
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 2, 2);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 2, 2);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = c.getBackground();
            Color line = bg == null || (bg.getRed() * 299 + bg.getGreen() * 587 + bg.getBlue() * 114) / 1000 < 128
                    ? new Color(255, 255, 255, 52)
                    : new Color(17, 24, 39, 44);
            g2.setColor(line);
            g2.drawRoundRect(x, y, width - 7, height - 9, 14, 14);
            g2.dispose();
        }
    }

    protected static class RoundedLineBorder extends AbstractBorder {
        private final Color color;
        private final int arc;

        RoundedLineBorder(Color color, int arc) {
            this.color = color;
            this.arc = arc;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
            g2.dispose();
        }
    }

    protected static class MarkdownSupport {
        private final Object parser;
        private final Object renderer;
        private final Method parseMethod;
        private final Method renderMethod;

        private MarkdownSupport(Object parser, Object renderer, Method parseMethod, Method renderMethod) {
            this.parser = parser;
            this.renderer = renderer;
            this.parseMethod = parseMethod;
            this.renderMethod = renderMethod;
        }

        static MarkdownSupport create() {
            try {
                Class<?> parserClass = Class.forName("org.commonmark.parser.Parser");
                Object parserBuilder = parserClass.getMethod("builder").invoke(null);
                Object parser = parserBuilder.getClass().getMethod("build").invoke(parserBuilder);

                Class<?> rendererClass = Class.forName("org.commonmark.renderer.html.HtmlRenderer");
                Object rendererBuilder = rendererClass.getMethod("builder").invoke(null);
                rendererBuilder.getClass().getMethod("escapeHtml", boolean.class).invoke(rendererBuilder, true);
                rendererBuilder.getClass().getMethod("sanitizeUrls", boolean.class).invoke(rendererBuilder, true);
                Object renderer = rendererBuilder.getClass().getMethod("build").invoke(rendererBuilder);

                Class<?> nodeClass = Class.forName("org.commonmark.node.Node");
                Method parseMethod = parserClass.getMethod("parse", String.class);
                Method renderMethod = rendererClass.getMethod("render", nodeClass);
                return new MarkdownSupport(parser, renderer, parseMethod, renderMethod);
            } catch (Throwable ignored) {
                return new MarkdownSupport(null, null, null, null);
            }
        }

        String render(String markdown, java.util.function.Function<String, String> escapeHtml) {
            if (parser != null && renderer != null && parseMethod != null && renderMethod != null) {
                try {
                    Object document = parseMethod.invoke(parser, markdown);
                    Object html = renderMethod.invoke(renderer, document);
                    if (html != null) {
                        return html.toString();
                    }
                } catch (Throwable ignored) {
                }
            }
            return "<div class='content'>"
                    + escapeHtml.apply(markdown).replace("\n", "<br>")
                    + "</div>";
        }
    }
}
