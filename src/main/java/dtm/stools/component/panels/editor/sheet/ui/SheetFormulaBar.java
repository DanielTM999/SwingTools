package dtm.stools.component.panels.editor.sheet.ui;

import dtm.stools.component.panels.editor.sheet.SheetEditor;
import dtm.stools.configs.UiTokens;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class SheetFormulaBar extends JPanel {
    private final SheetEditor editor;
    private final JComboBox<String> nameBox = new JComboBox<>();
    private final JTextPane field = new JTextPane() {
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
    };
    private final JScrollPane scroll;
    private final JButton cancel, accept, function, expand;
    private boolean expanded, updating;

    public SheetFormulaBar(SheetEditor editor) {
        super(new BorderLayout(6, 0));
        this.editor = editor;
        setName("sheet.formulaBar");
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UiTokens.border()), BorderFactory.createEmptyBorder(3, 6, 3, 6)));
        nameBox.setEditable(true);
        nameBox.setName("sheet.nameBox");
        nameBox.setPreferredSize(new Dimension(130, 26));
        nameBox.setToolTipText("Caixa de Nome");
        ((JTextField) nameBox.getEditor().getEditorComponent()).addActionListener(e -> {
            if (updating) return;
            String text = String.valueOf(nameBox.getEditor().getItem()).strip();
            if (!text.isEmpty()) editor.goToOrDefine(text);
        });
        nameBox.addActionListener(e -> {
            if (updating || !"comboBoxChanged".equals(e.getActionCommand()) || !nameBox.isPopupVisible()) return;
            Object sel = nameBox.getSelectedItem();
            if (sel != null) editor.goToOrDefine(sel.toString());
        });
        cancel = tool("cancel", "Cancelar (Esc)", () -> editor.cancelEditing());
        accept = tool("check", "Inserir (Enter)", () -> editor.commitEditing(0, 0));
        function = tool("function", "Inserir Função (Shift+F3)", () -> editor.execute("sheet.insertFunction"));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.X_AXIS));
        left.setOpaque(false);
        left.add(nameBox);
        left.add(Box.createHorizontalStrut(6));
        left.add(cancel);
        left.add(accept);
        left.add(function);
        add(left, BorderLayout.WEST);
        field.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        field.setName("sheet.formulaField");
        field.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
        scroll = new JScrollPane(field, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createLineBorder(UiTokens.border()));
        add(scroll, BorderLayout.CENTER);
        expand = tool("expand", "Expandir barra de fórmulas (Ctrl+Shift+U)", this::toggleExpanded);
        add(expand, BorderLayout.EAST);
        field.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { if (!editor.isEditing()) SwingUtilities.invokeLater(() -> editor.startEditingFromFormulaBar(field.viewToModel2D(e.getPoint()))); }
        });
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { if (!editor.isEditing() && !editor.isReadOnlyView()) editor.startEditingFromFormulaBar(field.getCaretPosition()); }
        });
        field.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { editor.handleEditorKey(e, field); }
        });
        updateHeight();
        setEditingState(false);
    }

    private JButton tool(String icon, String tip, Runnable action) {
        JButton b = new JButton(SheetIcon.small(icon));
        b.setToolTipText(tip);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        b.setMargin(new java.awt.Insets(2, 2, 2, 2));
        b.addActionListener(e -> action.run());
        return b;
    }

    public JTextPane field() { return field; }
    public JComboBox<String> nameBox() { return nameBox; }

    public void setDocument(StyledDocument document) { field.setStyledDocument(document); }

    public void toggleExpanded() { expanded = !expanded; expand.setIcon(SheetIcon.small(expanded ? "collapse" : "expand")); updateHeight(); }

    private void updateHeight() {
        int lines = expanded ? 5 : 1;
        int h = field.getFontMetrics(field.getFont()).getHeight() * lines + 10;
        scroll.setPreferredSize(new Dimension(200, h));
        revalidate();
    }

    public void setEditingState(boolean editing) {
        cancel.setEnabled(editing);
        accept.setEnabled(editing);
    }

    public void setNameBoxText(String text, List<String> names) {
        updating = true;
        try {
            if (nameBox.getItemCount() != names.size() || !sameItems(names)) {
                nameBox.removeAllItems();
                for (String n : names) nameBox.addItem(n);
            }
            nameBox.getEditor().setItem(text);
        } finally {
            updating = false;
        }
    }

    private boolean sameItems(List<String> names) {
        for (int k = 0; k < names.size(); k++) if (!names.get(k).equals(nameBox.getItemAt(k))) return false;
        return true;
    }

    public void setEditable(boolean value) { field.setEditable(value); }
}
