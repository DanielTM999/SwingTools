package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.model.WordFormField;
import javax.swing.*;
import java.util.Arrays;
import java.util.List;

public final class WordFormFieldPanel extends WordPropertiesPanel<WordFormField> {
    private final WordFormField field;
    private final JTextField name = new JTextField(), value = new JTextField();
    private final JTextArea options = new JTextArea(4,24);
    private final JComboBox<String> choice = new JComboBox<>();
    private final JCheckBox checked = new JCheckBox("Marcado");

    public WordFormFieldPanel(WordFormField field) {
        this.field = field;
        name.setText(field.name()); row("Nome do campo",name);
        switch (field.kind()) {
            case CHECKBOX -> { checked.setSelected(field.checked()); checked.setOpaque(false); row(null,checked); }
            case DROPDOWN -> {
                options.setText(String.join("\n",field.options())); row("Opções (uma por linha)",new JScrollPane(options));
                choice.addItem(""); field.options().forEach(choice::addItem); choice.setSelectedItem(field.value()); row("Valor",choice);
            }
            default -> { value.setText(field.value()); row(field.kind() == WordFormField.Kind.DATE ? "Data (dd/mm/aaaa)" : "Valor",value); }
        }
    }
    @Override public String title() { return "Campo de formulário"; }
    @Override public WordFormField result() {
        return switch (field.kind()) {
            case CHECKBOX -> new WordFormField(field.id(),field.kind(),name.getText().strip(),"",List.of(),checked.isSelected(),field.placeholder());
            case DROPDOWN -> {
                List<String> list = Arrays.stream(options.getText().split("\n")).map(String::strip).filter(s -> !s.isEmpty()).distinct().toList();
                String v = choice.getSelectedItem() == null ? "" : choice.getSelectedItem().toString();
                yield new WordFormField(field.id(),field.kind(),name.getText().strip(),list.contains(v) ? v : "",list,false,field.placeholder());
            }
            case DATE -> {
                String v = value.getText().strip();
                if (!v.isEmpty() && !v.matches("\\d{2}/\\d{2}/\\d{4}")) throw new IllegalArgumentException("Use o formato dd/mm/aaaa");
                yield new WordFormField(field.id(),field.kind(),name.getText().strip(),v,List.of(),false,field.placeholder());
            }
            case TEXT -> new WordFormField(field.id(),field.kind(),name.getText().strip(),value.getText(),List.of(),false,field.placeholder());
        };
    }
}
