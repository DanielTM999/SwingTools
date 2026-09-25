package dtm.stools.component.panels.editor.sheet.controller;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.formula.Formulas;
import dtm.stools.component.panels.editor.sheet.model.CellAddress;
import dtm.stools.component.panels.editor.sheet.model.CellValue;
import dtm.stools.component.panels.editor.sheet.model.ComparisonOperator;
import dtm.stools.component.panels.editor.sheet.model.ConditionalFormat;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRule;
import dtm.stools.component.panels.editor.sheet.model.ConditionalRuleType;
import dtm.stools.component.panels.editor.sheet.model.DifferentialStyle;
import dtm.stools.component.panels.editor.sheet.model.IconSetType;
import dtm.stools.component.panels.editor.sheet.model.NumberValue;
import dtm.stools.component.panels.editor.sheet.model.TimePeriod;
import dtm.stools.component.panels.editor.sheet.ui.popup.ConditionalRulePanel;
import dtm.stools.component.panels.editor.sheet.ui.popup.SheetForm;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public final class ConditionalController {
    private final SheetEditor editor;

    public ConditionalController(SheetEditor editor) { this.editor = editor; }

    private CellAddress host() { return editor.getSelection().range().first(); }

    public String criterion(String text) {
        String t = text.strip();
        if (t.startsWith("=")) return editor.editing().canonicalize(t, host());
        CellValue v = editor.valueParser().parse(t).value();
        if (v instanceof NumberValue n) return NumberValue.general(n.value());
        return "\"" + t.replace("\"", "\"\"") + "\"";
    }

    private static final String[] PRESETS = {"Preenchimento Vermelho Claro e Texto Vermelho Escuro", "Preenchimento Amarelo e Texto Amarelo Escuro", "Preenchimento Verde e Texto Verde Escuro"};
    private static final DifferentialStyle[] STYLES = {DifferentialStyle.LIGHT_RED, DifferentialStyle.YELLOW, DifferentialStyle.GREEN};

    private void valueRule(String title, ComparisonOperator op) {
        boolean two = op == ComparisonOperator.BETWEEN;
        JTextField a = SheetForm.text("", 14), b = SheetForm.text("", 14);
        JComboBox<String> preset = SheetForm.combo(PRESETS);
        SheetForm f = new SheetForm();
        f.add(two ? "Entre:" : "Valor:", a);
        if (two) f.add("e:", b);
        f.add("com:", preset);
        editor.popups().dialog("sheet.cf.quick", title, f, () -> ConditionalRule.cellValue(op, criterion(a.getText()), two ? criterion(b.getText()) : null, STYLES[preset.getSelectedIndex()]))
                .ifPresent(editor.format()::addConditionalRule);
    }

    public void quick(String kind) {
        switch (kind) {
            case "greater" -> valueRule("É Maior do que", ComparisonOperator.GREATER);
            case "less" -> valueRule("É Menor do que", ComparisonOperator.LESS);
            case "between" -> valueRule("Está Entre", ComparisonOperator.BETWEEN);
            case "equal" -> valueRule("É Igual a", ComparisonOperator.EQUAL);
            case "text" -> {
                JTextField a = SheetForm.text("", 18);
                JComboBox<String> preset = SheetForm.combo(PRESETS);
                SheetForm f = new SheetForm();
                f.add("Texto que contém:", a);
                f.add("com:", preset);
                editor.popups().dialog("sheet.cf.text", "Texto que Contém", f, () -> ConditionalRule.text(ConditionalRuleType.CONTAINS_TEXT, a.getText().strip(), STYLES[preset.getSelectedIndex()]))
                        .ifPresent(editor.format()::addConditionalRule);
            }
            case "date" -> {
                JComboBox<TimePeriod> period = new JComboBox<>(TimePeriod.values());
                JComboBox<String> preset = SheetForm.combo(PRESETS);
                SheetForm f = new SheetForm();
                f.add("Data:", period);
                f.add("com:", preset);
                editor.popups().dialog("sheet.cf.date", "Uma Data Ocorrendo", f, () -> ConditionalRule.simple(ConditionalRuleType.TIME_PERIOD, STYLES[preset.getSelectedIndex()]).withTimePeriod((TimePeriod) period.getSelectedItem()))
                        .ifPresent(editor.format()::addConditionalRule);
            }
            case "duplicates" -> editor.format().addConditionalRule(ConditionalRule.simple(ConditionalRuleType.DUPLICATE, DifferentialStyle.LIGHT_RED));
            case "unique" -> editor.format().addConditionalRule(ConditionalRule.simple(ConditionalRuleType.UNIQUE, DifferentialStyle.LIGHT_RED));
            case "top10" -> editor.format().addConditionalRule(ConditionalRule.top(10, false, false, DifferentialStyle.LIGHT_RED));
            case "top10pct" -> editor.format().addConditionalRule(ConditionalRule.top(10, true, false, DifferentialStyle.LIGHT_RED));
            case "bottom10" -> editor.format().addConditionalRule(ConditionalRule.top(10, false, true, DifferentialStyle.LIGHT_RED));
            case "bottom10pct" -> editor.format().addConditionalRule(ConditionalRule.top(10, true, true, DifferentialStyle.LIGHT_RED));
            case "above" -> editor.format().addConditionalRule(ConditionalRule.simple(ConditionalRuleType.ABOVE_AVERAGE, DifferentialStyle.LIGHT_RED));
            case "below" -> editor.format().addConditionalRule(ConditionalRule.simple(ConditionalRuleType.ABOVE_AVERAGE, DifferentialStyle.LIGHT_RED).withBelow(true));
            case "bar.blue" -> editor.format().addConditionalRule(ConditionalRule.dataBar(0xFF638EC6, true));
            case "bar.green" -> editor.format().addConditionalRule(ConditionalRule.dataBar(0xFF63C384, true));
            case "bar.red" -> editor.format().addConditionalRule(ConditionalRule.dataBar(0xFFFF555A, true));
            case "bar.orange" -> editor.format().addConditionalRule(ConditionalRule.dataBar(0xFFFFB628, true));
            case "scale.gyr" -> editor.format().addConditionalRule(ConditionalRule.threeColorScale(0xFFF8696B, 0xFFFFEB84, 0xFF63BE7B));
            case "scale.ryg" -> editor.format().addConditionalRule(ConditionalRule.threeColorScale(0xFF63BE7B, 0xFFFFEB84, 0xFFF8696B));
            case "scale.wr" -> editor.format().addConditionalRule(ConditionalRule.twoColorScale(0xFFFFFFFF, 0xFFF8696B));
            case "scale.wg" -> editor.format().addConditionalRule(ConditionalRule.twoColorScale(0xFFFFFFFF, 0xFF63BE7B));
            case "icons.arrows" -> editor.format().addConditionalRule(ConditionalRule.iconSet(IconSetType.ARROWS_3));
            case "icons.lights" -> editor.format().addConditionalRule(ConditionalRule.iconSet(IconSetType.TRAFFIC_LIGHTS_3));
            case "icons.flags" -> editor.format().addConditionalRule(ConditionalRule.iconSet(IconSetType.FLAGS_3));
            case "icons.stars" -> editor.format().addConditionalRule(ConditionalRule.iconSet(IconSetType.STARS_3));
            case "icons.rating" -> editor.format().addConditionalRule(ConditionalRule.iconSet(IconSetType.RATING_5));
            default -> { }
        }
    }

    public void newRule() {
        ConditionalRulePanel panel = new ConditionalRulePanel(this::criterion);
        editor.popups().dialog(SheetDialogIds.CONDITIONAL_RULE, "Nova Regra de Formatação", panel, panel::result).ifPresent(editor.format()::addConditionalRule);
    }

    public void manage() {
        int s = editor.activeSheetIndex();
        List<ConditionalFormat> formats = editor.activeSheet().properties().conditionalFormats();
        List<String> labels = new ArrayList<>();
        List<int[]> index = new ArrayList<>();
        for (int i = 0; i < formats.size(); i++) {
            ConditionalFormat f = formats.get(i);
            String ranges = f.ranges().stream().map(r -> r.toA1()).collect(Collectors.joining(" "));
            for (int j = 0; j < f.rules().size(); j++) {
                ConditionalRule r = f.rules().get(j);
                String detail = r.formula1() == null ? "" : " " + Formulas.toDisplay(r.formula1(), editor.formulaLocale()) + (r.formula2() == null ? "" : " e " + Formulas.toDisplay(r.formula2(), editor.formulaLocale()));
                labels.add(ranges + " — " + r.type().name().toLowerCase().replace('_', ' ') + (r.operator() != null && r.type() == ConditionalRuleType.CELL_VALUE ? " " + r.operator().name().toLowerCase() : "") + detail);
                index.add(new int[]{i, j});
            }
        }
        if (labels.isEmpty()) { editor.popups().info("Gerenciador de Regras", "Não há regras de formatação condicional nesta planilha."); return; }
        JList<String> list = new JList<>(labels.toArray(String[]::new));
        list.setSelectedIndex(0);
        JComboBox<String> action = SheetForm.combo("Excluir regra selecionada", "Mover para cima", "Mover para baixo");
        SheetForm f = new SheetForm();
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(560, 220));
        f.grow(scroll);
        f.add("Ação:", action);
        editor.popups().dialog(SheetDialogIds.CONDITIONAL_MANAGER, "Gerenciador de Regras de Formatação Condicional", f, () -> new int[]{list.getSelectedIndex(), action.getSelectedIndex()}).filter(o -> o[0] >= 0).ifPresent(o -> {
            int[] at = index.get(o[0]);
            editor.edit("Gerenciar regras", tx -> tx.updateProperties(s, p -> {
                List<ConditionalFormat> list2 = new ArrayList<>(p.conditionalFormats());
                if (o[1] == 0) {
                    ConditionalFormat cf = list2.get(at[0]);
                    List<ConditionalRule> rules = new ArrayList<>(cf.rules());
                    rules.remove(at[1]);
                    if (rules.isEmpty()) list2.remove(at[0]); else list2.set(at[0], new ConditionalFormat(cf.ranges(), rules));
                } else {
                    int target = at[0] + (o[1] == 1 ? -1 : 1);
                    if (target >= 0 && target < list2.size()) { ConditionalFormat x = list2.remove(at[0]); list2.add(target, x); }
                }
                return p.withConditionalFormats(list2);
            }));
        });
    }
}
