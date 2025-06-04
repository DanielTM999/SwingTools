package dtm.stools.activity;

import dtm.stools.configs.SystemTrayConfiguration;
import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.core.TrayEventType;
import dtm.stools.models.SystemTrayConfigurationConcrete;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Activity extends JFrame implements IWindow {

    private AtomicBoolean initialized = new AtomicBoolean(false);
    private final SystemTrayConfiguration systemTrayConfiguration;
    protected SystemTray tray;
    protected Image trayImage;
    protected TrayIcon trayIcon;

    protected Activity(){
        this.systemTrayConfiguration = new SystemTrayConfigurationConcrete();
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected Activity(String title){
        super(title);
        this.systemTrayConfiguration = new SystemTrayConfigurationConcrete();
        WindowContext.pushWindow(this);
        addEvents();
    }

    @Override
    public void init(){
        if(initialized.compareAndSet(false, true)) {
            applySystemTrayConfiguration(systemTrayConfiguration);
            onDrawing();
            SwingUtilities.invokeLater(() -> setVisible(true));
        };
    }

    @Override
    public void dispose() {
        safelyRemoveTrayIcon();
        WindowContext.removeWindow(this);
        super.dispose();
    }

    protected boolean isSystemTrayEnable(){
        return systemTrayConfiguration.isAvaiable();
    }

    protected void onDrawing() {
        setupWindow();
        setupSystemTray();
    }

    protected void onLoad(){}

    protected void onClose(){
        if(systemTrayConfiguration.isAvaiable()){
            int option = JOptionPane.showOptionDialog(
                    this,
                    "Deseja minimizar para a bandeja ou fechar o aplicativo?",
                    "Fechar aplicação",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new String[]{"Minimizar", "Fechar"},
                    "Minimizar"
            );

            if (option == JOptionPane.YES_OPTION) {
                minimizeToTray();
            } else if (option == JOptionPane.NO_OPTION) {
                exitApplication();
            }
        }
    }

    protected void onLostFocus(){}

    protected void onError(Throwable error){}

    protected void onSystemTrayClick(MouseEvent event, TrayEventType eventType){
        if(eventType == TrayEventType.MOUSE_CLICKED && event.getButton() == MouseEvent.BUTTON1){
            restoreFromTray();
        }
    }

    protected void applySystemTrayConfiguration(SystemTrayConfiguration systemTrayConfiguration){}

    protected void addSystemTray() {
        trayIcon = new TrayIcon(trayImage, getTitle());
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onSystemTrayClick(e, TrayEventType.MOUSE_CLICKED);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                onSystemTrayClick(e, TrayEventType.MOUSE_PRESSED);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                onSystemTrayClick(e, TrayEventType.MOUSE_RELEASED);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                onSystemTrayClick(e, TrayEventType.MOUSE_ENTERED);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                onSystemTrayClick(e, TrayEventType.MOUSE_EXITED);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                onSystemTrayClick(e, TrayEventType.MOUSE_WHEEL_MOVED);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                onSystemTrayClick(e, TrayEventType.MOUSE_DRAGGED);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                onSystemTrayClick(e, TrayEventType.MOUSE_MOVED);
            }
        });
    }

    protected void restoreFromTray() {
        if(systemTrayConfiguration.removeOnRestore()) safelyRemoveTrayIcon();;
        this.setVisible(true);
        this.setExtendedState(JFrame.NORMAL);
    }

    protected void minimizeToTray() {
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            onError(e);
        }
        if (tray != null && trayIcon != null) {
            this.setVisible(false);
            this.setExtendedState(JFrame.ICONIFIED);
        }
    }

    protected void exitApplication() {
        safelyRemoveTrayIcon();
        System.exit(0);
    }

    private void setupSystemTray(){
        if(!systemTrayConfiguration.isAvaiable()) return;
        tray = SystemTray.getSystemTray();
        trayImage = systemTrayConfiguration.getImageIcon().getImage();
        addSystemTray();
    }

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

    private void safelyRemoveTrayIcon() {
        if (tray != null && trayIcon != null) {
            tray.remove(trayIcon);
        }
    }


}
