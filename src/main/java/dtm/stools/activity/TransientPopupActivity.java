package dtm.stools.activity;

import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;

import javax.swing.*;
import java.awt.event.WindowEvent;

public abstract class TransientPopupActivity extends JWindow implements IWindow {

    public TransientPopupActivity(){
        WindowContext.pushWindow(this);
    }

    @Override
    public void init(){
        onDrawing();
        setVisible(true);
    }

    protected void onDrawing() {
        setupWindow();
        addEvents();
    }

    protected void onLoad(){}

    protected void onClose(){}

    protected void onLostFocus(){}

    private void addEvents(){
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onClose();
            }

            @Override
            public void windowOpened(WindowEvent e) {
                onLoad();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                onLostFocus();
            }
        });
    }

    private void setupWindow() {
        setSize(800, 600);
        setLocationRelativeTo(null);
    }

}
