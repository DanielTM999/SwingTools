package dtm.stools.component.panels.editor.code.signature;

import dtm.stools.i18n.I18n;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.util.List;

/**
 * Popup que renderiza o {@link SignatureHelp}: a assinatura ativa com o parametro
 * corrente destacado, documentacao opcional e um contador de sobrecargas quando ha
 * mais de uma. E posicionado, preferencialmente, acima da linha do caret.
 */
public class SignatureHelpPopup {

    private static String text(String key, String defaultValue) {
        return I18n.getText(SignatureHelpPopup.class, key, defaultValue);
    }

    protected final JComponent owner;
    protected final JPopupMenu popup = new JPopupMenu();
    protected final JPanel panel = new SignaturePanel();
    protected final JPanel headerPanel = new JPanel(new BorderLayout());
    protected final JLabel overloadLabel = new JLabel();
    protected final JLabel navHintLabel = new JLabel();
    protected final JEditorPane contentPane = new JEditorPane();
    protected final JScrollPane scrollPane = new JScrollPane(contentPane);

    @Getter
    protected SignatureHelp signatureHelp;

    /** Indice da sobrecarga atualmente exibida (pode diferir do default ao navegar). */
    @Getter
    protected int activeSignature;

    /** Indica que o usuario navegou manualmente entre sobrecargas. */
    @Getter
    protected boolean userSelectedSignature;

    @Getter
    @Setter
    protected Dimension maxSize = new Dimension(620, 280);

    public SignatureHelpPopup(JComponent owner) {
        this.owner = owner;
        panel.setLayout(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new SignatureBorder(),
                BorderFactory.createEmptyBorder(8, 12, 9, 12)
        ));

        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        overloadLabel.setHorizontalAlignment(SwingConstants.LEFT);
        navHintLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        headerPanel.add(overloadLabel, BorderLayout.WEST);
        headerPanel.add(navHintLabel, BorderLayout.EAST);

        contentPane.setEditable(false);
        contentPane.setOpaque(false);
        contentPane.setBorder(null);
        contentPane.setFocusable(false);
        contentPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        panel.add(headerPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        popup.setLayout(new BorderLayout());
        popup.setBorder(BorderFactory.createEmptyBorder(4, 4, 7, 7));
        popup.setOpaque(false);
        popup.setFocusable(false);
        popup.add(panel, BorderLayout.CENTER);
    }

    /**
     * Exibe o popup. Tenta posiciona-lo acima da linha do caret (entre
     * {@code anchorTopY} e o topo da tela); se nao houver espaco, mostra abaixo.
     *
     * @param help        conteudo a exibir
     * @param x           coordenada x (em coordenadas do owner) alinhada ao caret
     * @param anchorTopY  y do topo da linha do caret (coordenadas do owner)
     * @param anchorBottomY y da base da linha do caret (coordenadas do owner)
     */
    public void show(SignatureHelp help, int x, int anchorTopY, int anchorBottomY) {
        if (help == null || help.isEmpty()) {
            hide();
            return;
        }
        boolean sameSession = this.signatureHelp != null && popup.isVisible();
        this.signatureHelp = help;
        if (sameSession && userSelectedSignature && activeSignature < help.signatureCount()) {
            // preserva a sobrecarga escolhida pelo usuario durante a digitacao
        } else {
            this.activeSignature = help.safeActiveSignature();
            this.userSelectedSignature = false;
        }
        render();

        popup.pack();
        Dimension size = popup.getPreferredSize();
        int gap = 2;
        int y = placeAbove(anchorTopY, size.height, gap) ? anchorTopY - size.height - gap : anchorBottomY + gap;
        popup.show(owner, x, y);
    }

    public void hide() {
        signatureHelp = null;
        userSelectedSignature = false;
        activeSignature = 0;
        popup.setVisible(false);
    }

    public boolean isVisible() {
        return popup.isVisible();
    }

    public void nextSignature() {
        cycleSignature(1);
    }

    public void previousSignature() {
        cycleSignature(-1);
    }

    protected void cycleSignature(int delta) {
        if (signatureHelp == null || signatureHelp.signatureCount() <= 1) return;
        int count = signatureHelp.signatureCount();
        activeSignature = ((activeSignature + delta) % count + count) % count;
        userSelectedSignature = true;
        render();
        popup.pack();
    }

    protected boolean placeAbove(int anchorTopY, int popupHeight, int gap) {
        try {
            Point ownerScreen = owner.getLocationOnScreen();
            int screenTopY = ownerScreen.y + anchorTopY - popupHeight - gap;
            GraphicsConfiguration gc = owner.getGraphicsConfiguration();
            if (gc == null) return true;
            Rectangle screen = gc.getBounds();
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
            return screenTopY >= screen.y + insets.top;
        } catch (IllegalComponentStateException ex) {
            return true;
        }
    }

    protected void render() {
        applyTheme();
        SignatureInformation active = signatureHelp.withActiveSignature(activeSignature).activeSignatureInfo();
        int activeParam = signatureHelp.withActiveSignature(activeSignature).safeActiveParameter();

        int count = signatureHelp.signatureCount();
        if (count > 1) {
            headerPanel.setVisible(true);
            overloadLabel.setText((activeSignature + 1) + " / " + count);
            navHintLabel.setText(text("hint.switch", "↑↓ alterna"));
        } else {
            headerPanel.setVisible(false);
        }

        contentPane.setText(renderHtml(active, activeParam));
        contentPane.setCaretPosition(0);

        int contentWidth = Math.max(220, maxSize.width - 36);
        contentPane.setSize(new Dimension(contentWidth, Short.MAX_VALUE));
        Dimension textSize = contentPane.getPreferredSize();
        int width = Math.min(maxSize.width - 36, Math.max(260, textSize.width));
        contentPane.setSize(new Dimension(width, Short.MAX_VALUE));
        textSize = contentPane.getPreferredSize();
        int height = Math.min(maxSize.height - 30, Math.max(textSize.height, contentPane.getFontMetrics(contentPane.getFont()).getHeight()));
        scrollPane.setPreferredSize(new Dimension(width, height));
        panel.revalidate();
    }

    protected String renderHtml(SignatureInformation info, int activeParam) {
        StringBuilder sb = new StringBuilder("<html><body>");
        sb.append("<div class='sig'>").append(renderLabel(info, activeParam)).append("</div>");

        String paramDoc = activeParam >= 0 && activeParam < info.parameters().size()
                ? info.parameters().get(activeParam).documentation()
                : null;
        if (paramDoc != null && !paramDoc.isBlank()) {
            sb.append("<div class='pdoc'>").append(escapeHtml(paramDoc)).append("</div>");
        }
        if (info.hasDocumentation()) {
            sb.append("<div class='doc'>").append(escapeHtml(info.documentation()).replace("\n", "<br>")).append("</div>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    /** Renderiza o rotulo da assinatura com o parametro ativo destacado. */
    protected String renderLabel(SignatureInformation info, int activeParam) {
        String label = info.label();
        List<ParameterInformation> params = info.parameters();
        if (activeParam < 0 || activeParam >= params.size()) {
            return escapeHtml(label);
        }
        int[] span = parameterSpan(label, params, activeParam);
        if (span == null) {
            return escapeHtml(label);
        }
        String before = label.substring(0, span[0]);
        String mid = label.substring(span[0], span[1]);
        String after = label.substring(span[1]);
        return escapeHtml(before)
                + "<span class='active'>" + escapeHtml(mid) + "</span>"
                + escapeHtml(after);
    }

    /**
     * Localiza o intervalo {@code [start, end)} do parametro de indice
     * {@code activeParam} dentro do rotulo, procurando cada rotulo de parametro
     * sequencialmente para lidar com nomes repetidos. Retorna {@code null} se nao
     * for possivel localizar.
     */
    protected int[] parameterSpan(String label, List<ParameterInformation> params, int activeParam) {
        int searchFrom = 0;
        for (int i = 0; i <= activeParam; i++) {
            String paramLabel = params.get(i).label();
            if (paramLabel == null || paramLabel.isEmpty()) return null;
            int idx = label.indexOf(paramLabel, searchFrom);
            if (idx < 0) return null;
            int end = idx + paramLabel.length();
            if (i == activeParam) {
                return new int[]{idx, end};
            }
            searchFrom = end;
        }
        return null;
    }

    protected String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    protected void applyTheme() {
        Color editorBg = owner.getBackground();
        Color tooltipBg = UIManager.getColor("ToolTip.background");
        Color tooltipFg = UIManager.getColor("ToolTip.foreground");
        Color background = tooltipBg != null ? tooltipBg : (isDark(editorBg) ? new Color(0x1F2937) : Color.WHITE);
        Color foreground = tooltipFg != null ? tooltipFg : (isDark(background) ? new Color(0xE5E7EB) : new Color(0x111827));
        Color secondary = isDark(background) ? new Color(0xAEB7C2) : new Color(0x4B5563);
        Color activeFg = isDark(background) ? new Color(0xFFFFFF) : new Color(0x0F172A);
        Color activeBg = isDark(background) ? new Color(0x2D4A78) : new Color(0xDBEAFE);

        panel.setBackground(background);
        overloadLabel.setForeground(secondary);
        navHintLabel.setForeground(secondary);
        Font baseFont = resolveFont();
        Font smallFont = baseFont.deriveFont(Math.max(10f, baseFont.getSize2D() - 2f));
        overloadLabel.setFont(smallFont);
        navHintLabel.setFont(smallFont);
        contentPane.setForeground(foreground);
        contentPane.setFont(baseFont);

        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet css = kit.getStyleSheet();
        String family = baseFont.getFamily().replace("'", "\\'");
        int size = baseFont.getSize();
        css.addRule("body { margin: 0; padding: 0; font-family: '" + family + "'; font-size: " + size
                + "pt; color: " + toCss(foreground) + "; background: " + toCss(background) + "; }");
        css.addRule(".sig { font-family: Consolas, 'Courier New', monospace; line-height: 1.4; }");
        css.addRule(".active { font-weight: bold; color: " + toCss(activeFg)
                + "; background: " + toCss(activeBg) + "; }");
        css.addRule(".pdoc { margin-top: 7px; color: " + toCss(foreground) + "; line-height: 1.4; }");
        css.addRule(".doc { margin-top: 7px; color: " + toCss(secondary) + "; line-height: 1.4; }");
        contentPane.setEditorKit(kit);
    }

    protected Font resolveFont() {
        Font font = owner.getFont();
        if (font == null) font = UIManager.getFont("ToolTip.font");
        if (font == null) font = UIManager.getFont("Label.font");
        if (font == null) font = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        return font.deriveFont(Math.max(12f, font.getSize2D()));
    }

    protected boolean isDark(Color c) {
        if (c == null) return false;
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 < 128;
    }

    protected String toCss(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    protected static class SignaturePanel extends JPanel {
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

    protected static class SignatureBorder extends AbstractBorder {
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
}
