package dtm.stools.component.panels;

import dtm.stools.component.ViewPanel;
import dtm.stools.context.IWindowComponent;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class BlockingPanel extends ViewPanel implements IWindowComponent {

    private final AtomicBoolean locked = new AtomicBoolean(false);
    private final JComponent eventBlocker;

    protected BlockingPanel() {
        setOpaque(false);

        eventBlocker = createEventBlocker();
        eventBlocker.setVisible(false);

        super.add(eventBlocker);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                eventBlocker.setBounds(0, 0, getWidth(), getHeight());
            }
        });
    }

    protected BlockingPanel(LayoutManager layout){
        super(layout);
        setOpaque(false);

        eventBlocker = createEventBlocker();
        eventBlocker.setVisible(false);

        super.add(eventBlocker);
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                eventBlocker.setBounds(0, 0, getWidth(), getHeight());
            }
        });
    }

    public final void lockUI(){
        lockUI(null);
    }

    public final void lockUI(Consumer<BlockingPanel> onLock) {
        if (locked.compareAndSet(false, true)) {
            eventBlocker.setVisible(true);
            eventBlocker.setFocusable(true);
            eventBlocker.setRequestFocusEnabled(true);
            eventBlocker.requestFocusInWindow();
            if (onLock != null) {
                onLock.accept(this);
            }
            repaint();
        }
    }

    public final void unlockUI(){
        unlockUI(null);
    }

    public final void unlockUI(Consumer<BlockingPanel> onUnlock) {
        if (locked.compareAndSet(true, false)) {
            eventBlocker.setVisible(false);
            if (onUnlock != null) {
                onUnlock.accept(this);
            }
            repaint();
        }
    }

    public final boolean isLocked() {
        return locked.get();
    }

    @Override
    public Component add(Component comp) {
        Component c = super.add(comp);
        bringBlockerToFront();
        return c;
    }

    @Override
    protected void processMouseEvent(MouseEvent e) {
        if (locked.get()) {
            e.consume();
            return;
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (locked.get()) {
            e.consume();
            return;
        }
        super.processMouseMotionEvent(e);
    }

    @Override
    protected void processKeyEvent(KeyEvent e) {
        if (locked.get()) {
            e.consume();
            return;
        }
        super.processKeyEvent(e);
    }

    @Override
    public boolean isFocusable() {
        return !locked.get() && super.isFocusable();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }

    private void bringBlockerToFront() {
        setComponentZOrder(eventBlocker, 0);
        revalidate();
        repaint();
    }

    private JComponent createEventBlocker() {
        JPanel blocker = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };

        blocker.setOpaque(false);
        blocker.setFocusable(true);

        blocker.setAlignmentX(0.0f);
        blocker.setAlignmentY(0.0f);
        blocker.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

        MouseAdapter mouseEater = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { e.consume(); }
            @Override public void mouseReleased(MouseEvent e) { e.consume(); }
            @Override public void mouseClicked(MouseEvent e) { e.consume(); }
            @Override public void mouseEntered(MouseEvent e) { e.consume(); }
            @Override public void mouseExited(MouseEvent e) { e.consume(); }
            @Override public void mouseDragged(MouseEvent e) { e.consume(); }
            @Override public void mouseMoved(MouseEvent e) { e.consume(); }
        };

        blocker.addMouseListener(mouseEater);
        blocker.addMouseMotionListener(mouseEater);

        blocker.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { e.consume(); }
            @Override public void keyReleased(KeyEvent e) { e.consume(); }
            @Override public void keyTyped(KeyEvent e) { e.consume(); }
        });

        return blocker;
    }
}
