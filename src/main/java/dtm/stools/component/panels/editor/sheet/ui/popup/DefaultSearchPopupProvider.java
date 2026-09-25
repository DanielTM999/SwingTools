package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.provider.SheetPopupHandle;
import dtm.stools.component.panels.editor.sheet.provider.SheetSearchContext;
import dtm.stools.component.panels.editor.sheet.provider.SheetSearchHit;
import dtm.stools.component.panels.editor.sheet.provider.SheetSearchOptions;
import dtm.stools.component.panels.editor.sheet.provider.SheetSearchPopupProvider;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

public final class DefaultSearchPopupProvider implements SheetSearchPopupProvider {
    private SheetDialogActivity<Void> current;

    @Override public String id() { return "sheet.popup.search.default"; }

    @Override
    public SheetPopupHandle open(SheetSearchContext context, boolean replaceMode) {
        if (current != null && current.isDisplayable()) { current.toFront(); return handle(current); }
        SheetDialogActivity<Void> d = new SheetDialogActivity<>(context.owner(), "Localizar e Substituir", Dialog.ModalityType.MODELESS);
        current = d;
        JTextField find = new JTextField(24), replace = new JTextField(24);
        JCheckBox matchCase = new JCheckBox("Diferenciar maiúsculas de minúsculas"), entire = new JCheckBox("Coincidir conteúdo da célula inteira"), regex = new JCheckBox("Usar expressões regulares");
        JComboBox<String> within = new JComboBox<>(new String[]{"Planilha", "Pasta de trabalho"});
        JComboBox<String> look = new JComboBox<>(new String[]{"Fórmulas", "Valores"});
        JComboBox<String> order = new JComboBox<>(new String[]{"Por linhas", "Por colunas"});
        DefaultListModel<SheetSearchHit> model = new DefaultListModel<>();
        JList<SheetSearchHit> results = new JList<>(model);
        results.setCellRenderer((list, v, i, sel, focus) -> {
            JLabel l = new JLabel(v.sheetName() + "!" + v.cell().toA1() + "    " + v.text());
            l.setOpaque(true);
            l.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
            if (sel) { l.setBackground(list.getSelectionBackground()); l.setForeground(list.getSelectionForeground()); }
            return l;
        });
        results.addListSelectionListener(e -> { if (!e.getValueIsAdjusting() && results.getSelectedValue() != null) context.select(results.getSelectedValue()); });
        JLabel status = new JLabel(" ");
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(3, 3, 3, 3);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0; c.gridy = 0; form.add(new JLabel("Localizar:"), c);
        c.gridx = 1; c.weightx = 1; form.add(find, c);
        c.gridx = 0; c.gridy = 1; c.weightx = 0; form.add(new JLabel("Substituir por:"), c);
        c.gridx = 1; c.weightx = 1; form.add(replace, c);
        c.gridx = 0; c.gridy = 2; c.weightx = 0; form.add(new JLabel("Em:"), c);
        c.gridx = 1; form.add(within, c);
        c.gridx = 0; c.gridy = 3; form.add(new JLabel("Pesquisar:"), c);
        c.gridx = 1; form.add(order, c);
        c.gridx = 0; c.gridy = 4; form.add(new JLabel("Examinar:"), c);
        c.gridx = 1; form.add(look, c);
        c.gridx = 0; c.gridy = 5; c.gridwidth = 2; form.add(matchCase, c);
        c.gridy = 6; form.add(entire, c);
        c.gridy = 7; form.add(regex, c);
        c.gridy = 8; form.add(status, c);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.add(form, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(results);
        scroll.setPreferredSize(new Dimension(460, 160));
        content.add(scroll, BorderLayout.CENTER);
        d.setBody(content);
        java.util.function.Supplier<SheetSearchOptions> options = () -> new SheetSearchOptions(matchCase.isSelected(), entire.isSelected(), regex.isSelected(),
                look.getSelectedIndex() == 0, within.getSelectedIndex() == 1, order.getSelectedIndex() == 1);
        d.addAction("Localizar Tudo", () -> {
            model.clear();
            List<SheetSearchHit> hits = context.findAll(find.getText(), options.get());
            hits.forEach(model::addElement);
            status.setText(hits.size() + " célula(s) encontrada(s)");
        }, false);
        d.addAction("Localizar Próxima", () -> {
            if (model.isEmpty()) context.findAll(find.getText(), options.get()).forEach(model::addElement);
            if (model.isEmpty()) { status.setText("Nenhum resultado."); return; }
            int next = (results.getSelectedIndex() + 1) % model.size();
            results.setSelectedIndex(next);
            results.ensureIndexIsVisible(next);
        }, true);
        d.addAction("Substituir", () -> {
            SheetSearchHit hit = results.getSelectedValue();
            if (hit == null) { status.setText("Selecione um resultado."); return; }
            context.replace(hit, replace.getText(), options.get());
            model.remove(results.getSelectedIndex());
        }, false);
        d.addAction("Substituir Tudo", () -> {
            int n = context.replaceAll(find.getText(), replace.getText(), options.get());
            model.clear();
            status.setText(n + " substituição(ões) efetuada(s).");
        }, false);
        d.addAction("Fechar", d::dispose, false);
        d.onClosed(() -> current = null);
        d.getRootPane().setDefaultButton(null);
        find.addActionListener(e -> { model.clear(); context.findAll(find.getText(), options.get()).forEach(model::addElement); if (!model.isEmpty()) results.setSelectedIndex(0); });
        d.open();
        if (replaceMode) replace.requestFocusInWindow(); else find.requestFocusInWindow();
        return handle(d);
    }

    private static SheetPopupHandle handle(SheetDialogActivity<?> d) {
        return new SheetPopupHandle() {
            @Override public boolean isOpen() { return d.isDisplayable(); }
            @Override public void toFront() { d.toFront(); }
            @Override public void close() { d.dispose(); }
        };
    }
}
