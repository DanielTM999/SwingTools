package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.math.WordMath;
import dtm.stools.component.panels.editor.word.math.WordMathLayout;
import dtm.stools.component.panels.editor.word.math.WordMathParser;
import dtm.stools.component.panels.editor.word.model.WordEquation;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public final class WordEquationEditorPanel extends WordPropertiesPanel<WordEquation> {
    private static final String[][] TEMPLATES = {{"a/b","Fração"},{"x^2","Sobrescrito"},{"x_i","Subscrito"},{"√(x)","Raiz"},{"√(3&x)","Raiz n-ésima"},{"∑_(i=1)^n i","Somatório"},
            {"∫_0^1 f(x)dx","Integral"},{"lim_(x→0) f(x)","Limite"},{"(a+b)","Parênteses"},{"hat(x)","Acento"},{"sin θ","Função"},{"α β π ∞ ≤ ≥ ≠ ±","Símbolos"}};
    private final WordEquation equation;
    private final JTextField linear = new JTextField(28);
    private final JCheckBox display = new JCheckBox("Equação em destaque (linha própria)");
    private final JSpinner size;
    private final JLabel error = new JLabel(" ");
    private WordMath current;
    private final JComponent preview = new JComponent() {
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D)g.create();
            try {
                g2.setColor(Color.WHITE); g2.fillRect(0,0,getWidth(),getHeight());
                if (current == null) return;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                float s = 22; WordMathLayout.Box box = WordMathLayout.measure(current,s);
                float scale = Math.min(1,Math.min((getWidth()-16)/Math.max(1,box.width()),(getHeight()-8)/Math.max(1,box.height())));
                g2.translate((getWidth()-box.width()*scale)/2,(getHeight()-box.height()*scale)/2); g2.scale(scale,scale);
                g2.setColor(Color.BLACK); WordMathLayout.paint(g2,current,s,0,box.ascent());
            } finally { g2.dispose(); }
        }
    };

    public WordEquationEditorPanel(WordEquation equation) {
        this.equation = equation; this.current = equation.math();
        linear.setText(equation.linear()); row("Entrada linear",linear);
        JPanel templates = new JPanel(new GridLayout(0,4,4,4)); templates.setOpaque(false);
        for (String[] t : TEMPLATES) {
            JButton b = new JButton(t[1]); b.setToolTipText(t[0]);
            b.addActionListener(e -> { int at = linear.getCaretPosition(); String v = linear.getText(); linear.setText(v.substring(0,at) + t[0] + v.substring(at)); linear.requestFocusInWindow(); });
            templates.add(b);
        }
        wide(templates,0);
        preview.setPreferredSize(new Dimension(420,120)); preview.setBorder(BorderFactory.createLineBorder(new Color(0xD1D5DB)));
        wide(preview,1);
        display.setSelected(equation.display()); display.setOpaque(false); row(null,display);
        size = row("Tamanho (pt)",number(equation.fontSize(),4,200,1));
        error.setForeground(new Color(0xB91C1C)); wide(error,0);
        linear.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
        });
    }
    private void update() {
        try { current = WordMathParser.parse(linear.getText()); error.setText(" "); } catch (IllegalArgumentException e) { error.setText(e.getMessage()); }
        preview.repaint();
    }
    @Override public String title() { return "Editor de equação"; }
    @Override public WordEquation result() {
        WordMath math = WordMathParser.parse(linear.getText());
        return new WordEquation(equation.id(),math,display.isSelected(),value(size));
    }
}
