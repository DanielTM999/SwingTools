package dtm.stools.activity;

import dtm.stools.configs.SystemTrayConfiguration;
import dtm.stools.context.DomElementLoader;
import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.context.WindowExecutor;
import dtm.stools.context.enums.TrayEventType;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.exceptions.InvalidClientSideElementException;
import dtm.stools.internal.DomElementLoaderService;
import dtm.stools.internal.window.ActivityWindowExecutor;
import dtm.stools.models.SystemTrayConfigurationConcrete;
import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public abstract class Activity extends JFrame implements IWindow {
    private final Map<String, Object> clientSideElements;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final SystemTrayConfiguration systemTrayConfiguration;
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private final DomElementLoader domElementLoader;
    private final WindowExecutor windowExecutor;
    private final AtomicInteger lastWindowStateRef = new AtomicInteger();
    private final AtomicBoolean inTray = new AtomicBoolean();
    private final AtomicBoolean closing = new AtomicBoolean(false);

    @Getter
    protected SystemTray tray;

    protected Image trayImage;

    @Getter
    protected TrayIcon trayIcon;

    {
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> {
            Thread t = new Thread(r);
            t.setName("Activity-Main-Worker-" + System.identityHashCode(this));
            t.setDaemon(true);
            return t;
        });
    }

    protected Activity(){
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.systemTrayConfiguration = new SystemTrayConfigurationConcrete();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError, executorService);
    }

    protected Activity(String title){
        super(title);
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.systemTrayConfiguration = new SystemTrayConfigurationConcrete();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError, executorService);
    }

    @Override
    public void init() {
        windowExecutor.execute(() -> {
            if (executorService.isShutdown() || executorService.isTerminated()) {
                return;
            }

            if (initialized.compareAndSet(false, true)) {
                try {
                    applySystemTrayConfiguration(systemTrayConfiguration);
                    setupSystemTray();
                    onDrawing();

                    if (!executorService.isShutdown()) {
                        this.domElementLoader.load();
                    }

                    addEvents();

                    if (!executorService.isShutdown()) {
                        SwingUtilities.invokeLater(() -> {
                            if (!executorService.isShutdown()) {
                                setVisible(true);
                            }
                        });
                    }
                } catch (IllegalStateException | java.util.concurrent.RejectedExecutionException e) {
                    if (!executorService.isShutdown()) {
                        throw e;
                    }
                } catch (Exception e) {
                    if (!executorService.isShutdown()) {
                        onError("init", e);
                    }
                }
            }
        }, "init");
    }

    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            handleCloseRequest(e);
            return;
        }

        super.processWindowEvent(e);
    }

    @Override
    public void requestClose() {
        handleCloseRequest(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
    }

    @Override
    public void dispose() {
        windowExecutor.execute(() -> {
            if (!executorService.isShutdown()) executorService.shutdown();
            safelyRemoveTrayIcon(true);
            WindowContext.removeWindow(this);
            super.dispose();
        }, "dispose");
    }

    @Override
    public List<Component> findAllById(@NonNull String id) {
       return windowExecutor.execute(() -> {
            if (domElementLoader.isInitialized()) {
                if(!domElementLoader.isLoad())domElementLoader.completeLoad();
            } else {
                throw new DomNotLoadException("DomView ainda não foi iniciado.");
            }
            return domViewer.getOrDefault(id, Collections.EMPTY_LIST);
       }, "findAllById");
    }

    @Override
    public <T extends Component> T findById(@NonNull String id) {
        return windowExecutor.execute(() -> {
            List<Component> components = findAllById(id);

            if(!components.isEmpty()){
                return (T)components.getFirst();
            }
            throw new DomElementNotFoundException("Componente com id '" + id + "' não encontrado.");
        }, "findById");
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
       return windowExecutor.execute(() -> {
           final Object value = clientSideElements.getOrDefault(key, defaultValue);
           try{
               return (T)value;
           }catch (Exception e){
               throw new InvalidClientSideElementException(key, value, e);
           }
       }, "getFromClient");
    }

    @Override
    public WindowExecutor getWindowExecutor() {
        return windowExecutor;
    }

    public boolean windowInTray(){
        if (tray != null) {
            for (TrayIcon icon : SystemTray.getSystemTray().getTrayIcons()) {
                if (icon == trayIcon && inTray.get()) return true;
            }
        }
        return false;
    }

    public boolean isSystemTrayEnable(){
        return systemTrayConfiguration.isAvaiable();
    }

    protected void onResize() {}

    protected void onMove() {}

    protected void onShow() {}

    protected void onHidden() {}

    protected void onDrawing() {
        setupWindow();
    }

    protected void onLoad(WindowEvent e) throws Exception{}

    protected void onClose(WindowEvent e) throws Exception{
        if(systemTrayConfiguration.isAvaiable()){
           callSystemTrayOnClose();
        }
    }

    protected void onLostFocus(WindowEvent e) throws Exception{}

    protected void onFocus(WindowEvent e) throws Exception{}

    protected void onError(String action, Throwable error) {
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        } else {
            String className = getClass().getName();
            String message = "Unhandled exception in [" + className + "] during action [" + action + "]";
            throw new RuntimeException(message, error);
        }
    }

    protected void onSystemTrayClick(MouseEvent event, TrayEventType eventType, Activity currentActivity){
        if(eventType == TrayEventType.MOUSE_CLICKED && event.getButton() == MouseEvent.BUTTON1){
            restoreFromTray();
        }
    }

    private void handleCloseRequest(WindowEvent e) {
        if (closing.compareAndSet(false, true)) {
            windowExecutor.execute(() -> {
                try {
                    onClose(e);
                } catch (Exception ex) {
                    onError("onClose", ex);
                } finally {
                    closing.set(false);
                }
            }, "onClose");
        }
    }

    protected void applySystemTrayConfiguration(SystemTrayConfiguration systemTrayConfiguration){

    }

    protected void addSystemTray() {
        trayIcon = new TrayIcon(trayImage, getTitle());
        trayIcon.setImageAutoSize(true);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                windowExecutor.execute(() -> onSystemTrayClick(e, TrayEventType.MOUSE_CLICKED, Activity.this), "trayMouseClicked");
            }

            @Override
            public void mousePressed(MouseEvent e) {
                windowExecutor.execute(() -> onSystemTrayClick(e, TrayEventType.MOUSE_PRESSED, Activity.this), "trayMousePressed");
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                windowExecutor.execute(() -> onSystemTrayClick(e, TrayEventType.MOUSE_RELEASED, Activity.this), "trayMouseReleased");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                windowExecutor.execute(() -> onSystemTrayClick(e, TrayEventType.MOUSE_ENTERED, Activity.this), "trayMouseEntered");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                windowExecutor.execute(() -> onSystemTrayClick(e, TrayEventType.MOUSE_EXITED, Activity.this), "trayMouseExited");
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                windowExecutor.execute(() -> onSystemTrayClick(e, TrayEventType.MOUSE_WHEEL_MOVED, Activity.this), "trayMouseWheelMoved");
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                windowExecutor.execute(() -> onSystemTrayClick(e, TrayEventType.MOUSE_DRAGGED, Activity.this), "trayMouseDragged");
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                windowExecutor.execute(() -> onSystemTrayClick(e, TrayEventType.MOUSE_MOVED, Activity.this), "trayMouseMoved");
            }
        });

    }

    protected void restoreFromTray() {
        if(systemTrayConfiguration.isRemoveOnRestore()) safelyRemoveTrayIcon();
        this.setVisible(true);
        this.toFront();
        requestFocus();
    }

    protected void minimizeToTray() {
        int state = getExtendedState() & ~JFrame.ICONIFIED;
        lastWindowStateRef.set(state);
        safelyAddTrayIcon(!systemTrayConfiguration.isAlwaysVisible());
        if (tray != null && trayIcon != null) {
            Set<IWindow> windows = Arrays.stream(Window.getWindows())
                    .filter(w -> w instanceof IWindow iwin)
                    .map(w -> (IWindow) w)
                    .collect(Collectors.toSet());

            for (IWindow iWindow: windows){
                iWindow.setVisible(false);
            }

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

    protected void callSystemTrayOnClose(){
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

    private void setupSystemTray(){
        if(!systemTrayConfiguration.isAvaiable() && this.tray != null) return;
        initSystemTray();
        trayImage = systemTrayConfiguration.getImage();
        if (trayImage == null) {
            trayImage = createDefaultTrayIcon();
        }
        addSystemTray();
        if(systemTrayConfiguration.isAlwaysVisible()) safelyAddTrayIcon(true);
    }

    private void initSystemTray(){
        if(!systemTrayConfiguration.isAvaiable()) return;
        this.tray = SystemTray.getSystemTray();
    }

    private void addEvents(){
        this.addWindowListener(new java.awt.event.WindowAdapter() {

            @Override
            public void windowOpened(WindowEvent e) {
                windowExecutor.execute(() -> onLoad(e), "onLoad");
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                windowExecutor.execute(() -> onLostFocus(e), "onLostFocus");
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                windowExecutor.execute(() -> onFocus(e), "onFocus");
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                windowExecutor.execute(() -> onResize(), "onResize");
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                windowExecutor.execute(() -> onMove(), "onMove");
            }

            @Override
            public void componentShown(ComponentEvent e) {
                windowExecutor.execute(() -> onShow(), "onShow");
            }

            @Override
            public void componentHidden(ComponentEvent e) {
                windowExecutor.execute(() -> onHidden(), "onHidden");
            }
        });
    }

    private void setupWindow() {
        setSize(800, 600);
        setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    private void safelyRemoveTrayIcon(){
        safelyRemoveTrayIcon(false);
    }

    private void safelyRemoveTrayIcon(boolean force) {
        if (tray != null && trayIcon != null) {
            if(force || !systemTrayConfiguration.isAlwaysVisible()){
                tray.remove(trayIcon);
                inTray.set(false);
            }
        }
    }

    private void safelyAddTrayIcon(boolean callErrorHandler){
        try {
            tray.add(trayIcon);
            inTray.set(true);
        } catch (Exception e) {
            if(callErrorHandler) onError("safelyAddTrayIcon", e);
        }
    }

    private Image createDefaultTrayIcon() {
        int size = 16;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, size, size);

        g.setComposite(AlphaComposite.Src);
        g.setColor(Color.BLUE);
        g.fillOval(2, 2, size - 4, size - 4);
        g.setColor(Color.WHITE);
        g.drawOval(2, 2, size - 4, size - 4);

        g.dispose();
        return image;
    }

}
