package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.WordPlacement;
import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public abstract class WordPropertiesPanel<T> extends JPanel implements Scrollable {
    private int row;

    protected WordPropertiesPanel() {
        super(new GridBagLayout());
        setOpaque(false);
    }
    public abstract T result();
    public String title() { return "Propriedades"; }
    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(24,visible.height-24); }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }

    protected <C extends JComponent> C row(String label, C component) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row++; g.insets = new Insets(5,0,5,12); g.anchor = GridBagConstraints.WEST;
        if (label != null) { g.gridx = 0; JLabel fieldLabel=new JLabel(label);fieldLabel.setLabelFor(component);component.getAccessibleContext().setAccessibleName(label);add(fieldLabel,g); }
        g.gridx = label == null ? 0 : 1; g.gridwidth = label == null ? 2 : 1; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        add(component,g);
        return component;
    }
    protected <C extends JComponent> C wide(C component, double weighty) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = row++; g.gridx = 0; g.gridwidth = 2; g.weightx = 1; g.weighty = weighty; g.fill = GridBagConstraints.BOTH; g.insets = new Insets(4,0,4,0);
        add(component,g);
        return component;
    }
    protected static JSpinner number(double value, double min, double max, double step) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(Math.max(min,Math.min(max,value)),min,max,step));
        spinner.setPreferredSize(new Dimension(90,spinner.getPreferredSize().height));
        return spinner;
    }
    protected static float value(JSpinner spinner) { return ((Number)spinner.getValue()).floatValue(); }

    protected Supplier<WordPlacement> placement(WordPlacement current) {
        JComboBox<String> wrap = row("Disposição",new JComboBox<>(new String[]{"Alinhado com o texto","Quadrado","Justo","Superior e inferior","Atrás do texto","Na frente do texto"}));
        wrap.setSelectedIndex(current.wrap().ordinal());
        JSpinner x = row("Posição horizontal (pt)",number(current.x(),-1000,14400,1)), y = row("Posição vertical (pt)",number(current.y(),-1000,14400,1));
        Runnable sync = () -> { boolean floating = wrap.getSelectedIndex() > 0; x.setEnabled(floating); y.setEnabled(floating); };
        wrap.addActionListener(e -> sync.run()); sync.run();
        return () -> {
            WordPlacement.Wrap w = WordPlacement.Wrap.values()[wrap.getSelectedIndex()];
            if (w == WordPlacement.Wrap.INLINE) return WordPlacement.INLINE;
            WordPlacement base = current.floating() ? current : WordPlacement.floating(0,0,w);
            return base.withWrap(w).moveTo(value(x),value(y));
        };
    }

    public static final class ColorButton extends JButton {
        private Integer color;
        private final boolean optional;
        public ColorButton(Integer initial, boolean optional) {
            this.color = initial; this.optional = optional;
            setPreferredSize(new Dimension(110,26));
            addActionListener(e -> {
                WordColors.show(this,"Cor",color == null ? Color.WHITE : new Color(color),
                        chosen -> { color = chosen.getRGB() & 0xffffff; refresh(); }, optional ? this::clear : null);
            });
            refresh();
        }
        public Integer color() { return color; }
        public void clear() { if (optional) { color = null; refresh(); } }
        private void refresh() {
            setText(color == null ? "Nenhuma" : String.format("#%06X",color));
            setIcon(new Icon() {
                public int getIconWidth() { return 14; }
                public int getIconHeight() { return 14; }
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    if (color == null) { g.setColor(Color.GRAY); g.drawRect(x,y,13,13); g.drawLine(x,y+13,x+13,y); }
                    else { g.setColor(new Color(color)); g.fillRect(x,y,14,14); g.setColor(Color.DARK_GRAY); g.drawRect(x,y,13,13); }
                }
            });
        }
        public JComponent withClear() {
            if (!optional) return this;
            JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING,0,0)); p.setOpaque(false);
            JButton none = new JButton("Sem cor"); none.addActionListener(e -> clear());
            p.add(this); p.add(Box.createHorizontalStrut(4)); p.add(none);
            return p;
        }
    }
}
