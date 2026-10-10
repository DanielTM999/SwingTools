package dtm.stools.component.command;

import dtm.stools.configs.UiTokens;
import dtm.stools.i18n.I18n;
import dtm.stools.activity.DialogActivity;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.text.Normalizer;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Independent, embeddable palette. All UI methods run on the Swing EDT. */
public class CommandPalette extends JPanel implements AutoCloseable {
    private final Supplier<List<CommandEntry>> commands;
    private final Predicate<String> executor;
    private final JTextField search = new JTextField();
    private final DefaultListModel<CommandEntry> model = new DefaultListModel<>();
    private final JList<CommandEntry> list = new JList<>(model);
    private final JLabel status = new JLabel();
    private List<CommandEntry> catalog = List.of();
    private JDialog dialog;
    private Component previousFocus;
    private Runnable onClosed = () -> {};

    public CommandPalette(Supplier<List<CommandEntry>> commands, Predicate<String> executor) {
        super(new BorderLayout(0, UiTokens.space(2)));
        this.commands = Objects.requireNonNull(commands); this.executor = Objects.requireNonNull(executor);
        setBorder(BorderFactory.createEmptyBorder(UiTokens.space(3), UiTokens.space(3), UiTokens.space(3), UiTokens.space(3)));
        search.putClientProperty("JTextField.placeholderText", text("search", "Pesquisar comandos…"));
        search.getAccessibleContext().setAccessibleName(text("search", "Pesquisar comandos…"));
        list.getAccessibleContext().setAccessibleName(text("results", "Comandos disponíveis"));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                CommandEntry entry = (CommandEntry) value;
                label.putClientProperty("html.disable", Boolean.TRUE);
                label.setText(entry.name() + (entry.group().isEmpty() ? "" : "   ·  " + entry.group()) + (entry.shortcut().isEmpty() ? "" : "   ·  " + entry.shortcut()));
                label.setToolTipText(entry.description()); label.setEnabled(entry.enabled());
                label.getAccessibleContext().setAccessibleDescription(entry.description() + (entry.enabled() ? "" : " · " + text("disabled", "Indisponível")));
                label.setBorder(BorderFactory.createEmptyBorder(UiTokens.space(2), UiTokens.space(2), UiTokens.space(2), UiTokens.space(2)));
                return label;
            }
        });
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });
        bind(search, "DOWN", "next", () -> move(1)); bind(search, "UP", "previous", () -> move(-1));
        bind(search, "ENTER", "execute", this::executeSelected); bind(list, "ENTER", "execute", this::executeSelected);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (e.getClickCount() == 2 && index >= 0 && list.getCellBounds(index, index).contains(e.getPoint())) executeSelected();
            }
        });
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "close");
        getActionMap().put("close", action(this::close));
        add(search, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list); scroll.setPreferredSize(new Dimension(UiTokens.scale(520), UiTokens.scale(320)));
        add(scroll); add(status, BorderLayout.SOUTH); refresh();
    }
    private static String text(String key, String fallback) { return I18n.getText(CommandPalette.class, key, fallback); }
    private static AbstractAction action(Runnable run) { return new AbstractAction() { public void actionPerformed(ActionEvent e) { run.run(); } }; }
    private static void bind(JComponent target, String stroke, String key, Runnable run) {
        target.getInputMap().put(KeyStroke.getKeyStroke(stroke), key); target.getActionMap().put(key, action(run));
    }
    public JTextField getSearchField() { return search; }
    public JList<CommandEntry> getCommandList() { return list; }
    public CommandPalette onClosed(Runnable callback) { onClosed = Objects.requireNonNull(callback); return this; }
    public void refresh() { catalog = snapshot(); filter(); }
    private List<CommandEntry> snapshot() {
        List<CommandEntry> values = List.copyOf(commands.get()); Set<String> ids = new HashSet<>();
        for (CommandEntry value : values) if (!ids.add(value.id())) throw new IllegalArgumentException("duplicate command id: " + value.id());
        return values;
    }
    private static String normalize(String value) { return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", ""); }
    private void filter() {
        String previous = list.getSelectedValue() == null ? null : list.getSelectedValue().id();
        String q = normalize(search.getText().strip()); model.clear();
        for (CommandEntry entry : catalog) if (q.isEmpty() || normalize(entry.name() + " " + entry.group() + " " + entry.description() + " " + entry.id()).contains(q)) model.addElement(entry);
        if (!model.isEmpty()) {
            list.setSelectedIndex(0);
            for (int i = 0; i < model.size(); i++) if (model.get(i).id().equals(previous)) { list.setSelectedIndex(i); break; }
        }
        status.setText(model.isEmpty() ? text("empty", "Nenhum comando encontrado") : "");
        list.getAccessibleContext().firePropertyChange(javax.accessibility.AccessibleContext.ACCESSIBLE_VISIBLE_DATA_PROPERTY, null, model.size());
    }
    private void move(int delta) {
        if (model.isEmpty()) return;
        int index = Math.max(0, Math.min(model.size() - 1, list.getSelectedIndex() + delta));
        list.setSelectedIndex(index); list.ensureIndexIsVisible(index);
    }
    /** Rechecks the supplier immediately before executing. False keeps the palette open. */
    public boolean executeSelected() {
        CommandEntry selected = list.getSelectedValue(); if (selected == null) return false;
        catalog = snapshot();
        boolean enabled = catalog.stream().anyMatch(e -> e.id().equals(selected.id()) && e.enabled());
        if (!enabled) { filter(); return false; }
        try {
            if (!executor.test(selected.id())) { status.setText(text("failed", "Não foi possível executar o comando")); return false; }
            close(); return true;
        } catch (RuntimeException e) { status.setText(text("failed", "Não foi possível executar o comando")); return false; }
    }
    public void open(Component owner) {
        if (isOpen()) { refresh(); toFront(); return; }
        refresh(); previousFocus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        Window window = owner instanceof Window w ? w : SwingUtilities.getWindowAncestor(owner);
        JDialog created = new PaletteDialog(window, text("title", "Comandos"));
        if (getName() != null) created.setName(getName());
        dialog = created; created.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE); created.setContentPane(this);
        created.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) { if (dialog == created) finishClose(); }
        });
        created.pack(); Rectangle bounds = created.getGraphicsConfiguration().getBounds();
        created.setSize(Math.min(created.getWidth(), bounds.width), Math.min(created.getHeight(), bounds.height));
        created.setLocationRelativeTo(owner); created.setVisible(true); search.requestFocusInWindow();
    }
    public boolean isOpen() { return dialog != null && dialog.isDisplayable(); }
    public void toFront() { if (isOpen()) { dialog.toFront(); search.requestFocusInWindow(); } }
    @Override
    public void close() { if (dialog != null) { JDialog old = dialog; old.dispose(); if (dialog == old) finishClose(); } }
    private void finishClose() {
        JDialog closed = dialog; dialog = null;
        if (closed != null) closed.setContentPane(new JPanel());
        Component restore = previousFocus; previousFocus = null;
        if (restore != null && restore.isShowing()) restore.requestFocusInWindow(); onClosed.run();
    }
    private static final class PaletteDialog extends DialogActivity {
        PaletteDialog(Window owner, String title) { super(owner, title, Dialog.ModalityType.MODELESS); }
        @Override
        protected void onDrawing() {}
    }
    /** Installs one owner-scoped shortcut and returns an idempotent restoration handle. */
    public AutoCloseable installShortcut(JRootPane root, KeyStroke stroke) {
        Objects.requireNonNull(root); Objects.requireNonNull(stroke);
        InputMap input = root.getInputMap(WHEN_IN_FOCUSED_WINDOW); ActionMap actions = root.getActionMap();
        Object previous = input.get(stroke), key = new Object();
        input.put(stroke, key); actions.put(key, action(() -> open(root)));
        return new AutoCloseable() {
            private boolean closed;
            public void close() {
                if (closed) return; closed = true;
                if (Objects.equals(input.get(stroke), key)) { if (previous == null) input.remove(stroke); else input.put(stroke, previous); }
                actions.remove(key);
            }
        };
    }
}
