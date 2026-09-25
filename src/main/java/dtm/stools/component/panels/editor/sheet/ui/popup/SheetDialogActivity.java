package dtm.stools.component.panels.editor.sheet.ui.popup;

import dtm.stools.activity.DialogActivity;
import dtm.stools.component.panels.editor.sheet.provider.SheetDialogRequest;
import dtm.stools.configs.UiTokens;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public class SheetDialogActivity<T> extends DialogActivity {
    private final Component sourceOwner;
    private final JPanel body = new JPanel(new BorderLayout(0, 12));
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
    private final JLabel error = new JLabel(" ");
    private final AtomicBoolean disposed = new AtomicBoolean();
    private Runnable onClosed = () -> {};
    private T result;

    public SheetDialogActivity(Component owner, String title, ModalityType modality) {
        super(owner instanceof Window w ? w : SwingUtilities.getWindowAncestor(owner), title, modality);
        sourceOwner = owner;
        applyDrawingOnce();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        body.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        error.setForeground(UiTokens.danger());
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.add(error, BorderLayout.NORTH);
        footer.add(actions, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        setContentPane(body);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public SheetDialogActivity(SheetDialogRequest<T> request) {
        this(request.owner(), request.title(), request.modal() ? ModalityType.DOCUMENT_MODAL : ModalityType.MODELESS);
        setName(request.id());
        if (!request.message().isBlank()) {
            JTextArea message = new JTextArea(request.message());
            message.setEditable(false);
            message.setFocusable(false);
            message.setOpaque(false);
            message.setLineWrap(true);
            message.setWrapStyleWord(true);
            message.setColumns(45);
            body.add(message, BorderLayout.NORTH);
        }
        setBody(request.content());
        if (request.readOnly()) disableEditing(request.content());
        if (!request.readOnly()) addAction("Cancelar", this::dispose, false);
        JButton confirm = addAction(request.readOnly() ? "Fechar" : request.confirmText(), () -> {
            if (request.readOnly()) { dispose(); return; }
            try {
                commitEditors(request.content());
                T value = request.result().get();
                request.validate().accept(value);
                result = Objects.requireNonNull(value, "O formulário não produziu um valor.");
                dispose();
            } catch (RuntimeException failure) {
                showError(failure);
            }
        }, true);
        if (request.enterConfirms()) getRootPane().setDefaultButton(confirm);
    }

    @Override protected void onDrawing() { }

    public void onClosed(Runnable listener) { onClosed = listener; }

    public void setBody(JComponent content) {
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        Dimension preferred = content.getPreferredSize();
        scroll.setPreferredSize(new Dimension(preferred.width + scroll.getVerticalScrollBar().getPreferredSize().width, Math.min(640, preferred.height + 4)));
        body.add(scroll, BorderLayout.CENTER);
    }

    public JButton addAction(String label, Runnable action, boolean primary) {
        JButton button = new JButton(label);
        button.setMargin(new Insets(6, 16, 6, 16));
        if (primary) button.putClientProperty("JButton.buttonType", "roundRect");
        button.addActionListener(e -> action.run());
        actions.add(button);
        return button;
    }

    public void showError(Throwable failure) {
        error.setText(failure.getMessage() == null ? "Não foi possível aplicar a alteração." : failure.getMessage());
        error.setToolTipText(error.getText());
    }

    public void open() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Open sheet dialogs on the EDT");
        if (disposed.get()) return;
        dispatchDrawing();
        pack();
        GraphicsConfiguration gc = sourceOwner.getGraphicsConfiguration();
        if (gc == null) gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds = new Rectangle(gc.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        bounds.x += insets.left; bounds.y += insets.top;
        bounds.width -= insets.left + insets.right; bounds.height -= insets.top + insets.bottom;
        setSize(Math.min(Math.max(380, getWidth()), bounds.width), Math.min(Math.max(200, getHeight()), bounds.height));
        setLocationRelativeTo(sourceOwner);
        setLocation(Math.max(bounds.x, Math.min(getX(), bounds.x + bounds.width - getWidth())), Math.max(bounds.y, Math.min(getY(), bounds.y + bounds.height - getHeight())));
        setVisible(true);
    }

    public Optional<T> showResult() { open(); return Optional.ofNullable(result); }

    @Override
    public void dispose() {
        if (!SwingUtilities.isEventDispatchThread()) { SwingUtilities.invokeLater(this::dispose); return; }
        if (!disposed.compareAndSet(false, true)) return;
        try { super.dispose(); } finally { onClosed.run(); }
    }

    private static void commitEditors(Component component) {
        if (component instanceof JSpinner spinner) {
            try { spinner.commitEdit(); } catch (java.text.ParseException failure) { throw new IllegalArgumentException("Confira o valor numérico informado.", failure); }
        }
        if (component instanceof JTable table && table.isEditing() && !table.getCellEditor().stopCellEditing()) throw new IllegalArgumentException("Confira o valor da célula em edição.");
        if (component instanceof Container container) for (Component child : container.getComponents()) commitEditors(child);
    }

    private static void disableEditing(Component component) {
        if (component instanceof javax.swing.text.JTextComponent text) text.setEditable(false);
        else if (component instanceof AbstractButton || component instanceof JComboBox<?> || component instanceof JSpinner || component instanceof JTable) component.setEnabled(false);
        if (component instanceof Container container) for (Component child : container.getComponents()) disableEditing(child);
    }
}
