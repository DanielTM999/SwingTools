package dtm.stools.component.panels.scroll;

import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.Color;

/**
 * Painel rolável com barras finas e modernas, sem botões de seta.
 */
public class ScrollPanel extends JScrollPane {

    private int thickness = UiTokens.scale(10);
    private boolean paintTrack;

    public ScrollPanel() {
        this(null);
    }

    public ScrollPanel(JComponent view) {
        super(view);
        setOpaque(false);
        getViewport().setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        applyScrollBars();
    }

    /**
     * Define a espessura das barras de rolagem.
     */
    public ScrollPanel setScrollBarThickness(int thickness) {
        if (thickness <= 0) {
            throw new IllegalArgumentException("thickness must be greater than zero");
        }
        this.thickness = thickness;
        applyScrollBars();
        return this;
    }

    /**
     * Habilita a pintura do trilho das barras.
     */
    public ScrollPanel setPaintTrack(boolean paintTrack) {
        this.paintTrack = paintTrack;
        applyScrollBars();
        return this;
    }

    /**
     * Define as cores do polegar e do trilho das barras.
     */
    public ScrollPanel setScrollBarColors(Color thumbColor, Color trackColor) {
        applyUi(getVerticalScrollBar(), thumbColor, trackColor);
        applyUi(getHorizontalScrollBar(), thumbColor, trackColor);
        return this;
    }

    /**
     * Define o incremento aplicado a cada passo de rolagem.
     */
    public ScrollPanel setUnitIncrement(int increment) {
        if (increment <= 0) {
            throw new IllegalArgumentException("increment must be greater than zero");
        }
        getVerticalScrollBar().setUnitIncrement(increment);
        getHorizontalScrollBar().setUnitIncrement(increment);
        return this;
    }

    /**
     * Exibe ou oculta a barra de rolagem horizontal.
     */
    public ScrollPanel setHorizontalScrollEnabled(boolean enabled) {
        setHorizontalScrollBarPolicy(enabled
                ? ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED
                : ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return this;
    }

    private void applyScrollBars() {
        applyUi(getVerticalScrollBar(), null, null);
        applyUi(getHorizontalScrollBar(), null, null);
        setUnitIncrement(UiTokens.scale(16));
    }

    private void applyUi(JScrollBar scrollBar, Color thumbColor, Color trackColor) {
        if (scrollBar == null) {
            return;
        }
        ModernScrollBarUI ui = new ModernScrollBarUI(thickness, paintTrack);
        if (thumbColor != null || trackColor != null) {
            ui.setColors(thumbColor, trackColor);
        }
        scrollBar.setUI(ui);
        scrollBar.setOpaque(false);
        scrollBar.setBorder(BorderFactory.createEmptyBorder());
    }
}
