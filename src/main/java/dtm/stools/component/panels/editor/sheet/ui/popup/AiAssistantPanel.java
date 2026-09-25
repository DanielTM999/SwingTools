package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.component.panels.editor.sheet.io.SheetTextExporter;
import dtm.stools.component.panels.editor.sheet.model.CellRange;
import dtm.stools.component.panels.editor.sheet.provider.SheetAiProvider;
import dtm.stools.component.panels.editor.sheet.provider.SheetAiRequest;
import dtm.stools.component.panels.editor.sheet.provider.SheetAiResponse;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public final class AiAssistantPanel {
    private AiAssistantPanel() {}

    public static void open(SheetEditor editor, SheetAiProvider provider) {
        SheetDialogActivity<Void> d = new SheetDialogActivity<>(editor, "Assistente", Dialog.ModalityType.MODELESS);
        JComboBox<String> kind = SheetForm.combo("Sugerir fórmula", "Explicar fórmula", "Analisar dados", "Pergunta livre");
        JTextArea question = new JTextArea(4, 48);
        question.setLineWrap(true);
        question.setWrapStyleWord(true);
        JTextArea answer = new JTextArea(10, 48);
        answer.setEditable(false);
        answer.setLineWrap(true);
        answer.setWrapStyleWord(true);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        JPanel top = new JPanel(new BorderLayout(0, 4));
        top.add(kind, BorderLayout.NORTH);
        top.add(new JScrollPane(question), BorderLayout.CENTER);
        content.add(top, BorderLayout.NORTH);
        JScrollPane a = new JScrollPane(answer);
        a.setPreferredSize(new Dimension(560, 220));
        content.add(a, BorderLayout.CENTER);
        JButton apply = new JButton("Aplicar Fórmula");
        apply.setEnabled(false);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        south.add(apply);
        content.add(south, BorderLayout.SOUTH);
        d.setBody(content);
        AtomicBoolean closed = new AtomicBoolean();
        SheetAiResponse[] last = new SheetAiResponse[1];
        d.addAction("Enviar", () -> {
            int s = editor.activeSheetIndex();
            CellRange range = editor.clipboard().bounded(editor.activeSheet(), editor.getSelection().range());
            if (range.cellCount() > 5000) range = range.resize(Math.min(range.rowCount(), 5000 / Math.max(1, range.columnCount())), range.columnCount());
            List<List<String>> data = new SheetTextExporter().grid(editor.getWorkbook(), editor.getEngine(), editor.formatter(), s, range);
            String formula = editor.editing().editText(s, editor.getSelection().active());
            SheetAiRequest.Kind k = SheetAiRequest.Kind.values()[kind.getSelectedIndex()];
            SheetAiRequest request = new SheetAiRequest(k, question.getText(), editor.activeSheet().name(), range.toA1(), data, formula.startsWith("=") ? formula : null, editor.getConfig().locale(), closed::get);
            answer.setText("Aguardando resposta…");
            apply.setEnabled(false);
            provider.ask(request).whenComplete((response, error) -> SwingUtilities.invokeLater(() -> {
                if (closed.get()) return;
                if (error != null) { answer.setText("Falha: " + (error.getMessage() == null ? error : error.getMessage())); return; }
                last[0] = response;
                answer.setText(response.text() + (response.formula() == null ? "" : "\n\n" + response.formula()));
                apply.setEnabled(response.formula() != null && !editor.isReadOnlyView());
            }));
        }, true);
        apply.addActionListener(e -> {
            SheetAiResponse r = last[0];
            if (r == null || r.formula() == null) return;
            String target = r.targetCell() == null || r.targetCell().isBlank() ? editor.getSelection().active().toA1() : r.targetCell();
            String f = r.formula().startsWith("=") ? r.formula() : "=" + r.formula();
            editor.run(() -> editor.input(target, f));
        });
        d.addAction("Fechar", d::dispose, false);
        d.onClosed(() -> closed.set(true));
        d.open();
    }
}
