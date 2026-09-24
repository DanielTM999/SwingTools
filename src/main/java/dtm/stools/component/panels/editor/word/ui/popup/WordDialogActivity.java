package dtm.stools.component.panels.editor.word.ui.popup;

import dtm.stools.activity.DialogActivity;
import dtm.stools.component.panels.editor.word.provider.WordDialogRequest;
import dtm.stools.configs.UiTokens;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/** Window belonging to a particular editor, with Swing work performed exclusively on the EDT. */
public class WordDialogActivity<T> extends DialogActivity {
    private final Component sourceOwner;
    private final JPanel body = new JPanel(new BorderLayout(0, 12));
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
    private final JLabel error = new JLabel(" ");
    private final AtomicBoolean disposed = new AtomicBoolean();
    private Runnable onClosed = () -> {};
    private T result;

    public WordDialogActivity(Component owner, String title, ModalityType modality) {
        super(owner instanceof Window w ? w : SwingUtilities.getWindowAncestor(owner), title, modality);
        sourceOwner = owner;
        applyDrawingOnce();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        error.setForeground(UiTokens.danger());
        error.getAccessibleContext().setAccessibleName("Validação");
        JPanel footer = new JPanel(new BorderLayout(0, 8));
        footer.add(error, BorderLayout.NORTH); footer.add(actions, BorderLayout.SOUTH);
        body.add(footer, BorderLayout.SOUTH);
        setContentPane(body);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    public WordDialogActivity(WordDialogRequest<T> request) {
        this(request.owner(), request.title(), ModalityType.DOCUMENT_MODAL);
        setName(request.id());
        if (!request.message().isBlank()) {
            JTextArea message = new JTextArea(request.message());
            message.setEditable(false); message.setFocusable(false); message.setOpaque(false);
            message.setLineWrap(true); message.setWrapStyleWord(true); message.setColumns(45);
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
                result = java.util.Objects.requireNonNull(value, "O formulário não produziu um valor.");
                dispose();
            } catch (RuntimeException failure) { showError(failure); }
        }, true);
        if (request.enterConfirms()) getRootPane().setDefaultButton(confirm);
    }

    // Modal calls must remain synchronous. init() schedules visibility asynchronously, so these
    // component-owned activities dispatch drawing directly and use Swing's modal event loop.
    @Override protected void onDrawing() { }

    public Component sourceOwner() { return sourceOwner; }
    public void onClosed(Runnable listener) { onClosed = listener; }
    public void setBody(JComponent content) {
        JComponent view = content;
        if (content instanceof JTextField || content instanceof JComboBox<?> || content instanceof JSpinner) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.add(content, BorderLayout.NORTH); view = holder;
        }
        JScrollPane scroll = new JScrollPane(view);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        Dimension preferred = view.getPreferredSize();
        scroll.setPreferredSize(new Dimension(preferred.width + scroll.getVerticalScrollBar().getPreferredSize().width, preferred.height));
        body.add(scroll, BorderLayout.CENTER);
    }
    public JButton addAction(String label, Runnable action, boolean primary) {
        JButton button = new JButton(label);
        button.setMargin(new Insets(6, 16, 6, 16));
        if (primary) button.putClientProperty("JButton.buttonType", "roundRect");
        button.addActionListener(e -> action.run()); actions.add(button); return button;
    }
    public void showError(Throwable failure) {
        error.setText(failure.getMessage() == null ? "Não foi possível aplicar a alteração." : failure.getMessage());
        error.setToolTipText(error.getText());
    }
    public void open() {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Open Word dialogs on the EDT");
        if (disposed.get()) return;
        dispatchDrawing();
        pack();
        GraphicsConfiguration gc = sourceOwner.getGraphicsConfiguration();
        if (gc == null) gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        Rectangle bounds = new Rectangle(gc.getBounds());
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        bounds.x += insets.left; bounds.y += insets.top;
        bounds.width -= insets.left + insets.right; bounds.height -= insets.top + insets.bottom;
        setSize(Math.min(Math.max(420, getWidth()), bounds.width), Math.min(Math.max(220, getHeight()), bounds.height));
        setLocationRelativeTo(sourceOwner);
        setLocation(Math.max(bounds.x, Math.min(getX(), bounds.x + bounds.width - getWidth())),
                Math.max(bounds.y, Math.min(getY(), bounds.y + bounds.height - getHeight())));
        setVisible(true);
    }
    public Optional<T> showResult() { open(); return Optional.ofNullable(result); }
    @Override public void dispose() {
        if (!SwingUtilities.isEventDispatchThread()) { SwingUtilities.invokeLater(this::dispose); return; }
        if (!disposed.compareAndSet(false, true)) return;
        try { super.dispose(); } finally {
            onClosed.run();
            if (sourceOwner instanceof dtm.stools.component.panels.editor.word.WordEditor editor && !editor.isClosed()) editor.getCanvas().requestFocusInWindow();
        }
    }
    private static void commitEditors(Component component) {
        if (component instanceof JSpinner spinner) {
            try { spinner.commitEdit(); }
            catch (java.text.ParseException failure) { spinner.requestFocusInWindow(); throw new IllegalArgumentException("Confira o valor numérico informado.",failure); }
        }
        if (component instanceof JTable table && table.isEditing() && !table.getCellEditor().stopCellEditing())
            throw new IllegalArgumentException("Confira o valor da célula em edição.");
        if (component instanceof Container container) for (Component child : container.getComponents()) commitEditors(child);
    }
    private static void disableEditing(Component component) {
        if (component instanceof javax.swing.text.JTextComponent text) text.setEditable(false);
        else if (component instanceof AbstractButton || component instanceof JComboBox<?> || component instanceof JSpinner || component instanceof JTable) component.setEnabled(false);
        if (component instanceof Container container) for (Component child : container.getComponents()) disableEditing(child);
    }
}
