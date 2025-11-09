package dtm.stools.context.popup;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public final class PopupBuilder {

    private final JDialog dialog;
    private final JPanel contentPanel;
    private JComponent content;
    private int width = 400;
    private int height = 400;
    private boolean modal = false;
    private boolean undecorated = false;
    private boolean resizable = true;
    private Component parent;
    private String title;
    private Long autoCloseDelay;

    private PopupBuilder() {
        dialog = new JDialog();
        contentPanel = new JPanel(new BorderLayout());
        dialog.setContentPane(contentPanel);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    public static PopupBuilder create() {
        return new PopupBuilder();
    }

    public PopupBuilder title(String title) {
        this.title = title;
        return this;
    }

    public PopupBuilder size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public PopupBuilder modal(boolean modal) {
        this.modal = modal;
        return this;
    }

    public PopupBuilder undecorated(boolean undecorated) {
        this.undecorated = undecorated;
        return this;
    }

    public PopupBuilder resizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    public PopupBuilder centerOn(Component parent) {
        this.parent = parent;
        return this;
    }

    public PopupBuilder content(JComponent component) {
        this.content = component;
        return this;
    }

    public PopupBuilder autoCloseAfter(long millis) {
        this.autoCloseDelay = millis;
        return this;
    }

    public JDialog build() {
        dialog.setTitle(title);
        dialog.setModal(modal);
        dialog.setUndecorated(undecorated);
        dialog.setResizable(resizable);

        contentPanel.removeAll();
        if (content != null) {
            contentPanel.add(content, BorderLayout.CENTER);
        }

        dialog.pack();
        dialog.setSize(width, height);

        if (parent != null)
            dialog.setLocationRelativeTo(parent);
        else
            dialog.setLocationRelativeTo(null);

        if (autoCloseDelay != null && autoCloseDelay > 0) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    SwingUtilities.invokeLater(dialog::dispose);
                    timer.cancel();
                }
            }, autoCloseDelay);
        }

        return dialog;
    }

    public JDialog show() {
        JDialog built = build();
        built.setVisible(true);
        return built;
    }

    public JDialog showAndWait() {
        boolean oldModal = this.modal;
        this.modal = true;
        JDialog built = build();
        built.setVisible(true);
        this.modal = oldModal;
        return built;
    }
}
