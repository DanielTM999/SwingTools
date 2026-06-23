package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticSeverity;
import dtm.stools.component.panels.editor.code.diagnostics.ErrorStripeClickEvent;
import dtm.stools.component.panels.editor.code.diagnostics.ErrorStripeClickListener;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Barra vertical fina (estilo "error stripe" do IntelliJ) que resume os
 * diagnósticos do documento. Fica à direita do editor: um quadradinho de status
 * no topo (com a severidade mais grave) e, abaixo, um marcador por diagnóstico
 * posicionado proporcionalmente à sua linha. É alimentada pelo
 * {@code DiagnosticsProvider} via {@link CodeEditorTextArea} e dispara um evento
 * de clique que, por padrão, navega até o diagnóstico.
 */
public class CodeEditorErrorStripe extends JComponent {

    private final CodeEditorTextArea textArea;
    private final JScrollPane scrollPane;

    @Getter
    private boolean enabled = true;

    @Getter
    @Setter
    private boolean navigateOnClick = true;

    @Getter
    private int stripeWidth = 14;

    /**
     * Quando true, desenha um quadradinho de status (pior severidade) no topo da
     * barra. Default false: o resumo já é mostrado pelo inspection widget, então
     * o quadrado ficaria redundante.
     */
    @Getter
    private boolean statusIndicatorEnabled = false;

    /** Altura do quadradinho de status no topo da barra (quando habilitado). */
    @Getter
    @Setter
    private int statusIndicatorHeight = 14;

    @Getter
    @Setter
    private int markerHeight = 3;

    /** Tolerância (px) para considerar que um clique atingiu um marcador. */
    @Getter
    @Setter
    private int clickTolerance = 4;

    @Getter
    @Setter
    private Color okColor = new Color(110, 180, 110);

    @Getter
    @Setter
    private boolean viewportIndicatorEnabled = true;

    @Getter
    @Setter
    private Color viewportIndicatorColor = new Color(128, 128, 128, 40);

    private final CopyOnWriteArrayList<ErrorStripeClickListener> clickListeners = new CopyOnWriteArrayList<>();

    public CodeEditorErrorStripe(CodeEditorTextArea textArea, JScrollPane scrollPane) {
        this.textArea = textArea;
        this.scrollPane = scrollPane;
        setOpaque(true);

        textArea.addDiagnosticsChangeListener(d -> repaint());
        if (scrollPane != null && scrollPane.getViewport() != null) {
            scrollPane.getViewport().addChangeListener(e -> {
                if (viewportIndicatorEnabled) repaint();
            });
        }

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!enabled) return;
                handleClick(e);
            }
        };
        addMouseListener(mouse);
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        setVisible(enabled);
        revalidate();
        repaint();
    }

    public void setStripeWidth(int width) {
        this.stripeWidth = Math.max(4, width);
        revalidate();
        repaint();
    }

    public void setStatusIndicatorEnabled(boolean enabled) {
        if (this.statusIndicatorEnabled == enabled) return;
        this.statusIndicatorEnabled = enabled;
        repaint();
    }

    public void addClickListener(ErrorStripeClickListener listener) {
        if (listener != null) clickListeners.add(listener);
    }

    public void removeClickListener(ErrorStripeClickListener listener) {
        if (listener != null) clickListeners.remove(listener);
    }

    private List<Diagnostic> diagnostics() {
        return textArea.getDiagnostics();
    }

    private int trackTop() {
        return statusIndicatorEnabled ? statusIndicatorHeight + 2 : 2;
    }

    private int trackHeight() {
        return Math.max(1, getHeight() - trackTop() - 2);
    }

    private int markerY(Diagnostic d) {
        int total = textArea.getPreferredSize().height;
        if (total <= 0) return trackTop();
        int line = Math.max(0, Math.min(d.startLine(), textArea.getBuffer().lineCount() - 1));
        float ratio = (float) textArea.yOfBufferLine(line) / total;
        return trackTop() + Math.round(ratio * trackHeight());
    }

    private DiagnosticSeverity worstSeverity() {
        DiagnosticSeverity worst = null;
        for (Diagnostic d : diagnostics()) {
            DiagnosticSeverity s = d.severity();
            if (s == null) continue;
            if (worst == null || s.ordinal() < worst.ordinal()) worst = s;
        }
        return worst;
    }

    private Diagnostic markerAt(int y) {
        Diagnostic best = null;
        int bestDist = Integer.MAX_VALUE;
        int tol = clickTolerance + markerHeight;
        for (Diagnostic d : diagnostics()) {
            int my = markerY(d);
            int dist = Math.abs(my - y);
            if (dist <= tol && dist < bestDist) {
                bestDist = dist;
                best = d;
            }
        }
        return best;
    }

    private void handleClick(MouseEvent e) {
        Diagnostic d = markerAt(e.getY());
        if (d == null) return;
        int line = Math.max(0, Math.min(d.startLine(), textArea.getBuffer().lineCount() - 1));
        int col = Math.max(0, d.startCol());

        if (navigateOnClick) {
            textArea.setCaretPosition(line, col);
            textArea.requestFocusInWindow();
        }

        ErrorStripeClickEvent event = new ErrorStripeClickEvent(d, line, e);
        for (ErrorStripeClickListener listener : clickListeners) {
            listener.onMarkerClicked(event);
        }
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Diagnostic d = markerAt(e.getY());
        if (d == null) return null;
        int line = Math.max(0, d.startLine()) + 1;
        String msg = d.message() != null ? d.message() : "";
        return "<html><b>" + d.severity() + "</b> (linha " + line + ")<br>" + escape(msg) + "</html>";
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color editorBg = textArea.getDefaultStyle().getBackground();
        boolean dark = (editorBg.getRed() * 299 + editorBg.getGreen() * 587 + editorBg.getBlue() * 114) / 1000 < 128;
        int delta = dark ? 12 : -10;
        Color bg = new Color(
                clamp255(editorBg.getRed() + delta),
                clamp255(editorBg.getGreen() + delta),
                clamp255(editorBg.getBlue() + delta));
        g2.setColor(bg);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(new Color(128, 128, 128, 80));
        g2.drawLine(0, 0, 0, getHeight());

        if (statusIndicatorEnabled) paintStatusIndicator(g2);

        if (viewportIndicatorEnabled) paintViewportIndicator(g2);

        int x = 2;
        int w = Math.max(1, getWidth() - 4);
        int h = Math.max(2, markerHeight);
        for (Diagnostic d : diagnostics()) {
            int y = markerY(d) - h / 2;
            g2.setColor(d.effectiveColor());
            g2.fillRect(x, y, w, h);
        }

        g2.dispose();
    }

    private void paintStatusIndicator(Graphics2D g2) {
        int size = Math.min(statusIndicatorHeight, getWidth());
        int x = (getWidth() - size) / 2;
        int y = (statusIndicatorHeight - size) / 2 + 1;
        DiagnosticSeverity worst = worstSeverity();
        Color color = worst != null ? worst.defaultColor : okColor;
        g2.setColor(color);
        g2.fillRoundRect(x, y, size, size, 4, 4);
    }

    private void paintViewportIndicator(Graphics2D g2) {
        if (scrollPane == null) return;
        JViewport vp = scrollPane.getViewport();
        if (vp == null) return;
        int total = textArea.getPreferredSize().height;
        if (total <= vp.getHeight() || total <= 0) return;

        float topRatio = (float) vp.getViewPosition().y / total;
        float hRatio = (float) vp.getHeight() / total;
        int y = trackTop() + Math.round(topRatio * trackHeight());
        int h = Math.max(4, Math.round(hRatio * trackHeight()));
        g2.setColor(viewportIndicatorColor);
        g2.fillRect(1, y, getWidth() - 2, h);
    }

    private static int clamp255(int v) {
        return Math.max(0, Math.min(255, v));
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(stripeWidth, super.getPreferredSize().height);
    }
}
