package dtm.stools.activity;

import dtm.stools.configs.SystemTrayConfiguration;
import dtm.stools.context.DomElementLoader;
import dtm.stools.context.IWindow;
import dtm.stools.context.WindowContext;
import dtm.stools.context.WindowExecutor;
import dtm.stools.context.enums.TrayEventType;
import dtm.stools.context.enums.TrayIconScope;
import dtm.stools.exceptions.DomElementNotFoundException;
import dtm.stools.exceptions.DomNotLoadException;
import dtm.stools.exceptions.InvalidClientSideElementException;
import dtm.stools.internal.DomElementLoaderService;
import dtm.stools.internal.window.ActivityWindowExecutor;
import dtm.stools.internal.DrawingOnceGate;
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
    private static final Object SHARED_TRAY_LOCK = new Object();
    private static final Deque<Activity> SHARED_TRAY_MEMBERS = new ArrayDeque<>();
    private static TrayIcon sharedTrayIcon;
    private static boolean sharedTrayIconAdded;

    private final DrawingOnceGate drawingOnceGate = new DrawingOnceGate();
    private final Map<String, Object> clientSideElements;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final SystemTrayConfiguration systemTrayConfiguration;
    private final ExecutorService executorService;
    private final Map<String, List<Component>> domViewer;
    private final DomElementLoader domElementLoader;
    private final WindowExecutor windowExecutor;
    private final AtomicInteger lastWindowStateRef = new AtomicInteger();
    private final AtomicBoolean inTray = new AtomicBoolean();
    private volatile boolean sharedTrayMember;
    private final AtomicBoolean trayUsable = new AtomicBoolean(true);
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
                    dispatchDrawing();

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
        if (tray == null || trayIcon == null || !isTrayIconAdded()) {
            return false;
        }
        for (TrayIcon icon : SystemTray.getSystemTray().getTrayIcons()) {
            if (icon == trayIcon) return true;
        }
        return false;
    }

    public boolean isSystemTrayEnable(){
        return systemTrayConfiguration.isAvaiable();
    }

    public boolean isSystemTrayUsable(){
        return systemTrayConfiguration.isAvaiable() && trayUsable.get() && tray != null && trayIcon != null;
    }

    protected void onResize() {}

    protected void onMove() {}

    protected void onShow() {}

    protected void onHidden() {}

    /**
     * Faz com que o {@code onDrawing()} seja executado apenas uma vez durante
     * todo o ciclo de vida deste componente, mesmo que o gatilho de desenho
     * seja disparado novamente.
     */
    protected final void applyDrawingOnce() {
        drawingOnceGate.enable();
    }

    /** Dispara o {@code onDrawing()} respeitando o {@link #applyDrawingOnce()}. */
    protected final void dispatchDrawing() {
        drawingOnceGate.dispatch(this::onDrawing);
    }

    protected void onDrawing() {
        setupWindow();
    }

    protected void onLoad(WindowEvent e) throws Exception{}

    protected void onClose(WindowEvent e) throws Exception{
        if(isSystemTrayUsable()){
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
        if (systemTrayConfiguration.getTrayIconScope() == TrayIconScope.ACTIVITY) {
            sharedTrayMember = false;
            trayIcon = createTrayIcon(trayImage, getTitle(), () -> this);
            return;
        }

        synchronized (SHARED_TRAY_LOCK) {
            sharedTrayMember = true;
            SHARED_TRAY_MEMBERS.remove(this);
            SHARED_TRAY_MEMBERS.addLast(this);
            if (sharedTrayIcon == null) {
                sharedTrayIcon = createTrayIcon(trayImage, getTitle(), Activity::currentSharedTrayTarget);
            }
            trayIcon = sharedTrayIcon;
        }
    }

    public void restoreFromTray() {
        if(systemTrayConfiguration.isRemoveOnRestore()) safelyRemoveTrayIcon();
        if (sharedTrayMember) {
            for (Activity member : sharedTrayMembersSnapshot()) {
                if (member != this && member.isDisplayable() && !member.isVisible()) {
                    member.setVisible(true);
                }
            }
        }
        this.setVisible(true);
        this.toFront();
        requestFocus();
    }

    protected void minimizeToTray() {
        int state = getExtendedState() & ~JFrame.ICONIFIED;
        lastWindowStateRef.set(state);
        boolean iconVisible = safelyAddTrayIcon(!systemTrayConfiguration.isAlwaysVisible());
        if (!iconVisible) {
            setExtendedState(getExtendedState() | JFrame.ICONIFIED);
            return;
        }

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
        if (!systemTrayConfiguration.isAvaiable()) {
            return;
        }

        if (!SystemTray.isSupported()) {
            return;
        }

        if (tray != null) {
            return;
        }

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
            public void windowActivated(WindowEvent e) {
                markAsRecentTrayMember();
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
        if (tray == null || trayIcon == null) {
            return;
        }

        synchronized (SHARED_TRAY_LOCK) {
            if (force && sharedTrayMember) {
                SHARED_TRAY_MEMBERS.remove(this);
                if (!SHARED_TRAY_MEMBERS.isEmpty()) {
                    refreshSharedTrayToolTip();
                    return;
                }
                tray.remove(trayIcon);
                setTrayIconAdded(false);
                if (sharedTrayIcon == trayIcon) {
                    sharedTrayIcon = null;
                }
                return;
            }

            if (force || !systemTrayConfiguration.isAlwaysVisible()) {
                tray.remove(trayIcon);
                setTrayIconAdded(false);
            }
        }
    }

    private boolean safelyAddTrayIcon(boolean callErrorHandler){
        if (tray == null || trayIcon == null) {
            return false;
        }

        if (isTrayIconAdded()) {
            return true;
        }

        if (!trayUsable.get()) {
            return false;
        }

        Exception failure;
        synchronized (SHARED_TRAY_LOCK) {
            if (isTrayIconAdded()) {
                return true;
            }
            try {
                tray.add(trayIcon);
                setTrayIconAdded(true);
                return true;
            } catch (AWTException e) {
                trayUsable.set(false);
                setTrayIconAdded(false);
                return false;
            } catch (Exception e) {
                failure = e;
            }
        }

        if(callErrorHandler) onError("safelyAddTrayIcon", failure);
        return false;
    }

    private boolean isTrayIconAdded() {
        if (!sharedTrayMember) {
            return inTray.get();
        }
        synchronized (SHARED_TRAY_LOCK) {
            return sharedTrayIconAdded;
        }
    }

    private void setTrayIconAdded(boolean added) {
        if (!sharedTrayMember) {
            inTray.set(added);
            return;
        }
        synchronized (SHARED_TRAY_LOCK) {
            sharedTrayIconAdded = added;
        }
    }

    private void markAsRecentTrayMember() {
        synchronized (SHARED_TRAY_LOCK) {
            if (trayIcon == null || !SHARED_TRAY_MEMBERS.remove(this)) {
                return;
            }
            SHARED_TRAY_MEMBERS.addLast(this);
            refreshSharedTrayToolTip();
        }
    }

    private static List<Activity> sharedTrayMembersSnapshot() {
        synchronized (SHARED_TRAY_LOCK) {
            return new ArrayList<>(SHARED_TRAY_MEMBERS);
        }
    }

    private static Activity currentSharedTrayTarget() {
        synchronized (SHARED_TRAY_LOCK) {
            Iterator<Activity> iterator = SHARED_TRAY_MEMBERS.descendingIterator();
            while (iterator.hasNext()) {
                Activity candidate = iterator.next();
                if (candidate.isDisplayable()) {
                    return candidate;
                }
            }
            return SHARED_TRAY_MEMBERS.peekLast();
        }
    }

    private static void refreshSharedTrayToolTip() {
        TrayIcon icon = sharedTrayIcon;
        Activity target = SHARED_TRAY_MEMBERS.peekLast();
        if (icon != null && target != null) {
            icon.setToolTip(target.getTitle());
        }
    }

    private static void dispatchTrayEvent(Supplier<Activity> targetSupplier, MouseEvent event, TrayEventType eventType, String action) {
        Activity target = targetSupplier.get();
        if (target == null) {
            return;
        }
        target.windowExecutor.execute(() -> target.onSystemTrayClick(event, eventType, target), action);
    }

    private static TrayIcon createTrayIcon(Image image, String toolTip, Supplier<Activity> targetSupplier) {
        TrayIcon icon = new TrayIcon(image, toolTip);
        icon.setImageAutoSize(true);
        icon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                dispatchTrayEvent(targetSupplier, e, TrayEventType.MOUSE_CLICKED, "trayMouseClicked");
            }

            @Override
            public void mousePressed(MouseEvent e) {
                dispatchTrayEvent(targetSupplier, e, TrayEventType.MOUSE_PRESSED, "trayMousePressed");
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dispatchTrayEvent(targetSupplier, e, TrayEventType.MOUSE_RELEASED, "trayMouseReleased");
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                dispatchTrayEvent(targetSupplier, e, TrayEventType.MOUSE_ENTERED, "trayMouseEntered");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                dispatchTrayEvent(targetSupplier, e, TrayEventType.MOUSE_EXITED, "trayMouseExited");
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                dispatchTrayEvent(targetSupplier, e, TrayEventType.MOUSE_WHEEL_MOVED, "trayMouseWheelMoved");
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                dispatchTrayEvent(targetSupplier, e, TrayEventType.MOUSE_DRAGGED, "trayMouseDragged");
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                dispatchTrayEvent(targetSupplier, e, TrayEventType.MOUSE_MOVED, "trayMouseMoved");
            }
        });
        return icon;
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
