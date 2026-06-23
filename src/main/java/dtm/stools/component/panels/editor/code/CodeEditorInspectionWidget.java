package dtm.stools.component.panels.editor.code;

import dtm.stools.component.panels.editor.code.diagnostics.Diagnostic;
import dtm.stools.component.panels.editor.code.diagnostics.DiagnosticSeverity;
import dtm.stools.component.panels.editor.code.diagnostics.InspectionWidgetClickEvent;
import dtm.stools.component.panels.editor.code.diagnostics.InspectionWidgetClickListener;
import dtm.stools.component.panels.editor.code.listeners.DiagnosticsChangeListener;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Widget de inspeção (estilo IntelliJ) exibido no canto superior direito do
 * editor. Mostra apenas as contagens de <b>erros</b> e <b>warnings</b>,
 * alimentadas pelo {@code DiagnosticsProvider} via {@link CodeEditorTextArea}.
 * Possui flag de habilitado (default ligado) e dispara um evento de clique que,
 * por padrão, navega até o primeiro erro (ou warning).
 */
public class CodeEditorInspectionWidget extends JComponent {

    /**
     * Comportamento do widget quando não há erros nem warnings.
     */
    public enum CleanMode {
        /** Esconde o widget até receber algum diagnóstico (default). */
        HIDE,
        /** Mantém o widget visível com um "tique" verde de "tudo certo". */
        SHOW_OK
    }

    private final CodeEditorTextArea textArea;

    @Getter
    private boolean enabled = true;

    @Getter
    @Setter
    private boolean navigateOnClick = true;

    /** O que fazer quando não há diagnósticos. Default {@link CleanMode#HIDE}. */
    @Getter
    private CleanMode cleanMode = CleanMode.HIDE;

    @Getter
    @Setter
    private Color okColor = new Color(110, 180, 110);

    @Getter
    @Setter
    private int iconSize = 11;

    @Getter
    @Setter
    private int hpad = 8;

    @Getter
    @Setter
    private int vpad = 3;

    @Getter
    @Setter
    private int gap = 10;

    /** Distância (px) entre o topo do editor e o widget. */
    @Getter
    @Setter
    private int marginTop = 6;

    /** Distância (px) entre a borda direita (scrollbar) e o widget. */
    @Getter
    @Setter
    private int marginRight = 12;

    private int errorCount;
    private int warningCount;

    private final CopyOnWriteArrayList<InspectionWidgetClickListener> clickListeners = new CopyOnWriteArrayList<>();

    private final DiagnosticsChangeListener diagnosticsListener = this::recount;

    public CodeEditorInspectionWidget(CodeEditorTextArea textArea) {
        this.textArea = textArea;
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setFont(getFont() != null ? getFont().deriveFont(Font.BOLD, 11f)
                : new Font(Font.SANS_SERIF, Font.BOLD, 11));

        recount(textArea.getDiagnostics());
        textArea.addDiagnosticsChangeListener(diagnosticsListener);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (!enabled) return;
                handleClick(e);
            }
        });
        ToolTipManager.sharedInstance().registerComponent(this);
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;
        updateVisibility();
        revalidate();
        repaint();
    }

    public void setCleanMode(CleanMode cleanMode) {
        if (cleanMode == null || this.cleanMode == cleanMode) return;
        this.cleanMode = cleanMode;
        updateVisibility();
        revalidate();
        Container parent = getParent();
        if (parent != null) {
            parent.revalidate();
            parent.repaint();
        }
        repaint();
    }

    /** Decide se o widget deve estar visível conforme habilitado/limpo/modo. */
    protected void updateVisibility() {
        boolean shouldShow = enabled && (!isClean() || cleanMode == CleanMode.SHOW_OK);
        if (isVisible() != shouldShow) {
            setVisible(shouldShow);
            Container parent = getParent();
            if (parent != null) {
                parent.revalidate();
                parent.repaint();
            }
        }
    }

    public void addClickListener(InspectionWidgetClickListener listener) {
        if (listener != null) clickListeners.add(listener);
    }

    public void removeClickListener(InspectionWidgetClickListener listener) {
        if (listener != null) clickListeners.remove(listener);
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    private void recount(List<Diagnostic> diagnostics) {
        int errors = 0;
        int warnings = 0;
        for (Diagnostic d : diagnostics) {
            if (d.severity() == DiagnosticSeverity.ERROR) errors++;
            else if (d.severity() == DiagnosticSeverity.WARNING) warnings++;
        }
        this.errorCount = errors;
        this.warningCount = warnings;
        updateVisibility();
        revalidate();
        // o overlay (JLayeredPane) precisa reposicionar quando o tamanho muda
        Container parent = getParent();
        if (parent != null) parent.revalidate();
        repaint();
    }

    private boolean isClean() {
        return errorCount == 0 && warningCount == 0;
    }

    private void handleClick(MouseEvent e) {
        if (navigateOnClick) navigateToFirst();
        InspectionWidgetClickEvent event = new InspectionWidgetClickEvent(collectDiagnostics(), e);
        for (InspectionWidgetClickListener listener : clickListeners) {
            listener.onWidgetClicked(event);
        }
    }

    /** Diagnósticos representados pelo widget (apenas erros e warnings). */
    protected List<Diagnostic> collectDiagnostics() {
        List<Diagnostic> out = new ArrayList<>();
        for (Diagnostic d : textArea.getDiagnostics()) {
            if (d.severity() == DiagnosticSeverity.ERROR || d.severity() == DiagnosticSeverity.WARNING) {
                out.add(d);
            }
        }
        return List.copyOf(out);
    }

    private void navigateToFirst() {
        Diagnostic target = firstOf(DiagnosticSeverity.ERROR);
        if (target == null) target = firstOf(DiagnosticSeverity.WARNING);
        if (target == null) return;
        int line = Math.max(0, Math.min(target.startLine(), textArea.getBuffer().lineCount() - 1));
        textArea.setCaretPosition(line, Math.max(0, target.startCol()));
        textArea.requestFocusInWindow();
    }

    private Diagnostic firstOf(DiagnosticSeverity severity) {
        Diagnostic best = null;
        for (Diagnostic d : textArea.getDiagnostics()) {
            if (d.severity() != severity) continue;
            if (best == null || d.startLine() < best.startLine()) best = d;
        }
        return best;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (isClean()) return "Sem erros ou warnings";
        return errorCount + " erro(s), " + warningCount + " warning(s)";
    }

    /**
     * Copia as características comportamentais (estado de habilitado, navegação e
     * ouvintes de clique já registrados) de um widget anterior. Usado ao trocar a
     * implementação visual via {@code CodeEditor.setInspectionWidget(...)} para que
     * o usuário possa mudar só a aparência sem perder o comportamento.
     */
    protected void inheritStateFrom(CodeEditorInspectionWidget previous) {
        if (previous == null || previous == this) return;
        this.enabled = previous.enabled;
        this.navigateOnClick = previous.navigateOnClick;
        this.cleanMode = previous.cleanMode;
        this.clickListeners.addAll(previous.clickListeners);
        updateVisibility();
    }

    /** Desconecta o widget dos diagnósticos do editor. Chamado ao ser substituído. */
    public void dispose() {
        textArea.removeDiagnosticsChangeListener(diagnosticsListener);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        paintWidgetBackground(g2);

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        int cy = getHeight() / 2;

        if (isClean()) {
            if (cleanMode == CleanMode.SHOW_OK) {
                paintCheck(g2, hpad, cy);
            }
            g2.dispose();
            return;
        }

        int x = hpad;
        x = paintGroup(g2, fm, x, cy, DiagnosticSeverity.ERROR, errorCount);
        x += gap;
        paintGroup(g2, fm, x, cy, DiagnosticSeverity.WARNING, warningCount);

        g2.dispose();
    }

    /** Desenha o fundo do widget. Sobrescreva para mudar a aparência da "pílula". */
    protected void paintWidgetBackground(Graphics2D g2) {
        Color editorBg = textArea.getDefaultStyle().getBackground();
        g2.setColor(new Color(editorBg.getRed(), editorBg.getGreen(), editorBg.getBlue(), 200));
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        g2.setColor(new Color(128, 128, 128, 70));
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
    }

    protected int paintGroup(Graphics2D g2, FontMetrics fm, int x, int cy,
                           DiagnosticSeverity severity, int count) {
        if (severity == DiagnosticSeverity.ERROR) {
            paintErrorIcon(g2, x, cy, severity.defaultColor);
        } else {
            paintWarningIcon(g2, x, cy, severity.defaultColor);
        }
        x += iconSize + 4;

        String text = Integer.toString(count);
        Color fg = textArea.getDefaultStyle().getForeground();
        g2.setColor(fg != null ? fg : Color.LIGHT_GRAY);
        g2.drawString(text, x, cy + fm.getAscent() / 2 - 1);
        x += fm.stringWidth(text);
        return x;
    }

    protected void paintErrorIcon(Graphics2D g2, int x, int cy, Color color) {
        int s = iconSize;
        int y = cy - s / 2;
        g2.setColor(color);
        g2.fillOval(x, y, s, s);
        // sinal de exclamação branco
        g2.setColor(Color.WHITE);
        int cx = x + s / 2;
        g2.fillRect(cx - 1, y + 2, 2, s - 6);
        g2.fillRect(cx - 1, y + s - 3, 2, 2);
    }

    protected void paintWarningIcon(Graphics2D g2, int x, int cy, Color color) {
        int s = iconSize;
        int y = cy - s / 2;
        int[] xs = {x, x + s, x + s / 2};
        int[] ys = {y + s, y + s, y};
        g2.setColor(color);
        g2.fillPolygon(xs, ys, 3);
        // sinal de exclamação escuro
        g2.setColor(new Color(40, 40, 40));
        int cx = x + s / 2;
        g2.fillRect(cx - 1, y + 4, 2, s - 7);
        g2.fillRect(cx - 1, y + s - 2, 2, 2);
    }

    protected void paintCheck(Graphics2D g2, int x, int cy) {
        int s = iconSize;
        int y = cy - s / 2;
        // badge verde preenchido com o "tique" branco — bem visível, nunca "some"
        g2.setColor(okColor);
        g2.fillOval(x, y, s, s);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawPolyline(
                new int[]{x + 3, x + s / 2, x + s - 2},
                new int[]{y + s / 2, y + s - 3, y + 2},
                3);
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int h = Math.max(iconSize, fm.getHeight()) + vpad * 2;
        if (isClean()) {
            return new Dimension(hpad * 2 + iconSize, h);
        }
        int w = hpad * 2
                + iconSize + 4 + fm.stringWidth(Integer.toString(errorCount))
                + gap
                + iconSize + 4 + fm.stringWidth(Integer.toString(warningCount));
        return new Dimension(w, h);
    }
}
