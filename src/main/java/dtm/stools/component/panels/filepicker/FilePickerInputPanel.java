package dtm.stools.component.panels.filepicker;

import dtm.stools.component.events.EventType;
import dtm.stools.component.inputfields.textfield.MaskedTextField;
import dtm.stools.component.inputfields.textfield.PathSearchTextField;
import dtm.stools.component.inputfields.textfield.SearchTextField;
import dtm.stools.component.inputfields.filepicker.FileSelectionMode;
import dtm.stools.component.panels.base.PanelEventListener;
import dtm.stools.component.panels.filepicker.models.FileWrapperTableModel;
import dtm.stools.component.panels.filepicker.renderers.*;
import dtm.stools.component.panels.filepicker.utils.FileTreeNode;
import dtm.stools.component.panels.filepicker.utils.FileWrapper;
import dtm.stools.exceptions.FileSelectionException;
import dtm.stools.i18n.I18n;
import lombok.Getter;

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
import java.nio.file.*;
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

    private static String text(String key, String defaultValue) {
        return I18n.getText(FilePickerInputPanel.class, key, defaultValue);
    }

    private final ExecutorService executor;
    private final Set<File> selectedFiles;

    @Getter
    private final Path basePath;

    private final AtomicReference<FileSelectionMode> fileSelectionModeAtomicReference;
    private final AtomicBoolean multiSelect;
    private final AtomicBoolean required;
    private final AtomicBoolean showHiddenFiles;
    private final AtomicBoolean allowNewFileInput;
    private final AtomicBoolean permitSelectFolder;

    private String btnConfirmSelectionText;

    private Path navigatorPath;

    private JPanel pnlToolbar;
    private PathSearchTextField<Path> txtPath;
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

    private JPanel pnlSingleFile;
    private JLabel lblSingleFile;
    private SearchTextField<FileWrapper> txtSingleFile;

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
        this.required = new AtomicBoolean(true);
        this.allowNewFileInput = new AtomicBoolean(false);
        this.executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> {
           Thread t = new Thread(r, "File-Picker-worker");
           t.setDaemon(true);
           return t;
        });
        this.permitSelectFolder = new AtomicBoolean(true);

        if (path != null && Files.exists(path) && Files.isDirectory(path)) {
            this.basePath = path.toAbsolutePath();
        } else {
            this.basePath = Path.of(System.getProperty("user.home")).toAbsolutePath();
        }

        this.navigatorPath = basePath;
        this.setLayout(new BorderLayout(5, 5));
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        lockUI(p -> p.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)));
        initComponents();
        createContextMenu();
        createSelectionListContextMenu();
        initListeners();

        setMultiSelectionEnabled(true);
        navigateTo(navigatorPath, null);
    }

    public void setOnEndSelection(Consumer<FilePickerInputPanel> listener) {
        this.onEndSelectionListener = listener;
    }

    public void setMultiSelectionEnabled(boolean enabled) {
        multiSelect.set(enabled);

        selectionScrollPane.setVisible(enabled);
        btnClearSelection.setVisible(enabled);

        if (enabled) {
            pnlBottomBar.remove(pnlSingleFile);
            pnlBottomBar.add(lblSelectionCount, BorderLayout.CENTER);
            lblSelectionCount.setVisible(true);
            txtSingleFile.setEditable(false);
        } else {
            pnlBottomBar.remove(lblSelectionCount);
            pnlBottomBar.add(pnlSingleFile, BorderLayout.CENTER);
            pnlSingleFile.setVisible(true);
            if (txtSingleFile != null) {
                txtSingleFile.setText("");
                permitSelectFolder.set(true);
            }
            txtSingleFile.setEditable(allowNewFileInput.get());
        }
        pnlBottomBar.revalidate();
        pnlBottomBar.repaint();

        tableFileView.setSelectionMode(enabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);
        listFileView.setSelectionMode(enabled ? ListSelectionModel.MULTIPLE_INTERVAL_SELECTION : ListSelectionModel.SINGLE_SELECTION);

        if (!enabled) {
            clearSelection();
        }

        if(btnConfirmSelectionText == null || btnConfirmSelectionText.isEmpty()){
            btnConfirmSelection.setText(enabled
                    ? text("button.select", "Selecionar")
                    : text("button.open", "Abrir"));
        }else{
            btnConfirmSelection.setText(btnConfirmSelectionText);
        }
    }

    public void setFileSelectionMode(FileSelectionMode mode) {
        fileSelectionModeAtomicReference.set(mode != null ? mode : FileSelectionMode.FILES_AND_DIRECTORIES);
        clearSelection();
        navigateTo(navigatorPath, null);
    }

    public void setConfirmSelectionVButtonText(String text){
        this.btnConfirmSelectionText = text;
    }

    public void setFileFilter(FileNameExtensionFilter filter) {
        this.currentFileFilter = filter;
        if (txtFilterView != null) {
            txtFilterView.setText(filter != null ? filter.getDescription() : text("filter.allFiles", "Todos os arquivos"));
        }
        navigateTo(navigatorPath, null);
    }

    public void setShowHiddenFiles(boolean show) {
        showHiddenFiles.set(show);
        if (btnShowHidden != null) {
            btnShowHidden.setSelected(show);
        }
        navigateTo(navigatorPath, null);
    }

    public void setRequired(boolean required){
        this.required.set(required);
    }

    public void setAllowNewFileInput(boolean enabled) {
        this.allowNewFileInput.set(enabled);
        if (!multiSelect.get()) {
            txtSingleFile.setEditable(enabled);
            this.permitSelectFolder.set(!enabled);
        }
    }

    public void setSelectedFile(String filePath) {
        if (filePath == null || filePath.isBlank()) {
            setSelectedFile((File) null);
        } else {
            setSelectedFile(new File(filePath));
        }
    }

    public void setSelectedFile(File file) {
        if (file == null) {
            clearSelection();
            return;
        }

        if (file.isDirectory() && file.exists()) {
            navigateTo(file.toPath(), null);
            txtSingleFile.setText("");
            return;
        }

        File parentDir = file.getParentFile();

        if (parentDir == null || !parentDir.exists()) {
            txtSingleFile.ignoreCurrentSugestions();
            txtSingleFile.setText(file.getName());
            return;
        }

        if (parentDir.exists()) {
            Runnable task = () -> {
                int modelRow = tableModel.findFile(file);
                if (modelRow != -1) {
                    FileWrapper wrapper = tableModel.getFileWrapperAt(modelRow);
                    if (wrapper != null) {
                        selectFileInTable(wrapper);
                    }
                } else {
                    if(!permitSelectFolder.get() && parentDir.isDirectory()) return;
                    txtSingleFile.ignoreCurrentSugestions();
                    txtSingleFile.setText(file.getName());
                }
            };
            navigateTo(parentDir.toPath(), task);
        }
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

    public void dispose(){
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window != null) {
            window.dispose();
        }
    }

    private void initComponents() {
        pnlToolbar = new JPanel(new BorderLayout(5, 0));

        txtPath = new PathSearchTextField<>(FileSystems.getDefault().getSeparator());
        txtPath.useStartsWithSearch();
        txtPath.setCaseSensitive(false);
        txtPath.setShowPopup(false);
        txtPath.addEventListener(PathSearchTextField.PATH_SEGMENT_CLICK, e -> {
            String pathStr = e.tryGetValue();
            navigateTo(Paths.get(pathStr), null);
        });

        btnUp = new JButton("↑");
        btnUp.setToolTipText(text("tooltip.up", "Subir um nível"));
        btnUp.setMargin(new Insets(2, 5, 2, 5));

        txtSearchFolder = new SearchTextField<>(null);
        txtSearchFolder.setPlaceholder(text("placeholder.search", "pesquisar..."));
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
        selectionScrollPane.setBorder(BorderFactory.createTitledBorder(text("title.selection", "Seleção")));
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

        txtFilterView = new JTextField(text("filter.allFiles", "Todos os arquivos"));
        txtFilterView.setEditable(false);
        txtFilterView.setPreferredSize(new Dimension(180, (int) txtFilterView.getPreferredSize().getHeight()));

        btnShowHidden = new JToggleButton(text("button.hidden", "Ocultos"));
        btnShowHidden.setToolTipText(text("tooltip.hidden", "Exibir arquivos ocultos"));

        btnListView = new JToggleButton(text("button.list", "Lista"));
        btnGridView = new JToggleButton(text("button.grid", "Grade"));
        ButtonGroup viewGroup = new ButtonGroup();
        viewGroup.add(btnListView);
        viewGroup.add(btnGridView);
        btnListView.setSelected(true);

        pnlConfigBar.add(txtFilterView);
        pnlConfigBar.addSeparator();
        pnlConfigBar.add(btnShowHidden);
        pnlConfigBar.add(btnListView);
        pnlConfigBar.add(btnGridView);

        lblSingleFile = new JLabel(text("label.file", "Arquivo:"));
        txtSingleFile = new SearchTextField<>(30);
        txtSingleFile.setEditable(false);

        txtSingleFile.setDisplayFunction(r -> r.getFile().getName());
        txtSingleFile.addSearchOption(r -> r.getFile().getName());
        txtSingleFile.setOnSelectListener(this::selectFileInTable);

        pnlSingleFile = new JPanel(new BorderLayout(5, 0));
        pnlSingleFile.setBorder(new EmptyBorder(0, 5, 0, 0));
        pnlSingleFile.add(lblSingleFile, BorderLayout.WEST);
        pnlSingleFile.add(txtSingleFile, BorderLayout.CENTER);

        pnlBottomBar.add(pnlConfigBar, BorderLayout.WEST);

        lblSelectionCount = new JLabel(text("selection.none", "Nenhum item selecionado"));
        lblSelectionCount.setHorizontalAlignment(SwingConstants.CENTER);

        btnClearSelection = new JButton(text("button.clearSelection", "Limpar Seleção"));
        btnConfirmSelection = new JButton(text("button.select", "Selecionar"));

        JPanel pnlBottomButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        pnlBottomButtons.add(btnClearSelection);
        pnlBottomButtons.add(btnConfirmSelection);

        pnlBottomBar.add(pnlBottomButtons, BorderLayout.EAST);

        this.add(pnlToolbar, BorderLayout.NORTH);
        this.add(splitPane, BorderLayout.CENTER);
        this.add(pnlBottomBar, BorderLayout.SOUTH);
    }

    private void setupNavigationTree() {
        FileTreeNode root = new FileTreeNode(text("tree.root", "Raiz"));

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

        FileTreeNode nodeDiscos = new FileTreeNode(text("tree.disks", "Discos"));
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
        miMarkAsSelected = new JMenuItem(text("menu.markSelected", "Marcar como Selecionado"));
        miRename = new JMenuItem(text("menu.rename", "Renomear"));
        miDelete = new JMenuItem(text("menu.delete", "Excluir"));
        miNewFolder = new JMenuItem(text("menu.newFolder", "Nova Pasta"));
        miRefresh = new JMenuItem(text("menu.refresh", "Atualizar"));

        miMarkAsSelected.addActionListener(e -> markSelectedFiles());
        miRename.addActionListener(e -> renameFile());
        miDelete.addActionListener(e -> deleteFiles());
        miNewFolder.addActionListener(e -> createNewFolder());
        miRefresh.addActionListener(e -> navigateTo(Path.of(txtPath.getText()), null));

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

        miRemoveSelected = new JMenuItem(text("menu.removeSelected", "Remover dos Selecionados"));
        miRemoveSelected.addActionListener(e -> removeFilesFromSelection());
        selectionListContextMenu.add(miRemoveSelected);

        selectionListContextMenu.addSeparator();
        miRemoveAllSelected = new JMenuItem(text("menu.clearSelection", "Limpar Seleção"));
        miRemoveAllSelected.addActionListener(e -> clearSelection());
        selectionListContextMenu.add(miRemoveAllSelected);
    }

    private void initListeners() {
        txtPath.addActionListener((ActionEvent e) -> {
            Path newPath = Path.of(txtPath.getText());
            navigateTo(newPath, null);
        });
        txtPath.setDisplayFunction(Path::toString);
        txtPath.addSearchOption(Path::toString);
        txtPath.setOnSelectListener(this::navigateTo);

        btnUp.addActionListener(e -> {
            Path currentPath = Path.of(txtPath.getText());
            Path parentPath = currentPath.getParent();
            if (parentPath != null) {
                navigateTo(parentPath, null);
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
                            navigateTo(file.toPath(), null);
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

        tableFileView.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !multiSelect.get()) {
                updateSingleFileTextField();
            }
        });

        listFileView.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !multiSelect.get()) {
                updateSingleFileTextField();
            }
        });

        txtSingleFile.addEventListener(EventType.INPUT, e -> {
            permitSelectFolder.set(false);
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
            navigateTo(file.toPath(), null);
        } else if (!multiSelect.get()) {
            if (isFileTypeValid(file)) {
                txtSingleFile.ignoreCurrentSugestions();
                txtSingleFile.setText(file.getName());
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
                navigateTo(file.toPath(), null);
            } else if (isFileTypeValid(file)) {
                if(multiSelect.get()){
                    if(markAsSelectedFile(file)){
                        dispachEvent(EventType.INPUT, this, file, new HashMap<>());
                        updateSelectionCountLabel();
                        updateSelectedFilesList();
                    }
                }else{
                    txtSingleFile.ignoreCurrentSugestions();
                    txtSingleFile.setText(file.getName());
                    permitSelectFolder.set(true);
                    finalizeSelection();
                }
            }
        }
    }

    private void createNewFolder() {
        String newFolderName = JOptionPane.showInputDialog(
                this,
                text("dialog.newFolder.message", "Nome da nova pasta:"),
                text("dialog.newFolder.title", "Nova Pasta"),
                JOptionPane.PLAIN_MESSAGE
        );
        if (newFolderName != null && !newFolderName.isBlank()) {
            try {
                Path currentPath = Path.of(txtPath.getText());
                Files.createDirectory(currentPath.resolve(newFolderName));
                navigateTo(currentPath, null);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                        this,
                        text("error.createFolder", "Não foi possível criar a pasta: ") + e.getMessage(),
                        text("title.error", "Erro"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void renameFile() {
        FileWrapper wrapper = getSelectedFileWrapperFromActiveView();
        if (wrapper == null) return;

        File fileToRename = wrapper.getFile();

        String newName = (String) JOptionPane.showInputDialog(
                this,
                text("dialog.rename.message", "Novo nome:"),
                text("dialog.rename.title", "Renomear"),
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                fileToRename.getName()
        );
        if (newName != null && !newName.isBlank() && !newName.equals(fileToRename.getName())) {
            try {
                Files.move(fileToRename.toPath(), fileToRename.toPath().resolveSibling(newName));
                navigateTo(navigatorPath, null);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(
                        this,
                        text("error.rename", "Não foi possível renomear: ") + e.getMessage(),
                        text("title.error", "Erro"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void deleteFiles() {
        List<FileWrapper> wrappersToDelete = getSelectedFileWrappersFromActiveView();
        if (wrappersToDelete.isEmpty()) return;

        String msg = wrappersToDelete.size() == 1
                ? text("dialog.delete.one", "Tem certeza que deseja excluir este item?")
                : text("dialog.delete.many", "Tem certeza que deseja excluir {count} itens?")
                        .replace("{count}", String.valueOf(wrappersToDelete.size()));
        int result = JOptionPane.showConfirmDialog(this, msg, text("dialog.delete.title", "Excluir"), JOptionPane.YES_NO_OPTION);

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
                    JOptionPane.showMessageDialog(
                            this,
                            text("error.delete", "Não foi possível excluir ") + file.getName() + ": " + e.getMessage(),
                            text("title.error", "Erro"),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

            updateSelectedFilesList();
            updateSelectionCountLabel();
            navigateTo(navigatorPath, null);
        }
    }

    private void deleteDirectory(File directory) throws IOException {
        try (Stream<Path> walk = Files.walk(directory.toPath())) {
            walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    private void navigateTo(Path path, Runnable onComplete){
        navigateTo(path, onComplete, true);
    }

    private void navigateTo(Path path, Runnable onComplete, boolean executeWithExecutor) {
        if (path == null || !Files.exists(path) || !Files.isReadable(path)) {
            JOptionPane.showMessageDialog(
                    this,
                    text("error.invalidPath", "Caminho inválido ou sem permissão: ") + path,
                    text("title.error", "Erro"),
                    JOptionPane.ERROR_MESSAGE
            );
            txtPath.setShowPopup(false);
            txtPath.setText(navigatorPath.toString());
            txtPath.setShowPopup(true);
            return;
        }

        lockUI();
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

        Runnable loadAction = () -> {
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
                unlockUI(p -> p.setCursor(Cursor.getDefaultCursor()));

                if (onComplete != null) {
                    onComplete.run();
                }
            });
            txtSingleFile.setDataSource(wrappers);
        };

        if(executeWithExecutor){
            executor.submit(loadAction);
        }else{
            loadAction.run();
        }

        updateSearchDataSources(absolutePath);
    }

    private void navigateTo(Path path) {
        navigateTo(path, null);
    }

    private boolean passesFilters(File file) {
        try {
            if (!showHiddenFiles.get() && file.isHidden()) {
                return false;
            }

            if (file.isDirectory()) {
                return true;
            }

            if(file.isFile() && fileSelectionModeAtomicReference.get() == FileSelectionMode.DIRECTORIES_ONLY){
                return false;
            }

            if (currentFileFilter != null) {
                String fileName = file.getName();
                int i = fileName.lastIndexOf('.');

                if (i > 0 && i < fileName.length() - 1) {
                    String extension = fileName.substring(i + 1);

                    for (String allowedExt : currentFileFilter.getExtensions()) {
                        if (allowedExt.equalsIgnoreCase(extension)) {
                            return true;
                        }
                    }
                }
                return false;
            }

        } catch (SecurityException e) {
            return false;
        }

        return true;
    }

    private void updateTreeSelection(Path path) {
        DefaultTreeModel model = (DefaultTreeModel) treeNavigation.getModel();
        FileTreeNode root = (FileTreeNode) model.getRoot();
        FileTreeNode bestMatchNode = null;

        Enumeration<?> e = root.children();
        while (e.hasMoreElements()) {
            FileTreeNode node = (FileTreeNode) e.nextElement();
            if (node.getUserObject().equals(text("tree.disks", "Discos"))) {
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
            if (bestMatchNode != null && !bestMatchNode.getUserObject().equals(text("tree.disks", "Discos"))) {
                break;
            }
        }

        if (bestMatchNode != null) {
            TreePath treePath = new TreePath(bestMatchNode.getPath());
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
            } catch (IOException e) { }
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
                dispachEvent(EventType.INPUT, this, file, new HashMap<>());
                selectedFiles.add(file);
            }
        }
        updateSelectionCountLabel();
        updateSelectedFilesList();
        tableFileView.clearSelection();
        listFileView.clearSelection();
    }

    private boolean isFileTypeValid(File file) {
        if(file == null) return false;
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
            lblSelectionCount.setText(text("selection.none", "Nenhum item selecionado"));
        } else if (count == 1) {
            lblSelectionCount.setText(text("selection.one", "1 item selecionado"));
        } else {
            lblSelectionCount.setText(text("selection.many", "{count} itens selecionados")
                    .replace("{count}", String.valueOf(count)));
        }
    }

    private void updateSelectedFilesList() {
        listSelectedFilesModel.clear();
        listSelectedFilesModel.addAll(selectedFiles);
    }

    private void updateSingleFileTextField() {
        if (multiSelect.get()) {
            txtSingleFile.setText("");
            permitSelectFolder.set(true);
            return;
        }

        boolean selectFolder = permitSelectFolder.get();
        FileWrapper wrapper = getSelectedFileWrapperFromActiveView();

        if(wrapper == null) return;

        if(!selectFolder && wrapper.isDirectory()) return;

        if (isFileTypeValid(wrapper.getFile())) {
            txtSingleFile.ignoreCurrentSugestions();
            txtSingleFile.setText(wrapper.getFile().getName());
            permitSelectFolder.set(true);
        } else {
            txtSingleFile.setText("");
        }
    }

    private void clearSelection() {
        selectedFiles.clear();
        tableFileView.clearSelection();
        listFileView.clearSelection();
        updateSelectionCountLabel();
        updateSelectedFilesList();
        if (txtSingleFile != null) {
            txtSingleFile.setText("");
        }
        permitSelectFolder.set(true);
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
            selectedFiles.clear();
            String fileName = txtSingleFile.getText();

            if (fileName != null && !fileName.isBlank()) {
                File expectedFile = navigatorPath.resolve(fileName).toFile();
                boolean isNewFile = (allowNewFileInput.get()) && !expectedFile.exists();

                if(!expectedFile.isDirectory()){
                    String extension = getFileExtension(expectedFile.getName());
                    boolean hasExtension = extension != null && !extension.isBlank();
                    boolean validExtension = isExtensionValid(extension);

                    if (!hasExtension && currentFileFilter != null) {
                        String[] exts = currentFileFilter.getExtensions();
                        if (exts.length == 1) {
                            String rawExt = exts[0];
                            String cleanExt = rawExt.startsWith(".") ? rawExt.substring(1) : rawExt;
                            String sep = expectedFile.getName().endsWith(".") ? "" : ".";
                            expectedFile = new File(expectedFile.getAbsolutePath() + sep + cleanExt);
                            extension = cleanExt;
                            hasExtension = true;
                            validExtension = true;
                        }
                    }

                    if (!validExtension) {
                        String message;
                        if (!hasExtension) {
                            message = text(
                                    "validation.missingExtension",
                                    "O arquivo selecionado não possui uma extensão.\nPor favor, insira uma extensão válida, como: {extensions}"
                            ).replace("{extensions}", getValidExtensionsString());
                        } else {
                            String allowed = String.join(", ", currentFileFilter.getExtensions());
                            message = text(
                                    "validation.invalidExtension",
                                    "A extensão do arquivo '{extension}' não é permitida.\nExtensões válidas: {extensions}."
                            )
                                    .replace("{extension}", extension)
                                    .replace("{extensions}", allowed);
                        }

                        JOptionPane.showMessageDialog(
                                this,
                                message,
                                text("title.invalidExtension", "Extensão inválida"),
                                JOptionPane.WARNING_MESSAGE
                        );
                        return;
                    }

                }

                if (isNewFile) {
                    markAsSelectedFile(expectedFile);
                } else {
                    FileWrapper wrapper = getSelectedFileWrapperFromActiveView();
                    if (wrapper != null && wrapper.getFile().getName().equals(fileName) && isFileTypeValid(wrapper.getFile())) {
                        markAsSelectedFile(wrapper.getFile());
                    }
                }
            }
        }

        if(selectedFiles.isEmpty() && required.get()){
            String message;
            if (multiSelect.get()) {
                message = text("validation.selectAtLeastOne", "Selecione ao menos 1 arquivo.");
            } else if (allowNewFileInput.get() || (!txtSingleFile.getText().isBlank()) ) {
                message = text("validation.typeFileName", "Digite um nome de arquivo.");
            } else {
                message = text("validation.selectFile", "Selecione um arquivo.");
            }
            JOptionPane.showMessageDialog(this, message, text("title.requiredSelection", "Seleção Obrigatória"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (onEndSelectionListener != null) {
            onEndSelectionListener.accept(this);
        }

        if(!executor.isShutdown()){
            executor.shutdown();
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

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase();
        }
        return null;
    }

    private boolean isExtensionValid(String extension) {
        if (currentFileFilter == null || currentFileFilter.getExtensions() == null)
            return true;

        for (String ext : currentFileFilter.getExtensions()) {
            if (ext.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    private String getValidExtensionsString(){
        if (currentFileFilter == null || currentFileFilter.getExtensions() == null)
            return ".txt";

        String[] exts = currentFileFilter.getExtensions();
        if (exts.length == 0)
            return ".txt";

        return String.join(", ",
                Arrays.stream(exts)
                        .map(ext -> ext.startsWith(".") ? ext : "." + ext)
                        .toArray(String[]::new)
        );

    }

    private boolean markAsSelectedFile(File file) {
        try {
            if (file.exists()) {
                validateExistingFile(file);
            } else {
                validateNewFile(file);
            }
            performFileSelection(file);
            return true;
        } catch (FileSelectionException e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    e.getTitle(),
                    JOptionPane.WARNING_MESSAGE
            );
            return false;
        }
    }

    private void validateExistingFile(File file) {
        FileSelectionMode mode = fileSelectionModeAtomicReference.get();

        if (mode == FileSelectionMode.FILES_ONLY) {
            if (file.isDirectory()) {
                throw new FileSelectionException(
                        text("title.invalidType", "Tipo Inválido"),
                        text("validation.expectedFile", "A seleção deve ser um arquivo, mas uma pasta foi selecionada.")
                );
            }
            validateFileExtension(file);

        } else if (mode == FileSelectionMode.DIRECTORIES_ONLY) {
            if (file.isFile()) {
                throw new FileSelectionException(
                        text("title.invalidType", "Tipo Inválido"),
                        text("validation.expectedFolder", "A seleção deve ser uma pasta, mas um arquivo foi selecionado.")
                );
            }
        }
    }

    private void validateNewFile(File file) {
        if (!allowNewFileInput.get()) {
            throw new FileSelectionException(
                    text("title.missingFile", "Arquivo Inexistente"),
                    text("validation.fileDoesNotExist", "O arquivo selecionado não existe.")
            );
        }

        if (fileSelectionModeAtomicReference.get() == FileSelectionMode.FILES_ONLY) {
            validateFileExtension(file);
        }
    }

    private void validateFileExtension(File file) {
        if (currentFileFilter == null) return;

        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        String extension = (dotIndex > 0 && dotIndex < fileName.length() - 1) ? fileName.substring(dotIndex + 1) : "";

        if (extension.isEmpty()) {
            throw new FileSelectionException(
                    text("title.invalidExtensionAlt", "Extensão Inválida"),
                    text("validation.fileMustHaveExtension", "O arquivo deve possuir uma extensão.")
            );
        }

        boolean isValid = false;
        for (String allowedExt : currentFileFilter.getExtensions()) {
            if (allowedExt.equalsIgnoreCase(extension)) {
                isValid = true;
                break;
            }
        }

        if (!isValid) {
            throw new FileSelectionException(
                    text("title.invalidExtensionAlt", "Extensão Inválida"),
                    text("validation.extensionNotAllowed", "A extensão do arquivo não é permitida. Esperado: {expected}")
                            .replace("{expected}", currentFileFilter.getDescription())
            );
        }
    }

    private void performFileSelection(File file) {
        selectedFiles.add(file);
    }

}
