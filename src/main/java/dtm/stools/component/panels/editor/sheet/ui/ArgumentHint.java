package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.formula.FormulaLocale;
import dtm.stools.component.panels.editor.sheet.function.FunctionRegistry;
import dtm.stools.component.panels.editor.sheet.function.SheetFunction;
import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

public final class ArgumentHint {
    private final JWindow window;
    private final JLabel label = new JLabel();

    public ArgumentHint(Component owner) {
        window = new JWindow(SwingUtilities.getWindowAncestor(owner));
        window.setFocusableWindowState(false);
        label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiTokens.border()), BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        label.setOpaque(true);
        window.setContentPane(label);
    }

    public void hide() { window.setVisible(false); }

    public void update(JTextComponent text, FunctionRegistry functions, FormulaLocale locale) {
        FormulaHighlighter.CallContext call = FormulaHighlighter.callAt(text.getText(), text.getCaretPosition(), locale);
        if (call == null) { hide(); return; }
        Optional<SheetFunction> f = functions.find(locale.canonicalFunction(call.function()));
        if (f.isEmpty()) { hide(); return; }
        List<String> params = FunctionSignatures.parameters(f.get());
        StringBuilder b = new StringBuilder("<html>").append(locale.localizeFunction(f.get().name())).append('(');
        String sep = String.valueOf(locale.argumentSeparator()) + " ";
        for (int k = 0; k < params.size(); k++) {
            if (k > 0) b.append(sep);
            boolean active = k == call.argument() || k == params.size() - 1 && params.get(k).equals("...") && call.argument() >= k;
            String p = params.get(k).replace("<", "&lt;");
            b.append(active ? "<b>" + p + "</b>" : p);
        }
        b.append(")</html>");
        label.setText(b.toString());
        try {
            Rectangle r = text.modelToView2D(0).getBounds();
            Point p = new Point(r.x, text.getHeight() + 2);
            SwingUtilities.convertPointToScreen(p, text);
            window.pack();
            window.setLocation(p);
            window.setVisible(true);
        } catch (Exception e) { hide(); }
    }

    public void dispose() { window.dispose(); }
}
