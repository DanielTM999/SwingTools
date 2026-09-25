package dtm.stools.component.panels.editor.sheet.ui.popup;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.GridLayout;

public class GoToSpecialPanel extends JPanel {
    public enum Kind { NOTES, CONSTANTS, FORMULAS, BLANKS, CURRENT_REGION, CURRENT_ARRAY, OBJECTS, PRECEDENTS, DEPENDENTS, LAST_CELL, VISIBLE, CONDITIONAL, VALIDATION, ERRORS }

    private static final String[] LABELS = {"Comentários e anotações", "Constantes", "Fórmulas", "Em branco", "Região atual", "Matriz atual", "Objetos", "Precedentes", "Dependentes",
            "Última célula", "Somente células visíveis", "Formatos condicionais", "Validação de dados", "Erros"};
    private final JRadioButton[] buttons = new JRadioButton[LABELS.length];

    public GoToSpecialPanel() {
        super(new GridLayout(0, 2, 6, 2));
        ButtonGroup g = new ButtonGroup();
        for (int i = 0; i < LABELS.length; i++) { buttons[i] = new JRadioButton(LABELS[i], i == 1); g.add(buttons[i]); add(buttons[i]); }
    }

    public Kind result() {
        for (int i = 0; i < buttons.length; i++) if (buttons[i].isSelected()) return Kind.values()[i];
        return Kind.CONSTANTS;
    }
}
