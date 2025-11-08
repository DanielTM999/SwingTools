package dtm.stools.component.panels.filepicker;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.SearchTextField;
import dtm.stools.component.inputfields.filepicker.FileSelectionMode;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.component.panels.filepicker.models.FileWrapperTableModel;
import dtm.stools.component.panels.filepicker.renderers.*;
import dtm.stools.component.panels.filepicker.utils.FileTreeNode;
import dtm.stools.component.panels.filepicker.utils.FileWrapper;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class FilePickerInputPanel extends PanelEventListener {

    private final ExecutorService executor;
    private final Set<File> selectedFiles;
    private final Path basePath;
    private final AtomicReference<FileSelectionMode> fileSelectionModeAtomicReference;
    private final AtomicBoolean multiSelect;
    private final AtomicBoolean showHiddenFiles;

    private Path navigatorPath;

    private JPanel pnlToolbar;
    private SearchTextField<Path> txtPath;
    private SearchTextField<FileWrapper> txtSearchFolder;
    private JButton btnUp;

    private JSplitPane splitPane;
    private JTree treeNavigation;
    private JTable tableFileView;
    private JScrollPane treeScrollPane;
    private JScrollPane tableScrollPane;

    private JList<FileWrapper> listFileView;
    private JScrollPane listScrollPane;
    private DefaultListModel<FileWrapper> listFileViewModel;

    private JPanel pnlViewSwitcher;
    private CardLayout viewCardLayout;

    private JPanel pnlLeft;
    private JScrollPane selectionScrollPane;
    private JList<File> listSelectedFiles;
    private DefaultListModel<File> listSelectedFilesModel;

    private JPanel pnlBottomBar;
    private JLabel lblSelectionCount;
    private JButton btnClearSelection;
    private JButton btnConfirmSelection;

    private JToolBar pnlConfigBar;
    private JTextField txtFilterView;
    private JToggleButton btnShowHidden;
    private JToggleButton btnListView;
    private JToggleButton btnGridView;

    private FileWrapperTableModel tableModel;
    private FileSystemView fsv;
    private FileNameExtensionFilter currentFileFilter;

    private JPopupMenu contextMenu;
    private JMenuItem miNewFolder;
    private JMenuItem miRefresh;
    private JMenuItem miRename;
    private JMenuItem miDelete;
    private JMenuItem miMarkAsSelected;

    private JPopupMenu selectionListContextMenu;
    private JMenuItem miRemoveSelected;
    private JMenuItem miRemoveAllSelected;

    private Consumer<FilePickerInputPanel> onEndSelectionListener;

    public FilePickerInputPanel() {
        this(System.getProperty("user.home"));
    }

    public FilePickerInputPanel(String path) {
        this(Path.of(path));
    }

    public FilePickerInputPanel(Path path) {
        this.fsv = FileSystemView.getFileSystemView();
        this.selectedFiles = ConcurrentHashMap.newKeySet();
        this.listSelectedFilesModel = new DefaultListModel<>();
        this.fileSelectionModeAtomicReference = new AtomicReference<>(FileSelectionMode.FILES_AND_DIRECTORIES);
        this.multiSelect = new AtomicBoolean(true);
        this.showHiddenFiles = new AtomicBoolean(false);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            this.basePath = path.toAbsolutePath();
        } else {
            this.basePath = Path.of(System.getProperty("user.home")).toAbsolutePath();
        }

        this.navigatorPath = basePath;
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        initComponents();
        createContextMenu();
        createSelectionListContextMenu();
        initListeners();

        setMultiSelectionEnabled(true);
        navigateTo(navigatorPath);
    }
    

    public void setOnEndSelection(Consumer<FilePickerInputPanel> listener) {
        this.onEndSelectionListener = listener;
    }

    public void setMultiSelectionEnabled(boolean enabled) {
        multiSelect.set(enabled);
        lblSelectionCount.setVisible(enabled);
        btnClearSelection.setVisible(enabled);
        selectionScrollPane.setVisible(enabled);

        tableFileView.setSelectionMode(enabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
        listFileView.setSelectionMode(enabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);

        if (!enabled) {
            clearSelection();
        }
        btnConfirmSelection.setText(enabled ? "Selecionar" : "Abrir");
    }

    public void setFileSelectionMode(FileSelectionMode mode) {
        fileSelectionModeAtomicReference.set(mode != null ? mode : FileSelectionMode.FILES_AND_DIRECTORIES);
        clearSelection();
        navigateTo(navigatorPath);
    }

    public void setFileFilter(FileNameExtensionFilter filter) {
        this.currentFileFilter = filter;
        if (txtFilterView != null) {
            txtFilterView.setText(filter != null ? filter.getDescription() : "Todos os arquivos");
        }
        navigateTo(navigatorPath);
    }

    public void setShowHiddenFiles(boolean show) {
        showHiddenFiles.set(show);
        if (btnShowHidden != null) {
            btnShowHidden.setSelected(show);
        }
        navigateTo(navigatorPath);
    }

    public Set<File> getSelectedFiles() {
        return Collections.unmodifiableSet(selectedFiles);
    }

    public File getSelectedFile() {
        if (selectedFiles.isEmpty()) {
            return null;
        }
        return selectedFiles.iterator().next();
    }

    private void initComponents() {
        pnlToolbar = new JPanel(new BorderLayout(5, 0));

        txtPath = new SearchTextField<>();
        txtPath.useStartsWithSearch();
        txtPath.setCaseSensitive(false);
        txtPath.setShowPopup(false);

        btnUp = new JButton("↑");
        btnUp.setToolTipText("Subir um nível");
        btnUp.setMargin(new Insets(2, 5, 2, 5));

        txtSearchFolder = new SearchTextField<>(null);
        txtSearchFolder.setPlaceholder("perquisar...");
        txtSearchFolder.setPreferredSize(new Dimension(200, (int) txtSearchFolder.getPreferredSize().getHeight()));

        JPanel pnlPath = new JPanel(new BorderLayout(5, 0));
        pnlPath.add(btnUp, BorderLayout.WEST);
        pnlPath.add(txtPath, BorderLayout.CENTER);

        pnlToolbar.add(pnlPath, BorderLayout.CENTER);
        pnlToolbar.add(txtSearchFolder, BorderLayout.EAST);

        pnlLeft = new JPanel(new BorderLayout());
        treeNavigation = new JTree();
        setupNavigationTree();
        treeScrollPane = new JScrollPane(treeNavigation);
        treeScrollPane.setMinimumSize(new Dimension(180, 100));

        listSelectedFiles = new JList<>(listSelectedFilesModel);
        listSelectedFiles.setCellRenderer(new FileListCellRenderer(fsv));
        selectionScrollPane = new JScrollPane(listSelectedFiles);
        selectionScrollPane.setBorder(BorderFactory.createTitledBorder("Seleção"));
        selectionScrollPane.setPreferredSize(new Dimension(0, 150));

        pnlLeft.add(treeScrollPane, BorderLayout.CENTER);
        pnlLeft.add(selectionScrollPane, BorderLayout.SOUTH);

        tableModel = new FileWrapperTableModel();
        tableFileView = new JTable(tableModel);
        tableFileView.setFillsViewportHeight(true);
        tableFileView.setShowGrid(false);
        tableFileView.setIntercellSpacing(new Dimension(0, 0));
        tableFileView.setDefaultRenderer(FileWrapper.class, new FileWrapperCellRenderer(fsv));
        tableFileView.setDefaultRenderer(Long.class, new FileSizeCellRenderer());
        tableFileView.setDefaultRenderer(Date.class, new DefaultTableCellRenderer() {
            private final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Date) {
                    value = format.format((Date) value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        tableFileView.getColumnModel().getColumn(0).setPreferredWidth(250);
        tableFileView.getColumnModel().getColumn(1).setPreferredWidth(120);
        tableFileView.getColumnModel().getColumn(2).setPreferredWidth(80);
        tableScrollPane = new JScrollPane(tableFileView);

        listFileViewModel = new DefaultListModel<>();
        listFileView = new JList<>(listFileViewModel);
        listFileView.setCellRenderer(new FileWrapperGridCellRenderer(fsv));
        listFileView.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        listFileView.setVisibleRowCount(-1);
        listFileView.setFixedCellWidth(100);
        listFileView.setFixedCellHeight(100);
        listScrollPane = new JScrollPane(listFileView);

        viewCardLayout = new CardLayout();
        pnlViewSwitcher = new JPanel(viewCardLayout);
        pnlViewSwitcher.add(tableScrollPane, "TABLE");
        pnlViewSwitcher.add(listScrollPane, "GRID");

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, pnlLeft, pnlViewSwitcher);
        splitPane.setDividerLocation(220);
        splitPane.setDividerSize(5);

        pnlBottomBar = new JPanel(new BorderLayout(10, 0));
        pnlBottomBar.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        pnlConfigBar = new JToolBar();
        pnlConfigBar.setFloatable(false);
        pnlConfigBar.setBorder(new EmptyBorder(0,0,0,0));

        txtFilterView = new JTextField("Todos os arquivos");
        txtFilterView.setEditable(false);
        txtFilterView.setPreferredSize(new Dimension(180, (int) txtFilterView.getPreferredSize().getHeight()));

        btnShowHidden = new JToggleButton("Ocultos");
        btnShowHidden.setToolTipText("Exibir arquivos ocultos");

        btnListView = new JToggleButton("Lista");
        btnGridView = new JToggleButton("Grade");
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(btnListView);
        viewGroup.add(btnGridView);
        btnListView.setSelected(true);

        pnlConfigBar.add(txtFilterView);
        pnlConfigBar.addSeparator();
        pnlConfigBar.add(btnShowHidden);
        pnlConfigBar.add(btnListView);
        pnlConfigBar.add(btnGridView);

        pnlBottomBar.add(pnlConfigBar, BorderLayout.WEST);

        lblSelectionCount = new JLabel("Nenhum item selecionado");
        lblSelectionCount.setHorizontalAlignment(SwingConstants.CENTER);
        pnlBottomBar.add(lblSelectionCount, BorderLayout.CENTER);

        btnClearSelection = new JButton("Limpar Seleção");
        btnConfirmSelection = new JButton("Selecionar");

        JPanel pnlBottomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        pnlBottomButtons.add(btnClearSelection);
        pnlBottomButtons.add(btnConfirmSelection);

        pnlBottomBar.add(pnlBottomButtons, BorderLayout.EAST);

        this.add(pnlToolbar, BorderLayout.NORTH);
        this.add(splitPane, BorderLayout.CENTER);
        this.add(pnlBottomBar, BorderLayout.SOUTH);
    }

    private void setupNavigationTree() {
        FileTreeNode root = new FileTreeNode("Raiz");

        File home = fsv.getHomeDirectory();
        FileTreeNode nodeHome = new FileTreeNode(home);
        root.add(nodeHome);

        String userHome = System.getProperty("user.home");
        File desktop = new File(userHome + File.separator + "Desktop");
        if (desktop.exists() && desktop.isDirectory()) {
            root.add(new FileTreeNode(desktop));
        }
        File downloads = new File(userHome + File.separator + "Downloads");
        if (downloads.exists() && downloads.isDirectory()) {
            root.add(new FileTreeNode(downloads));
        }
        File documents = new File(userHome + File.separator + "Documents");
        if (!documents.exists() || !documents.isDirectory()) {
            documents = fsv.getDefaultDirectory();
        }
        root.add(new FileTreeNode(documents));

        File pictures = new File(userHome + File.separator + "Pictures");
        if (pictures.exists() && pictures.isDirectory()) {
            root.add(new FileTreeNode(pictures));
        }
        File music = new File(userHome + File.separator + "Music");
        if (music.exists() && music.isDirectory()) {
            root.add(new FileTreeNode(music));
        }
        File videos = new File(userHome + File.separator + "Videos");
        if (videos.exists() && videos.isDirectory()) {
            root.add(new FileTreeNode(videos));
        }

        FileTreeNode nodeDiscos = new FileTreeNode("Discos");
        File[] roots = File.listRoots();
        if (roots != null) {
            for (File disk : roots) {
                nodeDiscos.add(new FileTreeNode(disk));
            }
        }
        root.add(nodeDiscos);

        treeNavigation.setModel(new DefaultTreeModel(root));
        treeNavigation.setRootVisible(false);
        treeNavigation.setCellRenderer(new FileTreeCellRenderer(fsv));

        treeNavigation.setRowHeight(22);
        treeNavigation.setShowsRootHandles(true);
        treeNavigation.expandPath(new TreePath(nodeHome.getPath()));
        treeNavigation.expandPath(new TreePath(nodeDiscos.getPath()));
    }

    private void createContextMenu() {
        contextMenu = new JPopupMenu();
        miMarkAsSelected = new JMenuItem("Marcar como Selecionado");
        miRename = new JMenuItem("Renomear");
        miDelete = new JMenuItem("Excluir");
        miNewFolder = new JMenuItem("Nova Pasta");
        miRefresh = new JMenuItem("Atualizar");

        miMarkAsSelected.addActionListener(e -> markSelectedFiles());
        miRename.addActionListener(e -> renameFile());
        miDelete.addActionListener(e -> deleteFiles());
        miNewFolder.addActionListener(e -> createNewFolder());
        miRefresh.addActionListener(e -> navigateTo(Path.of(txtPath.getText())));

        contextMenu.add(miMarkAsSelected);
        contextMenu.addSeparator();
        contextMenu.add(miRename);
        contextMenu.add(miDelete);
        contextMenu.addSeparator();
        contextMenu.add(miNewFolder);
        contextMenu.add(miRefresh);
    }

    private void createSelectionListContextMenu() {
        selectionListContextMenu = new JPopupMenu();

        miRemoveSelected = new JMenuItem("Remover dos Selecionados");
        miRemoveSelected.addActionListener(e -> removeFilesFromSelection());
        selectionListContextMenu.add(miRemoveSelected);

        selectionListContextMenu.addSeparator();
        miRemoveAllSelected = new JMenuItem("Limpar Seleção");
        miRemoveAllSelected.addActionListener(e -> clearSelection());
        selectionListContextMenu.add(miRemoveAllSelected);
    }

    private void initListeners() {
        txtPath.addActionListener((ActionEvent e) -> {
            Path newPath = Path.of(txtPath.getText());
            navigateTo(newPath);
        });
        txtPath.setDisplayFunction(Path::toString);
        txtPath.addSearchOption(Path::toString);
        txtPath.setOnSelectListener(this::navigateTo);

        btnUp.addActionListener(e -> {
            Path currentPath = Path.of(txtPath.getText());
            Path parentPath = currentPath.getParent();
            if (parentPath != null) {
                navigateTo(parentPath);
            }
        });

        btnListView.addActionListener(e -> viewCardLayout.show(pnlViewSwitcher, "TABLE"));
        btnGridView.addActionListener(e -> viewCardLayout.show(pnlViewSwitcher, "GRID"));
        btnShowHidden.addActionListener(e -> setShowHiddenFiles(btnShowHidden.isSelected()));

        txtSearchFolder.setDisplayFunction(FileWrapper::getDisplayName);
        txtSearchFolder.addSearchOption(FileWrapper::getDisplayName);
        txtSearchFolder.setOnSelectListener(this::selectFileInTable);

        treeNavigation.addTreeSelectionListener(e -> {
            Object lastComponent = treeNavigation.getLastSelectedPathComponent();
            if (lastComponent instanceof FileTreeNode selectedNode) {
                Object userObject = selectedNode.getUserObject();

                if (userObject instanceof File file) {
                    if (file.isDirectory()) {
                        String newPath = file.toPath().toAbsolutePath().toString();
                        if (!newPath.equals(txtPath.getText())) {
                            navigateTo(file.toPath());
                        }
                    }
                }
            }
        });

        MouseAdapter contextMenuAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleTableDoubleClick();
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showContextMenu(e);
                }
            }
        };
        tableFileView.addMouseListener(contextMenuAdapter);
        tableScrollPane.addMouseListener(contextMenuAdapter);

        MouseAdapter gridContextMenuAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleGridDoubleClick();
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showGridContextMenu(e);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showGridContextMenu(e);
                }
            }
        };
        listFileView.addMouseListener(gridContextMenuAdapter);
        listScrollPane.addMouseListener(gridContextMenuAdapter);

        listSelectedFiles.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showSelectionListContextMenu(e);
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showSelectionListContextMenu(e);
                }
            }
        });

        btnClearSelection.addActionListener(e -> clearSelection());
        btnConfirmSelection.addActionListener(e -> finalizeSelection());
    }

    private void showSelectionListContextMenu(MouseEvent e) {
        int index = listSelectedFiles.locationToIndex(e.getPoint());
        if (index != -1) {
            boolean isSelected = false;
            for (int selIndex : listSelectedFiles.getSelectedIndices()) {
                if (selIndex == index) {
                    isSelected = true;
                    break;
                }
            }
            if (!isSelected) {
                listSelectedFiles.setSelectedIndex(index);
            }
        }

        miRemoveSelected.setEnabled(!listSelectedFiles.getSelectedValuesList().isEmpty());
        miRemoveAllSelected.setEnabled(listSelectedFilesModel.size() > 1);
        selectionListContextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void removeFilesFromSelection() {
        List<File> filesToRemove = listSelectedFiles.getSelectedValuesList();
        if (filesToRemove.isEmpty()) {
            return;
        }

        for (File file : filesToRemove) {
            selectedFiles.remove(file);
        }

        updateSelectedFilesList();
        updateSelectionCountLabel();
    }

    private void showGridContextMenu(MouseEvent e) {
        int index = listFileView.locationToIndex(e.getPoint());
        if (index == -1 || e.getSource() == listScrollPane) {
            listFileView.clearSelection();
            miMarkAsSelected.setVisible(multiSelect.get());
            miMarkAsSelected.setEnabled(false);
            miRename.setEnabled(false);
            miDelete.setEnabled(false);
            miNewFolder.setEnabled(true);
        } else {
            if (!listFileView.isSelectedIndex(index)) {
                listFileView.setSelectedIndex(index);
            }
            int selectedCount = listFileView.getSelectedIndices().length;
            miMarkAsSelected.setVisible(multiSelect.get());
            miMarkAsSelected.setEnabled(selectedCount > 0);
            miRename.setEnabled(selectedCount == 1);
            miDelete.setEnabled(selectedCount > 0);
            miNewFolder.setEnabled(true);
        }
        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void handleGridDoubleClick() {
        int index = listFileView.getSelectedIndex();
        if (index < 0) return;

        FileWrapper wrapper = listFileViewModel.getElementAt(index);
        File file = wrapper.getFile();
        if (file.isDirectory()) {
            navigateTo(file.toPath());
        } else if (!multiSelect.get()) {
            if (isFileTypeValid(file)) {
                selectedFiles.clear();
                selectedFiles.add(file);
                finalizeSelection();
            }
        }
    }

    private void showContextMenu(MouseEvent e) {
        int selectedRow = tableFileView.rowAtPoint(e.getPoint());
        if (selectedRow == -1 || e.getSource() == tableScrollPane) {
            tableFileView.clearSelection();
            miMarkAsSelected.setVisible(multiSelect.get());
            miMarkAsSelected.setEnabled(false);
            miRename.setEnabled(false);
            miDelete.setEnabled(false);
            miNewFolder.setEnabled(true);
        } else {
            if (!tableFileView.isRowSelected(selectedRow)) {
                tableFileView.setRowSelectionInterval(selectedRow, selectedRow);
            }
            int selectedCount = tableFileView.getSelectedRows().length;
            miMarkAsSelected.setVisible(multiSelect.get());
            miMarkAsSelected.setEnabled(selectedCount > 0);
            miRename.setEnabled(selectedCount == 1);
            miDelete.setEnabled(selectedCount > 0);
            miNewFolder.setEnabled(true);
        }

        contextMenu.show(e.getComponent(), e.getX(), e.getY());
    }

    private void handleTableDoubleClick() {
        int viewRow = tableFileView.getSelectedRow();
        if (viewRow >= 0) {
            int modelRow = tableFileView.convertRowIndexToModel(viewRow);
            FileWrapper wrapper = tableModel.getFileWrapperAt(modelRow);
            File file = wrapper.getFile();
            if (file.isDirectory()) {
                navigateTo(file.toPath());
            } else if (!multiSelect.get()) {
                if (isFileTypeValid(file)) {
                    selectedFiles.clear();
                    selectedFiles.add(file);
                    finalizeSelection();
                }
            }
        }
    }

    private void createNewFolder() {
        String newFolderName = JOptionPane.showInputDialog(this, "Nome da nova pasta:", "Nova Pasta", JOptionPane.PLAIN_MESSAGE);
        if (newFolderName != null && !newFolderName.isBlank()) {
            try {
                Path currentPath = Path.of(txtPath.getText());
                Files.createDirectory(currentPath.resolve(newFolderName));
                navigateTo(currentPath);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Não foi possível criar a pasta: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void renameFile() {
        FileWrapper wrapper = getSelectedFileWrapperFromActiveView();
        if (wrapper == null) return;

        File fileToRename = wrapper.getFile();

        String newName = (String) JOptionPane.showInputDialog(this, "Novo nome:", "Renomear", JOptionPane.PLAIN_MESSAGE, null, null, fileToRename.getName());
        if (newName != null && !newName.isBlank() && !newName.equals(fileToRename.getName())) {
            try {
                Files.move(fileToRename.toPath(), fileToRename.toPath().resolveSibling(newName));
                navigateTo(navigatorPath);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Não foi possível renomear: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteFiles() {
        List<FileWrapper> wrappersToDelete = getSelectedFileWrappersFromActiveView();
        if (wrappersToDelete.isEmpty()) return;

        String msg = wrappersToDelete.size() == 1 ? "Tem certeza que deseja excluir este item?" : "Tem certeza que deseja excluir " + wrappersToDelete.size() + " itens?";
        int result = JOptionPane.showConfirmDialog(this, msg, "Excluir", JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            for (FileWrapper wrapper : wrappersToDelete) {
                File file = wrapper.getFile();
                try {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        Files.delete(file.toPath());
                    }
                    selectedFiles.remove(file);
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(this, "Não foi possível excluir " + file.getName() + ": " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                }
            }

            updateSelectedFilesList();
            updateSelectionCountLabel();
            navigateTo(navigatorPath);
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory.toPath())) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private void navigateTo(Path path) {
        if (path == null || !Files.exists(path) || !Files.isReadable(path)) {
            JOptionPane.showMessageDialog(this, "Caminho inválido ou sem permissão: " + path, "Erro", JOptionPane.ERROR_MESSAGE);
            txtPath.setShowPopup(false);
            txtPath.setText(navigatorPath.toString());
            txtPath.setShowPopup(true);
            return;
        }

        if (!Files.isDirectory(path)) {
            path = path.getParent();
            if (path == null) path = navigatorPath;
        }

        Path absolutePath = path.toAbsolutePath();
        navigatorPath = absolutePath;

        txtPath.setShowPopup(false);
        txtPath.setText(absolutePath.toString());
        txtPath.setShowPopup(true);

        txtSearchFolder.clearAndClose();
        tableFileView.clearSelection();
        listFileView.clearSelection();
        tableModel.setFiles(Collections.emptyList());
        listFileViewModel.clear();

        updateTreeSelection(absolutePath);

        executor.submit(() -> {
            List<FileWrapper> wrappers = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(absolutePath)) {
                for (Path p : stream) {
                    File file = p.toFile();
                    if (!passesFilters(file)) {
                        continue;
                    }

                    Icon icon = fsv.getSystemIcon(file);
                    String dn = fsv.getSystemDisplayName(file);
                    long len = file.isDirectory() ? 0L : file.length();
                    long mod = file.lastModified();
                    boolean dir = file.isDirectory();

                    wrappers.add(new FileWrapper(file, icon, dn, len, mod, dir));
                }
            } catch (IOException e) {
                System.err.println("Erro ao listar diretório: " + e.getMessage());
            }

            wrappers.sort((w1, w2) -> {
                File f1 = w1.getFile();
                File f2 = w2.getFile();
                if (f1.isDirectory() && !f2.isDirectory()) return -1;
                if (!f1.isDirectory() && f2.isDirectory()) return 1;
                return f1.getName().compareToIgnoreCase(f2.getName());
            });

            SwingUtilities.invokeLater(() -> {
                tableModel.setFiles(wrappers);
                listFileViewModel.clear();
                listFileViewModel.addAll(wrappers);
            });
        });

        updateSearchDataSources(absolutePath);
    }

    private boolean passesFilters(File file) {
        try {
            if (!showHiddenFiles.get() && file.isHidden()) {
                return false;
            }
        } catch (SecurityException e) {
            return false;
        }

        FileSelectionMode mode = fileSelectionModeAtomicReference.get();
        boolean passesMode = (mode == FileSelectionMode.FILES_AND_DIRECTORIES) ||
                (mode == FileSelectionMode.FILES_ONLY && file.isFile()) ||
                (mode == FileSelectionMode.DIRECTORIES_ONLY && file.isDirectory());

        if (!passesMode) {
            return false;
        }

        boolean passesFilter = (currentFileFilter == null) || file.isDirectory() || currentFileFilter.accept(file);

        return passesFilter;
    }

    private void updateTreeSelection(Path path) {
        DefaultTreeModel model = (DefaultTreeModel) treeNavigation.getModel();
        FileTreeNode root = (FileTreeNode) model.getRoot();
        FileTreeNode bestMatchNode = null;

        Enumeration<?> e = root.children();
        while (e.hasMoreElements()) {
            FileTreeNode node = (FileTreeNode) e.nextElement();
            if (node.getUserObject().equals("Discos")) {
                Enumeration<?> diskNodes = node.children();
                while (diskNodes.hasMoreElements()) {
                    FileTreeNode diskNode = (FileTreeNode) diskNodes.nextElement();
                    if (isPathChildOf(path, (File) diskNode.getUserObject())) {
                        bestMatchNode = diskNode;
                        break;
                    }
                }
            }

            if (node.getUserObject() instanceof File) {
                if (isPathChildOf(path, (File) node.getUserObject())) {
                    bestMatchNode = node;
                }
            }
            if (bestMatchNode != null && !bestMatchNode.getUserObject().equals("Discos")) {
                break;
            }
        }

        if (bestMatchNode != null) {
            TreePath treePath = new TreePath(bestMatchNode.getPath());
            //treeNavigation.setSelectionPath(treePath);
            treeNavigation.scrollPathToVisible(treePath);
        } else {
            treeNavigation.clearSelection();
        }
    }

    private boolean isPathChildOf(Path child, File potentialParent) {
        if (child == null || potentialParent == null) return false;
        Path parentPath = potentialParent.toPath().toAbsolutePath();
        return child.toAbsolutePath().startsWith(parentPath);
    }

    private void updateSearchDataSources(Path currentPath) {
        executor.submit(() -> {
            List<FileWrapper> wrappers = new ArrayList<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath)) {
                for (Path p : stream) {
                    File file = p.toFile();
                    if (!passesFilters(file)) continue;
                    wrappers.add(new FileWrapper(file, fsv.getSystemIcon(file), fsv.getSystemDisplayName(file), 0,0,false));
                }
            } catch (IOException e) { /* Ignora erro */ }
            SwingUtilities.invokeLater(() -> txtSearchFolder.setDataSource(wrappers));
        });

        executor.submit(() -> {
            List<Path> pathSuggestions = new ArrayList<>(getPathsDerivados(currentPath));
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(currentPath, Files::isDirectory)) {
                for (Path p : stream) {
                    if (passesFilters(p.toFile())) {
                        pathSuggestions.add(p);
                    }
                }
            } catch (IOException e) {}

            SwingUtilities.invokeLater(() -> txtPath.setDataSource(pathSuggestions));
        });
    }

    private FileWrapper getSelectedFileWrapperFromActiveView() {
        if (btnGridView.isSelected()) {
            return listFileView.getSelectedValue();
        } else {
            int viewRow = tableFileView.getSelectedRow();
            if (viewRow == -1) return null;
            int modelRow = tableFileView.convertRowIndexToModel(viewRow);
            return tableModel.getFileWrapperAt(modelRow);
        }
    }

    private List<FileWrapper> getSelectedFileWrappersFromActiveView() {
        if (btnGridView.isSelected()) {
            return listFileView.getSelectedValuesList();
        } else {
            int[] viewRows = tableFileView.getSelectedRows();
            List<FileWrapper> wrappers = new ArrayList<>(viewRows.length);
            for (int viewRow : viewRows) {
                int modelRow = tableFileView.convertRowIndexToModel(viewRow);
                wrappers.add(tableModel.getFileWrapperAt(modelRow));
            }
            return wrappers;
        }
    }

    private void markSelectedFiles() {
        List<FileWrapper> wrappersToMark = getSelectedFileWrappersFromActiveView();
        if (wrappersToMark.isEmpty()) return;

        for (FileWrapper wrapper : wrappersToMark) {
            File file = wrapper.getFile();
            if (isFileTypeValid(file)) {
                dispachEvent(EventType.INPUT, this, file);
                selectedFiles.add(file);
            }
        }
        updateSelectionCountLabel();
        updateSelectedFilesList();
        tableFileView.clearSelection();
        listFileView.clearSelection();
    }

    private boolean isFileTypeValid(File file) {
        FileSelectionMode mode = fileSelectionModeAtomicReference.get();
        if (mode == FileSelectionMode.FILES_AND_DIRECTORIES) {
            return true;
        }
        if (mode == FileSelectionMode.FILES_ONLY && file.isFile()) {
            return true;
        }
        return mode == FileSelectionMode.DIRECTORIES_ONLY && file.isDirectory();
    }

    private void updateSelectionCountLabel() {
        int count = selectedFiles.size();
        if (count == 0) {
            lblSelectionCount.setText("Nenhum item selecionado");
        } else if (count == 1) {
            lblSelectionCount.setText("1 item selecionado");
        } else {
            lblSelectionCount.setText(count + " itens selecionados");
        }
    }

    private void updateSelectedFilesList() {
        listSelectedFilesModel.clear();
        listSelectedFilesModel.addAll(selectedFiles);
    }

    private void clearSelection() {
        selectedFiles.clear();
        tableFileView.clearSelection();
        listFileView.clearSelection();
        updateSelectionCountLabel();
        updateSelectedFilesList();
    }

    private void selectFileInTable(FileWrapper wrapperToSelect) {
        if (wrapperToSelect == null) return;

        int modelRow = tableModel.findFile(wrapperToSelect.getFile());
        if (modelRow != -1) {
            int viewRow = tableFileView.convertRowIndexToView(modelRow);
            tableFileView.setRowSelectionInterval(viewRow, viewRow);
            tableFileView.scrollRectToVisible(tableFileView.getCellRect(viewRow, 0, true));

            int listIndex = listFileViewModel.indexOf(wrapperToSelect);
            if (listIndex != -1) {
                listFileView.setSelectedIndex(listIndex);
                listFileView.ensureIndexIsVisible(listIndex);
            }
        }
    }

    private void finalizeSelection() {
        if (!multiSelect.get()) {
            FileWrapper wrapper = getSelectedFileWrapperFromActiveView();
            selectedFiles.clear();
            if (wrapper != null && isFileTypeValid(wrapper.getFile())) {
                selectedFiles.add(wrapper.getFile());
            }
        }

        if (onEndSelectionListener != null) {
            onEndSelectionListener.accept(this);
        }
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    private List<Path> getPathsDerivados(Path root) {
        List<Path> paths = new ArrayList<>();
        paths.add(root);
        Path parent = root.getParent();

        while (parent != null) {
            paths.add(parent);
            parent = parent.getParent();
        }
        return paths;
    }

}