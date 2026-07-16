package dtm.stools.component.panels.graphics;

import java.awt.*;
import java.awt.event.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GraphicsInputState implements GraphicsInput, KeyListener, MouseListener, MouseMotionListener, MouseWheelListener, FocusListener {

    private final Set<Integer> pressedKeys = ConcurrentHashMap.newKeySet();
    private final Set<Integer> pressedButtons = ConcurrentHashMap.newKeySet();
    private volatile int mouseX;
    private volatile int mouseY;
    private volatile boolean mouseInside;
    private volatile double wheelRotation;

    public void attach(Component component) {
        component.addKeyListener(this);
        component.addMouseListener(this);
        component.addMouseMotionListener(this);
        component.addMouseWheelListener(this);
        component.addFocusListener(this);
    }

    public void detach(Component component) {
        component.removeKeyListener(this);
        component.removeMouseListener(this);
        component.removeMouseMotionListener(this);
        component.removeMouseWheelListener(this);
        component.removeFocusListener(this);
    }

    @Override
    public boolean isKeyDown(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    @Override
    public boolean isAnyKeyDown() {
        return !pressedKeys.isEmpty();
    }

    @Override
    public boolean isMouseButtonDown(int button) {
        return pressedButtons.contains(button);
    }

    @Override
    public int getMouseX() {
        return mouseX;
    }

    @Override
    public int getMouseY() {
        return mouseY;
    }

    @Override
    public boolean isMouseInside() {
        return mouseInside;
    }

    @Override
    public double getWheelRotation() {
        return wheelRotation;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        pressedKeys.add(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        pressedKeys.remove(e.getKeyCode());
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        pressedButtons.add(e.getButton());
        updatePosition(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        pressedButtons.remove(e.getButton());
        updatePosition(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        mouseInside = true;
        updatePosition(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        mouseInside = false;
        updatePosition(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updatePosition(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        updatePosition(e);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        wheelRotation += e.getPreciseWheelRotation();
        updatePosition(e);
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void focusLost(FocusEvent e) {
        pressedKeys.clear();
        pressedButtons.clear();
    }

    private void updatePosition(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }
}
