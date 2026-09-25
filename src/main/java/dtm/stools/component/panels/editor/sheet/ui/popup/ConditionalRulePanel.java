package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.model.ComparisonOperator;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRule;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRuleType;
import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;
import dtm.stools.component.panels.editor.sheet.model.IconSetType;
import dtm.stools.component.panels.editor.sheet.model.TimePeriod;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.function.Function;

public class ConditionalRulePanel extends JPanel {
    private static final String[] KINDS = {"Formatar apenas células que contêm (valor)", "Formatar apenas células que contêm texto", "Formatar datas que ocorrem",
            "Formatar apenas os primeiros ou últimos valores", "Formatar valores acima ou abaixo da média", "Formatar valores duplicados ou exclusivos",
            "Usar uma fórmula para determinar quais células devem ser formatadas", "Escala de 2 cores", "Escala de 3 cores", "Barra de dados", "Conjunto de ícones",
            "Células em branco", "Células com erros"};
    private static final String[] OPS = {"está entre", "não está entre", "é igual a", "é diferente de", "é maior do que", "é menor do que", "é maior ou igual a", "é menor ou igual a"};
    private static final String[] TEXT_OPS = {"contém", "não contém", "começa com", "termina com"};
    private static final ConditionalRuleType[] TEXT_TYPES = {ConditionalRuleType.CONTAINS_TEXT, ConditionalRuleType.NOT_CONTAINS_TEXT, ConditionalRuleType.BEGINS_WITH, ConditionalRuleType.ENDS_WITH};
    private static final String[] PRESETS = {"Preenchimento Vermelho Claro e Texto Vermelho Escuro", "Preenchimento Amarelo e Texto Amarelo Escuro", "Preenchimento Verde e Texto Verde Escuro",
            "Preenchimento Vermelho Claro", "Texto Vermelho", "Borda Vermelha", "Negrito"};

    private final JComboBox<String> kind = SheetForm.combo(KINDS);
    private final JComboBox<String> operator = SheetForm.combo(OPS);
    private final JComboBox<String> textOp = SheetForm.combo(TEXT_OPS);
    private final JComboBox<TimePeriod> period = new JComboBox<>(TimePeriod.values());
    private final JTextField v1 = SheetForm.text("", 18), v2 = SheetForm.text("", 18);
    private final JSpinner rank = SheetForm.integer(10, 1, 1000);
    private final JCheckBox percent = SheetForm.check("% do intervalo selecionado", false), bottom = SheetForm.check("Últimos / abaixo / exclusivos", false);
    private final JComboBox<String> preset = SheetForm.combo(PRESETS);
    private final ColorButton color1 = new ColorButton(0xFFF8696B, "Automático"), color2 = new ColorButton(0xFFFFEB84, "Automático"), color3 = new ColorButton(0xFF63BE7B, "Automático");
    private final JComboBox<IconSetType> icons = new JComboBox<>(IconSetType.values());
    private final Function<String, String> toCanonical;

    public ConditionalRulePanel(Function<String, String> toCanonical) {
        super(new BorderLayout());
        this.toCanonical = toCanonical;
        SheetForm f = new SheetForm();
        f.add("Tipo de regra:", kind);
        f.add("Operador:", operator);
        f.add("Texto:", textOp);
        f.add("Período:", period);
        f.add("Valor / fórmula:", v1);
        f.add("E:", v2);
        f.add("Classificação:", rank);
        f.full(percent);
        f.full(bottom);
        f.add("Formato:", preset);
        f.add("Cor mínima / barra:", color1);
        f.add("Cor do ponto médio:", color2);
        f.add("Cor máxima:", color3);
        f.add("Ícones:", icons);
        add(f, BorderLayout.CENTER);
        setPreferredSize(new Dimension(560, 440));
        kind.addActionListener(e -> sync());
        sync();
    }

    private void sync() {
        int k = kind.getSelectedIndex();
        operator.setEnabled(k == 0);
        textOp.setEnabled(k == 1);
        period.setEnabled(k == 2);
        v1.setEnabled(k == 0 || k == 1 || k == 6);
        v2.setEnabled(k == 0);
        rank.setEnabled(k == 3);
        percent.setEnabled(k == 3);
        bottom.setEnabled(k == 3 || k == 4 || k == 5);
        preset.setEnabled(k <= 6 || k >= 11);
        color1.setEnabled(k >= 7 && k <= 9);
        color2.setEnabled(k == 8);
        color3.setEnabled(k == 7 || k == 8);
        icons.setEnabled(k == 10);
    }

    private DifferentialStyle style() {
        return switch (preset.getSelectedIndex()) {
            case 0 -> DifferentialStyle.LIGHT_RED;
            case 1 -> DifferentialStyle.YELLOW;
            case 2 -> DifferentialStyle.GREEN;
            case 3 -> new DifferentialStyle(null, null, null, null, null, 0xFFFFC7CE, null, null);
            case 4 -> new DifferentialStyle(0xFF9C0006, null, null, null, null, null, null, null);
            case 5 -> new DifferentialStyle(null, null, null, null, null, null, 0xFFC00000, null);
            default -> new DifferentialStyle(null, true, null, null, null, null, null, null);
        };
    }

    private String value(JTextField f) {
        String t = f.getText().strip();
        if (t.isEmpty()) throw new IllegalArgumentException("Informe o valor da regra.");
        return toCanonical.apply(t);
    }

    public ConditionalRule result() {
        int k = kind.getSelectedIndex();
        return switch (k) {
            case 0 -> {
                ComparisonOperator op = ComparisonOperator.values()[operator.getSelectedIndex()];
                boolean two = op == ComparisonOperator.BETWEEN || op == ComparisonOperator.NOT_BETWEEN;
                yield ConditionalRule.cellValue(op, value(v1), two ? value(v2) : null, style());
            }
            case 1 -> {
                if (v1.getText().isBlank()) throw new IllegalArgumentException("Informe o texto.");
                yield ConditionalRule.text(TEXT_TYPES[textOp.getSelectedIndex()], v1.getText().strip(), style());
            }
            case 2 -> ConditionalRule.simple(ConditionalRuleType.TIME_PERIOD, style()).withTimePeriod((TimePeriod) period.getSelectedItem());
            case 3 -> ConditionalRule.top(SheetForm.integer(rank), percent.isSelected(), bottom.isSelected(), style());
            case 4 -> ConditionalRule.simple(ConditionalRuleType.ABOVE_AVERAGE, style()).withBelow(bottom.isSelected());
            case 5 -> ConditionalRule.simple(bottom.isSelected() ? ConditionalRuleType.UNIQUE : ConditionalRuleType.DUPLICATE, style());
            case 6 -> {
                String t = v1.getText().strip();
                if (t.isEmpty()) throw new IllegalArgumentException("Informe a fórmula.");
                yield ConditionalRule.expression(toCanonical.apply(t.startsWith("=") ? t : "=" + t), style());
            }
            case 7 -> ConditionalRule.twoColorScale(c(color1, 0xFFF8696B), c(color3, 0xFF63BE7B));
            case 8 -> ConditionalRule.threeColorScale(c(color1, 0xFFF8696B), c(color2, 0xFFFFEB84), c(color3, 0xFF63BE7B));
            case 9 -> ConditionalRule.dataBar(c(color1, 0xFF638EC6), true);
            case 10 -> ConditionalRule.iconSet((IconSetType) icons.getSelectedItem());
            case 11 -> ConditionalRule.simple(ConditionalRuleType.BLANKS, style());
            default -> ConditionalRule.simple(ConditionalRuleType.ERRORS, style());
        };
    }

    private static int c(ColorButton b, int fallback) { return b.color() == null ? fallback : b.color(); }
}
