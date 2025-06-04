package dtm.stools.activity;

import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import javax.swing.*;
import java.awt.event.WindowEvent;

public abstract class FragmentActivity extends JDialog implements IWindow {

    protected FragmentActivity(){
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected FragmentActivity(JFrame owner, boolean modal){
        super(owner, modal);
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected FragmentActivity(JFrame owner, String title, boolean modal){
        super(owner, title, modal);
        WindowContext.pushWindow(this);
        addEvents();
    }

    @Override
    public void init(){
        onDrawing();
        SwingUtilities.invokeLater(() -> setVisible(true));
    }

    @Override
    public void dispose() {
        WindowContext.removeWindow(this);
        super.dispose();
    }

    protected void onDrawing() {
        setupWindow();
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
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }


}
