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
import lombok.NonNull;
import lombok.SneakyThrows;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
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
        this.windowExecutor = new ActivityWindowExecutor(this::onError);
    }

    protected Activity(String title){
        super(title);
        this.executorService = Executors.newVirtualThreadPerTaskExecutor();
        this.domViewer = new ConcurrentHashMap<>();
        this.clientSideElements = new ConcurrentHashMap<>();
        this.systemTrayConfiguration = new SystemTrayConfigurationConcrete();
        this.domElementLoader = new DomElementLoaderService<>(this, this.domViewer, this.executorService);
        WindowContext.pushWindow(this);
        this.windowExecutor = new ActivityWindowExecutor(this::onError);
    }

    /**
     * Inicializa a janela da atividade.
     * Este método deve ser chamado explicitamente após a criação da instância.
     *
     * <p>Executa a configuração da bandeja do sistema (se disponível),
     * chama o método {@code onDrawing()} para configurar a interface,
     * e realiza o carregamento automático do DOM.
     *
     * <p>Esse método só será executado uma vez, mesmo que chamado múltiplas vezes.
     */
    @Override
    public void init(){
        windowExecutor.execute(() -> {
            if(initialized.compareAndSet(false, true)) {
                applySystemTrayConfiguration(systemTrayConfiguration);
                setupSystemTray();
                onDrawing();
                this.domElementLoader.load();
                addEvents();
                SwingUtilities.invokeLater(() -> setVisible(true));
            };
        }, "init");
    }

    /**
     * Realiza a finalização da atividade, encerrando recursos como Executor e removendo o ícone da bandeja.
     * Este método também remove a janela do contexto global {@code WindowContext}.
     */
    @Override
    public void dispose() {
        windowExecutor.execute(() -> {
            if (!executorService.isShutdown()) executorService.shutdownNow();
            safelyRemoveTrayIcon();
            WindowContext.removeWindow(this);
            super.dispose();
        }, "dispose");
    }

    /**
     * Recupera todos os componentes associados ao ID especificado.
     *
     * @param id ID do componente.
     * @return Lista de componentes encontrados ou lista vazia.
     * @throws DomNotLoadException se o DOM ainda não tiver sido carregado.
     */
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

    /**
     * Recupera o primeiro componente associado ao ID especificado.
     *
     * @param id ID do componente.
     * @param <T> Tipo do componente.
     * @return Primeiro componente encontrado com o ID.
     * @throws DomElementNotFoundException se nenhum componente for encontrado com o ID.
     * @throws DomNotLoadException se o DOM ainda não tiver sido carregado.
     */
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

    /**
     * Recarrega os elementos do DOM, executando o processo de novo.
     */
    @Override
    public void reloadDomElements() {
        domElementLoader.reload();
    }

    /**
     * Armazena um valor no contexto da janela.
     *
     * @param key Chave de identificação.
     * @param value Valor a ser armazenado.
     * @return {@code true} se a chave ainda não existia.
     */
    @Override
    public boolean putInClient(String key, Object value) {
        return putInClient(key, value, false);
    }

    /**
     * Armazena um valor no contexto da janela com opção de sobrescrever.
     *
     * @param key Chave de identificação.
     * @param value Valor a ser armazenado.
     * @param replace Se {@code true}, sobrescreve o valor existente.
     * @return {@code true} se a chave foi adicionada ou sobrescrita.
     */
    @Override
    public boolean putInClient(String key, Object value, boolean replace) {
        if (replace) {
            clientSideElements.put(key, value);
            return true;
        }else{
            return clientSideElements.putIfAbsent(key, value) == null;
        }
    }

    /**
     * Recupera um valor armazenado no contexto da janela.
     *
     * @param key Chave de identificação.
     * @param <T> Tipo esperado do valor.
     * @return Valor associado ou {@code null} se não encontrado.
     */
    @Override
    public <T> T getFromClient(String key) {
        return getFromClient(key, null);
    }

    /**
     * Recupera um valor do contexto com valor padrão.
     *
     * @param key Chave de identificação.
     * @param defaultValue Valor padrão se a chave não existir.
     * @param <T> Tipo esperado do valor.
     * @return Valor armazenado ou valor padrão.
     */
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

    /**
     * Define se a bandeja do sistema está habilitada e disponível para uso.
     *
     * @return {@code true} se a bandeja está disponível e habilitada.
     */
    protected boolean isSystemTrayEnable(){
        return systemTrayConfiguration.isAvaiable();
    }

    /**
     * Responsável por desenhar os elementos da janela.
     * Este método é obrigatório e deve ser sobrescrito para configurar a UI.
     *
     * <p>Chamado automaticamente no {@code init()}.
     */
    protected void onDrawing() {
        setupWindow();
    }

    /**
     * Evento chamado quando a janela é aberta.
     * Pode ser sobrescrito para executar lógica inicial personalizada.
     *
     * @param e Evento de abertura da janela.
     */
    protected void onLoad(WindowEvent e) throws Exception{}

    /**
     * Evento chamado ao tentar fechar a janela.
     *
     * <p>O comportamento padrão exibe um diálogo perguntando se o usuário deseja minimizar
     * para a bandeja ou fechar a aplicação.
     *
     * <p>Este método pode ser sobrescrito, e o comportamento padrão pode ser preservado
     * com {@code super.onClose(e)}.
     *
     * @param e Evento de fechamento.
     */
    protected void onClose(WindowEvent e) throws Exception{
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

    /**
     * Evento chamado quando a janela perde o foco.
     * Pode ser sobrescrito para capturar perda de foco.
     *
     * @param e Evento de perda de foco.
     */
    protected void onLostFocus(WindowEvent e) throws Exception{}

    /**
     * Evento chamado quando a janela ganha o foco.
     * Pode ser sobrescrito para capturar ganho de foco.
     *
     * @param e Evento de foco.
     */
    protected void onFocus(WindowEvent e) throws Exception{}

    /**
     * Manipula erros ocorridos na atividade.
     * Deve ser sobrescrito para tratamento personalizado de erros relacionados ao ciclo de vida da janela.
     *
     * <p>Por padrão, este método apenas relança a exceção recebida.
     * Caso o desenvolvedor deseje adicionar lógica personalizada (como logging, notificações ou integração com serviços externos),
     * isso pode ser feito sobrescrevendo este método.
     * Se não tratado, o erro será relançado para o fluxo superior.</p>
     *
     * <strong>Nota:</strong> Este método trata exclusivamente falhas internas da estrutura da aplicação,
     * como erros durante {@code init()}, {@code onDrawing()}, {@code dispose()}, entre outros
     * pontos do ciclo de vida da {@code Activity}.
     * <br>
     * <strong>Ele <u>não</u> captura exceções lançadas por componentes adicionados pelo desenvolvedor</strong>,
     * como listeners personalizados, ações em botões ou lógicas arbitrárias associadas à interface gráfica.
     * <br>
     * Para esses casos, é responsabilidade do desenvolvedor implementar o tratamento local dessas exceções.
     *
     * @implNote Este método não cobre exceções geradas por interações arbitrárias definidas pelo usuário.
     * Tais exceções devem ser tratadas diretamente onde ocorrem ou encapsuladas em um mecanismo específico de captura.
     *
     * @param action Ação identificadora onde o erro ocorreu.
     * @param error  Exceção ou erro capturado.
     */
    protected void onError(String action, Throwable error) {
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        } else {
            String className = getClass().getName();
            String message = "Unhandled exception in [" + className + "] during action [" + action + "]";
            throw new RuntimeException(message, error);
        }
    }

    /**
     * Evento chamado quando o ícone da bandeja recebe interações do mouse.
     *
     * <p>Comportamento padrão: ao clicar com o botão esquerdo (BUTTON1),
     * restaura a janela da bandeja.
     *
     * <p>Pode ser sobrescrito. Se desejar manter o comportamento padrão,
     * chame {@code super.onSystemTrayClick(...)} dentro da sua implementação.
     *
     * @param event Evento de mouse.
     * @param eventType Tipo de evento da bandeja.
     */
    protected void onSystemTrayClick(MouseEvent event, TrayEventType eventType, Activity currentActivity){
        if(eventType == TrayEventType.MOUSE_CLICKED && event.getButton() == MouseEvent.BUTTON1){
            restoreFromTray();
        }
    }

    /**
     * Aplica a configuração da bandeja do sistema.
     * Pode ser sobrescrito para configurar ações personalizadas.
     *
     * @param systemTrayConfiguration Configuração da bandeja.
     */
    protected void applySystemTrayConfiguration(SystemTrayConfiguration systemTrayConfiguration){

    }

    /**
     * Adiciona o ícone da aplicação na bandeja do sistema.
     * Este método é chamado internamente e pode ser sobrescrito para adicionar menus personalizados.
     */
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

    /**
     * Restaura a janela da bandeja do sistema e torna todas as janelas visíveis novamente.
     */
    protected void restoreFromTray() {
        if(systemTrayConfiguration.isRemoveOnRestore()) safelyRemoveTrayIcon();
        Set<IWindow> windows = Arrays.stream(Window.getWindows())
                .filter(w -> w instanceof IWindow iwin)
                .map(w -> (IWindow) w)
                .collect(Collectors.toSet());

        for (IWindow iWindow: windows){
            iWindow.setVisible(true);
        }
        this.setExtendedState(JFrame.NORMAL);
    }

    /**
     * Minimiza a janela para a bandeja do sistema, ocultando todas as janelas do contexto.
     */
    protected void minimizeToTray() {
        safelyAddTrayIcon(!systemTrayConfiguration.isAlwaysVisible());
        if (tray != null && trayIcon != null) {
            Set<IWindow> windows = Arrays.stream(Window.getWindows())
                    .filter(w -> w instanceof IWindow iwin)
                    .map(w -> (IWindow) w)
                    .collect(Collectors.toSet());

            for (IWindow iWindow: windows){
                iWindow.setVisible(false);
            }

            this.setExtendedState(JFrame.ICONIFIED);
        }
    }

    /**
     * Encerra a aplicação removendo o ícone da bandeja e finalizando o processo com {@code System.exit(0)}.
     */
    protected void exitApplication() {
        safelyRemoveTrayIcon();
        System.exit(0);
    }

    /**
     * Retorna o executor principal da janela.
     *
     * @return ExecutorService principal da janela.
     */
    protected ExecutorService getMainExecutor(){
        return executorService;
    }

    /**
     * Executa um comando assíncrono usando o executor principal da janela.
     *
     * @param command Comando a ser executado.
     * @return {@code CompletableFuture} representando a execução.
     */
    protected CompletableFuture<?> runOnWindowExecutor(Runnable command){
        return CompletableFuture.runAsync(command, executorService);
    }

    /**
     * Executa uma operação assíncrona e retorna um resultado via {@code CompletableFuture}.
     *
     * @param command Operação a ser executada.
     * @param <T> Tipo do resultado.
     * @return {@code CompletableFuture} com o resultado.
     */
    protected <T> CompletableFuture<T> runOnWindowExecutor(Supplier<T> command){
        return CompletableFuture.supplyAsync(command, executorService);
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
            public void windowClosing(WindowEvent e) {
                windowExecutor.execute(() -> onClose(e), "onClose");
            }

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
    }

    private void setupWindow() {
        setSize(800, 600);
        setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    private void safelyRemoveTrayIcon() {
        if (tray != null && trayIcon != null && !systemTrayConfiguration.isAlwaysVisible()) {
            tray.remove(trayIcon);
        }
    }

    private void safelyAddTrayIcon(boolean callErrorHandler){
        try {
            tray.add(trayIcon);
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
