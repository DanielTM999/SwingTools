package dtm.stools.activity;

import dtm.stools.configs.SystemTrayConfiguration;
import dtm.stools.context.DomElementLoader;
import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.context.enums.TrayEventType;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.exceptions.InvalidClientSideElementException;
import dtm.stools.internal.DomElementLoaderService;
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
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public abstract class Activity extends JFrame implements IWindow {
    private final Map<String, Object> clientSideElements;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final SystemTrayConfiguration systemTrayConfiguration;
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private final DomElementLoader domElementLoader;
    protected SystemTray tray;
    protected Image trayImage;
    protected TrayIcon trayIcon;


    protected Activity(){
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.systemTrayConfiguration = new SystemTrayConfigurationConcrete();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        addEvents();
    }

    protected Activity(String title){
        super(title);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.systemTrayConfiguration = new SystemTrayConfigurationConcrete();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        addEvents();
    }

    @Override
    public void init(){
        if(initialized.compareAndSet(false, true)) {
            applySystemTrayConfiguration(systemTrayConfiguration);
            onDrawing();
            this.domElementLoader.load();
            SwingUtilities.invokeLater(() -> setVisible(true));
        };
    }

    @Override
    public void dispose() {
        if (!executorService.isShutdown()) executorService.shutdownNow();
        safelyRemoveTrayIcon();
        WindowContext.removeWindow(this);
        super.dispose();
    }

    @Override
    public List<Component> findAllById(@NonNull String id) {
        if (domElementLoader.isInitialized()) {
            if(!domElementLoader.isLoad())domElementLoader.completeLoad();
        } else {
            throw new DomNotLoadException("DomView ainda não foi iniciado.");
        }

        return domViewer.getOrDefault(id, Collections.EMPTY_LIST);
    }

    @Override
    public <T extends Component> T findById(@NonNull String id) {
        List<Component> components = findAllById(id);

        if(!components.isEmpty()){
            return (T)components.getFirst();
        }
        
        throw new DomElementNotFoundException("Componente com id '" + id + "' não encontrado.");
    }

    @Override
    public void reloadDomElements() {
        domElementLoader.reload();
    }

    @Override
    public boolean putInClient(String key, Object value) {
        return putInClient(key, value, false);
    }

    @Override
    public boolean putInClient(String key, Object value, boolean replace) {
        if (replace) {
            clientSideElements.put(key, value);
            return true;
        }else{
            return clientSideElements.putIfAbsent(key, value) == null;
        }
    }

    @Override
    public <T> T getFromClient(String key) {
        return getFromClient(key, null);
    }

    @Override
    public <T> T getFromClient(String key, T defaultValue) {
        final Object value = clientSideElements.getOrDefault(key, defaultValue);
        try{
            return (T)value;
        }catch (Exception e){
            throw new InvalidClientSideElementException(key, value, e);
        }
    }

    protected boolean isSystemTrayEnable(){
        return systemTrayConfiguration.isAvaiable();
    }

    protected void onDrawing() {
        setupWindow();
        setupSystemTray();
    }

    protected void onLoad(WindowEvent e){}

    protected void onClose(WindowEvent e){
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

    protected void onLostFocus(WindowEvent e){}

    protected void onFocus(WindowEvent e) {}

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

    protected ExecutorService getMainExecutor(){
        return executorService;
    }

    protected CompletableFuture<?> runOnWindowExecutor(Runnable command){
        return CompletableFuture.runAsync(command, executorService);
    }

    protected <T> CompletableFuture<T> runOnWindowExecutor(Supplier<T> command){
        return CompletableFuture.supplyAsync(command, executorService);
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
                onClose(e);
            }

            @Override
            public void windowOpened(WindowEvent e) {
                onLoad(e);
            }

            @Override
            public void windowLostFocus(WindowEvent e) { onLostFocus(e);}

            @Override
            public void windowGainedFocus(WindowEvent e) { onFocus(e);}
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
