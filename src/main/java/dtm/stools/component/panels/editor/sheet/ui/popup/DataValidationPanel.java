package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.model.ComparisonOperator;
import dtm.stools.component.panels.editor.sheet.model.DataValidation;
import dtm.stools.component.panels.editor.sheet.model.ErrorAlertStyle;
import dtm.stools.component.panels.editor.sheet.model.ValidationType;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;
import java.util.function.Function;

public class DataValidationPanel extends javax.swing.JPanel {
    private static final ValidationType[] TYPES = {ValidationType.ANY, ValidationType.WHOLE, ValidationType.DECIMAL, ValidationType.LIST, ValidationType.DATE, ValidationType.TIME, ValidationType.TEXT_LENGTH, ValidationType.CUSTOM, ValidationType.CHECKBOX};
    private static final String[] TYPE_NAMES = {"Qualquer valor", "Número inteiro", "Decimal", "Lista", "Data", "Hora", "Comprimento do texto", "Personalizado", "Caixa de seleção"};
    private static final ComparisonOperator[] OPS = ComparisonOperator.values();
    private static final String[] OP_NAMES = {"está entre", "não está entre", "é igual a", "é diferente de", "é maior do que", "é menor do que", "é maior ou igual a", "é menor ou igual a"};
    private static final ErrorAlertStyle[] STYLES = ErrorAlertStyle.values();

    private final JComboBox<String> type = SheetForm.combo(TYPE_NAMES);
    private final JComboBox<String> operator = SheetForm.combo(OP_NAMES);
    private final JTextField f1 = SheetForm.text("", 24), f2 = SheetForm.text("", 24);
    private final JCheckBox blank = SheetForm.check("Ignorar em branco", true), dropdown = SheetForm.check("Menu suspenso na célula", true);
    private final JCheckBox showInput = SheetForm.check("Mostrar mensagem de entrada ao selecionar a célula", true);
    private final JTextField inputTitle = SheetForm.text("", 24);
    private final JTextArea inputMessage = new JTextArea(4, 30);
    private final JCheckBox showError = SheetForm.check("Mostrar alerta de erro após a inserção de dados inválidos", true);
    private final JComboBox<String> errorStyle = SheetForm.combo("Parar", "Aviso", "Informações");
    private final JTextField errorTitle = SheetForm.text("", 24);
    private final JTextArea errorMessage = new JTextArea(4, 30);
    private final List<CellRange> ranges;
    private final Function<String, String> toCanonical;

    public DataValidationPanel(List<CellRange> ranges, DataValidation current, Function<String, String> toDisplay, Function<String, String> toCanonical) {
        super(new BorderLayout());
        this.ranges = List.copyOf(ranges);
        this.toCanonical = toCanonical;
        JTabbedPane tabs = new JTabbedPane();
        SheetForm settings = new SheetForm();
        settings.section("Critérios de validação");
        settings.add("Permitir:", type);
        settings.add("Dados:", operator);
        settings.add("Mínimo / Valor / Fonte:", f1);
        settings.add("Máximo:", f2);
        settings.full(blank);
        settings.full(dropdown);
        tabs.addTab("Configurações", settings);
        SheetForm input = new SheetForm();
        input.full(showInput);
        input.add("Título:", inputTitle);
        input.add("Mensagem:", new JScrollPane(inputMessage));
        tabs.addTab("Mensagem de Entrada", input);
        SheetForm error = new SheetForm();
        error.full(showError);
        error.add("Estilo:", errorStyle);
        error.add("Título:", errorTitle);
        error.add("Mensagem:", new JScrollPane(errorMessage));
        tabs.addTab("Alerta de Erro", error);
        add(tabs, BorderLayout.CENTER);
        setPreferredSize(new Dimension(520, 330));
        if (current != null) {
            for (int i = 0; i < TYPES.length; i++) if (TYPES[i] == current.type()) type.setSelectedIndex(i);
            operator.setSelectedIndex(current.operator().ordinal());
            f1.setText(current.formula1() == null ? "" : toDisplay.apply(current.formula1()));
            f2.setText(current.formula2() == null ? "" : toDisplay.apply(current.formula2()));
            blank.setSelected(current.allowBlank());
            dropdown.setSelected(current.showDropdown());
            showInput.setSelected(current.showInput());
            inputTitle.setText(current.inputTitle());
            inputMessage.setText(current.inputMessage());
            showError.setSelected(current.showError());
            errorStyle.setSelectedIndex(current.errorStyle().ordinal());
            errorTitle.setText(current.errorTitle());
            errorMessage.setText(current.errorMessage());
        }
        type.addActionListener(e -> sync());
        operator.addActionListener(e -> sync());
        sync();
    }

    private void sync() {
        ValidationType t = TYPES[type.getSelectedIndex()];
        boolean ranged = t == ValidationType.WHOLE || t == ValidationType.DECIMAL || t == ValidationType.DATE || t == ValidationType.TIME || t == ValidationType.TEXT_LENGTH;
        operator.setEnabled(ranged);
        f1.setEnabled(t != ValidationType.ANY && t != ValidationType.CHECKBOX);
        ComparisonOperator op = OPS[operator.getSelectedIndex()];
        f2.setEnabled(ranged && (op == ComparisonOperator.BETWEEN || op == ComparisonOperator.NOT_BETWEEN));
        dropdown.setEnabled(t == ValidationType.LIST);
    }

    public DataValidation result() {
        ValidationType t = TYPES[type.getSelectedIndex()];
        String a = f1.isEnabled() && !f1.getText().isBlank() ? convert(t, f1.getText().strip()) : null;
        String b = f2.isEnabled() && !f2.getText().isBlank() ? convert(t, f2.getText().strip()) : null;
        if (t != ValidationType.ANY && t != ValidationType.CHECKBOX && a == null) throw new IllegalArgumentException("Informe o valor ou a fonte da validação.");
        return new DataValidation(ranges, t, OPS[operator.getSelectedIndex()], a, b, blank.isSelected(), dropdown.isSelected(), STYLES[errorStyle.getSelectedIndex()],
                showInput.isSelected(), inputTitle.getText(), inputMessage.getText(), showError.isSelected(), errorTitle.getText(), errorMessage.getText());
    }

    private String convert(ValidationType t, String text) {
        if (t == ValidationType.LIST && !text.startsWith("=")) return "\"" + String.join(",", text.split("\\s*[;,]\\s*")) + "\"";
        return toCanonical.apply(text);
    }
}
