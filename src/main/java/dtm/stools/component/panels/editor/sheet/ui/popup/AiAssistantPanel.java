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
    private static final String STATE_KEY = AiAssistantPanel.class.getName() + ".state";
    private static final class State {
        SheetDialogActivity<Void> dialog;
        Runnable refresh;
        AtomicBoolean requestCancelled = new AtomicBoolean();
        long generation;
        boolean waiting, closed;
        String instruction = "";
        SheetAiResponse response;
        Throwable error;
    }
    private AiAssistantPanel() {}

    private static State state(SheetEditor editor) {
        State state = (State) editor.getClientProperty(STATE_KEY);
        if (state == null) { state = new State(); editor.putClientProperty(STATE_KEY, state); }
        return state;
    }

    public static void suspend(SheetEditor editor) {
        State state = (State) editor.getClientProperty(STATE_KEY);
        if (state != null && state.dialog != null) state.dialog.dispose();
    }

    public static void close(SheetEditor editor) {
        State state = (State) editor.getClientProperty(STATE_KEY);
        if (state == null) return;
        state.closed = true;
        state.requestCancelled.set(true);
        suspend(editor);
        editor.putClientProperty(STATE_KEY, null);
    }

    public static void open(SheetEditor editor, SheetAiProvider provider) {
        State state = state(editor);
        if (state.dialog != null && state.dialog.isDisplayable()) { state.dialog.toFront(); return; }
        SheetDialogActivity<Void> d = new SheetDialogActivity<>(editor, "Assistente", Dialog.ModalityType.MODELESS);
        state.dialog = d;
        JComboBox<String> kind = SheetForm.combo("Sugerir fórmula", "Explicar fórmula", "Analisar dados", "Pergunta livre");
        JTextArea question = new JTextArea(4, 48);
        question.setText(state.instruction);
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
        state.refresh = () -> {
            if (state.waiting) answer.setText("Aguardando resposta...");
            else if (state.error != null) answer.setText("Falha: " + (state.error.getMessage() == null ? state.error : state.error.getMessage()));
            else answer.setText(state.response == null ? "" : state.response.text() + (state.response.formula() == null ? "" : "\n\n" + state.response.formula()));
            apply.setEnabled(!state.waiting && state.response != null && state.response.formula() != null && !editor.isReadOnlyView());
        };
        state.refresh.run();
        d.addAction("Enviar", () -> {
            int s = editor.activeSheetIndex();
            CellRange range = editor.clipboard().bounded(editor.activeSheet(), editor.getSelection().range());
            if (range.cellCount() > 5000) range = range.resize(Math.min(range.rowCount(), 5000 / Math.max(1, range.columnCount())), range.columnCount());
            List<List<String>> data = new SheetTextExporter().grid(editor.getWorkbook(), editor.getEngine(), editor.formatter(), s, range);
            String formula = editor.editing().editText(s, editor.getSelection().active());
            SheetAiRequest.Kind k = SheetAiRequest.Kind.values()[kind.getSelectedIndex()];
            state.requestCancelled.set(true);
            state.requestCancelled = new AtomicBoolean();
            AtomicBoolean cancelled = state.requestCancelled;
            long ticket = ++state.generation;
            state.instruction = question.getText();
            state.waiting = true;
            state.response = null;
            state.error = null;
            state.refresh.run();
            SheetAiRequest request = new SheetAiRequest(k, state.instruction, editor.activeSheet().name(), range.toA1(), data, formula.startsWith("=") ? formula : null, editor.getConfig().locale(), cancelled::get);
            try {
                provider.ask(request).whenComplete((response, error) -> SwingUtilities.invokeLater(() -> {
                    if (state.closed || ticket != state.generation) return;
                    state.waiting = false;
                    state.error = error;
                    state.response = response;
                    if (state.refresh != null) state.refresh.run();
                }));
            } catch (RuntimeException error) {
                state.waiting = false;
                state.error = error;
                state.refresh.run();
            }
        }, true);
        apply.addActionListener(e -> {
            SheetAiResponse r = state.response;
            if (r == null || r.formula() == null) return;
            String target = r.targetCell() == null || r.targetCell().isBlank() ? editor.getSelection().active().toA1() : r.targetCell();
            String f = r.formula().startsWith("=") ? r.formula() : "=" + r.formula();
            editor.run(() -> editor.input(target, f));
        });
        d.addAction("Fechar", d::dispose, false);
        d.onClosed(() -> { if (state.dialog == d) { state.dialog = null; state.refresh = null; } });
        d.open();
    }
}
