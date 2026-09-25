package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.function.FunctionCategory;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.function.SheetFunction;
import dtm.stools.component.panels.editor.sheet.ui.FunctionSignatures;

import javax.swing.DefaultListModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class InsertFunctionPanel extends JPanel {
    private final FunctionRegistry functions;
    private final FormulaLocale locale;
    private final JTextField search = new JTextField(30);
    private final JComboBox<String> category;
    private final DefaultListModel<SheetFunction> model = new DefaultListModel<>();
    private final JList<SheetFunction> list = new JList<>(model);
    private final JLabel detail = new JLabel(" ");

    public InsertFunctionPanel(FunctionRegistry functions, FormulaLocale locale, FunctionCategory initial) {
        super(new BorderLayout(0, 8));
        this.functions = functions;
        this.locale = locale;
        List<String> cats = new ArrayList<>();
        cats.add("Todas");
        for (FunctionCategory c : FunctionCategory.values()) cats.add(c.label());
        category = new JComboBox<>(cats.toArray(String[]::new));
        if (initial != null) category.setSelectedItem(initial.label());
        JPanel top = new JPanel(new BorderLayout(6, 6));
        search.putClientProperty("JTextField.placeholderText", "Procure por uma função");
        top.add(search, BorderLayout.NORTH);
        top.add(category, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        list.setCellRenderer((l, f, i, sel, focus) -> {
            JLabel label = new JLabel(locale.localizeFunction(f.name()));
            label.setOpaque(true);
            label.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 6, 2, 6));
            if (sel) { label.setBackground(l.getSelectionBackground()); label.setForeground(l.getSelectionForeground()); }
            return label;
        });
        JScrollPane scroll = new JScrollPane(list);
        scroll.setPreferredSize(new Dimension(460, 240));
        add(scroll, BorderLayout.CENTER);
        detail.setPreferredSize(new Dimension(460, 70));
        detail.setVerticalAlignment(JLabel.TOP);
        add(detail, BorderLayout.SOUTH);
        list.addListSelectionListener(e -> describe());
        category.addActionListener(e -> filter());
        search.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filter(); }
            @Override public void removeUpdate(DocumentEvent e) { filter(); }
            @Override public void changedUpdate(DocumentEvent e) { filter(); }
        });
        filter();
    }

    private static String norm(String s) { return Normalizer.normalize(s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", ""); }

    private void filter() {
        String q = norm(search.getText().strip());
        int cat = category.getSelectedIndex();
        List<SheetFunction> all = new ArrayList<>(functions.all());
        all.sort(Comparator.comparing(f -> locale.localizeFunction(f.name())));
        model.clear();
        for (SheetFunction f : all) {
            if (cat > 0 && f.category() != FunctionCategory.values()[cat - 1]) continue;
            if (!q.isEmpty() && !norm(locale.localizeFunction(f.name())).contains(q) && !norm(f.name()).contains(q) && !norm(FunctionSignatures.description(f)).contains(q)) continue;
            model.addElement(f);
        }
        if (!model.isEmpty()) list.setSelectedIndex(0);
    }

    private void describe() {
        SheetFunction f = list.getSelectedValue();
        if (f == null) { detail.setText(" "); return; }
        String name = locale.localizeFunction(f.name());
        String sep = String.valueOf(locale.argumentSeparator());
        String params = String.join(sep + " ", FunctionSignatures.parameters(f));
        detail.setText("<html><b>" + name + "(" + params + ")</b><br>" + FunctionSignatures.description(f) + "</html>");
    }

    public String result() {
        SheetFunction f = list.getSelectedValue();
        if (f == null) throw new IllegalArgumentException("Selecione uma função.");
        return locale.localizeFunction(f.name());
    }
}
