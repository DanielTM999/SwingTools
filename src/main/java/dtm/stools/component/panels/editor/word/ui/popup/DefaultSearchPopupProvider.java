package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.component.panels.editor.word.provider.*;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class DefaultSearchPopupProvider implements WordSearchPopupProvider {
    @Override public String id() { return "word.popup.search.default"; }

    @Override public WordPopupHandle show(WordSearchContext context) {
        JTextField query = new JTextField(context.initialQuery(),24), replacement = new JTextField(24);
        JCheckBox matchCase = new JCheckBox("Diferenciar maiúsculas"), wholeWord = new JCheckBox("Palavra inteira"), regex = new JCheckBox("Expressão regular");
        JLabel status = new JLabel(" ");
        JButton previous = new JButton("Anterior"), replace = new JButton("Substituir"), replaceAll = new JButton("Substituir tudo");
        for (JComponent c : new JComponent[]{matchCase,wholeWord,regex,previous,replace,replaceAll}) c.setFocusable(true);
        JPanel panel = new JPanel(new GridBagLayout()); panel.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(3,0,3,6); g.anchor = GridBagConstraints.WEST; g.fill = GridBagConstraints.HORIZONTAL;
        g.gridx = 0; g.gridy = 0; panel.add(new JLabel("Localizar"),g); g.gridx = 1; g.weightx = 1; panel.add(query,g);
        g.gridx = 0; g.gridy = 1; g.weightx = 0; panel.add(new JLabel("Substituir por"),g); g.gridx = 1; g.weightx = 1; panel.add(replacement,g);
        JPanel options = new JPanel(new FlowLayout(FlowLayout.LEADING,0,0)); options.setOpaque(false);
        options.add(matchCase); options.add(Box.createHorizontalStrut(8)); options.add(wholeWord); options.add(Box.createHorizontalStrut(8)); options.add(regex);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; panel.add(options,g);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEADING,0,0)); buttons.setOpaque(false);
        buttons.add(previous); buttons.add(Box.createHorizontalStrut(6)); buttons.add(replace); buttons.add(Box.createHorizontalStrut(6)); buttons.add(replaceAll);
        g.gridy = 3; panel.add(buttons,g);
        g.gridy = 4; panel.add(status,g);
        java.util.function.Supplier<WordSearchOptions> current = () -> new WordSearchOptions(matchCase.isSelected(),wholeWord.isSelected(),regex.isSelected());
        Runnable refresh = () -> {
            String q = query.getText();
            if (q.isEmpty()) { status.setText(" "); return; }
            try {
                context.validate(q,current.get());
                int count = context.count(q,current.get()), index = context.currentIndex(q,current.get());
                status.setText(count == 0 ? "Nenhuma ocorrência" : index >= 0 ? (index+1) + " de " + count + " ocorrências" : count + (count == 1 ? " ocorrência" : " ocorrências"));
            } catch (RuntimeException e) { status.setText(e.getMessage()); }
        };
        java.util.function.Consumer<Runnable> guarded = action -> { try { action.run(); } catch (RuntimeException e) { status.setText(e.getMessage()); return; } refresh.run(); };
        previous.addActionListener(e -> guarded.accept(() -> context.findPrevious(query.getText(),current.get())));
        replace.addActionListener(e -> guarded.accept(() -> context.replace(query.getText(),replacement.getText(),current.get())));
        replaceAll.addActionListener(e -> { try { int n = context.replaceAll(query.getText(),replacement.getText(),current.get()); refresh.run(); status.setText(n + (n == 1 ? " substituição" : " substituições")); } catch (RuntimeException ex) { status.setText(ex.getMessage()); } });
        for (JCheckBox box : new JCheckBox[]{matchCase,wholeWord,regex}) box.addActionListener(e -> refresh.run());
        query.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refresh.run(); }
            public void removeUpdate(DocumentEvent e) { refresh.run(); }
            public void changedUpdate(DocumentEvent e) { refresh.run(); }
        });
        boolean readOnly = context.readOnly();
        replacement.setEnabled(!readOnly); replace.setEnabled(!readOnly); replaceAll.setEnabled(!readOnly);
        refresh.run();
        WordDialogActivity<Void> dialog = new WordDialogActivity<>(context.owner(), "Localizar e substituir", Dialog.ModalityType.MODELESS);
        dialog.setName("word.search.dialog");
        dialog.setBody(panel);
        dialog.addAction("Fechar", dialog::dispose, false);
        JButton next = dialog.addAction("Próximo", () -> guarded.accept(() -> context.findNext(query.getText(), current.get())), true);
        dialog.getRootPane().setDefaultButton(next);
        query.addActionListener(e -> next.doClick());
        dialog.onClosed(context::closed);
        dialog.open();
        query.requestFocusInWindow();
        return new WordPopupHandle() {
            public boolean isOpen() { return dialog.isDisplayable(); }
            public void toFront() { if (isOpen()) { dialog.toFront(); query.requestFocusInWindow(); query.selectAll(); } }
            public void close() { dialog.dispose(); }
        };
    }
}
