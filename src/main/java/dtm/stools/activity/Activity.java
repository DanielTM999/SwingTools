package dtm.stools.activity;

import dtm.stools.configs.SystemTrayConfiguration;
import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.core.TrayEventType;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.models.SystemTrayConfigurationConcrete;
import lombok.NonNull;
import lombok.SneakyThrows;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class Activity extends JFrame implements IWindow {
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final SystemTrayConfiguration systemTrayConfiguration;
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private Future<Void> loadDomList;
    protected SystemTray tray;
    protected Image trayImage;
    protected TrayIcon trayIcon;


    protected Activity(){
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.systemTrayConfiguration = new SystemTrayConfigurationConcrete();
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected Activity(String title){
        super(title);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.systemTrayConfiguration = new SystemTrayConfigurationConcrete();
        WindowContext.pushWindow(this);
        addEvents();
    }

    @Override
    public void init(){
        if(initialized.compareAndSet(false, true)) {
            applySystemTrayConfiguration(systemTrayConfiguration);
            onDrawing();
            loadDomList = loadDomView();
            SwingUtilities.invokeLater(() -> setVisible(true));
        };
    }

    @Override
    public void dispose() {
        safelyRemoveTrayIcon();
        WindowContext.removeWindow(this);
        super.dispose();
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    @Override
    public <T extends Component> T findById(@NonNull String id) {
        if (loadDomList != null) {
            try(executorService){
                loadDomList.get();
            }
        } else {
            throw new DomNotLoadException("DomView ainda não foi iniciado.");
        }

        List<Component> components = domViewer.get(id);
        if (components != null && !components.isEmpty()) {
            return (T) components.getFirst();
        }

        throw new DomElementNotFoundException("Componente com id '" + id + "' não encontrado.");
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

    private Future<Void> loadDomView(){
        return CompletableFuture.runAsync(this::loadThis, executorService);
    }

    private void loadThis(){
        List<Component> rootList = this.domViewer.computeIfAbsent("root", k ->
                Collections.synchronizedList(new ArrayList<>())
        );
        rootList.add(this);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Component component : this.getComponents()) {
            futures.add(CompletableFuture.runAsync(() ->
                            collectComponentsRecursive(component)
                    , executorService));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    }

    private void collectComponentsRecursive(Component component) {
        if (component == null) return;

        String name = component.getName();
        if (name != null && !name.isBlank()) {
            domViewer
                    .computeIfAbsent(name, k -> Collections.synchronizedList(new ArrayList<>()))
                    .add(component);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                collectComponentsRecursive(child);
            }
        }
    }


}
