package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.controller.PasteOptions;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.BorderLayout;
import java.awt.GridLayout;

public class PasteSpecialPanel extends JPanel {
    private static final String[] WHAT = {"Tudo", "Fórmulas", "Valores", "Formatos", "Comentários e anotações", "Validação", "Tudo, exceto bordas", "Larguras da coluna",
            "Fórmulas e formatos de número", "Valores e formatos de número"};
    private static final String[] OPS = {"Nenhuma", "Adição", "Subtração", "Multiplicação", "Divisão"};
    private final JRadioButton[] what = new JRadioButton[WHAT.length];
    private final JRadioButton[] ops = new JRadioButton[OPS.length];
    private final JCheckBox skipBlanks = new JCheckBox("Ignorar em branco"), transpose = new JCheckBox("Transpor"), link = new JCheckBox("Colar vínculo");

    public PasteSpecialPanel() {
        super(new BorderLayout(0, 8));
        JPanel w = new JPanel(new GridLayout(0, 2, 4, 2));
        w.setBorder(BorderFactory.createTitledBorder("Colar"));
        ButtonGroup g1 = new ButtonGroup();
        for (int i = 0; i < WHAT.length; i++) { what[i] = new JRadioButton(WHAT[i], i == 0); g1.add(what[i]); w.add(what[i]); }
        JPanel o = new JPanel(new GridLayout(0, 3, 4, 2));
        o.setBorder(BorderFactory.createTitledBorder("Operação"));
        ButtonGroup g2 = new ButtonGroup();
        for (int i = 0; i < OPS.length; i++) { ops[i] = new JRadioButton(OPS[i], i == 0); g2.add(ops[i]); o.add(ops[i]); }
        JPanel extra = new JPanel(new GridLayout(1, 3, 4, 0));
        extra.add(skipBlanks);
        extra.add(transpose);
        extra.add(link);
        add(w, BorderLayout.NORTH);
        add(o, BorderLayout.CENTER);
        add(extra, BorderLayout.SOUTH);
    }

    public PasteOptions result() {
        int wi = 0, oi = 0;
        for (int i = 0; i < what.length; i++) if (what[i].isSelected()) wi = i;
        for (int i = 0; i < ops.length; i++) if (ops[i].isSelected()) oi = i;
        return new PasteOptions(PasteOptions.What.values()[wi], PasteOptions.Operation.values()[oi], skipBlanks.isSelected(), transpose.isSelected(), link.isSelected());
    }
}
