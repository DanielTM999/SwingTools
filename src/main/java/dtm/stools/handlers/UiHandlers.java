package dtm.stools.handlers;

import dtm.stools.context.IWindow;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.util.function.Consumer;

public final class UiHandlers {

    private UiHandlers() {}

    public static ActionListener wrapAction(IWindow iWindow, String action, Runnable runnable) {
        return e -> iWindow.getWindowExecutor().execute(runnable::run, action);
    }

    public static MouseListener wrapMouse(IWindow iWindow, String actionPrefix, Consumer<MouseEvent> consumer) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#mouseClicked");
            }

            @Override
            public void mousePressed(MouseEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#mousePressed");
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#mouseReleased");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#mouseEntered");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#mouseExited");
            }
        };
    }

    public static KeyListener wrapKey(IWindow iWindow, String actionPrefix, Consumer<KeyEvent> consumer) {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#keyPressed");
            }

            @Override
            public void keyReleased(KeyEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#keyReleased");
            }

            @Override
            public void keyTyped(KeyEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#keyTyped");
            }
        };
    }

    public static DocumentListener wrapDocument(IWindow iWindow, String actionPrefix, Consumer<DocumentEvent> consumer) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#insertUpdate");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#removeUpdate");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                iWindow.getWindowExecutor().execute(() -> consumer.accept(e), actionPrefix + "#changedUpdate");
            }
        };
    }
}
